package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.application;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input.PaymentUseCase;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.output.PaymentProviderPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService implements PaymentUseCase {

    private PaymentProviderPort provider;

    @Autowired
    public PaymentService(PaymentProviderPort provider) {
        this.provider = provider;
    }

    @Override
    public PaymentDomain processPayment(PaymentDomain domain) {
        return domain.withStatus(provider.processPayment(domain));
    }
}
