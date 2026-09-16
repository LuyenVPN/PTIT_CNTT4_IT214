package com.storex.controller;

import com.storex.dto.Banner;
import com.storex.service.PromotionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final PromotionService promotionService;

    public BannerController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public Mono<Banner> getActiveBanner() {
        return promotionService.getActiveBanner();
    }
}
