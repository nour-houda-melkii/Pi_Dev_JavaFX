package com.controllers;

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

public class VerifyCodeController {
    @FXML private TextField codeField;
    @FXML private Label errorLabel;

    private String email;
    private AuthService authService;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showError("Please enter the verification code");
            return;
        }

        try {
            // Vérifier le code
            boolean isValid = authService.verifyCode(email, code);

            if (isValid) {
                // Rediriger vers la page de nouveau mot de passe
                redirectToResetPassword();
            } else {
                showError("Invalid verification code");
            }
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void redirectToResetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ResetPassword.fxml"));
            Parent root = loader.load();

            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);
            controller.setVerificationCode(codeField.getText().trim()); // Transmettre le code
            controller.setAuthService(authService);

            Scene currentScene = codeField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Reset Password");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Error", "Could not open reset password page");
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

            Scene currentScene = codeField.getScene();
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