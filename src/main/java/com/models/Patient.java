package com.models;

import java.util.ArrayList;
import java.util.List;

public class Patient extends User {
    private int id;
    private int userId;
    private List<RendezVous> rendezVousList;

    // Constructeur
    public Patient(int userId) {
        super();
        this.userId = userId;
        this.rendezVousList = new ArrayList<>();
    }

    public Patient() {
    }

    // Getters et Setters
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
    public int getAge() {
        return super.getAge(); // Récupère l'âge de la classe `User`
    }

    // Méthodes pour gérer les rendez-vous
    public void addRendezVous(RendezVous rendezVous) {
        this.rendezVousList.add(rendezVous);
    }

    public void removeRendezVous(RendezVous rendezVous) {
        this.rendezVousList.remove(rendezVous);
    }

    public List<RendezVous> getRendezVousList() {
        return rendezVousList;
    }

    @Override

    public String toString() {
        return super.toString() + " (Patient)";
    }
}
