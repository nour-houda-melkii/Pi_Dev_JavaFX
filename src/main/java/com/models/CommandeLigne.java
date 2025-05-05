package com.models;

import javafx.beans.property.*;

public class CommandeLigne {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty commandeId = new SimpleIntegerProperty();
    private final IntegerProperty produitId = new SimpleIntegerProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty priceAtPurchase = new SimpleDoubleProperty();

    // Constructors
    public CommandeLigne() {
        // Default constructor
    }

    public CommandeLigne(int id, int commandeId, int produitId, int quantity, Double priceAtPurchase) {
        this.id.set(id);
        this.commandeId.set(commandeId);
        this.produitId.set(produitId);
        this.quantity.set(quantity);
        this.priceAtPurchase.set(priceAtPurchase == null ? 0.0 : priceAtPurchase);
    }

    // Getters and Setters
    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public int getCommandeId() {
        return commandeId.get();
    }

    public IntegerProperty commandeIdProperty() {
        return commandeId;
    }

    public void setCommandeId(int commandeId) {
        this.commandeId.set(commandeId);
    }

    public int getProduitId() {
        return produitId.get();
    }

    public IntegerProperty produitIdProperty() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId.set(produitId);
    }

    public int getQuantity() {
        return quantity.get();
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public double getPriceAtPurchase() {
        return priceAtPurchase.get();
    }

    public DoubleProperty priceAtPurchaseProperty() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(Double priceAtPurchase) {
        this.priceAtPurchase.set(priceAtPurchase == null ? 0.0 : priceAtPurchase);
    }
}