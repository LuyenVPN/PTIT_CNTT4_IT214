### I. Danh sách các lỗi trong đoạn mã ban đầu

Trong lớp `StockCheckClient` ban đầu của `inventory-service`, tồn tại 4 lỗi kỹ thuật nghiêm trọng:

| STT | Vị trí / Lỗi | Nguyên nhân kỹ thuật | Hậu quả thực tế |
| --- | --- | --- | --- |
| **1** | **Thiếu Timeout (Nguy hiểm nhất)** | `RestTemplate` mặc định sử dụng `SimpleClientHttpRequestFactory` không giới hạn thời gian chờ socket (`connectTimeout` và `readTimeout` vô hạn). | Thread của ứng dụng bị treo vô hạn ở trạng thái `WAITING` nếu dịch vụ đích không phản hồi. |
| **2** | **Hardcode IP & Port (`192.168.0.12:8082`)** | Gán cứng địa chỉ mạng nội bộ của một máy cụ thể thay vì dùng định danh dịch vụ (`service-id`). | Mất tính linh hoạt; hệ thống lập tức sập khi instance đó đổi IP trong môi trường Docker/Kubernetes hoặc khi scale thêm instance. |
| **3** | **Thiếu `@LoadBalanced**` | Bean `RestTemplate` không được kích hoạt các Interceptor điều hướng của Spring Cloud Discovery. | Không thể tra cứu vị trí các service qua Eureka/Consul và không phân bổ được tải (Client-side Load Balancing). |
| **4** | **Thiếu Fallback & Exception Handling** | Không bắt ngoại lệ mạng `ResourceAccessException`. | Mọi sự cố mạng từ phía `product-service` sẽ văng trực tiếp mã lỗi HTTP 500 ra ngoài, làm gián đoạn luồng đặt hàng. |

---

### II. Cơ chế lan truyền lỗi (Cascading Failure) theo từng bước

Sự cố hệ thống lan rộng theo hiệu ứng Domino qua 5 giai đoạn:

```text
[product-service] (Nghẽn/Treo)
       ▲
       │  (Treo socket vô hạn)
[inventory-service] (50 req/s -> Cháy 200 worker threads sau 4s)
       ▲
       │  (Nghẽn dây chuyền)
[order-service] (Hàng đợi đầy -> Treo luồng theo)
       ▲
       │
[NGƯỜI DÙNG CUỐI] (Sập toàn bộ chức năng mua sắm)

```

* **Bước 1 — Khởi phát sự cố tại `product-service`:** `product-service` bị quá tải CPU, nghẽn truy vấn cơ sở dữ liệu hoặc gặp Deadlock, khiến thời gian phản hồi một request kéo dài bất thường (> 30 giây) hoặc treo hoàn toàn.
* **Bước 2 — Giữ luồng tại `inventory-service`:** Khi `inventory-service` gửi request sang `product-service`, do thiếu `readTimeout`, Tomcat worker thread tại `inventory-service` bị giữ lại chờ dữ liệu từ TCP socket.
* **Bước 3 — Cạn kiệt Thread Pool (Thread Pool Exhaustion):**
* Vào giờ cao điểm, tần suất gọi API là **50 request/giây**.
* Máy chủ nhúng Tomcat mặc định chỉ có tối đa **200 worker threads** (`server.tomcat.threads.max=200`).
* Khi mỗi request bị block quá 4 giây:
  $$\text{Số thread bị kẹt} = 50 \text{ req/s} \times 4 \text{ s} = 200 \text{ threads}$$


* Toàn bộ thread pool của `inventory-service` bị lấp đầy. Hàng đợi (Queue) quá tải, `inventory-service` từ chối tất cả request mới đến (kể cả những API hoàn toàn không liên quan đến kiểm tra kho).


* **Bước 4 — Lan truyền ngược sang `order-service`:** `order-service` gọi `inventory-service` để xác nhận đơn nhưng không nhận được phản hồi. Các luồng xử lý đơn hàng của `order-service` tiếp tục bị giữ lại và nghẽn theo.
* **Bước 5 — Sụp đổ toàn diện hệ thống:** Hiện tượng tắc nghẽn lan đến API Gateway, làm tê liệt toàn bộ luồng mua sắm của VietMart.

---

### III. Tóm tắt giải pháp khắc phục mã nguồn

1. **Khởi tạo Bean `@LoadBalanced RestTemplate` chuẩn:**
* `connectTimeout = 1000ms` (1 giây): Giới hạn thời gian bắt tay TCP.
* `readTimeout = 2000ms` (2 giây): Giới hạn thời gian tối đa chờ `product-service` trả kết quả.


2. **Loại bỏ Hardcode:** Chuyển URL thành dạng `http://product-service/api/stock/{pid}` để Eureka và Spring Cloud LoadBalancer tự động điều phối tải.
3. **Cơ chế Fallback an toàn:** Đóng gói khối `try-catch` bắt `ResourceAccessException`. Khi chạm ngưỡng 2 giây timeout hoặc mất mạng, hàm lập tức trả về `StockInfo.unavailable(productId)` (`isFallback=true`, `available=false`) thay vì làm sập tiến trình.

---

### IV. Kết quả thực nghiệm kiểm thử tích hợp (WireMock)

**Kịch bản kiểm thử:**

* Dựng máy chủ giả lập WireMock Server phản hồi chậm **5000ms** (5 giây).
* `StockCheckClient` gửi request với cấu hình `readTimeout = 2s`.

**Dữ liệu thực nghiệm thu được:**

```text
====== KẾT QUẢ THỰC NGHIỆM WIREMOCK ======
Thời gian bắt đầu gọi : 0 ms
Thời gian nhận kết quả: 2018 ms
Trạng thái trả về     : StockInfo[available=false, isFallback=true]
Ngoại lệ nội bộ       : ResourceAccessException (SocketTimeoutException)
Trạng thái kiểm thử   : PASSED (Tất cả Assertions thành công)
==========================================

```

**Đánh giá:**

* Thay vì bị treo đơ suốt 5 giây theo WireMock Server, `RestTemplate` đã chủ động ngắt kết nối chính xác sau **2018ms** ($\approx 2\text{s}$).
* Thời gian thực thi thỏa mãn tiêu chuẩn đề bài ($2\text{s} \le \text{thời gian thực tế} < 3\text{s}$), giải phóng luồng xử lý và kích hoạt dữ liệu dự phòng an toàn.

---

### V. Đánh giá rủi ro tồn dư đối với `order-service`

**Vấn đề đặt ra:** Sau khi đã cấu hình Timeout 2s, `order-service` có tiếp tục bị ảnh hưởng nếu `inventory-service` bị quá tải request không?

**Kết luận: CÓ, nguy cơ nghẽn dây chuyền vẫn tồn tại.**

**Phân tích nguyên nhân:**

* Dù đã có timeout 2s, nếu `product-service` sập hoàn toàn, **mỗi request gửi sang vẫn phải chiếm dụng 1 worker thread trong đủ 2 giây** trước khi ngắt.
* Với lưu lượng 50 req/s, số luồng bị giam giữ liên tục ở trạng thái chờ là:
  $$50 \text{ req/s} \times 2 \text{ s} = 100 \text{ threads}$$


* 100 thread này chiếm tới **50% dung lượng thread pool** của Tomcat. Nếu lưu lượng giờ cao điểm tăng vọt lên 100 req/s, toàn bộ 200 thread vẫn sẽ bị vét cạn.
* Thời gian phản hồi của `inventory-service` đối với `order-service` bị kéo dài thêm 2 giây cho mỗi đơn hàng, khiến hàng đợi tại `order-service` tiếp tục phình to và gây nghẽn tích lũy ngược.

---

### VI. Đề xuất các giải pháp kỹ thuật bổ sung

Để giải quyết triệt để rủi ro trên, cần áp dụng thêm các mẫu kiến trúc chịu lỗi (Resilience Patterns):

* **Circuit Breaker Pattern (Resilience4j):**
* Thiết lập bộ ngắt mạch giám sát tỷ lệ timeout. Khi tỷ lệ thất bại vượt ngưỡng (ví dụ: > 50% trong 10 giây), Circuit Breaker chuyển sang trạng thái **OPEN**.
* Ở trạng thái OPEN, mọi request tiếp theo sẽ được trả về fallback ngay lập tức (**Fail-Fast trong 0ms**) mà không thực hiện gọi qua mạng, không tiêu tốn 2 giây chờ đợi, bảo vệ tuyệt đối thread pool.


* **Bulkhead Pattern (Vách ngăn cách ly tài nguyên):**
* Tách riêng một thread pool hoặc semaphore giới hạn (ví dụ tối đa 20 luồng đồng thời) chuyên dùng để gọi `product-service`.
* Khi `product-service` gặp sự cố, tối đa chỉ có 20 luồng bị kẹt. 180 luồng còn lại của `inventory-service` vẫn xử lý thông suốt cho các tác vụ khác.


* **Distributed Cache với Stale Data (Redis):**
* Đệm thông tin tồn kho vào Redis. Khi `product-service` không phản hồi kịp, hệ thống lập tức đọc dữ liệu cũ gần nhất từ Redis trả về cho `order-service` thay vì báo lỗi không khả dụng.