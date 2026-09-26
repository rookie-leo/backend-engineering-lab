package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web;

import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto.PaymentRequest;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.input.web.dto.PaymentResponse;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.PaymentDomain;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.domain.model.enums.PaymentStatus;
import com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.port.input.PaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @Autowired
    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping("/{accountId}")
    public ResponseEntity<PaymentResponse> payment(
            @PathVariable("accountId") String accountId,
            @RequestBody @Valid PaymentRequest request
    ) {
        var domain = new PaymentDomain(
                accountId,
                request.amount(),
                request.currency(),
                PaymentStatus.RECEIVED
        );

        domain = paymentUseCase.processPayment(domain);

        var response = new PaymentResponse(
                domain.status().toString()
        );

        return ResponseEntity.ok(response);
    }

}
