<<<<<<< HEAD
package com.example.customerservice.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @GetMapping
    public List<Map<String, Object>> getCustomers() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "name", "Nguyen Van An",
                        "email", "an@gmail.com"
                ),
                Map.of(
                        "id", 2L,
                        "name", "Tran Thi Binh",
                        "email", "binh@gmail.com"
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "name", "Nguyen Van An",
                "email", "an@gmail.com"
        );
    }

    @PostMapping
    public Map<String, Object> createCustomer(
            @RequestBody Map<String, Object> customer) {

        return Map.of(
                "message", "Customer created successfully",
                "customer", customer
        );
    }
=======
package com.example.customerservice.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @GetMapping
    public List<Map<String, Object>> getCustomers() {
        return List.of(
                Map.of(
                        "id", 1L,
                        "name", "Nguyen Van An",
                        "email", "an@gmail.com"
                ),
                Map.of(
                        "id", 2L,
                        "name", "Tran Thi Binh",
                        "email", "binh@gmail.com"
                )
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "name", "Nguyen Van An",
                "email", "an@gmail.com"
        );
    }

    @PostMapping
    public Map<String, Object> createCustomer(
            @RequestBody Map<String, Object> customer) {

        return Map.of(
                "message", "Customer created successfully",
                "customer", customer
        );
    }
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
}