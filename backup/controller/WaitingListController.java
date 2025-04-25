package controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.dao.EventDao;
import model.dto.WaitingListEntryDto;
import model.entities.Event;
import model.entities.User;
import model.services.MailService;
import model.services.WaitingListService;

/**
 * Contrôleur pour la gestion des listes d'attente des événements
 */
public class WaitingListController implements Initializable {
    
    @FXML
    private Label eventTitle;
    
    @FXML
    private Label eventDate;
    
    @FXML
    private Label eventLocation;
    
    @FXML
    private Label capacityInfo;
    
    @FXML
    private Label waitingListSizeLabel;
    
    @FXML
    private TableView<WaitingListEntryDto> waitingListTable;
    
    @FXML
    private TableColumn<WaitingListEntryDto, Integer> positionColumn;
    
    @FXML
    private TableColumn<WaitingListEntryDto, String> firstNameColumn;
    
    @FXML
    private TableColumn<WaitingListEntryDto, String> lastNameColumn;
    
    @FXML
    private TableColumn<WaitingListEntryDto, String> emailColumn;
    
    @FXML
    private TableColumn<WaitingListEntryDto, String> dateAddedColumn;
    
    @FXML
    private Button promoteButton;
    
    @FXML
    private Button removeButton;
    
    @FXML
    private Button backButton;
    
    private Event currentEvent;
    private ObservableList<WaitingListEntryDto> waitingListData;
    private final WaitingListService waitingListService = WaitingListService.getInstance();
    private final EventDao eventDao = EventDao.getInstance();
    private final MailService mailService = MailService.getInstance();
    
    /**
     * Initialise le contrôleur
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuration des colonnes du tableau
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        dateAddedColumn.setCellValueFactory(new PropertyValueFactory<>("dateAdded"));
        
        // Initialiser la liste observable
        waitingListData = FXCollections.observableArrayList();
        waitingListTable.setItems(waitingListData);
        
        // Configuration de l'état des boutons en fonction de la sélection
        waitingListTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                promoteButton.setDisable(!hasSelection);
                removeButton.setDisable(!hasSelection);
            }
        );
        
        // Désactiver les boutons au démarrage
        promoteButton.setDisable(true);
        removeButton.setDisable(true);
    }
    
    /**
     * Charge les données d'un événement et sa liste d'attente
     * @param event L'événement à afficher
     */
    public void loadEvent(Event event) {
        this.currentEvent = event;
        
        // Mise à jour des informations de l'événement
        eventTitle.setText(event.getTitle());
        eventDate.setText(event.getFormattedDate());
        eventLocation.setText(event.getLocation());
        
        // Mise à jour des informations de capacité
        int currentCapacity = event.getMaxCapacity();
        int registered = event.getRegisteredCount();
        capacityInfo.setText(String.format("Capacité: %d / %d", registered, currentCapacity));
        
        // Charger la liste d'attente
        refreshWaitingList();
    }
    
    /**
     * Rafraîchit la liste d'attente
     */
    private void refreshWaitingList() {
        waitingListData.clear();
        
        List<WaitingListEntryDto> entries = waitingListService.getWaitingListEntries(currentEvent);
        waitingListData.addAll(entries);
        
        // Mise à jour du label de taille de la liste d'attente
        int size = waitingListData.size();
        waitingListSizeLabel.setText(String.format("Liste d'attente: %d %s", 
                size, size > 1 ? "personnes" : "personne"));
        
        // Mise à jour de l'état des boutons
        boolean isEmpty = waitingListData.isEmpty();
        promoteButton.setDisable(isEmpty);
        removeButton.setDisable(isEmpty);
    }
    
    /**
     * Gère l'action de promotion d'un utilisateur de la liste d'attente
     */
    @FXML
    private void handlePromoteUser(ActionEvent event) {
        WaitingListEntryDto selectedEntry = waitingListTable.getSelectionModel().getSelectedItem();
        
        if (selectedEntry == null) {
            return;
        }
        
        // Vérifier s'il y a une place disponible
        if (currentEvent.getRegisteredCount() >= currentEvent.getMaxCapacity()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Capacité maximale atteinte");
            alert.setHeaderText("Impossible de promouvoir un participant");
            alert.setContentText("L'événement a atteint sa capacité maximale. Vous devez augmenter la capacité "
                    + "ou retirer un participant inscrit avant de pouvoir promouvoir quelqu'un de la liste d'attente.");
            alert.showAndWait();
            return;
        }
        
        // Demander confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la promotion");
        confirm.setHeaderText("Promouvoir un participant de la liste d'attente");
        confirm.setContentText(String.format("Êtes-vous sûr de vouloir promouvoir %s %s de la liste d'attente "
                + "vers la liste des participants ?", 
                selectedEntry.getFirstName(), selectedEntry.getLastName()));
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            // Récupérer l'utilisateur complet
            User user = selectedEntry.getUser();
            
            // Promouvoir l'utilisateur
            boolean success = waitingListService.promoteUserFromWaitingList(currentEvent, user);
            
            if (success) {
                // Envoyer notification
                if (mailService.isEnabled()) {
                    mailService.sendEventConfirmation(user, currentEvent);
                }
                
                // Rafraîchir la liste
                refreshWaitingList();
                
                // Mettre à jour les informations de capacité
                int currentCapacity = currentEvent.getMaxCapacity();
                int registered = currentEvent.getRegisteredCount();
                capacityInfo.setText(String.format("Capacité: %d / %d", registered, currentCapacity));
                
                // Afficher un message de succès
                Alert success_alert = new Alert(Alert.AlertType.INFORMATION);
                success_alert.setTitle("Promotion réussie");
                success_alert.setHeaderText("Participant promu avec succès");
                success_alert.setContentText(String.format("%s %s a été promu(e) de la liste d'attente "
                        + "vers la liste des participants.", 
                        selectedEntry.getFirstName(), selectedEntry.getLastName()));
                success_alert.showAndWait();
            } else {
                // Afficher un message d'erreur
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText("Impossible de promouvoir le participant");
                error.setContentText("Une erreur s'est produite lors de la promotion du participant. "
                        + "Veuillez réessayer ultérieurement.");
                error.showAndWait();
            }
        }
    }
    
    /**
     * Gère l'action de suppression d'un utilisateur de la liste d'attente
     */
    @FXML
    private void handleRemoveUser(ActionEvent event) {
        WaitingListEntryDto selectedEntry = waitingListTable.getSelectionModel().getSelectedItem();
        
        if (selectedEntry == null) {
            return;
        }
        
        // Demander confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Retirer un participant de la liste d'attente");
        confirm.setContentText(String.format("Êtes-vous sûr de vouloir retirer %s %s de la liste d'attente ?", 
                selectedEntry.getFirstName(), selectedEntry.getLastName()));
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            // Supprimer l'utilisateur de la liste d'attente
            User user = selectedEntry.getUser();
            waitingListService.removeFromWaitingList(currentEvent, user);
            
            // Rafraîchir la liste
            refreshWaitingList();
            
            // Afficher un message de succès
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Suppression réussie");
            success.setHeaderText("Participant retiré avec succès");
            success.setContentText(String.format("%s %s a été retiré(e) de la liste d'attente.", 
                    selectedEntry.getFirstName(), selectedEntry.getLastName()));
            success.showAndWait();
        }
    }
    
    /**
     * Gère l'action de notification de tous les utilisateurs en liste d'attente
     */
    @FXML
    private void handleNotifyAll(ActionEvent event) {
        if (waitingListData.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Liste vide");
            alert.setHeaderText("Aucun participant en liste d'attente");
            alert.setContentText("Il n'y a actuellement aucun participant dans la liste d'attente.");
            alert.showAndWait();
            return;
        }
        
        // Demander confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la notification");
        confirm.setHeaderText("Notifier tous les participants en liste d'attente");
        confirm.setContentText("Êtes-vous sûr de vouloir envoyer une notification à tous les participants "
                + "en liste d'attente pour les informer de leur position actuelle ?");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            if (!mailService.isEnabled()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Service de mail désactivé");
                alert.setHeaderText("Impossible d'envoyer des notifications");
                alert.setContentText("Le service d'envoi de mails est actuellement désactivé. "
                        + "Veuillez l'activer dans les paramètres avant de pouvoir envoyer des notifications.");
                alert.showAndWait();
                return;
            }
            
            // Envoyer les notifications
            int notified = 0;
            for (WaitingListEntryDto entry : waitingListData) {
                User user = entry.getUser();
                mailService.sendWaitingListConfirmation(user, currentEvent, entry.getPosition());
                notified++;
            }
            
            // Afficher un message de succès
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Notifications envoyées");
            success.setHeaderText("Notifications envoyées avec succès");
            success.setContentText(String.format("%d participant(s) ont été notifiés de leur position "
                    + "dans la liste d'attente.", notified));
            success.showAndWait();
        }
    }
    
    /**
     * Gère l'action de retour à la page précédente
     */
    @FXML
    private void handleBackAction(ActionEvent event) {
        // Code pour revenir à la page précédente
        // À implémenter selon l'architecture de navigation de l'application
    }
} 