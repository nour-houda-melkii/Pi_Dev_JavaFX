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

public class UpdateMedecinController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ageField;
    @FXML private ComboBox<Gender> genderComboBox;
    @FXML private ComboBox<Specialite> specialiteComboBox;
    @FXML private TextField licenceField;
    @FXML private TextField addressField;
    private Runnable onMedecinUpdatedCallback;

    private User medecin;
    private final UserService userService = new UserService();

    public void initData(User medecin) {
        this.medecin = medecin;
        populateFields();
    }

    @FXML
    public void initialize() {
        genderComboBox.getItems().setAll(Gender.values());
        specialiteComboBox.getItems().setAll(Specialite.values());
    }

    private void populateFields() {
        firstNameField.setText(medecin.getFirstName());
        lastNameField.setText(medecin.getLastName());
        emailField.setText(medecin.getEmail());
        phoneField.setText(medecin.getPhoneNumber());
        ageField.setText(String.valueOf(medecin.getAge()));
        genderComboBox.setValue(medecin.getGender());
        specialiteComboBox.setValue(medecin.getSpecialite());
        licenceField.setText(medecin.getNumeroLicence());
        addressField.setText(medecin.getAddress());
    }

    public void setOnMedecinUpdatedCallback(Runnable callback) {
        this.onMedecinUpdatedCallback = callback;
    }

    @FXML
    private void handleUpdate() {
        try {
            // Mettre à jour les données du médecin
            medecin.setFirstName(firstNameField.getText());
            medecin.setLastName(lastNameField.getText());
            medecin.setEmail(emailField.getText());
            medecin.setPhoneNumber(phoneField.getText());
            medecin.setAge(Integer.parseInt(ageField.getText()));
            medecin.setGender(genderComboBox.getValue());
            medecin.setSpecialite(specialiteComboBox.getValue());
            medecin.setNumeroLicence(licenceField.getText());
            medecin.setAddress(addressField.getText());

            // Appeler le service pour modifier le médecin en base de données
            userService.modifierMedecin(medecin);

            AlertUtils.showSuccessAlert("Success", "Doctor updated successfully");

            // Fermer la fenêtre
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.close();

            // Notifier le rafraîchissement
            if (onMedecinUpdatedCallback != null) {
                onMedecinUpdatedCallback.run();
            }

        } catch (Exception e) {
            // Gérer les erreurs
            showAlert("Error", "Failed to update doctor: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
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
            showAlert("Error", "Unable to return to the doctor list");
        }
    }

    private void closeWindow() {
        ((Stage) firstNameField.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}