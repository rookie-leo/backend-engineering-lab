package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions;

import org.springframework.web.client.HttpServerErrorException;

public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
