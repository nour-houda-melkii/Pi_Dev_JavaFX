package com.models;

import java.util.ArrayList;
import java.util.List;

public class Medecin extends User {
    private int id;
    private int userId;
    private String typesRendezVous; // Stocke les IDs des états (ex: "1,2")
    private List<RendezVous> rendezVousList;

    public Medecin(int userId, String typesRendezVous) {
        super();
        this.userId = userId;
        this.typesRendezVous = typesRendezVous;
        this.rendezVousList = new ArrayList<>();
    }
    public void setFirstName(String firstName) {
        super.setFirstName(firstName);
    }

    public void setLastName(String lastName) {
        super.setLastName(lastName);
    }
    // --- Getters & Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }



    public String getTypesRendezVous() {
        return typesRendezVous;
    }

    public void setTypesRendezVous(String typesRendezVous) {
        this.typesRendezVous = typesRendezVous;
    }

    // --- Gestion des rdv ---
    public void addRendezVous(RendezVous rendezVous) {
        this.rendezVousList.add(rendezVous);
    }

    public void removeRendezVous(RendezVous rendezVous) {
        this.rendezVousList.remove(rendezVous);
    }

    public List<RendezVous> getRendezVousList() {
        return rendezVousList;
    }
}