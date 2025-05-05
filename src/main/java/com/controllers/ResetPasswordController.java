package com.controllers;

import com.services.AuthService;
import com.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;

public class ResetPasswordController {
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private String email;
    private AuthService authService;
    private String verificationCode;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setVerificationCode(String code) {
        this.verificationCode = code;
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        try {
            // Réinitialiser le mot de passe
            authService.resetPasswordWithCode(email, verificationCode,newPassword);
            AlertUtils.showSuccessAlert("Success", "Password has been reset successfully");

            // Rediriger vers la page de login
            handleBackToLogin();

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setAuthService(authService);

            Scene currentScene = newPasswordField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Login");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Error", "Could not return to login page");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}