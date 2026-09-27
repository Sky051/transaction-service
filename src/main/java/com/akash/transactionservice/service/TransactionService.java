package com.akash.transactionservice.service;

import com.akash.transactionservice.client.AccountServiceClient;
import com.akash.transactionservice.dto.TransactionResponse;
import com.akash.transactionservice.dto.TransferRequest;
import com.akash.transactionservice.entity.Transaction;
import com.akash.transactionservice.entity.TransactionStatus;
import com.akash.transactionservice.exception.InvalidTransferException;
import com.akash.transactionservice.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountServiceClient accountServiceClient) {

        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
    }

    @Transactional
    public TransactionResponse transfer(
            TransferRequest request,
            String idempotencyKey) {

        // 1. Check if this request was already processed
        Optional<Transaction> existingTransaction =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {

            Transaction transaction = existingTransaction.get();

            return mapToResponse(transaction);
        }

        // 2. Get sender and receiver IDs
        UUID senderId = request.getSenderAccountId();
        UUID receiverId = request.getReceiverAccountId();

        // 3. Sender and receiver cannot be the same
        if (senderId.equals(receiverId)) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts must be different"
            );
        }

        // 4. Create transaction with PENDING status
        Transaction transaction = Transaction.builder()
                .senderAccountId(senderId)
                .receiverAccountId(receiverId)
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        transaction = transactionRepository.save(transaction);

        UUID transactionId = transaction.getId();

        // 5. Debit sender account
        accountServiceClient.debit(
                senderId,
                request.getAmount(),
                transactionId
        );

        // 6. Credit receiver account
        try {

            accountServiceClient.credit(
                    receiverId,
                    request.getAmount(),
                    transactionId
            );

            // Receiver successfully credited
            transaction.setStatus(TransactionStatus.SUCCESS);

        } catch (Exception creditException) {

            // Receiver credit failed.
            // Compensate the sender's debit.

            UUID compensationOperationId = UUID.randomUUID();

            try {

                accountServiceClient.credit(
                        senderId,
                        request.getAmount(),
                        compensationOperationId
                );

                // Sender successfully received the money back
                transaction.setStatus(TransactionStatus.FAILED);

            } catch (Exception compensationException) {

                // Compensation also failed.
                // Money may still be missing.
                transaction.setStatus(
                        TransactionStatus.COMPENSATION_FAILED
                );
            }
        }

        // 7. Save final transaction status
        transaction = transactionRepository.save(transaction);

        // 8. Return response
        return mapToResponse(transaction);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .senderAccountId(transaction.getSenderAccountId())
                .receiverAccountId(transaction.getReceiverAccountId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}