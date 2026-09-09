## 1. Nguyên nhân

Lỗi 503 xảy ra do Service ID được cấu hình trong API Gateway không nhất quán với tên đăng ký của `order-service` trên Eureka.

Trong `order-service`, thuộc tính:

```yaml
spring:
  application:
    name: order-service
```

quy định Service ID của service là `order-service`.

Tuy nhiên, API Gateway lại sử dụng:

```yaml
uri: lb://orders-service
```

Gateway vì vậy yêu cầu Spring Cloud LoadBalancer tìm service có Service ID là `orders-service`. Do Eureka không có service với tên này nên Gateway không thể tìm được instance để định tuyến request và trả về lỗi `503 Service Unavailable`.

Việc `order-service` vẫn hiển thị `UP` trên Eureka Dashboard không mâu thuẫn với lỗi trên. Điều đó chỉ cho thấy instance `order-service` đã đăng ký thành công với Eureka; Gateway vẫn phải sử dụng đúng Service ID để tìm được instance.

## 2. Cách khắc phục

Sửa cấu hình route của API Gateway từ:

```yaml
uri: lb://orders-service
```

thành:

```yaml
uri: lb://order-service
```

Cấu hình hoàn chỉnh:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
```

## 3. Cơ chế hoạt động

Khi client gửi request:

```text
/api/orders
```

API Gateway kiểm tra predicate `Path=/api/orders/**` và xác định request thuộc route `order-service-route`.

Do URI sử dụng tiền tố `lb://`, Gateway chuyển việc tìm instance cho Spring Cloud LoadBalancer. LoadBalancer sử dụng Service ID `order-service` để lấy danh sách instance tương ứng từ Eureka.

Sau khi nhận được danh sách instance, LoadBalancer chọn một instance của `order-service` và Gateway chuyển tiếp request đến instance đó.

## 4. Kết quả

Sau khi đồng bộ Service ID:

```text
spring.application.name = order-service
              ↓
uri = lb://order-service
              ↓
Eureka tìm thấy order-service
              ↓
LoadBalancer chọn instance
              ↓
Gateway định tuyến thành công
              ↓
HTTP 200 OK
```

Kết luận: lỗi 503 được gây ra bởi sự không nhất quán giữa `orders-service` trong `uri: lb://...` và `order-service` trong `spring.application.name`. Chỉ cần đảm bảo hai Service ID này khớp nhau là Gateway có thể định tuyến chính xác đến `order-service`.
