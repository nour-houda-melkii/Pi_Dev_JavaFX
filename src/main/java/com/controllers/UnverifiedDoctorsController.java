package com.controllers;

import com.models.User;
import com.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UnverifiedDoctorsController implements Initializable {

    @FXML private ListView<User> unverifiedDoctorsListView;
    @FXML private Label titleLabel;
    @FXML private VBox mainContainer;

    private final UserService userService = new UserService();
    private final ObservableList<User> unverifiedDoctorsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStyles();
        setupListView();
        loadUnverifiedDoctors();
    }

    private void setupStyles() {
        mainContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20;");
        titleLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
    }

    private void setupListView() {
        unverifiedDoctorsListView.setCellFactory(param -> new ListCell<User>() {
            private final GridPane gridPane = new GridPane();
            private final Text nameText = new Text();
            private final Text emailText = new Text();
            private final Text phoneText = new Text();
            private final Text specialiteText = new Text();
            private final Text licenceText = new Text();
            private final HBox actionBox = new HBox(5);
            private final Button verifyButton = new Button("Verify");
            private final Button deleteButton = new Button("Delete");

            {
                // Configuration du GridPane
                gridPane.setHgap(10);
                gridPane.setVgap(5);

                // Style des textes
                nameText.setFont(Font.font("Poppins", FontWeight.BOLD, 14));
                emailText.setFont(Font.font("Poppins", 12));
                phoneText.setFont(Font.font("Poppins", 12));
                specialiteText.setFont(Font.font("Poppins", 12));
                licenceText.setFont(Font.font("Poppins", 12));

                // Configuration des boutons d'action
                verifyButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-family: 'Poppins';");
                deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-family: 'Poppins';");

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                actionBox.getChildren().addAll(verifyButton, deleteButton);

                // Ajout des éléments au GridPane
                gridPane.add(nameText, 0, 0, 2, 1);
                gridPane.add(new Text("Email:"), 0, 1);
                gridPane.add(emailText, 1, 1);
                gridPane.add(new Text("Phone:"), 0, 2);
                gridPane.add(phoneText, 1, 2);
                gridPane.add(new Text("Speciality:"), 0, 3);
                gridPane.add(specialiteText, 1, 3);
                gridPane.add(new Text("Licence:"), 0, 4);
                gridPane.add(licenceText, 1, 4);
                gridPane.add(actionBox, 2, 0, 1, 5);

                // Configuration des contraintes de colonne
                ColumnConstraints col1 = new ColumnConstraints();
                col1.setHgrow(Priority.NEVER);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setHgrow(Priority.ALWAYS);
                ColumnConstraints col3 = new ColumnConstraints();
                col3.setHgrow(Priority.NEVER);
                gridPane.getColumnConstraints().addAll(col1, col2, col3);

                // Style de la cellule
                setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
            }

            @Override
            protected void updateItem(User medecin, boolean empty) {
                super.updateItem(medecin, empty);

                if (empty || medecin == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    nameText.setText(medecin.getFirstName() + " " + medecin.getLastName());
                    emailText.setText(medecin.getEmail() != null ? medecin.getEmail() : "Not specified");
                    phoneText.setText(medecin.getPhoneNumber() != null ? medecin.getPhoneNumber() : "Not specified");
                    specialiteText.setText(medecin.getSpecialite() != null ? medecin.getSpecialite().toString() : "Not specified");
                    licenceText.setText(medecin.getNumeroLicence() != null ? medecin.getNumeroLicence() : "Not specified");

                    // Gestion des événements des boutons
                    verifyButton.setOnAction(event -> handleVerifyDoctor(medecin));
                    deleteButton.setOnAction(event -> handleDeleteDoctor(medecin));

                    setGraphic(gridPane);
                }
            }
        });
    }

    private void loadUnverifiedDoctors() {
        unverifiedDoctorsList.clear();
        unverifiedDoctorsList.addAll(userService.rechercherMedecinsNonVerifies());
        unverifiedDoctorsListView.setItems(unverifiedDoctorsList);
    }

    private void handleVerifyDoctor(User medecin) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Verify Doctor");
        confirmAlert.setHeaderText("Verify this doctor");
        confirmAlert.setContentText("Are you sure you want to verify Dr. " + medecin.getFirstName() + " " + medecin.getLastName() + "?");
        confirmAlert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = userService.verifierMedecin(medecin.getId());

                if (success) {
                    // Mettre à jour l'objet local
                    medecin.setStatus("verifie");

                    // Rafraîchir la liste
                    unverifiedDoctorsList.remove(medecin);

                    showAlert("Success", "Doctor successfully verified and added to the system.");
                } else {
                    showAlert("Error", "Failed to verify doctor. Please try again.");
                }
            }
        });
    }

    private void handleDeleteDoctor(User medecin) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete confirmation");
        alert.setHeaderText("Delete the doctor");
        alert.setContentText("Are you sure you want to delete Dr. " + medecin.getFirstName() + " " + medecin.getLastName() + "?");
        alert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.supprimerMedecin(medecin.getId());
                unverifiedDoctorsList.remove(medecin);
                showAlert("Success", "Doctor successfully deleted.");
            }
        });
    }

    @FXML
    private void handleBackToList() {
        try {
            // Charger le fichier FXML de la liste principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ListMedecin.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle scène
            Scene scene = new Scene(root);

            // Obtenir la fenêtre actuelle
            Stage stage = (Stage) unverifiedDoctorsListView.getScene().getWindow();

            // Changer la scène
            stage.setScene(scene);
            stage.setTitle("Doctors List");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to return to the doctors list.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");
        alert.showAndWait();
    }


}