package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions;

public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
