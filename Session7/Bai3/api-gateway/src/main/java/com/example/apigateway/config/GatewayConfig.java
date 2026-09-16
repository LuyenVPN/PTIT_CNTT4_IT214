package com.example.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder) {

        return builder.routes()

                .route("customer-service", r -> r
                        .path("/api/customers/**")
                        .uri("lb://customer-service"))

                .route("account-service", r -> r
                        .path("/api/accounts/**")
                        .uri("lb://account-service"))

                .route("transaction-service", r -> r
                        .path("/api/transactions/**")
                        .uri("lb://transaction-service"))

                .build();
    }
}
