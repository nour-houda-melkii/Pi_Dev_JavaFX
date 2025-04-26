package com.models;

public class Produit {
    private int id;
    private String name;
    private String description;
    private double price;
    private String imagePath;
    private int quantity;
    private int categoryId;

    // Constructors
    public Produit() {}

    public Produit(String name, String description, double price, String imagePath, int quantity, int categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.quantity = quantity;
        this.categoryId = categoryId;
    }

    public Produit(int id, String name, String description, double price, String imagePath, int quantity, int categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.quantity = quantity;
        this.categoryId = categoryId;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", imagePath='" + imagePath + '\'' +
                ", quantity=" + quantity +
                ", categoryId=" + categoryId +
                '}';
    }
}