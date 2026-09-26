package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto.PaymentProviderResponse;

public interface PaymentProvider {
    PaymentProviderResponse processPayment(PaymentProviderRequest providerRequest);
}
