package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto;

import java.math.BigDecimal;

public record PaymentProviderRequest(
        String accountId,
        BigDecimal amount,
        String currency
) {
}
