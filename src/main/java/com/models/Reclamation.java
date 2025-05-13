package com.models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Reclamation {
    private int id;
    private int typeReclamationId;
    private String typeReclamationName;
    private String description;
    private LocalDate dateReclamation;
    private int medecinId;
    private String medecinName;
    private String photoPath;
    private String status;

    public Reclamation(int id, int typeReclamationId, String typeReclamationName, String description,
                       LocalDate dateReclamation, int medecinId, String medecinName,
                       String photoPath, String status) {
        this.id = id;
        this.typeReclamationId = typeReclamationId;
        this.typeReclamationName = typeReclamationName;
        this.description = description;
        this.dateReclamation = dateReclamation;
        this.medecinId = medecinId;
        this.medecinName = medecinName;
        this.photoPath = photoPath;
        this.status = status;
    }

    public Reclamation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTypeReclamationId() { return typeReclamationId; }
    public void setTypeReclamationId(int typeReclamationId) { this.typeReclamationId = typeReclamationId; }

    public String getTypeReclamationName() { return typeReclamationName; }
    public void setTypeReclamationName(String typeReclamationName) { this.typeReclamationName = typeReclamationName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateReclamation() { return dateReclamation; }
    public void setDateReclamation(LocalDate dateReclamation) { this.dateReclamation = dateReclamation; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public String getMedecinName() { return medecinName; }
    public void setMedecinName(String medecinName) { this.medecinName = medecinName; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFormattedDate() {
        return dateReclamation != null ? dateReclamation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }
} 