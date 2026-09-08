# Bài 3 — Chuyển đổi từ SOA sang Microservice Architecture bằng REST API

## 1. Mục tiêu

Chuyển đổi cách giao tiếp giữa các service từ mô hình SOA sử dụng ESB sang Microservice Architecture sử dụng REST API.

Mục tiêu chính:

* Sử dụng REST API để giao tiếp trực tiếp giữa các Microservice.
* Sử dụng Service Discovery và Load Balancing thay vì IP cố định.
* Xử lý lỗi khi service đích không phản hồi.
* Chuyển thao tác `notifyOverdue` từ ESB sang REST API.
* Phân tích ưu, nhược điểm của SOA và MSA trong bối cảnh LibraX.

---

# 2. Phân tích lỗi gọi IP cố định

Code ban đầu:

```java
@Service
public class BookClientService {

    private RestTemplate restTemplate = new RestTemplate();

    public String getBookTitle(Long bookId) {

        String url =
                "http://192.168.1.15:8082/api/books/" + bookId;

        return restTemplate.getForObject(
                url,
                String.class
        );
    }
}
```

## 2.1. Vấn đề

Code đang gọi trực tiếp:

```text
http://192.168.1.15:8082
```

Đây là địa chỉ IP cố định của một instance cụ thể của `book-service`.

Trong môi trường Microservice, `book-service` có thể được scale thành nhiều instance:

```text
book-service
├── Instance 1 → 192.168.1.15:8082
├── Instance 2 → 192.168.1.16:8082
└── Instance 3 → 192.168.1.17:8082
```

Khi hệ thống scale hoặc một instance bị lỗi, IP và số lượng instance có thể thay đổi.

Nếu `borrowing-service` vẫn gọi:

```text
192.168.1.15:8082
```

thì nó chỉ phụ thuộc vào một instance duy nhất.

Nếu instance đó bị dừng:

```text
borrowing-service
       |
       v
192.168.1.15:8082
       |
       X
   Service Down
```

request sẽ thất bại mặc dù các instance khác của `book-service` vẫn đang hoạt động.

---

# 3. Giải pháp: Service Discovery + Load Balancing

Thay vì sử dụng IP cố định:

```text
http://192.168.1.15:8082/api/books/1
```

sử dụng tên logic của service:

```text
http://book-service/api/books/1
```

Sau đó sử dụng:

```java
@LoadBalanced
```

cho `RestTemplate`.

Kiến trúc:

```text
borrowing-service
       |
       | http://book-service
       v
Service Discovery
       |
       v
Load Balancer
    /   |   \
   /    |    \
  v     v     v
Instance 1  Instance 2  Instance 3
```

Load Balancer có thể chọn một instance đang hoạt động để xử lý request.

---

# 4. Cấu hình `RestTemplate`

Tạo class:

```java
package com.librax.borrowing.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

Annotation:

```java
@LoadBalanced
```

cho phép `RestTemplate` sử dụng cơ chế Load Balancing khi gọi service bằng tên logic.

---

# 5. BookClientService đã sửa

```java
package com.librax.borrowing.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BookClientService {

    private static final Logger log =
            LoggerFactory.getLogger(BookClientService.class);

    private final RestTemplate restTemplate;

    public BookClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getBookTitle(Long bookId) {

        String url =
                "http://book-service/api/books/" + bookId;

        try {

            return restTemplate.getForObject(
                    url,
                    String.class
            );

        } catch (RestClientException exception) {

            log.warn(
                    "Unable to communicate with book-service. bookId={}",
                    bookId,
                    exception
            );

            return "Book service is currently unavailable";
        }
    }
}
```

Điểm quan trọng:

### Code cũ

```java
String url =
        "http://192.168.1.15:8082/api/books/" + bookId;
```

### Code mới

```java
String url =
        "http://book-service/api/books/" + bookId;
```

Kết hợp với:

```java
@LoadBalanced
RestTemplate
```

---

# 6. Xử lý lỗi

Code mới sử dụng:

```java
try {
    return restTemplate.getForObject(
            url,
            String.class
    );
} catch (RestClientException exception) {

    log.warn(
            "Unable to communicate with book-service. bookId={}",
            bookId,
            exception
    );

    return "Book service is currently unavailable";
}
```

Nếu `book-service`:

* Không phản hồi.
* Connection bị lỗi.
* Trả HTTP error.
* Không thể kết nối tới instance.

thì `RestTemplate` có thể phát sinh `RestClientException`.

Thay vì để exception làm request của `borrowing-service` thất bại ngoài ý muốn, hệ thống bắt exception và ghi log.

Luồng:

```text
borrowing-service
       |
       v
book-service
       |
       X
  Connection Error
       |
       v
catch RestClientException
       |
       v
log.warn(...)
       |
       v
Return fallback response
```

Trong hệ thống thực tế có thể nâng cấp cơ chế này bằng:

```text
Timeout
Retry
Circuit Breaker
Fallback
Monitoring
```

---

# 7. REST API cho `notifyOverdue`

Trong Bài 2, thao tác `notifyOverdue` được thực hiện thông qua ESB:

```text
BorrowingService
       |
       v
ESB
       |
       v
NotificationService
```

Khi chuyển sang Microservice Architecture, ESB được loại bỏ khỏi luồng giao tiếp:

```text
BorrowingService
       |
       | HTTP POST
       v
NotificationService
```

---

# 8. Request DTO

Tạo:

```java
package com.librax.notification.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NotifyOverdueRequest(

        @NotNull
        Long memberId,

        @NotNull
        Long bookId,

        @NotNull
        LocalDate dueDate

) {
}
```

Request chứa ba thông tin theo Service Contract ở Bài 2:

```text
memberId
bookId
dueDate
```

---

# 9. Response DTO

```java
package com.librax.notification.dto;

public record NotifyOverdueResponse(
        boolean success,
        String message
) {
}
```

Ví dụ response:

```json
{
    "success": true,
    "message": "Overdue notification sent successfully"
}
```

---

# 10. NotificationController

```java
package com.librax.notification.controller;

import com.librax.notification.dto.NotifyOverdueRequest;
import com.librax.notification.dto.NotifyOverdueResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @PostMapping("/overdue")
    public ResponseEntity<NotifyOverdueResponse> notifyOverdue(
            @Valid @RequestBody NotifyOverdueRequest request
    ) {

        // Notification business logic

        NotifyOverdueResponse response =
                new NotifyOverdueResponse(
                        true,
                        "Overdue notification sent successfully"
                );

        return ResponseEntity.ok(response);
    }
}
```

Endpoint:

```text
POST /api/notifications/overdue
```

Request:

```json
{
    "memberId": 1001,
    "bookId": 2001,
    "dueDate": "2026-09-01"
}
```

Response:

```json
{
    "success": true,
    "message": "Overdue notification sent successfully"
}
```

---

# 11. Borrowing gọi Notification Service

Tạo client trong `borrowing-service`:

```java
package com.librax.borrowing.client;

import com.librax.borrowing.dto.NotifyOverdueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationClientService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationClientService.class);

    private final RestTemplate restTemplate;

    public NotificationClientService(
            RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
    }

    public boolean notifyOverdue(
            Long memberId,
            Long bookId,
            String dueDate
    ) {

        String url =
                "http://notification-service/api/notifications/overdue";

        NotifyOverdueRequest request =
                new NotifyOverdueRequest(
                        memberId,
                        bookId,
                        dueDate
                );

        try {

            restTemplate.postForObject(
                    url,
                    request,
                    String.class
            );

            return true;

        } catch (RestClientException exception) {

            log.warn(
                    "Unable to communicate with notification-service. " +
                    "memberId={}, bookId={}",
                    memberId,
                    bookId,
                    exception
            );

            return false;
        }
    }
}
```

DTO:

```java
package com.librax.borrowing.dto;

public record NotifyOverdueRequest(
        Long memberId,
        Long bookId,
        String dueDate
) {
}
```

---

# 12. Luồng xử lý `notifyOverdue`

## Bước 1 — BorrowingService phát hiện sách quá hạn

`borrowing-service` kiểm tra:

```text
Current Date > Due Date
```

Ví dụ:

```text
Due Date      = 2026-09-01
Current Date  = 2026-09-07
```

Kết luận:

```text
Book is overdue
```

---

## Bước 2 — Tạo REST Request

`borrowing-service` tạo:

```json
{
    "memberId": 1001,
    "bookId": 2001,
    "dueDate": "2026-09-01"
}
```

---

## Bước 3 — Gọi Notification Service

Request được gửi trực tiếp:

```text
POST http://notification-service/api/notifications/overdue
```

Không còn:

```text
BorrowingService → ESB → NotificationService
```

mà là:

```text
BorrowingService
       |
       | HTTP REST
       v
NotificationService
```

---

## Bước 4 — NotificationService xử lý

`NotificationController` nhận request:

```text
memberId = 1001
bookId   = 2001
dueDate  = 2026-09-01
```

Sau đó thực hiện nghiệp vụ gửi thông báo.

---

## Bước 5 — Trả response

```json
{
    "success": true,
    "message": "Overdue notification sent successfully"
}
```

---

# 13. Tổng quan kiến trúc MSA

```text
                         ┌──────────────────┐
                         │ Service Discovery│
                         └────────┬─────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             v                    v                    v
      ┌─────────────┐     ┌───────────────┐    ┌──────────────────┐
      │ book-service│     │borrowing-service│   │notification-service│
      └─────────────┘     └───────┬───────┘    └──────────────────┘
                                  │
                                  │ REST
                                  │
                                  v
                         notification-service
```

Đối với `book-service`:

```text
borrowing-service
       |
       | http://book-service
       v
Service Discovery
       |
       v
Load Balancer
   /    |    \
  v     v     v
Book  Book   Book
  1     2     3
```

---

# 14. So sánh SOA và MSA trong LibraX

## SOA

```text
BorrowingService
       |
       v
      ESB
       |
       v
NotificationService
```

## MSA

```text
BorrowingService
       |
       | REST
       v
NotificationService
```

---

# 15. Phân tích ưu và nhược điểm

Việc chuyển LibraX từ mô hình SOA sử dụng ESB sang Microservice Architecture sử dụng REST API mang lại nhiều lợi ích nhưng cũng tạo ra những đánh đổi đáng kể. Trong mô hình SOA trước đây, `BorrowingService` gửi message tới ESB và ESB chịu trách nhiệm định tuyến đến `NotificationService`. Cách tiếp cận này tạo ra một tầng trung gian giúp quản lý việc tích hợp tập trung, nhưng đồng thời ESB có thể trở thành thành phần phức tạp và là điểm phụ thuộc chung của nhiều service.

Khi chuyển sang MSA, `borrowing-service` có thể gọi trực tiếp `notification-service` hoặc `book-service` thông qua REST API. Điều này làm cho giao tiếp đơn giản và nhẹ hơn. Các service có API rõ ràng, dễ phát triển và có thể triển khai độc lập. Ví dụ, đội phát triển có thể thay đổi hoặc scale `book-service` mà không cần thay đổi toàn bộ hệ thống. Service Discovery kết hợp với Load Balancing cũng giúp `borrowing-service` không phải phụ thuộc vào một IP cố định của một instance cụ thể.

Về tốc độ phát triển, MSA cho phép các nhóm phát triển làm việc độc lập trên từng service. `book-service`, `borrowing-service` và `notification-service` có thể có vòng đời phát triển và triển khai riêng. Tuy nhiên, sự độc lập này đi kèm với yêu cầu phải thiết kế API contract rõ ràng và quản lý việc tương thích giữa các phiên bản API.

Về độ phức tạp vận hành, MSA phức tạp hơn SOA/Monolith. LibraX phải quản lý nhiều process, service discovery, load balancing, network communication, logging, monitoring và deployment. Một lỗi mạng hoặc service tạm thời không phản hồi cũng có thể ảnh hưởng đến request của service khác. Vì vậy cần có timeout, retry, circuit breaker và monitoring phù hợp.

Về khả năng chịu lỗi, MSA có ưu điểm là lỗi của một service không nhất thiết làm toàn bộ hệ thống dừng hoạt động. Ví dụ, nếu `notification-service` tạm thời không hoạt động, `borrowing-service` vẫn có thể tiếp tục xử lý nghiệp vụ mượn sách nếu có cơ chế xử lý lỗi phù hợp. Tuy nhiên, giao tiếp qua mạng cũng tạo thêm các failure mode mới như timeout, connection failure và service unavailable.

Tóm lại, MSA giúp LibraX tăng khả năng scale độc lập, phát triển độc lập và cô lập lỗi tốt hơn, nhưng phải đánh đổi bằng độ phức tạp vận hành và quản lý giao tiếp giữa các service. Việc chuyển từ SOA sang MSA phù hợp khi LibraX đã phát triển đủ lớn và các domain có nhu cầu scale, triển khai hoặc phát triển độc lập.

---

# 16. So sánh nhanh

| Tiêu chí          | SOA                           | MSA                         |
| ----------------- | ----------------------------- | --------------------------- |
| Giao tiếp         | Qua ESB                       | REST trực tiếp              |
| Trung gian        | ESB                           | Không cần ESB               |
| Coupling          | Có thể phụ thuộc ESB          | Service độc lập hơn         |
| Protocol          | SOAP/XML hoặc message         | REST/HTTP/JSON              |
| Scale             | Khó hơn                       | Scale từng service          |
| Deployment        | Ít độc lập hơn                | Độc lập                     |
| Vận hành          | Đơn giản hơn MSA              | Phức tạp hơn                |
| Service Discovery | Không nhất thiết              | Thường cần                  |
| Load Balancing    | Có thể qua infrastructure/ESB | Thường cần                  |
| Fault handling    | Qua middleware                | Phải xử lý ở client/service |
| Tốc độ phát triển | Tốt                           | Tốt khi hệ thống lớn        |
| Độ phức tạp       | Thấp hơn MSA                  | Cao hơn                     |

---

# 17. Cấu trúc project Microservice

Khác với Bài 1 và Bài 2, trong MSA các service nên là các ứng dụng có thể chạy độc lập:

```text
librax-microservices
│
├── book-service
│   └── src/main/java
│
├── borrowing-service
│   └── src/main/java
│
├── notification-service
│   └── src/main/java
│
└── discovery-server
    └── src/main/java
```

Mỗi service có thể có:

```text
Controller
Service
Repository
Model
DTO
```

và có thể được build/deploy độc lập.

---

# 18. Kết luận

Bài 3 chuyển LibraX từ mô hình SOA sử dụng ESB sang Microservice Architecture sử dụng REST API.

Các thay đổi chính:

1. Loại bỏ việc gọi IP cố định:

```text
http://192.168.1.15:8082
```

2. Sử dụng tên logic:

```text
http://book-service
```

3. Sử dụng:

```java
@LoadBalanced
RestTemplate
```

để hỗ trợ Service Discovery và Load Balancing.

4. Bổ sung xử lý:

```java
try {
    ...
} catch (RestClientException exception) {
    ...
}
```

để tránh lỗi từ service phụ thuộc làm crash toàn bộ request.

5. Chuyển:

```text
BorrowingService
      ↓
     ESB
      ↓
NotificationService
```

thành:

```text
BorrowingService
      ↓
   REST API
      ↓
NotificationService
```

6. `notifyOverdue` được thiết kế thành REST endpoint:

```text
POST /api/notifications/overdue
```

MSA giúp LibraX có khả năng scale và deploy từng service độc lập, nhưng đổi lại phải chấp nhận độ phức tạp cao hơn trong service discovery, networking, monitoring, deployment và xử lý lỗi.
