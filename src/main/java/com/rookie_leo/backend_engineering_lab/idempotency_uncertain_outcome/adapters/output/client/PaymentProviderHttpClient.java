package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderTimeoutException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderResponse;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.output.PaymentProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
public class PaymentProviderHttpClient implements PaymentProviderPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderHttpClient.class);
    private final RestClient restClient;

    public PaymentProviderHttpClient(
            RestClient.Builder builder,
            @Value("${provider.payment.base-url}") String baseUrl,
            @Value("${provider.payment.connect-timeout}") Duration connectTimeout,
            @Value("${provider.payment.read-timeout}") Duration readTimeout
    ) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public PaymentStatus processPayment(PaymentDomain domain) {
        var providerRequest = new PaymentProviderRequest(
                domain.accountId(),
                domain.amount(),
                domain.currency()
        );

        var body = new ProviderBody(
                providerRequest.amount(),
                providerRequest.currency()
        );

        try {
            var response = restClient.post()
                    .uri("/api/payment-provider/{accountId}", providerRequest.accountId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PaymentProviderResponse.class);

            if (response == null)
                throw new IllegalStateException("Payment Provider retornou uma resposta sem corpo");

            return PaymentStatus.valueOf(response.status());
        } catch (HttpServerErrorException ex) {
            throw new PaymentProviderException("Payment Provider retornou erro", ex);
        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof HttpTimeoutException)
                throw new PaymentProviderTimeoutException(
                        "Payment Provider excedeu o tempo limite de resposta",
                        ex
                );

            throw new PaymentProviderException("Falha de comunicação com Payment Provider", ex);
        }
    }

    public record ProviderBody(BigDecimal amount, String currency) {
    }
}
