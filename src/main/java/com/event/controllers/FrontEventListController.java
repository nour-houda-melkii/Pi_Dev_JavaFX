package com.event.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileInputStream;
import javafx.scene.image.Image;

import com.event.models.Event;
import com.event.models.User;
import com.event.utils.SessionManager;
import com.event.services.EventService;
import com.event.controllers.EventDetailsController;

public class FrontEventListController {
    @FXML
    private FlowPane activeEventsContainer;
    
    @FXML
    private FlowPane expiredEventsContainer;
    
    @FXML
    private WebView mapView;
    
    @FXML
    private AnchorPane mapContainer;
    
    private WebEngine webEngine;
    
    private EventService eventService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private User user;

    @FXML
    public void initialize() {
        try {
            // Toujours relire l'utilisateur depuis la session à chaque affichage
            user = com.event.utils.SessionManager.getCurrentUser();
            System.out.println("FrontEventListController initialisé avec utilisateur: " + 
                (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
            // Initialiser le service et charger les événements
            eventService = new EventService();
            // Initialiser la carte
            initializeMap();
            // Charger les événements après un court délai pour éviter les problèmes de rendu
            Platform.runLater(() -> {
                try {
                    loadEvents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initializeMap() {
        webEngine = mapView.getEngine();
        
        // Charger le HTML de la carte (utilisera Leaflet.js, une bibliothèque de cartographie JavaScript)
        String mapHTML = createMapHTML();
        webEngine.loadContent(mapHTML);
        
        // Attendre que la carte soit chargée avant d'ajouter des marqueurs
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                // Rendre le pont JavaScript-Java disponible
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", this);
                
                // La carte est prête, on peut charger les marqueurs
                Platform.runLater(this::loadEventMarkers);
            }
        });
    }
    
    public void refreshEvents() {
        loadEvents();
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.getAllEvents();
            
            // Vider les conteneurs
            activeEventsContainer.getChildren().clear();
            expiredEventsContainer.getChildren().clear();
            
            // Trier et afficher les événements
            for (Event event : events) {
                boolean isExpired = isEventExpired(event);
                boolean isArchived = event.isArchived();
                
                // Les événements expirés vont dans la section "Événements expirés"
                if (isExpired) {
                Node eventCard = createEventCard(event);
                    expiredEventsContainer.getChildren().add(eventCard);
                } 
                // Les événements non-expirés et non-archivés vont dans la section "Événements en cours"
                else if (!isArchived) {
                    Node eventCard = createEventCard(event);
                    activeEventsContainer.getChildren().add(eventCard);
                }
                // Les événements archivés mais non-expirés ne sont pas affichés
            }
            
            // Recharger les marqueurs sur la carte
            if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                loadEventMarkers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadEventMarkers() {
        try {
            // Vider d'abord tous les marqueurs existants
            webEngine.executeScript("clearAllMarkers()");
            
            List<Event> events = eventService.getAllEvents();
            
            for (Event event : events) {
                // Vérifier si l'événement a des coordonnées valides
                double latitude = event.getLatitude();
                double longitude = event.getLongitude();
                
                // Vérifier si l'événement est expiré ou archivé
                    boolean isExpired = isEventExpired(event);
                boolean isArchived = event.isArchived();
                
                // N'afficher que les événements qui sont soit expirés, soit non archivés (en cours)
                if ((isExpired || !isArchived) && latitude != 0 && longitude != 0) {
                    String markerColor = isExpired ? "red" : "green";
                    String title = event.getTitle();
                    String popupContent = String.format(
                        "<b>%s</b><br/>%s<br/>%s - %s<br/>Places: %d",
                        event.getTitle(),
                        event.getLocation(),
                        event.getStartDate().format(dateFormatter),
                        event.getEndDate().format(dateFormatter),
                        event.getPlacesDisponibles()
                    );
                    
                    // Échapper les caractères spéciaux pour JavaScript
                    title = title.replace("'", "\\'");
                    popupContent = popupContent.replace("'", "\\'");
                    
                    // Ajouter le marqueur
                    webEngine.executeScript(String.format(
                        "addMarker(%f, %f, '%s', '%s', '%s')",
                        latitude, longitude, title, popupContent, markerColor
                    ));
                }
            }
            
            // Ajuster la vue de la carte si nécessaire
            webEngine.executeScript("fitMapToMarkers()");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isEventExpired(Event event) {
        // Un événement est considéré comme expiré uniquement si sa date est passée
        return event.getEndDate().isBefore(LocalDateTime.now());
    }

    // Méthode JavaScript peut appeler cette méthode
    public void showEventDetailsFromMap(String eventId) {
        try {
            int id = Integer.parseInt(eventId);
            Event event = eventService.findById(id);
            if (event != null) {
                showEventDetails(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String createMapHTML() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Carte des Événements</title>\n" +
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
                "        // Initialiser la carte avec une vue centrée sur la Tunisie\n" +
                "        var map = L.map('map').setView([34.0, 9.0], 7);\n" +
                "\n" +
                "        // Ajouter la couche OpenStreetMap\n" +
                "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'\n" +
                "        }).addTo(map);\n" +
                "\n" +
                "        // Stocker tous les marqueurs pour pouvoir ajuster la vue plus tard\n" +
                "        var markers = [];\n" +
                "\n" +
                "        // Fonction pour ajouter un marqueur\n" +
                "        function addMarker(lat, lng, title, popupContent, color) {\n" +
                "            var markerOptions = {\n" +
                "                title: title\n" +
                "            };\n" +
                "\n" +
                "            // Déterminer l'icône en fonction de la couleur (vert pour actif, rouge pour expiré)\n" +
                "            var iconUrl = color === 'green' ? \n" +
                "                'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png' : \n" +
                "                'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png';\n" +
                "\n" +
                "            var customIcon = new L.Icon({\n" +
                "                iconUrl: iconUrl,\n" +
                "                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',\n" +
                "                iconSize: [25, 41],\n" +
                "                iconAnchor: [12, 41],\n" +
                "                popupAnchor: [1, -34],\n" +
                "                shadowSize: [41, 41]\n" +
                "            });\n" +
                "\n" +
                "            markerOptions.icon = customIcon;\n" +
                "            var marker = L.marker([lat, lng], markerOptions).addTo(map);\n" +
                "            marker.bindPopup(popupContent);\n" +
                "            markers.push(marker);\n" +
                "            return marker;\n" +
                "        }\n" +
                "\n" +
                "        // Fonction pour supprimer tous les marqueurs\n" +
                "        function clearAllMarkers() {\n" +
                "            for (var i = 0; i < markers.length; i++) {\n" +
                "                map.removeLayer(markers[i]);\n" +
                "            }\n" +
                "            markers = [];\n" +
                "        }\n" +
                "\n" +
                "        // Ajuster la vue pour voir tous les marqueurs\n" +
                "        function fitMapToMarkers() {\n" +
                "            if (markers.length > 0) {\n" +
                "                var group = new L.featureGroup(markers);\n" +
                "                map.fitBounds(group.getBounds().pad(0.2));\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private Node createEventCard(Event event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("event-card");
        card.setPadding(new Insets(0, 0, 10, 0));
        card.setMaxWidth(320);

        // Image de l'événement
        ImageView imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(180);
        
        try {
                if (event.getAffiche() != null && !event.getAffiche().isEmpty()) {
                // Essayer d'abord de charger depuis le dossier symfony
                String filePath = "src/main/resources/affiches/symfony/" + event.getAffiche();
                java.io.File file = new java.io.File(filePath);
                
                    if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), true);
                    imageView.setImage(image);
                    System.out.println("Image chargée avec succès: " + filePath);
                } else {
                    // Essayer de charger depuis le dossier parent
                    String parentPath = "src/main/resources/affiches/" + event.getAffiche();
                    java.io.File parentFile = new java.io.File(parentPath);
                    
                    if (parentFile.exists()) {
                        Image image = new Image(parentFile.toURI().toString(), true);
                        imageView.setImage(image);
                        System.out.println("Image chargée avec succès (dossier parent): " + parentPath);
                    } else {
                        System.err.println("Image non trouvée: " + filePath + " ni " + parentPath);
                        // Image par défaut si aucune image n'est trouvée
                        imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
                    }
                }
            } else {
                // Aucune image spécifiée, utiliser l'image par défaut
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, utiliser l'image par défaut
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
            } catch (Exception ex) {
                System.err.println("Impossible de charger l'image par défaut: " + ex.getMessage());
            }
        }

        // Conteneur pour le contenu de la carte
        VBox content = new VBox(10);
        content.getStyleClass().add("card-content");
        content.setPadding(new Insets(15));

        // Titre de l'événement
        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().addAll("card-title", "text-primary");
        titleLabel.setWrapText(true);

        // Container pour les badges (status et catégorie)
        HBox badgesContainer = new HBox(10);
        badgesContainer.setAlignment(Pos.CENTER_LEFT);
        badgesContainer.setPadding(new Insets(5, 0, 5, 0));

        // Badge de statut
        Label statusBadge = new Label();
        statusBadge.getStyleClass().addAll("badge");
        
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = event.getEndDate().isBefore(now);
        boolean isFull = event.getPlacesDisponibles() <= 0;
        
        if (isExpired) {
            statusBadge.setText("EXPIRÉ");
            statusBadge.getStyleClass().add("badge-expired");
        } else {
            statusBadge.setText("ACTIF");
            statusBadge.getStyleClass().add("badge-active");
        }

        // Badge de catégorie et ajout des badges
        badgesContainer.getChildren().add(statusBadge);
        
        if (event.getCategorie() != null) {
            Label categoryBadge = new Label(event.getCategorie().getNom());
            categoryBadge.getStyleClass().addAll("badge", "badge-category");
            badgesContainer.getChildren().add(categoryBadge);
        }

        // Informations détaillées
        VBox detailsContainer = new VBox(8);
        detailsContainer.getStyleClass().add("event-details");
        detailsContainer.setPadding(new Insets(10, 0, 10, 0));

        // Lieu
        HBox locationBox = new HBox(5);
        Label locationIcon = new Label("📍");
        Label locationLabel = new Label(event.getLocation());
        locationLabel.getStyleClass().add("location-text");
        locationBox.getChildren().addAll(locationIcon, locationLabel);

        // Dates
        HBox dateBox = new HBox(5);
        Label dateIcon = new Label("🗓");
        Label dateLabel = new Label(String.format("%s - %s",
            event.getStartDate().format(dateFormatter),
            event.getEndDate().format(dateFormatter)));
        dateLabel.getStyleClass().add("date-text");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        // Places disponibles
        HBox placesBox = new HBox(5);
        Label placesIcon = new Label("👥");
        Label placesLabel = new Label(String.format("Places disponibles: %d", event.getPlacesDisponibles()));
        placesLabel.getStyleClass().add("places-text");
        placesBox.getChildren().addAll(placesIcon, placesLabel);

        // Ajouter tous les éléments dans l'ordre
        detailsContainer.getChildren().addAll(locationBox, dateBox, placesBox);

        // Description
        Label descriptionLabel = new Label(event.getDescription());
        descriptionLabel.getStyleClass().add("description-text");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(60); // Limiter la hauteur à 60 pixels
        
        // Tronquer le texte de description si nécessaire
        String description = event.getDescription();
        if (description != null && description.length() > 150) {
            description = description.substring(0, 147) + "...";
        }
        descriptionLabel.setText(description);

        // Bouton Détails ou état (Terminé/Complet)
        Button detailsButton;
        
        // Pour les événements expirés, toujours afficher "Voir les détails"
        if (isExpired) {
            detailsButton = new Button("Voir les détails");
            detailsButton.getStyleClass().addAll("details-button", "button-expired");
            detailsButton.setOnAction(e -> showEventDetails(event));
        }
        // Pour les événements actifs, appliquer la logique d'affichage selon disponibilité
        else if (isFull) {
            // Événement complet -> Bouton "Complet" désactivé
            detailsButton = new Button("Complet");
            detailsButton.getStyleClass().addAll("details-button", "button-expired");  // Utiliser le style rouge pour indiquer indisponible
            detailsButton.setDisable(true);
        } else {
            // Événement actif et avec places disponibles -> Bouton "Voir les détails" actif
            detailsButton = new Button("Voir les détails");
                detailsButton.getStyleClass().addAll("details-button", "button-active");
            detailsButton.setOnAction(e -> showEventDetails(event));
        }

        // Ajout des éléments à la carte
        content.getChildren().addAll(
            titleLabel,
            badgesContainer,
            detailsContainer,
            descriptionLabel,
            detailsButton
        );

        card.getChildren().addAll(imageView, content);
        return card;
    }

    private void showEventDetails(Event event) {
        try {
            System.out.println("Ouverture des détails pour l'événement: " + event.getTitle());
            // Toujours relire l'utilisateur courant depuis la session
            User currentUser = com.event.utils.SessionManager.getCurrentUser();
            System.out.println("Utilisateur actuel (via SessionManager): " + 
                (currentUser != null ? currentUser.getEmail() + " (rôle: " + currentUser.getRole() + ")" : "null"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/event_details.fxml"));
            Parent root = loader.load();
            EventDetailsController controller = loader.getController();
            controller.setEvent(event);
            if (currentUser != null) {
                controller.setUser(currentUser);
            }
            System.out.println("Événement transmis au contrôleur de détails");
            Stage currentStage = (Stage) activeEventsContainer.getScene().getWindow();
            controller.setPreviousStage(currentStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/styles/event/style.css").toExternalForm());
            Stage detailsStage = new Stage();
            detailsStage.setTitle("Détails de l'événement: " + event.getTitle());
            detailsStage.setScene(scene);
            detailsStage.setWidth(800);
            detailsStage.setHeight(700);
            detailsStage.centerOnScreen();
            detailsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'affichage des détails de l'événement: " + e.getMessage());
        }
    }

    public void setUser(User user) {
        this.user = user;
        com.event.utils.SessionManager.setCurrentUser(user);
        loadEvents();
    }
}