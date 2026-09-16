<<<<<<< HEAD
package com.example.transactionservice.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @GetMapping
    public List<Map<String, Object>> getTransactions() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "fromAccount", "FB100001",
                        "toAccount", "FB100002",
                        "amount", 1000000
                ),
                Map.of(
                        "id", 2L,
                        "fromAccount", "FB100002",
                        "toAccount", "FB100001",
                        "amount", 500000
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getTransaction(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "fromAccount", "FB100001",
                "toAccount", "FB100002",
                "amount", 1000000
        );
    }

    @PostMapping
    public Map<String, Object> createTransaction(
            @RequestBody Map<String, Object> transaction) {

        return Map.of(
                "message", "Transaction created successfully",
                "transaction", transaction
        );
    }
}
=======
package com.example.transactionservice.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @GetMapping
    public List<Map<String, Object>> getTransactions() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "fromAccount", "FB100001",
                        "toAccount", "FB100002",
                        "amount", 1000000
                ),
                Map.of(
                        "id", 2L,
                        "fromAccount", "FB100002",
                        "toAccount", "FB100001",
                        "amount", 500000
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getTransaction(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "fromAccount", "FB100001",
                "toAccount", "FB100002",
                "amount", 1000000
        );
    }

    @PostMapping
    public Map<String, Object> createTransaction(
            @RequestBody Map<String, Object> transaction) {

        return Map.of(
                "message", "Transaction created successfully",
                "transaction", transaction
        );
    }
}
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
