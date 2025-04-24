package com.controllers;

import com.demo.enums.Gender;
import com.demo.enums.Role;
import com.models.User;
import com.services.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PatientRegistrationController {

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

    @FXML private ComboBox<String> genderComboBox;
    @FXML private Label genderError;

    @FXML private PasswordField passwordField;
    @FXML private Label passwordError;

    @FXML private CheckBox termsCheckBox;
    @FXML private Label termsError;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegistration(ActionEvent event) {
        try {
            if (!validateForm()) return;

            User patient = new User();
            patient.setFirstName(firstNameField.getText());
            patient.setLastName(lastNameField.getText());
            patient.setEmail(emailField.getText());
            patient.setPhoneNumber(phoneField.getText());
            patient.setAddress(addressField.getText());
            patient.setAge(ageSpinner.getValue());

            String genderText = genderComboBox.getValue();
            Gender gender;
            switch (genderText) {
                case "Male" -> gender = Gender.male;
                case "Female" -> gender = Gender.female;
                default -> throw new IllegalArgumentException("Invalid gender");
            }
            patient.setGender(gender);
            patient.setPassword(passwordField.getText());
            patient.setRoles(List.of(User.ROLE_USER));

            authService.registerPatient(patient);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful!");
            handleLoginLink(event);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid gender selection");
        }
    }

    private boolean validateForm() {
        // Clear all error labels
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        passwordError.setText("");
        genderError.setText("");
        termsError.setText("");
        phoneError.setText("");
        addressError.setText("");

        boolean isValid = true;

        if (firstNameField.getText().isEmpty()) {
            firstNameError.setText("First name is required");
            isValid = false;
        }

        if (lastNameField.getText().isEmpty()) {
            lastNameError.setText("Last name is required");
            isValid = false;
        }

        String email = emailField.getText();
        if (email.isEmpty()) {
            emailError.setText("Email is required");
            isValid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            emailError.setText("Invalid email format");
            isValid = false;
        }


        String phone = phoneField.getText();
        if (phone.isEmpty()) {
            phoneError.setText("Phone number is required");
            isValid = false;
        } else if (!phone.matches("^[259]\\d{7}$")) {
            phoneError.setText("Phone must start with 2, 5, or 9 and be 8 digits long");
            isValid = false;
        }


        if (addressField.getText().isEmpty()) {
            addressError.setText("Address is required");
            isValid = false;
        }


        if (passwordField.getText().length() < 6) {
            passwordError.setText("Password must be at least 6 characters");
            isValid = false;
        }

        if (genderComboBox.getValue() == null || genderComboBox.getValue().isEmpty()) {
            genderError.setText("Gender is required");
            isValid = false;
        }

        if (!termsCheckBox.isSelected()) {
            termsError.setText("You must agree to the terms");
            isValid = false;
        }

        return isValid;
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            // Charger le FXML de login
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/views/Login.fxml"));

            // Récupérer la scène actuelle
            Scene currentScene = ((Node) event.getSource()).getScene();

            // Remplacer le contenu de la scène existante
            currentScene.setRoot(loginRoot);

            // Mettre à jour le titre de la fenêtre
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
