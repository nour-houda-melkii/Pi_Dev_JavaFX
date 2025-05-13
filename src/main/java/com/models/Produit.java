package com.models;

public class Produit {
    private int id;
    private String name;
    private String desciption;
    private double price;
    private double originalPrice;
    private String imagePath;
    private int quantity;
    private int categoryId;
    private int favoritesCount;
    private String discountStatus;

    // Constructors
    public Produit() {}

    public Produit(String name, String desciption, double price, String imagePath, int quantity, int categoryId) {
        this.name = name;
        this.desciption = desciption;
        this.price = price;
        this.imagePath = imagePath;
        this.quantity = quantity;
        this.categoryId = categoryId;
    }

    public Produit(int id, String name, String desciption, double price, String imagePath, int quantity, int categoryId) {
        this.id = id;
        this.name = name;
        this.desciption = desciption;
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

    public String getDesciption() {
        return desciption;
    }

    public void setDesciption(String desciption) {
        this.desciption = desciption;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
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

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public String getDiscountStatus() {
        return discountStatus;
    }

    public void setDiscountStatus(String discountStatus) {
        this.discountStatus = discountStatus;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", desciption='" + desciption + '\'' +
                ", price=" + price +
                ", originalPrice=" + originalPrice +
                ", imagePath='" + imagePath + '\'' +
                ", quantity=" + quantity +
                ", categoryId=" + categoryId +
                ", favoritesCount=" + favoritesCount +
                ", discountStatus='" + discountStatus + '\'' +
                '}';
    }
}