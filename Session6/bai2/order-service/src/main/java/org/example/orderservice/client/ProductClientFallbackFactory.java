package org.example.orderservice.client;

import org.example.orderservice.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductInfo getById(Long id) {
                log.error("Kích hoạt Fallback getById cho id: {}. Chi tiết lỗi: {}", id, cause.getMessage(), cause);
                return ProductInfo.fallback(id);
            }

            @Override
            public List<ProductInfo> getAll() {
                log.error("Kích hoạt Fallback getAll. Chi tiết lỗi: {}", cause.getMessage(), cause);
                return Collections.emptyList();
            }
        };
    }
}