# VietMart - Order Service - Bài tập 2

## Nội dung

Bài tập chuyển giao tiếp từ RestTemplate sang Spring Cloud OpenFeign.

Đã hoàn thành:

- ProductClient với `@FeignClient(name = "product-service")`
- `getById(Long id)`
- `getAll()`
- UserClient với `@FeignClient(name = "user-service")`
- `getUserById(Long userId)`
- ProductClientFallbackFactory
- Log exception
- Fallback `ProductInfo.fallback(id)` cho `getById`
- Trả danh sách rỗng cho `getAll`
- `@EnableFeignClients`
- Cấu hình Feign timeout 2 giây connect và 3 giây read
- Eureka service discovery

## Chạy project

Yêu cầu:

1. Java 17+
2. Gradle
3. Eureka Server chạy tại `http://localhost:8761`

Sau đó:

```bash
./gradlew clean test
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

## Lưu ý endpoint

Project mẫu sử dụng:

- Product: `GET /api/products/{id}`
- Product list: `GET /api/products`
- User: `GET /api/users/{id}`

Nếu product-service hoặc user-service của bạn dùng endpoint khác, sửa các `@GetMapping` tương ứng.

## So sánh

RestTemplate yêu cầu viết implementation, tạo request, xử lý response và exception thủ công.

Feign khai báo interface và mapping HTTP bằng annotation; framework tự tạo implementation nên mã nguồn ngắn và dễ đọc hơn.

RestTemplate phù hợp khi cần kiểm soát HTTP request ở mức chi tiết hoặc duy trì hệ thống imperative hiện có. Feign phù hợp với các lời gọi service-to-service có API ổn định và muốn giảm boilerplate.

## Cấu trúc

```text
src/main/java/com/vietmart/orderservice
├── client
│   ├── ProductClient.java
│   ├── ProductClientFallbackFactory.java
│   └── UserClient.java
├── dto
│   ├── ProductInfo.java
│   └── UserInfo.java
└── OrderServiceApplication.java
```
