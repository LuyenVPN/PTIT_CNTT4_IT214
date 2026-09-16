# BÁO CÁO BÀI TẬP 2: CHUYỂN ĐỔI RESTTEMPLATE SANG FEIGNCLIENT

## 1. Mục tiêu

Chuyển cách giao tiếp đồng bộ từ RestTemplate sang Spring Cloud OpenFeign trong order-service, đồng thời bổ sung UserClient để gọi user-service.

## 2. ProductClient

`ProductClient` là interface được đánh dấu bằng:

```java
@FeignClient(name = "product-service",
             fallbackFactory = ProductClientFallbackFactory.class)
```

Feign sử dụng service ID `product-service` để kết hợp với service discovery thay vì hardcode IP/port.

Hai phương thức:

- `getById(Long id)`: gọi `GET /api/products/{id}`
- `getAll()`: gọi `GET /api/products`

Không cần tự viết implementation class.

## 3. UserClient

`UserClient` sử dụng:

```java
@FeignClient(name = "user-service")
```

và cung cấp:

```java
UserInfo getUserById(Long userId)
```

tương ứng với `GET /api/users/{id}`.

## 4. FallbackFactory

`ProductClientFallbackFactory` nhận `Throwable cause` và ghi log lỗi.

Khi Product Service gặp lỗi:

- `getById(id)` trả về `ProductInfo.fallback(id)`.
- `getAll()` trả về `Collections.emptyList()`.

Việc dùng FallbackFactory giúp nhận biết nguyên nhân lỗi và tạo fallback phù hợp.

## 5. So sánh RestTemplate và FeignClient

| Tiêu chí | RestTemplate | FeignClient |
|---|---|---|
| Phong cách | Imperative | Declarative |
| Implementation | Tự viết | Framework tạo |
| Boilerplate | Nhiều hơn | Ít hơn |
| Khai báo API | Tự xây request | Annotation |
| Service ID | Có thể dùng LoadBalancer | `@FeignClient(name=...)` |
| Fallback | Tự xử lý | Có Fallback/FallbackFactory |
| Khả năng kiểm soát HTTP | Chi tiết | Trừu tượng hóa cao hơn |

Không nên kết luận Feign luôn tốt hơn RestTemplate. Việc lựa chọn phụ thuộc yêu cầu hệ thống. Feign thuận tiện cho các API service-to-service có cấu trúc rõ ràng; RestTemplate có thể phù hợp khi cần kiểm soát request/response chi tiết hoặc hệ thống hiện tại đã phụ thuộc vào nó.

## 6. Kết luận

Project đã chuyển phần gọi Product Service sang FeignClient và bổ sung UserClient. Code không cần implementation class cho client. FallbackFactory cung cấp giá trị dự phòng khi Product Service không đáp ứng được request.

## 7. Lưu ý môi trường

Project này là project mẫu độc lập cho Bài 2. Khi tích hợp vào hệ thống VietMart thực tế, cần bảo đảm:

- Eureka Server đang chạy.
- `product-service` đăng ký với Eureka bằng service ID `product-service`.
- `user-service` đăng ký bằng service ID `user-service`.
- Endpoint thực tế của hai service khớp với các `@GetMapping` trong client.
