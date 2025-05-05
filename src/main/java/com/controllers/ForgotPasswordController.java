package com.controllers;

import com.exceptions.AuthException;
import com.services.AuthService;
import com.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class ForgotPasswordController {
    @FXML private TextField emailField;
    @FXML private Label errorLabel;

    private AuthService authService;

    // Méthode pour injecter le AuthService
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }

        try {
            if (authService == null) {
                throw new IllegalStateException("AuthService not initialized");
            }

            String verificationCode = authService.generateAndSaveVerificationCode(email);

            // Si on arrive ici, c'est que tout s'est bien passé
            redirectToCodeVerification(email);

        } catch (AuthException e) {
            // Afficher un message spécifique pour "User not found"
            if (e.getMessage().equals("User not found")) {
                showError("User not found");
            } else {
                showError("Error: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("An unexpected error occurred");
            e.printStackTrace();
        }
    }

    private void redirectToCodeVerification(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/VerifyCode.fxml"));
            Parent root = loader.load();

            // Passer l'email au contrôleur de la page suivante
            VerifyCodeController controller = loader.getController();
            controller.setEmail(email);
            controller.setAuthService(authService);

            // Changer de scène
            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Réinitialisation du mot de passe");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page de vérification");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            // Injecter le authService si nécessaire
            LoginController controller = loader.getController();
            controller.setAuthService(authService);

            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Connexion");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible de retourner à la page de connexion");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}