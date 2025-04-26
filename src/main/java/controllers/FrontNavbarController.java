package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import entities.NotificationHistory;
import services.NotificationService;
import services.UserSession;
import models.User;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import utils.SessionManager;
import java.util.ArrayList;

public class FrontNavbarController implements Initializable {
    
    @FXML
    private StackPane notificationBadge;
    
    @FXML
    private Label notificationCount;
    
    @FXML
    private MenuButton notificationMenuButton;
    
    private NotificationService notificationService;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationService = new NotificationService();
        
        // Initial badge load
        updateNotificationBadge();
        
        // Add a listener to update notifications when menu button is showing
        notificationMenuButton.setOnShowing(evt -> {
            updateNotificationBadge();
        });
    }
    
    /**
     * Met à jour le badge de notification avec le nombre de notifications non lues
     */
    public void updateNotificationBadge() {
        try {
            // Réinitialiser les éléments du menu
            notificationMenuButton.getItems().clear();
            
            // Vérifier si un utilisateur est connecté (vérifier à la fois UserSession et SessionManager)
            boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
            User loggedInUser = null;
            
            // Si UserSession n'a pas d'utilisateur, essayer avec SessionManager
            if (!isLoggedIn) {
                try {
                    loggedInUser = SessionManager.getCurrentUser();
                    isLoggedIn = loggedInUser != null && loggedInUser.getEmail() != null && !loggedInUser.getEmail().isEmpty();
                    
                    // Si un utilisateur est trouvé dans SessionManager mais pas dans UserSession, les synchroniser
                    if (isLoggedIn) {
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                        System.out.println("Synchronisation UserSession depuis SessionManager: " + loggedInUser.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'accès à SessionManager: " + e.getMessage());
                }
            } else {
                loggedInUser = UserSession.getInstance().getLoggedInUser();
            }
            
            if (isLoggedIn && loggedInUser != null) {
                int userId = loggedInUser.getId();
                
                // Récupérer les notifications non lues et les notifications totales
                List<NotificationHistory> unreadNotifications = new ArrayList<>();
                List<NotificationHistory> allNotifications = new ArrayList<>();
                
                // Utiliser un try-catch pour chaque appel individuel afin d'éviter les échecs complets
                try {
                    unreadNotifications = notificationService.getUnreadNotificationsForUser(userId);
                } catch (Exception e) {
                    System.err.println("Impossible de récupérer les notifications non lues: " + e.getMessage());
                }
                
                try {
                    allNotifications = notificationService.getNotificationsForUser(userId);
                } catch (Exception e) {
                    System.err.println("Impossible de récupérer toutes les notifications: " + e.getMessage());
                }
                
                int unreadCount = unreadNotifications.size();
                
                // Mettre à jour le badge
                notificationCount.setText(String.valueOf(unreadCount));
                notificationBadge.setVisible(unreadCount > 0);
                
                // Populate the MenuButton items
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM HH:mm");
                
                if (allNotifications.isEmpty()) {
                    MenuItem emptyItem = new MenuItem("Aucune notification");
                    emptyItem.setDisable(true);
                    notificationMenuButton.getItems().add(emptyItem);
                } else {
                    // Tri inverse (plus récentes en premier)
                    allNotifications.sort((a, b) -> {
                        if (a.getSentDate() == null) return 1;
                        if (b.getSentDate() == null) return -1;
                        return b.getSentDate().compareTo(a.getSentDate());
                    });
                    
                    for (NotificationHistory nh : allNotifications) {
                        String timeStr = "??/??";
                        try {
                            if (nh.getSentDate() != null) {
                                timeStr = fmt.format(nh.getSentDate());
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur de formatage de date: " + e.getMessage());
                        }
                        
                        String prefix = nh.isRead() ? "" : "🔵 ";  // Ajouter un indicateur pour les non lues
                        
                        MenuItem mi = new MenuItem(prefix + timeStr + " - " + nh.getDetails());
                        
                        // Style spécial pour les non lues
                        if (!nh.isRead()) {
                            mi.setStyle("-fx-font-weight: bold;");
                        }
                        
                        final NotificationHistory finalNh = nh;
                        mi.setOnAction(ev -> {
                            try {
                                // Marquer comme lu si nécessaire
                                if (!finalNh.isRead()) {
                                    notificationService.markNotificationAsRead(finalNh.getId());
                                }
                                
                                // Mettre à jour badge et menu
                                updateNotificationBadge();
                                
                                // Afficher le détail
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Notification");
                                alert.setHeaderText(finalNh.getNotificationType());
                                alert.setContentText(finalNh.getDetails());
                                alert.showAndWait();
                            } catch (Exception e) {
                                System.err.println("Erreur lors du traitement de la notification: " + e.getMessage());
                            }
                        });
                        
                        notificationMenuButton.getItems().add(mi);
                    }
                }
            } else {
                // Utilisateur non connecté : indiquer la nécessité de se connecter
                notificationBadge.setVisible(false);
                MenuItem loginRequired = new MenuItem("Veuillez vous connecter");
                loginRequired.setDisable(true);
                notificationMenuButton.getItems().add(loginRequired);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du badge de notification: " + e.getMessage());
            e.printStackTrace();
            
            // Assurer un état cohérent en cas d'erreur
            notificationBadge.setVisible(false);
            notificationMenuButton.getItems().clear();
            MenuItem errorItem = new MenuItem("Aucune notification");
            errorItem.setDisable(true);
            notificationMenuButton.getItems().add(errorItem);
        }
    }

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
            Node currentNode = notificationBadge.getScene().getRoot();
            BorderPane mainContainer = null;
            
            // Chercher le BorderPane dans la hiérarchie des parents
            if (currentNode instanceof BorderPane) {
                mainContainer = (BorderPane) currentNode;
            } else {
                // Recherche dans les parents
                while (currentNode.getParent() != null) {
                    currentNode = currentNode.getParent();
                    if (currentNode instanceof BorderPane) {
                        mainContainer = (BorderPane) currentNode;
                        break;
                    }
                }
            }
            
            if (mainContainer == null) {
                System.err.println("Impossible de trouver le BorderPane parent");
                return;
            }
            
            // Charger la nouvelle page
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            mainContainer.setCenter(page);
            
            // Si c'est la page de notifications, mettre à jour le badge
            if (fxmlPath.contains("Notification")) {
                updateNotificationBadge();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNotificationClick(MouseEvent event) {
        try {
            // Update the badge first
            updateNotificationBadge();
            
            // Ensure the menu button shows even if there's a problem with automatic showing
            if (!notificationMenuButton.isShowing()) {
                notificationMenuButton.show();
            }
            
            // Prevent event propagation that might interfere with menu showing
            event.consume();
        } catch (Exception e) {
            System.err.println("Erreur lors du clic sur les notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 