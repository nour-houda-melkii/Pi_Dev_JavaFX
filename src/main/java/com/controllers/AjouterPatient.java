package com.controllers;

import com.demo.enums.Gender;
import com.demo.enums.Role;
import com.models.User;
import com.services.UserService;
import com.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class AjouterPatient  {

    // Champs du formulaire
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumberField;
    @FXML private TextField adressField;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> genderComboBox;

    // Labels d'erreur
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneNumberError;
    @FXML private Label adressError;
    @FXML private Label ageError;
    @FXML private Label genderError;
    @FXML private Button backButton;
    private Runnable onPatientAddedCallback;

    private UserService userService;

    @FXML
    public void initialize() {
        // Initialisation du ComboBox Genre avec les valeurs d'affichage
        genderComboBox.getItems().addAll("Male", "Female");
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @FXML
    private void handleSave() {
        if (validateForm()) {
            try {
                User patient = createPatientFromForm();
                userService.ajouterPatient(patient);

                AlertUtils.showSuccessAlert("Success", "Patient added successfully");

                // Fermer la fenêtre
                Stage stage = (Stage) firstNameField.getScene().getWindow();
                stage.close();

                // Notifier le callback
                if (onPatientAddedCallback != null) {
                    onPatientAddedCallback.run();
                }

            } catch (NumberFormatException e) {
                AlertUtils.showErrorAlert("Format Error", "Please enter valid numeric values");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Error", "Failed to add patient: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setOnPatientAddedCallback(Runnable callback) {
        this.onPatientAddedCallback = callback;
    }


    // Méthode pour rediriger vers la liste des patients
    private void redirectToPatientList() {
        try {
            // Charger la vue de la liste des patients
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ListPatient.fxml"));
            Parent root = loader.load();

            // Obtenir la scène actuelle
            Scene currentScene = backButton.getScene();

            // Remplacer le contenu de la scène actuelle
            currentScene.setRoot(root);

            // Option alternative : obtenir la fenêtre et changer la scène
            // Stage stage = (Stage) backButton.getScene().getWindow();
            // stage.setScene(new Scene(root));
            // stage.setTitle("Liste des Patients");
            // stage.show();

        } catch (IOException e) {
            // Gérer les erreurs de chargement de la vue
            AlertUtils.showErrorAlert("Error", "Unable to load the patient list.");
            e.printStackTrace();
        }
    }

    private User createPatientFromForm() {
        User patient = new User();
        patient.setFirstName(firstNameField.getText().trim());
        patient.setLastName(lastNameField.getText().trim());
        patient.setEmail(emailField.getText().trim());
        patient.setPhoneNumber(phoneNumberField.getText().trim());
        patient.setAddress(adressField.getText().trim());
        patient.setAge(Integer.parseInt(ageField.getText().trim()));

        // Conversion de la valeur sélectionnée en enum Gender
        String selectedGender = genderComboBox.getValue();
        if (selectedGender != null) {
            switch (selectedGender) {
                case "Male":
                    patient.setGender(Gender.male);
                    break;
                case "Female":
                    patient.setGender(Gender.female);
                    break;
                default:
            }
        }

        return patient;
    }

    private void showSuccessAndClose(String title, String message) {
        AlertUtils.showSuccessAlert(title, message);
    }

    @FXML
    private void handleBackToList() {
        try {
            // Charger la vue de la liste des patients
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            // Obtenir la scène actuelle
            Scene currentScene = backButton.getScene();

            // Remplacer le contenu de la scène actuelle
            currentScene.setRoot(root);

            // Ou alternative: obtenir la fenêtre et changer la scène
            // Stage stage = (Stage) backButton.getScene().getWindow();
            // stage.setScene(new Scene(root));
            // stage.setTitle("Liste des Patients");
            // stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to return to the patient list.");
        }
    }

    // Méthode utilitaire pour afficher les alertes
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validation du prénom
        if (firstNameField.getText().trim().isEmpty()) {
            firstNameError.setText("First name is required");
            isValid = false;
        } else {
            firstNameError.setText("");
        }

        // Validation du nom
        if (lastNameField.getText().trim().isEmpty()) {
            lastNameError.setText("Last name is required");
            isValid = false;
        } else {
            lastNameError.setText("");
        }

        // Validation de l'email
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            emailError.setText("Invalid email");
            isValid = false;
        } else {
            emailError.setText("");
        }

// Phone validation
        String phone = phoneNumberField.getText().trim();
        if (phone.isEmpty() || !phone.matches("^[259][0-9]{7}$")) {
            phoneNumberError.setText("Invalid number (8 digits, starts with 2, 5, or 9)");
            isValid = false;
        } else {
            phoneNumberError.setText("");
        }


        // Validation de l'adresse
        if (adressField.getText().trim().isEmpty()) {
            adressError.setText("Adress is required");
            isValid = false;
        } else {
            adressError.setText("");
        }

        // Validation de l'âge
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age < 0 || age > 120) {
                ageError.setText("Invalid age (0-120)");
                isValid = false;
            } else {
                ageError.setText("");
            }
        } catch (NumberFormatException e) {
            ageError.setText("Invalid age");
            isValid = false;
        }

        // Validation du genre
        if (genderComboBox.getValue() == null) {
            genderError.setText("Gender is required");
            isValid = false;
        } else {
            genderError.setText("");
        }

        return isValid;
    }
}