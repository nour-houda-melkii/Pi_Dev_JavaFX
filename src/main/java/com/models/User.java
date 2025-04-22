package com.models;

import java.sql.Timestamp;
import com.demo.enums.Role;
import com.demo.enums.Specialite;
import com.demo.enums.Gender;

public class User {
    private int id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Role role;
    private String address;
    private String phoneNumber;
    private String numeroLicence;
    private int age;
    private Specialite specialite;
    private Gender gender;
    private String verificationCode;
    private Timestamp verificationCodeExpiration;
    private int loginAttempts;          // Nouveau champ pour le nombre de tentatives
    private boolean accountLocked;      // Nouveau champ pour le statut de verrouillage
    private Timestamp lockUntil;
    private  String medicalFile;// Nouveau champ pour la durée de verrouillage

    // Constructeurs
    public User() {}

    // Constructeur complet
    public User(int id, String email, String password, String firstName, String lastName,
                Role role, String address, String phoneNumber, String numeroLicence,
                int age, Specialite specialite, Gender gender, String verificationCode,
                Timestamp verificationCodeExpiration, int loginAttempts,
                boolean accountLocked, Timestamp lockUntil) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
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
        this(id, email, password, firstName, lastName, Role.PATIENT,
                address, phoneNumber, null, age, null, gender, null, null, 0, false, null);
    }

    // Constructeur pour les médecins
    public User(int id, String email, String password, String firstName, String lastName,
                String address, String phoneNumber, int age, Gender gender,
                String numeroLicence, Specialite specialite) {
        this(id, email, password, firstName, lastName, Role.MEDECIN,
                address, phoneNumber, numeroLicence, age, specialite, gender, null, null, 0, false, null);
    }

    // Méthodes de vérification
    public boolean isMedecin() {
        return Role.MEDECIN.equals(this.role);
    }

    public boolean isPatient() {
        return Role.PATIENT.equals(this.role);
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

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

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

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Timestamp getVerificationCodeExpiration() { return verificationCodeExpiration; }
    public void setVerificationCodeExpiration(Timestamp verificationCodeExpiration) {
        this.verificationCodeExpiration = verificationCodeExpiration;
    }

    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }

    public boolean isAccountLocked() { return isAccountLocked(); }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

    public Timestamp getLockUntil() { return lockUntil; }
    public void setLockUntil(Timestamp lockUntil) { this.lockUntil = lockUntil; }

    public String getMedicalFile() { return medicalFile; }
    public void setMedicalFile(String medicalFile) { this.medicalFile = medicalFile; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", loginAttempts=" + loginAttempts +
                ", accountLocked=" + accountLocked +
                ", lockUntil=" + lockUntil +
                ", verificationCode=" + (verificationCode != null ? "***" : "null") +
                ", verificationCodeExpiration=" + verificationCodeExpiration +
                '}';
    }

}