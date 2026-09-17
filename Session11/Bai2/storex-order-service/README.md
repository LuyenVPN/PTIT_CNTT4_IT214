# StoreX Order Service - Bài tập 2

## Mục tiêu

Xây dựng API đặt hàng bất đồng bộ bằng Spring WebFlux và Kafka Producer.

## Công nghệ

- Java 21
- Spring Boot 4.0.6
- Spring WebFlux
- Reactor Kafka
- Apache Kafka
- Gradle

## API

### POST /api/v1/orders

Request:

```json
{
  "customerId": "CUS-001",
  "items": [
    {
      "productId": 101,
      "quantity": 2,
      "unitPrice": 150000
    }
  ],
  "totalAmount": 300000
}
```

Response:

HTTP 202 Accepted

```text
ORD-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

## Kafka

Topic:

```text
storex-order-events
```

Topic được cấu hình 5 partitions.

Event:

```json
{
  "eventType": "order.created",
  "orderId": "ORD-...",
  "customerId": "CUS-001",
  "order": {
    "customerId": "CUS-001",
    "items": [
      {
        "productId": 101,
        "quantity": 2,
        "unitPrice": 150000
      }
    ],
    "totalAmount": 300000
  },
  "occurredAt": "2026-09-17T00:00:00Z"
}
```

## BUG-03

Producer bắt buộc sử dụng:

```text
Kafka Key = orderId
```

Trong code, `ProducerRecord` được tạo với `orderId` làm key:

```java
new ProducerRecord<>(topic, orderId, event)
```

Các event của cùng một orderId sẽ được Kafka định tuyến vào cùng partition.

## Chạy Kafka

Yêu cầu Docker Desktop đang chạy.

```bash
docker compose up -d
```

Kiểm tra container:

```bash
docker ps
```

## Chạy Spring Boot

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

## Test bằng curl

```bash
curl -X POST http://localhost:8080/api/v1/orders   -H "Content-Type: application/json"   -d "{"customerId":"CUS-001","items":[{"productId":101,"quantity":2,"unitPrice":150000}],"totalAmount":300000}"
```

## Kiểm tra topic

```bash
docker exec -it storex-kafka /opt/kafka/bin/kafka-topics.sh   --bootstrap-server localhost:9092   --describe   --topic storex-order-events
```

Kết quả phải có:

```text
PartitionCount: 5
```

## Lưu ý nghiệp vụ

Bài tập này tập trung vào Event Producer. Chưa có database để lưu draft order vì đề bài chưa yêu cầu implementation persistence.

API chỉ tạo orderId, tạo event `order.created`, gửi event vào Kafka rồi trả về HTTP 202.
