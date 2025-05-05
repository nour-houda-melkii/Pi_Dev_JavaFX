package com.models;

import java.sql.Timestamp;

public class Commentaire {
    private int id;
    private int produitId;
    private int userId;
    private String content;
    private Timestamp createdAt;

    public Commentaire() {
    }

    public Commentaire(int produitId, int userId, String content) {
        this.produitId = produitId;
        this.userId = userId;
        this.content = content;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", produitId=" + produitId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}