package com.controllers;

import com.models.User;
import com.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UnverifiedDoctorsController implements Initializable {

    @FXML private ListView<User> unverifiedDoctorsListView;
    @FXML private VBox mainContainer;

    private final UserService userService = new UserService();
    private final ObservableList<User> unverifiedDoctorsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        loadUnverifiedDoctors();
    }

    private void setupListView() {
        unverifiedDoctorsListView.setCellFactory(param -> new ListCell<User>() {
            private final GridPane gridPane = new GridPane();
            private final HBox headerBox = new HBox();
            private final Text nameText = new Text();
            private final Label statusLabel = new Label("Unverified");
            private final Text emailText = new Text();
            private final Text phoneText = new Text();
            private final Text specialiteText = new Text();
            private final Text licenceText = new Text();
            private final HBox actionBox = new HBox(5);
            private final Button viewButton = new Button();
            private final Button verifyButton = new Button();
            private final Button deleteButton = new Button();

            {
                // Configuration du GridPane
                gridPane.setHgap(10);
                gridPane.setVgap(5);
                gridPane.setPadding(new Insets(10));
                gridPane.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

                // Configuration de l'en-tête
                headerBox.setSpacing(10);
                headerBox.setAlignment(Pos.CENTER_LEFT);
                nameText.setFont(Font.font("Poppins", FontWeight.BOLD, 14));
                nameText.setFill(Color.web("#1e293b"));

                statusLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 12));
                statusLabel.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 2 8; -fx-background-radius: 9999;");
                headerBox.getChildren().addAll(nameText, statusLabel);

                // Style des textes
                emailText.setFont(Font.font("Poppins", 12));
                emailText.setFill(Color.web("#64748b"));
                phoneText.setFont(Font.font("Poppins", 12));
                phoneText.setFill(Color.web("#64748b"));
                specialiteText.setFont(Font.font("Poppins", 12));
                specialiteText.setFill(Color.web("#64748b"));
                licenceText.setFont(Font.font("Poppins", 12));
                licenceText.setFill(Color.web("#64748b"));

                // Configuration des boutons d'action
                viewButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-min-width: 32; -fx-min-height: 32; -fx-background-radius: 8;");
                verifyButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-min-width: 32; -fx-min-height: 32; -fx-background-radius: 8;");
                deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-min-width: 32; -fx-min-height: 32; -fx-background-radius: 8;");

                // Icônes des boutons (remplacer par vos propres icônes)
                viewButton.setGraphic(new Text("👁"));
                verifyButton.setGraphic(new Text("✓"));
                deleteButton.setGraphic(new Text("✕"));

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                actionBox.getChildren().addAll(viewButton, verifyButton, deleteButton);

                // Ajout des éléments au GridPane
                gridPane.add(headerBox, 0, 0, 2, 1);
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
                    viewButton.setOnAction(event -> handleViewDoctor(medecin));
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

    private void handleViewDoctor(User medecin) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Doctor Details");
        infoAlert.setHeaderText("Details for Dr. " + medecin.getFirstName() + " " + medecin.getLastName());
        infoAlert.setContentText(
                "Email: " + medecin.getEmail() + "\n" +
                        "Phone: " + medecin.getPhoneNumber() + "\n" +
                        "Speciality: " + medecin.getSpecialite() + "\n" +
                        "Licence: " + medecin.getNumeroLicence()
        );
        infoAlert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");
        infoAlert.showAndWait();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ListMedecin.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) unverifiedDoctorsListView.getScene().getWindow();
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