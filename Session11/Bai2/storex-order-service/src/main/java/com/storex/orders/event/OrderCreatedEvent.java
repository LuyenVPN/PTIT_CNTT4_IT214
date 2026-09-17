package com.storex.orders.event;

import com.storex.orders.dto.OrderRequest;

import java.time.Instant;

public record OrderCreatedEvent(
        String eventType,
        String orderId,
        String customerId,
        OrderRequest order,
        Instant occurredAt
) {
}
