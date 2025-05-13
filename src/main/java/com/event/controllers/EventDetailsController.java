package com.event.controllers;

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
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.paint.Color;

import com.event.models.Event;
import com.event.models.User;
import com.event.models.Inscription;
import com.event.services.InscriptionService;
import com.event.services.UserService;
import com.event.services.NotificationService;
import com.event.services.EventArchiverService;
import com.event.utils.SessionManager;
import com.event.services.RatingService;
import com.event.services.EmailService;
import com.event.services.EventService;
import com.event.models.Rating;

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
    
    @FXML
    private HBox ratingContainer;
    
    private WebEngine webEngine;
    private Event event;
    private Stage previousStage;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private InscriptionService inscriptionService;
    private UserService userService;
    private User currentUser;
    private EventService eventService;
    private RatingService ratingService;
    private int currentRating = 0;
    
    @FXML
    public void initialize() {
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        userService = new UserService();
        eventService = new EventService();
        inscriptionService = new InscriptionService();
        ratingService = new RatingService();
        
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
        
        // Toujours relire l'utilisateur depuis la session
        currentUser = com.event.utils.SessionManager.getCurrentUser();
        System.out.println("EventDetailsController initialisé avec utilisateur: " + 
            (currentUser != null ? currentUser.getEmail() + " (rôle: " + currentUser.getRole() + ")" : "null"));
        
        // Par défaut, on rend les boutons visibles mais désactivés
        inscriptionButton.setVisible(true);
        inscriptionButton.setDisable(currentUser == null); // Activé seulement si utilisateur connecté
        desinscriptionButton.setVisible(false);
        
        // Initialiser le conteneur de notation comme invisible par défaut
        if (ratingContainer != null) {
            ratingContainer.setVisible(false);
            ratingContainer.setManaged(false);
        }
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        com.event.utils.SessionManager.setCurrentUser(user);
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
            inscriptionButton.setDisable(true);
            System.out.println("Aucun utilisateur défini, boutons désactivés");
        }
    }
    
    public void setEvent(Event event) {
        this.event = event;
        // Toujours relire l'utilisateur courant depuis la session
        this.currentUser = com.event.utils.SessionManager.getCurrentUser();
        // Charger les détails d'abord
        displayEventDetails();
        // Forcer une vérification immédiate de l'état des boutons
        System.out.println("Vérification du statut de l'événement...");
        boolean isExpired = event.isArchived() || event.getEndDate().isBefore(java.time.LocalDateTime.now());
        System.out.println("L'événement est-il expiré: " + isExpired);
        if (currentUser != null) {
            updateInscriptionButtons();
            if (isExpired) {
                setupRatingSystem();
            }
            // Ajout : forcer la réactivation du bouton après update
            setUser(currentUser);
        } else {
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
                if (event.getAffiche() != null && !event.getAffiche().isEmpty()) {
                // Essayer d'abord de charger depuis le dossier symfony
                String filePath = "src/main/resources/affiches/symfony/" + event.getAffiche();
                java.io.File file = new java.io.File(filePath);
                
                    if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), true);
                    eventImage.setImage(image);
                    System.out.println("Image chargée avec succès: " + filePath);
                } else {
                    // Essayer de charger depuis le dossier parent
                    String parentPath = "src/main/resources/affiches/" + event.getAffiche();
                    java.io.File parentFile = new java.io.File(parentPath);
                    
                    if (parentFile.exists()) {
                        Image image = new Image(parentFile.toURI().toString(), true);
                        eventImage.setImage(image);
                        System.out.println("Image chargée avec succès (dossier parent): " + parentPath);
                    } else {
                        System.err.println("Image non trouvée: " + filePath + " ni " + parentPath);
                        // Image par défaut si aucune image n'est trouvée
                        eventImage.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
            }
                }
            } else {
                // Aucune image spécifiée, utiliser l'image par défaut
                eventImage.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, utiliser l'image par défaut
            try {
                eventImage.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
            } catch (Exception ex) {
                System.err.println("Impossible de charger l'image par défaut: " + ex.getMessage());
            }
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
        System.out.println("[DEBUG] Mise à jour des boutons d'inscription pour l'événement: " + event.getTitle());
        System.out.println("[DEBUG] Utilisateur: " + currentUser.getEmail() + " (ID: " + currentUser.getId() + ")");
        System.out.println("[DEBUG] Places disponibles: " + event.getPlacesDisponibles());
        System.out.println("[DEBUG] Date de fin: " + event.getEndDate());
        System.out.println("[DEBUG] Date actuelle: " + LocalDateTime.now());
        System.out.println("[DEBUG] Événement archivé: " + event.isArchived());
        
        boolean isInscrit = false;
        try {
            isInscrit = inscriptionService.isUserRegisteredForEvent(currentUser.getId(), event.getId());
            System.out.println("[DEBUG] L'utilisateur est-il inscrit? " + isInscrit);
        } catch (Exception e) {
            System.err.println("[DEBUG] Erreur lors de la vérification de l'inscription: " + e.getMessage());
        }
        boolean isEventExpired = event.isArchived() || event.getEndDate().isBefore(LocalDateTime.now());
        System.out.println("[DEBUG] L'événement est-il expiré? " + isEventExpired);
        boolean hasFreeSpots = event.getPlacesDisponibles() > 0;
        System.out.println("[DEBUG] Y a-t-il des places disponibles? " + hasFreeSpots);
        boolean isAdmin = (currentUser != null && currentUser.getRole() != null && 
                          (currentUser.getRole().contains("ADMIN") || currentUser.getRole().equalsIgnoreCase("admin")));
        System.out.println("[DEBUG] L'utilisateur est-il admin? " + isAdmin);

        // DEBUG: Forcer l'activation du bouton si utilisateur connecté, non inscrit, non archivé, places > 0
        if (!isInscrit && !isEventExpired && hasFreeSpots) {
            inscriptionButton.setText("S'inscrire");
            inscriptionButton.setDisable(false);
            inscriptionButton.setVisible(true);
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-active");
            desinscriptionButton.setVisible(false);
            System.out.println("[DEBUG] Bouton S'inscrire FORCÉ ACTIVÉ (debug)");
            return;
        }

        // ... (reste de la logique normale)
        if (isAdmin) {
            System.out.println("L'utilisateur est admin, activation des boutons");
            inscriptionButton.setDisable(false);
            desinscriptionButton.setDisable(false);
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-active");
            desinscriptionButton.getStyleClass().clear();
            desinscriptionButton.getStyleClass().addAll("details-button", "danger-button");
            inscriptionButton.setVisible(!isInscrit);
            desinscriptionButton.setVisible(isInscrit);
            inscriptionButton.setText("S'inscrire");
            return;
        }
        if (isInscrit) {
            desinscriptionButton.setVisible(true);
            desinscriptionButton.getStyleClass().clear();
            desinscriptionButton.getStyleClass().addAll("details-button", "danger-button");
            inscriptionButton.setVisible(false);
        } else if (!hasFreeSpots) {
            inscriptionButton.setText("Complet");
            inscriptionButton.setDisable(true);
            inscriptionButton.setVisible(true);
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-expired");
            desinscriptionButton.setVisible(false);
        } else if (isEventExpired) {
            inscriptionButton.setVisible(false);
            inscriptionButton.setManaged(false);
            desinscriptionButton.setVisible(false);
            desinscriptionButton.setManaged(false);
            if (currentUser != null) {
                setupRatingSystem();
                if (ratingContainer != null) {
                    ratingContainer.setVisible(true);
                    ratingContainer.setManaged(true);
                }
            }
        } else {
            inscriptionButton.setText("S'inscrire");
            inscriptionButton.setDisable(false);
            inscriptionButton.setVisible(true);
            inscriptionButton.getStyleClass().clear();
            inscriptionButton.getStyleClass().addAll("details-button", "button-active");
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
        
        // Vérifier les chevauchements d'événements
        InscriptionService.EventOverlapResult overlapResult = inscriptionService.checkEventTimeOverlap(currentUser.getId(), event.getId());
        if (overlapResult.hasOverlap()) {
            Event conflictingEvent = overlapResult.getConflictingEvent();
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conflit d'horaire");
            alert.setHeaderText("Vous êtes déjà inscrit(e) à un événement durant cette période");
            
            String message = String.format(
                "Vous êtes déjà inscrit(e) à l'événement \"%s\" qui se déroule du %s au %s.\n\n" +
                "Cet événement chevauche l'horaire de l'événement \"%s\" prévu du %s au %s.\n\n" +
                "Pour vous inscrire à cet événement, veuillez d'abord vous désinscrire de l'autre événement.",
                conflictingEvent.getTitle(),
                conflictingEvent.getStartDate().format(dateFormatter),
                conflictingEvent.getEndDate().format(dateFormatter),
                event.getTitle(),
                event.getStartDate().format(dateFormatter),
                event.getEndDate().format(dateFormatter)
            );
            
            alert.setContentText(message);
            alert.showAndWait();
            return;
        }
        
        // Debug check archiver service
        System.out.println("Forçage d'une vérification de l'EventArchiverService...");
        try {
            // Supprimer ou commenter l'appel à App.getEventArchiverService()
            // EventArchiverService archiverService = App.getEventArchiverService();
            // if (archiverService != null) {
            //     archiverService.forceCheck();
            //     System.out.println("Vérification de l'EventArchiverService forcée avec succès");
            // } else {
            //     System.out.println("EventArchiverService n'est pas disponible");
            // }
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
                
                // Envoyer un email de confirmation
                try {
                    EmailService emailService = new EmailService();
                    boolean emailSent = emailService.sendEventRegistrationConfirmation(currentUser, event);
                    if (emailSent) {
                        System.out.println("Email de confirmation envoyé avec succès à " + currentUser.getEmail());
                    } else {
                        System.out.println("L'email de confirmation n'a pas pu être envoyé.");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi de l'email de confirmation: " + e.getMessage());
                }
                
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription réussie");
                alert.setHeaderText("Vous êtes inscrit(e) !");
                
                String message = "Votre inscription à l'événement \"" + event.getTitle() + "\" a été enregistrée avec succès.";
                if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                    message += "\n\nUn email de confirmation a été envoyé à " + currentUser.getEmail();
                }
                
                alert.setContentText(message);
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
                // Envoyer un email de confirmation en mode simulation
                try {
                    EmailService emailService = new EmailService();
                    emailService.sendEventRegistrationConfirmation(currentUser, event);
                } catch (Exception emailEx) {
                    System.err.println("Erreur lors de l'envoi de l'email de confirmation: " + emailEx.getMessage());
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Mode démo");
                alert.setHeaderText("Inscription simulée");
                
                String message = "En mode démo, l'inscription a été simulée avec succès.";
                if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                    message += "\n\nUn email de confirmation a été simulé vers " + currentUser.getEmail();
                }
                
                alert.setContentText(message);
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
    
    /**
     * Configure le système de notation par étoiles pour les événements expirés
     */
    private void setupRatingSystem() {
        // S'assurer que le conteneur de notation existe
        if (ratingContainer == null) {
            return;
        }
        
        // Nettoyer le conteneur avant d'ajouter les étoiles
        ratingContainer.getChildren().clear();
        
        // Vérifier si l'utilisateur a déjà noté cet événement
        Rating existingRating = null;
        try {
            existingRating = ratingService.getUserRatingForEvent(currentUser.getId(), event.getId());
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la notation: " + e.getMessage());
        }
        
        // Si une note existe déjà, l'utiliser comme valeur initiale
        currentRating = (existingRating != null) ? (int)existingRating.getRating() : 0;
        
        // Créer les 5 étoiles
        for (int i = 1; i <= 5; i++) {
            final int starValue = i;
            Button starButton = createStarButton();
            
            // Appliquer le style approprié (vide ou rempli)
            SVGPath starPath = (SVGPath) starButton.getGraphic();
            if (i <= currentRating) {
                starPath.getStyleClass().add("star-filled");
            } else {
                starPath.getStyleClass().add("star-empty");
            }
            
            // Ajouter l'action de clic
            starButton.setOnAction(e -> rateEvent(starValue));
            
            // Ajouter l'étoile au conteneur
            ratingContainer.getChildren().add(starButton);
        }
        
        // Afficher le conteneur
        ratingContainer.setVisible(true);
        ratingContainer.setManaged(true);
    }
    
    /**
     * Crée un bouton avec une étoile SVG
     */
    private Button createStarButton() {
        // Créer un chemin SVG pour l'étoile
        SVGPath starPath = new SVGPath();
        starPath.setContent("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z");
        
        // Créer le bouton avec l'étoile
        Button starButton = new Button();
        starButton.setGraphic(starPath);
        starButton.getStyleClass().add("star-button");
        
        return starButton;
    }
    
    /**
     * Gère la notation d'un événement
     */
    private void rateEvent(int value) {
        if (currentUser == null || event == null) {
            return;
        }
        
        // Mettre à jour l'affichage des étoiles
        currentRating = value;
        updateStarsDisplay();
        
        // Créer ou mettre à jour la notation
        try {
            // Vérifier si l'utilisateur a déjà noté cet événement
            boolean hasRated = ratingService.hasUserRatedEvent(currentUser.getId(), event.getId());
            
            Rating rating = new Rating();
            rating.setUserId(currentUser.getId());
            rating.setEventId(event.getId());
            rating.setRating(value);
            
            boolean success;
            if (hasRated) {
                // Mettre à jour la notation existante
                success = ratingService.updateRating(rating);
            } else {
                // Créer une nouvelle notation
                success = ratingService.addRating(rating);
            }
            
            if (success) {
                // Afficher un message de succès
                System.out.println("Notation enregistrée avec succès: " + value + " étoiles");
                
                // On pourrait ajouter une animation ou un message de confirmation ici
            } else {
                System.err.println("Erreur lors de l'enregistrement de la notation");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la notation: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour l'affichage des étoiles en fonction de la notation actuelle
     */
    private void updateStarsDisplay() {
        // Parcourir les étoiles et mettre à jour leur classe CSS
        for (int i = 0; i < ratingContainer.getChildren().size(); i++) {
            Button starButton = (Button) ratingContainer.getChildren().get(i);
            SVGPath starPath = (SVGPath) starButton.getGraphic();
            
            // Supprimer les classes existantes
            starPath.getStyleClass().removeAll("star-empty", "star-filled");
            
            // Ajouter la classe appropriée
            if (i < currentRating) {
                starPath.getStyleClass().add("star-filled");
            } else {
                starPath.getStyleClass().add("star-empty");
            }
        }
    }
} 