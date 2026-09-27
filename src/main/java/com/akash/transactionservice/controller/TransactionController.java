package com.akash.transactionservice.controller;

import com.akash.transactionservice.dto.TransactionResponse;
import com.akash.transactionservice.dto.TransferRequest;
import com.akash.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.akash.transactionservice.client.AccountServiceClient;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountServiceClient accountServiceClient;

    public TransactionController(TransactionService transactionService,
                                 AccountServiceClient accountServiceClient
                                 )
    {
        this.transactionService = transactionService;
        this.accountServiceClient = accountServiceClient;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        TransactionResponse response =
                transactionService.transfer(request, idempotencyKey);

        return ResponseEntity.ok(response);
    }

}