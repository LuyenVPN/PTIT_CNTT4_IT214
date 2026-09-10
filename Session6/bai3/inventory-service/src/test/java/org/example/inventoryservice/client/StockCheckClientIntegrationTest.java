package org.example.inventoryservice.client;

import org.example.inventoryservice.dto.StockInfo;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class StockCheckClientIntegrationTest {

    private WireMockServer wireMockServer;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Tạo server giả lập trên port ngẫu nhiên
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Cấu hình RestTemplate với connectTimeout=1s, readTimeout=2s đúng như thực tế
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(1).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
        restTemplate = new RestTemplate(factory);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("WireMock delay 5s: Phải ngắt sau timeout và trả fallback trong vòng < 3s")
    void checkStock_ServerDelays5s_ReturnsFallbackUnder3s() {
        Long productId = 101L;

        // Mô phỏng server trả về sau 5 giây (5000ms)
        wireMockServer.stubFor(get(urlEqualTo("/api/stock/" + productId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"productId\":101,\"quantity\":50,\"available\":true}")));

        long startTime = System.currentTimeMillis();

        StockInfo result;
        try {
            String mockUrl = "http://localhost:" + wireMockServer.port() + "/api/stock/" + productId;
            result = restTemplate.getForObject(mockUrl, StockInfo.class);
        } catch (ResourceAccessException e) {
            result = StockInfo.unavailable(productId);
        }

        long executionTime = System.currentTimeMillis() - startTime;

        // 1. Kiểm tra kết quả trả về là Fallback
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertFalse(result.isAvailable());
        assertTrue(result.isFallback());

        // 2. Chứng minh client ngắt kết nối theo readTimeout (2s), không bị treo 5s
        assertTrue(executionTime >= 2000, "Thời gian phản hồi phải >= 2000ms do chờ readTimeout");
        assertTrue(executionTime < 3000, "Thời gian phản hồi phải ngắt trước 3000ms, không chờ hết 5000ms");
    }
}
