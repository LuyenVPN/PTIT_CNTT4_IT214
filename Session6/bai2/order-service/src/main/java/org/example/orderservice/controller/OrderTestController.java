package org.example.orderservice.controller;

import org.example.orderservice.client.ProductClient;
import org.example.orderservice.client.UserClient;
import org.example.orderservice.dto.ProductInfo;
import org.example.orderservice.dto.UserInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/test")
public class OrderTestController {

    private final ProductClient productClient;
    private final UserClient userClient;

    public OrderTestController(ProductClient productClient, UserClient userClient) {
        this.productClient = productClient;
        this.userClient = userClient;
    }

    @GetMapping("/products/{id}")
    public ProductInfo testGetProduct(@PathVariable Long id) {
        return productClient.getById(id);
    }

    @GetMapping("/products")
    public List<ProductInfo> testGetAllProducts() {
        return productClient.getAll();
    }

    @GetMapping("/users/{id}")
    public UserInfo testGetUser(@PathVariable Long id) {
        return userClient.getUserById(id);
    }
}
