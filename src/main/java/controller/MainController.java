package controller;

import entity.Reclamation;
import entity.Reponse;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
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
import services.NotificationService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import utils.NotificationManager;
import utils.NotificationManager.NotificationType;

public class MainController {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button addBtn;
    @FXML
    private TextField searchField;
    @FXML
    private Label notificationLabel;
    @FXML
    private StackPane notificationBellContainer;
    @FXML
    private Label notificationCountLabel;
    @FXML
    private Button notificationBellButton;

    private int notificationCount = 0;
    private List<String> pendingNotifications = new ArrayList<>();
    private final ReclamationServices service = new ReclamationServices();
    private Reclamation selectedReclamation;

    @FXML
    public void initialize() {
        System.out.println("Initialisation du MainController démarrée");

        // S'abonner au service de notification
        NotificationService.getInstance().addNotificationListener(this::addNotification);

        // Initialiser les éléments de notification
        notificationCount = 0;
        pendingNotifications = new ArrayList<>();

        // Vérifier les éléments d'interface
        if (notificationCountLabel != null) {
            notificationCountLabel.setVisible(false);
            System.out.println("notificationCountLabel initialisé correctement");
        } else {
            System.err.println("ERREUR: notificationCountLabel est null dans initialize()");
        }

        if (notificationBellButton != null) {
            System.out.println("notificationBellButton initialisé correctement");
        } else {
            System.err.println("ERREUR: notificationBellButton est null dans initialize()");
        }

        try {
            loadReclamations();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réclamations: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            setupSelectionButtons();
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration des boutons de sélection: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            setupSearchField();
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration du champ de recherche: " + e.getMessage());
            e.printStackTrace();
        }

        if (addBtn != null) {
            addBtn.setOnAction(e -> handleAdd());
        } else {
            System.err.println("Erreur: addBtn est null!");
        }

        if (scrollPane != null) {
            scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setUserData(this);
                    System.out.println("MainController défini comme userData de la scène");
                }
            });
        } else {
            System.err.println("Erreur: scrollPane est null!");
        }

        System.out.println("Initialisation du MainController terminée");
    }

    @FXML
    private void handleNotificationBell(ActionEvent event) {
        System.out.println("Clic sur la cloche de notification détecté");
        System.out.println("Nombre de notifications en attente : " + pendingNotifications.size());

        if (pendingNotifications.isEmpty()) {
            showAlert("Notifications", "Aucune nouvelle notification", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            // Afficher les notifications dans une fenêtre
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Notifications");
            dialog.setHeaderText("Nouvelles réponses");

            VBox content = new VBox(10);
            content.setPadding(new Insets(20));

            for (String notification : pendingNotifications) {
                Label notifLabel = new Label(notification);
                notifLabel.setWrapText(true);
                notifLabel.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
                content.getChildren().add(notifLabel);
            }

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);

            dialog.getDialogPane().setContent(scrollPane);

            // Ajouter un bouton de fermeture
            ButtonType closeButton = ButtonType.CLOSE;
            dialog.getDialogPane().getButtonTypes().add(closeButton);

            // Réinitialiser le compteur après lecture
            dialog.setOnCloseRequest(e -> {
                resetNotificationCount();
            });

            System.out.println("Affichage de la fenêtre de dialogue des notifications");
            dialog.showAndWait();
            System.out.println("Fenêtre de dialogue des notifications fermée");
        } catch (Exception e) {
            System.err.println("ERREUR lors de l'affichage des notifications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'afficher les notifications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void addNotification(String message) {
        System.out.println("addNotification appelée avec message: " + message);

        notificationCount++;
        pendingNotifications.add(message);

        // Important: utilisez Platform.runLater pour mettre à jour l'UI depuis un autre thread
        Platform.runLater(() -> updateNotificationCount());
    }

    private void updateNotificationCount() {
        System.out.println("updateNotificationCount appelée, count=" + notificationCount);

        if (notificationCountLabel == null) {
            System.err.println("ERREUR: notificationCountLabel est null!");
            return;
        }

        if (notificationCount > 0) {
            notificationCountLabel.setText(String.valueOf(notificationCount));
            notificationCountLabel.setVisible(true);
            System.out.println("Notification count affiché: " + notificationCountLabel.getText());
        } else {
            notificationCountLabel.setVisible(false);
        }
    }

    private void resetNotificationCount() {
        notificationCount = 0;
        pendingNotifications.clear();
        updateNotificationCount();
    }

    /**
     * Ouvre la vue des réclamations
     */
    @FXML
    private void handleReclamationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reclamation-view.fxml"));
            Parent root = loader.load();

            ReclamationViewController controller = loader.getController();
            controller.setMainController(this);
            System.out.println("MainController passé à ReclamationViewController");

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Gestion des Réclamations");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(scrollPane.getScene().getWindow());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la vue des réclamations: " + e.getMessage(), Alert.AlertType.ERROR);
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

            // Si backend-view contient ReclamationViewController, configurez-le
            if (loader.getController() instanceof ReclamationViewController) {
                ReclamationViewController controller = (ReclamationViewController) loader.getController();
                controller.setMainController(this);
                System.out.println("MainController passé à ReclamationViewController dans handleBackend");
            }

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            scene.setUserData(this);
            stage.setScene(scene);
            stage.setTitle("Administration SAHATECK");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors du chargement de l'interface admin: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupSearchField() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterReclamations(newValue);
            });
        } else {
            System.err.println("ATTENTION: searchField est null dans setupSearchField()");
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
        Button localEditBtn = new Button("Edit");
        localEditBtn.setOnAction(e -> handleEdit(reclamation));
        localEditBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        Button localDeleteBtn = new Button("Delete");
        localDeleteBtn.setOnAction(e -> handleDelete(reclamation));
        localDeleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        buttons.getChildren().addAll(new Region(), localEditBtn, localDeleteBtn);
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

            // Activer les boutons de la carte sélectionnée
            if (editBtn != null) {
                editBtn.setDisable(false);
            }
            if (deleteBtn != null) {
                deleteBtn.setDisable(false);
            }
        });

        card.getChildren().addAll(header, descriptionText, imageContainer, medecinLabel, buttons);
        return card;
    }

    private void setupSelectionButtons() {
        if (editBtn != null) {
            editBtn.setDisable(true);
        } else {
            System.err.println("ATTENTION: editBtn est null dans setupSelectionButtons()");
        }

        if (deleteBtn != null) {
            deleteBtn.setDisable(true);
        } else {
            System.err.println("ATTENTION: deleteBtn est null dans setupSelectionButtons()");
        }
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add-reclamation.fxml"));
            Parent root = loader.load();

            AddReclamationController controller = loader.getController();
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une réclamation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

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
                service.deleteReclamation(selectedReclamation.getId());
                showAlert("Succès", "Réclamation supprimée !", Alert.AlertType.INFORMATION);
                loadReclamations();
                selectedReclamation = null;

                if (editBtn != null) {
                    editBtn.setDisable(true);
                }
                if (deleteBtn != null) {
                    deleteBtn.setDisable(true);
                }
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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

    public void showSimpleNotification(String message) {
        NotificationManager.showInlineNotification(notificationLabel, message, NotificationType.SUCCESS);
    }

    public void showNotification(String message, boolean isError) {
        NotificationType type = isError ? NotificationType.ERROR : NotificationType.SUCCESS;
        NotificationManager.showInlineNotification(notificationLabel, message, type);

        if (notificationLabel != null) {
            notificationLabel.setText(message);

            if (isError) {
                notificationLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                notificationLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }

            notificationLabel.setVisible(true);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(5), notificationLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(3));
            fadeOut.play();

            fadeOut.setOnFinished(e -> notificationLabel.setVisible(false));
        }
    }

    // Méthode de test pour vérifier le système de notification
    @FXML
    private void testNotification() {
        String testMsg = "Test de notification " + System.currentTimeMillis();
        addNotification(testMsg);
        showSimpleNotification("Notification de test envoyée");
    }
}