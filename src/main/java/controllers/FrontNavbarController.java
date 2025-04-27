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
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import utils.SessionManager;
import java.util.ArrayList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.stage.Window;
import javafx.geometry.Side;
import java.util.logging.Logger;
import java.util.logging.Level;
import javafx.scene.control.ContextMenu;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.scene.effect.DropShadow;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;

public class FrontNavbarController implements Initializable {
    
    private static final Logger LOGGER = Logger.getLogger(FrontNavbarController.class.getName());
    
    @FXML
    private StackPane notificationBadge;
    
    @FXML
    private Label notificationCount;
    
    @FXML
    private MenuButton notificationMenuButton;
    
    private NotificationService notificationService;
    
    private Popup notificationPopup;
    private VBox notificationContainer;
    private ScrollPane notificationScrollPane;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationService = new NotificationService();
        
        updateNotificationBadge();
        
        initializeNotificationPopup();
        
        notificationBadge.setOnMouseClicked(event -> {
            event.consume();
            toggleNotificationPopup(event);
        });
        
        notificationMenuButton.setOnMouseClicked(event -> {
            event.consume();
            toggleNotificationPopup(event);
        });
        
        notificationMenuButton.setOnShowing(event -> {
            event.consume();
        });
    }
    
    /**
     * Initialise le popup personnalisé pour les notifications
     */
    private void initializeNotificationPopup() {
        notificationPopup = new Popup();
        notificationPopup.setAutoHide(true); // Se ferme en cliquant ailleurs
        notificationPopup.setAutoFix(true);
        notificationPopup.setHideOnEscape(true);
        
        // Créer le conteneur principal qui contiendra tous les éléments
        notificationContainer = new VBox();
        notificationContainer.setStyle(
            "-fx-padding: 0;" +
            "-fx-spacing: 0;"
        );
        
        // Créer le ScrollPane pour permettre le défilement
        notificationScrollPane = new ScrollPane(notificationContainer);
        notificationScrollPane.setStyle(
            "-fx-background-color: white;" + 
            "-fx-background: white;" +
            "-fx-border-color: #dddddd;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 0;" +
            "-fx-pref-width: 320px;" +
            "-fx-max-width: 320px;" +
            "-fx-max-height: 400px;"
        );
        notificationScrollPane.setFitToWidth(true);
        notificationScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        notificationScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Personnaliser la barre de défilement
        notificationScrollPane.getStyleClass().add("notification-scroll-pane");
        
        // Style de la barre de défilement - appliquer directement au nœud
        Platform.runLater(() -> {
            notificationScrollPane.lookup(".scroll-bar:vertical .thumb").setStyle(
                "-fx-background-color: #dedede; " +
                "-fx-background-radius: 5px; " +
                "-fx-padding: 2px;"
            );
            
            notificationScrollPane.lookup(".scroll-bar:vertical .thumb:hover").setStyle(
                "-fx-background-color: #bbbbbb;"
            );
            
            notificationScrollPane.lookup(".scroll-bar:vertical").setStyle(
                "-fx-background-color: #FFFFFF; " +  // Blanc opaque au lieu de transparent
                "-fx-padding: 0 2px 0 0;"
            );
            
            notificationScrollPane.lookup(".scroll-bar:vertical .track").setStyle(
                "-fx-background-color: #FFFFFF;"  // Blanc opaque au lieu de transparent
            );
            
            // Masquer les boutons de la barre de défilement
            notificationScrollPane.lookup(".scroll-bar .increment-button").setStyle("-fx-padding: 0; -fx-opacity: 0; -fx-background-color: white;");
            notificationScrollPane.lookup(".scroll-bar .decrement-button").setStyle("-fx-padding: 0; -fx-opacity: 0; -fx-background-color: white;");
        });
        
        // Ajouter un effet d'ombre
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.2));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        notificationScrollPane.setEffect(shadow);
        
        // Ajouter le ScrollPane au popup
        notificationPopup.getContent().add(notificationScrollPane);
    }
    
    /**
     * Bascule l'affichage du popup de notification
     */
    private void toggleNotificationPopup(MouseEvent event) {
        if (notificationPopup.isShowing()) {
            notificationPopup.hide();
        } else {
            // Mettre à jour et afficher les notifications
            updateNotificationsInPopup();
            
            // Calculer la position du popup - au-dessus de la navbar
            Bounds bounds = notificationBadge.localToScreen(notificationBadge.getBoundsInLocal());
            double xPos = bounds.getMinX() - 270; // Positionner à côté du badge
            double yPos = bounds.getMaxY() + 5;   // Juste sous le badge mais avec un espace
            
            // Afficher le popup
            notificationPopup.show(notificationBadge.getScene().getWindow(), xPos, yPos);
        }
    }
    
    private void updateNotificationsInPopup() {
        notificationContainer.getChildren().clear();
        
        try {
            boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
            User loggedInUser = null;
            
            if (!isLoggedIn) {
                try {
                    loggedInUser = SessionManager.getCurrentUser();
                    isLoggedIn = loggedInUser != null && loggedInUser.getEmail() != null && !loggedInUser.getEmail().isEmpty();
                    
                    if (isLoggedIn) {
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'accès à SessionManager: " + e.getMessage());
                }
            } else {
                loggedInUser = UserSession.getInstance().getLoggedInUser();
            }
            
            if (isLoggedIn && loggedInUser != null) {
                int userId = loggedInUser.getId();
                
                List<NotificationHistory> unreadNotifications = new ArrayList<>();
                List<NotificationHistory> allNotifications = new ArrayList<>();
                
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
                
                notificationCount.setText(String.valueOf(unreadCount));
                notificationBadge.setVisible(unreadCount > 0);
                
                Label headerLabel = new Label("Notifications récentes (" + allNotifications.size() + ")");
                headerLabel.setMaxWidth(Double.MAX_VALUE);
                headerLabel.getStyleClass().add("notification-header");
                headerLabel.setStyle(
                    "-fx-background-color: linear-gradient(to right, #4568dc, #b06ab3);" +
                    "-fx-padding: 10px 12px;" +  // Padding horizontal réduit
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 13px;" +      // Taille de police plus petite
                    "-fx-background-radius: 8px 8px 0 0;"
                );
                notificationContainer.getChildren().add(headerLabel);
                
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM HH:mm");
                
                if (allNotifications.isEmpty()) {
                    Label emptyLabel = new Label("Aucune notification");
                    emptyLabel.setMaxWidth(Double.MAX_VALUE);
                    emptyLabel.getStyleClass().add("notification-empty");
                    emptyLabel.setStyle(
                        "-fx-font-style: italic;" +
                        "-fx-text-fill: #888;" +
                        "-fx-padding: 15px;" +
                        "-fx-alignment: center;" +
                        "-fx-background-color: #f8f9fa;" +
                        "-fx-background-radius: 0 0 8px 8px;"
                    );
                    notificationContainer.getChildren().add(emptyLabel);
                } else {
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
                        
                        String title = nh.getNotificationType();
                        String content = nh.getDetails();
                        
                        String type = "";
                        
                        if (title != null && title.toUpperCase().contains("REMINDER")) {
                            type = "RAPPEL";
                        } else if (content != null && content.toUpperCase().contains("RAPPEL")) {
                            type = "RAPPEL";
                        } else if (title != null && title.toUpperCase().contains("REGISTRATION")) {
                            type = "INSCRIPTION";
                        } else if (content != null && content.contains("inscrit")) {
                            type = "INSCRIPTION";
                        } else {
                            type = "NOTIFICATION";
                        }
                        
                        VBox itemLayout = new VBox();
                        itemLayout.setSpacing(8);
                        
                        String baseStyle = 
                            "-fx-background-color: white;" +
                            "-fx-padding: 8px 12px;" +  // Padding réduit
                            "-fx-border-width: 0 0 1 0;" +
                            "-fx-border-color: #eaeaea;";
                        
                        if (!nh.isRead()) {
                            baseStyle += "-fx-background-color: #F0F8FF;";  // Fond bleu clair opaque au lieu de semi-transparent
                            if (type.equals("RAPPEL")) {
                                baseStyle += "-fx-border-left-color: #FFC107; -fx-border-width: 0 0 1 4; -fx-padding: 14px 20px 14px 16px;";
                            } else if (type.equals("INSCRIPTION")) {
                                baseStyle += "-fx-border-left-color: #1976D2; -fx-border-width: 0 0 1 4; -fx-padding: 14px 20px 14px 16px;";
                            } else {
                                baseStyle += "-fx-border-left-color: rgba(69,104,220,0.4); -fx-border-width: 0 0 1 3px;";
                            }
                        } else if (type.equals("RAPPEL")) {
                            baseStyle += "-fx-border-left-color: #FFC107; -fx-border-width: 0 0 1 4; -fx-padding: 14px 20px 14px 16px; -fx-background-color: #FFF8E1;";  // Fond jaune pâle opaque
                        } else if (type.equals("INSCRIPTION")) {
                            baseStyle += "-fx-border-left-color: #1976D2; -fx-border-width: 0 0 1 4; -fx-padding: 14px 20px 14px 16px; -fx-background-color: #E3F2FD;";  // Fond bleu pâle opaque
                        }
                        
                        itemLayout.setStyle(baseStyle);
                        
                        HBox header = new HBox();
                        header.setSpacing(10);
                        header.setAlignment(Pos.CENTER_LEFT);
                        
                        Label typeLabel = new Label(type);
                        typeLabel.getStyleClass().add("notification-type");
                        if (type.equals("RAPPEL")) {
                            typeLabel.setStyle(
                                "-fx-text-fill: #FF9800;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 12px;" +  // Taille de police plus petite
                                "-fx-background-color: rgba(255,152,0,0.1);" +
                                "-fx-padding: 2px 5px;" + // Padding horizontal réduit
                                "-fx-background-radius: 4px;"
                            );
                        } else if (type.equals("INSCRIPTION")) {
                            typeLabel.setStyle(
                                "-fx-text-fill: #1976D2;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 12px;" +  // Taille de police plus petite
                                "-fx-background-color: rgba(25,118,210,0.1);" +
                                "-fx-padding: 2px 5px;" + // Padding horizontal réduit
                                "-fx-background-radius: 4px;"
                            );
                        } else {
                            typeLabel.setStyle(
                                "-fx-text-fill: #1976D2;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 12px;" +  // Taille de police plus petite
                                "-fx-padding: 2px 5px 3px 0;" +
                                "-fx-background-radius: 4px;"
                            );
                        }
                        
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        
                        Label dateLabel = new Label(timeStr);
                        dateLabel.getStyleClass().add("notification-date");
                        dateLabel.setStyle(
                            "-fx-font-style: italic;" +
                            "-fx-text-fill: #666;" +
                            "-fx-font-size: 12px;" +
                            "-fx-padding: 2px 0 3px 0;"
                        );
                        
                        header.getChildren().addAll(typeLabel, spacer, dateLabel);
                        
                        String displayContent = content;
                        if (displayContent != null && displayContent.length() > 120) {
                            displayContent = displayContent.substring(0, 117) + "...";
                        }
                        
                        Label contentLabel = new Label(displayContent);
                        contentLabel.getStyleClass().add("notification-content");
                        contentLabel.setWrapText(true);
                        contentLabel.setMaxWidth(380);
                        contentLabel.setStyle(
                            "-fx-text-fill: #333;" +
                            "-fx-font-size: 12px;" +     // Taille de police plus petite
                            "-fx-padding: 5px 0 0 0;" +
                            "-fx-wrap-text: true;" +
                            "-fx-line-spacing: 2px;"     // Interligne plus petit
                        );
                        
                        itemLayout.getChildren().addAll(header, contentLabel);
                        
                        final NotificationHistory finalNh = nh;
                        itemLayout.setOnMouseClicked(event -> {
                            try {
                                if (!finalNh.isRead()) {
                                    notificationService.markNotificationAsRead(finalNh.getId());
                                }
                                
                                updateNotificationBadge();
                                
                                notificationPopup.hide();
                                
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Notification");
                                alert.setHeaderText(finalNh.getNotificationType());
                                alert.setContentText(finalNh.getDetails());
                                alert.showAndWait();
                            } catch (Exception e) {
                                System.err.println("Erreur lors du traitement de la notification: " + e.getMessage());
                            }
                        });
                        
                        // Ajouter un effet de survol
                        final String finalBaseStyle = baseStyle; // Créer une version finale du style
                        itemLayout.setOnMouseEntered(event -> {
                            String hoverStyle = finalBaseStyle + "-fx-background-color: rgba(69,104,220,0.08);";
                            itemLayout.setStyle(hoverStyle);
                            itemLayout.setCursor(javafx.scene.Cursor.HAND);
                        });
                        
                        itemLayout.setOnMouseExited(event -> {
                            itemLayout.setStyle(finalBaseStyle);
                        });
                        
                        notificationContainer.getChildren().add(itemLayout);
                    }
                    
                    if (unreadCount > 0) {
                        Label footerLabel = new Label("Tout marquer comme lu");
                        footerLabel.setMaxWidth(Double.MAX_VALUE);
                        footerLabel.getStyleClass().add("notification-footer");
                        footerLabel.setStyle(
                            "-fx-background-color: #f8f9fa;" +
                            "-fx-padding: 8px 10px;" +    // Padding réduit
                            "-fx-font-size: 12px;" +      // Taille de police plus petite
                            "-fx-text-fill: #4568dc;" +
                            "-fx-font-weight: bold;" +
                            "-fx-alignment: center;" +
                            "-fx-cursor: hand;" +
                            "-fx-background-radius: 0 0 8px 8px;"
                        );
                        
                        final List<NotificationHistory> finalUnreadNotifications = new ArrayList<>(unreadNotifications);
                        
                        footerLabel.setOnMouseClicked(event -> {
                            try {
                                for (NotificationHistory nh : finalUnreadNotifications) {
                                    notificationService.markNotificationAsRead(nh.getId());
                                }
                                updateNotificationBadge();
                                notificationPopup.hide();
                            } catch (Exception e) {
                                System.err.println("Erreur lors du marquage des notifications: " + e.getMessage());
                            }
                        });
                        
                        footerLabel.setOnMouseEntered(event -> {
                            footerLabel.setStyle(
                                "-fx-background-color: #edf2fd;" +
                                "-fx-padding: 8px 10px;" +    // Padding réduit
                                "-fx-font-size: 12px;" +      // Taille de police plus petite
                                "-fx-text-fill: #3052c7;" +
                                "-fx-font-weight: bold;" +
                                "-fx-alignment: center;" +
                                "-fx-cursor: hand;" +
                                "-fx-background-radius: 0 0 8px 8px;"
                            );
                        });
                        
                        footerLabel.setOnMouseExited(event -> {
                            footerLabel.setStyle(
                                "-fx-background-color: #f8f9fa;" +
                                "-fx-padding: 8px 10px;" +    // Padding réduit
                                "-fx-font-size: 12px;" +      // Taille de police plus petite
                                "-fx-text-fill: #4568dc;" +
                                "-fx-font-weight: bold;" +
                                "-fx-alignment: center;" +
                                "-fx-cursor: hand;" +
                                "-fx-background-radius: 0 0 8px 8px;"
                            );
                        });
                        
                        notificationContainer.getChildren().add(footerLabel);
                    } else {
                        if (!notificationContainer.getChildren().isEmpty()) {
                            Node lastItem = notificationContainer.getChildren().get(notificationContainer.getChildren().size() - 1);
                            if (lastItem instanceof VBox) {
                                String currentStyle = ((VBox) lastItem).getStyle();
                                ((VBox) lastItem).setStyle(currentStyle + "-fx-background-radius: 0 0 8px 8px; -fx-border-width: 0;");
                            }
                        }
                    }
                }
            } else {
                notificationBadge.setVisible(false);
                
                Label headerLabel = new Label("Notification");
                headerLabel.setMaxWidth(Double.MAX_VALUE);
                headerLabel.getStyleClass().add("notification-header");
                headerLabel.setStyle(
                    "-fx-background-color: linear-gradient(to right, rgba(69,104,220,0.2), rgba(176,106,179,0.2));" +
                    "-fx-padding: 14px 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #4568dc;" +
                    "-fx-font-size: 16px;" +
                    "-fx-background-radius: 12px 12px 0 0;"
                );
                notificationContainer.getChildren().add(headerLabel);
                
                Label loginRequired = new Label("Veuillez vous connecter pour voir vos notifications");
                loginRequired.setMaxWidth(Double.MAX_VALUE);
                loginRequired.getStyleClass().add("notification-empty");
                loginRequired.setStyle(
                    "-fx-font-style: italic;" +
                    "-fx-text-fill: #888;" +
                    "-fx-padding: 20px;" +
                    "-fx-alignment: center;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 0 0 12px 12px;"
                );
                notificationContainer.getChildren().add(loginRequired);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des notifications: " + e.getMessage());
            e.printStackTrace();
            
            Label headerLabel = new Label("Erreur");
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.getStyleClass().add("notification-header");
            headerLabel.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(69,104,220,0.2), rgba(176,106,179,0.2));" +
                "-fx-padding: 14px 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4568dc;" +
                "-fx-font-size: 16px;" +
                "-fx-background-radius: 12px 12px 0 0;"
            );
            notificationContainer.getChildren().add(headerLabel);
            
            Label errorLabel = new Label("Impossible de charger les notifications");
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            errorLabel.getStyleClass().add("notification-empty");
            errorLabel.setStyle(
                "-fx-font-style: italic;" +
                "-fx-text-fill: #888;" +
                "-fx-padding: 20px;" +
                "-fx-alignment: center;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 0 0 12px 12px;"
            );
            notificationContainer.getChildren().add(errorLabel);
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
     * Gère le clic sur le bouton Donate
     */
    @FXML
    private void handleDonate() {
        try {
            // Charger le fichier FXML du formulaire de don
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/donation_form.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur pour pouvoir lui passer la référence de la fenêtre
            DonationFormController controller = loader.getController();
            
            // Créer une nouvelle fenêtre modale
            Stage donationStage = new Stage();
            donationStage.initModality(Modality.APPLICATION_MODAL);
            donationStage.setTitle("Faire un don");
            
            // Créer la scène avec le formulaire
            Scene scene = new Scene(root);
            
            // Ajouter la feuille de style
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            // Configurer la fenêtre
            donationStage.setScene(scene);
            donationStage.setResizable(false);
            
            // Passer la référence de la fenêtre au contrôleur
            controller.setStage(donationStage);
            
            // Afficher la fenêtre
            donationStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du formulaire de don: " + e.getMessage());
            e.printStackTrace();
            
            // Afficher un message d'erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'afficher le formulaire de don. Veuillez réessayer plus tard.");
            alert.showAndWait();
        }
    }
    
    private void loadPage(String fxmlPath) {
        try {
            Node currentNode = notificationBadge.getScene().getRoot();
            BorderPane mainContainer = null;
            
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
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            mainContainer.setCenter(page);
            
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
            updateNotificationBadge();
            
            if (notificationMenuButton.isShowing()) {
                notificationMenuButton.hide();
            } else {
                toggleNotificationPopup(event);
            }
            
            event.consume();
        } catch (Exception e) {
            System.err.println("Erreur lors du clic sur les notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showNotifications(ActionEvent event) {
        try {
            if (!notificationMenuButton.isShowing()) {
                LOGGER.log(Level.INFO, "About to show notification menu");
                
                notificationMenuButton.show();
                
                Platform.runLater(() -> {
                    if (notificationMenuButton.getContextMenu() != null) {
                        Bounds buttonBounds = notificationMenuButton.localToScreen(notificationMenuButton.getBoundsInLocal());
                        double menuWidth = notificationMenuButton.getContextMenu().getWidth();
                        
                        Bounds badgeBounds = notificationBadge.localToScreen(notificationBadge.getBoundsInLocal());
                        notificationMenuButton.getContextMenu().setX(badgeBounds.getMinX());
                        notificationMenuButton.getContextMenu().setY(badgeBounds.getMaxY() + 5);
                        
                        notificationMenuButton.getContextMenu().getStyleClass().add("notification-context-menu");
                        
                        if (notificationMenuButton.getContextMenu().getItems().size() > 0) {
                            try {
                                CustomMenuItem firstItem = null;
                                if (notificationMenuButton.getContextMenu().getItems().get(0) instanceof CustomMenuItem) {
                                    firstItem = (CustomMenuItem) notificationMenuButton.getContextMenu().getItems().get(0);
                                    firstItem.getStyleClass().add("first-notification-item");
                                }
                                
                                int lastIndex = notificationMenuButton.getContextMenu().getItems().size() - 1;
                                if (lastIndex > 0 && notificationMenuButton.getContextMenu().getItems().get(lastIndex) instanceof CustomMenuItem) {
                                    CustomMenuItem lastItem = (CustomMenuItem) notificationMenuButton.getContextMenu().getItems().get(lastIndex);
                                    lastItem.getStyleClass().add("last-notification-item");
                                }
                            } catch (Exception e) {
                                System.err.println("Erreur lors de l'application des styles: " + e.getMessage());
                            }
                        }
                    }
                });
                
                LOGGER.log(Level.INFO, "Notification menu shown");
            } else {
                notificationMenuButton.hide();
                LOGGER.log(Level.INFO, "Notification menu hidden");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la gestion de la mise à jour des notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNotificationBadgeClick(MouseEvent event) {
        if (notificationMenuButton.isShowing()) {
            notificationMenuButton.hide();
        } else {
            toggleNotificationPopup(event);
        }
    }

    /**
     * Met à jour le badge de notification avec le nombre de notifications non lues
     */
    public void updateNotificationBadge() {
        try {
            // Vérifier si un utilisateur est connecté
            boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
            User loggedInUser = null;
            
            // Si UserSession n'a pas d'utilisateur, essayer avec SessionManager
            if (!isLoggedIn) {
                try {
                    loggedInUser = SessionManager.getCurrentUser();
                    isLoggedIn = loggedInUser != null && loggedInUser.getEmail() != null && !loggedInUser.getEmail().isEmpty();
                    
                    if (isLoggedIn) {
                        UserSession.getInstance().setLoggedInUser(loggedInUser);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'accès à SessionManager: " + e.getMessage());
                }
            } else {
                loggedInUser = UserSession.getInstance().getLoggedInUser();
            }
            
            if (isLoggedIn && loggedInUser != null) {
                int userId = loggedInUser.getId();
                
                // Récupérer les notifications non lues
                List<NotificationHistory> unreadNotifications = new ArrayList<>();
                
                try {
                    unreadNotifications = notificationService.getUnreadNotificationsForUser(userId);
                } catch (Exception e) {
                    System.err.println("Impossible de récupérer les notifications non lues: " + e.getMessage());
                }
                
                int unreadCount = unreadNotifications.size();
                
                // Mettre à jour le badge
                notificationCount.setText(String.valueOf(unreadCount));
                notificationBadge.setVisible(unreadCount > 0);
            } else {
                // Utilisateur non connecté
                notificationBadge.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du badge de notification: " + e.getMessage());
            e.printStackTrace();
            
            // Assurer un état cohérent en cas d'erreur
            notificationBadge.setVisible(false);
        }
    }
} 