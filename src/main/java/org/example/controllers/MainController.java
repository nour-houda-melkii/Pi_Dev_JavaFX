package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainController {
    @FXML
    private BorderPane contentArea;
    
    @FXML
    private VBox eventsSubMenu;
    
    @FXML
    private Button dashboardBtn;
    
    @FXML
    private Button usersManagementBtn;
    
    @FXML
    private Button productsManagementBtn;
    
    @FXML
    private Button appointmentsManagementBtn;
    
    @FXML
    private Button eventsManagementBtn;
    
    @FXML
    private Button postsManagementBtn;
    
    @FXML
    private Button complaintsManagementBtn;
    
    // Store all sidebar buttons for easier management
    private List<Button> sidebarButtons;

    @FXML
    public void initialize() {
        // Initialize the list of sidebar buttons
        sidebarButtons = Arrays.asList(
            dashboardBtn,
            usersManagementBtn,
            productsManagementBtn,
            appointmentsManagementBtn,
            eventsManagementBtn,
            postsManagementBtn,
            complaintsManagementBtn
        );
        
        // Set dashboard as active by default
        setActiveButton(dashboardBtn);
        
        // Charger la page d'accueil par défaut
        loadPage("/views/welcome.fxml");
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        // Redirection vers la page d'accueil
        loadPage("/views/welcome.fxml");
    }

    @FXML
    private void handleEvents() {
        setActiveButton(eventsManagementBtn);
        loadPage("/views/event_list.fxml");
    }

    @FXML
    private void handleCategories() {
        setActiveButton(eventsManagementBtn);
        loadPage("/views/categorieEvent_list.fxml");
    }

    @FXML
    private void handleAddEvent() {
        setActiveButton(eventsManagementBtn);
        loadPage("/views/event_form.fxml");
    }

    @FXML
    private void toggleEventsSubMenu() {
        setActiveButton(eventsManagementBtn);
        boolean isVisible = eventsSubMenu.isVisible();
        eventsSubMenu.setVisible(!isVisible);
        eventsSubMenu.setManaged(!isVisible);
    }
    
    @FXML
    void handlePlaceholder(ActionEvent event) {
        // Set the clicked button as active
        if (event.getSource() instanceof Button) {
            setActiveButton((Button) event.getSource());
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à venir");
        alert.setHeaderText("Fonctionnalité non disponible");
        alert.setContentText("Cette fonctionnalité sera disponible dans une future mise à jour.");
        alert.showAndWait();
    }
    
    /**
     * Sets the given button as active and deactivates all others
     */
    private void setActiveButton(Button activeButton) {
        // Remove the active class from all buttons
        sidebarButtons.forEach(button -> button.getStyleClass().remove("active"));
        
        // Add the active class to the current button
        activeButton.getStyleClass().add("active");
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.setCenter(page);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
            e.printStackTrace();
            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de chargement");
            alert.setContentText("Impossible de charger la page demandée.");
            alert.showAndWait();
        }
    }
} 