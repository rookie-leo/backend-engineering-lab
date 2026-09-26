package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;

public interface PaymentUseCase {
    PaymentDomain processPayment(PaymentDomain domain);
}
