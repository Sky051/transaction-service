package com.akash.transactionservice.dto;

import com.akash.transactionservice.entity.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponse {

    private UUID transactionId;

    private UUID senderAccountId;

    private UUID receiverAccountId;

    private BigDecimal amount;

    private TransactionStatus status;

    private LocalDateTime createdAt;
}