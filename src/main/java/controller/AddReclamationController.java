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
import javafx.collections.ObservableList;

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
    private MainController mainController;

    @FXML
    public void initialize() {
        try {
            initializeComboBoxes(); // Initialise les ComboBox
            datePicker.setValue(LocalDate.now()); // Date par défaut = aujourd'hui

            // Configuration des écouteurs d'événements
            browseButton.setOnAction(event -> browsePhoto());
            generateDescriptionButton.setOnAction(event -> generateDescription());

        } catch (Exception e) {
            showAlert("Erreur d'initialisation",
                    "Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void initializeMedecinComboBox() {
        try {
            List<Medecin> medecinsList = reclamationService.getAllMedecins();
            ObservableList<String> medecins = FXCollections.observableArrayList(
                    medecinsList.stream()
                            .map(m -> m.getId() + " - " + m.getNom())
                            .collect(Collectors.toList())
            );
            medecinComboBox.setItems(medecins);

            if (!medecins.isEmpty()) {
                medecinComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showAlert("Error", "Error loading doctors: " + e.getMessage());
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void generateDescription() {
        String selectedType = typeComboBox.getValue();
        if (selectedType != null && !selectedType.isEmpty()) {
            String typeName = selectedType.split(" - ")[1];
            descriptionField.setText("Default description for: " + typeName);
        }
    }

    @FXML
    private void handleAdd() {
        addReclamation();
    }

    @FXML
    private void addReclamation() {
        // Field validation
        if (typeComboBox.getValue() == null) {
            showAlert("Error", "Please select a claim type");
            return;
        }

        if (medecinComboBox.getValue() == null) {
            showAlert("Error", "Please select a doctor");
            return;
        }

        if (descriptionField.getText().trim().isEmpty()) {
            showAlert("Error", "Description cannot be empty");
            return;
        }

        if (datePicker.getValue() == null) {
            showAlert("Error", "Please select a date");
            return;
        }

        try {
            // Extract IDs
            int typeId = Integer.parseInt(typeComboBox.getValue().split(" - ")[0]);
            int medecinId = Integer.parseInt(medecinComboBox.getValue().split(" - ")[0]);
            String photoPath = selectedFile != null ? selectedFile.getAbsolutePath() : null;

            // Create reclamation
            Reclamation nouvelleReclamation = new Reclamation();
            nouvelleReclamation.setTypeReclamationId(typeId);
            nouvelleReclamation.setDescription(descriptionField.getText());
            nouvelleReclamation.setDateReclamation(datePicker.getValue());
            nouvelleReclamation.setMedecinId(medecinId);
            nouvelleReclamation.setPhotoPath(photoPath);

            // Add to database
            boolean success = reclamationService.addReclamation(nouvelleReclamation);

            if (success) {
                showSuccessAlert("Success", "Claim added successfully!");

                // Refresh main table if mainController is set


                // Close the window
                ((Stage) typeComboBox.getScene().getWindow()).close();
            } else {
                showAlert("Error", "Failed to add claim");
            }

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid ID format: " + e.getMessage());
        } catch (SQLException e) {
            showAlert("Database Error", "Error adding claim: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Error", "Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeComboBoxes() {
        try {
            // Initialisation ComboBox types
            List<TypeReclamation> typesList = reclamationService.getAllTypes();
            ObservableList<String> types = FXCollections.observableArrayList();

            for (TypeReclamation type : typesList) {
                types.add(type.getId() + " - " + type.getNom());
            }

            typeComboBox.setItems(types);

            // Initialisation ComboBox médecins
            List<Medecin> medecinsList = reclamationService.getAllMedecins();
            ObservableList<String> medecins = FXCollections.observableArrayList();

            for (Medecin medecin : medecinsList) {
                medecins.add(medecin.getId() + " - " + medecin.getNom());
            }

            medecinComboBox.setItems(medecins);

            // Sélection automatique du premier élément
            if (!types.isEmpty()) {
                typeComboBox.getSelectionModel().selectFirst();
            }
            if (!medecins.isEmpty()) {
                medecinComboBox.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            showAlert("Erreur de base de données",
                    "Impossible de charger les listes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void browsePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                photoView.setImage(image);
            } catch (Exception e) {
                showAlert("Error", "Could not load image: " + e.getMessage());
            }
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
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not load dashboard: " + e.getMessage());
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