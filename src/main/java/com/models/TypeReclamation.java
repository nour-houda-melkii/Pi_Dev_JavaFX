package com.models;

public class TypeReclamation {
    private int id;
    private String nom;

    public TypeReclamation() {}
    public TypeReclamation(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    @Override
    public String toString() { return id + " - " + nom; }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TypeReclamation that = (TypeReclamation) obj;
        return id == that.id;
    }
    @Override
    public int hashCode() { return id; }
} 