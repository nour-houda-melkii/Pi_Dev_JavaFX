package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
                dateFin.setStyle("-fx-border-color: red;");
                dateDebut.setStyle("-fx-border-color: red;");
            } else {
                dateFin.setStyle("");
                dateDebut.setStyle("");
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

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();

        // Validation du titre
        if (fieldTitre.getText() == null || fieldTitre.getText().trim().isEmpty()) {
            errors.add("Le titre est obligatoire");
        }

        // Validation des dates
        if (dateDebut.getValue() == null || dateFin.getValue() == null) {
            errors.add("Les dates sont obligatoires");
        } else {
            LocalDateTime start = LocalDateTime.of(dateDebut.getValue(), 
                LocalTime.of(hourStartSpinner.getValue(), minuteStartSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(dateFin.getValue(), 
                LocalTime.of(hourEndSpinner.getValue(), minuteEndSpinner.getValue()));

            if (start.isAfter(end)) {
                errors.add("La date de début doit être avant la date de fin");
            }
        }

        // Validation des coordonnées
        try {
            double lat = Double.parseDouble(fieldLatitude.getText());
            double lon = Double.parseDouble(fieldLongitude.getText());
            
            if (lat < -90 || lat > 90) {
                errors.add("La latitude doit être entre -90 et 90");
            }
            if (lon < -180 || lon > 180) {
                errors.add("La longitude doit être entre -180 et 180");
            }
        } catch (NumberFormatException e) {
            errors.add("Les coordonnées doivent être des nombres valides");
        }

        // Validation des places
        try {
            int places = Integer.parseInt(fieldPlaces.getText());
            if (places < 0) {
                errors.add("Le nombre de places doit être positif");
            }
        } catch (NumberFormatException e) {
            errors.add("Le nombre de places doit être un nombre valide");
        }

        // Validation de la catégorie
        if (comboCategorie.getValue() == null) {
            errors.add("La catégorie est obligatoire");
        }

        return errors;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    @FXML
    private void handleSave(ActionEvent e) {
        List<String> errors = validateForm();
        if (!errors.isEmpty()) {
            showError(String.join("\n", errors));
            return;
        }

        try {
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

            if (event == null) event = new Event();

            event.setTitle(title);
            event.setDescription(description);
            event.setStartDate(start);
            event.setEndDate(end);
            event.setLocation(lieu);
            event.setLatitude(lat);
            event.setLongitude(lon);
            event.setPlacesDisponibles(places);
            event.setCategorie(categorie);

            if (selectedImageFile != null) {
                String extension = selectedImageFile.getName().substring(selectedImageFile.getName().lastIndexOf("."));
                String uniqueName = UUID.randomUUID().toString() + extension;
                Path dest = Paths.get(IMAGE_DIR + uniqueName);
                Files.copy(selectedImageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                event.setAffiche(uniqueName);
            }

            if (event.getId() == 0) {
                eventDAO.insert(event);
            } else {
                eventDAO.update(event);
            }

            if (onFormSubmitted != null) onFormSubmitted.accept(null);
            closeForm(e);
        } catch (Exception ex) {
            showError("Une erreur est survenue lors de la sauvegarde : " + ex.getMessage());
        }
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