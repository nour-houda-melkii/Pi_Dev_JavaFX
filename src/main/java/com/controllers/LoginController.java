package com.controllers;

import com.controllers.nour.ProductFrontController;
import com.models.User;
import com.services.AuthService;
import com.sun.javafx.menu.MenuItemBase;
import com.utils.AlertUtils;
import com.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private AuthService authService;

    public void initialize() {
        authService = new AuthService();

        // For debugging - log if there's any stored user ID
        int storedUserId = AuthManager.getUserId();
        LOGGER.info("Stored user ID at login screen: " + storedUserId);
    }


    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        LOGGER.info("Login attempt with email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires");
            return;
        }

        try {
            // Clear any existing auth data before logging in
            AuthManager.clearAll();

            // Authentification
            String token = authService.login(email, password);
            if (token == null || token.isEmpty()) {
                showError("Authentication failed - invalid token");
                return;
            }

            AuthManager.storeToken(token);

            // Récupération de l'utilisateur
            User user = authService.getUserFromToken(token);
            if (user == null || user.getId() <= 0) {
                showError("Failed to retrieve user data");
                return;
            }

            // Store user ID in session
            LOGGER.info("Login successful for user ID: " + user.getId());
            AuthManager.storeUserId(user.getId());

            // Verify the user ID was properly stored
            int storedId = AuthManager.getUserId();
            LOGGER.info("Verified stored user ID: " + storedId);

            if (storedId <= 0) {
                showError("Error saving session data");
                return;
            }

            // Redirection selon le rôle
            if (user.getRoles().contains("ROLE_MEDECIN") || user.getRoles().contains("ROLE_ADMIN")) {
                redirectToAdminDashboard();
            } else {
                redirectToFrontOffice(user.getId());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login error", e);
            showError("Login error: " + e.getMessage());
        }
    }

    private void redirectToAdminDashboard() {
        try {
            LOGGER.info("Redirecting to admin dashboard");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/admin_dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to admin dashboard", e);
            showError("Failed to navigate to admin dashboard: " + e.getMessage());
        }
    }

    // Ajoutez cette nouvelle méthode dans le même contrôleur

    private void redirectToFrontOffice(int userId) {
        try {
            LOGGER.info("Redirecting to front office with user ID: " + userId);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Parent root = loader.load();

            ProductFrontController controller = loader.getController();
            controller.setCurrentUserId(userId);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Products");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to products view", e);
            showError("Failed to navigate to products view: " + e.getMessage());
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
        MenuItemBase errorLabel = null;
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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

    public void setAuthService(AuthService authService) {
    }
}