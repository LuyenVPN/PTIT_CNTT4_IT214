package com.storex.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class PromotionServiceTest {

    @Test
    void promotionServiceError_returnsDefaultBanner() {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:1")
                .build();

        PromotionService service = new PromotionService(client);

        StepVerifier.create(service.getActiveBanner())
                .expectNextMatches(banner ->
                        "Khuyến mãi đang được cập nhật".equals(banner.getMessage()))
                .verifyComplete();
    }
}
