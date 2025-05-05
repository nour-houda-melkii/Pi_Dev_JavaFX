package com.models;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class RendezVous {
    private int id; // ID primaire
    private int medecinId; // ID du médecin
    private int patientId; // ID du patient
    private List<EtatRendezVous> etats; // Liste des états associés
    private String lienJitsi; // Lien Jitsi pour la réunion
    private Date date; // Date du rendez-vous
    private Time heure; // Heure du rendez-vous
    private boolean statut; // Statut du rendez-vous (0 ou 1)
    private boolean annule; // Indique si le rendez-vous est annulé
    private String cause; // Cause de l'annulation (si applicable)
    private int etatId;
    // Constructeur

    public RendezVous(String lienJitsi, Date date, Time heure, boolean statut, boolean annule, String cause, int etatId) {
        this.lienJitsi = lienJitsi;
        this.date = date;
        this.heure = heure;
        this.statut = statut;
        this.annule = annule;
        this.cause = cause;
        this.etatId = etatId;
        this.etats = new ArrayList<>();
    }




    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedecinId() {
        return medecinId;
    }

    public void setMedecinId(int medecinId) {
        this.medecinId = medecinId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public List<EtatRendezVous> getEtats() {
        return etats;
    }

    public void addEtat(EtatRendezVous etat) {
        this.etats.add(etat);
        etat.addRendezVous(this); // Ajoute ce rendez-vous à l'état
    }

    public void removeEtat(EtatRendezVous etat) {
        this.etats.remove(etat);
        etat.removeRendezVous(this); // Supprime ce rendez-vous de l'état
    }

    public String getLienJitsi() {
        return lienJitsi;
    }

    public void setLienJitsi(String lienJitsi) {
        this.lienJitsi = lienJitsi;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getHeure() {
        return heure;
    }

    public void setHeure(Time heure) {
        this.heure = heure;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }

    public boolean isAnnule() {
        return annule;
    }

    public void setAnnule(boolean annule) {
        this.annule = annule;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
    public int getEtatId() {
        return etatId;
    }

    public void setEtatId(int etatId) {
        this.etatId = etatId;
    }
    @Override

    public String toString() {
        return "RendezVous{" +
                "id=" + id +
                ", medecinId=" + medecinId +
                ", patientId=" + patientId +
                ", etatId=" + etatId +
                ", lienJitsi='" + lienJitsi + '\'' +
                ", date=" + date +
                ", heure=" + heure +
                ", statut=" + statut +
                ", annule=" + annule +
                ", cause='" + cause + '\'' +
                '}';
    }




}
