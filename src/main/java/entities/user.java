package entities;

import javafx.beans.property.*;

public class user {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty roles = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty gender = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty phoneNumber = new SimpleStringProperty();
    private final StringProperty numeroLicence = new SimpleStringProperty();
    private final StringProperty specialite = new SimpleStringProperty();
    private final IntegerProperty age = new SimpleIntegerProperty();
    private final StringProperty medicalFile = new SimpleStringProperty();
    private final BooleanProperty isVerified = new SimpleBooleanProperty();
    private final StringProperty verificationToken = new SimpleStringProperty();

    // Constructors
    public user() {
        // Default constructor
    }

    // Getters and Setters for properties
    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getEmail() {
        return email.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public String getRoles() {
        return roles.get();
    }

    public StringProperty rolesProperty() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles.set(roles);
    }

    public String getPassword() {
        return password.get();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public String getFirstName() {
        return firstName.get();
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public String getLastName() {
        return lastName.get();
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public String getGender() {
        return gender.get();
    }

    public StringProperty genderProperty() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender.set(gender);
    }

    public String getAddress() {
        return address.get();
    }

    public StringProperty addressProperty() {
        return address;
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public String getPhoneNumber() {
        return phoneNumber.get();
    }

    public StringProperty phoneNumberProperty() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber.set(phoneNumber);
    }

    public String getNumeroLicence() {
        return numeroLicence.get();
    }

    public StringProperty numeroLicenceProperty() {
        return numeroLicence;
    }

    public void setNumeroLicence(String numeroLicence) {
        this.numeroLicence.set(numeroLicence);
    }

    public String getSpecialite() {
        return specialite.get();
    }

    public StringProperty specialiteProperty() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite.set(specialite);
    }

    public int getAge() {
        return age.get();
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public void setAge(int age) {
        this.age.set(age);
    }

    public String getMedicalFile() {
        return medicalFile.get();
    }

    public StringProperty medicalFileProperty() {
        return medicalFile;
    }

    public void setMedicalFile(String medicalFile) {
        this.medicalFile.set(medicalFile);
    }

    public boolean isIsVerified() {
        return isVerified.get();
    }

    public BooleanProperty isVerifiedProperty() {
        return isVerified;
    }





    public String getPhone() {
        return null;
    }

    public String getFullName() {
        return null;
    }
}