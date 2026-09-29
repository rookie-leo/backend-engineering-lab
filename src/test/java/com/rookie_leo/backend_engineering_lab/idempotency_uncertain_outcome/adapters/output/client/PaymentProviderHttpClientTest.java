package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderTimeoutException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderHttpClientTest {

    private final PaymentDomain payment = new PaymentDomain(
            "ACC-0042", new BigDecimal("123.45"), "BRL", PaymentStatus.RECEIVED);

    // The interceptor replaces transport only: RestClient still serializes requests,
    // handles HTTP status codes and deserializes responses. No sockets or sleeps.
    private PaymentProviderHttpClient client(ClientHttpRequestInterceptor transport) {
        return new PaymentProviderHttpClient(
                RestClient.builder().requestInterceptor(transport),
                "http://provider.example", Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    private MockClientHttpResponse response(HttpStatus status, String body) {
        var response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCESS", "DECLINED"})
    void sendsExpectedHttpContractAndTranslatesBusinessOutcome(PaymentStatus status) {
        var calls = new AtomicInteger();
        var client = client((request, body, execution) -> {
            calls.incrementAndGet();
            assertEquals(HttpMethod.POST, request.getMethod());
            assertEquals("http://provider.example/api/payment-provider/ACC-0042", request.getURI().toString());
            assertEquals(MediaType.APPLICATION_JSON, request.getHeaders().getContentType());
            assertTrue(request.getHeaders().getAccept().contains(MediaType.APPLICATION_JSON));
            var json = JsonMapper.builder().build().readTree(body);
            assertEquals(2, json.size());
            assertEquals(0, new BigDecimal("123.45").compareTo(json.get("amount").decimalValue()));
            assertEquals("BRL", json.get("currency").asText());
            return response(HttpStatus.OK,
                    "{\"status\":\"" + status + "\",\"createdAt\":\"26-09-2026\"}");
        });

        assertEquals(status, client.processPayment(payment));
        assertEquals(1, calls.get());
    }

    @Test
    void rejectsResponseWithoutBody() {
        var client = client((request, body, execution) -> response(HttpStatus.OK, ""));

        var error = assertThrows(IllegalStateException.class, () -> client.processPayment(payment));

        assertEquals("Payment Provider retornou uma resposta sem corpo", error.getMessage());
    }

    @Test
    void wrapsServerErrorAndPreservesCauseWithoutRetry() {
        var calls = new AtomicInteger();
        var client = client((request, body, execution) -> {
            calls.incrementAndGet();
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "{\"message\":\"provider failed\"}");
        });

        var error = assertThrowsExactly(PaymentProviderException.class, () -> client.processPayment(payment));

        var cause = assertInstanceOf(HttpServerErrorException.class, error.getCause());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, cause.getStatusCode());
        assertTrue(cause.getResponseBodyAsString().contains("provider failed"));
        assertEquals(1, calls.get());
    }

    @Test
    void distinguishesTimeoutAndPreservesCauseWithoutRetry() {
        var cause = new HttpTimeoutException("Response deadline exceeded");
        var calls = new AtomicInteger();
        var client = failingClient(cause, calls);

        var error = assertThrows(PaymentProviderTimeoutException.class, () -> client.processPayment(payment));

        assertInstanceOf(ResourceAccessException.class, error.getCause());
        assertSame(cause, error.getCause().getCause());
        assertEquals(1, calls.get());
    }

    @Test
    void distinguishesConnectionFailureFromTimeoutWithoutRetry() {
        var cause = new ConnectException("Connection refused");
        var calls = new AtomicInteger();
        var client = failingClient(cause, calls);

        var error = assertThrowsExactly(PaymentProviderException.class, () -> client.processPayment(payment));

        assertInstanceOf(ResourceAccessException.class, error.getCause());
        assertSame(cause, error.getCause().getCause());
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotConvertNotFoundIntoBusinessDecline() {
        var client = client((request, body, execution) -> response(HttpStatus.NOT_FOUND, "{}"));

        var error = assertThrows(HttpClientErrorException.class, () -> client.processPayment(payment));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void rejectsUnknownProviderStatus() {
        var client = client((request, body, execution) -> response(HttpStatus.OK, "{\"status\":\"UNRECOGNIZED\"}"));

        assertThrows(IllegalArgumentException.class, () -> client.processPayment(payment));
    }

    @Test
    void rejectsMalformedJsonInsteadOfReturningSuccess() {
        var client = client((request, body, execution) -> response(HttpStatus.OK, "not-json"));

        assertThrows(RestClientException.class, () -> client.processPayment(payment));
    }

    private PaymentProviderHttpClient failingClient(IOException failure, AtomicInteger calls) {
        return client((request, body, execution) -> {
            calls.incrementAndGet();
            throw failure;
        });
    }
}
