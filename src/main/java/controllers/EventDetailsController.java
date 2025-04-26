package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import java.time.LocalDateTime;

import models.Event;
import models.Inscription;
import models.User;
import services.InscriptionService;
import services.UserService;
import utils.SessionManager;
import services.EventService;
import services.NotificationService;
import services.EventArchiverService;

public class EventDetailsController {
    @FXML
    private Label titleLabel;
    
    @FXML
    private Label descriptionLabel;
    
    @FXML
    private Label dateLabel;
    
    @FXML
    private Label locationLabel;
    
    @FXML
    private Label placesLabel;
    
    @FXML
    private Label categoryLabel;
    
    @FXML
    private ImageView eventImage;
    
    @FXML
    private WebView mapView;
    
    @FXML
    private Button inscriptionButton;
    
    @FXML
    private Button desinscriptionButton;
    
    @FXML
    private Button backButton;
    
    private WebEngine webEngine;
    private Event event;
    private Stage previousStage;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private InscriptionService inscriptionService;
    private UserService userService;
    private User currentUser;
    private EventService eventService;
    
    @FXML
    public void initialize() {
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        userService = new UserService();
        eventService = new EventService();
        inscriptionService = new InscriptionService();
        
        // Initialisation des styles pour les boutons
        backButton.getStyleClass().clear();
        backButton.getStyleClass().add("back-button");
        
        inscriptionButton.getStyleClass().clear();
        inscriptionButton.getStyleClass().addAll("details-button", "button-active");
        
        desinscriptionButton.getStyleClass().clear();
        desinscriptionButton.getStyleClass().addAll("details-button", "danger-button");
        
        // Initialisation de la WebView pour la carte
        webEngine = mapView.getEngine();
        
        System.out.println("Initialisation du contrôleur EventDetailsController");
        
        // Récupérer l'utilisateur depuis le SessionManager
        currentUser = utils.SessionManager.getCurrentUser();
        System.out.println("EventDetailsController initialisé avec utilisateur: " + 
            (currentUser != null ? currentUser.getEmail() + " (rôle: " + currentUser.getRole() + ")" : "null"));
        
        // Par défaut, on rend les boutons visibles mais désactivés
        // Ils seront activés quand un utilisateur sera défini
        inscriptionButton.setVisible(true);
        inscriptionButton.setDisable(currentUser == null); // Activé seulement si utilisateur connecté
        desinscriptionButton.setVisible(false);
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        
        System.out.println("setUser appelé avec utilisateur: " + 
            (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
        
        // Activer les boutons si l'utilisateur est défini
        if (currentUser != null) {
            inscriptionButton.setDisable(false);
            desinscriptionButton.setDisable(false);
            
            System.out.println("Boutons activés pour l'utilisateur");
            
            // Mettre à jour l'état des boutons d'inscription
            if (event != null) {
                updateInscriptionButtons();
            }
        } else {
            System.out.println("Aucun utilisateur défini, boutons désactivés");
        }
    }
    
    public void setEvent(Event event) {
        this.event = event;
        
        // Charger les détails d'abord
        displayEventDetails();
        
        // Forcer une vérification immédiate de l'état des boutons
        System.out.println("Vérification du statut de l'événement...");
        // Un événement est considéré comme expiré s'il est archivé ou si sa date est passée
        boolean isExpired = event.isArchived() || event.getEndDate().isBefore(LocalDateTime.now());
        System.out.println("L'événement est-il expiré: " + isExpired);
        
        // Mettre à jour les boutons d'inscription selon l'état de l'événement
        if (currentUser != null) {
            updateInscriptionButtons();
        } else {
            // Même sans utilisateur, mettre à jour l'état des boutons pour les événements expirés
            if (isExpired) {
                inscriptionButton.setText("Terminé");
                inscriptionButton.setDisable(true);
                inscriptionButton.getStyleClass().removeAll("button-active");
                inscriptionButton.getStyleClass().add("button-expired");
            } else if (event.getPlacesDisponibles() <= 0) {
                inscriptionButton.setText("Complet");
                inscriptionButton.setDisable(true);
                inscriptionButton.getStyleClass().removeAll("button-active");
                inscriptionButton.getStyleClass().add("button-expired");
            }
        }
    }
    
    public void setPreviousStage(Stage previousStage) {
        this.previousStage = previousStage;
    }
    
    private void displayEventDetails() {
        if (event == null) return;
        
        // Remplir les informations de l'événement
        titleLabel.setText(event.getTitle());
        descriptionLabel.setText(event.getDescription());
        
        String dateText = String.format("Du %s au %s", 
            event.getStartDate().format(dateFormatter), 
            event.getEndDate().format(dateFormatter));
        dateLabel.setText(dateText);
        
        locationLabel.setText(event.getLocation());
        placesLabel.setText("Places disponibles: " + event.getPlacesDisponibles());
        
        if (event.getCategorie() != null) {
            categoryLabel.setText(event.getCategorie().getNom());
        } else {
            categoryLabel.setText("Catégorie non spécifiée");
        }
        
        // Charger l'image
        try {
            String imagePath = "/affiches/" + event.getAffiche();
            Image image = null;
            
            try {
                if (event.getAffiche() != null && !event.getAffiche().isEmpty()) {
                    image = new Image(getClass().getResourceAsStream(imagePath));
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image depuis resources: " + e.getMessage());
            }
            
            if (image == null || image.isError()) {
                try {
                    String directPath = "src/main/resources/affiches/" + event.getAffiche();
                    java.io.File file = new java.io.File(directPath);
                    if (file.exists()) {
                        image = new Image(file.toURI().toString(), true);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image depuis le système de fichiers: " + e.getMessage());
                }
            }
            
            if (image == null || image.isError()) {
                image = new Image(getClass().getResourceAsStream("/images/default-event.jpg"));
            }
            
            eventImage.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
        }
        
        // Charger la carte avec le marqueur de l'événement
        initializeMap();
    }
    
    private void initializeMap() {
        if (event == null || webEngine == null) return;
        
        double latitude = event.getLatitude();
        double longitude = event.getLongitude();
        
        if (latitude == 0 && longitude == 0) {
            // Coordonnées par défaut si non disponibles
            latitude = 34.0;
            longitude = 9.0;
        }
        
        // Créer le HTML pour la carte
        String mapHTML = createMapHTML(latitude, longitude, event.getTitle(), event.getLocation());
        webEngine.loadContent(mapHTML);
    }
    
    private String createMapHTML(double latitude, double longitude, String title, String location) {
        String markerColor = event.getEndDate().isBefore(java.time.LocalDateTime.now()) ? "red" : "green";
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Localisation de l'événement</title>\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        html, body, #map {\n" +
                "            height: 100%;\n" +
                "            width: 100%;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([" + latitude + ", " + longitude + "], 15);\n" +
                "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'\n" +
                "        }).addTo(map);\n" +
                "\n" +
                "        var iconUrl = '" + markerColor + "' === 'green' ? \n" +
                "            'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png' : \n" +
                "            'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png';\n" +
                "\n" +
                "        var customIcon = new L.Icon({\n" +
                "            iconUrl: iconUrl,\n" +
                "            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',\n" +
                "            iconSize: [25, 41],\n" +
                "            iconAnchor: [12, 41],\n" +
                "            popupAnchor: [1, -34],\n" +
                "            shadowSize: [41, 41]\n" +
                "        });\n" +
                "\n" +
                "        L.marker([" + latitude + ", " + longitude + "], {icon: customIcon})\n" +
                "            .addTo(map)\n" +
                "            .bindPopup('<b>" + title.replace("'", "\\'") + "</b><br>" + location.replace("'", "\\'") + "')\n" +
                "            .openPopup();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
    
    private void updateInscriptionButtons() {
        if (currentUser == null || event == null) {
            System.out.println("updateInscriptionButtons: Utilisateur ou événement null");
            return;
        }
        
        System.out.println("Mise à jour des boutons d'inscription pour l'événement: " + event.getTitle());
        System.out.println("Places disponibles: " + event.getPlacesDisponibles());
        System.out.println("Date de fin: " + event.getEndDate());
        System.out.println("Date actuelle: " + LocalDateTime.now());
        System.out.println("Événement archivé: " + event.isArchived());
        
        boolean isInscrit = false;
        
        try {
            isInscrit = inscriptionService.isUserRegisteredForEvent(currentUser.getId(), event.getId());
            System.out.println("L'utilisateur est-il inscrit? " + isInscrit);
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de l'inscription: " + e.getMessage());
            // Continuer avec isInscrit = false
        }
        
        // Un événement est considéré comme expiré s'il est archivé ou si sa date est passée
        boolean isEventExpired = event.isArchived() || event.getEndDate().isBefore(LocalDateTime.now());
        System.out.println("L'événement est-il expiré? " + isEventExpired);
        
        boolean hasFreeSpots = event.getPlacesDisponibles() > 0;
        System.out.println("Y a-t-il des places disponibles? " + hasFreeSpots);
        
        // Pour les administrateurs, activer toujours les boutons (ils peuvent gérer tous les événements)
        boolean isAdmin = (currentUser != null && currentUser.getRole() != null && 
                          (currentUser.getRole().contains("ADMIN") || currentUser.getRole().equalsIgnoreCase("admin")));
        
        if (isAdmin) {
            System.out.println("L'utilisateur est admin, activation des boutons");
            inscriptionButton.setDisable(false);
            desinscriptionButton.setDisable(false);
            
            // Appliquer les classes CSS appropriées
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-active");
            
            desinscriptionButton.getStyleClass().clear();
            desinscriptionButton.getStyleClass().addAll("details-button", "danger-button");
            
            // Afficher le bouton approprié selon l'état d'inscription
            inscriptionButton.setVisible(!isInscrit);
            desinscriptionButton.setVisible(isInscrit);
            
            // Mettre le texte standard
            inscriptionButton.setText("S'inscrire");
            return;
        }

        // 1. Si l'événement est expiré
        if (isEventExpired) {
            // Afficher "Terminé" et désactiver le bouton
            inscriptionButton.setText("Terminé");
            inscriptionButton.setDisable(true);
            inscriptionButton.setVisible(true);
            
            // Réinitialiser les classes et ajouter celles nécessaires
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-expired");
            
            // Cacher le bouton de désinscription
            desinscriptionButton.setVisible(false);
        }
        // 2. Si l'utilisateur est déjà inscrit
        else if (isInscrit) {
            // Afficher le bouton de désinscription
            desinscriptionButton.setVisible(true);
            
            // Réinitialiser les classes et ajouter celles nécessaires
            desinscriptionButton.getStyleClass().clear();
            desinscriptionButton.getStyleClass().addAll("details-button", "danger-button");
            
            // Cacher le bouton d'inscription
            inscriptionButton.setVisible(false);
        }
        // 3. Si l'événement est complet et l'utilisateur n'est pas inscrit
        else if (!hasFreeSpots) {
            // Afficher "Complet" et désactiver le bouton
            inscriptionButton.setText("Complet");
            inscriptionButton.setDisable(true);
            inscriptionButton.setVisible(true);
            
            // Réinitialiser les classes et ajouter celles nécessaires
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-expired");
            
            // Cacher le bouton de désinscription
            desinscriptionButton.setVisible(false);
        }
        // 4. Par défaut: l'événement est actif, a des places, et l'utilisateur n'est pas inscrit
        else {
            // Afficher le bouton d'inscription
            inscriptionButton.setText("S'inscrire");
            inscriptionButton.setDisable(false);
            inscriptionButton.setVisible(true);
            
            // Réinitialiser les classes et ajouter celles nécessaires
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-active");
            
            // Cacher le bouton de désinscription
            desinscriptionButton.setVisible(false);
        }
        
        System.out.println("État final - Bouton inscription: visible=" + inscriptionButton.isVisible() + 
                          ", désactivé=" + inscriptionButton.isDisable() + 
                          ", texte=" + inscriptionButton.getText() +
                          ", classes=" + inscriptionButton.getStyleClass());
        System.out.println("État final - Bouton désinscription: visible=" + desinscriptionButton.isVisible() + 
                          ", désactivé=" + desinscriptionButton.isDisable() +
                          ", classes=" + desinscriptionButton.getStyleClass());
    }
    
    @FXML
    private void handleInscription() {
        // Check all the required dependencies are available
        if (currentUser == null || event == null) {
            return;
        }
        
        // Check if event is not archived or completed
        if (event.isArchived()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Événement archivé");
            alert.setHeaderText("Cet événement n'est plus disponible");
            alert.setContentText("Cet événement a été archivé et n'accepte plus d'inscriptions.");
            alert.showAndWait();
            return;
        }
        
        // Check if event has available spots
        if (event.getPlacesDisponibles() <= 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Événement complet");
            alert.setHeaderText("Toutes les places sont prises");
            alert.setContentText("Cet événement n'a plus de places disponibles. Veuillez choisir un autre événement.");
            alert.showAndWait();
            return;
        }
        
        // Debug check archiver service
        System.out.println("Forçage d'une vérification de l'EventArchiverService...");
        try {
            EventArchiverService archiverService = org.example.App.getEventArchiverService();
            if (archiverService != null) {
                archiverService.forceCheck();
                System.out.println("Vérification de l'EventArchiverService forcée avec succès");
            } else {
                System.out.println("EventArchiverService n'est pas disponible");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du forçage de la vérification: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Create new inscription
        Inscription inscription = new Inscription();
        inscription.setUserId(currentUser.getId());
        inscription.setEventId(event.getId());
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setHasUnsubscribed(false);
        
        // Try to add it
        try {
            // For demo mode or in case of DB issues, simulate success
            boolean success = inscriptionService.addInscription(inscription);
            
            if (success) {
                // Update places left
                int placesLeft = event.getPlacesDisponibles() - 1;
                event.setPlacesDisponibles(placesLeft);
                eventService.updatePlacesDisponibles(event.getId(), placesLeft);
                
                // Update UI
                placesLabel.setText(placesLeft + " places disponibles");
                
                // Hide/show buttons accordingly
                inscriptionButton.setVisible(false);
                desinscriptionButton.setVisible(true);
                
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription réussie");
                alert.setHeaderText("Vous êtes inscrit(e) !");
                alert.setContentText("Votre inscription à l'événement \"" + event.getTitle() + "\" a été enregistrée avec succès.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'inscription");
                alert.setHeaderText("Impossible de vous inscrire");
                alert.setContentText("Une erreur est survenue lors de l'inscription. Veuillez réessayer plus tard.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            // Log error
            System.err.println("Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
            
            // Simulate success in demo mode
            if (inscriptionService.checkConnection() == false) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Mode démo");
                alert.setHeaderText("Inscription simulée");
                alert.setContentText("En mode démo, l'inscription a été simulée avec succès.");
                alert.showAndWait();
                
                // Hide/show buttons accordingly
                inscriptionButton.setVisible(false);
                desinscriptionButton.setVisible(true);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Erreur lors de l'inscription");
                alert.setContentText("Une erreur est survenue: " + e.getMessage());
                alert.showAndWait();
            }
        }
        
        // Final UI update
        updateInscriptionButtons();
        System.out.println("État final des boutons: Inscription " + 
                          (inscriptionButton.isVisible() ? "visible" : "non visible") + 
                          ", Désinscription " + 
                          (desinscriptionButton.isVisible() ? "visible" : "non visible"));
    }
    
    @FXML
    private void handleDesinscription() {
        if (currentUser == null || event == null) return;
        
        // Vérifier que l'événement n'est pas terminé ou archivé
        if (event.isArchived() || event.getEndDate().isBefore(LocalDateTime.now())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Désinscription impossible");
            alert.setHeaderText(null);
            alert.setContentText("Cet événement est déjà terminé.");
            alert.showAndWait();
            return;
        }
        
        boolean success = false;
        
        try {
            success = inscriptionService.unsubscribeFromEvent(currentUser.getId(), event.getId());
            
            // Si la désinscription a réussi, mettre à jour les places disponibles dans la base de données
            if (success) {
                int newPlaces = event.getPlacesDisponibles() + 1;
                boolean updateSuccess = eventService.updatePlacesDisponibles(event.getId(), newPlaces);
                
                if (updateSuccess) {
                    System.out.println("Places disponibles mises à jour avec succès: " + newPlaces);
                } else {
                    System.err.println("Échec de la mise à jour des places disponibles");
                }
                
                // Mettre à jour l'objet event local
                event.setPlacesDisponibles(newPlaces);
                
                // Envoyer une notification d'annulation
                try {
                    NotificationService notificationService = new NotificationService();
                    String details = "Vous vous êtes désinscrit de l'événement \"" + event.getTitle() + 
                               "\" qui était prévu le " + event.getStartDate().toLocalDate() + 
                               " à " + event.getStartDate().getHour() + "h" + 
                               (event.getStartDate().getMinute() > 0 ? event.getStartDate().getMinute() : "") +
                               ". Lieu: " + event.getLocation();
                    
                    notificationService.sendEventCancellationNotification(currentUser.getId(), event.getId(), details);
                    System.out.println("✅ Notification d'annulation envoyée avec succès");
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi de la notification d'annulation: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la désinscription: " + e.getMessage());
            e.printStackTrace();
            // Mode démo - simuler un succès même si la BD n'est pas disponible
            success = true;
            event.setPlacesDisponibles(event.getPlacesDisponibles() + 1);
        }
        
        if (success) {
            // Mettre à jour l'interface
            updateInscriptionButtons();
            placesLabel.setText("Places disponibles: " + event.getPlacesDisponibles());
            
            // Afficher un message de confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Désinscription réussie");
            alert.setHeaderText(null);
            alert.setContentText("Vous êtes désinscrit de l'événement: " + event.getTitle());
            alert.showAndWait();
        } else {
            // Gérer l'échec
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de désinscription");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de vous désinscrire de cet événement. Veuillez réessayer.");
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleRetour() {
        System.out.println("Retour depuis la page de détails de l'événement");
        
        if (previousStage != null) {
            previousStage.show();
            
            // Obtenir la fenêtre actuelle et la fermer
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.close();
        } else {
            System.err.println("Aucune scène précédente définie");
        }
    }
} 