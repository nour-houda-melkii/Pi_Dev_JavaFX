package com.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PostCategory {
    private int id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    // Constructor without ID (for insertions)
    public PostCategory(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now(); // Auto-set to current time
    }

    // Constructor with ID (for reading existing records)
    public PostCategory(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public PostCategory(int id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public PostCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        return createdAt.format(formatter);
    }

    public String toTableRow() {
        String desc = description.length() > 30 ? description.substring(0, 27) + "..." : description;
        return String.format("| %-15s | %-30s | %-20s |",
                name,
                desc,
                getFormattedCreatedAt());
    }
}
