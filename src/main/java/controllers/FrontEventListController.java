package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileInputStream;
import javafx.scene.image.Image;

import models.Event;
import services.EventService;

public class FrontEventListController {
    @FXML
    private FlowPane activeEventsContainer;
    
    @FXML
    private FlowPane expiredEventsContainer;
    
    private EventService eventService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            // Forcer l'utilisation du pipeline logiciel
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.forceGPU", "false");
            System.setProperty("prism.vsync", "false");
            
            // Initialiser le service et charger les événements
            eventService = new EventService();
            
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
                Node eventCard = createEventCard(event);
                if (isEventExpired(event)) {
                    expiredEventsContainer.getChildren().add(eventCard);
                } else {
                    activeEventsContainer.getChildren().add(eventCard);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isEventExpired(Event event) {
        return event.getEndDate().isBefore(LocalDateTime.now());
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
            String imagePath = "/affiches/" + event.getAffiche();
            Image image = null;
            
            // Essayer de charger l'image depuis le classpath
            try {
                if (event.getAffiche() != null && !event.getAffiche().isEmpty()) {
                    image = new Image(getClass().getResourceAsStream(imagePath));
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image depuis resources: " + e.getMessage());
            }
            
            // Si cela échoue, essayer de charger directement depuis le système de fichiers
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
            
            // Si les deux tentatives échouent, utiliser l'image par défaut
            if (image == null || image.isError()) {
                image = new Image(getClass().getResourceAsStream("/images/default-event.jpg"));
                if (image == null || image.isError()) {
                    System.err.println("Image par défaut non trouvée");
                }
            }
            
            imageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
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

        // Bouton Détails
        Button detailsButton = new Button("Voir les détails");
        detailsButton.getStyleClass().addAll("details-button", 
            isExpired ? "button-expired" : "button-active");
        
        detailsButton.setOnAction(e -> showEventDetails(event));

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event_details.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("SAHATECH - Détails de " + event.getTitle());
            
            // Configurer en plein écran
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}