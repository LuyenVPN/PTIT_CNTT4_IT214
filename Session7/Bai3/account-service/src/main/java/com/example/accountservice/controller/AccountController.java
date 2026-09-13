package com.example.accountservice.controller;

import com.example.accountservice.dto.AmountRequest;
import com.example.accountservice.entity.Account;
import com.example.accountservice.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Value("${server.port}")
    private String port;

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(accountService.getAll());
    }

    @GetMapping("/info")
    public Map<String, String> getInstanceInfo() {
        return Map.of(
                "service", "ACCOUNT-SERVICE",
                "port", port
        );
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(
            @PathVariable String accountNumber) {

        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestBody Account account) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(account));
    }

    @PutMapping("/{accountNumber}/debit")
    public ResponseEntity<Account> debit(
            @PathVariable String accountNumber,
            @RequestBody com.example.accountservice.dto.AmountRequest request) {

        return ResponseEntity.ok(
                accountService.debit(accountNumber, request.getAmount())
        );
    }

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<Account> credit(
            @PathVariable String accountNumber,
            @RequestBody com.example.accountservice.dto.AmountRequest request) {

        return ResponseEntity.ok(
                accountService.credit(accountNumber, request.getAmount())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", "FAILED",
                        "message", e.getMessage()
                )
        );
    }
}
