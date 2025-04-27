package controller;

import entity.Reclamation;
import entity.TypeReclamation;
import entity.Medecin;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ReclamationServices;

import javafx.event.ActionEvent;
import java.io.File;
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
    @FXML private Button generateDescriptionButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Reclamation reclamationToEdit;
    private MainController mainController;
    private final ReclamationServices service = new ReclamationServices();
    private String photoPath;

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
            initializeTypeComboBox();
            initializeMedecinComboBox();
            setupButtons();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void setupButtons() {
        browseButton.setOnAction(e -> handleBrowse());

        if (generateDescriptionButton != null) {
            generateDescriptionButton.setOnAction(e -> generateDescription());
        }

        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> handleCancel());
    }

    private void populateFields() {
        if (reclamationToEdit != null) {
            try {
                // Sélectionner le type dans la ComboBox
                TypeReclamation type = service.getTypeById(reclamationToEdit.getTypeReclamationId());
                if (type != null) {
                    typeComboBox.getSelectionModel().select(type.getId() + " - " + type.getNom());
                }

                // Sélectionner le médecin dans la ComboBox
                Medecin medecin = service.getMedecinById(reclamationToEdit.getMedecinId());
                if (medecin != null) {
                    medecinComboBox.getSelectionModel().select(medecin.getId() + " - " + medecin.getNom());
                }

                descriptionField.setText(reclamationToEdit.getDescription());
                datePicker.setValue(reclamationToEdit.getDateReclamation());

                // Charger la photo si elle existe
                if (reclamationToEdit.getPhotoPath() != null && !reclamationToEdit.getPhotoPath().isEmpty()) {
                    loadImage(reclamationToEdit.getPhotoPath());
                }

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur de chargement des données: " + e.getMessage());
            }
        }
    }


    private void loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                photoView.setImage(image);
                this.photoPath = path;
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger l'image: " + e.getMessage());
        }
    }

    private void initializeTypeComboBox() throws SQLException {
        typeComboBox.getItems().clear();
        List<TypeReclamation> types = service.getAllTypes();
        typeComboBox.getItems().addAll(
                types.stream()
                        .map(t -> t.getId() + " - " + t.getNom())
                        .collect(Collectors.toList())
        );
    }

    private void initializeMedecinComboBox() throws SQLException {
        medecinComboBox.getItems().clear();
        List<Medecin> medecins = service.getAllMedecins();
        medecinComboBox.getItems().addAll(
                medecins.stream()
                        .map(m -> m.getId() + " - " + m.getNom())
                        .collect(Collectors.toList())
        );
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            loadImage(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void generateDescription() {
        // Implémentez la génération automatique de description si nécessaire
        // descriptionField.setText(generateAutoDescription());
    }

    @FXML
    private void handleSave() {
        try {
            if (!validateFields()) {
                return;
            }

            updateReclamationFromFields();

            boolean success = service.updateReclamation(reclamationToEdit);
            if (success) {
                mainController.refreshReclamations(); // Rafraîchir la liste
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de la mise à jour");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Échec de la mise à jour: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        if (typeComboBox.getValue() == null || medecinComboBox.getValue() == null ||
                descriptionField.getText().isEmpty() || datePicker.getValue() == null) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
            return false;
        }
        return true;
    }

    private void updateReclamationFromFields() {
        String[] typeParts = typeComboBox.getValue().split(" - ");
        String[] medecinParts = medecinComboBox.getValue().split(" - ");

        // Conversion en int avant l'affectation
        reclamationToEdit.setTypeReclamationId(Integer.parseInt(typeParts[0]));
        reclamationToEdit.setMedecinId(Integer.parseInt(medecinParts[0]));
        reclamationToEdit.setDescription(descriptionField.getText());
        reclamationToEdit.setDateReclamation(datePicker.getValue());

        if (photoPath != null) {
            reclamationToEdit.setPhotoPath(photoPath);
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // Fermer la fenêtre actuelle
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }



}