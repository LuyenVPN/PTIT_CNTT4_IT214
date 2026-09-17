package com.storex.orders.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        String customerId,
        List<OrderItem> items,
        BigDecimal totalAmount
) {
    public record OrderItem(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }
}
