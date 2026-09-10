**Bảng so sánh số lượng code và đặc điểm:**

| Tiêu chí | `ProductServiceClientRT` (RestTemplate) | `ProductClient` (FeignClient) |
| :--- | :--- | :--- |
| **Mô hình triển khai** | **Imperative:** Tự quản lý instance, ghép chuỗi URL, gửi request và hứng mã lỗi thủ công. | **Declarative:** Chỉ khai báo interface và mapping URL qua Annotation của Spring MVC. |
| **Số dòng mã nguồn** | ~45–55 dòng (phải viết constructor inject RestTemplate, cài try-catch cho từng hàm). | ~15 dòng (chỉ khai báo signature của method và path). |
| **Cơ chế Fallback** | Try-catch lồng trực tiếp bên trong method nghiệp vụ, dễ gây rối logic. | Tách rời thành `FallbackFactory` độc lập, dễ bảo trì và mở rộng. |
| **Cân bằng tải (Load Balancing)** | Cần tự tạo và cấu hình Bean `@LoadBalanced RestTemplate`. | Tự động tích hợp thông qua thuộc tính `name = "product-service"`. |

**Lập luận khi nào nên dùng từng cách:**
* **Dùng FeignClient khi:** Xây dựng kiến trúc Microservice với các service nội bộ giao tiếp với nhau (East-West traffic). Giúp chuẩn hóa giao tiếp, giảm thiểu mã nguồn trùng lặp và tách biệt cơ chế chịu lỗi (Circuit Breaker).
* **Dùng RestTemplate/WebClient khi:** Gọi các bên thứ ba (Third-party APIs) với định dạng headers/params biến đổi linh hoạt, hoặc khi cần can thiệp sâu vào tầng network (chỉnh sửa socket trực tiếp, truyền file/stream dữ liệu lớn).