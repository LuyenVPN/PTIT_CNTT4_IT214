# FoodX — Centralized Configuration Repository

Kho cấu hình tập trung cho hệ thống **FoodX** sử dụng **Spring Cloud Config Server**.  
Tất cả các microservice kéo cấu hình từ repository này thay vì nhúng trực tiếp vào source code.

---

## 1. Lý do tách cấu hình ra khỏi Source Code

| Vấn đề khi nhúng config vào code | Giải pháp với Centralized Config |
|---|---|
| Phải build lại app khi thay đổi cấu hình | Thay đổi config không cần rebuild |
| Mật khẩu lộ trong Git history của code | Chỉ một repo config được kiểm soát chặt |
| Khó quản lý cấu hình cho nhiều môi trường | Profile-based files rõ ràng, dễ audit |
| Config bị phân tán ở nhiều repo | Một nguồn sự thật duy nhất |

---

## 2. Cấu trúc thư mục Git Repository

```
foodx-config-repo/               <- Git repository này
|
+-- README.md                     <- File hướng dẫn (file này)
|
+-- application.yml               <- Cấu hình CHUNG cho tất cả service
|                                    (log format, actuator, tracing, ...)
|
+-- configs/
    +-- restaurant-service.yml    <- Cấu hình riêng cho restaurant-service
    +-- order-service.yml         <- Cấu hình riêng cho order-service
    +-- delivery-service.yml      <- Cấu hình riêng cho delivery-service
```

> **Quy ước đặt tên (bắt buộc):**
> Tên file **phải khớp** với giá trị `spring.application.name` của service đó.
> Config Server tra cứu theo thứ tự ưu tiên:
> 1. `{appName}-{profile}.yml`  -> ví dụ: `restaurant-service-prod.yml`
> 2. `{appName}.yml`            -> ví dụ: `restaurant-service.yml`
> 3. `application-{profile}.yml`
> 4. `application.yml`

---

## 3. Phân tích Lỗi ban đầu và Cách Khắc phục

### Lỗi 1 — Đặt tên file sai quy ước

**Tình trạng ban đầu:** File được đặt tên là `config.yml`.

**Hậu quả:**
Config Server tìm file theo pattern `{spring.application.name}.yml`.
Vì `spring.application.name = "restaurant-service"`, server sẽ tìm `restaurant-service.yml`.
Không thấy file -> service khởi động với **toàn bộ giá trị mặc định** (hoặc fail nếu thiếu cấu hình bắt buộc), không kết nối được database, sai port, v.v.

**Khắc phục:** Đổi tên file thành `restaurant-service.yml`.

---

### Lỗi 2 — Lưu mật khẩu dạng Plaintext

**Tình trạng ban đầu:**
```yaml
password: RestaurantPass123
```

**Rủi ro bảo mật:**
- Bất kỳ ai có quyền đọc Git repository đều thấy mật khẩu ngay lập tức.
- Mật khẩu tồn tại vĩnh viễn trong **Git history** — xóa file không xóa được lịch sử.
- Nếu repo bị leak (public GitHub, internal breach), database lập tức bị lộ.
- Vi phạm các tiêu chuẩn bảo mật như PCI-DSS, ISO 27001.

**Khắc phục:** Sử dụng cú pháp `{cipher}` của Spring Cloud Config:
```yaml
password: "{cipher}AQB7k9mXpL2nVqRtYwZsHjDcFuOeIbGa..."
```
Config Server sẽ **tự động giải mã** giá trị trước khi trả về cho service client.
Chuỗi sau `{cipher}` là kết quả mã hóa bằng **symmetric key** (hoặc RSA key pair) được cấu hình riêng cho Config Server — **key này không lưu trong repo**.

---

## 4. Danh sách File Cấu hình

| File | Service | Port | Database |
|---|---|---|---|
| `configs/restaurant-service.yml` | restaurant-service | 8085 | restaurants_db |
| `configs/order-service.yml` | order-service | 8086 | orders_db |
| `configs/delivery-service.yml` | delivery-service | 8087 | delivery_db |

---

## 5. Cách Config Server phục vụ cấu hình

```
[restaurant-service] --bootstrap--> [Config Server]
                                          |
                             clone/pull   |
                                          v
                               [foodx-config-repo] (Git)
                                          |
                             tìm file:    |  restaurant-service.yml
                                          |  application.yml
                                          v
                              merge & decrypt {cipher}
                                          |
                              ────────────┘
                              trả về cấu hình đã giải mã
                                          |
                              ────────────v
                          [restaurant-service khởi động]
```

### Cấu hình bootstrap của mỗi service (trong source code)

```yaml
# bootstrap.yml (nằm trong source code của từng service)
spring:
  application:
    name: restaurant-service        # <- phải khớp tên file config
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true               # Dừng khởi động nếu không lấy được config
```

---

## 6. Quy tắc Bảo mật bắt buộc

1. **Không bao giờ** commit mật khẩu, API key, secret dạng plaintext.
2. Mọi giá trị nhạy cảm phải dùng cú pháp `{cipher}...`.
3. Encryption key của Config Server được truyền qua **environment variable**, không lưu trong repo.
4. Repository này phải được đặt ở chế độ **private** và chỉ Config Server mới có deploy key.
5. Bật **branch protection** — mọi thay đổi phải qua Pull Request và được review.

---

## 7. Hướng dẫn mã hóa mật khẩu mới (cho DevOps)

```bash
# Gọi endpoint /encrypt của Config Server để lấy chuỗi đã mã hóa
curl -X POST http://config-server:8888/encrypt \
     -H "Content-Type: text/plain" \
     -d "MyNewSecretPassword"

# Output mẫu:
# AQB7k9mXpL2nVqRtYwZsHjDcFuOeIbGa...

# Sau đó dán vào file config:
# password: "{cipher}AQB7k9mXpL2nVqRtYwZsHjDcFuOeIbGa..."
```

---
