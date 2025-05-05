package com.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;
import java.io.IOException;

public class RegistrationChoice {

    @FXML
    private Button patientButton;

    @FXML
    private Button medecinButton;

    @FXML
    private Hyperlink loginLink;

    // Add initialize method to verify injection
    @FXML
    public void initialize() {
        System.out.println("Controller initialized");
        System.out.println("Login link: " + loginLink);
        System.out.println("Patient button: " + patientButton);
        System.out.println("Medecin button: " + medecinButton);
    }

    @FXML
    private void handlePatientRegistration(ActionEvent event) {
        try {
            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/PatientRegistration.fxml"));
            Parent root = loader.load();

            // Récupérer la scène actuelle
            Scene currentScene = ((Node) event.getSource()).getScene();

            // Remplacer le contenu de la scène existante
            currentScene.setRoot(root);

            // Optionnel: Redimensionner la fenêtre si nécessaire
            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle("Patient Registration");
            stage.sizeToScene(); // Ajuste la taille de la fenêtre au nouveau contenu

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to load patient registration form: " + e.getMessage());
        }
    }

    @FXML
    private void handleMedecinRegistration(ActionEvent event) {
        try {
            // 1. Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/DoctorRegistration.fxml"));
            Parent root = loader.load();

            // 2. Récupérer la scène actuelle
            Scene currentScene = ((Node) event.getSource()).getScene();

            // 3. Remplacer le contenu de la scène existante
            currentScene.setRoot(root);

            // 4. Mettre à jour le titre de la fenêtre
            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle("Doctor Registration");

            // 5. Optionnel: Ajuster la taille de la fenêtre
            stage.sizeToScene();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to load doctor registration form: " + e.getMessage());

            // Debug: afficher le chemin du fichier
            System.err.println("Attempted to load from: " +
                    getClass().getResource("/com/views/DoctorRegistration.fxml"));
        }
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            // 1. Charger la nouvelle vue (Login.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent loginView = loader.load();

            // 2. Obtenir la scène actuelle
            Scene currentScene = loginLink.getScene();

            // 3. Remplacer le contenu de la scène existante
            currentScene.setRoot(loginView);

            // 4. Optionnel: Redimensionner la fenêtre si nécessaire
            Stage stage = (Stage) currentScene.getWindow();
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (IOException e) {
            System.err.println("Erreur de chargement de la page de login:");
            e.printStackTrace();
            showErrorAlert("Erreur", "Impossible de charger la page de connexion");
        }
    }

    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}