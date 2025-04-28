package controller;

import entity.Reclamation;
import entity.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import services.ReclamationServices;
import services.ReponseService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ReclamationViewController {
    @FXML private FlowPane cardsContainer;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;

    private final ReclamationServices reclamationService = new ReclamationServices();
    private final ReponseService reponseService = new ReponseService();
    private final ObservableList<Reclamation> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        loadData();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupFilters() {
        try {
            // Get all reclamations then extract unique type names
            List<Reclamation> allReclamations = reclamationService.getAllReclamationsWithNames();
            List<String> types = allReclamations.stream()
                    .map(Reclamation::getTypeReclamationName)
                    .distinct()
                    .collect(Collectors.toList());

            typeFilter.getItems().add("Tous les types");
            typeFilter.getItems().addAll(types);
            typeFilter.setValue("Tous les types");
            typeFilter.setOnAction(e -> applyFilters());
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement des types: " + e.getMessage(), Alert.AlertType.ERROR);
        }

        // Setup status filter options
        statusFilter.setVisible(false); // Cacher le ComboBox de statut
    }

    @FXML
    public void refreshReclamations() {
        loadData();
    }

    private void applyFilters() {
        String selectedType = typeFilter.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        try {
            List<Reclamation> allReclamations = reclamationService.getAllReclamationsWithNames();
            List<Reclamation> filteredList = new ArrayList<>(allReclamations);

            if (selectedType != null && !"Tous les types".equals(selectedType)) {
                filteredList = filteredList.stream()
                        .filter(r -> selectedType.equalsIgnoreCase(r.getTypeReclamationName()))
                        .collect(Collectors.toList());
            }

            if (!searchText.isEmpty()) {
                filteredList = filteredList.stream()
                        .filter(r ->
                                (r.getDescription() != null && r.getDescription().toLowerCase().contains(searchText)) ||
                                        (r.getFormattedDate() != null && r.getFormattedDate().toLowerCase().contains(searchText)) ||
                                        (r.getMedecinName() != null && r.getMedecinName().toLowerCase().contains(searchText))
                        )
                        .collect(Collectors.toList());
            }

            data.setAll(filteredList);
            displayReclamationsAsCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void loadData() {
        try {
            data.setAll(reclamationService.getAllReclamationsWithNames());
            displayReclamationsAsCards();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayReclamationsAsCards() {
        cardsContainer.getChildren().clear();

        for (Reclamation reclamation : data) {
            // Create card container
            VBox card = new VBox(10);
            card.getStyleClass().add("reclamation-card");
            card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; " +
                    "-fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 5);");
            card.setPrefWidth(300);
            card.setMaxWidth(300);

            // Card header with type only (ID removed)
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setSpacing(10);

            Label typeLabel = new Label(reclamation.getTypeReclamationName());
            typeLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 3 8; -fx-background-radius: 4;");

            header.getChildren().add(typeLabel);

            // Date info
            Label dateLabel = new Label("Date: " + reclamation.getFormattedDate());

            // Description
            Label descriptionTitle = new Label("Description:");
            descriptionTitle.setStyle("-fx-font-weight: bold;");

            TextFlow descriptionFlow = new TextFlow();
            Text descriptionText = new Text(reclamation.getDescription());
            descriptionFlow.getChildren().add(descriptionText);
            descriptionFlow.setStyle("-fx-padding: 5;");

            // Médecin
            Label medecinLabel = new Label("Médecin: " + reclamation.getMedecinName());

            // Action buttons
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button respondBtn = new Button("Répondre");
            respondBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
            respondBtn.setOnAction(e -> showResponseDialog(reclamation));

            Button viewResponsesBtn = new Button("Voir réponses");
            viewResponsesBtn.setOnAction(e -> showExistingResponses(reclamation));

            actions.getChildren().addAll(viewResponsesBtn, respondBtn);

            // Add all elements to card
            card.getChildren().addAll(
                    header,
                    dateLabel,
                    medecinLabel,
                    descriptionTitle,
                    descriptionFlow,
                    actions
            );

            // Add card to container
            cardsContainer.getChildren().add(card);
        }
    }

    private void showExistingResponses(Reclamation reclamation) {
        List<Reponse> responses = reponseService.getResponsesForReclamation(reclamation.getId());

        if (responses == null || responses.isEmpty()) {
            showAlert("Information", "Aucune réponse n'a été fournie pour cette réclamation.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réponses pour cette réclamation");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        for (Reponse response : responses) {
            VBox responseBox = new VBox(5);
            responseBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label dateLabel = new Label(response.getDateReponse().toString());
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

            Label contentLabel = new Label(response.getContenu());
            contentLabel.setWrapText(true);

            responseBox.getChildren().addAll(dateLabel, contentLabel);
            content.getChildren().add(responseBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void showResponseDialog(Reclamation reclamation) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Répondre à la réclamation");

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
        List<Reponse> responses = reponseService.getResponsesForReclamation(reclamation.getId());
        if (responses != null && !responses.isEmpty()) {
            VBox responsesBox = new VBox(5);
            responsesBox.getChildren().add(new Label("Réponses existantes:"));

            for (Reponse r : responses) {
                Label responseLabel = new Label(r.getDateReponse() + ": " + r.getContenu());
                responseLabel.setWrapText(true);
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
                reponse.setReclamationId(reclamation.getId());
                return reponse;
            }
            return null;
        });

        Optional<Reponse> result = dialog.showAndWait();

        result.ifPresent(reponse -> {
            try {
                reponseService.addResponse(reponse);
                showAlert("Succès", "Réponse enregistrée avec succès!", Alert.AlertType.INFORMATION);

                // Update status to "Traitée" (gardé pour la fonctionnalité interne)
                try {
                    reclamationService.updateStatus(reclamation.getId(), "Traitée");
                } catch (Exception ex) {
                    try {
                        String updateQuery = "UPDATE reclamation SET status = 'Traitée' WHERE id = " + reclamation.getId();
                        reclamationService.executeUpdate(updateQuery);
                    } catch (Exception e) {
                        // Pas d'alerte car nous ne montrons plus le statut
                    }
                }

                loadData(); // Reload data to reflect changes
            } catch (Exception e) {
                showAlert("Erreur", "Échec de l'enregistrement de la réponse: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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