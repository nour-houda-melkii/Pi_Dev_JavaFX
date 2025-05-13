package controller;

import com.models.Reaction;
import com.models.Reclamation;
import com.models.Reponse;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.*;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import utils.NotificationManager;
import utils.NotificationManager.NotificationType;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import com.utils.DataSource;


public class MainController implements ReactionNotificationService.ReactionNotificationListener {
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
    private HBox reactionsBox; // Add this line
    private Reponse currentResponse; // Also add this if not already present

    private final ReactionService reactionService = new ReactionService();
    private final ReponseService reponseService = new ReponseService();
    private String currentUser = "utilisateur_courant";
    private int notificationCount = 0;
    private List<String> pendingNotifications = new ArrayList<>();
    private final ReclamationServices service = new ReclamationServices();
    private Reclamation selectedReclamation;
    private List<Reclamation> lastLoadedReclamations = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("Initialisation du MainController démarrée");

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
        ReactionNotificationService.getInstance().addUserListener(this);
    }
    public void shutdown() {
        // Se désabonner correctement
        ReactionNotificationService.getInstance().removeUserListener(this);
    }

        // Assurez-vous que cette méthode existe et est implémentée
        @Override
        public void onReactionAdded(Reaction reaction, String message) {
            // Code pour gérer les notifications de réaction dans l'interface utilisateur
            Platform.runLater(() -> {
                // Mettez à jour le compteur de notifications, etc.
            });
            System.out.println("MainController: Notification de réaction ignorée - réservée à l'interface backend");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/backend-view.fxml"));
            Parent root = loader.load();

            // Récupérer le contrôleur backend
            BackendController backendController = loader.getController();

            // Créer et configurer la scène
            Stage stage = new Stage();
            Scene scene = new Scene(root);
               stage.setScene(scene);
            stage.setTitle("Administration SAHATECK");
stage.setFullScreen(true);
            // Lorsque la fenêtre est affichée, vérifier les notifications en attente
            stage.setOnShown(e -> {
                // Vérifier s'il y a des notifications en attente
                ReactionNotificationService.getInstance().checkPendingNotifications();
            });

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface admin: " + e.getMessage(), Alert.AlertType.ERROR);
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
            if (cardsContainer == null) {
                System.err.println("Erreur: cardsContainer est null dans filterReclamations");
                return;
            }

            List<Reclamation> reclamations = service.getAllReclamationsWithNames();
            cardsContainer.getChildren().clear();

            if (keyword == null || keyword.isEmpty()) {
                // If search field is empty, show all reclamations
                for (Reclamation reclamation : reclamations) {
                    VBox card = createCard(reclamation);
                    cardsContainer.getChildren().add(card);
                }
                return;
            }

            String lowercaseKeyword = keyword.toLowerCase();
            boolean foundMatch = false;

            for (Reclamation reclamation : reclamations) {
                // Vérifie si la description ou le type correspond au texte saisi
                if ((reclamation.getDescription() != null && reclamation.getDescription().toLowerCase().contains(lowercaseKeyword)) ||
                        (reclamation.getTypeReclamationName() != null && reclamation.getTypeReclamationName().toLowerCase().contains(lowercaseKeyword)) ||
                        (reclamation.getMedecinName() != null && reclamation.getMedecinName().toLowerCase().contains(lowercaseKeyword))) {

                    VBox card = createCard(reclamation);
                    cardsContainer.getChildren().add(card);
                    foundMatch = true;
                }
            }

            if (!foundMatch) {
                Label emptyLabel = new Label("Aucune réclamation correspondante");
                emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");
                cardsContainer.getChildren().add(emptyLabel);
            }

        } catch (SQLException e) {
            System.err.println("Erreur de base de données dans filterReclamations:");
            e.printStackTrace();

            if (cardsContainer != null) {
                Label errorLabel = new Label("Erreur lors de la recherche: " + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red;");
                cardsContainer.getChildren().add(errorLabel);
            }
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

            // Vérifier si une nouvelle réclamation a été ajoutée
            checkForNewReclamations(reclamations);

            // Sauvegarder la liste de réclamations pour la comparaison future
            lastLoadedReclamations = new ArrayList<>(reclamations);

        } catch (SQLException e) {
            System.err.println("Erreur de base de données:");
            e.printStackTrace();

            Label errorLabel = new Label("Erreur de connexion à la base de données");
            cardsContainer.getChildren().add(errorLabel);
        }
    }


    private void checkForNewReclamations(List<Reclamation> currentReclamations) {
        if (lastLoadedReclamations.isEmpty()) {
            return; // Première charge, pas de vérification nécessaire
        }

        // Si nous avons plus de réclamations qu'avant
        if (currentReclamations.size() > lastLoadedReclamations.size()) {
            // Créer un ensemble des IDs des réclamations déjà connues
            Set<Integer> knownIds = new HashSet<>();
            for (Reclamation rec : lastLoadedReclamations) {
                knownIds.add(rec.getId());
            }

            // Trouver les nouvelles réclamations (celles dont l'ID n'est pas dans knownIds)
            for (Reclamation newReclamation : currentReclamations) {
                if (!knownIds.contains(newReclamation.getId())) {
                    // Envoyer un email pour la nouvelle réclamation
                    Thread emailThread = new Thread(() -> sendEmailForReclamation(newReclamation));
                    emailThread.setDaemon(true); // Ne pas bloquer la fermeture de l'application
                    emailThread.start();
                }
            }
        }
    }



    private void sendEmailForReclamation(Reclamation reclamation) {
        try {
            // Construction du contenu de l'email
            String subject = "Nouvelle réclamation: " + reclamation.getTypeReclamationName();

            StringBuilder messageContent = new StringBuilder();
            messageContent.append("Une nouvelle réclamation a été ajoutée dans le système SAHATECK.\n\n");
            messageContent.append("Détails de la réclamation:\n");
            messageContent.append("---------------------------\n");
            messageContent.append("Type: ").append(reclamation.getTypeReclamationName()).append("\n");
            messageContent.append("Médecin concerné: ").append(reclamation.getMedecinName()).append("\n");
            messageContent.append("Date: ").append(reclamation.getDateReclamation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            messageContent.append("Description: ").append(reclamation.getDescription()).append("\n\n");
            messageContent.append("Veuillez consulter l'application SAHATECK pour traiter cette réclamation.");

            // Envoi de l'email
            sendEmail("sourournajjar2@gmail.com", subject, messageContent.toString());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi d'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie un email pour notifier de la nouvelle réponse
     * @param reclamation La réclamation concernée
     * @param reponse La nouvelle réponse
     */
    private void sendEmailForNewResponse(Reclamation reclamation, Reponse reponse) {
        try {
            // Construction du contenu de l'email
            String subject = "Nouvelle réponse à votre réclamation: " + reclamation.getTypeReclamationName();

            StringBuilder messageContent = new StringBuilder();
            messageContent.append("Une nouvelle réponse a été ajoutée à votre réclamation dans le système SAHATECK.\n\n");
            messageContent.append("Détails de la réclamation:\n");
            messageContent.append("---------------------------\n");
            messageContent.append("Type: ").append(reclamation.getTypeReclamationName()).append("\n");
            messageContent.append("Date: ").append(reclamation.getDateReclamation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            messageContent.append("Description: ").append(reclamation.getDescription()).append("\n\n");
            messageContent.append("Nouvelle réponse (").append(reponse.getDateReponse().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("):\n");
            messageContent.append("---------------------------\n");
            messageContent.append(reponse.getContenu()).append("\n\n");
            messageContent.append("Veuillez consulter l'application SAHATECK pour voir toutes les réponses et y réagir.");

            // Envoyer l'email au destinataire approprié
            sendEmail("sourournajjar2@gmail.com", subject, messageContent.toString());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi d'email pour la nouvelle réponse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            // Configuration des propriétés pour l'envoi d'email
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.connectiontimeout", "10000"); // Timeout in milliseconds
            props.put("mail.smtp.timeout", "10000");

            // Créer une session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("sourournajjar2@gmail.com", "runa hfvh forx yjuz");
                }
            });

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("sourournajjar2@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            // Envoyer le message
            Transport.send(message);
            System.out.println("Email envoyé avec succès à " + to);

            // Notify UI of success
            Platform.runLater(() -> showNotification("Email envoyé avec succès", false));
        } catch (MessagingException e) {
            System.err.println("Échec de l'envoi d'email: " + e.getMessage());
            e.printStackTrace();

            // Notify UI of error
            Platform.runLater(() -> showNotification("Échec d'envoi d'email: " + e.getMessage(), true));
        }
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
            // Add confirmation dialog to prevent accidental deletion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation de suppression");
            confirmAlert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette réclamation?");
            confirmAlert.setContentText("Cette action ne peut pas être annulée.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
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
        } else {
            showAlert("Aucune sélection", "Veuillez sélectionner une réclamation à supprimer", Alert.AlertType.WARNING);
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

    /**
     * Rafraîchit l'affichage des réponses pour une réclamation
     */
    private void refreshResponses(Reclamation reclamation) {
        if (reclamation != null) {
            // Fermer la fenêtre actuelle et la rouvrir pour afficher les données mises à jour
            showExistingResponses(reclamation);
        }
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

    /**
     * Méthode de test pour vérifier l'envoi d'emails
     */
    @FXML
    private void testEmail() {
        new Thread(() -> {
            sendEmail("sourournajjar2@gmail.com", "Test d'envoi d'email",
                    "Ceci est un test d'envoi d'email depuis l'application SAHATECK.");
        }).start();
        showSimpleNotification("Email de test envoyé");
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

        // Ajouter un bouton pour voir les réponses
        Button viewResponsesBtn = new Button("Voir réponses");
        viewResponsesBtn.setStyle("-fx-background-color: rgb(135,206,235); -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");
        viewResponsesBtn.setOnAction(e -> showExistingResponses(reclamation));

        Button localEditBtn = new Button("Edit");
        localEditBtn.setOnAction(e -> handleEdit(reclamation));
        localEditBtn.setStyle("-fx-background-color: rgb(135,206,235); -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        Button localDeleteBtn = new Button("Delete");
        localDeleteBtn.setOnAction(e -> handleDelete(reclamation));
        localDeleteBtn.setStyle("-fx-background-color: rgb(135,206,235); -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 5 10;");

        buttons.getChildren().addAll(new Region(), viewResponsesBtn, localEditBtn, localDeleteBtn);
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

        // Ajouter tous les éléments à la carte (sans le conteneur de réactions)
        card.getChildren().addAll(
                header,
                descriptionText,
                imageContainer,
                medecinLabel,
                buttons
        );

        return card;
    }

    private void showExistingResponses(Reclamation reclamation) {
        selectedReclamation = reclamation;
        List<Reponse> responses = reponseService.getResponsesForReclamation(reclamation.getId());

        if (responses == null || responses.isEmpty()) {
            showAlert("Information", "Aucune réponse n'a été fournie pour cette réclamation.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réponses pour cette réclamation");
        dialog.setHeaderText("Réclamation: " + reclamation.getTypeReclamationName());
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

// Set the dialog size to match screen bounds
        dialog.getDialogPane().setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // Afficher un résumé de la réclamation en haut
        VBox reclamationSummary = new VBox(5);
        reclamationSummary.setStyle("-fx-background-color: #e7f5ff; -fx-padding: 15; -fx-border-color: #bee5eb; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");

        Label reclamationTitle = new Label("Description de la réclamation:");
        reclamationTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label reclamationDesc = new Label(reclamation.getDescription());
        reclamationDesc.setWrapText(true);

        reclamationSummary.getChildren().addAll(reclamationTitle, reclamationDesc);
        content.getChildren().add(reclamationSummary);

        // Titre pour la section des réponses
        Label responsesTitle = new Label("Réponses:");
        responsesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        content.getChildren().add(responsesTitle);

        // Parcourir toutes les réponses
        for (Reponse response : responses) {
            VBox responseBox = new VBox(8);
            responseBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #e6e6e6; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 2);");

            // En-tête de la réponse avec date et auteur
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Label dateLabel = new Label(response.getDateReponse().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-weight: bold;");

            Label authorLabel = new Label(response.getUtilisateur() != null ? "Par: " + response.getUtilisateur() : "Réponse officielle");
            authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            header.getChildren().addAll(dateLabel, spacer, authorLabel);
            header.setPadding(new Insets(0, 0, 5, 0));

            // Contenu de la réponse
            TextArea contentArea = new TextArea(response.getContenu());
            contentArea.setWrapText(true);
            contentArea.setEditable(false);
            contentArea.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 14px;");
            contentArea.setPrefRowCount(3);
            contentArea.setMaxHeight(100);

            // Zone de réactions pour les réponses - améliorée avec un nouveau design
            HBox reactionsBox = createResponseReactionUI(response);

            responseBox.getChildren().addAll(header, contentArea, reactionsBox);
            content.getChildren().add(responseBox);
        }


        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-padding: 10;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(600);

        dialog.showAndWait();
    }


    private HBox createResponseReactionUI(Reponse response) {
        HBox reactionsContainer = new HBox(10);
        reactionsContainer.setAlignment(Pos.CENTER_LEFT);
        reactionsContainer.setPadding(new Insets(10, 0, 5, 0));
        reactionsContainer.setStyle("-fx-border-color: #eaeaea; -fx-border-width: 1 0 0 0; -fx-padding: 8 0;");

        try {
            // Get user's current reaction
            final String userReactionType = reactionService.getUserReactionTypeForReponse(response.getId(), currentUser);

            // Get reaction counts
            final Map<String, Integer> reactionCounts = reactionService.getReactionCountsForReponse(response.getId());

            // Define all possible reactions
            List<String[]> reactions = List.of(
                    new String[]{"LIKE", "👍", "#1877f2"},    // Facebook blue
                    new String[]{"LOVE", "❤️", "#f33e58"},    // Red
                    new String[]{"HAHA", "😂", "#f7b125"},    // Yellow
                    new String[]{"WOW", "😮", "#f7b125"},     // Yellow
                    new String[]{"SAD", "😢", "#f7b125"},    // Yellow
                    new String[]{"ANGRY", "😠", "#e9710f"}   // Orange
            );

            // Create the main reaction button (default shows Like or user's current reaction)
            String[] defaultReaction = reactions.get(0); // Default to Like
            if (userReactionType != null) {
                defaultReaction = reactions.stream()
                        .filter(r -> r[0].equals(userReactionType))
                        .findFirst()
                        .orElse(reactions.get(0));
            }

            // Reference to store main button to update later
            HBox[] mainButtonRef = new HBox[1];

            // Create the reaction popup (will appear on hover)
            HBox reactionPopup = createReactionPopup(reactions, response, userReactionType, mainButtonRef, reactionCounts);
            reactionPopup.setVisible(false);

            // Position the popup above the main button
            reactionPopup.setTranslateY(-40);
            reactionPopup.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 1);");

            // Main reaction button container
            HBox mainReactionButton = createReactionButton(
                    defaultReaction,
                    reactionCounts.getOrDefault(defaultReaction[0], 0),
                    userReactionType != null && userReactionType.equals(defaultReaction[0]),
                    reactionCounts.values().stream().mapToInt(Integer::intValue).sum() > 0
            );

            // Store reference for updates
            mainButtonRef[0] = mainReactionButton;

            // Create container for both elements
            StackPane container = new StackPane();

            // Show popup on hover
            mainReactionButton.setOnMouseEntered(e -> {
                reactionPopup.setVisible(true);
            });

            // Hide popup when mouse leaves both container
            container.setOnMouseExited(e -> {
                reactionPopup.setVisible(false);
            });

            // Keep popup visible when hovering over it
            reactionPopup.setOnMouseEntered(e -> {
                reactionPopup.setVisible(true);
            });

            container.getChildren().addAll(mainReactionButton, reactionPopup);
            reactionsContainer.getChildren().add(container);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return reactionsContainer;
    }

    // Method to create the reaction popup with all reaction options
    private HBox createReactionPopup(List<String[]> reactions, Reponse response, String userReactionType,
                                     HBox[] mainButtonRef, Map<String, Integer> reactionCounts) {
        HBox popup = new HBox(5);
        popup.setAlignment(Pos.CENTER);
        popup.setPadding(new Insets(5));

        for (String[] reaction : reactions) {
            String type = reaction[0];
            String emoji = reaction[1];
            String color = reaction[2];

            boolean isSelected = type.equals(userReactionType);

            // Create emoji button
            Text emojiText = new Text(emoji);
            emojiText.setStyle("-fx-font-size: 20px;");

            // Button container
            StackPane button = new StackPane(emojiText);
            button.setPadding(new Insets(5));
            button.setStyle("-fx-cursor: hand; -fx-background-radius: 50%;");

            // Add hover effect
            button.setOnMouseEntered(e -> {
                button.setStyle("-fx-cursor: hand; -fx-background-radius: 50%; -fx-background-color: #f3f4f6;");
                // Scale effect on hover
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), button);
                scaleTransition.setToX(1.2);
                scaleTransition.setToY(1.2);
                scaleTransition.play();
            });

            button.setOnMouseExited(e -> {
                button.setStyle("-fx-cursor: hand; -fx-background-radius: 50%;");
                // Scale back on exit
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), button);
                scaleTransition.setToX(1.0);
                scaleTransition.setToY(1.0);
                scaleTransition.play();
            });

            // Add click handler to update reaction
            button.setOnMouseClicked(e -> {
                try {
                    // If already selected, remove the reaction
                    if (isSelected) {
                        reactionService.removeReaction(response.getId(), currentUser, type);
                        // Update main button back to default
                        updateMainButton(mainButtonRef[0], reactions.get(0), 0, false, false);
                        System.out.println("Reaction removed: " + type);
                    } else {
                        // Remove previous reaction if exists
                        if (userReactionType != null) {
                            reactionService.removeReaction(response.getId(), currentUser, userReactionType);
                        }

                        // Add new reaction
                        Reaction responseReaction = new Reaction();
                        responseReaction.setReponseId(response.getId());
                        responseReaction.setType(type);
                        responseReaction.setUtilisateur(currentUser);
                        responseReaction.setDateReaction(LocalDate.now());

                        reactionService.addReaction(responseReaction);

                        // Update main button to show the selected reaction
                        int count = reactionCounts.getOrDefault(type, 0) + 1;
                        updateMainButton(mainButtonRef[0], reaction, count, true, true);
                        System.out.println("New reaction added: " + type);

                        // Notification
                        ReactionNotificationService.getInstance().notifyBellOfNewReaction(
                                responseReaction,
                                emoji,
                                currentUser
                        );
                    }

                    // Hide popup after selection
                    ((StackPane)button.getParent().getParent()).getChildren().get(1).setVisible(false);

                } catch (SQLException ex) {
                    System.err.println("Error processing reaction: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            popup.getChildren().add(button);
        }

        return popup;
    }



    // Method to update the main button after reaction changes
    private void updateMainButton(HBox button, String[] reaction, int count, boolean isSelected, boolean hasReaction) {
        // Clear existing children
        button.getChildren().clear();

        String type = reaction[0];
        String emoji = reaction[1];
        String color = reaction[2];

        // Update button style
        if (isSelected) {
            button.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 20; " +
                    "-fx-padding: 4 10; -fx-border-color: " + color + "; " +
                    "-fx-border-width: 1.5; -fx-border-radius: 20;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-background-radius: 20; " +
                    "-fx-padding: 4 10; -fx-cursor: hand;");
        }

        // Emoji
        Text emojiText = new Text(emoji);
        emojiText.setStyle("-fx-font-size: 16px;");
        button.getChildren().add(emojiText);

        // Add text label
        Text label = new Text(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
        label.setStyle("-fx-font-size: 14px; -fx-fill: " + (isSelected ? color : "#64748b") + ";");
        button.getChildren().add(label);

        // Add count if greater than 0
        if (count > 0) {
            Label countLabel = new Label(String.valueOf(count));
            countLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
                    (isSelected ? color : "#718096") + ";");
            button.getChildren().add(countLabel);
        }

        // Force layout update
        button.applyCss();
        button.layout();
    }
    private void refreshReactionUI(VBox responseBox) {
        if (currentResponse != null && responseBox != null) {
            // Find the reactions container in the response box
            Node reactionsNode = responseBox.getChildren().stream()
                    .filter(node -> node instanceof HBox &&
                            ((HBox) node).getStyle().contains("-fx-border-color: #eaeaea"))
                    .findFirst()
                    .orElse(null);

            if (reactionsNode != null) {
                int index = responseBox.getChildren().indexOf(reactionsNode);
                if (index >= 0) {
                    // Create new reactions UI
                    HBox newReactionsBox = createResponseReactionUI(currentResponse);

                    // Replace the old box with the new one
                    responseBox.getChildren().set(index, newReactionsBox);

                    // Animation for smoother refresh
                    FadeTransition fade = new FadeTransition(Duration.millis(300), newReactionsBox);
                    fade.setFromValue(0.5);
                    fade.setToValue(1.0);
                    fade.play();
                }
            }
        }
    }

    private HBox createReactionButton(String[] reaction, int count, boolean isSelected, boolean hasAnyReaction) {
        String type = reaction[0];
        String emoji = reaction[1];
        String color = reaction[2];

        HBox button = new HBox(3);
        button.setAlignment(Pos.CENTER);
        button.setStyle("-fx-background-radius: 20; -fx-padding: 4 8;");

        // Emoji
        Text emojiText = new Text(emoji);
        emojiText.setStyle("-fx-font-size: 16px;");

        // Count
        Label countLabel = null;
        if (count > 0) {
            countLabel = new Label(String.valueOf(count));
            countLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
                    (isSelected ? color : "#65676B") + ";");
        }

        // Assemble
        button.getChildren().add(emojiText);
        if (countLabel != null) {
            button.getChildren().add(countLabel);
        }

        // Styling
        updateButtonStyle(button, isSelected, hasAnyReaction, color);

        // Hover effects
        button.setOnMouseEntered(e -> updateButtonStyle(button, isSelected, hasAnyReaction, color, true));
        button.setOnMouseExited(e -> updateButtonStyle(button, isSelected, hasAnyReaction, color, false));

        return button;
    }

    private void updateButtonStyle(HBox button, boolean isSelected, boolean hasAnyReaction, String color) {
        updateButtonStyle(button, isSelected, hasAnyReaction, color, false);
    }

    private void updateButtonStyle(HBox button, boolean isSelected, boolean hasAnyReaction, String color, boolean hover) {
        if (isSelected) {
            button.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 20; " +
                    "-fx-padding: 4 8; -fx-border-color: " + color + "; " +
                    "-fx-border-width: 1; -fx-border-radius: 20;");
        } else if (hover && !hasAnyReaction) {
            button.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 20; " +
                    "-fx-padding: 4 8; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-background-radius: 20; " +
                    "-fx-padding: 4 8; -fx-cursor: " + (hasAnyReaction ? "not-allowed" : "hand") + ";");
        }
    }

    private String getEmojiForType(String type) {
        switch (type) {
            case "LIKE": return "👍";
            case "LOVE": return "❤️";
            case "HAHA": return "😂";
            case "WOW": return "😮";
            case "SAD": return "😢";
            case "ANGRY": return "😡";
            default: return "👍";
        }
    }

    private FlowPane createReactionPanel(Reclamation reclamation, Map<String, Integer> reactionCounts) {
        FlowPane reactionPanel = new FlowPane(5, 5);
        reactionPanel.setStyle("-fx-background-color: transparent; -fx-padding: 5;");

        // Définir les types de réactions avec leurs émojis
        String[][] reactions = {
                {"LIKE", "👍", "#4267B2"}, // Bleu Facebook
                {"LOVE", "❤️", "#E53935"}, // Rouge
                {"HAHA", "😂", "#FFD54F"}, // Jaune
                {"WOW", "😮", "#FFB74D"},  // Orange
                {"SAD", "😢", "#64B5F6"},  // Bleu clair
                {"ANGRY", "😡", "#FF7043"} // Orange-rouge
        };

        try {
            for (String[] reactionData : reactions) {
                String type = reactionData[0];
                String emoji = reactionData[1];
                String color = reactionData[2];

                // Vérifier si l'utilisateur a déjà cette réaction
                boolean hasReacted = reactionService.userHasReacted(reclamation.getId(), currentUser, type);

                // Créer le bouton de réaction
                ToggleButton reactionBtn = new ToggleButton();
                reactionBtn.setSelected(hasReacted);

                // Styliser le bouton
                String buttonStyle = "-fx-background-color: transparent; -fx-padding: 2 5; -fx-background-radius: 15;";
                String selectedStyle = "-fx-background-color: #e0e0e0; -fx-padding: 2 5; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);";

                reactionBtn.setStyle(hasReacted ? selectedStyle : buttonStyle);

                // Créer le contenu du bouton avec emoji et compteur
                HBox content = new HBox(3);
                content.setAlignment(javafx.geometry.Pos.CENTER);

                Text emojiText = new Text(emoji);
                emojiText.setStyle("-fx-font-size: 16px;");

                // Créer le label pour le compteur
                Label countLabel = new Label(String.valueOf(reactionCounts.getOrDefault(type, 0)));
                countLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #555;");

                content.getChildren().addAll(emojiText, countLabel);
                reactionBtn.setGraphic(content);

                // Configurer l'action du bouton
                reactionBtn.setOnAction(e -> {
                    try {
                        Reaction reaction = new Reaction();
                        reaction.setReclamationId(reclamation.getId());
                        reaction.setType(type);
                        reaction.setUtilisateur(currentUser);
                        reaction.setDateReaction(LocalDate.now());

                        boolean success = reactionService.addReaction(reaction);

                        if (success) {
                            // Mettre à jour l'interface
                            reactionBtn.setSelected(!reactionBtn.isSelected());
                            reactionBtn.setStyle(reactionBtn.isSelected() ? selectedStyle : buttonStyle);

                            // Rafraîchir les réclamations pour mettre à jour les compteurs
                            loadReclamations();

                            // Notification si ajout d'une réaction
                            NotificationService.getInstance().sendBackendNotification(
                                    "Nouvelle réaction " + emoji + " sur une réponse à la réclamation");
                        }
                    } catch (SQLException ex) {
                        System.err.println("Erreur lors de l'ajout de la réaction: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });

                reactionPanel.getChildren().add(reactionBtn);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des réactions: " + e.getMessage());
            e.printStackTrace();
        }

        return reactionPanel;
    }

    /**
     * Affiche un dialogue pour ajouter une nouvelle réponse à une réclamation
     * @param reclamation La réclamation à laquelle ajouter une réponse
     */
    private void showAddResponseDialog(Reclamation reclamation) {
        // Créer la boîte de dialogue
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une réponse");
        dialog.setHeaderText("Répondre à la réclamation: " + reclamation.getTypeReclamationName());

        // Définir les boutons
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Créer le contenu de la boîte de dialogue
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Résumé de la réclamation
        VBox reclamationSummary = new VBox(5);
        reclamationSummary.setStyle("-fx-background-color: #e7f5ff; -fx-padding: 15; -fx-border-color: #bee5eb; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");

        Label reclamationTitle = new Label("Description de la réclamation:");
        reclamationTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label reclamationDesc = new Label(reclamation.getDescription());
        reclamationDesc.setWrapText(true);

        reclamationSummary.getChildren().addAll(reclamationTitle, reclamationDesc);

        // Zone de texte pour la réponse
        Label responseLabel = new Label("Votre réponse:");
        responseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea responseArea = new TextArea();
        responseArea.setWrapText(true);
        responseArea.setPrefHeight(200);
        responseArea.setPromptText("Saisissez votre réponse ici...");

        // Ajouter les éléments au contenu
        content.getChildren().addAll(reclamationSummary, responseLabel, responseArea);

        // Définir le contenu de la boîte de dialogue
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(500);

        // Activer/désactiver le bouton de sauvegarde en fonction du contenu
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Activer le bouton lorsque le texte est saisi
        responseArea.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty());
        });

        // Convertir le résultat lors de la validation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Reponse reponse = new Reponse();
                reponse.setReclamationId(reclamation.getId());
                reponse.setContenu(responseArea.getText());
                reponse.setDateReponse(LocalDate.now());
                reponse.setUtilisateur(currentUser);
                return reponse;
            }
            return null;
        });

        // Afficher la boîte de dialogue et traiter le résultat
        Optional<Reponse> result = dialog.showAndWait();

        // Dans le bloc qui traite l'ajout d'une nouvelle réponse (dans showAddResponseDialog)
        result.ifPresent(reponse -> {
            try {
                // Sauvegarder la réponse dans la base de données
                if (reponseService.addReponse(reponse)) {
                    // Créer un message de notification
                    String notificationMessage = "Nouvelle réponse à la réclamation: " +
                            (reclamation.getDescription().length() > 20 ?
                                    reclamation.getDescription().substring(0, 20) + "..." :
                                    reclamation.getDescription());

                    // Ajouter la notification à la cloche principale
                    addNotification(notificationMessage);

                    // Également envoyer une notification au système backend si nécessaire
                    NotificationService.getInstance().sendNotification(notificationMessage);

                    // Envoyer un email pour notifier de la nouvelle réponse
                    new Thread(() -> {
                        sendEmailForNewResponse(reclamation, reponse);
                    }).start();

                    // Montrer la réponse ajoutée
                    showExistingResponses(reclamation);
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de l'ajout de la réponse: " + e.getMessage());
                e.printStackTrace();
                showAlert("Erreur", "Impossible d'ajouter la réponse: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Affiche un dialogue pour répondre à une réponse existante
     * @param parentResponse La réponse à laquelle on répond
     */
    private void showReplyDialog(Reponse parentResponse) {
        try {
            // Récupérer la réclamation associée à la réponse
            Reclamation reclamation = service.getReclamationById(parentResponse.getReclamationId());
            if (reclamation == null) {
                showAlert("Erreur", "Impossible de trouver la réclamation associée", Alert.AlertType.ERROR);
                return;
            }

            // Créer le dialogue
            Dialog<Reponse> dialog = new Dialog<>();
            dialog.setTitle("Répondre à un commentaire");
            dialog.setHeaderText("Répondre au commentaire");

            // Définir les boutons
            ButtonType saveButtonType = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Créer le contenu de la boîte de dialogue
            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            // Afficher la réponse parente
            VBox parentResponseBox = new VBox(5);
            parentResponseBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-color: #e0e0e0; " +
                    "-fx-border-radius: 5; -fx-background-radius: 5;");

            Label parentResponseTitle = new Label("En réponse à:");
            parentResponseTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea parentResponseContent = new TextArea(parentResponse.getContenu());
            parentResponseContent.setEditable(false);
            parentResponseContent.setWrapText(true);
            parentResponseContent.setStyle("-fx-control-inner-background: #f8f9fa; -fx-border-color: transparent;");
            parentResponseContent.setPrefHeight(80);

            parentResponseBox.getChildren().addAll(parentResponseTitle, parentResponseContent);

            // Zone de texte pour la réponse
            Label responseLabel = new Label("Votre réponse:");
            responseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea responseArea = new TextArea();
            responseArea.setWrapText(true);
            responseArea.setPrefHeight(150);
            responseArea.setPromptText("Saisissez votre réponse ici...");

            // Ajouter les éléments au contenu
            content.getChildren().addAll(parentResponseBox, responseLabel, responseArea);

            // Définir le contenu de la boîte de dialogue
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(500);

            // Activer/désactiver le bouton de sauvegarde en fonction du contenu
            Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);

            // Activer le bouton lorsque le texte est saisi
            responseArea.textProperty().addListener((observable, oldValue, newValue) -> {
                saveButton.setDisable(newValue.trim().isEmpty());
            });

            // Convertir le résultat lors de la validation
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    Reponse reponse = new Reponse();
                    reponse.setReclamationId(reclamation.getId());
                    // Pour garder une trace de la réponse parente, on peut ajouter une référence dans le contenu
                    // ou utiliser une mise en forme spéciale ou, idéalement, modifier le modèle de données pour supporter les réponses imbriquées
                    String replyText = "En réponse à: \"" +
                            (parentResponse.getContenu().length() > 30 ?
                                    parentResponse.getContenu().substring(0, 30) + "...\"" :
                                    parentResponse.getContenu() + "\"") +
                            "\n\n" + responseArea.getText();

                    reponse.setContenu(replyText);
                    reponse.setDateReponse(LocalDate.now());
                    reponse.setUtilisateur(currentUser);
                    // Idéalement, ajouter un champ parentId à l'entité Reponse pour les réponses imbriquées
                    return reponse;
                }
                return null;
            });

            // Afficher la boîte de dialogue et traiter le résultat
            Optional<Reponse> result = dialog.showAndWait();

            result.ifPresent(reponse -> {
                try {
                    // Sauvegarder la réponse dans la base de données
                    if (reponseService.addReponse(reponse)) {
                        // Envoyer une notification
                        String notificationMessage = "Nouvelle réponse à un commentaire concernant la réclamation: " +
                                (reclamation.getDescription().length() > 20 ?
                                        reclamation.getDescription().substring(0, 20) + "..." :
                                        reclamation.getDescription());

                        NotificationService.getInstance().sendNotification(notificationMessage);

                        // Envoyer un email
                        new Thread(() -> {
                            sendEmailForNewResponse(reclamation, reponse);
                        }).start();

                        // Rafraîchir l'affichage des réponses
                        showExistingResponses(reclamation);
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de l'ajout de la réponse: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible d'ajouter la réponse: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la réclamation: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}