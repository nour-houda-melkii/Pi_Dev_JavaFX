package gui;

import entities.NotificationHistory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import services.EventService;
import services.NotificationService;
import services.UserSession;
import models.User;
import org.example.App;

import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationViewController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(NotificationViewController.class.getName());
    
    @FXML
    private TableView<NotificationHistory> notificationsTable;
    
    @FXML
    private TableColumn<NotificationHistory, String> typeColumn;
    
    @FXML
    private TableColumn<NotificationHistory, String> detailsColumn;
    
    @FXML
    private TableColumn<NotificationHistory, String> dateColumn;
    
    @FXML
    private TableColumn<NotificationHistory, String> statusColumn;
    
    @FXML
    private TableColumn<NotificationHistory, String> eventColumn;
    
    @FXML
    private Button markAsReadButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private ToggleButton filterToggle;
    
    @FXML
    private Label notificationCountLabel;
    
    @FXML
    private VBox detailsPanel;
    
    @FXML
    private Label detailsTitleLabel;
    
    @FXML
    private Label detailsDateLabel;
    
    @FXML
    private TextArea detailsContentArea;
    
    @FXML
    private Label noNotificationsLabel;
    
    private NotificationService notificationService;
    private EventService eventService;
    private ObservableList<NotificationHistory> notificationsList;
    private boolean showUnreadOnly = false;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationService = new NotificationService();
        eventService = new EventService();
        notificationsList = FXCollections.observableArrayList();
        
        // Configurer les colonnes de la table
        setupTableColumns();
        
        // Désactiver les boutons par défaut
        markAsReadButton.setDisable(true);
        deleteButton.setDisable(true);
        
        // Ajouter un écouteur de sélection
        notificationsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleNotificationSelection(newSelection));
        
        // Si le label n'existe pas encore, l'ajouter
        if (noNotificationsLabel == null) {
            noNotificationsLabel = new Label("Aucune notification à afficher");
            noNotificationsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            BorderPane parent = (BorderPane) notificationsTable.getParent();
            if (parent != null) {
                StackPane centerPane = new StackPane();
                centerPane.getChildren().addAll(notificationsTable, noNotificationsLabel);
                parent.setCenter(centerPane);
            }
        }
        
        // Charger les notifications
        loadNotifications();
    }
    
    private void setupTableColumns() {
        typeColumn.setCellValueFactory(cell -> {
            String type = cell.getValue().getNotificationType();
            String displayType = "";
            
            switch (type) {
                case NotificationHistory.TYPE_REMINDER:
                    displayType = "Rappel";
                    break;
                case NotificationHistory.TYPE_EVENT_CHANGE:
                    displayType = "Modification";
                    break;
                case NotificationHistory.TYPE_REGISTRATION:
                    displayType = "Inscription";
                    break;
                case NotificationHistory.TYPE_CANCELLATION:
                    displayType = "Annulation";
                    break;
                default:
                    displayType = type;
            }
            
            return new SimpleStringProperty(displayType);
        });
        
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        
        dateColumn.setCellValueFactory(cell -> {
            Timestamp date = cell.getValue().getSentDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return new SimpleStringProperty(sdf.format(date));
        });
        
        statusColumn.setCellValueFactory(cell -> {
            boolean isRead = cell.getValue().isRead();
            return new SimpleStringProperty(isRead ? "Lue" : "Non lue");
        });
        
        eventColumn.setCellValueFactory(cell -> {
            int eventId = cell.getValue().getEventId();
            try {
                String eventTitle = eventService.getEventTitle(eventId);
                return new SimpleStringProperty(eventTitle != null ? eventTitle : "Événement #" + eventId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Impossible de récupérer le titre de l'événement #" + eventId, e);
                return new SimpleStringProperty("Événement #" + eventId);
            }
        });
    }
    
    private void loadNotifications() {
        // Pour tester, afficher l'ID utilisateur dans la console
        System.out.println("Chargement des notifications...");
        
        // Récupérer l'ID utilisateur (par défaut 1)
        int userId = 1;
        
        try {
            if (UserSession.getInstance().isLoggedIn()) {
                User user = UserSession.getInstance().getLoggedInUser();
                userId = user.getId();
                System.out.println("Utilisateur connecté: ID=" + userId + ", Email=" + user.getEmail());
            } else {
                System.out.println("Aucun utilisateur connecté, utilisation de l'ID par défaut: " + userId);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
        
        List<NotificationHistory> notifications;
        if (showUnreadOnly) {
            notifications = notificationService.getUnreadNotificationsForUser(userId);
        } else {
            notifications = notificationService.getNotificationsForUser(userId);
        }
        
        System.out.println("Nombre de notifications récupérées: " + notifications.size());
        
        notificationsList.clear();
        notificationsList.addAll(notifications);
        notificationsTable.setItems(notificationsList);
        
        // Afficher un message si aucune notification n'est trouvée
        if (noNotificationsLabel != null) {
            noNotificationsLabel.setVisible(notifications.isEmpty());
        }
        
        // Mettre à jour le compteur de notifications
        updateNotificationCount();
    }
    
    private void updateNotificationCount() {
        int totalCount = notificationsList.size();
        long unreadCount = notificationsList.stream()
                .filter(n -> !n.isRead())
                .count();
        
        if (showUnreadOnly) {
            notificationCountLabel.setText("Affichage de " + totalCount + " notification(s) non lue(s)");
        } else {
            notificationCountLabel.setText(totalCount + " notification(s) dont " + unreadCount + " non lue(s)");
        }
    }
    
    private void handleNotificationSelection(NotificationHistory notification) {
        boolean hasSelection = notification != null;
        markAsReadButton.setDisable(!hasSelection || notification.isRead());
        deleteButton.setDisable(!hasSelection);
        
        if (hasSelection) {
            displayNotificationDetails(notification);
        } else {
            hideNotificationDetails();
        }
    }
    
    private void displayNotificationDetails(NotificationHistory notification) {
        detailsPanel.setVisible(true);
        
        String typeLabel = "";
        switch (notification.getNotificationType()) {
            case NotificationHistory.TYPE_REMINDER:
                typeLabel = "Rappel";
                break;
            case NotificationHistory.TYPE_EVENT_CHANGE:
                typeLabel = "Modification d'événement";
                break;
            case NotificationHistory.TYPE_REGISTRATION:
                typeLabel = "Confirmation d'inscription";
                break;
            case NotificationHistory.TYPE_CANCELLATION:
                typeLabel = "Annulation";
                break;
            default:
                typeLabel = notification.getNotificationType();
        }
        
        detailsTitleLabel.setText(typeLabel);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy à HH:mm");
        detailsDateLabel.setText("Reçu le " + sdf.format(notification.getSentDate()));
        
        detailsContentArea.setText(notification.getDetails());
        
        // Si la notification n'est pas lue, la marquer comme lue automatiquement
        if (!notification.isRead()) {
            notificationService.markNotificationAsRead(notification.getId());
            notification.setRead(true);
            notification.setReadDate(new Timestamp(System.currentTimeMillis()));
            notificationsTable.refresh();
            updateNotificationCount();
            
            // Mettre à jour le badge dans la barre de navigation
            try {
                // Récupérer le contrôleur de la barre de navigation et mettre à jour le badge
                BorderPane mainPane = (BorderPane) notificationsTable.getScene().getRoot();
                if (mainPane != null) {
                    controllers.FrontNavbarController navController = 
                            (controllers.FrontNavbarController) mainPane.getTop().getUserData();
                    if (navController != null) {
                        navController.updateNotificationBadge();
                    }
                }
            } catch (Exception e) {
                System.err.println("Impossible de mettre à jour le badge de notification: " + e.getMessage());
            }
        }
    }
    
    private void hideNotificationDetails() {
        detailsPanel.setVisible(false);
    }
    
    @FXML
    private void handleMarkAsRead(ActionEvent event) {
        NotificationHistory selectedNotification = notificationsTable.getSelectionModel().getSelectedItem();
        if (selectedNotification != null && !selectedNotification.isRead()) {
            boolean success = notificationService.markNotificationAsRead(selectedNotification.getId());
            
            if (success) {
                selectedNotification.setRead(true);
                selectedNotification.setReadDate(new Timestamp(System.currentTimeMillis()));
                notificationsTable.refresh();
                markAsReadButton.setDisable(true);
                updateNotificationCount();
            } else {
                showAlert("Erreur", "Impossible de marquer la notification comme lue.");
            }
        }
    }
    
    @FXML
    private void handleDelete(ActionEvent event) {
        NotificationHistory selectedNotification = notificationsTable.getSelectionModel().getSelectedItem();
        if (selectedNotification != null) {
            boolean confirm = showConfirmation("Suppression", 
                    "Êtes-vous sûr de vouloir supprimer cette notification ?");
            
            if (confirm) {
                boolean success = notificationService.deleteNotification(selectedNotification.getId());
                
                if (success) {
                    notificationsList.remove(selectedNotification);
                    hideNotificationDetails();
                    updateNotificationCount();
                    
                    // Afficher un message si la liste est vide
                    if (noNotificationsLabel != null) {
                        noNotificationsLabel.setVisible(notificationsList.isEmpty());
                    }
                } else {
                    showAlert("Erreur", "Impossible de supprimer la notification.");
                }
            }
        }
    }
    
    @FXML
    private void handleFilterToggle(ActionEvent event) {
        showUnreadOnly = filterToggle.isSelected();
        loadNotifications();
    }
    
    @FXML
    private void handleRefresh(ActionEvent event) {
        loadNotifications();
    }
    
    @FXML
    private void handleCreateTestNotification() {
        // Pour tester, afficher une boîte de dialogue pour saisir l'ID de l'événement
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Créer une notification de test");
        dialog.setHeaderText("Cette fonction crée une notification de test pour voir l'interface");
        dialog.setContentText("Entrez l'ID de l'événement:");

        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(eventIdStr -> {
            try {
                int eventId = Integer.parseInt(eventIdStr);
                int userId = 1; // ID par défaut
                
                if (UserSession.getInstance().isLoggedIn()) {
                    userId = UserSession.getInstance().getLoggedInUser().getId();
                }
                
                // Créer une notification de test
                App.getNotificationScheduler().createTestNotification(userId, eventId);
                
                // Rafraîchir la liste des notifications
                loadNotifications();
                
                showAlert("Notification créée", "Une notification de test a été créée avec succès.");
            } catch (NumberFormatException e) {
                showAlert("Erreur", "L'ID de l'événement doit être un nombre entier valide.");
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de créer la notification de test: " + e.getMessage());
            }
        });
    }
    
    @FXML
    private void handleRetour() {
        // Revenir à la page précédente (événements)
        try {
            BorderPane mainContainer = (BorderPane) notificationsTable.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_event_list.fxml"));
            Parent page = loader.load();
            mainContainer.setCenter(page);
        } catch (Exception e) {
            System.err.println("Erreur lors du retour à la liste des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
} 