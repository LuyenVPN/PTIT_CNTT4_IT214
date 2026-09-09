# BÁO CÁO THỰC HÀNH: CẤU HÌNH RANDOM LOADBALANCER

* **Mã bài toán:** SPRING-CLOUD-S05-EX03
* **Độ khó:** Nâng cao (3 sao)
* **Dự án:** Hệ sinh thái VietMart (API Gateway & Product Service)

---

## 1. Mục tiêu

* Thay đổi chiến lược cân bằng tải mặc định của Spring Cloud LoadBalancer từ **Round Robin** sang **RandomLoadBalancer** riêng cho `product-service`.
* Đảm bảo tính cô lập của cấu hình: Chỉ áp dụng thuật toán ngẫu nhiên cho `product-service`, giữ nguyên thuật toán mặc định cho các dịch vụ khác trong hệ thống VietMart.

---

## 2. Cấu hình RandomLoadBalancer

Tạo lớp cấu hình `RandomLoadBalancerConfig` để định nghĩa Bean `ReactorLoadBalancer<ServiceInstance>` sử dụng chiến lược ngẫu nhiên (`RandomLoadBalancer`).

```java
package com.vietmart.gateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RandomLoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {

        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(
                        serviceId,
                        ServiceInstanceListSupplier.class
                ),
                serviceId
        );
    }
}

```

* **Cơ chế hoạt động:** `LoadBalancerClientFactory.PROPERTY_NAME` lấy động tên service từ ngữ cảnh gọi. Bean trả về một `RandomLoadBalancer` truy xuất danh sách instance từ `ServiceInstanceListSupplier` và chọn ngẫu nhiên một instance xử lý request.

---

## 3. Áp dụng cho product-service

Tại lớp khởi chạy `ApiGatewayApplication`, áp dụng cấu hình thông qua annotation `@LoadBalancerClient`:

```java
package com.vietmart.gateway;

import com.vietmart.gateway.config.RandomLoadBalancerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;

@SpringBootApplication
@LoadBalancerClient(
        name = "product-service",
        configuration = RandomLoadBalancerConfig.class
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

```

* **Phạm vi tác động:** Thuộc tính `name = "product-service"` đảm bảo lớp `RandomLoadBalancerConfig` chỉ được nạp vào Spring Child Application Context dành riêng cho `product-service`. Các service khác khi đi qua Gateway vẫn chạy thuật toán Round Robin chuẩn.

---

## 4. Cấu hình Gateway

File `application.yml` của API Gateway thiết lập định tuyến với tiền tố `lb://`:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: product-service-route
          uri: lb://product-service
          predicates:
            - Path=/products, /products/**

```

* Giao thức `lb://product-service` kích hoạt bộ lọc của Spring Cloud LoadBalancer để phân giải tên service thành địa chỉ IP/Port của các instance đang sẵn sàng (thông qua Service Discovery hoặc cấu hình instance tĩnh).

---

## 5. Kiểm thử và Thực nghiệm

### 5.1. Thiết lập môi trường chạy

Khởi chạy đồng thời 2 instance của `product-service` cùng mã nguồn, khác cổng:

* **Instance 1:** `server.port=8081`
* **Instance 2:** Khởi chạy với tham số ghi đè `--server.port=8082`
* **API Gateway:** Chạy ở cổng `8080`

Đoạn mã ghi nhận log tại `ProductController` của `product-service`:

```java
@GetMapping
public String getProducts() {
    log.info(">>> [PRODUCT-SERVICE] Request handled by instance on PORT: {}", port);
    return "Handled by port: " + port;
}

```

### 5.2. Kịch bản kiểm thử

Gửi liên tiếp 10 request từ terminal qua API Gateway:

```powershell
1..10 | ForEach-Object { curl.exe -s http://localhost:8080/products; Write-Host "" }

```

### 5.3. Kết quả ghi nhận

Thứ tự xử lý thực tế qua 10 request:

```text
Request 01 -> Handled by port: 8081
Request 02 -> Handled by port: 8081
Request 03 -> Handled by port: 8082
Request 04 -> Handled by port: 8081
Request 05 -> Handled by port: 8082
Request 06 -> Handled by port: 8082
Request 07 -> Handled by port: 8082
Request 08 -> Handled by port: 8081
Request 09 -> Handled by port: 8082
Request 10 -> Handled by port: 8081

```

* **Tổng kết:** Port 8081 xử lý 5 request, Port 8082 xử lý 5 request.
* **Đặc điểm phân phối:** Thứ tự xử lý hoàn toàn ngẫu nhiên (xuất hiện cụm `8081 -> 8081` và `8082 -> 8082 -> 8082`), không tuân theo chu kỳ luân phiên xen kẽ ngặt nghèo `8081 -> 8082 -> 8081 -> 8082` như Round Robin mặc định.

---

## 6. Kết luận

* Cấu hình chuyển đổi chiến lược sang `RandomLoadBalancer` cho `product-service` đã hoạt động chính xác và đạt yêu cầu bài toán.
* Việc áp dụng annotation `@LoadBalancerClient` giúp cô lập cấu hình theo từng client cụ thể, tránh ảnh hưởng đến các service khác trong hệ thống microservice.
* **Đánh giá nghiệp vụ:** Thuật toán ngẫu nhiên phá vỡ quy luật phân phối tuần tự, tuy nhiên không đánh giá được tải thực tế (CPU, RAM, số kết nối hoạt động) của các máy chủ. Trong môi trường production có server cấu hình lệch nhau rõ rệt, cần cân nhắc các giải pháp nâng cao hơn như Weighted Load Balancing hoặc cơ chế kiểm tra tải động (Resource-based metrics).