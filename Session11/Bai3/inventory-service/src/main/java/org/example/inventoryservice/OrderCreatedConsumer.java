package org.example.inventoryservice;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    @KafkaListener(
            topics = "${storex.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String orderId = record.key();
        String event = record.value();

        System.out.printf(
                "[INVENTORY] order.created received | partition=%d | offset=%d | key(orderId)=%s | payload=%s%n",
                record.partition(),
                record.offset(),
                orderId,
                event
        );

        // Business processing placeholder:
        // inventoryService.deductStock(orderId, event);
    }
}
