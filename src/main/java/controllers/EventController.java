package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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

        // Badge coloré pour places
        colPlaces.setCellFactory(col -> new TableCell<>() {
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

        colPlacesArch.setCellFactory(col -> new TableCell<>() {
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

        addActionsToActifs();
        addActionsToArchives();
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

    private void addActionsToActifs() {
        colActionsActifs.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnArchive = new Button("📦");

            {
                btnEdit.getStyleClass().addAll("button", "edit");
                btnArchive.getStyleClass().addAll("button", "archive");

                btnEdit.setOnAction(e -> handleEditEvent(getTableView().getItems().get(getIndex())));
                btnArchive.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    event.setArchived(true);
                    eventDAO.updateEventArchiveStatus(event.getId(), true);
                    loadEvents();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8, btnEdit, btnArchive);
                    setGraphic(hbox);
                }
            }
        });
    }


    private void addActionsToArchives() {
        colActionsArchives.setCellFactory(col -> new TableCell<>() {
            private final Button btnRestore = new Button("🔁");
            private final Button btnDelete = new Button("🗑️");

            {
                btnRestore.getStyleClass().addAll("button", "restore");
                btnDelete.getStyleClass().addAll("button", "delete");

                btnRestore.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    event.setArchived(false);
                    eventDAO.updateEventArchiveStatus(event.getId(), false);
                    loadEvents();
                });

                btnDelete.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    eventDAO.deleteEvent(event.getId());
                    loadEvents();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(5, btnRestore, btnDelete);
                    setGraphic(vbox);
                }
            }
        });
    }


    @FXML
    private void handleAddEvent(ActionEvent event) {
        openEventForm(null);
    }

    private void handleEditEvent(Event event) {
        openEventForm(event);
    }

    private void openEventForm(Event eventToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event_form.fxml"));
            AnchorPane formPane = loader.load();

            EventFormController formController = loader.getController();
            formController.setEvent(eventToEdit);
            formController.setOnFormSubmitted(v -> loadEvents());

            Stage dialogStage = new Stage();
            dialogStage.setTitle(eventToEdit == null ? "Ajouter un événement" : "Modifier l'événement");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(formPane));
            dialogStage.show();

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