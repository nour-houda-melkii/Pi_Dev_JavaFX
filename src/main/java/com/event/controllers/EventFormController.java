package com.event.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.event.models.Event;
import com.event.models.CategorieEvent;
import com.event.services.CategorieEventDAO;
import com.event.services.EventDAO;
import com.event.services.LlamaService;
import com.event.services.GeminiService;
import javafx.scene.control.ProgressIndicator;
import com.event.services.StabilityAIService;
import com.event.services.InscriptionService;
import com.event.services.EventService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;

public class EventFormController {

    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private Spinner<Integer> hourStartSpinner;
    @FXML private Spinner<Integer> minuteStartSpinner;
    @FXML private Spinner<Integer> hourEndSpinner;
    @FXML private Spinner<Integer> minuteEndSpinner;
    @FXML private TextField fieldLieu;
    @FXML private TextField fieldLatitude;
    @FXML private TextField fieldLongitude;
    @FXML private TextField fieldPlaces;
    @FXML private ComboBox<CategorieEvent> comboCategorie;
    @FXML private Label labelImagePath;
    @FXML private ImageView imagePreview;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label errorLabel;
    @FXML private Label titreError;
    @FXML private Label descriptionError;
    @FXML private Label dateDebutError;
    @FXML private Label dateFinError;
    @FXML private Label lieuError;
    @FXML private Label latitudeError;
    @FXML private Label longitudeError;
    @FXML private Label placesError;
    @FXML private Label categorieError;
    @FXML private Label imageError;
    @FXML private WebView mapView;
    @FXML private Label selectedLocationLabel;
    @FXML private StackPane loadingOverlay;
    @FXML private Button generateDescriptionBtn;
    @FXML private ImageView aiIcon;

    private final EventDAO eventDAO = new EventDAO();
    private final CategorieEventDAO categorieDAO = new CategorieEventDAO();

    private Event event;
    private Consumer<Void> onFormSubmitted;
    private File selectedImageFile;
    private final String IMAGE_DIR = "src/main/resources/affiches/symfony/";
    private WebEngine webEngine;
    private LlamaService llamaService;
    private GeminiService geminiService;
    private StabilityAIService stabilityAIService;

    @FXML
    public void initialize() {
        // Initialiser les services
        geminiService = new GeminiService();
        llamaService = new LlamaService();
        stabilityAIService = new StabilityAIService();

        // Configuration de l'interface
        initializeDateSpinners();
        initMap();
        initializeEventCategories();
        setupValidations();
        clearErrors();
        
        // Configurer le comportement de chargement
        loadingOverlay.setVisible(false);

        // Configurer l'icône IA
        try {
            aiIcon.setImage(new Image(getClass().getResourceAsStream("/images/icons/ai_icon.png")));
        } catch (Exception e) {
            System.err.println("Impossible de charger l'icône IA: " + e.getMessage());
        }
        
        // Vérifier si Gemini est disponible
        if (!geminiService.isAvailable()) {
            generateDescriptionBtn.setDisable(true);
            generateDescriptionBtn.setTooltip(new Tooltip("API Gemini non configurée"));
        }
        
        // S'assurer que le dossier des images existe
        try {
            Files.createDirectories(Paths.get(IMAGE_DIR));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier des images: " + e.getMessage());
        }
    }

    private void initMap() {
        try {
            webEngine = mapView.getEngine();
            
            // Désactiver les popups d'erreurs JavaScript
            webEngine.setOnError(event -> {
                System.err.println("JavaScript Error: " + event.getMessage());
            });

            // Charger le HTML de la carte
            webEngine.loadContent(getMapHtml());
            
            // Attendre que la carte soit chargée
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    try {
                        // Permettre à JavaScript d'appeler des méthodes Java
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaController", this);
                        
                        // Résoudre le problème de dimensionnement de la carte
                        webEngine.executeScript("setTimeout(function() { map.invalidateSize(true); }, 1000);");
                        
                        // Si nous avons déjà des coordonnées (en mode édition), centrer la carte dessus
                        if (event != null && event.getLatitude() != 0 && event.getLongitude() != 0) {
                            try {
                                double lat = event.getLatitude();
                                double lng = event.getLongitude();
                                webEngine.executeScript("setMarker(" + lat + ", " + lng + ")");
                                updateCoordinatesLabel(lat, lng);
                            } catch (Exception e) {
                                System.err.println("Erreur lors de la définition du marqueur initial: " + e.getMessage());
                            }
                        } else if (fieldLatitude.getText() != null && !fieldLatitude.getText().isEmpty() 
                                && fieldLongitude.getText() != null && !fieldLongitude.getText().isEmpty()) {
                            try {
                                double lat = Double.parseDouble(fieldLatitude.getText());
                                double lng = Double.parseDouble(fieldLongitude.getText());
                                webEngine.executeScript("setMarker(" + lat + ", " + lng + ")");
                                updateCoordinatesLabel(lat, lng);
                            } catch (NumberFormatException e) {
                                // Coordonnées invalides, centrer sur la Tunisie
                                webEngine.executeScript("map.setView([34.0, 9.0], 6);");
                            }
                        } else {
                            // Par défaut, centrer sur la Tunisie
                            webEngine.executeScript("map.setView([34.0, 9.0], 6);");
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'initialisation de la carte: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de la carte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Méthode appelée depuis JavaScript quand un utilisateur clique sur la carte
    public void updateCoordinates(double lat, double lng) {
        try {
            fieldLatitude.setText(String.format(Locale.US, "%.6f", lat));
            fieldLongitude.setText(String.format(Locale.US, "%.6f", lng));
            updateCoordinatesLabel(lat, lng);
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des coordonnées: " + e.getMessage());
        }
    }
    
    private void updateCoordinatesLabel(double lat, double lng) {
        selectedLocationLabel.setText("Latitude sélectionnée : " + String.format(Locale.US, "%.6f", lat) + 
                                     " | Longitude sélectionnée : " + String.format(Locale.US, "%.6f", lng));
    }
    
    private String getMapHtml() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"fr\">\n" +
               "<head>\n" +
               "    <meta charset=\"utf-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Carte de sélection d'emplacement</title>\n" +
               "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.css\" />\n" +
               "    <script src=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.js\"></script>\n" +
               "    <style>\n" +
               "        html, body { height: 100%; width: 100%; margin: 0; padding: 0; }\n" +
               "        #map { height: 100%; width: 100%; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div id=\"map\"></div>\n" +
               "    <script>\n" +
               "        // Initialiser la carte avec une vue par défaut sur la Tunisie\n" +
               "        var map = L.map('map', {\n" +
               "            center: [34.0, 9.0],\n" +
               "            zoom: 6,\n" +
               "            attributionControl: true,\n" +
               "            zoomControl: true\n" +
               "        });\n" +
               "        \n" +
               "        // Ajouter la couche de tuiles OpenStreetMap\n" +
               "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
               "            attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors',\n" +
               "            maxZoom: 19\n" +
               "        }).addTo(map);\n" +
               "        \n" +
               "        // Variable pour stocker le marqueur\n" +
               "        var marker = null;\n" +
               "        \n" +
               "        // Fonction pour définir/déplacer le marqueur\n" +
               "        function setMarker(lat, lng) {\n" +
               "            // Supprimer le marqueur existant s'il y en a un\n" +
               "            if (marker) {\n" +
               "                map.removeLayer(marker);\n" +
               "            }\n" +
               "            \n" +
               "            // Créer un nouveau marqueur\n" +
               "            marker = L.marker([lat, lng], {\n" +
               "                draggable: true,\n" +
               "                title: 'Emplacement de l\\'événement'\n" +
               "            }).addTo(map);\n" +
               "            \n" +
               "            // Ajouter une popup au marqueur\n" +
               "            marker.bindPopup(\n" +
               "                '<b>Emplacement sélectionné</b><br>' +\n" +
               "                'Latitude: ' + lat.toFixed(6) + '<br>' +\n" +
               "                'Longitude: ' + lng.toFixed(6)\n" +
               "            ).openPopup();\n" +
               "            \n" +
               "            // Mettre à jour les coordonnées lorsque le marqueur est déplacé\n" +
               "            marker.on('dragend', function() {\n" +
               "                var pos = marker.getLatLng();\n" +
               "                marker.bindPopup(\n" +
               "                    '<b>Emplacement sélectionné</b><br>' +\n" +
               "                    'Latitude: ' + pos.lat.toFixed(6) + '<br>' +\n" +
               "                    'Longitude: ' + pos.lng.toFixed(6)\n" +
               "                ).openPopup();\n" +
               "                javaController.updateCoordinates(pos.lat, pos.lng);\n" +
               "            });\n" +
               "            \n" +
               "            // Centrer la carte sur le marqueur\n" +
               "            map.setView([lat, lng], 13);\n" +
               "        }\n" +
               "        \n" +
               "        // Gérer les clics sur la carte\n" +
               "        map.on('click', function(e) {\n" +
               "            setMarker(e.latlng.lat, e.latlng.lng);\n" +
               "            javaController.updateCoordinates(e.latlng.lat, e.latlng.lng);\n" +
               "        });\n" +
               "        \n" +
               "        // Forcer le redimensionnement correct de la carte\n" +
               "        setTimeout(function() { map.invalidateSize(); }, 100);\n" +
               "        \n" +
               "        // Redimensionner la carte quand la fenêtre change de taille\n" +
               "        window.addEventListener('resize', function() {\n" +
               "            map.invalidateSize();\n" +
               "        });\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }

    private void setupValidations() {
        // Validation du titre
        fieldTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.trim().isEmpty()) {
                fieldTitre.setStyle("-fx-border-color: red;");
            } else {
                fieldTitre.setStyle("");
            }
        });

        // Validation des dates
        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());

        // Validation des coordonnées
        fieldLatitude.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinates());
        fieldLongitude.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinates());

        // Validation des places
        fieldPlaces.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int places = Integer.parseInt(newVal);
                if (places < 0) {
                    fieldPlaces.setStyle("-fx-border-color: red;");
                } else {
                    fieldPlaces.setStyle("");
                }
            } catch (NumberFormatException e) {
                fieldPlaces.setStyle("-fx-border-color: red;");
            }
        });
    }

    private void validateDates() {
        if (dateDebut.getValue() != null && dateFin.getValue() != null) {
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));

            if (start.isAfter(end)) {
                dateDebut.setStyle("-fx-border-color: red;");
                dateFin.setStyle("-fx-border-color: red;");
            } else {
                dateDebut.setStyle("");
                dateFin.setStyle("");
            }
        }
    }

    private void validateCoordinates() {
        try {
            double lat = Double.parseDouble(fieldLatitude.getText());
            double lon = Double.parseDouble(fieldLongitude.getText());
            
            if (lat < -90 || lat > 90) {
                fieldLatitude.setStyle("-fx-border-color: red;");
            } else {
                fieldLatitude.setStyle("");
            }
            
            if (lon < -180 || lon > 180) {
                fieldLongitude.setStyle("-fx-border-color: red;");
            } else {
                fieldLongitude.setStyle("");
            }
        } catch (NumberFormatException e) {
            fieldLatitude.setStyle("-fx-border-color: red;");
            fieldLongitude.setStyle("-fx-border-color: red;");
        }
    }

    public void setEvent(Event event) {
        this.event = event;

        if (event != null) {
            fieldTitre.setText(event.getTitle());
            fieldDescription.setText(event.getDescription());
            dateDebut.setValue(event.getStartDate().toLocalDate());
            hourStartSpinner.getValueFactory().setValue(event.getStartDate().getHour());
            minuteStartSpinner.getValueFactory().setValue(event.getStartDate().getMinute());
            dateFin.setValue(event.getEndDate().toLocalDate());
            hourEndSpinner.getValueFactory().setValue(event.getEndDate().getHour());
            minuteEndSpinner.getValueFactory().setValue(event.getEndDate().getMinute());
            fieldLieu.setText(event.getLocation());
            fieldLatitude.setText(String.valueOf(event.getLatitude()));
            fieldLongitude.setText(String.valueOf(event.getLongitude()));
            fieldPlaces.setText(String.valueOf(event.getPlacesDisponibles()));
            comboCategorie.setValue(event.getCategorie());

            if (event.getAffiche() != null) {
                labelImagePath.setText(event.getAffiche());
                File imgFile = new File(IMAGE_DIR + event.getAffiche());
                if (imgFile.exists()) {
                    imagePreview.setImage(new Image(imgFile.toURI().toString()));
                }
            }
        }
    }

    public void setOnFormSubmitted(Consumer<Void> callback) {
        this.onFormSubmitted = callback;
    }

    @FXML
    private void handleChooseImage(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            // Vérifier la taille du fichier (max 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                showError("L'image ne doit pas dépasser 5MB");
                return;
            }

            selectedImageFile = file;
            labelImagePath.setText(file.getName());
            imagePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleRetourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/event_list.fxml"));
            Parent eventList = loader.load();
            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) fieldTitre.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(eventList);
            }
        } catch (IOException e) {
            showError("Erreur lors du retour à la liste : " + e.getMessage());
        }
    }

    @FXML
    private void handleSave(ActionEvent e) {
        // Réinitialiser les erreurs
        clearErrors();

        // Valider le formulaire
        List<String> errors = validateForm();
        if (!errors.isEmpty()) {
            return;
        }

        try {
            // Récupérer les valeurs du formulaire
            String title = fieldTitre.getText();
            String description = fieldDescription.getText();
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));
            String lieu = fieldLieu.getText();
            double lat = Double.parseDouble(fieldLatitude.getText());
            double lon = Double.parseDouble(fieldLongitude.getText());
            int places = Integer.parseInt(fieldPlaces.getText());
            CategorieEvent categorie = comboCategorie.getValue();

            // Créer ou mettre à jour l'événement
            if (event == null) {
                event = new Event();
            }

            event.setTitle(title);
            event.setDescription(description);
            event.setStartDate(start);
            event.setEndDate(end);
            event.setLocation(lieu);
            event.setLatitude(lat);
            event.setLongitude(lon);
            event.setPlacesDisponibles(places);
            event.setCategorie(categorie);

            // Gérer l'image si elle a été sélectionnée
            if (selectedImageFile != null) {
                String extension = selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf("."));
                String uniqueName = UUID.randomUUID().toString() + extension;
                Path dest = Paths.get(IMAGE_DIR + uniqueName);
                Files.copy(selectedImageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                event.setAffiche(uniqueName);
                
                // Forcer une synchronisation des images
                try {
                    com.utils.ImageSynchronizer.syncNow();
                    System.out.println("Synchronisation des images forcée après l'ajout de: " + uniqueName);
                } catch (Exception ex) {
                    System.err.println("Erreur lors de la synchronisation des images: " + ex.getMessage());
                }
            }

            // Sauvegarder dans la base de données
            boolean isNewEvent = event.getId() == 0;
            if (isNewEvent) {
                eventDAO.insert(event);
                System.out.println("Nouvel événement créé avec ID: " + event.getId());
                
                // Inscrire automatiquement l'utilisateur actuel pour qu'il puisse recevoir des notifications
                try {
                    InscriptionService inscriptionService = new InscriptionService();
                    com.event.services.UserSession userSession = com.event.services.UserSession.getInstance();
                    
                    if (userSession.isLoggedIn()) {
                        com.event.models.User currentUser = userSession.getLoggedInUser();
                        int userId = currentUser.getId();
                        
                        System.out.println("Tentative d'inscription automatique de l'utilisateur " + userId + 
                                " à l'événement " + event.getId());
                        
                        boolean inscriptionSuccess = inscriptionService.inscrireUtilisateur(userId, event.getId());
                        
                        if (inscriptionSuccess) {
                            System.out.println("Inscription automatique réussie pour l'événement " + event.getId());
                        } else {
                            System.err.println("Échec de l'inscription automatique pour l'événement " + event.getId());
                        }
                    } else {
                        System.out.println("Aucun utilisateur connecté pour l'inscription automatique");
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur lors de l'inscription automatique: " + ex.getMessage());
                    ex.printStackTrace();
                }
                
                // Vérifier spécifiquement ce nouvel événement pour créer des notifications
                try {
                    // Afficher une notification de création réussie
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Événement créé");
                    successAlert.setHeaderText("Création réussie");
                    successAlert.setContentText("L'événement a été créé avec succès. Les notifications seront envoyées automatiquement aux participants 24h avant le début de l'événement.");
                    successAlert.showAndWait();
                } catch (Exception ex) {
                    System.err.println("Erreur lors de la vérification des notifications pour le nouvel événement: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                eventDAO.update(event);
                
                // Forcer la vérification des notifications pour l'événement mis à jour aussi
                try {
                    // Afficher une notification de mise à jour réussie
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Événement mis à jour");
                    successAlert.setHeaderText("Mise à jour réussie");
                    successAlert.setContentText("L'événement a été mis à jour avec succès.");
                    successAlert.showAndWait();
                } catch (Exception ex) {
                    System.err.println("Erreur lors de la vérification des notifications pour l'événement mis à jour: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // Notifier le callback et retourner à la liste
            if (onFormSubmitted != null) {
                onFormSubmitted.accept(null);
            }
            
            // Retourner à la liste des événements
            handleRetourListe();
        } catch (Exception ex) {
            showError("Une erreur est survenue lors de la sauvegarde : " + ex.getMessage());
        }
    }

    @FXML
    private void handleGenerateImageButton(ActionEvent event) {
        if (fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Titre requis", "Veuillez saisir un titre avant de générer une affiche.");
            return;
        }
        
        // Désactiver les boutons pendant la génération
        btnSave.setDisable(true);
        loadingOverlay.setVisible(true);
        
            String title = fieldTitre.getText();
        String description = fieldDescription.getText();
            
        // Utiliser Stability AI pour une qualité professionnelle
        if (stabilityAIService.isAvailable()) {
            stabilityAIService.generateEventPoster(title, description)
                .thenAccept(imageName -> {
                    Platform.runLater(() -> {
                        loadImage(imageName);
                        loadingOverlay.setVisible(false);
                        btnSave.setDisable(false);
                        showAlert(Alert.AlertType.INFORMATION, "Image générée", 
                                "Une affiche de qualité professionnelle a été générée avec succès !");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        handleStabilityGenerationError(ex, title);
                    });
                    return null;
                });
        }
        // Utiliser Llama comme fallback si Stability n'est pas disponible
        else if (llamaService.isAvailable()) {
            try {
                String formattedStartDate = "";
                String formattedEndDate = "";
                
                if (dateDebut.getValue() != null) {
                    formattedStartDate = dateDebut.getValue().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
                }
                
                if (dateFin.getValue() != null) {
                    formattedEndDate = dateFin.getValue().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
                }
                
                // Use Llama to generate the poster asynchronously
                llamaService.generateEventPoster(title, formattedStartDate, formattedEndDate)
                    .thenAccept(imageName -> {
                        Platform.runLater(() -> {
                            loadImage(imageName);
                            loadingOverlay.setVisible(false);
                            btnSave.setDisable(false);
                            showAlert(Alert.AlertType.INFORMATION, "Image générée", 
                                    "Une affiche basique a été générée. Pour de meilleurs résultats, activez votre clé Stability AI.");
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            handleGenerationError(ex, title);
                        });
                        return null;
                    });
            } catch (Exception e) {
                handleGenerationError(e, title);
            }
            } else {
            // Fallback si aucun service IA n'est disponible
                    try {
                        String placeholderImageName = llamaService.generatePlaceholderImage(title);
                        loadImage(placeholderImageName);
                showAlert(Alert.AlertType.INFORMATION, "Mode basique", 
                        "Aucun service IA n'est disponible. Une image de substitution a été générée.");
                    } catch (IOException e) {
                        handleGenerationError(e, title);
                    } finally {
                        loadingOverlay.setVisible(false);
                        btnSave.setDisable(false);
            }
        }
    }
    
    private void handleStabilityGenerationError(Throwable ex, String title) {
        loadingOverlay.setVisible(false);
        btnSave.setDisable(false);
        
        System.err.println("Erreur Stability AI: " + ex.getMessage());
        ex.printStackTrace();
        
        // Essayer d'utiliser Llama comme fallback
        if (llamaService.isAvailable()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de génération avancée", 
                    "Impossible de générer une image avec Stability AI. Utilisation de l'alternative basique...");
            
            try {
                String formattedStartDate = dateDebut.getValue() != null ? 
                    dateDebut.getValue().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) : "";
                String formattedEndDate = dateFin.getValue() != null ? 
                    dateFin.getValue().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)) : "";
                
                loadingOverlay.setVisible(true);
                llamaService.generateEventPoster(title, formattedStartDate, formattedEndDate)
                    .thenAccept(imageName -> {
                        Platform.runLater(() -> {
                            loadImage(imageName);
                            loadingOverlay.setVisible(false);
                            btnSave.setDisable(false);
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            handleGenerationError(e, title);
                        });
                        return null;
                    });
        } catch (Exception e) {
            handleGenerationError(e, title);
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur de génération d'image", 
                    "Impossible de générer une affiche : " + ex.getMessage());
        }
    }
    
    private void handleGenerationError(Throwable ex, String title) {
        System.err.println("Erreur lors de la génération d'image: " + ex.getMessage());
        ex.printStackTrace();
        
        try {
            // Essayer de générer une image de substitution
            String placeholderImageName = llamaService.generatePlaceholderImage(title);
            loadImage(placeholderImageName);
            showAlert(Alert.AlertType.ERROR, "Erreur de génération", 
                    "Une erreur s'est produite lors de la génération de l'image. Une image de substitution a été créée.");
        } catch (IOException e) {
            System.err.println("Échec de génération d'image de substitution: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Impossible de générer l'image ou une image de substitution.");
        } finally {
            loadingOverlay.setVisible(false);
            btnSave.setDisable(false);
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearErrors() {
        titreError.setVisible(false);
        titreError.setManaged(false);
        descriptionError.setVisible(false);
        descriptionError.setManaged(false);
        dateDebutError.setVisible(false);
        dateDebutError.setManaged(false);
        dateFinError.setVisible(false);
        dateFinError.setManaged(false);
        lieuError.setVisible(false);
        lieuError.setManaged(false);
        latitudeError.setVisible(false);
        latitudeError.setManaged(false);
        longitudeError.setVisible(false);
        longitudeError.setManaged(false);
        placesError.setVisible(false);
        placesError.setManaged(false);
        categorieError.setVisible(false);
        categorieError.setManaged(false);
        imageError.setVisible(false);
        imageError.setManaged(false);

        // Réinitialiser les styles des champs
        fieldTitre.setStyle("");
        fieldDescription.setStyle("");
        dateDebut.setStyle("");
        dateFin.setStyle("");
        fieldLieu.setStyle("");
        fieldLatitude.setStyle("");
        fieldLongitude.setStyle("");
        fieldPlaces.setStyle("");
        comboCategorie.setStyle("");
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        clearErrors();

        // Validation du titre
        if (fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            showFieldError(titreError, "Le titre est obligatoire");
            fieldTitre.setStyle("-fx-border-color: red;");
            errors.add("Le titre est obligatoire");
        }

        // Validation des dates
        if (dateDebut.getValue() == null) {
            showFieldError(dateDebutError, "La date de début est obligatoire");
            dateDebut.setStyle("-fx-border-color: red;");
            errors.add("La date de début est obligatoire");
        }

        if (dateFin.getValue() == null) {
            showFieldError(dateFinError, "La date de fin est obligatoire");
            dateFin.setStyle("-fx-border-color: red;");
            errors.add("La date de fin est obligatoire");
        }

        if (dateDebut.getValue() != null && dateFin.getValue() != null) {
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));

            if (start.isAfter(end)) {
                showFieldError(dateDebutError, "La date de début doit être avant la date de fin");
                dateDebut.setStyle("-fx-border-color: red;");
                dateFin.setStyle("-fx-border-color: red;");
                errors.add("La date de début doit être avant la date de fin");
            }
        }

        // Validation du lieu
        if (fieldLieu.getText() == null || fieldLieu.getText().trim().isEmpty()) {
            showFieldError(lieuError, "Le lieu est obligatoire");
            fieldLieu.setStyle("-fx-border-color: red;");
            errors.add("Le lieu est obligatoire");
        }

        // Validation des coordonnées
        try {
            double lat = Double.parseDouble(fieldLatitude.getText());
            if (lat < -90 || lat > 90) {
                showFieldError(latitudeError, "La latitude doit être entre -90 et 90");
                fieldLatitude.setStyle("-fx-border-color: red;");
                errors.add("La latitude doit être entre -90 et 90");
            }
        } catch (NumberFormatException e) {
            showFieldError(latitudeError, "La latitude doit être un nombre valide");
            fieldLatitude.setStyle("-fx-border-color: red;");
            errors.add("La latitude doit être un nombre valide");
        }

        try {
            double lon = Double.parseDouble(fieldLongitude.getText());
            if (lon < -180 || lon > 180) {
                showFieldError(longitudeError, "La longitude doit être entre -180 et 180");
                fieldLongitude.setStyle("-fx-border-color: red;");
                errors.add("La longitude doit être entre -180 et 180");
            }
        } catch (NumberFormatException e) {
            showFieldError(longitudeError, "La longitude doit être un nombre valide");
            fieldLongitude.setStyle("-fx-border-color: red;");
            errors.add("La longitude doit être un nombre valide");
        }

        // Validation des places
        try {
            int places = Integer.parseInt(fieldPlaces.getText());
            if (places < 0) {
                showFieldError(placesError, "Le nombre de places doit être positif");
                fieldPlaces.setStyle("-fx-border-color: red;");
                errors.add("Le nombre de places doit être positif");
            }
        } catch (NumberFormatException e) {
            showFieldError(placesError, "Le nombre de places doit être un nombre valide");
            fieldPlaces.setStyle("-fx-border-color: red;");
            errors.add("Le nombre de places doit être un nombre valide");
        }

        // Validation de la catégorie
        if (comboCategorie.getValue() == null) {
            showFieldError(categorieError, "La catégorie est obligatoire");
            comboCategorie.setStyle("-fx-border-color: red;");
            errors.add("La catégorie est obligatoire");
        }

        return errors;
    }

    @FXML
    private void handleCancel(ActionEvent e) {
        closeForm(e);
    }

    private void closeForm(ActionEvent e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.close();
    }

    /**
     * Charge une image à partir de son nom de fichier
     * @param imageName Le nom du fichier image à charger
     */
    private void loadImage(String imageName) {
        try {
            File imgFile = new File(IMAGE_DIR + imageName);
            if (imgFile.exists()) {
                Image image = new Image(imgFile.toURI().toString());
                imagePreview.setImage(image);
                labelImagePath.setText(imageName);
                selectedImageFile = imgFile;
            } else {
                throw new FileNotFoundException("Image file not found: " + imageName);
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            showError("Erreur lors du chargement de l'image: " + e.getMessage());
        }
    }
    
    /**
     * Affiche une alerte avec le type, titre et message spécifiés
     * @param alertType Le type d'alerte
     * @param title Le titre de l'alerte
     * @param message Le message de l'alerte
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Gère la génération automatique de description avec Gemini AI
     */
    @FXML
    private void handleGenerateDescription() {
        String title = fieldTitre.getText();
        if (title == null || title.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Titre manquant", 
                     "Veuillez d'abord saisir un titre pour générer une description.");
            return;
        }
        
        // Créer un indicateur de chargement
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(24, 24);
        
        // Désactiver le bouton et montrer l'indicateur de chargement
        Button originalButton = generateDescriptionBtn;
        String originalText = originalButton.getText();
        originalButton.setText("Génération...");
        originalButton.setGraphic(progressIndicator);
        originalButton.setDisable(true);
        fieldDescription.setDisable(true);
        
        // Appeler l'API Gemini
        geminiService.generateEventDescription(title)
            .thenAccept(description -> {
                Platform.runLater(() -> {
                    fieldDescription.setText(description);
                    
                    // Réinitialiser le bouton
                    originalButton.setText(originalText);
                    originalButton.setGraphic(null);
                    originalButton.setDisable(false);
                    fieldDescription.setDisable(false);
                    
                    // Montrer un message de succès
                    showAlert(Alert.AlertType.INFORMATION, "Description générée", 
                             "Une description a été générée avec succès pour votre événement.");
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    // Réinitialiser le bouton
                    originalButton.setText(originalText);
                    originalButton.setGraphic(null);
                    originalButton.setDisable(false);
                    fieldDescription.setDisable(false);
                    
                    // Afficher l'erreur
                    showAlert(Alert.AlertType.ERROR, "Erreur de génération", 
                             "Impossible de générer une description: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Initialise les spinners pour les heures et minutes
     */
    private void initializeDateSpinners() {
        // Initialiser les spinners d'heure/minute
        hourStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        minuteStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        hourEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18));
        minuteEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    }
    
    /**
     * Charge les catégories d'événements actives
     */
    private void initializeEventCategories() {
        // Charger les catégories actives
        List<CategorieEvent> categories = categorieDAO.getAllActive();
        comboCategorie.setItems(FXCollections.observableArrayList(categories));
    }
}