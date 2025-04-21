package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;

import models.Event;

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
    
    private WebEngine webEngine;
    private Event event;
    private Stage previousStage;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @FXML
    public void initialize() {
        // Initialisation de la WebView pour la carte
        webEngine = mapView.getEngine();
    }
    
    public void setEvent(Event event) {
        this.event = event;
        displayEventDetails();
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
    
    @FXML
    private void handleRetour() {
        Stage currentStage = (Stage) titleLabel.getScene().getWindow();
        currentStage.close();
    }
} 