# FinBank - Bài tập tổng hợp 3: Giao tiếp đồng bộ bằng RestTemplate

## 1. Mục tiêu

Project này được nâng cấp từ Bài 2 để đáp ứng Bài tập Tổng hợp 3:

- Account Service cung cấp API lấy tài khoản, lấy số dư, debit và credit.
- Transaction Service gọi Account Service bằng `RestTemplate`.
- `RestTemplate` được cấu hình với `@LoadBalanced`.
- Transaction Service gọi Account Service qua Eureka Service ID `account-service`, không hard-code `localhost:port`.
- Transaction được lưu vào MySQL với trạng thái `SUCCESS` hoặc `FAILED`.
- API bên ngoài được gọi qua API Gateway port `8222`.

## 2. Các service

| Service | Port | Vai trò |
|---|---:|---|
| Eureka Server | 8761 | Service Discovery |
| API Gateway | 8222 | Gateway |
| Account Service | 8082 | Quản lý tài khoản |
| Transaction Service | 8083 | Chuyển tiền |
| Customer Service | 8081 | Giữ lại từ Bài 2 |

> Account Service vẫn có thể chạy nhiều instance. Instance khác có thể ghi đè port bằng `--server.port=8092`, `--server.port=8102`.

## 3. Cấu hình MySQL

Account Service sử dụng database:

```text
finbank_account
```

Transaction Service sử dụng database:

```text
finbank_transaction
```

Trong hai file `application.yaml`, thay:

```yaml
password: YOUR_MYSQL_PASSWORD
```

bằng mật khẩu MySQL của máy.

Có thể tạo database thủ công:

```sql
create database finbank_account;
create database finbank_transaction;
```

Nếu dùng `createDatabaseIfNotExist=true`, MySQL cũng có thể tự tạo database khi kết nối.

## 4. Thứ tự chạy

1. MySQL
2. Eureka Server - `8761`
3. Account Service - `8082`
4. Transaction Service - `8083`
5. API Gateway - `8222`

Mở:

```text
http://localhost:8761
```

Kiểm tra Eureka phải thấy ít nhất:

```text
ACCOUNT-SERVICE
TRANSACTION-SERVICE
API-GATEWAY
```

## 5. Account API

Tất cả API bên ngoài được gọi qua Gateway.

### Lấy tài khoản

```http
GET http://localhost:8222/api/accounts/1001
```

### Lấy số dư

```http
GET http://localhost:8222/api/accounts/1001/balance
```

### Tạo tài khoản

```http
POST http://localhost:8222/api/accounts
Content-Type: application/json
```

```json
{
  "accountNumber": "1001",
  "balance": 10000000,
  "customerName": "Nguyen Van A"
}
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

## 6. RestTemplate + Eureka

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

thay vì:

```text
http://localhost:8082/api/accounts/...
```

`account-service` là giá trị:

```yaml
spring:
  application:
    name: account-service
```

Eureka cung cấp địa chỉ instance cho LoadBalancer.

## 7. Luồng chuyển tiền

```text
Postman
   |
   v
API Gateway :8222
   |
   v
Transaction Service
   |
   | RestTemplate + @LoadBalanced
   v
Eureka
   |
   v
Account Service
   |
   +--> kiểm tra tài khoản nguồn
   |
   +--> kiểm tra tài khoản đích
   |
   +--> kiểm tra số dư
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

## 8. API chuyển tiền

```http
POST http://localhost:8222/api/transactions/transfer
Content-Type: application/json
```

Body:

```json
{
  "fromAccountNumber": "1001",
  "toAccountNumber": "1002",
  "amount": 2000000,
  "description": "Chuyen tien thanh toan hoa don"
}
```

## 9. Test case 1 - Thành công

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

## 10. Test case 2 - Không đủ số dư

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

## 11. Test case 3 - Tài khoản đích không tồn tại

Chuyển:

```text
1001 -> 9999
1,000,000
```

Kết quả:

```text
status = FAILED
```

Không thực hiện debit tài khoản nguồn.

## 12. Postman

Collection nằm tại:

```text
postman/FinBank-Bai3-RestTemplate.postman_collection.json
```

Import collection vào Postman.

Biến:

```text
baseUrl = http://localhost:8222
```

## 13. Lưu ý kiến trúc

Đây là giao tiếp đồng bộ giữa các Microservice. `@Transactional` trong Transaction Service không thể rollback database của Account Service.

Code có best-effort compensation nếu debit thành công nhưng credit thất bại, nhưng đây chưa phải distributed transaction/Saga hoàn chỉnh. Phạm vi này phù hợp với yêu cầu bài tập RestTemplate cơ bản.

## 14. Các file quan trọng được thêm/sửa

### Account Service

```text
entity/Account.java
dto/AmountRequest.java
repository/AccountRepository.java
service/AccountService.java
controller/AccountController.java
application.yaml
build.gradle
```

### Transaction Service

```text
config/AppConfig.java
dto/TransferRequest.java
dto/AccountResponse.java
dto/AmountRequest.java
entity/Transaction.java
repository/TransactionRepository.java
service/TransactionService.java
controller/TransactionController.java
application.yaml
build.gradle
```

### API Gateway

Giữ route:

```text
/api/accounts/**       -> lb://account-service
/api/transactions/**   -> lb://transaction-service
```

và bổ sung dependency LoadBalancer.

## 15. Các bước nộp bài

- Push source code lên Git.
- Giữ `README.md`.
- Giữ Postman Collection trong thư mục `postman/`.
- Kiểm tra 3 test case trước khi nộp.
