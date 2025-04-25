package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import entities.NotificationHistory;
import services.NotificationService;
import services.UserSession;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;

public class FrontNavbarController implements Initializable {
    
    @FXML
    private StackPane notificationBadge;
    
    @FXML
    private Label notificationCount;
    
    @FXML
    private Button notificationButton;
    
    @FXML
    private VBox notificationDropdown;
    
    @FXML
    private VBox notificationList;
    
    private NotificationService notificationService;
    private boolean badgeVisible = false;
    private boolean dropdownVisible = false;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationService = new NotificationService();
        
        // S'assurer que les champs @FXML ont été injectés
        if (notificationButton == null || notificationDropdown == null || notificationList == null) {
            System.err.println("ATTENTION: Certains éléments de l'interface n'ont pas été injectés correctement!");
            System.err.println("notificationButton: " + (notificationButton != null));
            System.err.println("notificationDropdown: " + (notificationDropdown != null));
            System.err.println("notificationList: " + (notificationList != null));
            
            // Éviter les NullPointerException plus tard
            return;
        }
        
        // Initialiser le dropdown - caché par défaut
        notificationDropdown.setVisible(false);
        notificationDropdown.setManaged(false);
        
        // Charger les notifications et mettre à jour le badge
        updateNotificationBadge();
        
        // Ajouter un gestionnaire pour l'événement quand la scène est prête
        notificationButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Maintenant que la scène est chargée, on peut ajouter l'écouteur d'événement
                System.out.println("Scène initialisée, prêt pour les interactions avec le menu de notifications");
            }
        });
    }
    
    /**
     * Affiche ou masque le menu déroulant des notifications
     */
    @FXML
    private void toggleNotificationMenu() {
        try {
            if (dropdownVisible) {
                hideNotificationDropdown();
            } else {
                System.out.println("Ouverture du menu des notifications...");
                showNotificationDropdown();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la bascule du menu des notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Affiche le menu déroulant des notifications
     */
    private void showNotificationDropdown() {
        try {
            // Charger les notifications dans le dropdown
            loadNotificationsIntoDropdown();
            
            // Afficher le dropdown
            notificationDropdown.setVisible(true);
            notificationDropdown.setManaged(true);
            dropdownVisible = true;
            
            // Ajouter un gestionnaire de clic sur la scène pour fermer le dropdown
            if (notificationButton.getScene() != null) {
                notificationButton.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, closeDropdownHandler);
                System.out.println("Gestionnaire d'événements de fermeture ajouté");
            } else {
                System.err.println("Impossible d'ajouter le gestionnaire d'événements - Scene est null");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage du menu des notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gestionnaire d'événement pour fermer le dropdown en cliquant ailleurs
     */
    private EventHandler<MouseEvent> closeDropdownHandler = event -> {
        Node source = (Node) event.getTarget();
        
        // Vérifier si le clic est à l'extérieur du dropdown et du bouton
        boolean isOutsideDropdown = true;
        boolean isOutsideButton = true;
        
        Node current = source;
        while (current != null) {
            if (current == notificationDropdown) {
                isOutsideDropdown = false;
                break;
            }
            if (current == notificationButton) {
                isOutsideButton = false;
                break;
            }
            current = current.getParent();
        }
        
        if (isOutsideDropdown && isOutsideButton) {
            hideNotificationDropdown();
        }
    };
    
    /**
     * Masque le menu déroulant des notifications
     */
    private void hideNotificationDropdown() {
        notificationDropdown.setVisible(false);
        notificationDropdown.setManaged(false);
        dropdownVisible = false;
        
        // Supprimer le gestionnaire de clic
        notificationButton.getScene().removeEventFilter(MouseEvent.MOUSE_CLICKED, closeDropdownHandler);
    }
    
    /**
     * Charge les notifications dans le menu déroulant
     */
    private void loadNotificationsIntoDropdown() {
        notificationList.getChildren().clear();
        
        // Toujours utiliser l'ID 1 pour s'assurer de charger les notifications même si session n'est pas détectée
        int userId = 1;
        
        try {
            if (UserSession.getInstance().isLoggedIn()) {
                userId = UserSession.getInstance().getLoggedInUser().getId();
                System.out.println("Chargement des notifications pour l'utilisateur: " + userId);
            } else {
                System.out.println("Aucune session utilisateur détectée, utilisation de l'ID par défaut: " + userId);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération de la session: " + e.getMessage());
        }
        
        // Récupérer toutes les notifications (max 5)
        List<NotificationHistory> recentNotifications = notificationService.getNotificationsForUser(userId);
        System.out.println("Nombre de notifications récupérées pour le dropdown: " + recentNotifications.size());
        
        if (recentNotifications.isEmpty()) {
            showNoNotificationsMessage();
            return;
        }
        
        // Limiter à 5 notifications pour le dropdown
        int count = Math.min(recentNotifications.size(), 5);
        
        // Ajouter les notifications au menu
        for (int i = 0; i < count; i++) {
            NotificationHistory notification = recentNotifications.get(i);
            HBox notifItem = createNotificationItem(notification);
            notificationList.getChildren().add(notifItem);
        }
    }
    
    /**
     * Affiche un message quand il n'y a pas de notifications
     */
    private void showNoNotificationsMessage() {
        VBox emptyMessage = new VBox();
        emptyMessage.getStyleClass().add("no-notifications");
        emptyMessage.setAlignment(Pos.CENTER);
        
        Label messageLabel = new Label("Aucune notification");
        messageLabel.getStyleClass().add("no-notifications-text");
        
        emptyMessage.getChildren().add(messageLabel);
        notificationList.getChildren().add(emptyMessage);
    }
    
    /**
     * Crée un élément de notification pour le menu déroulant
     */
    private HBox createNotificationItem(NotificationHistory notification) {
        HBox item = new HBox();
        item.setSpacing(10);
        item.setPrefWidth(330);
        item.getStyleClass().add("notification-item");
        
        // Ajouter la classe "unread" si nécessaire
        if (!notification.isRead()) {
            item.getStyleClass().add("unread");
        }
        
        // Contenu de la notification (VBox à gauche)
        VBox content = new VBox();
        content.getStyleClass().add("notification-item-content");
        content.setSpacing(3);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        // Titre selon le type de notification
        String typeLabel = getNotificationTypeLabel(notification.getNotificationType());
        Label titleLabel = new Label(typeLabel);
        titleLabel.getStyleClass().add("notification-item-title");
        content.getChildren().add(titleLabel);
        
        // Détails de la notification
        Label detailsLabel = new Label(notification.getDetails());
        detailsLabel.getStyleClass().add("notification-item-details");
        detailsLabel.setWrapText(true);
        content.getChildren().add(detailsLabel);
        
        // Date de la notification
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Label dateLabel = new Label(sdf.format(notification.getSentDate()));
        dateLabel.getStyleClass().add("notification-item-date");
        content.getChildren().add(dateLabel);
        
        // Bouton pour marquer comme lu (à droite)
        Button markReadBtn = new Button("×");
        markReadBtn.getStyleClass().add("notification-mark-read");
        
        // Ne pas afficher le bouton si déjà lu
        if (notification.isRead()) {
            markReadBtn.setVisible(false);
            markReadBtn.setManaged(false);
        } else {
            // Gérer le clic sur le bouton
            markReadBtn.setOnAction(e -> {
                markNotificationAsRead(notification);
                e.consume(); // Empêcher la propagation
            });
        }
        
        // Gestion du clic sur l'élément
        item.setOnMouseClicked(e -> {
            // Si on clique sur l'élément (pas sur le bouton)
            if (e.getTarget() != markReadBtn) {
                viewNotificationDetails(notification);
            }
        });
        
        // Assembler l'élément
        item.getChildren().addAll(content, markReadBtn);
        
        return item;
    }
    
    /**
     * Renvoie le libellé selon le type de notification
     */
    private String getNotificationTypeLabel(String type) {
        switch (type) {
            case NotificationHistory.TYPE_REMINDER:
                return "Rappel";
            case NotificationHistory.TYPE_EVENT_CHANGE:
                return "Modification d'événement";
            case NotificationHistory.TYPE_REGISTRATION:
                return "Confirmation d'inscription";
            case NotificationHistory.TYPE_CANCELLATION:
                return "Annulation";
            default:
                return type;
        }
    }
    
    /**
     * Marque une notification comme lue
     */
    private void markNotificationAsRead(NotificationHistory notification) {
        if (!notification.isRead()) {
            boolean success = notificationService.markNotificationAsRead(notification.getId());
            
            if (success) {
                notification.setRead(true);
                
                // Mettre à jour l'interface
                updateNotificationBadge();
                loadNotificationsIntoDropdown();
            }
        }
    }
    
    /**
     * Marque toutes les notifications comme lues
     */
    @FXML
    private void markAllAsRead() {
        if (UserSession.getInstance().isLoggedIn()) {
            int userId = UserSession.getInstance().getLoggedInUser().getId();
            List<NotificationHistory> unreadNotifications = notificationService.getUnreadNotificationsForUser(userId);
            
            for (NotificationHistory notification : unreadNotifications) {
                notificationService.markNotificationAsRead(notification.getId());
            }
            
            // Mettre à jour l'interface
            updateNotificationBadge();
            loadNotificationsIntoDropdown();
        }
    }
    
    /**
     * Affiche les détails d'une notification
     */
    private void viewNotificationDetails(NotificationHistory notification) {
        // Marquer comme lue si nécessaire
        if (!notification.isRead()) {
            markNotificationAsRead(notification);
        }
        
        // Aller à la page des notifications
        hideNotificationDropdown();
        loadPage("/gui/NotificationView.fxml");
    }
    
    /**
     * Met à jour le badge de notification avec le nombre de notifications non lues
     */
    public void updateNotificationBadge() {
        try {
            // Vérifier si un utilisateur est connecté
            if (UserSession.getInstance().isLoggedIn()) {
                int userId = UserSession.getInstance().getLoggedInUser().getId();
                
                // Récupérer les notifications non lues
                List<NotificationHistory> unreadNotifications = notificationService.getUnreadNotificationsForUser(userId);
                int count = unreadNotifications.size();
                
                // Mettre à jour le badge
                if (count > 0) {
                    notificationCount.setText(count > 99 ? "99+" : String.valueOf(count));
                    
                    // Si le badge n'était pas visible avant, l'animer
                    if (!badgeVisible) {
                        notificationBadge.setScaleX(0);
                        notificationBadge.setScaleY(0);
                        notificationBadge.setVisible(true);
                        
                        ScaleTransition st = new ScaleTransition(Duration.millis(300), notificationBadge);
                        st.setFromX(0);
                        st.setFromY(0);
                        st.setToX(1);
                        st.setToY(1);
                        st.play();
                        
                        badgeVisible = true;
                    }
                } else {
                    if (badgeVisible) {
                        // Animation de disparition
                        FadeTransition ft = new FadeTransition(Duration.millis(300), notificationBadge);
                        ft.setFromValue(1.0);
                        ft.setToValue(0.0);
                        ft.setOnFinished(e -> notificationBadge.setVisible(false));
                        ft.play();
                        
                        badgeVisible = false;
                    } else {
                        notificationBadge.setVisible(false);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du badge de notification: " + e.getMessage());
        }
    }

    @FXML
    private void handleHome() {
        if (dropdownVisible) {
            hideNotificationDropdown();
        }
        loadPage("/views/front_home.fxml");
    }

    @FXML
    private void handleEvents() {
        if (dropdownVisible) {
            hideNotificationDropdown();
        }
        loadPage("/views/front_event_list.fxml");
    }

    @FXML
    private void handleAbout() {
        if (dropdownVisible) {
            hideNotificationDropdown();
        }
        loadPage("/views/front_about.fxml");
    }

    @FXML
    private void handleContact() {
        if (dropdownVisible) {
            hideNotificationDropdown();
        }
        loadPage("/views/front_contact.fxml");
    }
    
    @FXML
    private void handleNotifications() {
        if (dropdownVisible) {
            hideNotificationDropdown();
        }
        loadPage("/gui/NotificationView.fxml");
    }
    
    /**
     * Charge une page dans le conteneur principal
     */
    private void loadPage(String fxmlPath) {
        try {
            // Trouver le BorderPane parent
            Node currentNode = notificationButton.getScene().getRoot();
            BorderPane mainContainer = null;
            
            // Chercher le BorderPane dans la hiérarchie
            if (currentNode instanceof BorderPane) {
                mainContainer = (BorderPane) currentNode;
            } else {
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
            
            // Stocker le contrôleur dans les données utilisateur
            if (mainContainer.getTop() != null) {
                mainContainer.getTop().setUserData(this);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la page: " + fxmlPath);
            e.printStackTrace();
        }
    }
} 