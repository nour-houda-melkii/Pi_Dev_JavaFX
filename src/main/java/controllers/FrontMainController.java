package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import services.UserSession;
import models.User;
import utils.SessionManager;

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
        // Récupérer l'utilisateur connecté
        try {
            // Vérifier d'abord SessionManager
            this.currentUser = SessionManager.getCurrentUser();
            
            // Si aucun utilisateur dans SessionManager, essayer UserSession
            if (this.currentUser == null) {
                this.currentUser = UserSession.getInstance().getLoggedInUser();
                
                // Si on trouve un utilisateur dans UserSession mais pas dans SessionManager, synchroniser
                if (this.currentUser != null) {
                    SessionManager.setCurrentUser(this.currentUser);
                    System.out.println("Synchronisation SessionManager depuis UserSession: " + currentUser.getEmail());
                }
            } else {
                // Si on trouve un utilisateur dans SessionManager mais pas dans UserSession, synchroniser
                if (!UserSession.getInstance().isLoggedIn()) {
                    UserSession.getInstance().setLoggedInUser(this.currentUser);
                    System.out.println("Synchronisation UserSession depuis SessionManager: " + currentUser.getEmail());
                }
            }
            
            if (this.currentUser != null) {
                System.out.println("FrontMainController initialisé avec utilisateur: " + 
                    currentUser.getEmail() + " (rôle: " + currentUser.getRole() + ")");
            } else {
                System.out.println("FrontMainController initialisé sans utilisateur connecté");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Charger d'abord la barre de navigation pour avoir son contrôleur
        loadNavbar();
        
        // Puis charger le contenu par défaut
        loadDefaultContent();
        
        // Mettre à jour les notifications après un court délai
        Platform.runLater(() -> {
            if (navController != null) {
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_event_list.fxml"));
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_navbar.fxml"));
            Parent navbar = loader.load();
            
            // Récupérer et stocker le contrôleur de la barre de navigation
            navController = loader.getController();
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
     * Définit l'utilisateur actuel
     * 
     * @param user L'utilisateur connecté
     */
    public void setUser(User user) {
        this.currentUser = user;
        System.out.println("Utilisateur défini dans FrontMainController: " + 
                (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
        
        // Mettre à jour les notifications après avoir défini l'utilisateur
        Platform.runLater(this::updateNotificationBadge);
    }
} 