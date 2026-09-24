# Báo Cáo Phân Tích & Giải Pháp Kỹ Thuật: Hệ Thống Quản Lý Tồn Kho Với Cache-Aside Pattern & Redis

---

## 1. Phân tích thiết kế Cache-Aside Pattern

### Nguyên lý hoạt động

Trong mô hình **Cache-Aside** (Lazy Loading), ứng dụng đóng vai trò điều phối trực tiếp giữa Data Store chính (RDBMS) và Cache (Redis). Hệ thống cache không giao tiếp trực tiếp với database.

* **Luồng Đọc (Read Flow):**
1. Client gửi yêu cầu đọc dữ liệu tồn kho theo `productId`.
2. Ứng dụng kiểm tra key trong Redis Cache.
3. **Cache Hit**: Nếu key tồn tại, lấy trực tiếp từ Redis và trả về client (thời gian phản hồi < 10ms).
4. **Cache Miss**: Nếu key không tồn tại, ứng dụng truy vấn RDBMS. Dữ liệu sau khi đọc từ RDBMS được ghi ngược vào Redis (kèm cấu hình TTL) trước khi trả về cho client.


* **Luồng Ghi (Write / Update Flow):**
1. Client gửi yêu cầu cập nhật số lượng tồn kho `newQuantity` cho `productId`.
2. Ứng dụng thực thi cập nhật trực tiếp vào RDBMS trước để đảm bảo tính toàn vẹn (ACID transaction).
3. Sau khi ghi DB thành công, ứng dụng thực hiện **Evict (xóa)** cache key tương ứng trong Redis thay vì update trực tiếp vào cache (tránh race condition khi có nhiều thread ghi cùng lúc).
4. Lần đọc tiếp theo sẽ rơi vào cache miss và tự động nạp dữ liệu mới nhất từ DB vào cache.



### Đặc tả Input / Output

* **Đọc tồn kho (`getInventory`)**:
* **Input**: `productId` (`String`, bắt buộc khác null và không chứa chuỗi rỗng/khoảng trắng).
* **Output**: `ProductInventoryDTO` gồm `productId` (`String`), `quantity` (`Integer`).


* **Cập nhật tồn kho (`updateInventory`)**:
* **Input**: `productId` (`String`, không null/rỗng), `newQuantity` (`Integer`, điều kiện ràng buộc: `newQuantity >= 0`).
* **Output**: `ProductInventoryDTO` chứa thông tin sau khi cập nhật thành công.



---

## 2. Mã nguồn hoàn chỉnh

### 2.1. Cấu hình Redis & Xử lý lỗi Cache (`RedisCacheConfig.java`)

Để hệ thống không bị sập hay gián đoạn khi Redis gặp sự cố mạng (failover, connection reset), ta triển khai `CachingConfigurer` và cài đặt custom `CacheErrorHandler`:

```java
package com.tiki.inventory.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // TTL ngắn (5 phút) để phòng ngừa lỗi khi evict thất bại
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                // Fallback xuống DB: log warning và bỏ qua lỗi để code đi tiếp vào method thực
                log.warn("Redis GET error for key [{}]: {}. Fallback to DB.", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis PUT error for key [{}]: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                // Khi evict lỗi, log error để hệ thống giám sát cảnh báo
                log.error("Redis EVICT error for key [{}]: {}. Data might be stale until TTL expires.", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Redis CLEAR error: {}", exception.getMessage());
            }
        };
    }
}

```

### 2.2. DTO và Entity

```java
package com.tiki.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String productId;
    private Integer quantity;
}

```

```java
package com.tiki.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    private String productId;
    private Integer quantity;
}

```

### 2.3. Service Layer (`InventoryService.java`)

Áp dụng `@Cacheable` cho hàm đọc và `@CacheEvict` cho hàm ghi. Kiểm tra validate điều kiện số lượng âm và ID rỗng ngay từ đầu hàm.

```java
package com.tiki.inventory.service;

import com.tiki.inventory.dto.ProductInventoryDTO;
import com.tiki.inventory.entity.Inventory;
import com.tiki.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Cacheable(
        value = "inventory",
        key = "#productId",
        condition = "#productId != null && !#productId.trim().isEmpty()",
        unless = "#result == null"
    )
    public ProductInventoryDTO getInventory(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("productId không hợp lệ");
        }

        log.info(">>> Truy vấn Database cho productId: {}", productId);
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId));

        return new ProductInventoryDTO(inventory.getProductId(), inventory.getQuantity());
    }

    @Transactional
    @CacheEvict(value = "inventory", key = "#productId")
    public ProductInventoryDTO updateInventory(String productId, Integer newQuantity) {
        // Tình huống 1: Chặn số lượng âm và validate ID
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("productId không được để trống");
        }
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm: " + newQuantity);
        }

        log.info(">>> Cập nhật Database cho productId: {} với số lượng: {}", productId, newQuantity);
        Inventory inventory = inventoryRepository.findById(productId)
                .orElse(new Inventory(productId, 0));

        inventory.setQuantity(newQuantity);
        Inventory saved = inventoryRepository.save(inventory);

        // Sau khi method return thành công, @CacheEvict sẽ xóa key trong Redis
        return new ProductInventoryDTO(saved.getProductId(), saved.getQuantity());
    }
}

```

---

## 3. Phân tích giải pháp xử lý sự cố & Tình huống biên

### Tình huống 1: newQuantity bị nhập số âm

* **Vấn đề**: Nhân viên kho gửi nhầm `-10` hoặc API client truyền tham số âm do tính toán sai, dẫn đến hỏng số liệu kinh doanh.
* **Biện pháp**:
* Chặn kiểm tra fail-fast ngay dòng đầu tiên của `updateInventory()`: `if (newQuantity == null || newQuantity < 0)`.
* Không cho phép giao dịch DB mở ra hoặc can thiệp vào cache. Ném ngoại lệ `IllegalArgumentException` để Global Exception Handler trả về mã lỗi HTTP `400 Bad Request`.



### Tình huống 2: Sự cố kết nối Redis (Connection Breakdown)

* **Khi Đọc (`handleCacheGetError`)**:
* Spring Cache mặc định sẽ ném exception làm gián đoạn API nếu Redis offline.
* Thông qua `Custom CacheErrorHandler`, khi `handleCacheGetError` bắt được ngoại lệ kết nối, nó sẽ ghi log cảnh báo và **nuốt lỗi (suppress error)**. Lúc này, proxy coi như cache miss và tự động để method `getInventory()` thực thi xuống Database. Khách hàng vẫn xem được tồn kho bình thường mà không nhận lỗi `500`.


* **Khi Xóa Cache (`handleCacheEvictError`)**:
* Khi cập nhật DB xong mà Redis mất mạng, lệnh evict sẽ thất bại, dẫn tới dữ liệu trên Redis bị cũ (Stale Data).
* **Giải pháp kết hợp**:
1. **TTL Ngắn (Short Expiration)**: Cấu hình `entryTtl(Duration.ofMinutes(5))` trong `RedisCacheConfiguration`. Dù lệnh evict thất bại, dữ liệu bẩn tối đa chỉ tồn tại trong vòng 5 phút trước khi Redis tự động dọn dẹp.
2. **Transactional Outbox / Message Queue (Dành cho cấp độ Production nâng cao)**: Bắn một event `InventoryUpdatedEvent` vào Kafka/RabbitMQ. Consumer sẽ thực hiện retry xóa cache nhiều lần với cơ chế Exponential Backoff cho đến khi Redis online trở lại.





---

## 4. Test Case kiểm chứng (`InventoryServiceTest.java`)

```java
package com.tiki.inventory.service;

import com.tiki.inventory.dto.ProductInventoryDTO;
import com.tiki.inventory.entity.Inventory;
import com.tiki.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @MockBean
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Cache-Aside Read: Lần 1 gọi DB, lần 2 lấy từ Cache")
    void testGetInventory_CacheAside() {
        String productId = "PROD_IPHONE15";
        when(inventoryRepository.findById(productId))
                .thenReturn(Optional.of(new Inventory(productId, 100)));

        // Lần 1: Cache Miss -> Query DB
        ProductInventoryDTO res1 = inventoryService.getInventory(productId);
        assertEquals(100, res1.getQuantity());
        verify(inventoryRepository, times(1)).findById(productId);

        // Lần 2: Cache Hit -> Không gọi lại DB
        ProductInventoryDTO res2 = inventoryService.getInventory(productId);
        assertEquals(100, res2.getQuantity());
        verify(inventoryRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Ném ngoại lệ và không ghi DB khi newQuantity là số âm")
    void testUpdateInventory_NegativeQuantity_ThrowsException() {
        String productId = "PROD_IPHONE15";

        assertThrows(IllegalArgumentException.class, () -> 
            inventoryService.updateInventory(productId, -10)
        );

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật DB và xóa cache hợp lệ khi newQuantity >= 0")
    void testUpdateInventory_Success() {
        String productId = "PROD_IPHONE15";
        Inventory existing = new Inventory(productId, 100);
        Inventory updated = new Inventory(productId, 95);

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updated);

        ProductInventoryDTO result = inventoryService.updateInventory(productId, 95);

        assertEquals(95, result.getQuantity());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }
}

```