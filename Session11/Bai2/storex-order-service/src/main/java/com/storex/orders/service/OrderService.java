package com.storex.orders.service;

import com.storex.orders.config.KafkaTopicConfig;
import com.storex.orders.dto.OrderRequest;
import com.storex.orders.event.OrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final KafkaSender<String, OrderCreatedEvent> kafkaSender;

    public OrderService(KafkaSender<String, OrderCreatedEvent> kafkaSender) {
        this.kafkaSender = kafkaSender;
    }

    public Mono<String> createOrder(OrderRequest request) {

        // Tạo orderId duy nhất cho đơn hàng
        String orderId = "ORD-" + UUID.randomUUID();

        // Tạo event order.created
        OrderCreatedEvent event = new OrderCreatedEvent(
                "order.created",
                orderId,
                request.customerId(),
                request,
                Instant.now()
        );

        /*
         * BUG-03:
         *
         * Bắt buộc dùng orderId làm Kafka Key.
         *
         * Không chỉ định partition thủ công.
         * Kafka sẽ dựa vào key để xác định partition.
         *
         * Vì vậy các event có cùng orderId
         * sẽ được gửi vào cùng một partition.
         */
        ProducerRecord<String, OrderCreatedEvent> producerRecord =
                new ProducerRecord<>(
                        KafkaTopicConfig.ORDER_EVENTS_TOPIC,
                        orderId,
                        event
                );

        SenderRecord<String, OrderCreatedEvent, String> senderRecord =
                SenderRecord.create(
                        producerRecord,
                        orderId
                );

        return kafkaSender
                .send(Mono.just(senderRecord))

                // Đợi Kafka trả về kết quả gửi message
                .single()

                .flatMap(result -> {

                    // Kafka gửi thất bại
                    if (result.exception() != null) {

                        log.error(
                                "Kafka send failed - orderId={}",
                                orderId,
                                result.exception()
                        );

                        return Mono.error(result.exception());
                    }

                    // Kafka gửi thành công
                    log.info(
                            "Order event sent successfully - orderId={}, topic={}, partition={}, offset={}",
                            orderId,
                            result.recordMetadata().topic(),
                            result.recordMetadata().partition(),
                            result.recordMetadata().offset()
                    );

                    return Mono.just(orderId);
                })

                // Nếu xảy ra exception trong quá trình gửi Kafka
                .doOnError(ex ->
                        log.error(
                                "Cannot publish order.created event - orderId={}",
                                orderId,
                                ex
                        )
                )

                // Chuyển lỗi Kafka thành HTTP 503
                .onErrorMap(ex -> {

                    if (ex instanceof ResponseStatusException) {
                        return ex;
                    }

                    return new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Không thể gửi order event tới Kafka",
                            ex
                    );
                });
    }
}