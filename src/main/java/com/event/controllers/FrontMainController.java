package com.event.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import com.event.services.UserSession;
import com.event.models.User;
import com.event.utils.SessionManager;
import com.event.controllers.FrontEventListController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.Node;
import javafx.application.Platform;

public class FrontMainController implements Initializable {
    
    @FXML
    private BorderPane mainContainer;
    
    private User currentUser;
    private FrontNavbarController navController;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Toujours relire l'utilisateur depuis la session
        this.currentUser = com.event.utils.SessionManager.getCurrentUser();
        if (this.currentUser != null) {
            System.out.println("FrontMainController initialisé avec utilisateur: " + currentUser.getEmail());
        } else {
            System.out.println("FrontMainController initialisé sans utilisateur connecté");
        }
        loadNavbar();
        // Mettre à jour les notifications après un court délai
        Platform.runLater(() -> {
            if (navController != null) {
                navController.setUser(currentUser);
                navController.updateNotificationBadge();
            }
        });
    }
    
    /**
     * Charge le contenu par défaut (liste des événements)
     */
    private void loadDefaultContent() {
        try {
            if (mainContainer == null) {
                System.err.println("ERREUR: mainContainer est null dans loadDefaultContent");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/front_event_list.fxml"));
            Parent eventList = loader.load();
            // Transmettre l'utilisateur au contrôleur
            FrontEventListController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.setUser(currentUser);
                System.out.println("Utilisateur transmis au contrôleur d'événements depuis FrontMainController");
            }
            mainContainer.setCenter(eventList);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du contenu par défaut: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge la barre de navigation
     */
    private void loadNavbar() {
        try {
            if (mainContainer == null) {
                System.err.println("ERREUR: mainContainer est null dans loadNavbar");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/front_navbar.fxml"));
            Parent navbar = loader.load();
            // Récupérer et stocker le contrôleur de la barre de navigation
            navController = loader.getController();
            // Forcer la récupération de l'utilisateur courant
            User user = this.currentUser;
            if (user == null) {
                user = com.event.utils.SessionManager.getCurrentUser();
            }
            if (navController != null && user != null) {
                navController.setUser(user);
            }
            navbar.setUserData(navController);
            mainContainer.setTop(navbar);
            // Essayer de mettre à jour immédiatement les notifications
            if (navController != null) {
                navController.updateNotificationBadge();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la barre de navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Met à jour le badge de notification dans la barre de navigation
     */
    public void updateNotificationBadge() {
        try {
            if (navController != null) {
                navController.updateNotificationBadge();
            } else {
                Node navbar = mainContainer.getTop();
                if (navbar != null) {
                    FrontNavbarController controller = (FrontNavbarController) navbar.getUserData();
                    if (controller != null) {
                        controller.updateNotificationBadge();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du badge de notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère le contrôleur de la barre de navigation
     * 
     * @return Le contrôleur FrontNavbarController
     */
    public FrontNavbarController getNavbarController() {
        return navController;
    }
    
    /**
     * Définit l'utilisateur actuel
     * 
     * @param user L'utilisateur connecté
     */
    public void setUser(User user) {
        this.currentUser = user;
        com.event.utils.SessionManager.setCurrentUser(user);
        System.out.println("Utilisateur défini dans FrontMainController: " + 
                (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
        // Propager à la navbar si elle existe
        if (navController != null) navController.setUser(user);
        Platform.runLater(this::updateNotificationBadge);
    }
} 