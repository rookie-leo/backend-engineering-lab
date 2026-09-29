package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions;

public class PaymentProviderTimeoutException extends PaymentProviderException {
    public PaymentProviderTimeoutException(String message, Throwable ex) {
        super(message, ex);
    }
}
