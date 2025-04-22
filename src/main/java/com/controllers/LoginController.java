package com.controllers;

import com.services.AuthService;
import com.utils.AlertUtils;
import com.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;

    private AuthService authService;

    // Ajoutez cette méthode
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }


    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires");
            return;
        }

        try {
            String token = authService.login(email, password);

            // Stocker le token dans votre système de gestion d'authentification
            AuthManager.storeToken(token);  // <-- Nouvelle ligne pour stocker le token

            // Si l'authentification réussit
            redirectToAdminDashboard();


        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ForgotPassword.fxml"));
            Parent root = loader.load();

            // Injectez le service ici
            ForgotPasswordController controller = loader.getController();
            controller.setAuthService(this.authService); // Utilisez le même authService que LoginController

            // Remplacez la scène actuelle
            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Mot de passe oublié");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page de réinitialisation");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoogleLogin() {
        // Implémentation de la connexion Google
        AlertUtils.showInfoAlert("Information", "Google login will be implemented soon");
    }

    @FXML
    private void handleFacebookLogin() {
        // Implémentation de la connexion Facebook
        AlertUtils.showInfoAlert("Information", "Facebook login will be implemented soon");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void redirectToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            // Passer le token si nécessaire
            AdminDashboardController controller = loader.getController();
            controller.setToken(AuthManager.getStoredToken());  // <-- Passage du token

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Erreur lors du chargement du dashboard");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/registrationChoice.fxml"));
            Parent root = loader.load();

            // Injectez le service si nécessaire
            // RegistrationChoiceController controller = loader.getController();
            // controller.setAuthService(this.authService);

            // Remplacez la scène actuelle
            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Choix d'inscription");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page d'inscription");
            e.printStackTrace();
        }
    }

}