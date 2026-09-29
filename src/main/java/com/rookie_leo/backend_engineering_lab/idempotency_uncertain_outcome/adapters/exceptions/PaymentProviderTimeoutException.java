package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions;

import org.springframework.web.client.ResourceAccessException;

public class PaymentProviderTimeoutException extends RuntimeException {
    public PaymentProviderTimeoutException(String message, ResourceAccessException ex) {
        super(message, ex);
    }
}
