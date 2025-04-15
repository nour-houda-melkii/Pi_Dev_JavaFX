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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private TableView<Event> tableActifs;
    @FXML private TableColumn<Event, Integer> colId;
    @FXML private TableColumn<Event, String> colTitre;
    @FXML private TableColumn<Event, String> colLieu;
    @FXML private TableColumn<Event, String> colDebut;
    @FXML private TableColumn<Event, String> colFin;
    @FXML private TableColumn<Event, Integer> colPlaces;
    @FXML private TableColumn<Event, String> colCategorie;
    @FXML private TableColumn<Event, ImageView> colAffiche;
    @FXML private TableColumn<Event, Void> colActionsActifs;

    @FXML private TableView<Event> tableArchives;
    @FXML private TableColumn<Event, Integer> colIdArch;
    @FXML private TableColumn<Event, String> colTitreArch;
    @FXML private TableColumn<Event, String> colLieuArch;
    @FXML private TableColumn<Event, String> colDebutArch;
    @FXML private TableColumn<Event, String> colFinArch;
    @FXML private TableColumn<Event, Integer> colPlacesArch;
    @FXML private TableColumn<Event, String> colCategorieArch;
    @FXML private TableColumn<Event, ImageView> colAfficheArch;
    @FXML private TableColumn<Event, Void> colActionsArchives;

    @FXML private TextField searchFieldArchives;

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
        setupColumns();
        loadCategories();
        loadEvents();
    }

    private void setupColumns() {
        // Configuration des colonnes pour les événements actifs
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("location"));
        colPlaces.setCellValueFactory(new PropertyValueFactory<>("placesDisponibles"));
        colDebut.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colFin.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colCategorie.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategorie().getNom()));
        colAffiche.setCellValueFactory(cell -> new SimpleObjectProperty<>(
                loadImageView(cell.getValue().getAffiche())));

        // Configuration des colonnes pour les événements archivés
        colIdArch.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitreArch.setCellValueFactory(new PropertyValueFactory<>("title"));
        colLieuArch.setCellValueFactory(new PropertyValueFactory<>("location"));
        colPlacesArch.setCellValueFactory(new PropertyValueFactory<>("placesDisponibles"));
        colDebutArch.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colFinArch.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colCategorieArch.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategorie().getNom()));
        colAfficheArch.setCellValueFactory(cell -> new SimpleObjectProperty<>(
                loadImageView(cell.getValue().getAffiche())));

        setupPlacesColumn(colPlaces);
        setupPlacesColumn(colPlacesArch);
        setupActionsColumn();
        setupArchiveActionsColumn();
    }

    private void setupPlacesColumn(TableColumn<Event, Integer> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item == 0 ? "Complet" : item.toString());
                    badge.getStyleClass().add("badge");
                    if (item == 0) {
                        badge.getStyleClass().add("danger");
                    } else if (item < 10) {
                        badge.getStyleClass().add("warning");
                    } else {
                        badge.getStyleClass().add("success");
                    }
                    setGraphic(badge);
                }
            }
        });
    }

    private void setupActionsColumn() {
        colActionsActifs.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(8);
            private final Button editButton = createButton("Modifier", "/images/icons/edit.png");
            private final Button archiveButton = createButton("Archiver", "/images/icons/archive.png");

            {
                editButton.getStyleClass().addAll("action-button", "edit-button");
                archiveButton.getStyleClass().addAll("action-button", "archive-button");

                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.getChildren().addAll(editButton, archiveButton);

                editButton.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    handleEditEvent(event);
                });

                archiveButton.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
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
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupArchiveActionsColumn() {
        colActionsArchives.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(8);
            private final Button restoreButton = createButton("Restaurer", "/images/icons/restore.png");
            private final Button deleteButton = createButton("Supprimer", "/images/icons/delete.png");

            {
                restoreButton.getStyleClass().addAll("action-button", "restore-button");
                deleteButton.getStyleClass().addAll("action-button", "delete-button");

                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.getChildren().addAll(restoreButton, deleteButton);

                restoreButton.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
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
                    Event event = getTableView().getItems().get(getIndex());
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
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
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
        List<Event> actifs = eventDAO.getAllActifs();
        FilteredList<Event> filteredActifs = new FilteredList<>(FXCollections.observableArrayList(actifs), p -> true);

        searchTitle.textProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredActifs));
        searchDate.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredActifs));
        searchCategory.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredActifs));

        SortedList<Event> sortedActifs = new SortedList<>(filteredActifs);
        sortedActifs.comparatorProperty().bind(tableActifs.comparatorProperty());
        tableActifs.setItems(sortedActifs);

        List<Event> archives = eventDAO.getAllArchives();
        FilteredList<Event> filteredArchives = new FilteredList<>(FXCollections.observableArrayList(archives), p -> true);

        searchTitleArchives.textProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredArchives));
        searchDateArchives.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredArchives));
        searchCategoryArchives.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters(filteredArchives));

        SortedList<Event> sortedArchives = new SortedList<>(filteredArchives);
        sortedArchives.comparatorProperty().bind(tableArchives.comparatorProperty());
        tableArchives.setItems(sortedArchives);
    }

    private void applyFilters(FilteredList<Event> filteredData) {
        filteredData.setPredicate(event -> {
            boolean matchesTitle = (searchTitle.getText() == null || searchTitle.getText().isEmpty() || event.getTitle().toLowerCase().contains(searchTitle.getText().toLowerCase())) &&
                                   (searchTitleArchives.getText() == null || searchTitleArchives.getText().isEmpty() || event.getTitle().toLowerCase().contains(searchTitleArchives.getText().toLowerCase()));

            boolean matchesDate = (searchDate.getValue() == null || event.getStartDate().toLocalDate().equals(searchDate.getValue())) &&
                                  (searchDateArchives.getValue() == null || event.getStartDate().toLocalDate().equals(searchDateArchives.getValue()));

            boolean matchesCategory = (searchCategory.getValue() == null || searchCategory.getValue().equals("Toutes les catégories") || event.getCategorie().getNom().equals(searchCategory.getValue())) &&
                                      (searchCategoryArchives.getValue() == null || searchCategoryArchives.getValue().equals("Toutes les catégories") || event.getCategorie().getNom().equals(searchCategoryArchives.getValue()));

            return matchesTitle && matchesDate && matchesCategory;
        });
    }

    private ImageView loadImageView(String fileName) {
        if (fileName == null) return new ImageView();
        File file = new File(IMAGE_DIR + fileName);
        if (!file.exists()) return new ImageView();
        ImageView imageView = new ImageView(new Image(file.toURI().toString()));
        imageView.setFitHeight(60);
        imageView.setFitWidth(60);
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
            BorderPane mainContent = (BorderPane) tableActifs.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(formPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        // This method is triggered by the search fields
    }

    @FXML
    private void handleSearchArchives() {
        // This method is triggered by the search field for archives
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
}