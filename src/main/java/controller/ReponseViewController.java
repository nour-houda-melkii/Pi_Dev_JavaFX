package controller;

import entity.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import services.ReponseService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ReponseViewController {
    // Constantes de validation
    private static final int MIN_CONTENU_LENGTH = 10;
    private static final int MAX_CONTENU_LENGTH = 500;

    // Composants FXML
    @FXML private TableView<Reponse> reponseTable;
    @FXML private TableColumn<Reponse, Integer> idColumn;
    @FXML private TableColumn<Reponse, String> contenuColumn;
    @FXML private TableColumn<Reponse, LocalDate> dateColumn;
    @FXML private TableColumn<Reponse, Integer> reclamationIdColumn;
    @FXML private TableColumn<Reponse, Void> actionColumn;
    @FXML private TextArea newReponseText;
    @FXML private Label errorLabel;

    private final ReponseService reponseService = new ReponseService();
    private final ObservableList<Reponse> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadData();
        addActionButtons();
        setupTextValidation();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateReponse"));
        reclamationIdColumn.setCellValueFactory(new PropertyValueFactory<>("reclamationId"));
    }

    private void loadData() {
        try {
            data.setAll(reponseService.getAllResponses());
            reponseTable.setItems(data);
        } catch (Exception e) {
            showError("Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addActionButtons() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox pane = new HBox(editBtn, deleteBtn);

            {
                pane.setSpacing(5);
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                editBtn.setOnAction(event -> editReponse(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> deleteReponse(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupTextValidation() {
        newReponseText.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > MAX_CONTENU_LENGTH) {
                newReponseText.setText(oldVal);
            }
        });
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

        // Configuration des boutons
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        // Composants
        TextArea contenuArea = new TextArea(reponse.getContenu());
        contenuArea.setWrapText(true);
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: red;");

        // Validation en temps réel
        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            validationLabel.setText(validateContenu(newVal.trim()));
        });

        // Layout
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

    // Méthodes utilitaires
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
        // Implémentez la logique pour obtenir l'ID de la réclamation sélectionnée
        return 1; // Valeur par défaut - à adapter
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