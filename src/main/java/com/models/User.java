package com.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import com.demo.enums.Specialite;
import com.demo.enums.Gender;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class User {
    private int id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private List<String> roles = new ArrayList<>();
    private String address;
    private String phoneNumber;
    private String numeroLicence;
    private int age;
    private Specialite specialite;
    private Gender gender;
    private String verificationCode;
    private Timestamp verificationCodeExpiration;
    private int loginAttempts;
    private boolean accountLocked;
    private Timestamp lockUntil;
    private String medicalFile;
    private boolean isVerified = false;
    private String status = "non_verifie";

    // Constantes pour les rôles
    public static final String ROLE_USER = "ROLE_USER";    // Patient
    public static final String ROLE_MEDECIN = "ROLE_MEDECIN"; // Médecin
    public static final String ROLE_ADMIN = "ROLE_ADMIN";   // Admin

    // Constructeurs
    public User() {
        // Par défaut, chaque utilisateur est un patient (ROLE_USER)
        this.roles.add(ROLE_USER);
    }

    // Constructeur complet
    public User(int id, String email, String password, String firstName, String lastName,
                List<String> roles, String address, String phoneNumber, String numeroLicence,
                int age, Specialite specialite, Gender gender, String verificationCode,
                Timestamp verificationCodeExpiration, int loginAttempts,
                boolean accountLocked, Timestamp lockUntil) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = new ArrayList<>(roles);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.numeroLicence = numeroLicence;
        this.age = age;
        this.specialite = specialite;
        this.gender = gender;
        this.verificationCode = verificationCode;
        this.verificationCodeExpiration = verificationCodeExpiration;
        this.loginAttempts = loginAttempts;
        this.accountLocked = accountLocked;
        this.lockUntil = lockUntil;
    }

    // Constructeur pour les patients
    public User(int id, String email, String password, String firstName, String lastName,
                String address, String phoneNumber, int age, Gender gender) {
        this(id, email, password, firstName, lastName, List.of(ROLE_USER),
                address, phoneNumber, null, age, null, gender, null, null, 0, false, null);
    }

    // Constructeur pour les médecins
    public User(int id, String email, String password, String firstName, String lastName,
                String address, String phoneNumber, int age, Gender gender,
                String numeroLicence, Specialite specialite) {
        this(id, email, password, firstName, lastName, List.of(ROLE_MEDECIN),
                address, phoneNumber, numeroLicence, age, specialite, gender, null, null, 0, false, null);
    }

    // Constructeur pour les admins
    public User(int id, String email, String password, String firstName, String lastName,
                String address, String phoneNumber, int age, Gender gender,
                boolean isAdmin) {
        this(id, email, password, firstName, lastName, List.of(ROLE_ADMIN),
                address, phoneNumber, null, age, null, gender, null, null, 0, false, null);
    }

    // Méthodes de vérification des rôles
    public boolean isMedecin() {
        return this.roles.contains(ROLE_MEDECIN);
    }

    public boolean isPatient() {
        return this.roles.contains(ROLE_USER);
    }

    public boolean isAdmin() {
        return this.roles.contains(ROLE_ADMIN);
    }

    // Méthodes utilitaires pour les rôles
    public void addRole(String role) {
        if (!this.roles.contains(role)) {
            this.roles.add(role);
        }
    }

    public void removeRole(String role) {
        this.roles.remove(role);
    }

    public boolean hasRole(String role) {
        return this.roles.contains(role);
    }

    // Méthode pour vérifier si le code est valide
    public boolean isVerificationCodeValid(String code) {
        if (verificationCode == null || verificationCodeExpiration == null) {
            return false;
        }
        return verificationCode.equals(code) &&
                verificationCodeExpiration.after(new Timestamp(System.currentTimeMillis()));
    }

    // Méthode pour vérifier si le compte est verrouillé
    public boolean isCurrentlyLocked() {
        if (accountLocked && lockUntil != null) {
            if (lockUntil.after(new Timestamp(System.currentTimeMillis()))) {
                return true;
            } else {
                // Déverrouillage automatique
                this.accountLocked = false;
                this.lockUntil = null;
                this.loginAttempts = 0;
                return false;
            }
        }
        return false;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }

    public void setRoles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNumeroLicence() { return numeroLicence; }
    public void setNumeroLicence(String numeroLicence) { this.numeroLicence = numeroLicence; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Specialite getSpecialite() { return specialite; }
    public void setSpecialite(Specialite specialite) { this.specialite = specialite; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Timestamp getVerificationCodeExpiration() { return verificationCodeExpiration; }
    public void setVerificationCodeExpiration(Timestamp verificationCodeExpiration) {
        this.verificationCodeExpiration = verificationCodeExpiration;
    }

    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }

    public boolean isAccountLocked() { return accountLocked; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

    public Timestamp getLockUntil() { return lockUntil; }
    public void setLockUntil(Timestamp lockUntil) { this.lockUntil = lockUntil; }

    public String getMedicalFile() { return medicalFile; }
    public void setMedicalFile(String medicalFile) { this.medicalFile = medicalFile; }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // Dans la classe User, modifiez le getter pour les rôles
    public String getRolesAsJson() {
        return new Gson().toJson(this.roles);
    }

    // Et le setter correspondant
    public void setRolesFromJson(String jsonRoles) {
        if (jsonRoles == null || jsonRoles.isEmpty()) {
            this.roles = new ArrayList<>();
            return;
        }

        try {
            // Essayez d'abord de parser comme List<String>
            this.roles = new Gson().fromJson(jsonRoles, new TypeToken<List<String>>(){}.getType());
        } catch (Exception e) {
            // Si échec, essayez comme String simple
            this.roles = new ArrayList<>();
            this.roles.add(jsonRoles.replace("\"", "").trim());
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", roles=" + roles +
                ", loginAttempts=" + loginAttempts +
                ", accountLocked=" + accountLocked +
                ", lockUntil=" + lockUntil +
                ", verificationCode=" + (verificationCode != null ? "***" : "null") +
                ", verificationCodeExpiration=" + verificationCodeExpiration +
                '}';
    }
}