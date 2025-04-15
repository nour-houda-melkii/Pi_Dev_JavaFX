package entity;

public class Medecin {
    private int id;
    private String nom;

    // Constructeur par défaut
    public Medecin() {
    }

    // Constructeur avec paramètres
    public Medecin(int id, String nom) {
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

    // Méthode toString() pour l'affichage
    @Override
    public String toString() {
        return id + " - " + nom;
    }

    // Méthode equals() pour comparer deux médecins
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Medecin medecin = (Medecin) obj;
        return id == medecin.id;
    }

    // Méthode hashCode()
    @Override
    public int hashCode() {
        return id;
    }
}