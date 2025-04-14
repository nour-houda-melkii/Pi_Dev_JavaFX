package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
        
        addActionsToTable();
    }

    private void loadCategories() {
        List<CategorieEvent> categories = categorieDAO.getAll();
        tableCategories.setItems(FXCollections.observableArrayList(categories));
    }

    private void addActionsToTable() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnArchive = new Button("📦");

            {
                btnEdit.getStyleClass().addAll("button", "edit");
                btnArchive.getStyleClass().addAll("button", "archive");

                btnEdit.setOnAction(e -> handleEditCategorie(getTableView().getItems().get(getIndex())));
                btnArchive.setOnAction(e -> {
                    CategorieEvent categorie = getTableView().getItems().get(getIndex());
                    categorie.setArchived(true);
                    categorieDAO.update(categorie);
                    loadCategories();
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

    @FXML
    private void handleAddCategorie(ActionEvent event) {
        openCategorieForm(null);
    }

    private void handleEditCategorie(CategorieEvent categorie) {
        openCategorieForm(categorie);
    }

    private void openCategorieForm(CategorieEvent categorieToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/categorie_form.fxml"));
            AnchorPane formPane = loader.load();

            CategorieEventFormController formController = loader.getController();
            formController.setCategorie(categorieToEdit);
            formController.setOnFormSubmitted(v -> loadCategories());

            Stage dialogStage = new Stage();
            dialogStage.setTitle(categorieToEdit == null ? "Ajouter une catégorie" : "Modifier la catégorie");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setScene(new Scene(formPane));
            dialogStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}