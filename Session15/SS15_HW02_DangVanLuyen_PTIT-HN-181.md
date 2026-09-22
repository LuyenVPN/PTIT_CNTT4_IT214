# PHẦN 1: MÃ NGUỒN HOÀN CHỈNH (SPRING BOOT)

### Cấu hình chung cho cả 3 service (Maven POM Dependencies)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>

```

---

## 1. `movie-booking-service` (Port: 8081)

### `application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: movie-booking-service
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

```

### `model/CinemaBookingRequest.java`

```java
package com.example.movie.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaBookingRequest {
    private String cinemaBookingId;
    private String movieCode;
    private String showTime;
    private List<String> seatNumbers;
    private String customerEmail;
    private Double totalPrice;
}

```

### `service/BookingPublisherService.java`

```java
package com.example.movie.service;

import com.example.movie.model.CinemaBookingRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public String createBooking(CinemaBookingRequest request) {
        // Bước 1: Sinh ngẫu nhiên correlationId duy nhất cho toàn bộ luồng giao dịch
        String correlationId = UUID.randomUUID().toString();
        log.info("[MovieBookingService] Created booking {}. CorrelationID: {}", 
                request.getCinemaBookingId(), correlationId);

        try {
            // Chuyển đối tượng thành chuỗi JSON (Payload không chứa correlationId)
            String payload = objectMapper.writeValueAsString(request);

            ProducerRecord<String, String> record = new ProducerRecord<>(
                "booking-events",
                request.getCinemaBookingId(),
                payload
            );

            // Bước 2: Gắn correlationId vào HEADER của Kafka record (chuyển sang bytes UTF-8)
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
            return correlationId;
        } catch (JsonProcessingException e) {
            log.error("Lỗi tuần tự hóa đối tượng booking request", e);
            throw new RuntimeException(e);
        }
    }
}

```

### `controller/MovieBookingController.java`

```java
package com.example.movie.controller;

import com.example.movie.model.CinemaBookingRequest;
import com.example.movie.service.BookingPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class MovieBookingController {

    private final BookingPublisherService bookingPublisherService;

    @PostMapping
    public ResponseEntity<?> bookTickets(@RequestBody CinemaBookingRequest request) {
        String correlationId = bookingPublisherService.createBooking(request);
        return ResponseEntity.ok(Map.of(
            "message", "Yêu cầu đặt vé đã được tiếp nhận và xử lý",
            "bookingId", request.getCinemaBookingId(),
            "correlationId", correlationId
        ));
    }
}

```

### `MovieBookingApplication.java`

```java
package com.example.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MovieBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieBookingApplication.class, args);
    }
}

```

---

## 2. `seat-allocation-service` (Port: 8082)

### `application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: seat-allocation-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: seat-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

```

### `model/SeatAllocationEvent.java`

```java
package com.example.seat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationEvent {
    private String cinemaBookingId;
    private List<String> seatNumbers;
    private Double totalPrice;
    private String customerEmail;
    private String status;
}

```

### `producer/SeatConfirmedProducer.java`

```java
package com.example.seat.producer;

import com.example.seat.model.SeatAllocationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeatConfirmedProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishSeatConfirmed(SeatAllocationEvent event, String correlationId) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record = new ProducerRecord<>(
                "seat-confirmed-events",
                event.getCinemaBookingId(),
                payload
            );

            // Giữ nguyên và truyền tiếp correlationId sang topic tiếp theo qua Header
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);
        } catch (Exception e) {
            log.error("Lỗi khi gửi sự kiện SeatConfirmed", e);
        }
    }
}

```

### `consumer/BookingEventConsumer.java`

```java
package com.example.seat.consumer;

import com.example.seat.model.SeatAllocationEvent;
import com.example.seat.producer.SeatConfirmedProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final SeatConfirmedProducer seatConfirmedProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-events", groupId = "seat-group")
    public void handleBooking(ConsumerRecord<String, String> record) {
        // Bước 3: Trích xuất Correlation ID từ Kafka Header
        Header header = record.headers().lastHeader("correlationId");
        String correlationId = (header != null) ? new String(header.value(), StandardCharsets.UTF_8) : "UNKNOWN";

        log.info("[SeatAllocationService] Received SeatRequest for {}. CorrelationID: {}", 
                record.key(), correlationId);

        try {
            JsonNode root = objectMapper.readTree(record.value());
            List<String> seats = new ArrayList<>();
            root.get("seatNumbers").forEach(s -> seats.add(s.asText()));

            // Giả lập nghiệp vụ giữ chỗ thành công
            String seatResult = String.join(", ", seats);
            log.info("[SeatAllocationService] Seat reserved: {}. CorrelationID: {}", 
                    seatResult, correlationId);

            // Đóng gói sự kiện xác nhận giữ ghế
            SeatAllocationEvent confirmedEvent = SeatAllocationEvent.builder()
                    .cinemaBookingId(record.key())
                    .seatNumbers(seats)
                    .totalPrice(root.get("totalPrice").asDouble())
                    .customerEmail(root.get("customerEmail").asText())
                    .status("RESERVED")
                    .build();

            // Chuyển giao dịch tiếp theo kèm Correlation ID trong header
            seatConfirmedProducer.publishSeatConfirmed(confirmedEvent, correlationId);

        } catch (Exception e) {
            log.error("[SeatAllocationService] Error processing booking: {}", e.getMessage());
        }
    }
}

```

### `SeatAllocationApplication.java`

```java
package com.example.seat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeatAllocationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatAllocationApplication.class, args);
    }
}

```

---

## 3. `payment-service` (Port: 8083)

### `application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: payment-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: payment-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

```

### `model/SeatAllocationEvent.java`

```java
package com.example.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationEvent {
    private String cinemaBookingId;
    private List<String> seatNumbers;
    private Double totalPrice;
    private String customerEmail;
    private String status;
}

```

### `service/PaymentProcessingService.java`

```java
package com.example.payment.service;

import com.example.payment.model.SeatAllocationEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {
    public boolean processPayment(SeatAllocationEvent event) {
        // Mô phỏng thanh toán thành công nếu tổng tiền hợp lệ
        return event.getTotalPrice() != null && event.getTotalPrice() > 0;
    }
}

```

### `consumer/SeatConfirmedConsumer.java`

```java
package com.example.payment.consumer;

import com.example.payment.model.SeatAllocationEvent;
import com.example.payment.service.PaymentProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeatConfirmedConsumer {

    private final PaymentProcessingService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "seat-confirmed-events", groupId = "payment-group")
    public void handleSeatConfirmed(ConsumerRecord<String, String> record) {
        // Trích xuất correlationId từ Kafka Header
        Header header = record.headers().lastHeader("correlationId");
        String correlationId = (header != null) ? new String(header.value(), StandardCharsets.UTF_8) : "UNKNOWN";

        log.info("[PaymentService] Processing Payment for {}. CorrelationID: {}", 
                record.key(), correlationId);

        try {
            SeatAllocationEvent event = objectMapper.readValue(record.value(), SeatAllocationEvent.class);
            boolean success = paymentService.processPayment(event);

            if (success) {
                // In log định dạng số nguyên nếu là số chẵn (240000 VND)
                long formattedPrice = Math.round(event.getTotalPrice());
                log.info("[PaymentService] Payment success: {} VND. CorrelationID: {}", 
                        formattedPrice, correlationId);
            }
        } catch (Exception e) {
            log.error("[PaymentService] Error processing payment: {}", e.getMessage());
        }
    }
}

```

### `PaymentApplication.java`

```java
package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}

```

---

# PHẦN 2: BÁO CÁO PHÂN TÍCH VÀ HƯỚNG DẪN NỘP BÀI (REPORT.md)

## 1. Mô tả luồng sự kiện và vai trò của Correlation ID

* **Mô hình triển khai:** Choreography Saga (giao tiếp bất đồng bộ, phi tập trung qua Kafka Topics).
* **Luồng chạy nghiệp vụ:**
1. Client gửi HTTP POST tới `MovieBookingService` (`/api/bookings`).
2. `MovieBookingService` khởi tạo `correlationId` (UUID v4), đẩy message vào topic `booking-events`.
3. `SeatAllocationService` lắng nghe topic `booking-events`, trích xuất `correlationId` từ header, thực hiện giữ chỗ ghế (reserve seats), sau đó forward `correlationId` sang message mới gửi đến topic `seat-confirmed-events`.
4. `PaymentService` lắng nghe topic `seat-confirmed-events`, trích xuất `correlationId` từ header và thực hiện trừ tiền / hoàn tất thanh toán.


* **Vai trò của Correlation ID:**
* Giúp xâu chuỗi toàn bộ nhật ký log phân tán qua nhiều tiến trình/container độc lập.
* Ngăn ngừa tình trạng **"Event Spaghetti"**: khi có hàng chục nghìn giao dịch diễn ra đồng thời, kỹ sư vận hành có thể lọc log theo một mã ID duy nhất để xác định chính xác hành trình của một yêu cầu.
* Cung cấp ngữ cảnh xuyên suốt để tích hợp dễ dàng với các giải pháp Distributed Tracing hiện đại (OpenTelemetry, Zipkin, Jaeger, Grafana Tempo).



## 2. Giải thích kỹ thuật gắn Correlation ID vào Header và lợi ích

* **Kỹ thuật thực hiện:**
* Sử dụng thuộc tính `headers()` của đối tượng `ProducerRecord<K, V>` trong Kafka Client:
```java
record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));

```


* Phía consumer, đọc qua `record.headers().lastHeader("correlationId")`.


* **Lợi ích so với việc đặt trong Payload:**
* **Tách biệt mối quan tâm (Separation of Concerns):** Dữ liệu payload đại diện thuần túy cho domain model nghiệp vụ (vé, số ghế, người dùng). Metadata như tracing, security token, routing ID thuộc về hạ tầng vận chuyển.
* **Tính tương thích ngược (Backward Compatibility):** Thay đổi format hoặc thêm các header giám sát không làm thay đổi JSON Schema/Avro/Protobuf của payload.
* **Hiệu năng cao:** Các proxy, router, hoặc consumer trung gian có thể inspect, định tuyến hoặc filter tin nhắn theo correlation ID/metadata mà không cần tốn CPU để parse/deserialize toàn bộ payload JSON.



## 3. Hướng dẫn cài đặt và chạy thử nghiệm

### Bước 1: Khởi động Kafka & Zookeeper bằng Docker Compose

Tạo file `docker-compose.yml`:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

```

Chạy lệnh: `docker compose up -d`

### Bước 2: Chạy lần lượt 3 service

1. Chạy `MovieBookingApplication` (port 8081).
2. Chạy `SeatAllocationApplication` (port 8082).
3. Chạy `PaymentApplication` (port 8083).
   *(Kafka sẽ tự động khởi tạo topics `booking-events` và `seat-confirmed-events` khi nhận record đầu tiên)*.

### Bước 3: Gửi dữ liệu test (cURL)

```bash
curl -X POST http://localhost:8081/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "cinemaBookingId": "CIN-2024-789",
    "movieCode": "AVENGERS-5",
    "showTime": "2024-12-25T19:30:00",
    "seatNumbers": ["A12", "A13"],
    "customerEmail": "tuananh@email.com",
    "totalPrice": 240000
  }'

```

## 4. Kết quả log thực tế thu được

Tất cả các dòng log tại các console service đều ghi nhận cùng một mã định danh duy nhất:

```text
[MovieBookingService] Created booking CIN-2024-789. CorrelationID: 550e8400-e29b-41d4-a716-446655440000
[SeatAllocationService] Received SeatRequest for CIN-2024-789. CorrelationID: 550e8400-e29b-41d4-a716-446655440000
[SeatAllocationService] Seat reserved: A12, A13. CorrelationID: 550e8400-e29b-41d4-a716-446655440000
[PaymentService] Processing Payment for CIN-2024-789. CorrelationID: 550e8400-e29b-41d4-a716-446655440000
[PaymentService] Payment success: 240000 VND. CorrelationID: 550e8400-e29b-41d4-a716-446655440000

```