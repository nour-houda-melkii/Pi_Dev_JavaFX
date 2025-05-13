package controller;

import com.models.TypeReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.TypeReclamationService;

import java.io.IOException;
import java.sql.SQLException;

public class TypeReclamationController {
    @FXML private FlowPane cardsContainer;
    @FXML private TextField nomField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    private final TypeReclamationService service = new TypeReclamationService();
    private final ObservableList<TypeReclamation> data = FXCollections.observableArrayList();
    private TypeReclamation selectedType;

    @FXML
    public void initialize() {
        loadData();
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void loadData() {
        try {
            data.setAll(service.getAllTypes());
            displayCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void displayCards() {
        cardsContainer.getChildren().clear();

        if (data.isEmpty()) {
            Label emptyLabel = new Label("Aucun type de réclamation trouvé");
            cardsContainer.getChildren().add(emptyLabel);
        } else {
            for (TypeReclamation type : data) {
                VBox card = createTypeCard(type);
                cardsContainer.getChildren().add(card);
            }
        }
    }

    private VBox createTypeCard(TypeReclamation type) {
        VBox card = new VBox(10);
        // Style de base de la carte
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-radius: 10; -fx-border-color: #e0e0e0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 250;");

        // Nom du type
        Label nomLabel = new Label(type.getNom());
        nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #00B4D8;");
        card.getChildren().add(nomLabel);

        // Effets de survol
        card.setOnMouseEntered(e -> {
            if (!card.getStyle().contains("-fx-background-color: #d4e6f1")) {
                card.setStyle("-fx-background-color: #e0e0e0; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-border-color: #bbb; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                        "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 250;");
            }
        });

        card.setOnMouseExited(e -> {
            if (!card.getStyle().contains("-fx-background-color: #d4e6f1")) {
                card.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                        "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 250;");
            }
        });

        // Gestion du clic
        card.setOnMouseClicked(e -> {
            // Désélectionner toutes les cartes
            cardsContainer.getChildren().forEach(c -> {
                c.setStyle("-fx-background-color: white; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                        "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 250;");
            });

            // Sélectionner la carte cliquée
            card.setStyle("-fx-background-color: #d4e6f1; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: #3498db; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                    "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 250;");

            selectedType = type;
            updateButton.setDisable(false);
            deleteButton.setDisable(false);
            nomField.setText(type.getNom());
        });

        return card;
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
        if (selectedType != null && validateForm()) {
            selectedType.setNom(nomField.getText());

            try {
                service.updateType(selectedType);
                loadData();
                clearForm();
                showAlert("Succès", "Type modifié avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de modification: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedType != null) {
            try {
                service.deleteType(selectedType.getId());
                loadData();
                clearForm();
                showAlert("Succès", "Type supprimé!");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de suppression: " + e.getMessage());
            }
        }
    }

    public void refreshData() {
        loadData();
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
        selectedType = null;
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}