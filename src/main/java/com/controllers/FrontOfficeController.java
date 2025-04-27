package com.controllers;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class FrontOfficeController {

    @FXML
    private Button connexionButton;

    @FXML
    private Button inscriptionButton;

    @FXML
    private Hyperlink accueilLink;

    @FXML
    private Hyperlink produitsLink;

    @FXML
    private Hyperlink evenementsLink;

    @FXML
    private Hyperlink articlesLink;

    @FXML
    private TextField searchField;

    @FXML
    private Button rendezvousButton;

    @FXML
    private Button newsletterButton;

    @FXML
    private void initialize() {
        // Initialisation des actions pour les boutons et liens
        connexionButton.setOnAction(event -> handleConnexion());
        inscriptionButton.setOnAction(event -> handleInscription());

        accueilLink.setOnAction(event -> handleNavigation("accueil"));
        produitsLink.setOnAction(event -> handleNavigation("produits"));
        evenementsLink.setOnAction(event -> handleNavigation("evenements"));
        articlesLink.setOnAction(event -> handleNavigation("articles"));

        rendezvousButton.setOnAction(event -> handleRendezVous());
        newsletterButton.setOnAction(event -> handleNewsletter());
    }
    private void navigateTo(String fxmlPath, Button sourceButton, String errorTitle) {
        try {
            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Obtenir et mettre à jour la scène actuelle
            Scene currentScene = sourceButton.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);

                // Ajuster la taille de la fenêtre
                Stage stage = (Stage) currentScene.getWindow();
                stage.sizeToScene();
            } else {
                throw new IllegalStateException("La scène actuelle n'est pas disponible");
            }

        } catch (IOException | IllegalStateException e) {
            System.err.println("Erreur lors de la navigation: " + e.getMessage());
            e.printStackTrace();

            showErrorDialog(
                    errorTitle,
                    "Impossible de charger la page demandée",
                    "Le fichier " + fxmlPath + " est introuvable ou corrompu."
            );
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    @FXML
    private void handleConnexion() {
        navigateTo("/com/views/login.fxml", connexionButton, "Erreur de connexion");
    }

    @FXML
    private void handleInscription() {
        navigateTo("/com/views/registrationChoice.fxml", inscriptionButton, "Erreur d'inscription");
    }

    private void handleNavigation(String page) {
        System.out.println("Navigation vers: " + page);
        // Ajoutez ici la logique pour changer de page
    }

    private void handleRendezVous() {
        System.out.println("Prendre rendez-vous");
        // Ajoutez ici la logique pour prendre rendez-vous
    }

    private void handleNewsletter() {
        System.out.println("S'abonner à la newsletter");
        // Ajoutez ici la logique pour l'abonnement à la newsletter
    }
}
