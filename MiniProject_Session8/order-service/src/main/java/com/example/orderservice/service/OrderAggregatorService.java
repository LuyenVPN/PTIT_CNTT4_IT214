package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.OrderItemSummaryDTO;
import com.example.orderservice.dto.OrderSummaryDTO;
import com.example.orderservice.dto.ProductDTO;
import com.example.orderservice.dto.UserDTO;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAggregatorService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final Environment environment;

    public OrderSummaryDTO getOrderSummary(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String serverPort = environment.getProperty("local.server.port", environment.getProperty("server.port", "unknown"));
        String instanceInfo = "order-service:port=" + serverPort;

        // 1. Gọi đồng bộ sang user-service qua OpenFeign (phát hiện dịch vụ qua Eureka)
        UserDTO customer;
        try {
            customer = userClient.getUserById(order.getUserId());
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin khách hàng id {}: {}", order.getUserId(), e.getMessage());
            customer = UserDTO.builder()
                    .id(order.getUserId())
                    .fullName("Khách hàng #" + order.getUserId())
                    .email("N/A")
                    .build();
        }

        // 2. Gọi đồng bộ sang product-service qua OpenFeign cho từng item
        List<OrderItemSummaryDTO> itemSummaries = new ArrayList<>();
        if (order.getItems() != null) {
            for (var item : order.getItems()) {
                String productName = "Sản phẩm #" + item.getProductId();
                String category = "N/A";
                try {
                    ProductDTO product = productClient.getProductById(item.getProductId());
                    if (product != null) {
                        productName = product.getName();
                        category = product.getCategory();
                    }
                } catch (Exception e) {
                    log.warn("Không thể lấy thông tin sản phẩm id {}: {}", item.getProductId(), e.getMessage());
                }

                BigDecimal subTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemSummaries.add(OrderItemSummaryDTO.builder()
                        .orderItemId(item.getId())
                        .productId(item.getProductId())
                        .productName(productName)
                        .productCategory(category)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subTotal(subTotal)
                        .build());
            }
        }

        // 3. Đóng gói DTO tổng hợp 1 màn hình duy nhất
        return OrderSummaryDTO.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .customer(customer)
                .items(itemSummaries)
                .handledByInstance(instanceInfo)
                .build();
    }
}
