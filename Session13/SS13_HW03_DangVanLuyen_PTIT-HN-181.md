### 1. Phân tích và điền các thông số kỳ vọng

Dựa trên thỏa thuận SLA và yêu cầu đề bài:

* **Loại cửa sổ:** `TIME_BASED`
* **Kích thước cửa sổ (sliding-window-size):** `30` (tính bằng giây)
* **Thời gian chờ khi ngắt mạch (wait-duration-in-open-state):** `20s` (ngưng gọi trong đúng 20 giây trước khi chuyển sang HALF_OPEN)
* **Số request thử nghiệm (permitted-number-of-calls-in-half-open-state):** `3` (gửi thử 3 đơn hàng qua khi ở trạng thái HALF_OPEN)
* **Tự động chuyển trạng thái (automatic-transition-from-open-to-half-open-enabled):** `true`
* **Ngưỡng lỗi để phục hồi về CLOSED:** Cả 3 request đều phải thành công $\rightarrow$ Ngưỡng tỷ lệ lỗi `failure-rate-threshold` đặt dưới mức cho phép 1 lỗi (ví dụ: `1` đến `33`, hoặc đặt mặc định lỗi 0%). Nếu để `failure-rate-threshold: 50` thì chỉ cần 1 lỗi (1/3 = 33.3% < 50%) mạch vẫn sẽ CLOSED, vì vậy để đảm bảo "cả 3 đều thành công" thì cần đặt ngưỡng lỗi nhỏ hơn hoặc bằng 33.3% (thường đặt `1` đến `30` hoặc `33.3%`).

---

### 2. Cấu hình `application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      shippingClient:
        # Cửa sổ trượt theo thời gian 30 giây
        sliding-window-type: TIME_BASED
        sliding-window-size: 30
        
        # Số cuộc gọi tối thiểu trong cửa sổ trượt để bắt đầu tính toán
        minimum-number-of-calls: 5
        
        # Ngưỡng lỗi: Để cả 3 request ở HALF_OPEN đều phải thành công mới đóng mạch,
        # ngưỡng lỗi phải < 33.3% (nếu 1 request lỗi = 33.3% >= 30% -> mạch quay lại OPEN ngay)
        failure-rate-threshold: 30
        
        # SLA: Ngưng gọi trong đúng 20 giây khi sập (OPEN -> HALF_OPEN)
        wait-duration-in-open-state: 20s
        
        # SLA: Gửi thử đúng 3 request khi ở HALF_OPEN
        permitted-number-of-calls-in-half-open-state: 3
        
        # Tự động hé mạch sang HALF_OPEN sau 20s mà không cần request đầu tiên kích hoạt
        automatic-transition-from-open-to-half-open-enabled: true

```

---

### 3. Giải thích chi tiết cơ chế hoạt động

* **`automaticTransitionFromOpenToHalfOpenEnabled = true`:**
* Mặc định trong Resilience4j, khi hết thời gian `wait-duration-in-open-state`, mạch vẫn ở trạng thái OPEN thụ động cho đến khi có một request mới đến đóng vai trò "mồi" để kích hoạt chuyển sang HALF_OPEN.
* Khi bật cờ này lên `true`, một background thread scheduler nội bộ sẽ tự động đẩy trạng thái Circuit Breaker sang **HALF_OPEN** ngay khi hết 20 giây.


* **`permitted-number-of-calls-in-half-open-state = 3`:**
* Khi mạch ở HALF_OPEN, bộ đếm chỉ cấp quota cho đúng **3 request**.
* Nếu bạn bắn đồng thời 10 request: 3 request đầu tiên được cấp phép đi tới `Shipping-Service`, 7 request còn lại bị từ chối ngay lập tức tại proxy với ngoại lệ `CallNotPermittedException`.
* Nếu cả 3 request thử nghiệm trả về thành công $\rightarrow$ Tỷ lệ lỗi là 0% ($\le 30\%$) $\rightarrow$ Mạch tự động chuyển về trạng thái bình thường (**CLOSED**).