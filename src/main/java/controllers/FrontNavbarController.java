package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class FrontNavbarController {

    @FXML
    private void handleHome() {
        loadPage("/views/front_home.fxml");
    }

    @FXML
    private void handleEvents() {
        loadPage("/views/front_event_list.fxml");
    }

    @FXML
    private void handleAbout() {
        loadPage("/views/front_about.fxml");
    }

    @FXML
    private void handleContact() {
        loadPage("/views/front_contact.fxml");
    }
    
    /**
     * Charge une page dans le conteneur principal (BorderPane)
     */
    private void loadPage(String fxmlPath) {
        try {
            // Trouver le BorderPane parent
            BorderPane mainContainer = (BorderPane) this.getClass()
                                                     .getMethod("getScene")
                                                     .invoke(this)
                                                     .getClass()
                                                     .getMethod("getRoot")
                                                     .invoke(this.getClass().getMethod("getScene").invoke(this));
            
            // Charger la nouvelle page
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainContainer.setCenter(page);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
            e.printStackTrace();
        }
    }
} 