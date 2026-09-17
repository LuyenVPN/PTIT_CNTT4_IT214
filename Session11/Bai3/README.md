# BÀI 3 - Kafka Fan-out và Event Consumer

## 1. Kiến trúc

Topic dùng chung: `storex-order-events` với **5 partitions**.

Có 2 Consumer Group độc lập:

- `storex-inventory`: chỉ dành cho Inventory-Service.
- `storex-loyalty`: chỉ dành cho Loyalty-Service.

Khi Producer gửi `order.created` vào topic, Kafka phân phối message theo từng Consumer Group độc lập. Vì vậy:

```text
                         storex-order-events (5 partitions)
                                  |
                    +-------------+-------------+
                    |                           |
          group: storex-inventory      group: storex-loyalty
                    |                           |
          +---------+---------+                 |
          |         |         |                 |
        Inv #1    Inv #2    Inv #3          Loyalty #1
          P0/P3     P1/P4      P2               P0..P4
```

Các partition cụ thể được Kafka coordinator phân phối động; sơ đồ trên chỉ minh họa. Không nên giả định một instance luôn giữ đúng partition nào.

## 2. BUG-04 - dùng chung group-id

Sai:

```yaml
spring:
  kafka:
    consumer:
      group-id: storex-system
```

cho cả Inventory và Loyalty.

Đúng:

```yaml
# inventory-service
spring:
  kafka:
    consumer:
      group-id: storex-inventory
```

```yaml
# loyalty-service
spring:
  kafka:
    consumer:
      group-id: storex-loyalty
```

Lý do: các consumer trong **cùng một group** cạnh tranh để xử lý một message/partition. Hai service nghiệp vụ khác nhau phải thuộc hai group khác nhau để mỗi group nhận được toàn bộ luồng sự kiện.

## 3. REQ-01 - số Partition

Inventory có tối đa 3 instance trong cùng một consumer group.

Vì Kafka chỉ gán một partition cho tối đa một consumer trong cùng group tại một thời điểm, cần:

**Partition tối thiểu = 3.**

Bài này cấu hình **5 partitions**, nên đáp ứng yêu cầu và còn dư 2 partition để tăng khả năng phân phối tải.

Điểm cần phân biệt:

- 5 partitions + 3 Inventory instances: tối đa 3 consumer active, một số instance có thể nhận 1-2 partitions.
- Nếu có 1 Inventory instance: instance đó có thể nhận cả 5 partitions.
- Nếu chạy 6 Inventory instances với topic chỉ có 5 partitions: tối đa 5 consumer có partition; 1 instance sẽ ở trạng thái idle.

## 4. Scale-out

Ba Inventory-Service instance phải dùng **cùng** group:

`storex-inventory`

nhưng nên có `client-id`/`INSTANCE_ID` khác nhau để dễ quan sát log.

Ví dụ:

```text
Inventory #1: group=storex-inventory, client-id=inventory-1
Inventory #2: group=storex-inventory, client-id=inventory-2
Inventory #3: group=storex-inventory, client-id=inventory-3

Loyalty #1:   group=storex-loyalty,   client-id=loyalty-1
```

`INSTANCE_ID` không dùng để chia group; **group-id mới là yếu tố quyết định consumer nào cạnh tranh với nhau**.

## 5. Kafka key

Consumer đọc:

```java
String orderId = record.key();
```

Do đó Producer của bài trước phải gửi `orderId` làm Kafka key. Cùng một `orderId` sẽ được route ổn định về cùng partition trong điều kiện partitioner/key mapping không thay đổi, giúp giữ thứ tự các event của cùng order trong partition.

## 6. Cấu trúc

Mỗi service là một Gradle project độc lập:

```text
Bai3/
├── inventory-service/
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/...
├── loyalty-service/
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/...
├── settings.gradle
└── README.md
```

Không phụ thuộc vào Gradle project cha để build từng service.
