## Phần 1 – Báo cáo phân tích

Circuit Breaker **không mở** dù cả 5 request đều lỗi vì Resilience4j có tham số `minimumNumberOfCalls`.

`minimumNumberOfCalls` quy định **số lượng request tối thiểu phải được ghi nhận trong Sliding Window trước khi Circuit Breaker bắt đầu tính Failure Rate để quyết định có chuyển sang trạng thái OPEN hay không**.

Nếu không cấu hình, giá trị mặc định của `minimumNumberOfCalls` là **100**. Trong khi đó, hệ thống chỉ có 5 request và cả 5 đều lỗi, nên chưa đạt số lượng request tối thiểu để tính Failure Rate.

Do đó:

```text
5 request lỗi / 5 request = 100% lỗi
```

nhưng Circuit Breaker vẫn **CLOSED** vì:

```text
5 < minimumNumberOfCalls
```

Để hệ thống nhạy bén hơn, có thể cấu hình `minimumNumberOfCalls: 5`.

Tuy nhiên, cần chú ý điều kiện:

```text
minimumNumberOfCalls >= permittedNumberOfCallsInHalfOpenState
```

Trong bài:

```text
minimumNumberOfCalls = 5
permittedNumberOfCallsInHalfOpenState = 3
```

nên:

```text
5 >= 3
```

Điều kiện được đảm bảo.

---

## Phần 2 – Triển khai

Để cầu dao phản ứng nhanh với lượng traffic thấp mà vẫn tuân thủ điều kiện ràng buộc `minimumNumberOfCalls >= permittedNumberOfCallsInHalfOpenState` (trong đó `permittedNumberOfCallsInHalfOpenState = 3`), ta thiết lập `minimumNumberOfCalls = 5` (hoặc giá trị hợp lý từ 3 đến 5). Khi đó, chỉ cần 5 cuộc gọi ban đầu thất bại (5/5 = 100% > 50%), cầu dao sẽ lập tức ngắt mạch sang OPEN.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      bankClient:
        # Kiểu cửa sổ trượt đếm số lượng request
        slidingWindowType: COUNT_BASED
        # Kích thước cửa sổ trượt: 20 request gần nhất
        slidingWindowSize: 20
        # Số lượng request tối thiểu để bắt đầu tính toán tỷ lệ lỗi (thỏa mãn >= 3)
        minimumNumberOfCalls: 5
        # Ngưỡng tỷ lệ lỗi kích hoạt mở mạch: 50%
        failureRateThreshold: 50
        # Thời gian duy trì trạng thái OPEN trước khi sang HALF_OPEN: 30 giây
        waitDurationInOpenState: 30s
        # Số request trinh sát được phép đi qua khi ở trạng thái HALF_OPEN
        permittedNumberOfCallsInHalfOpenState: 3
        # Tự động chuyển từ OPEN sang HALF_OPEN khi hết 30s mà không cần đợi request kế tiếp
        automaticTransitionFromOpenToHalfOpenEnabled: true

```