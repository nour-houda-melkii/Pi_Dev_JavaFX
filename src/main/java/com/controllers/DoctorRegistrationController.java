package com.controllers;

import com.demo.enums.Gender;
import com.demo.enums.Role;
import com.demo.enums.Specialite;
import com.models.User;
import com.services.AuthService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class DoctorRegistrationController implements Initializable {

    // Champs de formulaire
    @FXML private TextField firstNameField;
    @FXML private Label firstNameError;

    @FXML private TextField lastNameField;
    @FXML private Label lastNameError;

    @FXML private TextField emailField;
    @FXML private Label emailError;

    @FXML private TextField phoneField;
    @FXML private Label phoneError;

    @FXML private TextField addressField;
    @FXML private Label addressError;

    @FXML private Spinner<Integer> ageSpinner;
    @FXML private Label ageError;

    @FXML private ComboBox<String> genderComboBox;
    @FXML private Label genderError;

    @FXML private ComboBox<Specialite> specialtyComboBox;
    @FXML private Label specialtyError;

    @FXML private PasswordField passwordField;
    @FXML private Label passwordError;

    @FXML private TextField licenseField;
    @FXML private Label licenseError;

    @FXML private CheckBox termsCheckBox;
    @FXML private Label termsError;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser les ComboBox
        genderComboBox.getItems();
        specialtyComboBox.setItems(FXCollections.observableArrayList(Specialite.values()));

        // Valeur par défaut pour l'âge
        ageSpinner.getValueFactory().setValue(30);
    }

    @FXML
    private void handleRegistration(ActionEvent event) {
        try {
            if (!validateForm()) {
                return;
            }

            User doctor = new User();
            doctor.setFirstName(firstNameField.getText());
            doctor.setLastName(lastNameField.getText());
            doctor.setEmail(emailField.getText());
            doctor.setPhoneNumber(phoneField.getText());
            doctor.setAddress(addressField.getText());
            doctor.setAge(ageSpinner.getValue());

            // Conversion du genre
            String genderText = genderComboBox.getValue();
            Gender gender = switch (genderText) {
                case "Male" -> Gender.male;
                case "Female" -> Gender.female;
                default -> throw new IllegalArgumentException("Invalid gender");
            };
            doctor.setGender(gender);

            doctor.setPassword(passwordField.getText());
            doctor.setRoles(List.of(User.ROLE_MEDECIN));
            doctor.setSpecialite(specialtyComboBox.getValue());
            doctor.setNumeroLicence(licenseField.getText());

            // Enregistrement
            authService.registerMedecin(doctor);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Doctor registered successfully!");
            handleLoginLink(event);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid gender selection");
        }
    }

    private boolean validateForm() {
        // Réinitialiser les erreurs
        clearErrors();

        boolean isValid = true;

        // Validation du prénom
        if (firstNameField.getText().isEmpty()) {
            firstNameError.setText("First name is required");
            isValid = false;
        }

        // Validation du nom
        if (lastNameField.getText().isEmpty()) {
            lastNameError.setText("Last name is required");
            isValid = false;
        }

        // Validation de l'email
        String email = emailField.getText();
        if (email.isEmpty()) {
            emailError.setText("Email is required");
            isValid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            emailError.setText("Invalid email format");
            isValid = false;
        }

        // Validation du téléphone
        String phone = phoneField.getText();
        if (phone.isEmpty()) {
            phoneError.setText("Phone number is required");
            isValid = false;
        } else if (!phone.matches("^[259]\\d{7}$")) {
            phoneError.setText("Phone must start with 2, 5, or 9 and be 8 digits");
            isValid = false;
        }

        // Validation de l'adresse
        if (addressField.getText().isEmpty()) {
            addressError.setText("Address is required");
            isValid = false;
        }

        // Validation du genre
        if (genderComboBox.getValue() == null) {
            genderError.setText("Gender is required");
            isValid = false;
        }

        // Validation de la spécialité
        if (specialtyComboBox.getValue() == null) {
            specialtyError.setText("Specialty is required");
            isValid = false;
        }

        // Validation du mot de passe
        if (passwordField.getText().length() < 6) {
            passwordError.setText("Password must be at least 6 characters");
            isValid = false;
        }

// Validation du numéro de licence (format : ABC12345)
        String license = licenseField.getText().trim();
        if (license.isEmpty()) {
            licenseError.setText("Le numéro de licence est requis");
            isValid = false;
        } else if (!license.matches("[A-Z]{3}\\d{5}")) {
            licenseError.setText("Format invalide. Exemple : ABC12345");
            isValid = false;
        }

        // Validation des conditions
        if (!termsCheckBox.isSelected()) {
            termsError.setText("You must agree to the terms");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        phoneError.setText("");
        addressError.setText("");
        genderError.setText("");
        specialtyError.setText("");
        passwordError.setText("");
        licenseError.setText("");
        termsError.setText("");
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/views/Login.fxml"));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(loginRoot);

            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle("Login");

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to load login page: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}