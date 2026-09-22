# PHẦN 1: MÃ NGUỒN

### Cấu hình Dependency chung (áp dụng cho cả 3 service)

Trong file `pom.xml` của từng service:

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

## 1. `concert-booking-service` (Port: 8081)

### `application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: concert-booking-service
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

```

### `model/ConcertBookingEvent.java`

```java
package com.example.concert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertBookingEvent {
    private String correlationId;
    private String concertCode;
    private String customerEmail;
    private Integer ticketQuantity;
}

```

### `service/BookingPublisherService.java`

```java
package com.example.concert.service;

import com.example.concert.model.ConcertBookingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_CONCERT_EVENTS = "concert-events";

    public void publishBookingEvent(ConcertBookingEvent event) {
        try {
            // Chuyển đối tượng sang chuỗi JSON chứa sẵn correlationId trong payload
            String payload = objectMapper.writeValueAsString(event);
            
            log.info("[BookingService] Publishing concert booking event with correlationId: {} to topic: {}", 
                     event.getCorrelationId(), TOPIC_CONCERT_EVENTS);
            
            // Gửi message lên topic với key là correlationId
            kafkaTemplate.send(TOPIC_CONCERT_EVENTS, event.getCorrelationId(), payload);
        } catch (JsonProcessingException e) {
            log.error("[BookingService] Error serializing event: {}", e.getMessage());
            throw new RuntimeException("Serialization error", e);
        }
    }
}

```

### `controller/ConcertBookingController.java`

```java
package com.example.concert.controller;

import com.example.concert.model.ConcertBookingEvent;
import com.example.concert.service.BookingPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/concerts/bookings")
@RequiredArgsConstructor
public class ConcertBookingController {

    private final BookingPublisherService bookingPublisherService;

    @PostMapping
    public ResponseEntity<?> createConcertBooking(@RequestBody ConcertBookingEvent request) {
        bookingPublisherService.publishBookingEvent(request);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Yêu cầu đặt vé concert đã được gửi vào hàng đợi xử lý",
            "correlationId", request.getCorrelationId()
        ));
    }
}

```

### `ConcertBookingApplication.java`

```java
package com.example.concert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConcertBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConcertBookingApplication.class, args);
    }
}

```

---

## 2. `seat-assignment-service` (Port: 8082)

### `application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: seat-assignment-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: seat-assignment-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

```

### `model/ConcertBookingEvent.java`

```java
package com.example.seat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcertBookingEvent {
    private String correlationId;
    private String concertCode;
    private String customerEmail;
    private Integer ticketQuantity;
}

```

### `model/SeatReservedEvent.java`

```java
package com.example.seat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservedEvent {
    private String correlationId;
    private String customerEmail;
    private Integer ticketQuantity;
    private String status;
}

```

### `service/SeatAssignmentService.java`

```java
package com.example.seat.service;

import com.example.seat.model.ConcertBookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeatAssignmentService {

    public void reserveSeat(ConcertBookingEvent event) {
        // Mô phỏng lưu thông tin giữ ghế vào cơ sở dữ liệu
        // Giả lập xử lý thành công
    }
}

```

### `producer/SeatEventProducer.java`

```java
package com.example.seat.producer;

import com.example.seat.model.SeatReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeatEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_SEAT_EVENTS = "seat-events";

    public void publishSeatReserved(SeatReservedEvent seatEvent) {
        try {
            String payload = objectMapper.writeValueAsString(seatEvent);
            kafkaTemplate.send(TOPIC_SEAT_EVENTS, seatEvent.getCorrelationId(), payload);
        } catch (JsonProcessingException e) {
            log.error("[SeatService] Lỗi serialize SeatReservedEvent", e);
        }
    }
}

```

### `consumer/ConcertBookingConsumer.java`

```java
package com.example.seat.consumer;

import com.example.seat.model.ConcertBookingEvent;
import com.example.seat.model.SeatReservedEvent;
import com.example.seat.producer.SeatEventProducer;
import com.example.seat.service.SeatAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConcertBookingConsumer {

    private final SeatAssignmentService seatService;
    private final SeatEventProducer seatEventProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "concert-events", groupId = "seat-assignment-group")
    public void handleConcertBooking(ConsumerRecord<String, String> record) {
        try {
            ConcertBookingEvent event = objectMapper.readValue(record.value(), ConcertBookingEvent.class);
            String correlationId = event.getCorrelationId();

            // Log 1: Nhận event
            log.info("[SeatService] Received event with correlationId: {}", correlationId);

            // Mô phỏng giữ chỗ ghế
            seatService.reserveSeat(event);

            // Log 2: Giữ ghế thành công
            log.info("[SeatService] Seat reserved successfully for correlationId: {}", correlationId);

            // Gán y nguyên correlationId vào event tiếp theo
            SeatReservedEvent seatEvent = SeatReservedEvent.builder()
                    .correlationId(correlationId)
                    .customerEmail(event.getCustomerEmail())
                    .ticketQuantity(event.getTicketQuantity())
                    .status("RESERVED")
                    .build();

            seatEventProducer.publishSeatReserved(seatEvent);

            // Log 3: Publish event sang topic seat-events
            log.info("[SeatService] Publishing SeatReserved event with correlationId: {} to topic: seat-events", correlationId);

        } catch (Exception e) {
            log.error("[SeatService] Error processing concert booking: {}", e.getMessage());
        }
    }
}

```

### `SeatAssignmentApplication.java`

```java
package com.example.seat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeatAssignmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatAssignmentApplication.class, args);
    }
}

```

---

## 3. `notification-service` (Port: 8083)

### `application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: notification-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

```

### `model/SeatReservedEvent.java`

```java
package com.example.notification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservedEvent {
    private String correlationId;
    private String customerEmail;
    private Integer ticketQuantity;
    private String status;
}

```

### `service/NotificationService.java`

```java
package com.example.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    public void sendEmailConfirmation(String email, String correlationId) {
        // Mô phỏng logic gửi email SMTP hoặc qua dịch vụ bên thứ 3 (SendGrid/SES)
    }
}

```

### `consumer/SeatReservedConsumer.java`

```java
package com.example.notification.consumer;

import com.example.notification.model.SeatReservedEvent;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeatReservedConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "seat-events", groupId = "notification-group")
    public void handleSeatReserved(ConsumerRecord<String, String> record) {
        try {
            SeatReservedEvent event = objectMapper.readValue(record.value(), SeatReservedEvent.class);
            String correlationId = event.getCorrelationId();
            String email = event.getCustomerEmail();

            // Log khớp định dạng yêu cầu
            log.info("[NotifyService] Received confirmation for correlationId: {} - Sending email to {}", correlationId, email);

            // Tiến hành gửi thông báo
            notificationService.sendEmailConfirmation(email, correlationId);

        } catch (Exception e) {
            log.error("[NotifyService] Error processing seat reserved event: {}", e.getMessage());
        }
    }
}

```

### `NotificationApplication.java`

```java
package com.example.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}

```

---

# PHẦN 2: BÁO CÁO PHÂN TÍCH

## 1. Mô tả luồng sự kiện Choreography Saga và vai trò của từng service

* **Mô hình Choreography Saga:**
* Khác với Orchestration Saga (cần một coordinator điều phối tập trung), Choreography Saga hoạt động theo cơ chế **sự kiện tự điều phối (Event-Driven)**.
* Mỗi dịch vụ tự phản ứng độc lập khi tiêu thụ sự kiện từ topic của mình, thực hiện nghiệp vụ cục bộ, sau đó xuất ra sự kiện mới để kích hoạt bước tiếp theo mà không gọi API trực tiếp (REST/gRPC/FeignClient) giữa các bên.


* **Vai trò các Service:**
1. **`ConcertBookingService` (Producer khởi tạo):** Đóng vai trò điểm tiếp nhận đơn đặt vé. Dịch vụ này nhận dữ liệu yêu cầu, đóng gói thành `ConcertBookingEvent` mang mã định danh `correlationId` và phát sự kiện vào topic `concert-events`.
2. **`SeatAssignmentService` (Consumer & Producer trung gian):** Lắng nghe topic `concert-events`, thực hiện nghiệp vụ giữ chỗ và cập nhật trạng thái ghế. Khi hoàn tất, dịch vụ tạo sự kiện `SeatReservedEvent` với cùng `correlationId` và bắn lên topic `seat-events`.
3. **`NotificationService` (Consumer đích):** Lắng nghe topic `seat-events`, trích xuất thông tin khách hàng và `correlationId` để gửi email xác nhận hoàn tất giao dịch.



```
[Client] 
   │ HTTP POST
   ▼
[ConcertBookingService] ──(concert-events)──► [SeatAssignmentService] ──(seat-events)──► [NotificationService]

```

## 2. Cách Correlation ID được truyền xuyên suốt

* `correlationId` (`CONCERT-2024-999`) đóng vai trò là khoá truy vết nghiệp vụ duy nhất (Unique Business Transaction Identifier).
* **Luồng truyền:**
1. `ConcertBookingService` tiếp nhận hoặc khởi tạo `correlationId` trong payload JSON ban đầu.
2. `SeatAssignmentService` giải mã JSON từ `record.value()`, đọc thuộc tính `event.getCorrelationId()`.
3. Tại bước chuyển tiếp, `SeatAssignmentService` gán giá trị biến này trực tiếp vào `SeatReservedEvent.setCorrelationId(correlationId)`.
4. `NotificationService` đọc thuộc tính `event.getCorrelationId()` để ghi log.


* Nhờ vậy, chuỗi nhật ký của nhiều service chạy trên các máy ảo/container phân tán hoàn toàn có thể được tập hợp về Elasticsearch/Loki/CloudWatch và lọc theo từ khóa `CONCERT-2024-999` để kiểm tra toàn vẹn chu trình đặt vé.

## 3. Hướng dẫn cài đặt và chạy dự án

### Bước 1: Khởi tạo Kafka Broker bằng Docker

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

Khởi động container:

```bash
docker compose up -d

```

### Bước 2: Tạo Topics thủ công (hoặc để Kafka tự tạo khi gửi message)

```bash
docker exec -it <kafka-container-id> kafka-topics --create --topic concert-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it <kafka-container-id> kafka-topics --create --topic seat-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

```

### Bước 3: Khởi động 3 dịch vụ

* Mở 3 terminal riêng hoặc chạy từ IDE:
* `mvn spring-boot:run` tại thư mục `concert-booking-service`
* `mvn spring-boot:run` tại thư mục `seat-assignment-service`
* `mvn spring-boot:run` tại thư mục `notification-service`



### Bước 4: Kiểm thử với dữ liệu đầu vào

Sử dụng cURL:

```bash
curl -X POST http://localhost:8081/api/concerts/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "correlationId": "CONCERT-2024-999",
    "concertCode": "LIVE-HCM-2024",
    "customerEmail": "nguyenvanA@email.com",
    "ticketQuantity": 3
  }'

```

## 4. Kết quả chạy thử nghiệm (Expected Log Output)

Sau khi gọi API, kiểm tra console log của `seat-assignment-service` và `notification-service`:

```text
[SeatService] Received event with correlationId: CONCERT-2024-999
[SeatService] Seat reserved successfully for correlationId: CONCERT-2024-999
[SeatService] Publishing SeatReserved event with correlationId: CONCERT-2024-999 to topic: seat-events
[NotifyService] Received confirmation for correlationId: CONCERT-2024-999 - Sending email to nguyenvanA@email.com

```