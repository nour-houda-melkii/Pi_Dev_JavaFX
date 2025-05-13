package com.models;

import java.time.LocalDateTime;

public class Reactions {
    private int id;
    private int postId;
    private int userId;
    private String type; // "LIKE", "LOVE", "SAD"
    private LocalDateTime createdAt;

    // Default constructor
    public Reactions() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with fields
    public Reactions(int id, int postId, int userId, String type, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Constructor for creating new reactions
    public Reactions(int postId, int userId, String type) {
        this.postId = postId;
        this.userId = userId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "id=" + id +
                ", postId=" + postId +
                ", userId=" + userId +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}