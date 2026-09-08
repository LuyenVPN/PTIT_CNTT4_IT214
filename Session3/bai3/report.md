# Báo cáo: Xây dựng và sửa lỗi cấu hình Config Server

## 1. Sửa lỗi thiếu annotation `@EnableConfigServer`
- **Sửa lỗi:** Đã thêm `@EnableConfigServer` vào file `ConfigServerApplication.java`.
- **Giải thích:** Spring Boot là một framework linh hoạt. Theo mặc định, `@SpringBootApplication` chỉ khởi động một ứng dụng Spring Boot thông thường (thường là một REST API server với Tomcat nhúng, nếu có dependency web). Để ứng dụng này đóng vai trò là một **Spring Cloud Config Server**, nó cần được cấu hình và nạp các bean đặc thù của Config Server (như Git repository reader, các endpoint `/encrypt`, `/decrypt`, và endpoint trả về cấu hình). Annotation `@EnableConfigServer` làm nhiệm vụ kích hoạt tính năng này. Nếu thiếu nó, ứng dụng vẫn chạy bình thường nhưng không cung cấp các endpoint phục vụ cấu hình, dẫn đến việc `restaurant-service` gọi tới bị lỗi 404 hoặc 500 do không tìm thấy đường dẫn hoặc sai định dạng trả về.

## 2. Sửa lỗi cấu hình Git URI
- **Sửa lỗi:** 
  - Đã thêm `.git` vào cuối URL: `uri: https://github.com/foodx/config-repo.git`
  - Đã sửa tên nhánh mặc định thành nhánh thực tế: `default-label: main` (ban đầu là `master`).
- **Giải thích:** Config Server sử dụng Git client (JGit) để clone repository chứa cấu hình. Một số Git server yêu cầu URL kết thúc bằng `.git` để xác định đúng là Git repository. Ngoài ra, Github đã chuyển nhánh mặc định từ `master` sang `main`. Nếu không chỉ định đúng `default-label: main`, Config Server sẽ cố gắng checkout nhánh `master` (không tồn tại), dẫn đến lỗi khi đọc cấu hình.

## 3. Mô tả luồng xử lý `GET /restaurant-service/prod`
Khi Config Server nhận được HTTP request `GET /restaurant-service/prod`:
1. **Tiếp nhận Request:** Endpoint nội bộ của Config Server nhận request với các tham số tương ứng: `{application} = restaurant-service` và `{profile} = prod`.
2. **Xác định Repository & Nhánh:** Config Server sẽ dùng cấu hình git đã định nghĩa (`uri: https://github.com/foodx/config-repo.git`, nhánh `main` - do không có `{label}` trong URL nên dùng `default-label`).
3. **Đồng bộ mã nguồn:** Config Server sẽ clone hoặc pull repository cấu hình về thư mục tạm cục bộ (nếu chưa có hoặc có sự thay đổi).
4. **Tìm kiếm file cấu hình:** Nó sẽ tìm kiếm trong repository các file cấu hình tương ứng với ứng dụng và profile, ví dụ:
   - `application.yml` / `application.properties` (cấu hình chung cho tất cả service)
   - `restaurant-service.yml` / `restaurant-service.properties` (cấu hình chung của service)
   - `restaurant-service-prod.yml` / `restaurant-service-prod.properties` (cấu hình riêng cho profile prod)
5. **Gộp (Merge) cấu hình:** Config Server đọc các file này và gộp (merge) chúng lại theo thứ tự ưu tiên (file cụ thể hơn như `-prod` ghi đè file chung).
6. **Trả về kết quả:** Config Server đóng gói kết quả thành định dạng JSON chứa các PropertySources và trả về HTTP Response 200 OK cho `restaurant-service`.

## 4. Đề xuất cách kiểm thử Config Server độc lập
Để xác nhận Config Server hoạt động đúng trước khi tích hợp với `restaurant-service`, ta có thể sử dụng các công cụ gọi API trực tiếp để kiểm tra mà không cần chạy `restaurant-service` thật:
- **Dùng Browser hoặc cURL, Postman:**
  - Khởi động ứng dụng Config Server (`./gradlew bootRun` hoặc chạy qua IDE).
  - Gửi request trực tiếp đến Config Server. Mặc định Config Server lắng nghe ở cổng `8888`.
  - Dùng lệnh: `curl http://localhost:8888/restaurant-service/prod`
  - **Kết quả mong đợi:** Config Server trả về mã HTTP 200 kèm một JSON object chứa danh sách cấu hình. JSON này chứa một mảng `propertySources`, liệt kê các thuộc tính được lấy từ các file cấu hình tương ứng trong Git repo. Nếu có lỗi (ví dụ không kết nối được Git), nó sẽ trả về lỗi rõ ràng để ta dễ dàng khắc phục.
