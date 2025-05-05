package com.controllers;

import com.models.User;
import com.services.UserService;
import com.demo.enums.Gender;
import com.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class UpdatePatientController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField ageField;
    @FXML private ComboBox<Gender> genderComboBox;

    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label addressError;
    @FXML private Label ageError;
    @FXML private Label genderError;
    private Runnable onPatientUpdatedCallback;

    private UserService userService = new UserService();
    private User patientToUpdate;

    @FXML
    public void initialize() {
        // Initialiser la ComboBox pour le genre
        genderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
    }

    public void initData(User patient) {
        this.patientToUpdate = patient;

        // Remplir les champs avec les données existantes
        firstNameField.setText(patient.getFirstName());
        lastNameField.setText(patient.getLastName());
        emailField.setText(patient.getEmail());
        phoneField.setText(patient.getPhoneNumber());
        addressField.setText(patient.getAddress());
        ageField.setText(String.valueOf(patient.getAge()));
        genderComboBox.setValue(patient.getGender());
    }

    // Dans UpdatePatientController.java
    @FXML
    private void handleUpdate() {
        if (validateForm()) {
            try {
                updatePatientFields(); // Met à jour l'objet patient

                boolean success = userService.modifierPatient(patientToUpdate);

                if (!success) {
                    throw new RuntimeException("Update operation failed");
                }

                AlertUtils.showSuccessAlert("Success", "Patient updated successfully");

                // Fermer la fenêtre
                Stage stage = (Stage) firstNameField.getScene().getWindow();
                stage.close();

                // Notifier le callback
                if (onPatientUpdatedCallback != null) {
                    onPatientUpdatedCallback.run();
                }

            } catch (Exception e) {
                AlertUtils.showErrorAlert("Error", "Failed to update patient: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setOnPatientUpdatedCallback(Runnable callback) {
        this.onPatientUpdatedCallback = callback;
    }

    private void updatePatientFields() {
        patientToUpdate.setFirstName(firstNameField.getText().trim());
        patientToUpdate.setLastName(lastNameField.getText().trim());
        patientToUpdate.setEmail(emailField.getText().trim());
        patientToUpdate.setPhoneNumber(phoneField.getText().trim());
        patientToUpdate.setAddress(addressField.getText().trim());
        patientToUpdate.setAge(Integer.parseInt(ageField.getText().trim()));
        patientToUpdate.setGender(genderComboBox.getValue());
    }

    private void showAlertAndReturnToList(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait().ifPresent(response -> {
            // Retour à la liste après fermeture de l'alerte
            returnToList();
        });
    }


    private void returnToList() {
        try {
            // Fermer la fenêtre actuelle
            closeWindow();

            // Recharger la liste des patients
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            // Obtenir la scène actuelle et la remplacer
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to return to the patient list");
        }
    }

    @FXML
    private void handleBackToList() {
        // Simple confirmation avant de retourner sans sauvegarder
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Return without saving");
        alert.setContentText("Do you really want to exit without saving the changes?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                returnToList();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Réinitialiser les erreurs
        resetErrors();

        // Validation du prénom
        if (firstNameField.getText().isEmpty()) {
            firstNameError.setText("First name is required");
            firstNameError.setVisible(true);
            isValid = false;
        }

        // Validation du nom
        if (lastNameField.getText().isEmpty()) {
            lastNameError.setText("Last name is required");
            lastNameError.setVisible(true);
            isValid = false;
        }

        // Validation de l'email
        if (emailField.getText().isEmpty() || !emailField.getText().contains("@")) {
            emailError.setText("Invalid email");
            emailError.setVisible(true);
            isValid = false;
        }

        // Validation de l'âge
        try {
            int age = Integer.parseInt(ageField.getText());
            if (age <= 0) {
                ageError.setText("Invalid age");
                ageError.setVisible(true);
                isValid = false;
            }
        } catch (NumberFormatException e) {
            ageError.setText("Age must be a number");
            ageError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void resetErrors() {
        firstNameError.setVisible(false);
        lastNameError.setVisible(false);
        emailError.setVisible(false);
        phoneError.setVisible(false);
        addressError.setVisible(false);
        ageError.setVisible(false);
        genderError.setVisible(false);
    }

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}