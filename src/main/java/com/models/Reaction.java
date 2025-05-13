package com.models;

import java.time.LocalDate;

public class Reaction {
    private int id;
    private int reclamationId; // Pour les réactions aux réclamations
    private int reponseId;     // Pour les réactions aux réponses
    private String type;
    private String utilisateur;
    private LocalDate dateReaction;

    // Constructeurs
    public Reaction() {
    }

    public Reaction(int id, int reclamationId, int reponseId, String type, String utilisateur, LocalDate dateReaction) {
        this.id = id;
        this.reclamationId = reclamationId;
        this.reponseId = reponseId;
        this.type = type;
        this.utilisateur = utilisateur;
        this.dateReaction = dateReaction;
    }

    // Pour les réactions aux réclamations
    public Reaction(int id, int reclamationId, String type, String utilisateur, LocalDate dateReaction) {
        this.id = id;
        this.reclamationId = reclamationId;
        this.reponseId = 0; // Pas de réponse associée
        this.type = type;
        this.utilisateur = utilisateur;
        this.dateReaction = dateReaction;
    }

    // Pour les réactions aux réponses
    public static Reaction forReponse(int id, int reponseId, String type, String utilisateur, LocalDate dateReaction) {
        Reaction reaction = new Reaction();
        reaction.id = id;
        reaction.reponseId = reponseId;
        reaction.reclamationId = 0; // Pas de réclamation associée directement
        reaction.type = type;
        reaction.utilisateur = utilisateur;
        reaction.dateReaction = dateReaction;
        return reaction;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(int reclamationId) {
        this.reclamationId = reclamationId;
    }

    public int getReponseId() {
        return reponseId;
    }

    public void setReponseId(int reponseId) {
        this.reponseId = reponseId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public LocalDate getDateReaction() {
        return dateReaction;
    }

    public void setDateReaction(LocalDate dateReaction) {
        this.dateReaction = dateReaction;
    }

    @Override
    public String toString() {
        if (reponseId > 0) {
            return "Reaction{" +
                    "id=" + id +
                    ", reponseId=" + reponseId +
                    ", type='" + type + '\'' +
                    ", utilisateur='" + utilisateur + '\'' +
                    ", dateReaction=" + dateReaction +
                    '}';
        } else {
            return "Reaction{" +
                    "id=" + id +
                    ", reclamationId=" + reclamationId +
                    ", type='" + type + '\'' +
                    ", utilisateur='" + utilisateur + '\'' +
                    ", dateReaction=" + dateReaction +
                    '}';
        }
    }
}