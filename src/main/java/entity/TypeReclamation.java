package entity;

public class TypeReclamation {
    private int id;
    private String nom;

    // Constructeurs
    public TypeReclamation() {
    }

    public TypeReclamation(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Méthode toString()
    @Override
    public String toString() {
        return id + " - " + nom;
    }

    // Méthodes equals() et hashCode()
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TypeReclamation that = (TypeReclamation) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}