package com.storex.dto;

public class Banner {
    private Long id;
    private String title;
    private String imageUrl;
    private String message;

    public Banner() {}

    public Banner(Long id, String title, String imageUrl, String message) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
