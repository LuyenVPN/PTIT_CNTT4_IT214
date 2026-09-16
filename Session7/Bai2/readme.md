# BÀI TẬP TỔNG HỢP 2: CẤU HÌNH LOAD BALANCING VỚI NHIỀU INSTANCE MICROSERVICE

## 1. Thông tin bài tập

**Hệ thống:** FinBank
**Chủ đề:** Spring Cloud Load Balancing với nhiều instance Microservice

### Mục tiêu

Cấu hình hệ thống FinBank sử dụng nhiều instance của `ACCOUNT-SERVICE` và thực hiện Load Balancing thông qua API Gateway.

Mô hình sau khi hoàn thành:

```text
                    ┌─────────────────────┐
                    │     API Gateway     │
                    │       :8222         │
                    └──────────┬──────────┘
                               │
                     lb://ACCOUNT-SERVICE
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
       ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
       │   Account   │  │   Account   │  │   Account   │
       │   Service   │  │   Service   │  │   Service   │
       │    :8082    │  │    :8092    │  │    :8102    │
       └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
              │                │                │
              └────────────────┼────────────────┘
                               │
                         ┌─────▼─────┐
                         │  Eureka   │
                         │   :8761   │
                         └───────────┘
```

---

# 2. Công nghệ sử dụng

* Java 21
* Spring Boot
* Spring Cloud
* Spring Cloud Netflix Eureka
* Spring Cloud Gateway
* Spring Cloud LoadBalancer
* Gradle
* IntelliJ IDEA

---

# 3. Các Service

| Service         | Port | Vai trò                      |
| --------------- | ---: | ---------------------------- |
| Eureka Server   | 8761 | Service Discovery            |
| API Gateway     | 8222 | API Gateway                  |
| Account Service | 8082 | Account Service - Instance 1 |
| Account Service | 8092 | Account Service - Instance 2 |
| Account Service | 8102 | Account Service - Instance 3 |

Ba instance Account Service sử dụng cùng:

```text
spring.application.name=account-service
```

Do đó Eureka quản lý cả ba instance dưới service:

```text
ACCOUNT-SERVICE
```

---

# 4. Cấu hình Account Service

File:

```text
account-service/src/main/resources/application.yml
```

```yaml
server:
  port: 8082

spring:
  application:
    name: account-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

  instance:
    prefer-ip-address: true
```

Port `8082` là port mặc định.

Khi chạy nhiều instance, có thể ghi đè port bằng tham số:

```text
--server.port=8092
```

hoặc:

```text
--server.port=8102
```

Không thay đổi:

```yaml
spring:
  application:
    name: account-service
```

vì cả ba instance phải đăng ký cùng một Service ID.

---

# 5. API kiểm tra Instance

Account Service cung cấp API:

```http
GET /api/accounts/info
```

Controller:

```java
@Value("${server.port}")
private String port;

@GetMapping("/info")
public Map<String, String> getInstanceInfo() {
    return Map.of(
            "service", "ACCOUNT-SERVICE",
            "port", port
    );
}
```

API trả về port của instance đang xử lý request.

Ví dụ:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8082"
}
```

hoặc:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8092"
}
```

hoặc:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8102"
}
```

---

# 6. Chạy Eureka Server

Khởi động Eureka Server trước.

Port:

```text
8761
```

Truy cập Dashboard:

```text
http://localhost:8761
```

Sau khi các Account Service chạy, Eureka sẽ hiển thị nhiều instance của:

```text
ACCOUNT-SERVICE
```

---

# 7. Chạy Account Service Instance 1

Instance mặc định chạy port `8082`.

Tại thư mục:

```text
account-service
```

chạy:

```powershell
.\gradlew bootRun
```

Hoặc:

```powershell
./gradlew bootRun
```

Kiểm tra:

```powershell
curl.exe http://localhost:8082/api/accounts/info
```

Kết quả:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8082"
}
```

---

# 8. Chạy Account Service Instance 2

Mở một terminal mới.

Di chuyển vào thư mục:

```text
account-service
```

Chạy:

```powershell
.\gradlew bootRun --args='--server.port=8092'
```

Kiểm tra:

```powershell
curl.exe http://localhost:8092/api/accounts/info
```

Kết quả:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8092"
}
```

---

# 9. Chạy Account Service Instance 3

Mở thêm một terminal mới.

Di chuyển vào thư mục:

```text
account-service
```

Chạy:

```powershell
.\gradlew bootRun --args='--server.port=8102'
```

Kiểm tra:

```powershell
curl.exe http://localhost:8102/api/accounts/info
```

Kết quả:

```json
{
  "service": "ACCOUNT-SERVICE",
  "port": "8102"
}
```

---

# 10. Kiểm tra trực tiếp 3 Instance

Chạy:

```powershell
curl.exe http://localhost:8082/api/accounts/info
curl.exe http://localhost:8092/api/accounts/info
curl.exe http://localhost:8102/api/accounts/info
```

Kết quả:

```text
{"service":"ACCOUNT-SERVICE","port":"8082"}
{"service":"ACCOUNT-SERVICE","port":"8092"}
{"service":"ACCOUNT-SERVICE","port":"8102"}
```

Điều này chứng minh cả ba instance đang hoạt động độc lập.

---

# 11. Cấu hình API Gateway

Gateway phải sử dụng Service Discovery và Load Balancing thay vì trỏ trực tiếp đến một port cố định.

Route:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: account-service
          uri: lb://ACCOUNT-SERVICE
          predicates:
            - Path=/api/accounts/**
```

Điểm quan trọng:

```text
lb://ACCOUNT-SERVICE
```

Không sử dụng:

```text
http://localhost:8082
```

Vì nếu Gateway sử dụng địa chỉ cố định `8082`, request sẽ luôn được gửi đến một instance duy nhất.

Khi sử dụng:

```text
lb://ACCOUNT-SERVICE
```

Gateway sẽ lấy danh sách các instance `ACCOUNT-SERVICE` từ Eureka và thực hiện Load Balancing.

---

# 12. Dependency Load Balancer

Gateway cần dependency:

```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
```

Dependency này cung cấp cơ chế Spring Cloud LoadBalancer để lựa chọn instance khi Gateway sử dụng:

```text
lb://ACCOUNT-SERVICE
```

---

# 13. Kiểm tra Load Balancing qua Gateway

Sau khi khởi động:

```text
Eureka Server : 8761
Account Service : 8082
Account Service : 8092
Account Service : 8102
API Gateway : 8222
```

Gọi API:

```text
http://localhost:8222/api/accounts/info
```

Có thể kiểm tra bằng:

```powershell
curl.exe http://localhost:8222/api/accounts/info
```

Gateway sẽ chuyển request đến một trong ba instance.

---

# 14. Kiểm tra 9 request

Chạy PowerShell:

```powershell
for ($i=1; $i -le 9; $i++) {
    Write-Host "Lan $i"
    curl.exe -s http://localhost:8222/api/accounts/info
    Write-Host ""
}
```

Kết quả thực tế:

```text
Lan 1
{"port":"8092","service":"ACCOUNT-SERVICE"}

Lan 2
{"service":"ACCOUNT-SERVICE","port":"8082"}

Lan 3
{"service":"ACCOUNT-SERVICE","port":"8102"}

Lan 4
{"port":"8092","service":"ACCOUNT-SERVICE"}

Lan 5
{"service":"ACCOUNT-SERVICE","port":"8082"}

Lan 6
{"service":"ACCOUNT-SERVICE","port":"8102"}

Lan 7
{"port":"8092","service":"ACCOUNT-SERVICE"}

Lan 8
{"service":"ACCOUNT-SERVICE","port":"8082"}

Lan 9
{"service":"ACCOUNT-SERVICE","port":"8102"}
```

Kết quả cho thấy request được phân phối lần lượt giữa:

```text
8092 → 8082 → 8102
8092 → 8082 → 8102
8092 → 8082 → 8102
```

Đây là cơ chế **Round Robin Load Balancing**.

> Thứ tự instance cụ thể có thể khác tùy thời điểm và thứ tự Eureka trả về danh sách instance. Điều quan trọng là request được phân phối giữa các instance đang hoạt động.

---

# 15. Kiểm tra khi một Instance bị dừng

Dừng instance `8102` bằng cách mở terminal đang chạy instance này và nhấn:

```text
Ctrl + C
```

Sau đó chờ Eureka cập nhật trạng thái instance.

Tiếp tục gọi:

```powershell
for ($i=1; $i -le 9; $i++) {
    Write-Host "Lan $i"
    curl.exe -s http://localhost:8222/api/accounts/info
    Write-Host ""
}
```

Kết quả mong đợi:

```text
8082
8092
8082
8092
8082
8092
...
```

Không còn:

```text
8102
```

Điều này chứng minh Load Balancer có thể loại bỏ instance không còn hoạt động khỏi danh sách lựa chọn thông qua Service Discovery.

---

# 16. Các URL kiểm tra

## Eureka Dashboard

```text
http://localhost:8761
```

## Account Service Instance 1

```text
http://localhost:8082/api/accounts/info
```

## Account Service Instance 2

```text
http://localhost:8092/api/accounts/info
```

## Account Service Instance 3

```text
http://localhost:8102/api/accounts/info
```

## API Gateway

```text
http://localhost:8222/api/accounts/info
```

---

# 17. Kết quả đạt được

Sau khi hoàn thành bài tập:

* [x] Account Service chạy trên port `8082`
* [x] Tạo thêm instance trên port `8092`
* [x] Tạo thêm instance trên port `8102`
* [x] Ba instance đăng ký cùng `ACCOUNT-SERVICE` trên Eureka
* [x] API `/api/accounts/info` trả về port của instance
* [x] API Gateway sử dụng `lb://ACCOUNT-SERVICE`
* [x] Spring Cloud LoadBalancer phân phối request
* [x] Kiểm tra được cơ chế Round Robin
* [x] Khi dừng instance `8102`, request được chuyển sang các instance còn lại
* [x] Không cần thay đổi URL Gateway khi thêm hoặc bớt instance

---

# 18. Kết luận

Bài tập đã triển khai thành công mô hình Load Balancing cho nhiều instance của Microservice.

Thay vì Gateway gọi trực tiếp:

```text
http://localhost:8082
```

Gateway sử dụng:

```text
lb://ACCOUNT-SERVICE
```

Eureka chịu trách nhiệm Service Discovery và cung cấp danh sách các instance đang đăng ký.

Spring Cloud LoadBalancer lựa chọn instance để xử lý request.

Mô hình:

```text
Client
   │
   ▼
API Gateway :8222
   │
   │ lb://ACCOUNT-SERVICE
   ▼
Eureka :8761
   │
   ├── ACCOUNT-SERVICE :8082
   ├── ACCOUNT-SERVICE :8092
   └── ACCOUNT-SERVICE :8102
```

Kết quả kiểm thử 9 request cho thấy các request được phân phối đều qua ba instance theo cơ chế Round Robin.

Khi instance `8102` bị dừng, hệ thống tiếp tục phân phối request cho `8082` và `8092`, giúp tăng khả năng chịu lỗi và tính sẵn sàng của hệ thống.
