package com.akash.transactionservice.repository;

import com.akash.transactionservice.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository
        extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findById(UUID id);

    Optional<Account> findByAccountNumber(String accountNumber);
}