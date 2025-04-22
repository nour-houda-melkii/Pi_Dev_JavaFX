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

public class ListMedecin implements Initializable {

    @FXML private ListView<User> medecinListView;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button addButton;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox mainContainer;

    private final UserService userService = new UserService();
    private final ObservableList<User> medecinList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStyles();
        setupListView();
        loadMedecins();
    }

    private void setupStyles() {
        // Style du conteneur principal
        mainContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20;");

        // Style du titre
        titleLabel.setFont(Font.font("Poppins", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#012970"));

        // Style du sous-titre
        subtitleLabel.setFont(Font.font("Poppins", FontWeight.LIGHT, 13));
        subtitleLabel.setTextFill(Color.web("#6c757d"));

        // Style de la barre de recherche
        searchField.setStyle("-fx-font-family: 'Poppins'; -fx-font-weight: 200; -fx-font-size: 13px;");
        searchField.setPromptText("Search for a doctor...");

        // Style des boutons
        searchButton.setStyle("-fx-background-color: #00B4D8; -fx-text-fill: white; -fx-font-family: 'Poppins';");
        addButton.setStyle("-fx-background-color: #00B4D8; -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");
    }

    private void setupListView() {
        medecinListView.setCellFactory(param -> new ListCell<User>() {
            private final GridPane gridPane = new GridPane();
            private final Text nameText = new Text();
            private final Text emailText = new Text();
            private final Text phoneText = new Text();
            private final Text specialiteText = new Text();
            private final Text licenceText = new Text();
            private final HBox actionBox = new HBox(5);
            private final Button viewButton = new Button();
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();

            {
                // Configuration du GridPane
                gridPane.setHgap(10);
                gridPane.setVgap(5);
                gridPane.setPadding(new Insets(10));

                // Style des textes
                nameText.setFont(Font.font("Poppins", FontWeight.BOLD, 14));
                emailText.setFont(Font.font("Poppins", 12));
                phoneText.setFont(Font.font("Poppins", 12));
                specialiteText.setFont(Font.font("Poppins", 12));
                licenceText.setFont(Font.font("Poppins", 12));

                // Configuration des boutons d'action
                viewButton.setGraphic(new Text("\uD83D\uDC41")); // Icône œil
                viewButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");

                editButton.setGraphic(new Text("\u270E")); // Icône crayon
                editButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");

                deleteButton.setGraphic(new Text("\uD83D\uDDD1")); // Icône poubelle
                deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                actionBox.getChildren().addAll(viewButton, editButton, deleteButton);

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
                setPadding(new Insets(5));
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
                    viewButton.setOnAction(event -> handleViewMedecin(medecin));
                    editButton.setOnAction(event -> handleEditMedecin(medecin));
                    deleteButton.setOnAction(event -> handleDeleteMedecin(medecin));

                    setGraphic(gridPane);
                }
            }
        });
    }

    private void loadMedecins() {
        medecinList.clear();
        medecinList.addAll(userService.rechercherTousMedecins());
        medecinListView.setItems(medecinList);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            medecinListView.setItems(medecinList);
            return;
        }

        ObservableList<User> filteredList = FXCollections.observableArrayList();
        for (User medecin : medecinList) {
            if ((medecin.getFirstName() != null && medecin.getFirstName().toLowerCase().contains(keyword)) ||
                    (medecin.getLastName() != null && medecin.getLastName().toLowerCase().contains(keyword)) ||
                    (medecin.getEmail() != null && medecin.getEmail().toLowerCase().contains(keyword)) ||
                    (medecin.getPhoneNumber() != null && medecin.getPhoneNumber().contains(keyword)) ||
                    (medecin.getSpecialite() != null && medecin.getSpecialite().toString().toLowerCase().contains(keyword)) ||
                    (medecin.getNumeroLicence() != null && medecin.getNumeroLicence().toLowerCase().contains(keyword))) {
                filteredList.add(medecin);
            }
        }
        medecinListView.setItems(filteredList);
    }

    @FXML
    private void handleAddMedecin() {
        try {
            // Charger le fichier FXML de la page AjouterMedecin
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AjouterMedecin.fxml"));
            Parent root = loader.load();

            // Créer une nouvelle scène
            Scene scene = new Scene(root);

            // Obtenir la fenêtre actuelle
            Stage stage = (Stage) addButton.getScene().getWindow();

            // Changer la scène
            stage.setScene(scene);
            stage.setTitle("Add Doctor");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open the doctor addition page.");
        }
    }

    private void handleViewMedecin(User medecin) {
        try {
            // Charger la vue des détails
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/DetailsMedecin.fxml"));
            Parent root = loader.load();

            // Passer les données au contrôleur
            DetailsMedecinController controller = loader.getController();
            controller.initData(medecin.getId());

            // Créer une nouvelle scène
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Doctor Details - " + medecin.getFirstName());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open doctor details");
        }
    }

    private void handleEditMedecin(User medecin) {
        try {
            // Charger le fichier FXML de la page de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/UpdateMedecin.fxml"));
            Parent root = loader.load();

            // Obtenir le contrôleur et initialiser les données du médecin
            UpdateMedecinController controller = loader.getController();
            controller.initData(medecin);

            // Créer une nouvelle scène
            Scene scene = new Scene(root);

            // Obtenir la fenêtre actuelle
            Stage stage = (Stage) medecinListView.getScene().getWindow();

            // Changer la scène
            stage.setScene(scene);
            stage.setTitle("Edit Doctor");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open the doctor edit page.");
        }
    }

    private void handleDeleteMedecin(User medecin) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete confirmation");
        alert.setHeaderText("Delete the doctor");
        alert.setContentText("Are you sure you want to delete Dr. " + medecin.getFirstName() + " " + medecin.getLastName() + "?");
        alert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.supprimerMedecin(medecin.getId());
                medecinList.remove(medecin);
                showAlert("Success", "Doctor successfully deleted.");
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadMedecins();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");
        alert.showAndWait();
    }

    public void setUserService(UserService userService) {
    }
}