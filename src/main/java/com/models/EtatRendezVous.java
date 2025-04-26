package com.models;

import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtatRendezVous {


    private int id;
    private String libelle;
    private List<RendezVous> rendezVousList;
    private List<Medecin> medecins;

    // Constructeur
    public EtatRendezVous(String libelle) {
        this.libelle = libelle;
        this.rendezVousList = new ArrayList<>();
        this.medecins = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    //  gérer les rdvs
    public void addRendezVous(RendezVous rendezVous) {
        this.rendezVousList.add(rendezVous);
    }

    public void removeRendezVous(RendezVous rendezVous) {
        this.rendezVousList.remove(rendezVous);
    }

    public List<RendezVous> getRendezVousList() {
        return rendezVousList;
    }

    // gérer les méd
    public void addMedecin(Medecin medecin) {
        this.medecins.add(medecin);
    }

    public void removeMedecin(Medecin medecin) {
        this.medecins.remove(medecin);
    }

    public List<Medecin> getMedecins() {
        return medecins;
    }

    @Override
    public String toString() {
        return this.getLibelle();
    }

}
