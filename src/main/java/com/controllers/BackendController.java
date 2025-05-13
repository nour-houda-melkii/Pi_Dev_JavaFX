package controller;

import com.models.Reaction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.NotificationService;
import services.ReactionNotificationService;
import services.ReactionNotificationService.ReactionNotificationListener;

import javafx.event.ActionEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BackendController implements ReactionNotificationService.ReactionNotificationListener {
    @FXML private TableView<?> reclamationTable;
    @FXML private VBox contentArea;

    // Boutons de navigation
    @FXML private Button tableauDeBordBtn;
    @FXML private Button gestionProduitsBtn;
    @FXML private Button gestionUtilisateursBtn;

    @FXML
    private ListView<String> notificationListView;

    @FXML
    private Label statusLabel;
    @FXML private Label notificationBadge;
    @FXML
    private Button notificationBellButton;

    private int notificationCount = 0;
    private List<String> pendingNotifications = new ArrayList<>();
    @FXML private Label notificationCountLabel;
    private final List<String> notifications = new LinkedList<>();
    private final int MAX_NOTIFICATIONS = 100;

    @FXML
    public void initialize() {
        // Vérifier que les éléments sont correctement injectés
        if (tableauDeBordBtn != null) {
            setActiveButton(tableauDeBordBtn);
            loadTypeReclamationView();

            tableauDeBordBtn.setOnAction(e -> {
                setActiveButton(tableauDeBordBtn);
                loadTypeReclamationView();
            });
        }

        if (gestionProduitsBtn != null) {
            gestionProduitsBtn.setOnAction(e -> {
                setActiveButton(gestionProduitsBtn);
                loadReclamationView();
            });
        }

        if (gestionUtilisateursBtn != null) {
            gestionUtilisateursBtn.setOnAction(e -> {
                setActiveButton(gestionUtilisateursBtn);
                loadReponseView();
            });
        }
        ReactionNotificationService.getInstance().addAdminListener(this);
        NotificationService.getInstance().addBackendNotificationListener(this::handleBackendNotification);
        updateNotificationCount();

        if (statusLabel != null) {
            statusLabel.setText("Prêt à recevoir des notifications");
        }

        if (notificationListView != null) {
            notificationListView.getItems().clear();
        }
        updateNotificationBadge();
        System.out.println("Initialize du BackendController démarré");
        System.out.println("notificationBellButton : " + (notificationBellButton != null ? "OK" : "NULL"));
        System.out.println("notificationCountLabel : " + (notificationCountLabel != null ? "OK" : "NULL"));

        if (notificationBellButton != null) {
            notificationBellButton.setOnAction(e -> handleBackendNotificationBell(e));
        }
    }


    @Override
    public void onReactionAdded(Reaction reaction, String message) {
        // Utiliser Platform.runLater pour s'assurer que les mises à jour de l'interface se font sur le thread JavaFX
        Platform.runLater(() -> {
            System.out.println("BackendController: notification reçue pour la cloche: " + message);

            // Incrémenter le compteur de notifications
            notificationCount++;

            // Ajouter à la liste des notifications en attente
            pendingNotifications.add(message);

            // Ajouter à la liste des notifications pour l'affichage
            notifications.add(0, message);

            // Limiter le nombre de notifications
            if (notifications.size() > MAX_NOTIFICATIONS) {
                notifications.remove(notifications.size() - 1);
            }

            // Mettre à jour l'affichage du compteur
            if (notificationCountLabel != null) {
                notificationCountLabel.setText(String.valueOf(notificationCount));
                notificationCountLabel.setVisible(true);
                System.out.println("Badge de notification mis à jour: " + notificationCount);
            } else {
                System.out.println("ERREUR: notificationCountLabel est null!");
            }

            // Mise à jour des autres éléments d'interface
            updateNotificationBadge();
            updateNotificationList();

            // Mettre à jour le statut si disponible
            if (statusLabel != null) {
                statusLabel.setText("Dernière notification reçue: " + reaction.getDateReaction());
            }
        });
    }


    private void handleBackendNotification(String message) {
        System.out.println("BackendController: notification backend reçue: " + message);

        // Incrémenter le compteur de notifications
        notificationCount++;

        // Ajouter le message à la liste des notifications en attente
        pendingNotifications.add(message);

        // Mettre à jour l'UI depuis le thread approprié
        Platform.runLater(() -> {
            System.out.println("BackendController: mise à jour de l'UI pour la notification");
            updateNotificationCount();
            updateNotificationBadge();
        });
    }


    private void updateNotificationCount() {
        if (notificationCountLabel != null) {
            if (notificationCount > 0) {
                notificationCountLabel.setText(String.valueOf(notificationCount));
                notificationCountLabel.setVisible(true);
            } else {
                notificationCountLabel.setVisible(false);
            }
        }
    }

    private void updateNotificationBadge() {
        if (notificationBadge != null) {
            if (notificationCount > 0) {
                notificationBadge.setText(String.valueOf(notificationCount));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        }
    }

    private void setActiveButton(Button activeButton) {
        // Vérifier que les boutons ne sont pas null avant de leur appliquer un style
        if (tableauDeBordBtn != null) {
            tableauDeBordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(135,206,235);");
        }
        if (gestionProduitsBtn != null) {
            gestionProduitsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(135,206,235);");
        }
        if (gestionUtilisateursBtn != null) {
            gestionUtilisateursBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgb(135,206,235);");
        }

        // Style du bouton actif (seulement s'il n'est pas null)
        if (activeButton != null) {
            activeButton.setStyle("-fx-background-color: #f0f7ff; -fx-text-fill: #2c3e50; -fx-border-color: #00b2ff; -fx-border-width: 0 0 0 3;");
        }
    }

    private void loadTypeReclamationView() {
        if (contentArea == null) {
            showAlert("Erreur", "La zone de contenu n'est pas disponible", Alert.AlertType.ERROR);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/type-reclamation.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de la vue des types de réclamation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadReclamationView() {
        if (contentArea == null) {
            showAlert("Erreur", "La zone de contenu n'est pas disponible", Alert.AlertType.ERROR);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Reclamation-view.fxml"));
            Node node = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de la vue des réclamations: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadReponseView() {
        if (contentArea == null) {
            showAlert("Erreur", "La zone de contenu n'est pas disponible", Alert.AlertType.ERROR);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reponse-view.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de la vue des réponses: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBackendNotificationBell(ActionEvent event) {
        System.out.println("Cloche de notification backend cliquée");
        if (pendingNotifications.isEmpty()) {
            showAlert("Notifications", "Aucune nouvelle notification", Alert.AlertType.INFORMATION);
            return;
        }

        // Créer une boîte de dialogue pour afficher les notifications
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
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Réinitialiser le compteur à la fermeture
        dialog.setOnCloseRequest(e -> {
            notificationCount = 0;
            pendingNotifications.clear();
            updateNotificationCount();
        });

        dialog.showAndWait();
    }

    private void updateNotificationList() {
        if (notificationListView != null) {
            notificationListView.getItems().clear();
            notificationListView.getItems().addAll(notifications);
        }
    }

    @FXML
    public void showNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/notification-view.fxml"));
            Parent root = loader.load();

            NotificationViewController controller = loader.getController();
            controller.setNotifications(notifications);

            Stage stage = new Stage();
            stage.setTitle("Notifications");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Réinitialiser le compteur après avoir affiché les notifications
            notificationCount = 0;
            updateNotificationBadge();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'afficher les notifications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void addNotification(String message) {
        System.out.println("BackendController: addNotification appelée avec message: " + message);

        notificationCount++;
        pendingNotifications.add(message);

        // Mettre à jour l'UI depuis le thread approprié
        Platform.runLater(() -> updateNotificationCount());
    }


    private void resetNotificationCount() {
        notificationCount = 0;
        pendingNotifications.clear();
        updateNotificationCount();
    }


    @FXML
    private void clearNotifications() {
        notifications.clear();
        notificationCount = 0;
        updateNotificationList();
        updateNotificationBadge();

        if (statusLabel != null) {
            statusLabel.setText("Notifications effacées");
        }
    }

    public void shutdown() {
        // Se désabonner du service de notification
        NotificationService.getInstance().removeBackendNotificationListener(this::addNotification);
    }

    @FXML
    private void testBackendNotification() {
        // Créer une réaction de test
        Reaction testReaction = new Reaction();
        testReaction.setType("TEST");
        testReaction.setUtilisateur("Utilisateur test");
        testReaction.setDateReaction(LocalDate.now());

        // Envoyer une notification de test
        ReactionNotificationService.getInstance().notifyAdminsOnly(
                testReaction,
                "Ceci est une notification de test pour le backend"
        );
    }

}