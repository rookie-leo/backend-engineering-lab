package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class PaymentProviderHttpClient implements PaymentProvider {

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
    public PaymentProviderResponse processPayment(PaymentProviderRequest providerRequest) {
        var body = new ProviderBody(
                providerRequest.amount(),
                providerRequest.currency()
        );

        var response = restClient.post()
                .uri("/api/payment-provider/{accountId}", providerRequest.accountId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(PaymentProviderResponse.class);

        if (response == null)
            throw new IllegalStateException("Payment Provider retornou uma resposta sem corpo");

        return response;
    }

    public record ProviderBody(BigDecimal amount, String currency) {
    }
}
