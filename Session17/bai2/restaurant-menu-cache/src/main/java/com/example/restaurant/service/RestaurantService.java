package com.example.restaurant.service;

import com.example.restaurant.entity.MenuItem;
import com.example.restaurant.repository.MenuItemRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    private final MenuItemRepository menuItemRepository;

    public RestaurantService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Cacheable(value = "restaurantMenu", key = "#id")
    public List<MenuItem> getMenuByRestaurantId(Long id) {

        System.out.println(">>> CACHE MISS - Đang truy vấn database...");
        System.out.println(">>> restaurantId = " + id);

        try {
            // Giả lập truy vấn database phức tạp mất 3 giây
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread bị interrupt", e);
        }

        List<MenuItem> menuItems =
                menuItemRepository.findByRestaurantId(id);

        System.out.println(">>> Đã truy vấn database xong.");

        return menuItems;
    }
}