package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.handler;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderTimeoutException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.PaymentController;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input.PaymentUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void timeoutReturns504AndCommunicatesUncertaintyWithoutInternalDetails() {
        var exception = new PaymentProviderTimeoutException("internal-host:8081", new HttpTimeoutException("deadline"));

        var response = handler.handlePaymentProviderTimeoutException(exception);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals(new ApiErrorResponse(504,
                "Não foi possível confirmar o resultado do pagamento.", Optional.empty()), response.getBody());
    }

    @Test
    void providerFailureReturns502WithoutExposingInternalDetails() {
        var exception = new PaymentProviderException("internal-host:8081", new ConnectException("refused"));

        var response = handler.handlePaymentProviderException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(new ApiErrorResponse(502,
                "Não foi possível processar o pagamento devido a uma falha no provedor de pagamentos.",
                Optional.empty()), response.getBody());
    }

    // Standalone MVC complements the direct unit tests: it verifies exception
    // annotation routing and the actual JSON contract without starting Boot or a server.
    @ParameterizedTest
    @MethodSource("failures")
    void routesControllerFailureToAdviceAndSerializesPublicError(RuntimeException failure, int code, String message)
            throws Exception {
        var useCase = mock(PaymentUseCase.class);
        when(useCase.processPayment(any(PaymentDomain.class))).thenThrow(failure);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(useCase))
                .setControllerAdvice(handler).build();

        mvc.perform(post("/payments/ACC-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1500.00,\"currency\":\"BRL\"}"))
                .andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(code))
                .andExpect(jsonPath("$.errorMessage").value(message))
                .andExpect(jsonPath("$.errorsDetails").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(result -> assertSame(failure, result.getResolvedException()));

        verify(useCase).processPayment(any(PaymentDomain.class));
        verifyNoMoreInteractions(useCase);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> failures() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        new PaymentProviderTimeoutException("internal timeout details", new HttpTimeoutException("deadline")),
                        504, "Não foi possível confirmar o resultado do pagamento."),
                org.junit.jupiter.params.provider.Arguments.of(
                        new PaymentProviderException("internal connection details", new ConnectException("refused")),
                        502, "Não foi possível processar o pagamento devido a uma falha no provedor de pagamentos.")
        );
    }
}
