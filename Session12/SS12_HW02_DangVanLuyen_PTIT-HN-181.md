**Phần 1 – Phân tích quy tắc chữ ký (signature) của phương thức Fallback**

Trong Resilience4j, phương thức Fallback phải tuân thủ nghiêm ngặt các quy tắc sau:

* **Kiểu dữ liệu trả về (Return type):** Phải tương đồng hoặc là lớp cha (compatible) với kiểu trả về của phương thức gốc.
* **Danh sách tham số:** Phải giữ nguyên toàn bộ tham số của phương thức gốc theo đúng thứ tự.
* **Tham số ngoại lệ:** Bắt buộc phải thêm một tham số ngoại lệ (`Throwable` hoặc exception cụ thể) ở **vị trí cuối cùng** của danh sách tham số.
* **Vị trí và quyền truy cập:** Thường nằm trong cùng class với phương thức gốc (nếu dùng cơ chế proxy Spring AOP mặc định) và có access modifier phù hợp (thường là `public` hoặc cùng package).

---

**Phần 2 – Thực thi & Xử lý bẫy dữ liệu**

Dưới đây là đoạn mã hoàn chỉnh triển khai phương thức gọi `Banner-Service` cùng hàm `fallbackMethod`:

```java
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class HomeBannerService {

    private static final Logger log = LoggerFactory.getLogger(HomeBannerService.class);

    // Bổ sung thuộc tính fallbackMethod trỏ tới hàm xử lý dự phòng
    @CircuitBreaker(name = "bannerService", fallbackMethod = "getDefaultBannersFallback")
    public List<String> getBanners() {
        // Giả lập gọi sang Banner-Service qua HTTP/FeignClient
        return callRemoteBannerService();
    }

    /**
     * Phương thức Fallback bảo vệ giao diện
     * Giữ nguyên kiểu trả về List<String>, nhận Throwable ở tham số cuối cùng
     */
    public List<String> getDefaultBannersFallback(Throwable throwable) {
        // Phân loại nguyên nhân lỗi để ghi log chi tiết
        if (throwable instanceof CallNotPermittedException) {
            log.warn("Circuit Breaker đang ở trạng thái OPEN (Mở mạch), chặn gọi Banner-Service. Chi tiết: {}", 
                     throwable.getMessage());
        } else if (throwable instanceof IOException) {
            log.error("Lỗi kết nối mạng khi gọi Banner-Service (Network/IO Failure): {}", 
                      throwable.getMessage());
        } else {
            log.error("Banner-Service gặp sự cố không xác định: {}", 
                      throwable.getMessage(), throwable);
        }

        // Kế hoạch B: Trả về banner mặc định để trang chủ không bị lỗi hoặc trắng tinh
        return List.of("Freeship mọi đơn hàng");
    }

    private List<String> callRemoteBannerService() {
        // Logic gọi remote service thực tế
        return List.of("Khuyến mãi mùa hè giảm 50%", "Flash sale công nghệ");
    }
}

```