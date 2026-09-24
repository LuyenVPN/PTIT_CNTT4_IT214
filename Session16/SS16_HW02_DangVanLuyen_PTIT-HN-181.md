# Báo Cáo Phân Tích & Giải Pháp Khắc Phục Lỗi Spring Cache

---

## 1. Phân tích nguyên nhân cache không hoạt động

### Annotation bị thiếu

File cấu hình chính (`Application.java`) bị thiếu annotation `@EnableCaching`.

### Cơ chế AOP Proxy & CacheInterceptor trong Spring Cache

* **Vai trò của `@Cacheable**`: `@Cacheable` chỉ đóng vai trò là metadata đánh dấu trên method hoặc class, khai báo ý định lưu/truy xuất kết quả vào cache. Bản thân nó không tự kích hoạt logic can thiệp vào luồng thực thi.
* **Vai trò của `@EnableCaching**`: Kích hoạt bộ xử lý `CachingConfigurationSelector`, qua đó đăng ký các bean hạ tầng cốt lõi:
* `BeanFactoryCacheOperationSourceAdvisor`: Quét và nhận diện các bean có method gắn `@Cacheable`, `@CachePut`, `@CacheEvict`.
* `CacheInterceptor`: Advisor interceptor chứa logic caching chính.


* **Cơ chế AOP Proxy**: Khi `@EnableCaching` được bật, Spring Container sẽ bọc target bean (`UserService`) bằng một Dynamic Proxy (CGLIB hoặc JDK Dynamic Proxy).
* Khi client gọi `getUserById()`, request đi qua Proxy trước.
* Proxy chuyển luồng sang `CacheInterceptor`.
* `CacheInterceptor` kiểm tra `CacheManager` xem key (`userId`) đã tồn tại chưa:
* **Cache Hit**: Trả về dữ liệu ngay từ cache, ngắt luồng và không thực thi method thật.
* **Cache Miss**: Cho phép method thực thi (truy vấn Database), sau đó lấy kết quả trả về ghi vào cache trước khi phản hồi client.




* **Lý do hệ thống liên tục truy vấn Database**: Do thiếu `@EnableCaching`, Spring không kích hoạt các Cache Advisor. `UserService` được inject dưới dạng bean thông thường (hoặc proxy không có `CacheInterceptor`), dẫn đến việc mọi lần gọi hàm đều nhảy thẳng vào body của `getUserById()` và thực thi `userRepository.findById(userId)`.

---

## 2. Mã nguồn hoàn chỉnh sau khi khắc phục

### File cấu hình Cache (`CacheConfig.java`)

Tách cấu hình sang lớp riêng để quản lý rõ ràng và phục vụ việc thay thế `CacheManager` (Redis, Caffeine,...) linh hoạt hơn sau này:

```java
package com.example.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Khởi tạo in-memory cache manager với cache tên "users"
        return new ConcurrentMapCacheManager("users");
    }
}

```

### File cấu hình chính (`Application.java`)

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

```

### Service Layer (`UserService.java`)

Bổ sung kiểm tra fail-fast cho tham số đầu vào và áp dụng điều kiện SpEL:

* `condition = "#userId != null && !#userId.trim().isEmpty()"`: Chỉ kích hoạt kiểm tra cache khi ID hợp lệ.
* `unless = "#result == null"`: Không lưu vào cache nếu kết quả trả về là `null`.

```java
package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Cacheable(
        value = "users",
        key = "#userId",
        condition = "#userId != null && !#userId.trim().isEmpty()",
        unless = "#result == null"
    )
    public User getUserById(String userId) {
        // Fail-fast validation
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId không được để trống hoặc null");
        }

        System.out.println(">>> Truy vấn Database cho userId: " + userId);
        return userRepository.findById(userId).orElse(null);
    }
}

```

---

## 3. Test Case chứng minh Cache hoạt động (`UserServiceTest.java`)

Sử dụng `@SpringBootTest` kết hợp `@MockBean` và `Mockito.verify` để đo lường chính xác số lần method của Repository được gọi.

```java
package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Lần gọi 1 truy vấn DB (Cache Miss), lần gọi 2 lấy từ Cache (Cache Hit)")
    void testGetUserById_CacheHitAndMiss() {
        String userId = "USR_1001";
        User mockUser = new User(userId, "Nguyen Van A", "a.nguyen@fintech.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Lần gọi 1: Cache miss -> Phải truy vấn Database
        User firstCallResult = userService.getUserById(userId);
        assertNotNull(firstCallResult);
        assertEquals("Nguyen Van A", firstCallResult.getName());
        verify(userRepository, times(1)).findById(userId);

        // Lần gọi 2: Cache hit -> Không được truy vấn Database
        User secondCallResult = userService.getUserById(userId);
        assertNotNull(secondCallResult);
        assertEquals("Nguyen Van A", secondCallResult.getName());
        verify(userRepository, times(1)).findById(userId); // Vẫn chỉ là 1 lần duy nhất
    }

    @Test
    @DisplayName("Không cache kết quả khi User không tồn tại (trả về null)")
    void testGetUserById_DoNotCacheNullResult() {
        String notFoundId = "USR_9999";
        when(userRepository.findById(notFoundId)).thenReturn(Optional.empty());

        // Lần 1
        User firstCall = userService.getUserById(notFoundId);
        assertNull(firstCall);
        verify(userRepository, times(1)).findById(notFoundId);

        // Lần 2: Vì unless = "#result == null", cache không lưu -> tiếp tục query DB
        User secondCall = userService.getUserById(notFoundId);
        assertNull(secondCall);
        verify(userRepository, times(2)).findById(notFoundId);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi userId null hoặc rỗng")
    void testGetUserById_InvalidInput_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(null));
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById("   "));
        verifyNoInteractions(userRepository);
    }
}

```

---

## 4. Xử lý các tình huống đặc biệt

### Xử lý tham số đầu vào (`userId` là null hoặc rỗng)

* **Vấn đề**: Nếu client gửi `null` hoặc chuỗi rỗng `""`, cache key sinh ra sẽ là `null` hoặc rỗng, gây lãng phí bộ nhớ lưu các key rác hoặc gây lỗi `NullPointerException` ở một số Cache Provider (như Redis).
* **Giải pháp**:
1. **Fail-fast validation**: Bổ sung kiểm tra ở đầu hàm `if (userId == null || userId.trim().isEmpty())` để throw `IllegalArgumentException` ngay lập tức, ngắt luồng trước khi chạm tới tầng Database.
2. **SpEL condition**: Sử dụng thuộc tính `condition = "#userId != null && !#userId.trim().isEmpty()"` trên `@Cacheable` để đảm bảo proxy không can thiệp khởi tạo cache key nếu tham số không hợp lệ.



### Xử lý kết quả trả về `null`

Khi ID người dùng không tồn tại trong DB, method trả về `null`. Cần cân nhắc giữa hai hướng tiếp cận:

| Tiêu chí | **Không lưu null** (`unless = "#result == null"`) | **Lưu null tạm thời** (Cache Null Value) |
| --- | --- | --- |
| **Ưu điểm** | Tiết kiệm bộ nhớ cache; tránh lưu trữ dữ liệu không có giá trị. | Ngăn chặn triệt để tấn công **Cache Penetration** (kẻ xấu liên tục query ID ảo với tải 10.000 req/s để ép DB quá tải). |
| **Nhược điểm** | Dễ bị nghẽn DB nếu gặp bão request tra cứu các ID không tồn tại. | Tốn RAM; nếu user mới đăng ký với ID đó sau này, client có thể nhận giá trị null cũ (cần cơ chế evict khi tạo mới). |
| **Cấu hình** | Dùng SpEL: `unless = "#result == null"` trên method. | Với Redis: bật `RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()` là **sai** nếu muốn chống penetration; thay vào đó cấu hình cho phép cache null kèm TTL ngắn (1–2 phút). |

**Khuyến nghị cho hệ thống Fintech (10.000 req/s)**:

* Ở tầng Service logic cơ bản, dùng `unless = "#result == null"` để giữ mã nguồn tường minh.
* Khi nâng cấp lên Redis Cluster trong môi trường Production, nên cho phép cache giá trị null (hoặc đối tượng rỗng Sentinel Object) kèm TTL ngắn (60 giây) để giảm áp lực cho Database trước các đợt scan ID không tồn tại.