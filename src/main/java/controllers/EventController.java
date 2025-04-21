package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Event;
import services.EventDAO;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventController {

    @FXML private TextField searchField;
    @FXML private FlowPane eventsContainer;
    @FXML private FlowPane archivedEventsContainer;

    @FXML private TextField searchTitle;
    @FXML private DatePicker searchDate;
    @FXML private ComboBox<String> searchCategory;

    @FXML private TextField searchTitleArchives;
    @FXML private DatePicker searchDateArchives;
    @FXML private ComboBox<String> searchCategoryArchives;

    private final EventDAO eventDAO = new EventDAO();
    private final String IMAGE_DIR = "src/main/resources/affiches/";

    @FXML
    public void initialize() {
        loadCategories();
        loadEvents();
        setupSearch();
    }
    
    private void setupSearch() {
        // Configurer la recherche pour les événements actifs
        searchTitle.textProperty().addListener((observable, oldValue, newValue) -> filterActiveEvents());
        searchDate.valueProperty().addListener((observable, oldValue, newValue) -> filterActiveEvents());
        searchCategory.valueProperty().addListener((observable, oldValue, newValue) -> filterActiveEvents());
        
        // Configurer la recherche pour les événements archivés
        searchTitleArchives.textProperty().addListener((observable, oldValue, newValue) -> filterArchivedEvents());
        searchDateArchives.valueProperty().addListener((observable, oldValue, newValue) -> filterArchivedEvents());
        searchCategoryArchives.valueProperty().addListener((observable, oldValue, newValue) -> filterArchivedEvents());
    }
    
    private void filterActiveEvents() {
        eventsContainer.getChildren().clear();
        List<Event> events = eventDAO.getAllActifs();
        
        for (Event event : events) {
            if (matchesFilter(event, searchTitle.getText(), searchDate.getValue(), searchCategory.getValue())) {
                eventsContainer.getChildren().add(createEventCard(event, false));
            }
        }
    }
    
    private void filterArchivedEvents() {
        archivedEventsContainer.getChildren().clear();
        List<Event> events = eventDAO.getAllArchives();
        
        for (Event event : events) {
            if (matchesFilter(event, searchTitleArchives.getText(), searchDateArchives.getValue(), searchCategoryArchives.getValue())) {
                archivedEventsContainer.getChildren().add(createEventCard(event, true));
            }
        }
    }
    
    private boolean matchesFilter(Event event, String titleFilter, java.time.LocalDate dateFilter, String categoryFilter) {
        boolean matchesTitle = titleFilter == null || titleFilter.isEmpty() || 
                               event.getTitle().toLowerCase().contains(titleFilter.toLowerCase());
        
        boolean matchesDate = dateFilter == null || event.getStartDate().toLocalDate().equals(dateFilter);
        
        boolean matchesCategory = categoryFilter == null || categoryFilter.isEmpty() || 
                                  categoryFilter.equals("Toutes les catégories") || 
                                  event.getCategorie().getNom().equals(categoryFilter);
        
        return matchesTitle && matchesDate && matchesCategory;
    }

    private VBox createEventCard(Event event, boolean isArchived) {
        VBox card = new VBox();
        card.getStyleClass().add("event-card");
        card.setPrefWidth(320);
        card.setPrefHeight(380);
        card.setMaxWidth(320);
        
        // En-tête avec catégorie
        Label categoryBadge = new Label(truncateText(event.getCategorie().getNom(), 30));
        categoryBadge.getStyleClass().addAll("badge", "badge-category");
        categoryBadge.setPrefWidth(320);
        categoryBadge.setAlignment(Pos.CENTER);
        
        // Image de l'événement avec conteneur pour préserver les proportions
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinHeight(180);
        imageWrapper.setPrefHeight(180);
        imageWrapper.setMaxHeight(180);
        imageWrapper.getStyleClass().add("image-wrapper");
        
        ImageView eventImage = loadImageView(event.getAffiche());
        eventImage.setFitWidth(320);
        eventImage.setFitHeight(180);
        eventImage.setPreserveRatio(true);
        imageWrapper.getChildren().add(eventImage);
        
        // Badge de statut (actif/expiré)
        Label statusBadge = new Label(isArchived ? "Archivé" : "Actif");
        statusBadge.getStyleClass().addAll("badge", isArchived ? "badge-expired" : "badge-active");
        
        // Contenu principal
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getStyleClass().add("card-content");
        
        Label titleLabel = new Label(truncateText(event.getTitle(), 40));
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);
        
        // Informations sur l'événement
        VBox details = new VBox(5);
        details.getStyleClass().add("event-details");
        
        Label locationLabel = new Label("📍 " + truncateText(event.getLocation(), 35));
        locationLabel.getStyleClass().add("location-text");
        locationLabel.setWrapText(true);
        
        Label dateLabel = new Label("🗓️ Du " + 
                                   event.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + 
                                   " au " + 
                                   event.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLabel.getStyleClass().add("date-text");
        dateLabel.setWrapText(true);
        
        Label placesLabel = new Label("🪑 Places disponibles: " + event.getPlacesDisponibles());
        placesLabel.getStyleClass().add("places-text");
        
        details.getChildren().addAll(locationLabel, dateLabel, placesLabel);
        
        // Boutons d'action
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        
        if (!isArchived) {
            Button editButton = createButton("Modifier", "/images/icons/edit.png");
            Button archiveButton = createButton("Archiver", "/images/icons/archive.png");
            
                editButton.getStyleClass().addAll("action-button", "edit-button");
                archiveButton.getStyleClass().addAll("action-button", "archive-button");

            editButton.setOnAction(e -> handleEditEvent(event));
                archiveButton.setOnAction(e -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirmation d'archivage");
                confirmAlert.setHeaderText("Archiver l'événement");
                confirmAlert.setContentText("Êtes-vous sûr de vouloir archiver l'événement \"" + event.getTitle() + "\" ?");

                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    eventDAO.updateEventArchiveStatus(event.getId(), true);
                    loadEvents();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Archivage réussi");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("L'événement a été archivé avec succès.");
                    successAlert.showAndWait();
                }
            });
            
            actions.getChildren().addAll(editButton, archiveButton);
        } else {
            Button restoreButton = createButton("Restaurer", "/images/icons/restore.png");
            Button deleteButton = createButton("Supprimer", "/images/icons/delete.png");
            
                restoreButton.getStyleClass().addAll("action-button", "restore-button");
                deleteButton.getStyleClass().addAll("action-button", "delete-button");

                restoreButton.setOnAction(e -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirmation de restauration");
                confirmAlert.setHeaderText("Restaurer l'événement");
                confirmAlert.setContentText("Êtes-vous sûr de vouloir restaurer l'événement \"" + event.getTitle() + "\" ?");

                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    eventDAO.updateEventArchiveStatus(event.getId(), false);
                    loadEvents();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Restauration réussie");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("L'événement a été restauré avec succès.");
                    successAlert.showAndWait();
                }
                });

                deleteButton.setOnAction(e -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirmation de suppression");
                confirmAlert.setHeaderText("Supprimer l'événement");
                confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer définitivement l'événement \"" + event.getTitle() + "\" ? Cette action est irréversible.");

                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    eventDAO.deleteEvent(event.getId());
                    loadEvents();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Suppression réussie");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("L'événement a été supprimé avec succès.");
                    successAlert.showAndWait();
                }
            });
            
            actions.getChildren().addAll(restoreButton, deleteButton);
        }
        
        // Ajouter un séparateur
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        // Assembler le contenu
        content.getChildren().addAll(titleLabel, details, spacer, actions);
        
        // Assembler la carte
        StackPane imageContainer = new StackPane();
        imageContainer.getChildren().addAll(imageWrapper, statusBadge);
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(10, 10, 0, 0));
        
        card.getChildren().addAll(categoryBadge, imageContainer, content);
        
        return card;
    }

    private Button createButton(String tooltip, String iconPath) {
        Button button = new Button();
        try {
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(16);
            imageView.setFitWidth(16);
            button.setGraphic(imageView);
            button.setTooltip(new Tooltip(tooltip));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'icône " + iconPath + ": " + e.getMessage());
            button.setText(tooltip);
        }
        return button;
    }

    private void loadCategories() {
        // Charger les catégories dynamiquement
        List<String> categories = eventDAO.getAllCategories();
        searchCategory.getItems().addAll(categories);
        searchCategoryArchives.getItems().addAll(categories);
    }

    private void loadEvents() {
        // Charger les événements actifs
        eventsContainer.getChildren().clear();
        List<Event> actifs = eventDAO.getAllActifs();
        for (Event event : actifs) {
            eventsContainer.getChildren().add(createEventCard(event, false));
        }
        
        // Charger les événements archivés
        archivedEventsContainer.getChildren().clear();
        List<Event> archives = eventDAO.getAllArchives();
        for (Event event : archives) {
            archivedEventsContainer.getChildren().add(createEventCard(event, true));
        }
    }

    private ImageView loadImageView(String fileName) {
        if (fileName == null) return new ImageView();
        File file = new File(IMAGE_DIR + fileName);
        if (!file.exists()) return new ImageView();
        ImageView imageView = new ImageView(new Image(file.toURI().toString()));
        imageView.setFitHeight(180);
        imageView.setFitWidth(320);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    @FXML
    private void handleAddEvent(ActionEvent event) {
        try {
            Parent formPage = FXMLLoader.load(getClass().getResource("/views/event_form.fxml"));
            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) ((Node) event.getSource()).getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(formPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditEvent(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event_form.fxml"));
            Parent formPage = loader.load();
            
            // Configurer le contrôleur du formulaire
            EventFormController formController = loader.getController();
            formController.setEvent(event);
            formController.setOnFormSubmitted(v -> loadEvents());

            // Obtenir la référence au BorderPane principal et charger le formulaire
            BorderPane mainContent = (BorderPane) eventsContainer.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(formPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        filterActiveEvents();
    }

    @FXML
    private void handleSearchArchives() {
        filterArchivedEvents();
    }

    @FXML
    private void handleReset() {
        searchTitle.clear();
        searchDate.setValue(null);
        searchCategory.setValue(null);
        loadEvents();
    }

    @FXML
    private void handleResetArchives() {
        searchTitleArchives.clear();
        searchDateArchives.setValue(null);
        searchCategoryArchives.setValue(null);
        loadEvents();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}