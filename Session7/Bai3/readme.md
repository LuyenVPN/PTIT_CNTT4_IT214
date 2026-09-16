# FinBank - Bài tập Tổng hợp 3: Giao tiếp đồng bộ bằng RestTemplate

## 1. Mục tiêu

Nâng cấp hệ thống từ Bài 2 để thực hiện nghiệp vụ chuyển tiền bằng giao tiếp đồng bộ:

- Account Service cung cấp API lấy tài khoản, lấy số dư, debit và credit.
- Transaction Service dùng RestTemplate để gọi Account Service.
- RestTemplate được cấu hình với `@LoadBalanced`.
- Transaction Service gọi Account Service bằng Eureka service name `account-service`.
- Không hard-code URL của Account Service trong Transaction Service.
- Transaction được lưu vào database với `SUCCESS` hoặc `FAILED`.
- Client bên ngoài gọi qua API Gateway port `8222`.

## 2. Các service

| Service | Port | Vai trò |
|---|---:|---|
| Eureka Server | 8761 | Service Discovery |
| API Gateway | 8222 | Gateway |
| Customer Service | 8081 | Service từ Bài 2 |
| Account Service | 8082 | Quản lý tài khoản |
| Transaction Service | 8083 | Nghiệp vụ chuyển tiền |

Lưu ý: Bài 2 ban đầu có Transaction Service trùng port với Account Service. Bài này đã đổi Transaction Service sang `8083` để hai service có thể chạy đồng thời.

## 3. MySQL

Account Service:

```text
database: finbank_account
```

Transaction Service:

```text
database: finbank_transaction
```

Trong `application.yaml` hiện đang dùng:

```yaml
username: root
password: luyen123
```

Nếu mật khẩu MySQL của máy khác, sửa lại ở:

```text
account-service/src/main/resources/application.yaml
transaction-service/src/main/resources/application.yaml
```

## 4. Thứ tự chạy

1. MySQL
2. Eureka Server - `8761`
3. Customer Service - `8081` (nếu muốn giữ service của Bài 2)
4. Account Service - `8082`
5. Transaction Service - `8083`
6. API Gateway - `8222`

Mở:

```text
http://localhost:8761
```

Phải thấy các service đã đăng ký, đặc biệt:

```text
ACCOUNT-SERVICE
TRANSACTION-SERVICE
API-GATEWAY
```

## 5. Account Service API

Tất cả request kiểm thử bên ngoài đi qua Gateway.

### Lấy thông tin tài khoản

```http
GET http://localhost:8222/api/accounts/1001
```

### Lấy số dư

```http
GET http://localhost:8222/api/accounts/1001/balance
```

### Debit

```http
PUT http://localhost:8222/api/accounts/1001/debit
Content-Type: application/json
```

```json
{
  "amount": 2000000
}
```

### Credit

```http
PUT http://localhost:8222/api/accounts/1002/credit
Content-Type: application/json
```

```json
{
  "amount": 2000000
}
```

## 6. Tạo dữ liệu test

### Account 1001

```http
POST http://localhost:8222/api/accounts
```

```json
{
  "accountNumber": "1001",
  "balance": 10000000,
  "customerName": "Nguyen Van A"
}
```

### Account 1002

```http
POST http://localhost:8222/api/accounts
```

```json
{
  "accountNumber": "1002",
  "balance": 5000000,
  "customerName": "Nguyen Van B"
}
```

## 7. RestTemplate + @LoadBalanced

File:

```text
transaction-service/src/main/java/com/example/transactionservice/config/AppConfig.java
```

Cấu hình:

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

Transaction Service gọi:

```text
http://account-service/api/accounts/...
```

Không gọi:

```text
http://localhost:8082/api/accounts/...
```

`account-service` là Eureka service name được khai báo:

```yaml
spring:
  application:
    name: account-service
```

## 8. Luồng chuyển tiền

```text
Postman
   |
   | POST :8222/api/transactions/transfer
   v
API Gateway :8222
   |
   v
Transaction Service :8083
   |
   | RestTemplate + @LoadBalanced
   v
Eureka
   |
   v
Account Service :8082
   |
   +--> kiểm tra tài khoản nguồn
   |
   +--> kiểm tra số dư nguồn
   |
   +--> kiểm tra tài khoản đích
   |
   +--> debit tài khoản nguồn
   |
   +--> credit tài khoản đích
   |
   v
Transaction Service
   |
   v
Transaction DB
```

## 9. API Transfer

```http
POST http://localhost:8222/api/transactions/transfer
Content-Type: application/json
```

```json
{
  "fromAccountNumber": "1001",
  "toAccountNumber": "1002",
  "amount": 2000000,
  "description": "Chuyen tien thanh toan hoa don"
}
```

## 10. Test case 1

Dữ liệu ban đầu:

```text
1001 = 10,000,000
1002 = 5,000,000
```

Chuyển:

```text
1001 -> 1002
2,000,000
```

Kết quả:

```text
1001 = 8,000,000
1002 = 7,000,000
status = SUCCESS
```

## 11. Test case 2

Chuyển:

```text
1001 -> 1002
100,000,000
```

Kết quả:

```text
status = FAILED
message = Tài khoản nguồn không đủ số dư
```

Không thực hiện debit.

## 12. Test case 3

Chuyển:

```text
1001 -> 9999
1,000,000
```

Transaction Service kiểm tra nguồn và sau đó kiểm tra đích. Tài khoản `9999` không tồn tại nên giao dịch:

```text
status = FAILED
```

Không thực hiện debit tài khoản 1001.

## 13. Postman

Collection:

```text
postman/FinBank-Bai3-RestTemplate.postman_collection.json
```

Import file này vào Postman.

Biến:

```text
baseUrl = http://localhost:8222
```

## 14. Xử lý lỗi

Transaction Service lưu cả:

```text
status
message
```

Ví dụ:

```json
{
  "status": "FAILED",
  "message": "Tài khoản nguồn không đủ số dư"
}
```

hoặc:

```json
{
  "status": "FAILED",
  "message": "...9999..."
}
```

Ngoài ra, nếu debit thành công nhưng credit thất bại, code có best-effort compensation để cộng lại tiền vào tài khoản nguồn.

Đây chưa phải distributed transaction/Saga hoàn chỉnh. `@Transactional` của Transaction Service không thể tự rollback database của Account Service.

## 15. Nộp bài

Repository nên có:

```text
Bai3/
├── account-service/
├── transaction-service/
├── api-gateway/
├── customer-service/
├── eureka-server/
├── postman/
│   └── FinBank-Bai3-RestTemplate.postman_collection.json
└── README.md
```

Push source lên Git và nộp link repository qua LMS.
