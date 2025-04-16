package entity;

import java.time.LocalDate;

public class Reponse {
    private int id;
    private String contenu;
    private LocalDate dateReponse;
    private int reclamationId;

    // Constructeurs
    public Reponse() {}

    public Reponse(String contenu, LocalDate dateReponse, int reclamationId) {
        this.contenu = contenu;
        this.dateReponse = dateReponse;
        this.reclamationId = reclamationId;
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
}