package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private List<String> roles;
    private String firstName;
    private String lastName;
    private String gender;
    private String adress;
    private String phoneNumber;
    private String numeroLicence;
    private Integer age;
    private String specialite;
    private String medicalFile;
    private boolean isVerified;
    private String verificationToken;
    private int failedLoginAttempts;
    private boolean isBlocked;
    private LocalDateTime blockedUntil;
    private String status;

    // Constructeur vide
    public User() {
        this.roles = new ArrayList<>();
        this.isVerified = false;
        this.failedLoginAttempts = 0;
        this.isBlocked = false;
    }

    // Constructeur avec tous les champs
    public User(int id, String nom, String prenom, String email, String password, String role) {
        this();
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Méthode utilitaire pour convertir la chaîne JSON en liste
    public void setRolesFromJson(String rolesJson) {
        // Exemple simple de parsing, à adapter selon le format exact
        if (rolesJson != null && !rolesJson.isEmpty()) {
            rolesJson = rolesJson.replace("[", "").replace("]", "").replace("\"", "");
            String[] rolesArray = rolesJson.split(",");
            this.roles = Arrays.asList(rolesArray);
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public void setNumeroLicence(String numeroLicence) {
        this.numeroLicence = numeroLicence;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getMedicalFile() {
        return medicalFile;
    }

    public void setMedicalFile(String medicalFile) {
        this.medicalFile = medicalFile;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Méthode pour afficher les informations de l'utilisateur
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }

    // Méthode pour obtenir le nom complet
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    // Méthode utilitaire pour vérifier si un utilisateur a un rôle spécifique
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    // Méthode utilitaire pour ajouter un rôle
    public void addRole(String role) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        if (!this.roles.contains(role)) {
            this.roles.add(role);
        }
    }
} 