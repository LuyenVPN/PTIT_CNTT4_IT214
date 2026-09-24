package com.example.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RestaurantMenuCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantMenuCacheApplication.class, args);
    }

}
