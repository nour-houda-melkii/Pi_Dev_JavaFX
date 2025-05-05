package com.controllers;

import com.controllers.nour.ProductFrontController;
import com.services.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;


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
    private Hyperlink profileLink;

    @FXML
    private StackPane contentPane;

    @FXML
    private Button deconnexionButton;


    private String token;

    private AuthService authService = new AuthService();

    public void setToken(String token) {
        this.token = token;
        updateUIForLoggedInUser();
    }

    @FXML
    private void initialize() {
        // Initialisation des actions pour les boutons et liens
        connexionButton.setOnAction(event -> handleConnexion());
        inscriptionButton.setOnAction(event -> handleInscription());
        deconnexionButton.setOnAction(event -> handleDeconnexion());

        accueilLink.setOnAction(event -> handleNavigation("accueil"));
        produitsLink.setOnAction(event -> handleNavigation("produits"));
        evenementsLink.setOnAction(event -> handleNavigation("evenements"));

        // Initialisation existante...
        profileLink.setOnAction(event -> navigateToProfile());

        // Masquer le bouton par défaut, il sera visible seulement si l'utilisateur est connecté
        profileLink.setVisible(false);

        // Charge la page d'accueil par défaut
        handleNavigation("accueil");
    }


    private void updateUIForLoggedInUser() {
        System.out.println(token);
        if (token != null && !token.isEmpty()) {
            // Utilisateur connecté - afficher Profil, masquer Connexion/Inscription
            profileLink.setVisible(true);
            connexionButton.setVisible(false);
            inscriptionButton.setVisible(false);
            deconnexionButton.setVisible(true);
        } else {
            // Utilisateur non connecté - afficher Connexion/Inscription, masquer Profil
            profileLink.setVisible(false);
            connexionButton.setVisible(true);
            inscriptionButton.setVisible(true);
            deconnexionButton.setVisible(false);
        }
    }


    @FXML
    private void handleDeconnexion() {
        logout(); // Utilisation de la méthode logout que vous avez fournie
    }

    @FXML
    private void logout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Déconnexion");
        confirmation.setHeaderText("Confirmer la déconnexion");
        confirmation.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authService.logout(token);
                redirectToLogin();
            } catch (Exception e) {
                showErrorDialog("Erreur", "Échec de la déconnexion", e.getMessage());
            }
        }
    }

    private void redirectToLogin() {
        this.token = null;
        updateUIForLoggedInUser();
        handleConnexion();
        showInfoDialog("Déconnexion", "Vous avez été déconnecté avec succès.");
    }


    private void showInfoDialog(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    @FXML
    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/profile.fxml"));
            Node profileContent = loader.load();

            ProfileController profileController = loader.getController();
            profileController.setToken(this.token);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profileContent);
        } catch (IOException e) {
            showErrorDialog("Erreur", "Impossible de charger le profil", e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
        } catch (IOException e) {
            showErrorDialog("Erreur", "Impossible de charger le contenu", e.getMessage());
        }
    }

    @FXML
    private void handleNavigation(String page) {
        switch(page) {

            case "produits":
                loadProductsContent();
                break;

        }
    }

    private void loadProductsContent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Node productsContent = loader.load();

            // Get controller to pass any necessary data
            ProductFrontController productController = loader.getController();

            // If you have cart or other data to pass to the product controller
            // productController.setCartProducts(cart);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(productsContent);
        } catch (IOException e) {
            showErrorDialog("Erreur", "Impossible de charger les produits", e.getMessage());
            e.printStackTrace();
        }
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


    private void handleRendezVous() {
        System.out.println("Prendre rendez-vous");
        // Ajoutez ici la logique pour prendre rendez-vous
    }

    private void handleNewsletter() {
        System.out.println("S'abonner à la newsletter");
        // Ajoutez ici la logique pour l'abonnement à la newsletter
    }
}