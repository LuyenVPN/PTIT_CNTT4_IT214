package com.example.orderservice.config;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {
        if (orderRepository.count() == 0) {
            // Đơn hàng số 1: User 1 mua Bàn phím cơ (SP 1) và Chuột Logitech (SP 2)
            Order order1 = Order.builder()
                    .orderCode("ORD-202609-001")
                    .userId(1L)
                    .status("CONFIRMED")
                    .note("Giao hàng giờ hành chính, gọi trước 15 phút")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .totalAmount(new BigDecimal("4640000"))
                    .items(new ArrayList<>())
                    .build();

            order1.addItem(OrderItem.builder()
                    .productId(1L)
                    .quantity(1)
                    .unitPrice(new BigDecimal("2190000"))
                    .build());

            order1.addItem(OrderItem.builder()
                    .productId(2L)
                    .quantity(1)
                    .unitPrice(new BigDecimal("2450000"))
                    .build());

            // Đơn hàng số 2: User 2 mua Màn hình Dell (SP 3) và Tai nghe Sony (SP 4)
            Order order2 = Order.builder()
                    .orderCode("ORD-202609-002")
                    .userId(2L)
                    .status("PROCESSING")
                    .note("Đóng gói cẩn thận có tem bảo hành chính hãng")
                    .createdAt(LocalDateTime.now().minusHours(4))
                    .totalAmount(new BigDecimal("21490000"))
                    .items(new ArrayList<>())
                    .build();

            order2.addItem(OrderItem.builder()
                    .productId(3L)
                    .quantity(1)
                    .unitPrice(new BigDecimal("13500000"))
                    .build());

            order2.addItem(OrderItem.builder()
                    .productId(4L)
                    .quantity(1)
                    .unitPrice(new BigDecimal("7990000"))
                    .build());

            orderRepository.save(order1);
            orderRepository.save(order2);
        }
    }
}
