<<<<<<< HEAD
package com.example.accountservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Value("${server.port}")
    private String port;

    @GetMapping
    public List<Map<String, Object>> getAccounts() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "accountNumber", "FB100001",
                        "balance", 5000000
                ),
                Map.of(
                        "id", 2L,
                        "accountNumber", "FB100002",
                        "balance", 10000000
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getAccount(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "accountNumber", "FB100001",
                "balance", 5000000
        );
    }

    @PostMapping
    public Map<String, Object> createAccount(
            @RequestBody Map<String, Object> account) {

        return Map.of(
                "message", "Account created successfully",
                "account", account
        );
    }

    @GetMapping("/info")
    public Map<String, String> getInstanceInfo() {
        return Map.of(
                "service", "ACCOUNT-SERVICE",
                "port", port
        );
    }
=======
package com.example.accountservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Value("${server.port}")
    private String port;

    @GetMapping
    public List<Map<String, Object>> getAccounts() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "accountNumber", "FB100001",
                        "balance", 5000000
                ),
                Map.of(
                        "id", 2L,
                        "accountNumber", "FB100002",
                        "balance", 10000000
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getAccount(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "accountNumber", "FB100001",
                "balance", 5000000
        );
    }

    @PostMapping
    public Map<String, Object> createAccount(
            @RequestBody Map<String, Object> account) {

        return Map.of(
                "message", "Account created successfully",
                "account", account
        );
    }

    @GetMapping("/info")
    public Map<String, String> getInstanceInfo() {
        return Map.of(
                "service", "ACCOUNT-SERVICE",
                "port", port
        );
    }
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
}