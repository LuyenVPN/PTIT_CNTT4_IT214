package org.example.inventoryservice.client;

import org.example.inventoryservice.dto.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class StockCheckClient {

    private static final Logger log = LoggerFactory.getLogger(StockCheckClient.class);
    private static final String PRODUCT_SERVICE_URL = "http://product-service/api/stock/{pid}";

    private final RestTemplate restTemplate;

    public StockCheckClient(@LoadBalanced RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public StockInfo checkStock(Long productId) {
        try {
            return restTemplate.getForObject(PRODUCT_SERVICE_URL, StockInfo.class, productId);
        } catch (ResourceAccessException e) {
            log.warn("Gọi product-service thất bại do timeout/mạng (productId: {}). Kích hoạt Fallback.", productId, e);
            return StockInfo.unavailable(productId);
        }
    }
}
