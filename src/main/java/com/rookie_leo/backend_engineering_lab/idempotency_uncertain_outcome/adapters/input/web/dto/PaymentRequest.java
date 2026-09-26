package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        String currency
) {}
