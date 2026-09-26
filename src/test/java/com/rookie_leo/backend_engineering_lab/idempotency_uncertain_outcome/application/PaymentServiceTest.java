package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.application;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.output.PaymentProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private final PaymentProviderPort provider = mock(PaymentProviderPort.class);
    private final PaymentService service = new PaymentService(provider);

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCESS", "DECLINED"})
    void returnsProviderOutcomeWithoutChangingOriginalPayment(PaymentStatus outcome) {
        var payment = receivedPayment();
        when(provider.processPayment(payment)).thenReturn(outcome);

        var result = service.processPayment(payment);

        assertEquals(new PaymentDomain("ACC-0001", new BigDecimal("1500.00"), "BRL", outcome), result);
        assertEquals(PaymentStatus.RECEIVED, payment.status());
        assertNotSame(payment, result);
        verify(provider).processPayment(payment);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void propagatesProviderFailureWithoutRetryOrChangingPayment() {
        var payment = receivedPayment();
        var failure = new IllegalStateException("Provider unavailable");
        when(provider.processPayment(payment)).thenThrow(failure);

        var thrown = assertThrows(IllegalStateException.class, () -> service.processPayment(payment));

        assertSame(failure, thrown);
        assertEquals(PaymentStatus.RECEIVED, payment.status());
        verify(provider).processPayment(payment);
        verifyNoMoreInteractions(provider);
    }

    private PaymentDomain receivedPayment() {
        return new PaymentDomain("ACC-0001", new BigDecimal("1500.00"), "BRL", PaymentStatus.RECEIVED);
    }
}
