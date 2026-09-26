package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.application;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.PaymentProvider;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderResponse;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input.PaymentUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService implements PaymentUseCase {

    private PaymentProvider provider;

    @Autowired
    public PaymentService(PaymentProvider provider) {
        this.provider = provider;
    }

    @Override
    public PaymentDomain processPayment(PaymentDomain domain) {
        PaymentProviderRequest providerRequest = new PaymentProviderRequest(
                domain.accountId(),
                domain.amount(),
                domain.currency()
        );

        PaymentProviderResponse providerResponse = provider.processPayment(providerRequest);

        return domain.withStatus(PaymentStatus.valueOf(providerResponse.status()));
    }
}
