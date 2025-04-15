package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import java.io.IOException;

public class WelcomeController {
    
    @FXML
    private Button frontEndButton;

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
} 