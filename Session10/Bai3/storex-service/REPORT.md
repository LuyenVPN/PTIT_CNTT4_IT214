# BÁO CÁO BÀI TẬP 3

## 1. Phân tích lỗi
RestTemplate là HTTP client blocking/synchronous. Khi gọi `getForObject`, thread phải chờ response. Trong WebFlux, việc này làm event-loop thread bị block, khiến khả năng xử lý nhiều request đồng thời bị suy giảm và có thể dẫn đến nghẽn thread khi tải cao.

WebFlux không tự biến RestTemplate thành non-blocking.

## 2. Giải pháp
Thay RestTemplate bằng WebClient và trả về `Mono<Banner>`. Không gọi `.block()`.

## 3. Timeout và fallback
Dùng `timeout(Duration.ofSeconds(2))`. Khi xảy ra timeout hoặc lỗi HTTP/network, `onErrorResume` trả về Banner mặc định có thông báo `Khuyến mãi đang được cập nhật`.

## 4. Service Discovery
WebClient Builder được đánh dấu `@LoadBalanced` và sử dụng `http://promotion-service`, cho phép dùng service-id khi chạy cùng Eureka/Service Discovery.

## 5. Kết quả
Code phù hợp với mô hình reactive của Spring WebFlux, có timeout 2 giây và fallback, không dùng RestTemplate.
