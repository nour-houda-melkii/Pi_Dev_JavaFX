package entity;

import java.time.LocalDate;

public class Reclamation {
    private String id;
    private String typeReclamation;
    private String description;
    private LocalDate dateReclamation;
    private String medecin;
    private String photoPath;

    public Reclamation(String id, String typeReclamation, String description,
                       LocalDate dateReclamation, String medecin, String photoPath) {
        this.id = id;
        this.typeReclamation = typeReclamation;
        this.description = description;
        this.dateReclamation = dateReclamation;
        this.medecin = medecin;
        this.photoPath = photoPath;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTypeReclamation() { return typeReclamation; }
    public void setTypeReclamation(String typeReclamation) { this.typeReclamation = typeReclamation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateReclamation() { return dateReclamation; }
    public void setDateReclamation(LocalDate dateReclamation) { this.dateReclamation = dateReclamation; }

    public String getMedecin() { return medecin; }
    public void setMedecin(String medecin) { this.medecin = medecin; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}