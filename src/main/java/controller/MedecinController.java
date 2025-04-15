package controller;

import entity.Medecin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.MedecinService;

import java.sql.SQLException;

public class MedecinController {
    @FXML private TableView<Medecin> medecinTable;
    @FXML private TableColumn<Medecin, Integer> idColumn;
    @FXML private TableColumn<Medecin, String> nomColumn;
    @FXML private TextField nomField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    private final MedecinService service = new MedecinService();
    private final ObservableList<Medecin> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));

        // Chargement des données
        loadData();

        // Désactiver les boutons update et delete initialement
        updateButton.setDisable(true);
        deleteButton.setDisable(true);

        // Gestion de la sélection dans le tableau
        medecinTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean itemSelected = newSelection != null;
                    updateButton.setDisable(!itemSelected);
                    deleteButton.setDisable(!itemSelected);

                    if (itemSelected) {
                        fillForm(newSelection);
                    }
                });
    }

    private void loadData() {
        try {
            data.setAll(service.getAllMedecins());
            medecinTable.setItems(data);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void fillForm(Medecin medecin) {
        nomField.setText(medecin.getNom());
    }

    @FXML
    private void handleAdd() {
        if (validateForm()) {
            Medecin newMedecin = new Medecin(0, nomField.getText());

            try {
                service.addMedecin(newMedecin);
                loadData();
                clearForm();
                showAlert("Succès", "Médecin ajouté avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec d'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        Medecin selected = medecinTable.getSelectionModel().getSelectedItem();
        if (selected != null && validateForm()) {
            selected.setNom(nomField.getText());

            try {
                service.updateMedecin(selected);
                loadData();
                showAlert("Succès", "Médecin mis à jour!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de mise à jour: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Medecin selected = medecinTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                service.deleteMedecin(selected.getId());
                loadData();
                clearForm();
                showAlert("Succès", "Médecin supprimé!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de suppression: " + e.getMessage());
            }
        }
    }

    private boolean validateForm() {
        if (nomField.getText().isEmpty()) {
            showAlert("Erreur", "Le nom est obligatoire");
            return false;
        }
        return true;
    }

    private void clearForm() {
        nomField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}