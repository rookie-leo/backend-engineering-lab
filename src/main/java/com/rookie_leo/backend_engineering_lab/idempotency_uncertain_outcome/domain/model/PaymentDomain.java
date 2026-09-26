package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentDomain(
        String accountId,
        BigDecimal amount,
        String currency,
        PaymentStatus status
) {

    public PaymentDomain withStatus(PaymentStatus newStatus) {
        return new PaymentDomain(
                accountId, amount, currency, newStatus
        );
    }
}
