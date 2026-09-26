package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentDomainTest {

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCESS", "DECLINED"})
    void withStatusPreservesPaymentDataAndLeavesOriginalUnchanged(PaymentStatus status) {
        var original = new PaymentDomain("ACC-0042", new BigDecimal("123.45"), "BRL", PaymentStatus.RECEIVED);

        var updated = original.withStatus(status);

        assertEquals(new PaymentDomain("ACC-0042", new BigDecimal("123.45"), "BRL", status), updated);
        assertEquals(PaymentStatus.RECEIVED, original.status());
        assertNotSame(original, updated);
    }
}
