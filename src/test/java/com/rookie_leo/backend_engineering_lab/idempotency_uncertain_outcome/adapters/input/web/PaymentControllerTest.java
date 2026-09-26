package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto.PaymentRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto.PaymentResponse;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input.PaymentUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private final PaymentUseCase useCase = mock(PaymentUseCase.class);
    private final PaymentController controller = new PaymentController(useCase);

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCESS", "DECLINED"})
    void mapsInputAndReturnsBusinessOutcomeWithHttp200(PaymentStatus outcome) {
        var request = new PaymentRequest(new BigDecimal("123.45"), "BRL");
        var expectedPayment = new PaymentDomain("ACC-0042", request.amount(), request.currency(), PaymentStatus.RECEIVED);
        when(useCase.processPayment(expectedPayment)).thenReturn(expectedPayment.withStatus(outcome));

        var response = controller.payment("ACC-0042", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PaymentResponse(outcome.name()), response.getBody());
        verify(useCase).processPayment(expectedPayment);
        verifyNoMoreInteractions(useCase);
    }

    @Test
    void propagatesUseCaseFailureInsteadOfReturningSuccess() {
        var request = new PaymentRequest(new BigDecimal("1500.00"), "BRL");
        var payment = new PaymentDomain("ACC-0004", request.amount(), request.currency(), PaymentStatus.RECEIVED);
        var failure = new IllegalStateException("Provider unavailable");
        when(useCase.processPayment(payment)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.payment("ACC-0004", request)));
        verify(useCase).processPayment(payment);
        verifyNoMoreInteractions(useCase);
    }
}
