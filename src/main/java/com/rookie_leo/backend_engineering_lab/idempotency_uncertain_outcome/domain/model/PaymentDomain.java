package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto.PaymentResponse;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDomain(
        String accountId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDateTime createdAt
) {
    public PaymentResponse toResponse() {
        return new PaymentResponse(
                status.toString(),
                createdAt
        );
    }

    public PaymentDomain withStatus(PaymentStatus newStatus) {
        return new PaymentDomain(
                accountId, amount, currency, newStatus, createdAt
        );
    }
}
