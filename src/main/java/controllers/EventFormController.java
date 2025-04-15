package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.CategorieEvent;
import models.Event;
import services.CategorieEventDAO;
import services.EventDAO;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EventFormController {

    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private Spinner<Integer> hourStartSpinner;
    @FXML private Spinner<Integer> minuteStartSpinner;
    @FXML private Spinner<Integer> hourEndSpinner;
    @FXML private Spinner<Integer> minuteEndSpinner;
    @FXML private TextField fieldLieu;
    @FXML private TextField fieldLatitude;
    @FXML private TextField fieldLongitude;
    @FXML private TextField fieldPlaces;
    @FXML private ComboBox<CategorieEvent> comboCategorie;
    @FXML private Label labelImagePath;
    @FXML private ImageView imagePreview;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label errorLabel;
    @FXML private Label titreError;
    @FXML private Label descriptionError;
    @FXML private Label dateDebutError;
    @FXML private Label dateFinError;
    @FXML private Label lieuError;
    @FXML private Label latitudeError;
    @FXML private Label longitudeError;
    @FXML private Label placesError;
    @FXML private Label categorieError;
    @FXML private Label imageError;

    private final EventDAO eventDAO = new EventDAO();
    private final CategorieEventDAO categorieDAO = new CategorieEventDAO();

    private Event event;
    private Consumer<Void> onFormSubmitted;
    private File selectedImageFile;
    private final String IMAGE_DIR = "src/main/resources/affiches/";

    @FXML
    public void initialize() {
        // Initialiser les spinners d'heure/minute
        hourStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minuteStartSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        hourEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minuteEndSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Charger les catégories actives
        List<CategorieEvent> categories = categorieDAO.getAllActive();
        comboCategorie.setItems(FXCollections.observableArrayList(categories));

        // Initialiser les validations
        setupValidations();

        // Forcer l'affichage des boutons
        if (btnSave != null) btnSave.setVisible(true);
        if (btnCancel != null) btnCancel.setVisible(true);
    }

    private void setupValidations() {
        // Validation du titre
        fieldTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.trim().isEmpty()) {
                fieldTitre.setStyle("-fx-border-color: red;");
            } else {
                fieldTitre.setStyle("");
            }
        });

        // Validation des dates
        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());

        // Validation des coordonnées
        fieldLatitude.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinates());
        fieldLongitude.textProperty().addListener((obs, oldVal, newVal) -> validateCoordinates());

        // Validation des places
        fieldPlaces.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int places = Integer.parseInt(newVal);
                if (places < 0) {
                    fieldPlaces.setStyle("-fx-border-color: red;");
                } else {
                    fieldPlaces.setStyle("");
                }
            } catch (NumberFormatException e) {
                fieldPlaces.setStyle("-fx-border-color: red;");
            }
        });
    }

    private void validateDates() {
        if (dateDebut.getValue() != null && dateFin.getValue() != null) {
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));

            if (start.isAfter(end)) {
                dateDebut.setStyle("-fx-border-color: red;");
                dateFin.setStyle("-fx-border-color: red;");
            } else {
                dateDebut.setStyle("");
                dateFin.setStyle("");
            }
        }
    }

    private void validateCoordinates() {
        try {
            double lat = Double.parseDouble(fieldLatitude.getText());
            double lon = Double.parseDouble(fieldLongitude.getText());
            
            if (lat < -90 || lat > 90) {
                fieldLatitude.setStyle("-fx-border-color: red;");
            } else {
                fieldLatitude.setStyle("");
            }
            
            if (lon < -180 || lon > 180) {
                fieldLongitude.setStyle("-fx-border-color: red;");
            } else {
                fieldLongitude.setStyle("");
            }
        } catch (NumberFormatException e) {
            fieldLatitude.setStyle("-fx-border-color: red;");
            fieldLongitude.setStyle("-fx-border-color: red;");
        }
    }

    public void setEvent(Event event) {
        this.event = event;

        if (event != null) {
            fieldTitre.setText(event.getTitle());
            fieldDescription.setText(event.getDescription());
            dateDebut.setValue(event.getStartDate().toLocalDate());
            hourStartSpinner.getValueFactory().setValue(event.getStartDate().getHour());
            minuteStartSpinner.getValueFactory().setValue(event.getStartDate().getMinute());
            dateFin.setValue(event.getEndDate().toLocalDate());
            hourEndSpinner.getValueFactory().setValue(event.getEndDate().getHour());
            minuteEndSpinner.getValueFactory().setValue(event.getEndDate().getMinute());
            fieldLieu.setText(event.getLocation());
            fieldLatitude.setText(String.valueOf(event.getLatitude()));
            fieldLongitude.setText(String.valueOf(event.getLongitude()));
            fieldPlaces.setText(String.valueOf(event.getPlacesDisponibles()));
            comboCategorie.setValue(event.getCategorie());

            if (event.getAffiche() != null) {
                labelImagePath.setText(event.getAffiche());
                File imgFile = new File(IMAGE_DIR + event.getAffiche());
                if (imgFile.exists()) {
                    imagePreview.setImage(new Image(imgFile.toURI().toString()));
                }
            }
        }
    }

    public void setOnFormSubmitted(Consumer<Void> callback) {
        this.onFormSubmitted = callback;
    }

    @FXML
    private void handleChooseImage(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            // Vérifier la taille du fichier (max 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                showError("L'image ne doit pas dépasser 5MB");
                return;
            }

            selectedImageFile = file;
            labelImagePath.setText(file.getName());
            imagePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleRetourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event_list.fxml"));
            Parent eventList = loader.load();
            
            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) fieldTitre.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(eventList);
            }
        } catch (IOException e) {
            showError("Erreur lors du retour à la liste : " + e.getMessage());
        }
    }

    @FXML
    private void handleSave(ActionEvent e) {
        // Réinitialiser les erreurs
        clearErrors();

        // Valider le formulaire
        List<String> errors = validateForm();
        if (!errors.isEmpty()) {
            return;
        }

        try {
            // Récupérer les valeurs du formulaire
            String title = fieldTitre.getText();
            String description = fieldDescription.getText();
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));
            String lieu = fieldLieu.getText();
            double lat = Double.parseDouble(fieldLatitude.getText());
            double lon = Double.parseDouble(fieldLongitude.getText());
            int places = Integer.parseInt(fieldPlaces.getText());
            CategorieEvent categorie = comboCategorie.getValue();

            // Créer ou mettre à jour l'événement
            if (event == null) {
                event = new Event();
            }

            event.setTitle(title);
            event.setDescription(description);
            event.setStartDate(start);
            event.setEndDate(end);
            event.setLocation(lieu);
            event.setLatitude(lat);
            event.setLongitude(lon);
            event.setPlacesDisponibles(places);
            event.setCategorie(categorie);

            // Gérer l'image si elle a été sélectionnée
            if (selectedImageFile != null) {
                String extension = selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf("."));
                String uniqueName = UUID.randomUUID().toString() + extension;
                Path dest = Paths.get(IMAGE_DIR + uniqueName);
                Files.copy(selectedImageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                event.setAffiche(uniqueName);
            }

            // Sauvegarder dans la base de données
            if (event.getId() == 0) {
                eventDAO.insert(event);
            } else {
                eventDAO.update(event);
            }

            // Notifier le callback et retourner à la liste
            if (onFormSubmitted != null) {
                onFormSubmitted.accept(null);
            }
            
            // Retourner à la liste des événements
            handleRetourListe();
        } catch (Exception ex) {
            showError("Une erreur est survenue lors de la sauvegarde : " + ex.getMessage());
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearErrors() {
        titreError.setVisible(false);
        titreError.setManaged(false);
        descriptionError.setVisible(false);
        descriptionError.setManaged(false);
        dateDebutError.setVisible(false);
        dateDebutError.setManaged(false);
        dateFinError.setVisible(false);
        dateFinError.setManaged(false);
        lieuError.setVisible(false);
        lieuError.setManaged(false);
        latitudeError.setVisible(false);
        latitudeError.setManaged(false);
        longitudeError.setVisible(false);
        longitudeError.setManaged(false);
        placesError.setVisible(false);
        placesError.setManaged(false);
        categorieError.setVisible(false);
        categorieError.setManaged(false);
        imageError.setVisible(false);
        imageError.setManaged(false);

        // Réinitialiser les styles des champs
        fieldTitre.setStyle("");
        fieldDescription.setStyle("");
        dateDebut.setStyle("");
        dateFin.setStyle("");
        fieldLieu.setStyle("");
        fieldLatitude.setStyle("");
        fieldLongitude.setStyle("");
        fieldPlaces.setStyle("");
        comboCategorie.setStyle("");
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        clearErrors();

        // Validation du titre
        if (fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            showFieldError(titreError, "Le titre est obligatoire");
            fieldTitre.setStyle("-fx-border-color: red;");
            errors.add("Le titre est obligatoire");
        }

        // Validation des dates
        if (dateDebut.getValue() == null) {
            showFieldError(dateDebutError, "La date de début est obligatoire");
            dateDebut.setStyle("-fx-border-color: red;");
            errors.add("La date de début est obligatoire");
        }

        if (dateFin.getValue() == null) {
            showFieldError(dateFinError, "La date de fin est obligatoire");
            dateFin.setStyle("-fx-border-color: red;");
            errors.add("La date de fin est obligatoire");
        }

        if (dateDebut.getValue() != null && dateFin.getValue() != null) {
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));

            if (start.isAfter(end)) {
                showFieldError(dateDebutError, "La date de début doit être avant la date de fin");
                dateDebut.setStyle("-fx-border-color: red;");
                dateFin.setStyle("-fx-border-color: red;");
                errors.add("La date de début doit être avant la date de fin");
            }
        }

        // Validation du lieu
        if (fieldLieu.getText() == null || fieldLieu.getText().trim().isEmpty()) {
            showFieldError(lieuError, "Le lieu est obligatoire");
            fieldLieu.setStyle("-fx-border-color: red;");
            errors.add("Le lieu est obligatoire");
        }

        // Validation des coordonnées
        try {
            double lat = Double.parseDouble(fieldLatitude.getText());
            if (lat < -90 || lat > 90) {
                showFieldError(latitudeError, "La latitude doit être entre -90 et 90");
                fieldLatitude.setStyle("-fx-border-color: red;");
                errors.add("La latitude doit être entre -90 et 90");
            }
        } catch (NumberFormatException e) {
            showFieldError(latitudeError, "La latitude doit être un nombre valide");
            fieldLatitude.setStyle("-fx-border-color: red;");
            errors.add("La latitude doit être un nombre valide");
        }

        try {
            double lon = Double.parseDouble(fieldLongitude.getText());
            if (lon < -180 || lon > 180) {
                showFieldError(longitudeError, "La longitude doit être entre -180 et 180");
                fieldLongitude.setStyle("-fx-border-color: red;");
                errors.add("La longitude doit être entre -180 et 180");
            }
        } catch (NumberFormatException e) {
            showFieldError(longitudeError, "La longitude doit être un nombre valide");
            fieldLongitude.setStyle("-fx-border-color: red;");
            errors.add("La longitude doit être un nombre valide");
        }

        // Validation des places
        try {
            int places = Integer.parseInt(fieldPlaces.getText());
            if (places < 0) {
                showFieldError(placesError, "Le nombre de places doit être positif");
                fieldPlaces.setStyle("-fx-border-color: red;");
                errors.add("Le nombre de places doit être positif");
            }
        } catch (NumberFormatException e) {
            showFieldError(placesError, "Le nombre de places doit être un nombre valide");
            fieldPlaces.setStyle("-fx-border-color: red;");
            errors.add("Le nombre de places doit être un nombre valide");
        }

        // Validation de la catégorie
        if (comboCategorie.getValue() == null) {
            showFieldError(categorieError, "La catégorie est obligatoire");
            comboCategorie.setStyle("-fx-border-color: red;");
            errors.add("La catégorie est obligatoire");
        }

        return errors;
    }

    @FXML
    private void handleCancel(ActionEvent e) {
        closeForm(e);
    }

    private void closeForm(ActionEvent e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.close();
    }
}