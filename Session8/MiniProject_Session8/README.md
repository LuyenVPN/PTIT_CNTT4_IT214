# HỆ THỐNG MICROSERVICES QUẢN LÝ ĐƠN HÀNG (IT215 - SS8 MINIPROJECT)

Dự án hiện thực hóa kiến trúc Microservices chuyển đổi từ Monolithic theo **Bản ghi nhận yêu cầu khách hàng** của Project Manager (PM), tuân thủ 100% phạm vi kiến thức đã học tại các **Session 02, 03, 05, 06**.

---

## 1. Bản đồ Kiến trúc Hệ thống

```
                                      [ Client (Browser / Mobile / Postman) ]
                                                        │
                                                        ▼
                                       ┌──────────────────────────────────┐
                                       │    SPRING CLOUD API GATEWAY      │
                                       │           Port: 8080             │
                                       └────────────────┬─────────────────┘
                                                        │
                                  ┌─────────────────────┼─────────────────────┐
                     lb://user-service       lb://order-service      lb://product-service
                                  │                     │                     │
                                  ▼                     ▼                     ▼
                        ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
                        │   USER-SERVICE   │  │  ORDER-SERVICE   │  │ PRODUCT-SERVICE  │
                        │    Port: 8081    │  │  Port: 8082/8083 │  │    Port: 8084    │
                        └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
                                 │              │      │ OpenFeign           │
                                 │              │      └──────────────┐      │
                                 │              └──────────────┐      │      │
                                 │                             │      │      │
                                 ▼                             ▼      ▼      ▼
                        [ H2: user_db ]               [ H2: order_db ]      [ H2: product_db ]

   ─────────────────────────────────────────────────────────────────────────────────────────────
   [ HẠ TẦNG QUẢN TRỊ & ĐIỀU PHỐI ]
   • EUREKA DISCOVERY SERVER (Port 8761): Lưu trữ danh bạ, Heartbeat, Dynamic Service Registry
   • CONFIG SERVER (Port 8888): Cấu hình tập trung (Centralized Config), Native Profile
```

---

## 2. Đáp ứng chi tiết các yêu cầu của Khách hàng

### 2.1. Về chuyển đổi kiến trúc & ranh giới dữ liệu (Session 02)
- **Monolithic → Microservices**: Hệ thống tách thành các dịch vụ độc lập (`user-service`, `product-service`, `order-service`), mỗi service có vòng đời build, test, deploy riêng biệt.
- **Database-per-service**: 
  - `user-service` sở hữu database riêng `jdbc:h2:mem:user_db`.
  - `product-service` sở hữu database riêng `jdbc:h2:mem:product_db`.
  - `order-service` sở hữu database riêng `jdbc:h2:mem:order_db`.
  - Tuyệt đối không có chuyện service này truy cập SQL/JDBC trực tiếp sang database của service khác.

### 2.2. Về cấu hình tập trung (Session 03)
- Module **`config-server`** (port 8888) đóng vai trò trung tâm lưu trữ toàn bộ cấu hình hệ thống:
  - Cấu hình chung Eureka client trong `application.yml`.
  - Cấu hình riêng cho từng service trong `user-service.yml`, `product-service.yml`, `order-service.yml`, `api-gateway.yml`.
- Khi cần thay đổi thông số (timeout, log, datasource), chỉ cần cập nhật tại một nơi duy nhất trên Config Server.

### 2.3. Về phát hiện dịch vụ & Mở rộng linh hoạt (Session 03 & Session 05)
- Module **`discovery-server`** (Netflix Eureka Server port 8761):
  - Tất cả các service đăng ký metadata động (`service-id`, `ip`, `port`) lên Eureka.
  - Khi cần nhân bản thêm instance (ví dụ chạy `order-service` trên port 8083), service mới tự đăng ký lên Eureka.
  - **Spring Cloud LoadBalancer** tích hợp sẵn tại Gateway và OpenFeign tự động nhận diện và phân phối tải vòng tròn (Round-Robin) mà không cần chỉnh sửa cấu hình IP tĩnh.

### 2.4. Về điểm truy cập duy nhất & Cân bằng tải (Session 05)
- Module **`api-gateway`** (Spring Cloud Gateway port 8080):
  - Là **điểm truy cập duy nhất (Single Entry Point)** cho người dùng cuối và client.
  - Định tuyến thông minh dựa trên tên logic đã đăng ký với Eureka:
    - `/api/v1/users/**` ➔ `lb://user-service`
    - `/api/v1/products/**` ➔ `lb://product-service`
    - `/api/v1/orders/**` ➔ `lb://order-service`

### 2.5. Giải quyết yêu cầu "KHOAI NHẤT" của khách hàng (Mục 4 & Session 06)
> *"Khi khách hàng mở MỘT màn hình duy nhất, tôi cần thấy đầy đủ, chính xác mọi dữ liệu liên quan — cho dù dữ liệu đó đang nằm rải rác ở bao nhiêu dịch vụ khác nhau — trong cùng MỘT lần tải trang... và đội kỹ thuật KHÔNG được đụng vào code hay cấu hình của những dịch vụ khác... Tôi chỉ đưa cho các bạn MỘT địa chỉ duy nhất."*

- **Giải pháp**: Hiện thực hóa mô hình **Aggregator Pattern** kết hợp **OpenFeign**:
  - Client chỉ cần gọi duy nhất **1 endpoint qua Gateway**:  
    `GET http://localhost:8080/api/v1/orders/1/summary`
  - Gateway định tuyến tới `order-service`.
  - `order-service` đóng vai trò Aggregator:
    1. Truy vấn đơn hàng trong database nội bộ `order_db`.
    2. Dùng `@FeignClient(name = "user-service")` gọi lấy thông tin khách hàng từ `user-service`.
    3. Dùng `@FeignClient(name = "product-service")` gọi lấy chi tiết danh mục, tên sản phẩm từ `product-service`.
    4. Tổng hợp thành DTO `OrderSummaryDTO` và trả về toàn bộ dữ liệu trong **cùng 1 lần phản hồi**.

---

## 3. Trả lời 6 câu hỏi gợi mở của PM (Mục 6)

1. **Ai chịu trách nhiệm gộp dữ liệu?**  
   👉 **Aggregator Service** (`order-service`) chịu trách nhiệm gộp thông qua OpenFeign. API Gateway chỉ giữ vai trò Routing và Load Balancing để đảm bảo hiệu năng và không biến Gateway thành điểm nghẽn logic nghiệp vụ.
2. **Gọi đồng bộ liên tiếp, điều gì xảy ra nếu 1 service chậm/chết?**  
   👉 Thời gian đáp ứng bị cộng dồn ($T_{total} = T_1 + T_2 + T_3$). Trong phạm vi bài học (chưa dùng Circuit Breaker), nhóm đã cấu hình `connectTimeout: 5000` và `readTimeout: 5000` cho FeignClient, đồng thời bọc `try-catch` fallback cục bộ để nếu lấy thông tin phụ thất bại thì vẫn trả về dữ liệu đơn hàng chính.
3. **RestTemplate hay FeignClient?**  
   👉 Nhóm chọn **FeignClient (OpenFeign)** vì cú pháp khai báo dạng declarative interface rõ ràng, giảm thiểu code boilerplate, và tự động tích hợp Spring Cloud LoadBalancer.
4. **Địa chỉ duy nhất & nhận diện instance động?**  
   👉 Hiện thực bằng **Spring Cloud Gateway** kết hợp tiền tố `lb://<SERVICE-NAME>`. Gateway truy vấn danh bạ Eureka để resolve IP:Port thực tế theo thời gian thực.
5. **Eureka và Load Balancing phối hợp thế nào?**  
   👉 Instance mới khởi động sẽ gửi heartbeat lên Eureka. `Spring Cloud LoadBalancer` định kỳ đồng bộ danh sách instance từ Eureka và phân phối request (Round-Robin) tới các instance đang `UP`.
6. **Thông số nào đưa vào Config Server?**  
   👉 Cổng port, database url/credentials, log levels, timeout config, Eureka service url. Giúp hệ thống mở rộng và deploy đa môi trường mà không cần rebuild source code.

---

## 4. Hướng dẫn Chạy & Kiểm thử Hệ thống

### 4.1. Khởi động nhanh với PowerShell Script
Mở PowerShell tại thư mục dự án và chạy:
```powershell
# Chạy toàn bộ 6 microservices
.\run-all.ps1
```

Script sẽ tự động khởi động các cửa sổ terminal riêng biệt cho từng service theo đúng thứ tự:
1. `discovery-server` (Port 8761)
2. `config-server` (Port 8888)
3. `user-service` (Port 8081)
4. `product-service` (Port 8084)
5. `order-service` (Port 8082)
6. `api-gateway` (Port 8080)

### 4.2. Kiểm tra tự động
Sau khi các service khởi động xong (khoảng 30 giây), chạy:
```powershell
.\test-endpoints.ps1
```

### 4.3. Dừng hệ thống
Khi kiểm thử xong, chạy:
```powershell
.\stop-all.ps1
```

---

## 5. Danh sách Endpoints kiểm thử qua API Gateway (Port 8080)

| Nghiệp vụ | Phương thức | URL qua API Gateway | Mô tả |
| :--- | :--- | :--- | :--- |
| **User** | `GET` | `http://localhost:8080/api/v1/users` | Lấy danh sách tất cả người dùng |
| **User** | `GET` | `http://localhost:8080/api/v1/users/1` | Lấy chi tiết người dùng #1 |
| **Product** | `GET` | `http://localhost:8080/api/v1/products` | Lấy danh sách tất cả sản phẩm |
| **Product** | `GET` | `http://localhost:8080/api/v1/products/1` | Lấy chi tiết sản phẩm #1 |
| **Order** | `GET` | `http://localhost:8080/api/v1/orders` | Danh sách đơn hàng (thô) |
| **Order Summary** | `GET` | `http://localhost:8080/api/v1/orders/1/summary` | **Màn hình tổng quan đơn hàng (Dữ liệu tổng hợp từ 3 DB)** |

### Minh họa dữ liệu phản hồi tại `GET /api/v1/orders/1/summary`:
```json
{
  "orderId": 1,
  "orderCode": "ORD-202609-001",
  "status": "CONFIRMED",
  "totalAmount": 4640000.00,
  "note": "Giao hàng giờ hành chính, gọi trước 15 phút",
  "createdAt": "2026-09-13T07:49:00",
  "customer": {
    "id": 1,
    "fullName": "Nguyễn Văn An",
    "email": "an.nguyen@example.com",
    "phoneNumber": "0901234567",
    "address": "72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh",
    "memberTier": "VIP Gold"
  },
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "Bàn phím cơ không dây Keychron K2 Pro",
      "productCategory": "Phụ kiện công nghệ",
      "quantity": 1,
      "unitPrice": 2190000.00,
      "subTotal": 2190000.00
    },
    {
      "orderItemId": 2,
      "productId": 2,
      "productName": "Chuột không dây Logitech MX Master 3S",
      "productCategory": "Phụ kiện công nghệ",
      "quantity": 1,
      "unitPrice": 2450000.00,
      "subTotal": 2450000.00
    }
  ],
  "handledByInstance": "order-service:port=8082"
}
```
