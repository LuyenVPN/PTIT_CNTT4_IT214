package com.storex.service;

import com.storex.dto.Banner;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class PromotionService {

    private final WebClient promotionWebClient;

    public PromotionService(WebClient promotionWebClient) {
        this.promotionWebClient = promotionWebClient;
    }

    public Mono<Banner> getActiveBanner() {
        return promotionWebClient
                .get()
                .uri("/api/banners/active")
                .retrieve()
                .bodyToMono(Banner.class)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(error -> Mono.just(createDefaultBanner()));
    }

    private Banner createDefaultBanner() {
        return new Banner(
                null,
                "Khuyến mãi",
                null,
                "Khuyến mãi đang được cập nhật"
        );
    }
}
