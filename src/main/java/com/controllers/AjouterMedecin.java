package com.controllers;

import com.models.User;
import com.demo.enums.Gender;
import com.demo.enums.Specialite;
import com.services.UserService;
import com.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class AjouterMedecin {
    // Champs du formulaire
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ageField;
    @FXML private ComboBox<Gender> genderComboBox;
    @FXML private ComboBox<Specialite> specialiteComboBox;
    @FXML private TextField licenceField;
    @FXML private TextField addressField;
    @FXML private Button backButton;

    // Labels d'erreur
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label ageError;
    @FXML private Label genderError;
    @FXML private Label specialiteError;
    @FXML private Label licenceError;
    @FXML private Label addressError;
    private Runnable onMedecinAddedCallback;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Initialisation des ComboBox
        genderComboBox.getItems().setAll(Gender.values());
        specialiteComboBox.getItems().setAll(Specialite.values());

        // Configuration des prompts
        phoneField.setPromptText("e.g. 55263521");
        licenceField.setPromptText("e.g. ABC12345");
    }

    public void setOnMedecinAddedCallback(Runnable callback) {
        this.onMedecinAddedCallback = callback;
    }
    @FXML
    private void handleSave() {
        if (validateForm()) {
            try {
                User newMedecin = createMedecinFromForm();
                userService.ajouterMedecin(newMedecin);

                AlertUtils.showSuccessAlert("Doctor successfully added",
                        "A welcome email has been sent to the doctor.");

                // Fermer la fenêtre actuelle au lieu de rediriger
                Stage stage = (Stage) firstNameField.getScene().getWindow(); // Utilisez n'importe quel node de votre formulaire
                stage.close();

                if (onMedecinAddedCallback != null) {
                    onMedecinAddedCallback.run();
                }

            } catch (Exception e) {
                AlertUtils.showErrorAlert("Error", "Failed to add doctor: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleCancel() {
        redirectToMedecinList();
    }

    private User createMedecinFromForm() {
        User medecin = new User();
        medecin.setFirstName(firstNameField.getText().trim());
        medecin.setLastName(lastNameField.getText().trim());
        medecin.setEmail(emailField.getText().trim());
        medecin.setPhoneNumber(phoneField.getText().trim());
        medecin.setAge(Integer.parseInt(ageField.getText().trim()));
        medecin.setGender(genderComboBox.getValue());
        medecin.setSpecialite(specialiteComboBox.getValue());
        medecin.setNumeroLicence(licenceField.getText().trim());
        medecin.setAddress(addressField.getText().trim());

        return medecin;
    }

    private void redirectToMedecinList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            Scene currentScene = backButton.getScene();
            currentScene.setRoot(root);

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Error", "Unable to load the doctor list.");
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;
        resetErrorMessages();

        // Validation du prénom
        if (firstNameField.getText().trim().isEmpty()) {
            showError(firstNameError, "First name is required");
            isValid = false;
        }

        // Validation du nom
        if (lastNameField.getText().trim().isEmpty()) {
            showError(lastNameError, "Last name is required");
            isValid = false;
        }

        // Validation de l'email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError(emailError, "Email is required");
            isValid = false;
        } else if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError(emailError, "Invalid email format");
            isValid = false;
        }

        // Validation du téléphone
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            showError(phoneError, "Phone number is required");
            isValid = false;
        } else if (!phone.matches("^[259][0-9]{7}$")) {
            showError(phoneError, "Invalid number (8 digits, starts with 2, 5, or 9)");
            isValid = false;
        }

        // Validation de l'âge
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age < 18 || age > 80) {
                showError(ageError, "Invalid age (18-80)");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showError(ageError, "Invalid age");
            isValid = false;
        }

        // Validation du genre
        if (genderComboBox.getValue() == null) {
            showError(genderError, "Gender is required");
            isValid = false;
        }

        // Validation de la spécialité
        if (specialiteComboBox.getValue() == null) {
            showError(specialiteError, "Speciality is required");
            isValid = false;
        }

        // Validation du numéro de licence
        String licence = licenceField.getText().trim();
        if (licence.isEmpty()) {
            showError(licenceError, "Licence number is required");
            isValid = false;
        } else if (!licence.matches("^[A-Z]{3}[0-9]{5}$")) {
            showError(licenceError, "Invalid format (3 letters + 5 digits)");
            isValid = false;
        }

        // Validation de l'adresse
        if (addressField.getText().trim().isEmpty()) {
            showError(addressError, "Address is required");
            isValid = false;
        }

        return isValid;
    }

    private void resetErrorMessages() {
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        phoneError.setText("");
        ageError.setText("");
        genderError.setText("");
        specialiteError.setText("");
        licenceError.setText("");
        addressError.setText("");
    }

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }
}