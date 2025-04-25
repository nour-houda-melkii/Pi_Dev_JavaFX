package entities;

import javafx.beans.property.*;

public class Commande {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty userId = new SimpleIntegerProperty();
    private final ObjectProperty<java.time.LocalDate> dateCommande = new SimpleObjectProperty<>();
    private final StringProperty statut = new SimpleStringProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();

    // Constructors
    public Commande() {
        // Default constructor
    }

    public Commande(int id, int userId, java.time.LocalDate dateCommande, String statut, double total) {
        this.id.set(id);
        this.userId.set(userId);
        this.dateCommande.set(dateCommande);
        this.statut.set(statut);
        this.total.set(total);
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

    public int getUserId() {
        return userId.get();
    }

    public IntegerProperty userIdProperty() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId.set(userId);
    }

    public java.time.LocalDate getDateCommande() {
        return dateCommande.get();
    }

    public ObjectProperty<java.time.LocalDate> dateCommandeProperty() {
        return dateCommande;
    }

    public void setDateCommande(java.time.LocalDate dateCommande) {
        this.dateCommande.set(dateCommande);
    }

    public String getStatut() {
        return statut.get();
    }

    public StringProperty statutProperty() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut.set(statut);
    }

    public double getTotal() {
        return total.get();
    }

    public DoubleProperty totalProperty() {
        return total;
    }

    public void setTotal(double total) {
        this.total.set(total);
    }
}