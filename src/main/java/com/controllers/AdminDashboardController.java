package com.controllers;

import com.services.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class AdminDashboardController {
    @FXML private VBox sidebar;
    @FXML private StackPane contentPane;
    @FXML private VBox productMenu;
    @FXML private VBox userMenu;

    private String token;
    private AuthService authService = new AuthService();

    public void setToken(String token) {
        this.token = token;
        // Vous pouvez ajouter d'autres logiques si nécessaire
    }

    @FXML
    private void initialize() {
        // Initialization code
    }

    @FXML
    private void toggleSidebar() {
        sidebar.setVisible(!sidebar.isVisible());
    }

    @FXML
    private void showDashboard() {
        loadContent("/com/views/DashboardContent.fxml");
    }

    @FXML
    private void showAddProduct() {
        loadContent("/com/views/AddProduct.fxml");
    }


    @FXML
    private void showDoctors() {
        loadContent("/com/views/ListMedecin.fxml");
    }

    @FXML
    private void showPatients() {
        loadContent("/com/views/ListPatient.fxml");
    }

    // Méthode pour charger la page AjouterMedecin
    @FXML
    private void showAddDoctor() {
        loadContent("/com/views/AjouterMedecin.fxml");
    }

    // Méthode pour charger d'autres pages (exemple : liste des patients)
    @FXML
    private void showAddPatient() {
        loadContent("/com/views/AjouterPatient.fxml");
    }
    @FXML
    private void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/profile.fxml"));
            Parent content = loader.load();

            // Passez le token au ProfileController
            ProfileController profileController = loader.getController();
            profileController.setToken(this.token);

            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le profil", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void logout() {
        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Perform logout
                authService.logout(token);

                // Redirect to login
                redirectToLogin();
            } catch (Exception e) {
                showAlert("Error", "Logout failed", e.getMessage());
            }
        }
    }

    private void redirectToLogin() {
        try {
            // Get current window
            Stage currentStage = (Stage) sidebar.getScene().getWindow();

            // Load login scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            // Get login controller and set auth service
            LoginController loginController = loader.getController();
            loginController.setAuthService(authService);

            // Replace current scene
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Redirection failed", "Could not load login page: " + e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleProductMenu() {
        productMenu.setVisible(!productMenu.isVisible());
        productMenu.setManaged(!productMenu.isManaged());
    }

    @FXML
    private void toggleUserMenu() {
        userMenu.setVisible(!userMenu.isVisible());
        userMenu.setManaged(!userMenu.isManaged());
    }
}