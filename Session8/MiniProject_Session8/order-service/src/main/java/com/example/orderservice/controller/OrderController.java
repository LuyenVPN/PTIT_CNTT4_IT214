package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderSummaryDTO;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderAggregatorService orderAggregatorService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint giải quyết yêu cầu đặc biệt ("khoai nhất") của khách hàng:
     * Tổng hợp thông tin từ order-service (DB order), user-service (DB user)
     * và product-service (DB product) trong 1 lần tải duy nhất.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<OrderSummaryDTO> getOrderSummary(@PathVariable Long id) {
        return ResponseEntity.ok(orderAggregatorService.getOrderSummary(id));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderRepository.save(order));
    }
}
