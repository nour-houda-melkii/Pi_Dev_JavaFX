package controller;

import entity.Reclamation;
import entity.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import services.ReclamationServices;
import services.ReponseService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReclamationViewController {
    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> idColumn;
    @FXML private TableColumn<Reclamation, String> typeColumn;
    @FXML private TableColumn<Reclamation, String> descriptionColumn;
    @FXML private TableColumn<Reclamation, LocalDate> dateColumn;
    @FXML private TableColumn<Reclamation, String> medecinColumn;
    @FXML private TableColumn<Reclamation, String> photoColumn;
    @FXML private TableColumn<Reclamation, String> statusColumn;

    private final ReclamationServices reclamationService = new ReclamationServices();
    private final ReponseService reponseService = new ReponseService();
    private final ObservableList<Reclamation> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadData();
        addResponseColumn();
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
            data.setAll(reclamationService.getAllReclamations());
            reclamationTable.setItems(data);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addResponseColumn() {
        TableColumn<Reclamation, Void> actionCol = new TableColumn<>("Action");

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button respondBtn = new Button("Répondre");

            {
                respondBtn.setOnAction(event -> {
                    Reclamation reclamation = getTableView().getItems().get(getIndex());
                    showResponseDialog(reclamation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(respondBtn);
                }
            }
        });

        reclamationTable.getColumns().add(actionCol);
    }

    private void showResponseDialog(Reclamation reclamation) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Répondre à la réclamation #" + reclamation.getId());

        // Configuration des boutons
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextArea responseArea = new TextArea();
        responseArea.setPromptText("Entrez votre réponse ici...");
        responseArea.setPrefRowCount(5);

        grid.add(new Label("Réponse:"), 0, 0);
        grid.add(responseArea, 0, 1);

        // Afficher les réponses existantes
        List<Reponse> responses = reponseService.getResponsesForReclamation(Integer.parseInt(reclamation.getId()));
        if (responses != null && !responses.isEmpty()) {
            VBox responsesBox = new VBox(5);
            responsesBox.getChildren().add(new Label("Réponses existantes:"));

            for (Reponse r : responses) {
                Label responseLabel = new Label(r.getDateReponse() + ": " + r.getContenu());
                responsesBox.getChildren().add(responseLabel);
            }

            grid.add(responsesBox, 0, 2);
        }

        dialog.getDialogPane().setContent(grid);

        // Conversion du résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Reponse reponse = new Reponse();
                reponse.setContenu(responseArea.getText());
                reponse.setDateReponse(LocalDate.now());
                reponse.setReclamationId(Integer.parseInt(reclamation.getId()));
                return reponse;
            }
            return null;
        });

        Optional<Reponse> result = dialog.showAndWait();

        result.ifPresent(reponse -> {
            reponseService.addResponse(reponse);
            showAlert("Succès", "Réponse enregistrée avec succès!", Alert.AlertType.INFORMATION);
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}