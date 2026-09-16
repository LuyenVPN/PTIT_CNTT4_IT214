package com.vietmart.orderservice.client;

import com.vietmart.orderservice.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log =
            LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        log.error("Product Service call failed: {}", cause.getMessage(), cause);

        return new ProductClient() {
            @Override
            public ProductInfo getById(Long id) {
                return ProductInfo.fallback(id);
            }

            @Override
            public List<ProductInfo> getAll() {
                return Collections.emptyList();
            }
        };
    }
}
