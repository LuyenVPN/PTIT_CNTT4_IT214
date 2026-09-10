package org.example.inventoryservice.dto;

public class StockInfo {
    private Long productId;
    private Integer quantity;
    private boolean available;
    private boolean isFallback;

    public StockInfo() {}

    public StockInfo(Long productId, Integer quantity, boolean available) {
        this.productId = productId;
        this.quantity = quantity;
        this.available = available;
        this.isFallback = false;
    }

    public static StockInfo unavailable(Long productId) {
        StockInfo fallback = new StockInfo(productId, 0, false);
        fallback.setFallback(true);
        return fallback;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isFallback() { return isFallback; }
    public void setFallback(boolean fallback) { isFallback = fallback; }
}