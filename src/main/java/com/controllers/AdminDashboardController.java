package com.controllers;

import com.services.AuthService;
import com.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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

    // Correction: Utiliser les mêmes noms que dans le FXML
    @FXML private Label totalUsersLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalPatientsLabel;

    private String token;
    private AuthService authService = new AuthService();
    private UserService userService = new UserService();

    public void setToken(String token) {
        this.token = token;
        loadStats(); // Charger les stats quand le token est défini
    }

    @FXML
    private void initialize() {
        loadStats(); // Charger les stats à l'initialisation
    }

    private void loadStats() {
        try {
            int totalUsers = userService.countTotalUsers();
            int totalDoctors = userService.countTotalMedecins();
            int totalPatients = userService.countTotalPatients();

            // Mettre à jour les labels
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(totalUsers));
            if (totalDoctorsLabel != null) totalDoctorsLabel.setText(String.valueOf(totalDoctors));
            if (totalPatientsLabel != null) totalPatientsLabel.setText(String.valueOf(totalPatients));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSidebar() {
        sidebar.setVisible(!sidebar.isVisible());
    }

    @FXML
    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent content = loader.load();

            // Obtenir la référence au contrôleur
            AdminDashboardController controller = loader.getController();


            // Mettre à jour les statistiques
            controller.loadStats();

            // Remplacer le contenu
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le dashboard", e.getMessage());
        }
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

    @FXML
    private void showAddDoctor() {
        loadContent("/com/views/AjouterMedecin.fxml");
    }

    @FXML
    private void showAddPatient() {
        loadContent("/com/views/AjouterPatient.fxml");
    }

    @FXML
    private void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/profile.fxml"));
            Parent content = loader.load();

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
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authService.logout(token);
                redirectToLogin();
            } catch (Exception e) {
                showAlert("Error", "Logout failed", e.getMessage());
            }
        }
    }

    private void redirectToLogin() {
        try {
            Stage currentStage = (Stage) sidebar.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setAuthService(authService);

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