Cấu hình đầy đủ trong file `application.yml` theo chuẩn Resilience4j và code minh họa để đáp ứng yêu cầu:

### 1. Cấu hình `application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      ewalletClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        record-exceptions:
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.example.exception.InsufficientBalanceException

```

> **Lưu ý đường dẫn package:** Thay `com.example.exception.InsufficientBalanceException` bằng package thực tế chứa class ngoại lệ của bạn.

---

### 2. Định nghĩa các Exception và Service (Tham khảo)

**Tạo Custom Exception:**

```java
package com.example.exception;

// Ngoại lệ nghiệp vụ - lỗi 400 (bị ignore, không tính vào failure rate)
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

```

**Áp dụng `@CircuitBreaker` trên Service:**

```java
@Service
public class CheckoutService {

    @CircuitBreaker(name = "ewalletClient", fallbackMethod = "handlePaymentFallback")
    public String deductMoney(String accountId, double amount) throws TimeoutException {
        // Gọi sang EWallet-Service
        // Nếu số dư không đủ -> throw new InsufficientBalanceException("Số dư không đủ");
        // Nếu timeout -> throw new TimeoutException("Kết nối ví thất bại");
        return "Thanh toán thành công";
    }

    // Fallback method khi mạch OPEN (ném CallNotPermittedException) hoặc gặp TimeoutException
    public String handlePaymentFallback(String accountId, double amount, Throwable throwable) {
        return "Dịch vụ thanh toán tạm thời gián đoạn. Vui lòng thử lại sau!";
    }
}

```

---

### 3. Giải thích cơ chế hoạt động theo Checklist

* **8 request `InsufficientBalanceException`:**
* Nhờ cấu hình `ignore-exceptions`, toàn bộ các request này không bị tính vào tổng số request của cửa sổ trượt và không bị tính là lỗi (`failed`).
* Trạng thái mạch giữ nguyên **CLOSED**.


* **5 request liên tiếp `TimeoutException`:**
* Thuộc tính `minimum-number-of-calls: 5` đã được thỏa mãn (đủ 5 request).
* Thuộc tính `record-exceptions` ghi nhận cả 5 request đều là lỗi $\rightarrow$ Tỷ lệ lỗi là $5/5 = 100\%$.
* $100\% \ge 50\%$ (`failure-rate-threshold`) $\rightarrow$ Mạch lập tức chuyển sang trạng thái **OPEN**.


* **1 request hợp lệ tiếp theo khi đang OPEN:**
* Circuit Breaker chặn cuộc gọi ngay tại proxy mà không gửi request sang `EWallet-Service`.
* Ném ngay lập tức ngoại lệ `io.github.resilience4j.circuitbreaker.CallNotPermittedException`.