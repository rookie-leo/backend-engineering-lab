package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.handler;

import java.util.Map;
import java.util.Optional;

public record ApiErrorResponse (
    Integer errorCode,
    String errorMessage,
    Optional<Map<String, String>> errorsDetails
) {}