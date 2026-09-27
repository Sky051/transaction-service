package com.akash.transactionservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${account-service.url}") String accountServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(accountServiceUrl)
                .build();
    }

    public void debit(
            UUID accountId,
            BigDecimal amount,
            UUID operationId
    ) {

        restClient.patch()
                .uri("/api/v1/accounts/{id}/debit", accountId)
                .body(new BalanceOperationRequest(
                        amount,
                        operationId
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void credit(UUID accountId, BigDecimal amount, UUID operationId) {

        restClient.patch()
                .uri("/api/v1/accounts/{id}/credit", accountId)
                .body(new BalanceOperationRequest(amount,operationId))
                .retrieve()
                .toBodilessEntity();
    }

    private record BalanceOperationRequest(
            BigDecimal amount,
            UUID operationId
    ) {
    }
}