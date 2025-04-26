package com.models;

public class Favori {
    private int id;
    private int userId;
    private int produitId;

    // Default constructor
    public Favori() {
    }

    // Constructor with userId and produitId
    public Favori(int userId, int produitId) {
        this.userId = userId;
        this.produitId = produitId;
    }

    // Constructor with all fields
    public Favori(int id, int userId, int produitId) {
        this.id = id;
        this.userId = userId;
        this.produitId = produitId;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    @Override
    public String toString() {
        return "Favori{" +
                "id=" + id +
                ", userId=" + userId +
                ", produitId=" + produitId +
                '}';
    }
}