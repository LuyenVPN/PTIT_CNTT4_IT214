package org.example.orderservice.dto;

public class ProductInfo {
    private Long id;
    private String name;
    private Double price;
    private boolean isFallback;

    public ProductInfo() {}

    public ProductInfo(Long id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isFallback = false;
    }

    public static ProductInfo fallback(Long id) {
        ProductInfo p = new ProductInfo(id, "Dữ liệu dự phòng (Fallback)", 0.0);
        p.isFallback = true;
        return p;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public boolean isFallback() { return isFallback; }
    public void setFallback(boolean fallback) { isFallback = fallback; }
}
