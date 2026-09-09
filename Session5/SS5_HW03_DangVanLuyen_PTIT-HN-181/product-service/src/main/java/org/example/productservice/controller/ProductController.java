package org.example.productservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Value("${server.port}")
    private String port;

    @GetMapping
    public ResponseEntity<String> getAllProducts() {
        log.info(">>> [PRODUCT-SERVICE] Handled by instance on PORT: {}", port);
        return ResponseEntity.ok("Response from product-service on port: " + port);
    }
}
