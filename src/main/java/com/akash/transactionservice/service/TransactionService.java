package com.akash.transactionservice.service;

import com.akash.transactionservice.dto.TransactionResponse;
import com.akash.transactionservice.dto.TransferRequest;
import com.akash.transactionservice.entity.Account;
import com.akash.transactionservice.entity.Transaction;
import com.akash.transactionservice.entity.TransactionStatus;
import com.akash.transactionservice.repository.AccountRepository;
import com.akash.transactionservice.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.akash.transactionservice.exception.AccountNotFoundException;
import com.akash.transactionservice.exception.InsufficientBalanceException;

import com.akash.transactionservice.exception.InvalidTransferException;

import java.util.UUID;

import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
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

            return TransactionResponse.builder()
                    .transactionId(transaction.getId())
                    .senderAccountId(transaction.getSenderAccountId())
                    .receiverAccountId(transaction.getReceiverAccountId())
                    .amount(transaction.getAmount())
                    .status(transaction.getStatus())
                    .createdAt(transaction.getCreatedAt())
                    .build();
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

// 4. Determine a consistent locking order
        UUID firstId;
        UUID secondId;

        if (senderId.compareTo(receiverId) < 0) {
            firstId = senderId;
            secondId = receiverId;
        } else {
            firstId = receiverId;
            secondId = senderId;
        }

// 5. Lock the accounts in deterministic order
        Account firstAccount = accountRepository
                .findById(firstId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + firstId
                        ));

        Account secondAccount = accountRepository
                .findById(secondId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + secondId
                        ));

// 6. Identify which account is sender and receiver
        Account sender;
        Account receiver;

        if (senderId.equals(firstAccount.getId())) {
            sender = firstAccount;
            receiver = secondAccount;
        } else {
            sender = secondAccount;
            receiver = firstAccount;
        }

        // 4. Check balance
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // 5. Debit sender
        sender.setBalance(
                sender.getBalance().subtract(request.getAmount())
        );

        // 6. Credit receiver
        receiver.setBalance(
                receiver.getBalance().add(request.getAmount())
        );

        // 7. Create transaction
        Transaction transaction = Transaction.builder()
                .senderAccountId(sender.getId())
                .receiverAccountId(receiver.getId())
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .build();

        // 8. Save transaction
        transactionRepository.save(transaction);

        // 9. Return response
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