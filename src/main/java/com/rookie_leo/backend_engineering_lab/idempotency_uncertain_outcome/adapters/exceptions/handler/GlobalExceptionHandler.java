package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.handler;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderException;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.exceptions.PaymentProviderTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Optional;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentProviderTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentProviderTimeoutException(PaymentProviderTimeoutException ex){
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
                new ApiErrorResponse(
                        HttpStatus.GATEWAY_TIMEOUT.value(),
                        "Não foi possível confirmar o resultado do pagamento.",
                        Optional.empty()
                )
        );
    }

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentProviderException(PaymentProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
          new ApiErrorResponse(
                  HttpStatus.BAD_GATEWAY.value(),
                  "Não foi possível processar o pagamento devido a uma falha na comunicação com o provedor.",
                  Optional.empty()
          )
        );
    }
}
