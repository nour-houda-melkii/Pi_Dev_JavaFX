package com.event.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import com.event.models.User;
import java.io.IOException;
import com.event.controllers.FrontEventListController;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

public class WelcomeController {
    
    @FXML
    private Button frontEndButton;
    
    @FXML
    private Label userLabel;
    
    private User user;
    
    @FXML
    public void initialize() {
        // Initialisation du contrôleur
    }
    
    public void setUser(User user) {
        this.user = user;
        
        // Mettre à jour l'interface utilisateur si nécessaire
        if (userLabel != null && user != null) {
            userLabel.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
        }
    }

    @FXML
    private void handleShowEvents() {
        try {
            // Charger la vue front-end des événements
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_event_list.fxml"));
            Parent root = loader.load();
            
            // Créer une nouvelle fenêtre
            Stage eventStage = new Stage();
            
            // Définir la fenêtre parent (backend) comme propriétaire
            Stage backendStage = (Stage) frontEndButton.getScene().getWindow();
            eventStage.initOwner(backendStage);
            
            // Important : Ne pas définir de modalité pour permettre l'interaction avec la fenêtre backend
            eventStage.initModality(Modality.NONE);
            
            // Obtenir le contrôleur et lui passer l'utilisateur
            FrontEventListController controller = loader.getController();
            if (user != null) {
                controller.setUser(user);
            }
            
            // Créer la scène
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            // Configurer la fenêtre
            eventStage.setScene(scene);
            eventStage.setTitle("SAHATECH - Liste des Événements");
            
            // Configurer en plein écran
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            eventStage.setX(screenBounds.getMinX());
            eventStage.setY(screenBounds.getMinY());
            eventStage.setWidth(screenBounds.getWidth());
            eventStage.setHeight(screenBounds.getHeight());
            
            // Empêcher le redimensionnement
            eventStage.setResizable(false);
            
            // Afficher la nouvelle fenêtre sans bloquer la fenêtre backend
            eventStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors du chargement de la vue: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) frontEndButton.getScene().getWindow();
            
            // Ouvrir la page de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            Stage loginStage = new Stage();
            loginStage.setTitle("Connexion - Gestion d'Événements");
            loginStage.setScene(scene);
            loginStage.setWidth(500);
            loginStage.setHeight(600);
            loginStage.centerOnScreen();
            
            // Fermer la fenêtre actuelle et afficher la page de login
            currentStage.close();
            loginStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors du chargement de la page de login: " + e.getMessage());
        }
    }

    @FXML
    private void handleFrontEndAccess(ActionEvent event) {
        // Charger la liste des événements dans le centre du BorderPane parent
        try {
            Node node = frontEndButton.getScene().getRoot();
            BorderPane mainContainer = null;
            if (node instanceof BorderPane) {
                mainContainer = (BorderPane) node;
            } else {
                while (node.getParent() != null) {
                    node = node.getParent();
                    if (node instanceof BorderPane) {
                        mainContainer = (BorderPane) node;
                        break;
                    }
                }
            }
            if (mainContainer != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/front_event_list.fxml"));
                Parent eventList = loader.load();
                mainContainer.setCenter(eventList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 