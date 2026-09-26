package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRequest(
        BigDecimal amount,
        String currency
) {
    public PaymentDomain toDomain(String accountId) {
        return new PaymentDomain(
                accountId,
                amount,
                currency,
                PaymentStatus.PROCESSING,
                LocalDateTime.now()
        );
    }
}
