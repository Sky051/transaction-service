package com.akash.transactionservice.controller;

import com.akash.transactionservice.entity.Account;
import com.akash.transactionservice.entity.AccountStatus;
import com.akash.transactionservice.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal balance) {

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .build();

        return ResponseEntity.ok(accountRepository.save(account));
    }
}