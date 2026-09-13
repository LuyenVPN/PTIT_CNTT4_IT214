package com.example.transactionservice.controller;

import com.example.transactionservice.dto.TransferRequest;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(
                transactionService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                transactionService.getById(id)
        );
    }

    // Yêu cầu 3:
    // POST /api/transactions/transfer
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            @RequestBody TransferRequest request) {

        return ResponseEntity.ok(
                transactionService.transfer(request)
        );
    }
}
