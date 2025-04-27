package controller;

import entity.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.ReponseService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReponseViewController {
    private static final int MIN_CONTENU_LENGTH = 10;
    private static final int MAX_CONTENU_LENGTH = 500;

    @FXML private FlowPane cardsContainer;
    @FXML private TextArea newReponseText;
    @FXML private Label errorLabel;

    private final ReponseService reponseService = new ReponseService();
    private final ObservableList<Reponse> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        try {
            data.setAll(reponseService.getAllResponses());
            displayCards();
        } catch (Exception e) {
            showError("Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayCards() {
        cardsContainer.getChildren().clear();

        if (data.isEmpty()) {
            Label emptyLabel = new Label("Aucune réponse trouvée");
            cardsContainer.getChildren().add(emptyLabel);
        } else {
            for (Reponse reponse : data) {
                VBox card = createCard(reponse);
                cardsContainer.getChildren().add(card);
            }
        }
    }

    private VBox createCard(Reponse reponse) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-radius: 10; -fx-border-color: #e0e0e0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 300;");

        // En-tête avec date
        HBox header = new HBox();
        Label dateLabel = new Label("Date: " + reponse.getDateReponse().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        header.getChildren().add(dateLabel);
        HBox.setHgrow(header, Priority.ALWAYS);

        // Contenu de la réponse
        TextArea contenuText = new TextArea(reponse.getContenu());
        contenuText.setEditable(false);
        contenuText.setWrapText(true);
        contenuText.setStyle("-fx-text-fill: black; -fx-background-color: transparent; -fx-border-color: transparent;");
        contenuText.setPrefHeight(60);

        // Boutons d'action
        HBox buttons = new HBox(10);
        Button editBtn = new Button("Modifier");
        editBtn.setOnAction(e -> editReponse(reponse));
        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setOnAction(e -> deleteReponse(reponse));
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        buttons.getChildren().addAll(new Region(), editBtn, deleteBtn);
        HBox.setHgrow(buttons.getChildren().get(0), Priority.ALWAYS);

        // Effets de survol
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #e0e0e0; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: #bbb; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                    "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 300;");
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10; " +
                    "-fx-border-color: #e0e0e0; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                    "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 300;");
        });

        card.getChildren().addAll(header, contenuText, buttons);
        return card;
    }

    @FXML
    private void handleAddReponse() {
        String contenu = newReponseText.getText().trim();
        String errorMessage = validateContenu(contenu);

        if (errorMessage != null) {
            errorLabel.setText(errorMessage);
            return;
        }

        try {
            Reponse nouvelleReponse = new Reponse(contenu, LocalDate.now(), getSelectedReclamationId());
            reponseService.addResponse(nouvelleReponse);

            resetForm();
            loadData();
            showSuccess("Réponse ajoutée avec succès");
        } catch (Exception e) {
            showError("Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void editReponse(Reponse reponse) {
        Dialog<Reponse> dialog = createEditDialog(reponse);
        Optional<Reponse> result = dialog.showAndWait();
        result.ifPresent(this::updateReponse);
    }

    private Dialog<Reponse> createEditDialog(Reponse reponse) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Modifier la réponse");

        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        TextArea contenuArea = new TextArea(reponse.getContenu());
        contenuArea.setWrapText(true);
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: red;");

        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            validationLabel.setText(validateContenu(newVal.trim()));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Contenu:"), 0, 0);
        grid.add(contenuArea, 0, 1);
        grid.add(validationLabel, 0, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String nouveauContenu = contenuArea.getText().trim();
                if (validateContenu(nouveauContenu) == null) {
                    reponse.setContenu(nouveauContenu);
                    return reponse;
                }
            }
            return null;
        });

        return dialog;
    }

    private void updateReponse(Reponse reponse) {
        try {
            reponseService.updateResponse(reponse);
            loadData();
            showSuccess("Réponse modifiée avec succès");
        } catch (Exception e) {
            showError("Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteReponse(Reponse reponse) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette réponse?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reponseService.deleteResponse(reponse.getId());
                loadData();
                showSuccess("Réponse supprimée avec succès");
            } catch (SQLException e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String validateContenu(String contenu) {
        if (contenu.isEmpty()) return "Le contenu ne peut pas être vide";
        if (contenu.length() < MIN_CONTENU_LENGTH) {
            return "Minimum " + MIN_CONTENU_LENGTH + " caractères requis";
        }
        if (contenu.length() > MAX_CONTENU_LENGTH) {
            return "Maximum " + MAX_CONTENU_LENGTH + " caractères autorisés";
        }
        return null;
    }

    private void resetForm() {
        newReponseText.clear();
        errorLabel.setText("");
    }

    private int getSelectedReclamationId() {
        // À adapter selon votre logique
        return 1;
    }

    private void showSuccess(String message) {
        showAlert("Succès", message, Alert.AlertType.INFORMATION);
    }

    private void showError(String message) {
        showAlert("Erreur", message, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}