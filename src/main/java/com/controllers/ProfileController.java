package com.controllers;

import com.models.User;
import com.demo.enums.Specialite;
import com.demo.enums.Gender;
import com.services.AuthService;
import com.services.ProfileService;
import com.exceptions.AuthException;
import com.services.ProfileService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

public class ProfileController {
    // Champs pour la carte de profil
    @FXML
    private ImageView profileImage;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    // Champs pour l'onglet Aperçu
    @FXML
    private Label fullNameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label ageLabel;
    @FXML
    private Label genderLabel;
    @FXML
    private Label specialityLabel;
    @FXML
    private Label licenseLabel;
    @FXML
    private VBox doctorDetailsSection;

    // Champs pour l'onglet Modifier le profil
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField ageField;
    @FXML
    private ComboBox<Gender> genderCombo;
    @FXML
    private ComboBox<Specialite> specialityCombo;
    @FXML
    private TextField licenseField;
    @FXML
    private VBox doctorEditSection;

    // Champs pour l'onglet Dossier Médical
    @FXML
    private Tab medicalFileTab;
    @FXML
    private Label medicalFileNameLabel;

    // Champs pour l'onglet Changer le mot de passe
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private AuthService authService;
    private ProfileService profileService;
    private String token;
    private User currentUser;
    private File medicalFile;
    private File selectedMedicalFile;
    @FXML
    private Label medicalFileStatusLabel;




    public void setToken(String token) {
        this.token = token;
        initializeWithToken();
    }

    private void initializeWithToken() {
        try {
            if (token == null || token.isEmpty()) {
                throw new AuthException("Token JWT manquant");
            }

            authService = new AuthService();
            profileService = new ProfileService();
            currentUser = authService.getUserFromToken(token);
            System.out.println(currentUser);


            if (currentUser == null) {
                throw new AuthException("Utilisateur introuvable");
            }

            loadUserData();
            setupUI();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger le profil", e.getMessage());
        }
    }

    private void setupUI() {
        // Configurer les ComboBox
        genderCombo.getItems().setAll(Gender.values());
        specialityCombo.getItems().setAll(Specialite.values());

        // Définir une valeur par défaut si null
        if (currentUser.getGender() == null) {
            genderCombo.getSelectionModel().selectFirst(); // ou une valeur par défaut
        } else {
            genderCombo.setValue(currentUser.getGender());
        }

        // Afficher/masquer les sections selon le rôle
        if (currentUser.isMedecin()) {
            doctorDetailsSection.setVisible(true);
            doctorEditSection.setVisible(true);
            medicalFileTab.setDisable(true);
            userRoleLabel.setText("Médecin");
        } else {
            doctorDetailsSection.setVisible(false);
            doctorEditSection.setVisible(false);
            medicalFileTab.setDisable(false);
            userRoleLabel.setText("Patient");
        }
    }

    private void loadUserData() {
        // Carte de profil
        userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

        System.out.println("Gender from DB: " + currentUser.getGender());
        System.out.println("Gender class: " + (currentUser.getGender() != null ?
                currentUser.getGender().getClass() : "null"));
        System.out.println(currentUser);


        // Onglet Aperçu
        fullNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
        emailLabel.setText(currentUser.getEmail());
        phoneLabel.setText(currentUser.getPhoneNumber());
        addressLabel.setText(currentUser.getAddress());
        ageLabel.setText(String.valueOf(currentUser.getAge()));
        genderLabel.setText(currentUser.getGender() != null ? currentUser.getGender().toString() : "Non spécifié");

        if (currentUser.isMedecin()) {
            specialityLabel.setText(currentUser.getSpecialite().toString());
            licenseLabel.setText(currentUser.getNumeroLicence());
        }

        // Onglet Modifier le profil
        firstNameField.setText(currentUser.getFirstName());
        lastNameField.setText(currentUser.getLastName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhoneNumber());
        addressField.setText(currentUser.getAddress());
        ageField.setText(String.valueOf(currentUser.getAge()));

        // Initialisation du ComboBox de genre
        genderCombo.getItems().setAll(Gender.values());
        if (currentUser.getGender() != null) {
            genderCombo.setValue(currentUser.getGender());
        } else {
            genderCombo.getSelectionModel().selectFirst(); // Valeur par défaut
        }

        if (currentUser.isMedecin()) {
            specialityCombo.setValue(currentUser.getSpecialite());
            licenseField.setText(currentUser.getNumeroLicence());
        }
    }

    @FXML
    private void handleEditImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                profileImage.setImage(image);
                // Ici vous devriez aussi envoyer l'image au serveur
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
        try {
            User updates = new User();
            updates.setFirstName(firstNameField.getText());
            updates.setLastName(lastNameField.getText());
            updates.setEmail(emailField.getText());
            updates.setPhoneNumber(phoneField.getText());
            updates.setAddress(addressField.getText());
            updates.setGender(genderCombo.getValue());

            try {
                updates.setAge(Integer.parseInt(ageField.getText()));
            } catch (NumberFormatException e) {
                throw new AuthException("L'âge doit être un nombre valide");
            }

            if (currentUser.isMedecin()) {
                updates.setSpecialite(specialityCombo.getValue());
                updates.setNumeroLicence(licenseField.getText());
            }

            if (!isValidEmail(updates.getEmail())) {
                throw new AuthException("Format d'email invalide");
            }

            currentUser = authService.updateUserInfo(token, updates);
            loadUserData(); // Rafraîchir les données affichées
            showAlert("Succès", "Profil mis à jour", "Vos informations ont été mises à jour avec succès.");
        } catch (Exception e) {
            showAlert("Erreur", "Échec de la mise à jour", e.getMessage());
        }
    }

    @FXML
    private void handleBrowseMedicalFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un dossier médical");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.jpg", "*.jpeg", "*.png")
        );

        selectedMedicalFile = fileChooser.showOpenDialog(medicalFileNameLabel.getScene().getWindow());
        if (selectedMedicalFile != null) {
            medicalFileNameLabel.setText(selectedMedicalFile.getName());
        }
    }

    @FXML
    private void handleSaveMedicalFile() {
        if (selectedMedicalFile == null) {
            showAlert("Erreur", "Aucun fichier sélectionné", "Veuillez sélectionner un fichier à téléverser.");
            return;
        }

        try {
            // Lire le contenu du fichier
            byte[] fileContent = Files.readAllBytes(selectedMedicalFile.toPath());

            // Appeler le service pour uploader le fichier
            String storedFileName = authService.uploadMedicalFile(
                    token,
                    fileContent,
                    selectedMedicalFile.getName()
            );

            // Mettre à jour l'affichage
            medicalFileNameLabel.setText(storedFileName);
            showAlert("Succès", "Fichier téléversé", "Votre dossier médical a été téléversé avec succès.");
        } catch (IOException e) {
            showAlert("Erreur", "Erreur de lecture", "Impossible de lire le fichier: " + e.getMessage());
        } catch (AuthException e) {
            showAlert("Erreur", "Authentification", e.getMessage());
        } catch (SQLException e) {
            showAlert("Erreur", "Base de données", "Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword.isEmpty() || confirmPassword.isEmpty() || currentPassword.isEmpty()) {
            showAlert("Erreur", "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showAlert("Erreur", "Mots de passe différents", "Les nouveaux mots de passe ne correspondent pas.");
            return;
        }

        try {
            authService.changePassword(token, currentPassword, newPassword);
            showAlert("Succès", "Mot de passe changé", "Votre mot de passe a été changé avec succès.");
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (Exception e) {
            showAlert("Erreur", "Échec du changement", e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }


    @FXML
    private void handleDeleteAccount() {
        // Create confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Account");
        confirmation.setHeaderText("Account Deletion");
        confirmation.setContentText("Are you sure you want to permanently delete your account? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Create password dialog
            Dialog<String> passwordDialog = new Dialog<>();
            passwordDialog.setTitle("Confirmation");
            passwordDialog.setHeaderText("Confirm your password");

            // Set the button types
            ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            passwordDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

            // Create password field
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");

            // Add to dialog
            passwordDialog.getDialogPane().setContent(passwordField);

            // Convert result to password string when confirm button is clicked
            passwordDialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    return passwordField.getText();
                }
                return null;
            });

            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (passwordResult.isPresent() && !passwordResult.get().isEmpty()) {
                try {
                    // 1. Delete the account
                    boolean deleted = profileService.deleteUserAccount(token, passwordResult.get());

                    if (deleted) {

                        // 3. Show success message
                        showAlert("Success", "Account deleted", "Your account has been successfully deleted.");

                        // 4. Redirect to login
                        redirectToLogin();
                    }
                } catch (Exception e) {
                    showAlert("Error", "Deletion failed", e.getMessage());
                }
            }
        }
    }


    @FXML
    private void redirectToLogin() {
        try {
            // Get current window
            Stage currentStage = (Stage) profileImage.getScene().getWindow();

            // Load login scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            // Create new scene
            Scene scene = new Scene(root);

            // Close current window
            currentStage.close();

            // Create new stage for login
            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("Login");
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Redirection failed", "Unable to load login page: " + e.getMessage());

            // Fallback - at least close the current window
            Stage currentStage = (Stage) profileImage.getScene().getWindow();
            currentStage.close();
        }
    }


    @FXML
    private void handleViewMedicalFile() {
        try {
            // 1. Récupérer le fichier depuis le service
            AbstractMap.SimpleEntry<String, byte[]> fileEntry = authService.viewMedicalFile(token);

            // 2. Créer un fichier temporaire pour l'affichage
            File tempFile = File.createTempFile("medical_", fileEntry.getKey().substring(fileEntry.getKey().lastIndexOf(".")));
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileEntry.getValue());
            }

            // 3. Ouvrir le fichier avec l'application par défaut
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                showAlert("Information", "Visualisation",
                        "Le fichier a été récupéré mais ne peut pas être ouvert automatiquement. " +
                                "Il se trouve dans: " + tempFile.getAbsolutePath());
            }
        } catch (AuthException e) {
            showAlert("Erreur", "Authentification", e.getMessage());
        } catch (IOException e) {
            showAlert("Erreur", "Fichier", "Erreur lors de la visualisation: " + e.getMessage());
        }
    }

    @FXML
    private void handleDownloadMedicalFile() {
        try {
            // 1. Demander où sauvegarder le fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le dossier médical");

            // Récupérer le nom du fichier original
            String currentFile = medicalFileNameLabel.getText();
            if (currentFile != null && !currentFile.isEmpty()) {
                fileChooser.setInitialFileName(currentFile);
            } else {
                fileChooser.setInitialFileName("dossier_medical.pdf");
            }

            File destination = fileChooser.showSaveDialog(medicalFileNameLabel.getScene().getWindow());

            if (destination != null) {
                // 2. Télécharger le fichier
                authService.downloadMedicalFile(token, destination.getAbsolutePath());
                showAlert("Succès", "Téléchargement",
                        "Le fichier a été téléchargé avec succès vers: " + destination.getAbsolutePath());
            }
        } catch (AuthException e) {
            showAlert("Erreur", "Authentification", e.getMessage());
        } catch (IOException e) {
            showAlert("Erreur", "Fichier", "Erreur lors du téléchargement: " + e.getMessage());
        }
    }

}

