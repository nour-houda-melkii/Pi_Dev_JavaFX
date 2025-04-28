package controller;

import entity.Reclamation;
import entity.Reponse;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ReclamationServices;
import javafx.event.ActionEvent;
import javafx.animation.FadeTransition;
import javafx.util.Duration;


import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {
    @FXML private ScrollPane scrollPane;
    @FXML private FlowPane cardsContainer;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button addBtn;
    @FXML private TextField searchField;
    @FXML private Label notificationLabel;



    private final ReclamationServices service = new ReclamationServices();
    private Reclamation selectedReclamation;

    public static void show(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/view/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Gestion des Réclamations");
        stage.show();
    }
    private void setupSearchField() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterReclamations(newValue);
            });
        }
    }

    private void filterReclamations(String keyword) {
        try {
            List<Reclamation> reclamations = service.getAllReclamationsWithNames();
            cardsContainer.getChildren().clear();

            for (Reclamation reclamation : reclamations) {
                // Vérifie si la description ou le type correspond au texte saisi
                if (reclamation.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                        reclamation.getTypeReclamationName().toLowerCase().contains(keyword.toLowerCase()) ||
                        reclamation.getMedecinName().toLowerCase().contains(keyword.toLowerCase())) {

                    VBox card = createCard(reclamation);
                    cardsContainer.getChildren().add(card);
                }
            }



            if (cardsContainer.getChildren().isEmpty()) {
                Label emptyLabel = new Label("Aucune réclamation correspondante");
                cardsContainer.getChildren().add(emptyLabel);
            }

        } catch (SQLException e) {
            System.err.println("Erreur de base de données:");
            e.printStackTrace();
        }
    }



    @FXML
    public void initialize() {
        System.out.println("Initialisation du contrôleur démarrée");
        try {
            loadReclamations();
            setupSelectionButtons();
            setupSearchField(); // <-- ajouter ceci
            System.out.println("Initialisation du contrôleur terminée");
        } catch (Exception e) {
            System.err.println("Erreur dans initialize():");
            e.printStackTrace();
        }

        if(addBtn != null) {
            addBtn.setOnAction(e -> handleAdd());
        } else {
            System.err.println("Erreur: addBtn est null!");
        }
    }



    private void loadReclamations() {
        try {
            System.out.println("Tentative de chargement des données...");
            List<Reclamation> reclamations = service.getAllReclamationsWithNames();
            System.out.println("Nombre de réclamations chargées: " + reclamations.size());

            cardsContainer.getChildren().clear();

            if (reclamations.isEmpty()) {
                Label emptyLabel = new Label("Aucune réclamation trouvée");
                cardsContainer.getChildren().add(emptyLabel);
            } else {
                for (Reclamation reclamation : reclamations) {
                    VBox card = createCard(reclamation);
                    cardsContainer.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de base de données:");
            e.printStackTrace();

            Label errorLabel = new Label("Erreur de connexion à la base de données");
            cardsContainer.getChildren().add(errorLabel);
        }
    }



    private VBox createCard(Reclamation reclamation) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-radius: 10; -fx-border-color: #e0e0e0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); " +
                "-fx-padding: 15; -fx-spacing: 12; -fx-min-width: 300;");

        // Type et date
        HBox header = new HBox();
        Label typeLabel = new Label("Type: " + reclamation.getTypeReclamationName());
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #00B4D8;");

        Label dateLabel = new Label("Date: " + reclamation.getDateReclamation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        header.getChildren().addAll(typeLabel, new Region(), dateLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        // Description
        TextArea descriptionText = new TextArea(reclamation.getDescription());
        descriptionText.setEditable(false);
        descriptionText.setWrapText(true);
        descriptionText.setStyle("-fx-text-fill: black; -fx-background-color: transparent; -fx-border-color: transparent;");
        descriptionText.setPrefHeight(60);

        // Photo
        HBox imageContainer = new HBox();
        if (reclamation.getPhotoPath() != null && !reclamation.getPhotoPath().isEmpty()) {
            try {
                ImageView photoView = new ImageView(new Image(new File(reclamation.getPhotoPath()).toURI().toString()));
                photoView.setFitHeight(80);
                photoView.setFitWidth(80);
                photoView.setPreserveRatio(true);
                imageContainer.getChildren().add(photoView);
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'image: " + e.getMessage());
            }
        }

        // Médecin
        Label medecinLabel = new Label("Médecin: " + reclamation.getMedecinName());
        medecinLabel.setStyle("-fx-font-size: 12; -fx-text-fill: black;");

        // Boutons
        HBox buttons = new HBox(10);
        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> handleEdit(reclamation));
        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> handleDelete(reclamation));
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        buttons.getChildren().addAll(new Region(), editBtn, deleteBtn);
        HBox.setHgrow(buttons.getChildren().get(0), Priority.ALWAYS);

        // Effet de survol
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

        // Sélection
        card.setOnMouseClicked(e -> {
            selectedReclamation = reclamation;
            cardsContainer.getChildren().forEach(c ->
                    c.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;")
            );
            card.setStyle("-fx-background-color: #d4e6f1; -fx-border-color: #3498db;");
            this.editBtn.setDisable(false);
            this.deleteBtn.setDisable(false);
        });

        card.getChildren().addAll(header, descriptionText, imageContainer, medecinLabel, buttons);
        return card;
    }

    private void setupSelectionButtons() {
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
    }

    private void handleEdit(Reclamation reclamation) {
        selectedReclamation = reclamation;
        handleEdit();
    }

    private void handleDelete(Reclamation reclamation) {
        selectedReclamation = reclamation;
        handleDelete();
    }

    @FXML
    private void handleAdd() {
        try {
            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add-reclamation.fxml"));
            Parent root = loader.load();

            // Configurer le contrôleur
            AddReclamationController controller = loader.getController();
            controller.setMainController(this);
            if(controller == null) {
                System.err.println("ERREUR: Controller non initialisé!");
            }

            // Initialiser les ComboBox avec les données


            // Créer et afficher la fenêtre
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une réclamation");
            stage.initModality(Modality.APPLICATION_MODAL); // Rend la fenêtre modale
            stage.showAndWait(); // Attend la fermeture de la fenêtre

            // Rafraîchir après fermeture
            loadReclamations();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre d'ajout : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }



    @FXML
    private void handleEdit() {
        if (selectedReclamation != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/edit-reclamation.fxml"));
                Parent root = loader.load();

                EditReclamationController controller = loader.getController();
                controller.setReclamationToEdit(selectedReclamation);
                controller.setMainController(this);

                // Initialiser les ComboBox
                controller.initialize();

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Modifier Réclamation");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();

                loadReclamations();
            } catch (IOException e) {
                showAlert("Erreur", "Impossible d'ouvrir la fenêtre d'édition: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une réclamation à modifier", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedReclamation != null) {
            try {
                // Supprimer la conversion inutile puisque getId() retourne déjà un int
                service.deleteReclamation(selectedReclamation.getId());
                showAlert("Succès", "Réclamation supprimée !", Alert.AlertType.ERROR);
                loadReclamations();
                selectedReclamation = null;
                editBtn.setDisable(true);
                deleteBtn.setDisable(true);
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleBackend(ActionEvent event) {
        try {
            InputStream fxmlStream = getClass().getResourceAsStream("/view/backend-view.fxml");
            if (fxmlStream == null) {
                throw new IOException("Fichier backend-view.fxml introuvable dans les ressources");
            }

            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(fxmlStream);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Administration SAHATECK");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(((Node)event.getSource()).getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors du chargement de l'interface admin: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void listAllFilesInResources() {
        try {
            System.out.println("Contenu de resources:");
            Files.walk(Paths.get("src/main/resources"))
                    .forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Erreur lecture resources: " + e.getMessage());
        }
    }

    private void showDetailedError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Échec du chargement");

        TextArea textArea = new TextArea(
                "Message: " + e.getMessage() + "\n\n" +
                        "Stack Trace:\n" + getStackTraceAsString(e));
        textArea.setEditable(false);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public void refreshReclamations() {
        loadReclamations();
    }


    // Add these methods to your MainController class

    /**
     * Shows a notification message in the main view
     * @param message The message to display
     * @param isError Whether this is an error message
     */
    public void showNotification(String message, boolean isError) {
        if (notificationLabel != null) {
            notificationLabel.setText(message);

            // Style based on message type
            if (isError) {
                notificationLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                notificationLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }

            // Make the notification visible
            notificationLabel.setVisible(true);

            // Create a fade-out effect after a few seconds
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(5), notificationLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(3));
            fadeOut.play();

            // Hide the label after animation completes
            fadeOut.setOnFinished(e -> notificationLabel.setVisible(false));
        }
    }

    /**
     * Called when a response is added to a reclamation
     * @param reclamationId The ID of the reclamation
     * @param responseText The response text that was added
     */
    public void onResponseAdded(int reclamationId, String responseText) {
        try {
            // Fetch the updated reclamation with the new response
            Reclamation updatedReclamation = service.getReclamationByIdWithNames(reclamationId);

            if (updatedReclamation != null) {
                // Show a notification
                showNotification("Réponse ajoutée à la réclamation #" + reclamationId, false);

                // Refresh the view to show the updated data
                loadReclamations();
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger la réclamation mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

}