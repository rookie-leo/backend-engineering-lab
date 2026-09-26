package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record PaymentProviderResponse(
        String status,
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate createdAt
) {}
