package controller;

import entity.Reclamation;
import entity.TypeReclamation;
import entity.Medecin;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import services.ReclamationServices;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class EditReclamationController {
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> medecinComboBox;
    @FXML private ImageView photoView;
    @FXML private Button browseButton;
    @FXML private Button generateDescriptionButton; // Assurez-vous qu'il existe dans le FXML
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Reclamation reclamationToEdit;
    private MainController mainController;
    private final ReclamationServices service = new ReclamationServices();

    public void setReclamationToEdit(Reclamation reclamation) {
        this.reclamationToEdit = reclamation;
        populateFields();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        try {
            // Initialiser les ComboBox
            initializeTypeComboBox();
            initializeMedecinComboBox();

            // Configurer les boutons
            if (generateDescriptionButton != null) {
                generateDescriptionButton.setOnAction(e -> generateDescription());
            }

            saveButton.setOnAction(e -> handleSave());
            cancelButton.setOnAction(e -> handleCancel());

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage());
        }
    }

    private void populateFields() {
        if (reclamationToEdit != null) {
            try {
                // Trouver le type correspondant dans la ComboBox
                TypeReclamation type = service.getTypeById(Integer.parseInt(reclamationToEdit.getTypeReclamation()));
                if (type != null) {
                    typeComboBox.getSelectionModel().select(type.getId() + " - " + type.getNom());
                }

                // Trouver le médecin correspondant dans la ComboBox
                Medecin medecin = service.getMedecinById(Integer.parseInt(reclamationToEdit.getMedecin()));
                if (medecin != null) {
                    medecinComboBox.getSelectionModel().select(medecin.getId() + " - " + medecin.getNom());
                }

                descriptionField.setText(reclamationToEdit.getDescription());
                datePicker.setValue(reclamationToEdit.getDateReclamation());

                // Charger la photo si elle existe
                if (reclamationToEdit.getPhotoPath() != null && !reclamationToEdit.getPhotoPath().isEmpty()) {
                    // Implémentez la logique pour charger l'image
                }

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur de chargement des données: " + e.getMessage());
            }
        }
    }

    private void initializeTypeComboBox() throws SQLException {
        List<TypeReclamation> typesList = service.getAllTypes();
        List<String> types = typesList.stream()
                .map(t -> t.getId() + " - " + t.getNom())
                .collect(Collectors.toList());
        typeComboBox.getItems().addAll(types);
    }

    private void initializeMedecinComboBox() throws SQLException {
        List<Medecin> medecinsList = service.getAllMedecins();
        List<String> medecins = medecinsList.stream()
                .map(m -> m.getId() + " - " + m.getNom())
                .collect(Collectors.toList());
        medecinComboBox.getItems().addAll(medecins);
    }

    @FXML
    private void generateDescription() {
        // Implémentez la génération de description si nécessaire
    }

    @FXML
    private void handleSave() {
        try {
            // Valider les champs
            if (typeComboBox.getValue() == null || medecinComboBox.getValue() == null ||
                    descriptionField.getText().isEmpty() || datePicker.getValue() == null) {
                showAlert("Erreur", "Veuillez remplir tous les champs");
                return;
            }

            // Mettre à jour l'objet reclamationToEdit
            String[] typeParts = typeComboBox.getValue().split(" - ");
            String[] medecinParts = medecinComboBox.getValue().split(" - ");

            reclamationToEdit.setTypeReclamation(typeParts[0]);
            reclamationToEdit.setMedecin(medecinParts[0]);
            reclamationToEdit.setDescription(descriptionField.getText());
            reclamationToEdit.setDateReclamation(datePicker.getValue());

            // Enregistrer les modifications
            service.updateReclamation(reclamationToEdit);

            // Rafraîchir le tableau principal
            if (mainController != null) {
                mainController.refreshTable();
            }

            // Fermer la fenêtre
            ((Stage) saveButton.getScene().getWindow()).close();

        } catch (SQLException e) {
            showAlert("Erreur", "Échec de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}