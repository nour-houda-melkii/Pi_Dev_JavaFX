package controller;

import entity.Reclamation;
import entity.Medecin;
import entity.TypeReclamation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ReclamationServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;



public class AddReclamationController {
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> medecinComboBox;
    @FXML private ImageView photoView;
    @FXML private Button browseButton;
    @FXML private Button generateDescriptionButton;

    private File selectedFile;
    private final ReclamationServices reclamationService = new ReclamationServices();
    private final ReclamationServices service = new ReclamationServices();


    @FXML
    public void initialize() {
        try {
            // Charger les types
            List<TypeReclamation> typesList = service.getAllTypes();
            ObservableList<String> types = FXCollections.observableArrayList();
            for (TypeReclamation type : typesList) {
                types.add(type.getId() + " - " + type.getNom());
            }
            typeComboBox.setItems(types);

            // Charger les médecins
            List<Medecin> medecinsList = service.getAllMedecins();
            ObservableList<String> medecins = FXCollections.observableArrayList();
            for (Medecin medecin : medecinsList) {
                medecins.add(medecin.getId() + " - " + medecin.getNom());
            }
            medecinComboBox.setItems(medecins);

            // Sélectionner le premier élément par défaut si disponible
            if (!typeComboBox.getItems().isEmpty()) {
                typeComboBox.getSelectionModel().selectFirst();
            }
            if (!medecinComboBox.getItems().isEmpty()) {
                medecinComboBox.getSelectionModel().selectFirst();
            }

            // Définir la date par défaut à aujourd'hui
            datePicker.setValue(LocalDate.now());

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void initializeTypeComboBox() {
        try {
            List<TypeReclamation> typesList = reclamationService.getAllTypes();
            List<String> types = typesList.stream()
                    .map(t -> t.getId() + " - " + t.getNom())
                    .collect(Collectors.toList());
            typeComboBox.setItems(FXCollections.observableArrayList(types));
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement des types: " + e.getMessage());
        }
    }
    private MainController mainController;
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void initializeMedecinComboBox() {
        try {
            List<Medecin> medecinsList = reclamationService.getAllMedecins();
            List<String> medecins = medecinsList.stream()
                    .map(m -> m.getId() + " - " + m.getNom())
                    .collect(Collectors.toList());
            medecinComboBox.setItems(FXCollections.observableArrayList(medecins));
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement des médecins: " + e.getMessage());
        }
    }

    @FXML
    private void generateDescription() {
        String selectedType = typeComboBox.getValue();
        if (selectedType != null && !selectedType.isEmpty()) {
            // Ici vous pourriez implémenter une génération automatique de description
            // basée sur le type sélectionné, comme dans votre AddCategoryController
            String typeName = selectedType.split(" - ")[1];
            descriptionField.setText("Description générée pour: " + typeName);
        }
    }

    @FXML
    private void handleAdd() {
        try {
            // Vérifier qu'un type est sélectionné
            if (typeComboBox.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un type de réclamation");
                return;
            }

            // Extraire l'ID correctement
            String selected = typeComboBox.getValue();
            String[] parts = selected.split(" - ");

            if (parts.length < 2) {
                showAlert("Erreur", "Format de type invalide");
                return;
            }

            String typeId = parts[0]; // Contiendra 6, 7, 8 ou 9 selon votre BD
            System.out.println("Type ID sélectionné: " + typeId); // Debug

            // Créer la réclamation avec le bon ID
            Reclamation nouvelleReclamation = new Reclamation(
                    "0", // ID temporaire
                    typeId,
                    descriptionField.getText(),
                    datePicker.getValue(),
                    medecinComboBox.getValue().split(" - ")[0],
                    selectedFile != null ? selectedFile.getAbsolutePath() : ""
            );

            // Ajouter à la base
            service.addReclamation(nouvelleReclamation);
            showAlert("Succès", "Réclamation ajoutée avec succès!");
            clearFields();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void addReclamation() {
        // Validation des champs
        if (typeComboBox.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un type de réclamation");
            return;
        }

        if (medecinComboBox.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un médecin");
            return;
        }

        if (descriptionField.getText().trim().isEmpty()) {
            showAlert("Erreur", "La description ne peut pas être vide");
            return;
        }

        if (datePicker.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner une date");
            return;
        }

        try {
            // Extraire les IDs
            String typeId = typeComboBox.getValue().split(" - ")[0];
            String medecinId = medecinComboBox.getValue().split(" - ")[0];

            // Créer la réclamation
            Reclamation nouvelleReclamation = new Reclamation(
                    "0", // ID temporaire
                    typeId,
                    descriptionField.getText(),
                    datePicker.getValue(),
                    medecinId,
                    selectedFile != null ? selectedFile.getAbsolutePath() : ""
            );

            // Debug: Afficher les valeurs avant l'ajout
            System.out.println("Ajout réclamation avec:");
            System.out.println("Type ID: " + typeId);
            System.out.println("Médecin ID: " + medecinId);
            System.out.println("Description: " + descriptionField.getText());
            System.out.println("Date: " + datePicker.getValue());
            System.out.println("Photo: " + (selectedFile != null ? selectedFile.getAbsolutePath() : "null"));

            // Ajouter à la base
            service.addReclamation(nouvelleReclamation);

            // Message de succès
            showSuccessAlert("Succès", "Réclamation ajoutée avec succès!");

            // Fermer la fenêtre si mainController est défini
            if (mainController != null) {
                mainController.refreshTable();
                ((Stage) typeComboBox.getScene().getWindow()).close();
            }

        } catch (SQLException e) {
            showAlert("Erreur SQL", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void browsePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            photoView.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void clearFields() {
        typeComboBox.getSelectionModel().clearSelection();
        descriptionField.clear();
        datePicker.setValue(null);
        medecinComboBox.getSelectionModel().clearSelection();
        photoView.setImage(null);
        selectedFile = null;
    }

    @FXML
    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main-view.fxml"));
            Stage stage = (Stage) typeComboBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de bord");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger le tableau de bord: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}