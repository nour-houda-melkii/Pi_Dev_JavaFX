package entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Reponse {
    private int id;
    private String contenu;
    private LocalDate dateReponse;
    private int reclamationId;
    private String status;


    public static final String STATUS_EN_ATTENTE = "En attente";
    public static final String STATUS_EN_COURS = "En cours";
    public static final String STATUS_TERMINE = "Terminé";
    public static final String STATUS_REJETE = "Rejeté";

    // Constructeurs
    public Reponse() {
        this.dateReponse = LocalDate.now();
        this.status = STATUS_EN_ATTENTE;
    }

    public Reponse(String contenu, LocalDate dateReponse, int reclamationId) {
        this.contenu = contenu;
        this.dateReponse =  dateReponse != null ? dateReponse : LocalDate.now();
        this.reclamationId = reclamationId;
        this.status = status != null ? status : STATUS_EN_ATTENTE;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDate getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDate dateReponse) {
        this.dateReponse = dateReponse;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {
        return dateReponse.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}