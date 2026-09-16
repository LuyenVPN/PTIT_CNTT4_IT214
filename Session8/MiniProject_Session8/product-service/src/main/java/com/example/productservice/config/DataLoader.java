package com.example.productservice.config;

import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.saveAll(List.of(
                    Product.builder()
                            .name("Bàn phím cơ không dây Keychron K2 Pro")
                            .description("Bàn phím cơ layout 75%, hot-swap, kết nối Bluetooth 5.1 & Type-C")
                            .price(new BigDecimal("2190000"))
                            .stockQuantity(50)
                            .category("Phụ kiện công nghệ")
                            .build(),
                    Product.builder()
                            .name("Chuột không dây Logitech MX Master 3S")
                            .description("Cảm biến Darkfield 8000 DPI, cuộn MagSpeed siêu nhanh, yên tĩnh")
                            .price(new BigDecimal("2450000"))
                            .stockQuantity(35)
                            .category("Phụ kiện công nghệ")
                            .build(),
                    Product.builder()
                            .name("Màn hình Dell UltraSharp U2723QE 4K")
                            .description("Màn hình IPS Black 27 inch 4K UHD, 100% sRGB, cổng USB-C 90W")
                            .price(new BigDecimal("13500000"))
                            .stockQuantity(12)
                            .category("Màn hình máy tính")
                            .build(),
                    Product.builder()
                            .name("Tai nghe chống ồn Sony WH-1000XM5")
                            .description("Khử tiếng ồn đỉnh cao, pin 30 giờ, công nghệ Auto NC Optimizer")
                            .price(new BigDecimal("7990000"))
                            .stockQuantity(20)
                            .category("Âm thanh")
                            .build(),
                    Product.builder()
                            .name("Ghế công thái học Herman Miller Aeron")
                            .description("Thiết kế công thái học cao cấp, lưới Pellicle thoáng khí, hỗ trợ cột sống PostureFit SL")
                            .price(new BigDecimal("28900000"))
                            .stockQuantity(5)
                            .category("Nội thất văn phòng")
                            .build()
            ));
        }
    }
}
