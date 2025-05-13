package com.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Post {
    private int id;
    private int categoryId;
    private int authorId;
    private String title;
    private String description;
    private String type;
    private String image;
    private LocalDateTime createdAt;
    private boolean enabled;
    private String status; // ACTIVE, RESERVED, COMPLETED, EXPIRED

    private int reservedById; // User ID who reserved the post

    private String reservationDate; // When it was reserved
    private String expirationDate; // When the post expires

    // Constructor for new posts
    public Post(int categoryId, int authorId, String title, String description,
                String type, String image, boolean enabled, String status) {
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.createdAt = LocalDateTime.now();
        this.enabled = enabled;
        this.status = status;
    }

    // Constructor for existing posts from DB
    public Post(int id, int categoryId, int authorId, String title,
                String description, String type, String image,
                boolean enabled, String status) {
        this.id = id;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.enabled = enabled;
        this.status = status;
    }

    public Post(int id, int categoryId, int authorId, String title,
                String description, String type, String image,
                LocalDateTime createdAt, boolean enabled, String status) {
        this.id = id;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.createdAt = createdAt;
        this.enabled = enabled;
        this.status = status;
    }

    public Post() {
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // Formatted date display
    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
    }

    @Override
    public String toString() {
        return String.format("Post [id=%d, title='%s', categoryId=%d, authorId=%s, description='%s', type='%s', image='%s']",
                id, title, categoryId, authorId, description, type, image);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReservedById() {
        return reservedById;
    }

    public String getReservationDate() {
        return reservationDate;
    }

}