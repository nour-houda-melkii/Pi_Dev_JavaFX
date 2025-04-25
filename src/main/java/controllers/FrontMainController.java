package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import services.UserSession;
import models.User;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.Node;

public class FrontMainController implements Initializable {
    
    @FXML
    private BorderPane mainContainer;
    
    private User currentUser;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Récupérer l'utilisateur connecté
        try {
            this.currentUser = UserSession.getInstance().getLoggedInUser();
            System.out.println("FrontMainController initialisé avec utilisateur: " + 
                    currentUser.getEmail() + " (rôle: " + currentUser.getRole() + ")");
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
        
        loadDefaultContent();
        loadNavbar();
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
            if (currentUser != null) {
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
            
            // Stocker le contrôleur de la barre de navigation dans les userData pour y accéder plus tard
            FrontNavbarController navController = loader.getController();
            navbar.setUserData(navController);
            
            mainContainer.setTop(navbar);
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
            Node navbar = mainContainer.getTop();
            if (navbar != null) {
                FrontNavbarController navController = (FrontNavbarController) navbar.getUserData();
                if (navController != null) {
                    navController.updateNotificationBadge();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du badge de notification: " + e.getMessage());
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
    }
} 