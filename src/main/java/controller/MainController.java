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
    @FXML private Button backendButton;

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
                controller.setReclamationToEdit(selected);
                controller.setMainController(this);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Réclamation");
                stage.show();

            } catch (IOException e) {
                showAlert("Erreur", "Impossible d'ouvrir la fenêtre d'édition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBackend(javafx.event.ActionEvent event) {
        try {
            System.out.println("Tentative de chargement de l'interface admin...");
            // Chemin absolu vérifié
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/backend-view.fxml"));
            Parent root = loader.load();

            Stage adminStage = new Stage();
            adminStage.setTitle("Administration SAHATECK");
            adminStage.setScene(new Scene(root, 800, 600));

            // Empêche l'interaction avec la fenêtre parente
            adminStage.initModality(Modality.WINDOW_MODAL);
            adminStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            adminStage.show();

        } catch (Exception e) {
            showErrorAlert("ERREUR",
                    "Échec du chargement",
                    "Détails : " + e.getMessage() +
                            "\nVérifiez que le fichier existe dans view/backend-view.fxml");
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


    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }


    @FXML
    private void navigateToDashboard() {
        try {
            // Chargez votre vue dashboard si vous en avez une
            // Ou simplement fermez la fenêtre actuelle si c'est la page principale
            Stage stage = (Stage) reclamationTable.getScene().getWindow();
            stage.close();

            // Si vous avez un dashboard.fxml:
        /*
        Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        */

        } catch (Exception e) {
            showAlert("Erreur", "Impossible de revenir au tableau de bord: " + e.getMessage());
        }
    }

}