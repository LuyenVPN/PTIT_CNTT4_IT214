### 1. Mã nguồn triển khai: `MenuService.java`

```java
package com.example.grabfood.service;

import com.example.grabfood.entity.Menu;
import com.example.grabfood.repository.MenuRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * Lấy thông tin món ăn theo ID.
     * Sử dụng @Cacheable: kiểm tra Redis trước, nếu chưa có thì query DB rồi lưu vào Redis.
     */
    @Cacheable(value = "menuCache", key = "#id")
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu not found with id: " + id));
    }

    /**
     * Cập nhật thông tin món ăn:
     * 1. Cập nhật Database trước thông qua menuRepository.save(menu).
     * 2. Xóa cache cũ bằng @CacheEvict với key = "#menu.id".
     * Mặc định @CacheEvict chạy sau khi phương thức kết thúc thành công (beforeInvocation = false).
     */
    @Transactional
    @CacheEvict(value = "menuCache", key = "#menu.id")
    public Menu updateMenu(Menu menu) {
        // Kiểm tra tồn tại trước khi cập nhật
        if (!menuRepository.existsById(menu.getId())) {
            throw new RuntimeException("Cannot update. Menu does not exist: " + menu.getId());
        }

        // Bước 2: Cập nhật DB trước
        return menuRepository.save(menu);
    }
}

```

---

### 2. Các thành phần bổ trợ (Entity & Controller)

**Menu.java (Entity / DTO mapping với JSON đề bài):**

```java
package com.example.grabfood.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "menus")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("dish_name")
    @Column(name = "dish_name")
    private String dishName;

    private Double price;

    public Menu() {}

    public Menu(Long id, String dishName, Double price) {
        this.id = id;
        this.dishName = dishName;
        this.price = price;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}

```

**MenuController.java:**

```java
package com.example.grabfood.controller;

import com.example.grabfood.entity.Menu;
import com.example.grabfood.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenuById(id));
    }

    @PutMapping
    public ResponseEntity<Menu> updateMenu(@RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.updateMenu(menu));
    }
}

```

---

### 3. Giải thích: Vì sao chọn `@CacheEvict` thay vì `@CachePut` để tránh Race Condition?

Trong môi trường phân tán (Distributed System) và chịu tải đồng thời cao (High Concurrency):

1. **Nguy cơ Race Condition khi dùng `@CachePut` (Ghi đè cache):**
* Giả sử có 2 request cập nhật cùng lúc: **Request A** (đổi giá thành 75.000đ) và **Request B** (đổi giá thành 80.000đ).
* Thứ tự thực thi ở Database: Request A commit trước $\rightarrow$ Request B commit sau (dữ liệu cuối cùng trong DB là **80.000đ**).
* Tuy nhiên, do độ trễ mạng hoặc context switching, bước cập nhật cache của Request B có thể diễn ra trước, sau đó bước cập nhật cache của Request A mới hoàn tất và ghi đè lên Redis.
* **Hậu quả:** Trong Redis lưu giá **75.000đ** (của A), nhưng trong DB lại lưu **80.000đ** (của B). Cache bị sai lệch nghiêm trọng và tồn tại lâu dài cho đến lần cập nhật tiếp theo.


2. **Lợi thế vượt trội của `@CacheEvict` (Cache-Aside pattern):**
* Thay vì cố gắng ghi đè giá trị mới vào cache, `@CacheEvict` chỉ đơn giản thực hiện **xóa key** khỏi Redis sau khi DB cập nhật thành công.
* Cả Request A hay Request B xóa trước/sau thì kết quả cuối cùng đều là: **Key bị xóa khỏi Redis**.
* Ở request đọc tiếp theo (`GET /menu/1`), hệ thống gặp Cache Miss $\rightarrow$ truy vấn trực tiếp bản ghi mới nhất từ Database $\rightarrow$ nạp ngược lại vào Redis. Nhờ đó triệt tiêu hoàn toàn khả năng cache lưu dữ liệu cũ/sai lệch do tranh chấp thứ tự ghi.
