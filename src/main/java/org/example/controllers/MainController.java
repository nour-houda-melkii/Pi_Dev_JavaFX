package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {
    @FXML
    private BorderPane contentArea;
    
    @FXML
    private VBox eventsSubMenu;

    @FXML
    public void initialize() {
        // Charger la page d'accueil par défaut
        loadPage("/views/welcome.fxml");
    }

    @FXML
    private void handleDashboard() {
        // Redirection vers la page d'accueil
        loadPage("/views/welcome.fxml");
    }

    @FXML
    private void handleEvents() {
        loadPage("/views/event_list.fxml");
    }

    @FXML
    private void handleCategories() {
        loadPage("/views/categorieEvent_list.fxml");
    }

    @FXML
    private void handleAddEvent() {
        loadPage("/views/event_form.fxml");
    }

    @FXML
    private void toggleEventsSubMenu() {
        boolean isVisible = eventsSubMenu.isVisible();
        eventsSubMenu.setVisible(!isVisible);
        eventsSubMenu.setManaged(!isVisible);
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.setCenter(page);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
            e.printStackTrace();
        }
    }
} 