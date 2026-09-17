package com.storex.orders.controller;

import com.storex.orders.dto.OrderRequest;
import com.storex.orders.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<String> createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
}
