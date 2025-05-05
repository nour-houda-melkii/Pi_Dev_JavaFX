package com.models;

import java.util.Date;

public class Notification {
    private int id;
    private Integer patientId;
    private Integer medecinId;
    private String message;
    private boolean isRead;

    // Constructeur par défaut
    public Notification() {
    }

    // Constructeur avec paramètres
    public Notification(Integer patientId, Integer medecinId, String message) {
        this.patientId = patientId;
        this.medecinId = medecinId;
        this.message = message;
        this.isRead = false; // Par défaut, le message n'est pas lu
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public Integer getMedecinId() {
        return medecinId;
    }

    public void setMedecinId(Integer medecinId) {
        this.medecinId = medecinId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // Pour marquer la notification comme lue
    public void markAsRead() {
        this.isRead = true;
    }

    @Override
    public String toString() {
        return "NotificationMessage{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", medecinId=" + medecinId +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}