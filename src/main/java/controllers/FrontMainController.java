package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;
import javafx.application.Platform;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class FrontMainController {
    
    @FXML
    private BorderPane mainContainer;
    
    private FrontEventListController eventListController;
    
    @FXML
    public void initialize() {
        try {
            // Chargement initial de la page d'événements avec accès au contrôleur
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_event_list.fxml"));
            Parent page = loader.load();
            eventListController = loader.getController();
            mainContainer.setCenter(page);
            
            // Configurer un rafraîchissement périodique
            setupPeriodicRefresh();
            
            // Configurer le rafraîchissement lors de la réactivation de la fenêtre
            setupWindowFocusListener();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la page initiale: " + e.getMessage());
        }
    }
    
    /**
     * Configure un rafraîchissement périodique des événements (toutes les 30 secondes)
     */
    private void setupPeriodicRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    // Attendre 30 secondes
                    Thread.sleep(30000);
                    
                    // Rafraîchir sur le thread JavaFX
                    Platform.runLater(this::refreshEvents);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }
    
    /**
     * Configure un écouteur pour rafraîchir quand la fenêtre reprend le focus
     */
    private void setupWindowFocusListener() {
        Platform.runLater(() -> {
            Scene scene = mainContainer.getScene();
            if (scene != null) {
                Stage stage = (Stage) scene.getWindow();
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> refreshEvents());
                stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        // La fenêtre a repris le focus
                        refreshEvents();
                    }
                });
            }
        });
    }
    
    /**
     * Rafraîchit la liste des événements
     */
    public void refreshEvents() {
        if (eventListController != null) {
            System.out.println("Rafraîchissement de la liste des événements...");
            eventListController.refreshEvents();
        }
    }
    
    /**
     * Charge une page dans le conteneur central
     */
    public void loadPage(String fxmlPath) {
        try {
            // Si c'est la page des événements, utiliser l'instance existante pour permettre le rafraîchissement
            if ("/views/front_event_list.fxml".equals(fxmlPath) && eventListController != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent page = loader.load();
                eventListController = loader.getController();
                mainContainer.setCenter(page);
            } else {
                // Pour les autres pages
                Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
                mainContainer.setCenter(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
        }
    }
} 