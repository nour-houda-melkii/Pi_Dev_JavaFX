package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class BackendController {
    @FXML private VBox contentArea;
    @FXML private Button typeReclamationBtn;
    @FXML private Button reclamationBtn;
    @FXML private Button reponseBtn; // Ajout du bouton Réponses

    @FXML
    public void initialize() {
        // Set initial active button
        setActiveButton(typeReclamationBtn);
        loadTypeReclamationView();

        // Set button actions
        typeReclamationBtn.setOnAction(e -> {
            setActiveButton(typeReclamationBtn);
            loadTypeReclamationView();
        });

        reclamationBtn.setOnAction(e -> {
            setActiveButton(reclamationBtn);
            loadReclamationView();
        });

        // Ajout du gestionnaire pour le bouton Réponses
        reponseBtn.setOnAction(e -> {
            setActiveButton(reponseBtn);
            loadReponseView();
        });
    }

    private void setActiveButton(Button activeButton) {
        // Reset all buttons
        typeReclamationBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;");
        reclamationBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;");
        reponseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;");

        // Set active button style
        activeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;");
    }

    private void loadTypeReclamationView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/type-reclamation.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors du chargement de la vue des types de réclamation", Alert.AlertType.ERROR);
        }
    }

    private void loadReclamationView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reclamation-view.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors du chargement de la vue des réclamations", Alert.AlertType.ERROR);
        }
    }

    private void loadReponseView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reponse-view.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors du chargement de la vue des réponses", Alert.AlertType.ERROR);
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