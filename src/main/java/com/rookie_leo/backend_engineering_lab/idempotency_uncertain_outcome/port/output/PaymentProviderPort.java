package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.output;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;

public interface PaymentProviderPort {
    PaymentStatus processPayment(PaymentDomain domain);
}
