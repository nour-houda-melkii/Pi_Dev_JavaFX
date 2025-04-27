package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class BackendController {
    @FXML private TableView<?> reclamationTable;
    @FXML private VBox contentArea;

    // Boutons de navigation
    @FXML private Button tableauDeBordBtn;
    @FXML private Button gestionProduitsBtn;
    @FXML private Button gestionUtilisateursBtn;

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
    }

    private void setActiveButton(Button activeButton) {
        // Vérifier que les boutons ne sont pas null avant de leur appliquer un style
        if (tableauDeBordBtn != null) {
            tableauDeBordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a5a5a;");
        }
        if (gestionProduitsBtn != null) {
            gestionProduitsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a5a5a;");
        }
        if (gestionUtilisateursBtn != null) {
            gestionUtilisateursBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a5a5a;");
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
}