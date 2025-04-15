package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import models.CategorieEvent;
import services.CategorieEventDAO;

import java.io.IOException;
import java.util.List;

public class CategorieEventController {

    @FXML private TableView<CategorieEvent> tableCategories;
    @FXML private TableColumn<CategorieEvent, Integer> colId;
    @FXML private TableColumn<CategorieEvent, String> colNom;
    @FXML private TableColumn<CategorieEvent, String> colDescription;
    @FXML private TableColumn<CategorieEvent, Void> colActions;

    private final CategorieEventDAO categorieDAO = new CategorieEventDAO();

    @FXML
    public void initialize() {
        setupColumns();
        loadCategories();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        setupActionsColumn();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(8);
            private final Button editButton = createButton("Modifier", "/images/icons/edit.png");
            private final Button deleteButton = createButton("Supprimer", "/images/icons/delete.png");

            {
                editButton.getStyleClass().addAll("action-button", "edit-button");
                deleteButton.getStyleClass().addAll("action-button", "delete-button");

                container.setAlignment(javafx.geometry.Pos.CENTER);
                container.getChildren().addAll(editButton, deleteButton);

                editButton.setOnAction(e -> {
                    CategorieEvent categorie = getTableView().getItems().get(getIndex());
                    handleEditCategorie(categorie);
                });

                deleteButton.setOnAction(e -> {
                    CategorieEvent categorie = getTableView().getItems().get(getIndex());
                    handleDeleteCategorie(categorie);
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
            javafx.scene.image.Image image = new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath));
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
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
        List<CategorieEvent> categories = categorieDAO.getAll();
        tableCategories.setItems(FXCollections.observableArrayList(categories));
    }

    @FXML
    private void handleAddCategorie(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/categorieEvent_form.fxml"));
            Parent formPage = loader.load();
            
            CategorieEventFormController formController = loader.getController();
            formController.setOnFormSubmitted(v -> loadCategories());

            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) tableCategories.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(formPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditCategorie(CategorieEvent categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/categorieEvent_form.fxml"));
            Parent formPage = loader.load();
            
            CategorieEventFormController formController = loader.getController();
            formController.setCategorie(categorie);
            formController.setOnFormSubmitted(v -> loadCategories());

            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) tableCategories.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(formPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteCategorie(CategorieEvent categorie) {
        // Vérifier d'abord si la catégorie contient des événements
        if (categorieDAO.hasEvents(categorie.getId())) {
            Alert errorAlert = new Alert(Alert.AlertType.WARNING);
            errorAlert.setTitle("Impossible de supprimer");
            errorAlert.setHeaderText("Cette catégorie ne peut pas être supprimée");
            errorAlert.setContentText("Cette catégorie contient des événements. Veuillez d'abord supprimer ou modifier les événements associés avant de supprimer la catégorie.");
            errorAlert.showAndWait();
            return;
        }

        // Si pas d'événements, demander confirmation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer la catégorie");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette catégorie ?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                categorieDAO.delete(categorie.getId());
                loadCategories();
                
                // Afficher une alerte de succès
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Suppression réussie");
                successAlert.setHeaderText(null);
                successAlert.setContentText("La catégorie a été supprimée avec succès.");
                successAlert.showAndWait();
            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Une erreur est survenue");
                errorAlert.setContentText("Impossible de supprimer la catégorie : " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
}