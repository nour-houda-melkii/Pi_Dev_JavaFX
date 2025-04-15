package controller;

import entity.TypeReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.TypeReclamationService;

import java.sql.SQLException;

public class TypeReclamationController {
    @FXML private TableView<TypeReclamation> typeTable;
    @FXML private TableColumn<TypeReclamation, Integer> idColumn;
    @FXML private TableColumn<TypeReclamation, String> nomColumn;
    @FXML private TextField nomField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    private final TypeReclamationService service = new TypeReclamationService();
    private final ObservableList<TypeReclamation> data = FXCollections.observableArrayList();

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
        typeTable.getSelectionModel().selectedItemProperty().addListener(
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
            data.setAll(service.getAllTypes());
            typeTable.setItems(data);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void fillForm(TypeReclamation type) {
        nomField.setText(type.getNom());
    }

    @FXML
    private void handleAdd() {
        if (validateForm()) {
            TypeReclamation newType = new TypeReclamation(0, nomField.getText());

            try {
                service.addType(newType);
                loadData();
                clearForm();
                showAlert("Succès", "Type ajouté avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec d'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        TypeReclamation selected = typeTable.getSelectionModel().getSelectedItem();
        if (selected != null && validateForm()) {
            selected.setNom(nomField.getText());

            try {
                service.updateType(selected);
                loadData();
                showAlert("Succès", "Type mis à jour!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de mise à jour: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        TypeReclamation selected = typeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                service.deleteType(selected.getId());
                loadData();
                clearForm();
                showAlert("Succès", "Type supprimé!");
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