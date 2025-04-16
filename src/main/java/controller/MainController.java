package controller;

import entity.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.ReclamationServices;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;

public class MainController {
    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> idColumn;
    @FXML private TableColumn<Reclamation, String> typeColumn;
    @FXML private TableColumn<Reclamation, String> descriptionColumn;
    @FXML private TableColumn<Reclamation, LocalDate> dateColumn;
    @FXML private TableColumn<Reclamation, String> medecinColumn;
    @FXML private TableColumn<Reclamation, String> photoColumn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final ReclamationServices service = new ReclamationServices();
    private final ObservableList<Reclamation> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadData();
        setupSelectionListener();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeReclamation"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateReclamation"));
        medecinColumn.setCellValueFactory(new PropertyValueFactory<>("medecin"));
        photoColumn.setCellValueFactory(new PropertyValueFactory<>("photoPath"));
    }

    public void loadData() {
        try {
            data.setAll(service.getAllReclamations());
            reclamationTable.setItems(data);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void setupSelectionListener() {
        reclamationTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean itemSelected = newSelection != null;
                    editBtn.setDisable(!itemSelected);
                    deleteBtn.setDisable(!itemSelected);
                });
    }

    @FXML
    private void handleBackend(ActionEvent event) {
        try {
            // Charge le fichier FXML
            URL fxmlUrl = getClass().getResource("/view/backend-view.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Fichier backend-view.fxml introuvable");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Crée la nouvelle scène
            Stage backendStage = new Stage();
            backendStage.setTitle("Administration SAHATECK");
            backendStage.setScene(new Scene(root, 1000, 700));

            // Configure comme fenêtre modale
            backendStage.initModality(Modality.WINDOW_MODAL);
            backendStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            backendStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'interface d'administration: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            // Charger la vue d'ajout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add-reclamation.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle scène
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une réclamation");

            // Obtenir le contrôleur et définir une référence à ce contrôleur principal
            AddReclamationController addController = loader.getController();
            addController.setMainController(this);

            stage.showAndWait(); // Attend que la fenêtre soit fermée

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre d'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/edit-reclamation.fxml"));
                Parent root = loader.load();

                EditReclamationController controller = loader.getController();
                if (controller == null) {
                    showAlert("Erreur", "Impossible de charger le contrôleur d'édition");
                    return;
                }

                controller.setReclamationToEdit(selected);
                controller.setMainController(this);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Réclamation");
                stage.show();

            } catch (IOException e) {
                showAlert("Erreur", "Impossible d'ouvrir la fenêtre d'édition: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une réclamation à modifier");
        }
    }

    @FXML
    private void handleDelete() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                service.deleteReclamation(Integer.parseInt(selected.getId()));
                loadData();
                showAlert("Succès", "Réclamation supprimée!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de suppression: " + e.getMessage());
            }
        }
    }

    public void refreshTable() {
        loadData();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void handleBackend(javafx.event.ActionEvent event) {
        try {
            // Charge le fichier FXML
            URL fxmlUrl = getClass().getResource("/view/backend-view.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Fichier backend-view.fxml introuvable");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Crée la nouvelle scène
            Stage backendStage = new Stage();
            backendStage.setTitle("Administration SAHATECK");
            backendStage.setScene(new Scene(root, 1000, 700));

            // Configure comme fenêtre modale
            backendStage.initModality(Modality.WINDOW_MODAL);
            backendStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            backendStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'interface d'administration: " + e.getMessage());
        }
    }

}