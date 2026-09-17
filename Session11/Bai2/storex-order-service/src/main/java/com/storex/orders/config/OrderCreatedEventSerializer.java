package com.storex.orders.config;

import com.storex.orders.event.OrderCreatedEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

public class OrderCreatedEventSerializer
        implements Serializer<OrderCreatedEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, OrderCreatedEvent data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException(
                    "Không thể serialize OrderCreatedEvent",
                    e
            );
        }
    }

    @Override
    public void close() {
        // Không có tài nguyên cần đóng.
    }
}