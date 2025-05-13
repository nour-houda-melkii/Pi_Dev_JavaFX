package com.controllers;

import com.models.User;
import com.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ListPatient implements Initializable {

    @FXML private ListView<User> patientListView;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button addButton;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox mainContainer;
    @FXML private HBox paginationContainer;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;

    private final UserService userService = new UserService();
    private final ObservableList<User> patientList = FXCollections.observableArrayList();
    private FilteredList<User> filteredPatients;
    private final Executor executor = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    // Variables pour la pagination
    private int currentPage = 0;
    private final int itemsPerPage = 5;
    private int totalPages = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStyles();
        setupListView();
        setupDynamicSearch();
        setupPaginationControls();
        loadPatients();
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
        searchField.setPromptText("Search for a patient...");

        // Style des boutons
        searchButton.setStyle("-fx-background-color: #00B4D8; -fx-text-fill: white; -fx-font-family: 'Poppins';");
        addButton.setStyle("-fx-background-color: #00B4D8; -fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");

        // Style des boutons de pagination
        prevPageButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-family: 'Poppins';");
        nextPageButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-family: 'Poppins';");
        pageInfoLabel.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 14px;");
    }

    private void setupPaginationControls() {
        prevPageButton.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                updatePatientsView();
            }
        });

        nextPageButton.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updatePatientsView();
            }
        });
    }

    private void setupDynamicSearch() {
        filteredPatients = new FilteredList<>(patientList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredPatients.setPredicate(patient -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (patient.getFirstName() != null && patient.getFirstName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (patient.getLastName() != null && patient.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (patient.getEmail() != null && patient.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (patient.getPhoneNumber() != null && patient.getPhoneNumber().contains(lowerCaseFilter)) {
                    return true;
                } else if (patient.getAddress() != null && patient.getAddress().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });

            // Réinitialiser à la première page après une recherche
            currentPage = 0;
            updatePatientsView();
        });

        patientListView.setItems(filteredPatients);
    }

    private void setupListView() {
        patientListView.setCellFactory(param -> new ListCell<User>() {
            private final GridPane gridPane = new GridPane();
            private final Text nameText = new Text();
            private final Text emailText = new Text();
            private final Text phoneText = new Text();
            private final Text addressText = new Text();
            private final HBox actionBox = new HBox(5);
            private final Button viewButton = new Button();
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();

            {
                gridPane.setHgap(10);
                gridPane.setVgap(5);
                gridPane.setPadding(new Insets(10));

                nameText.setFont(Font.font("Poppins", FontWeight.BOLD, 14));
                emailText.setFont(Font.font("Poppins", 12));
                phoneText.setFont(Font.font("Poppins", 12));
                addressText.setFont(Font.font("Poppins", 12));

                viewButton.setGraphic(new Text("\uD83D\uDC41"));
                viewButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");

                editButton.setGraphic(new Text("\u270E"));
                editButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");

                deleteButton.setGraphic(new Text("\uD83D\uDDD1"));
                deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                actionBox.getChildren().addAll(viewButton, editButton, deleteButton);

                gridPane.add(nameText, 0, 0, 2, 1);
                gridPane.add(new Text("Email:"), 0, 1);
                gridPane.add(emailText, 1, 1);
                gridPane.add(new Text("Phone:"), 0, 2);
                gridPane.add(phoneText, 1, 2);
                gridPane.add(new Text("Address:"), 0, 3);
                gridPane.add(addressText, 1, 3);
                gridPane.add(actionBox, 2, 0, 1, 4);

                ColumnConstraints col1 = new ColumnConstraints();
                col1.setHgrow(Priority.NEVER);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setHgrow(Priority.ALWAYS);
                ColumnConstraints col3 = new ColumnConstraints();
                col3.setHgrow(Priority.NEVER);
                gridPane.getColumnConstraints().addAll(col1, col2, col3);

                setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                setPadding(new Insets(5));
            }

            @Override
            protected void updateItem(User patient, boolean empty) {
                super.updateItem(patient, empty);

                if (empty || patient == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    nameText.setText(patient.getFirstName() + " " + patient.getLastName());
                    emailText.setText(patient.getEmail() != null ? patient.getEmail() : "Not specified");
                    phoneText.setText(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "Not specified");
                    addressText.setText(patient.getAddress() != null ? patient.getAddress() : "Not specified");

                    viewButton.setOnAction(event -> handleViewPatient(patient));
                    editButton.setOnAction(event -> handleEditPatient(patient));
                    deleteButton.setOnAction(event -> handleDeletePatient(patient));

                    setGraphic(gridPane);
                }
            }
        });
    }

    private void loadPatients() {
        executor.execute(() -> {
            try {
                // Charger tous les patients (pour le filtrage)
                List<User> allPatients = userService.rechercherTousPatients();

                // Calculer le nombre total de pages
                totalPages = (int) Math.ceil((double) allPatients.size() / itemsPerPage);

                // Mettre à jour l'interface
                javafx.application.Platform.runLater(() -> {
                    patientList.setAll(allPatients);
                    updatePatientsView();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        showAlert("Error", "Failed to load patients: " + e.getMessage()));
            }
        });
    }

    private void updatePatientsView() {
        // Calculer l'index de début et de fin pour la page courante
        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredPatients.size());

        // Créer une sous-liste pour la page courante
        List<User> pagePatients = filteredPatients.subList(fromIndex, toIndex);

        // Mettre à jour la ListView
        patientListView.setItems(FXCollections.observableArrayList(pagePatients));

        // Mettre à jour les informations de pagination
        updatePaginationInfo();
    }

    private void updatePaginationInfo() {
        pageInfoLabel.setText(String.format("Page %d of %d", currentPage + 1, totalPages));

        // Désactiver les boutons si nécessaire
        prevPageButton.setDisable(currentPage <= 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1 || totalPages == 0);
    }

    @FXML
    private void handleSearch() {
        // Le filtrage est déjà géré par le listener
        currentPage = 0;
        updatePatientsView();
    }

    @FXML
    private void handleAddPatient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AjouterPatient.fxml"));
            Parent root = loader.load();

            AjouterPatient controller = loader.getController();
            controller.setUserService(userService);
            controller.setOnPatientAddedCallback(() -> {
                refreshPatientList();
            });

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(addButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("Add New Patient");
            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Unable to open patient form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshPatientList() {
        executor.execute(() -> {
            List<User> patients = userService.rechercherTousPatients();
            javafx.application.Platform.runLater(() -> {
                patientList.setAll(patients);
                totalPages = (int) Math.ceil((double) filteredPatients.size() / itemsPerPage);
                updatePatientsView();
            });
        });
    }

    private void handleViewPatient(User patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/DetailsPatient.fxml"));
            Parent root = loader.load();

            DetailsPatientController controller = loader.getController();
            controller.initData(patient.getId());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Patient Details - " + patient.getFirstName());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open patient details");
        }
    }

    private void handleEditPatient(User patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/UpdatePatient.fxml"));
            Parent root = loader.load();

            UpdatePatientController controller = loader.getController();
            controller.initData(patient);
            controller.setOnPatientUpdatedCallback(() -> {
                refreshPatientList();
            });

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(patientListView.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Patient - " + patient.getFirstName());
            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Could not open edit window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeletePatient(User patient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete confirmation");
        alert.setHeaderText("Delete the patient");
        alert.setContentText("Are you sure you want to delete? " + patient.getFirstName() + " " + patient.getLastName() + "?");
        alert.getDialogPane().setStyle("-fx-font-family: 'Poppins'");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executor.execute(() -> {
                    try {
                        userService.supprimerPatient(patient.getId());
                        javafx.application.Platform.runLater(() -> {
                            patientList.remove(patient);
                            totalPages = (int) Math.ceil((double) filteredPatients.size() / itemsPerPage);
                            updatePatientsView();
                            showAlert("Success", "Patient successfully deleted.");
                        });
                    } catch (Exception e) {
                        javafx.application.Platform.runLater(() ->
                                showAlert("Error", "Failed to delete patient: " + e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadPatients();
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
        // Pour l'injection de dépendance si nécessaire
    }
}