package com.controllers;


import com.controllers.front.CreatePostController;
import com.controllers.front.PostListController;
import com.exceptions.AuthException;
import com.services.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
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
    private Hyperlink rdvlink;

    @FXML
    private Hyperlink postLink;

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
        articlesLink.setOnAction(event -> handleNavigation("articles"));

        // Initialisation existante...
        profileLink.setOnAction(event -> navigateToProfile());
        rdvlink.setOnAction(event -> navigateToRdv());

        // Masquer le bouton par défaut, il sera visible seulement si l'utilisateur est connecté
        profileLink.setVisible(false);
    }

    private void navigateToRdv() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ajout.fxml"));
            Node profileContent = loader.load();

            Ajout ajoutController = loader.getController();
            ajoutController.setToken(this.token);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profileContent);
        } catch (IOException e) {
            // Gestion des erreurs
        }
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
            // Gestion des erreurs
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
            case "accueil":
                loadContent("/com/views/homeContent.fxml");
                break;
            case "produits":
                loadContent("/com/views/productsContent.fxml");
                break;
            // etc.
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


    @FXML
    private void showCreatePostForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/CreateFormPost.fxml"));
            Parent form = loader.load();

            // Pass current user ID to the form controller
            CreatePostController controller = loader.getController();
            controller.setToken(this.token);

            // Create a new stage for the form
            Stage formStage = new Stage();
            formStage.setTitle("Create New Post");
            formStage.setScene(new Scene(form));
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (AuthException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void showPostsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/PostList.fxml"));
            Parent content = loader.load();

            // Clear existing content and add new content
            contentPane.getChildren().setAll(content);

            // Only set the current user if the controller is PostListController
            if (loader.getController() instanceof PostListController) {
                PostListController controller = loader.getController();
                controller.setToken(this.token);
            }
            System.out.println(this.token);

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to error message
            contentPane.getChildren().setAll(new Label("Failed to load: " + "/views/Front/PostList.fxml"));
        } catch (AuthException e) {
            throw new RuntimeException(e);
        }
    }
}
