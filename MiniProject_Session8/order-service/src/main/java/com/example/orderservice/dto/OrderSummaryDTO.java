package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryDTO {
    private Long orderId;
    private String orderCode;
    private String status;
    private BigDecimal totalAmount;
    private String note;
    private LocalDateTime createdAt;
    
    // Khách hàng từ user-service (Database riêng)
    private UserDTO customer;
    
    // Danh sách sản phẩm từ product-service (Database riêng)
    private List<OrderItemSummaryDTO> items;
    
    // Thông tin bản sao xử lý (đáp ứng minh chứng Load Balancing)
    private String handledByInstance;
}
