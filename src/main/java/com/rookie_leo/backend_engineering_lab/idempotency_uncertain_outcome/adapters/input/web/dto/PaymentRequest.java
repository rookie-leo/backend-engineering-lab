package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Campo obrigatório")
        String idempotencyId,

        @NotNull(message = "amount é obrigatório")
        @DecimalMin(value = "0.1", message = "amount deve ser maior ou igual a 0.1")
        BigDecimal amount,

        @NotBlank(message = "Campo obrigatório")
        String currency
) {}
