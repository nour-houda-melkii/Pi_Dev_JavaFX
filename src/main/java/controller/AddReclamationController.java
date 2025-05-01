package controller;

import entity.Reclamation;
import entity.Medecin;
import entity.TypeReclamation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ReclamationServices;
import javafx.collections.ObservableList;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AddReclamationController {
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> medecinComboBox;
    @FXML private ImageView photoView;
    @FXML private Button browseButton;
    @FXML private Button generateDescriptionButton;

    private File selectedFile;
    private final ReclamationServices reclamationService = new ReclamationServices();
    private MainController mainController;

    @FXML
    public void initialize() {
        try {
            initializeComboBoxes(); // Initialise les ComboBox
            datePicker.setValue(LocalDate.now()); // Date par défaut = aujourd'hui

            // Configuration des écouteurs d'événements
            browseButton.setOnAction(event -> browsePhoto());

            // Vérifier si le bouton existe avant d'y ajouter un écouteur
            if (generateDescriptionButton != null) {
                generateDescriptionButton.setOnAction(event -> generateDescription());
            }

        } catch (Exception e) {
            showAlert("Erreur d'initialisation",
                    "Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeMedecinComboBox() {
        try {
            List<Medecin> medecinsList = reclamationService.getAllMedecins();
            ObservableList<String> medecins = FXCollections.observableArrayList(
                    medecinsList.stream()
                            .map(m -> m.getId() + " - " + m.getNom())
                            .collect(Collectors.toList())
            );
            medecinComboBox.setItems(medecins);

            if (!medecins.isEmpty()) {
                medecinComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showAlert("Error", "Error loading doctors: " + e.getMessage());
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void generateDescription() {
        String selectedType = typeComboBox.getValue();
        if (selectedType != null && !selectedType.isEmpty()) {
            String typeName = selectedType.split(" - ")[1];
            descriptionField.setText("Description par défaut pour: " + typeName);
        }
    }

    @FXML
    private void handleAdd() {
        addReclamation();
    }

    @FXML
    private void addReclamation() {
        // Field validation
        if (typeComboBox.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un type de réclamation");
            return;
        }

        if (medecinComboBox.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un médecin");
            return;
        }

        if (descriptionField.getText().trim().isEmpty()) {
            showAlert("Erreur", "La description ne peut pas être vide");
            return;
        }

        if (datePicker.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner une date");
            return;
        }

        try {
            // Extract IDs and names
            String[] typeInfo = typeComboBox.getValue().split(" - ");
            String[] medecinInfo = medecinComboBox.getValue().split(" - ");

            int typeId = Integer.parseInt(typeInfo[0]);
            int medecinId = Integer.parseInt(medecinInfo[0]);
            String photoPath = selectedFile != null ? selectedFile.getAbsolutePath() : null;

            String typeName = typeInfo.length > 1 ? typeInfo[1] : "";
            String medecinName = medecinInfo.length > 1 ? medecinInfo[1] : "";

            // Create reclamation
            Reclamation nouvelleReclamation = new Reclamation();
            nouvelleReclamation.setTypeReclamationId(typeId);
            nouvelleReclamation.setDescription(descriptionField.getText());
            nouvelleReclamation.setDateReclamation(datePicker.getValue());
            nouvelleReclamation.setMedecinId(medecinId);
            nouvelleReclamation.setPhotoPath(photoPath);

            // Add to database
            boolean success = reclamationService.addReclamation(nouvelleReclamation);

            if (success) {
                // Envoyer un email directement ici
                new Thread(() -> {
                    sendEmailNotification(typeName, descriptionField.getText(), medecinName,
                            datePicker.getValue().toString());
                }).start();

                showSuccessAlert("Succès", "Réclamation ajoutée avec succès! Un email a été envoyé.");

                // Refresh main table if mainController is set
                if (mainController != null) {
                    mainController.refreshReclamations();
                    mainController.showNotification("Réclamation ajoutée avec succès", false);
                }

                // Close the window
                ((Stage) typeComboBox.getScene().getWindow()).close();
            } else {
                showAlert("Erreur", "Échec de l'ajout de la réclamation");
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format d'ID invalide: " + e.getMessage());
        } catch (SQLException e) {
            showAlert("Erreur de base de données", "Erreur lors de l'ajout de la réclamation: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Méthode d'envoi d'email directement intégrée
    private void sendEmailNotification(String typeName, String description, String medecinName, String date) {
        try {
            // Configuration pour Gmail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            // Informations d'authentification (utilisez un mot de passe d'application)
            final String username = "sourournajjar2@gmail.com";
            final String password = "runa hfvh forx yjuz"; // Remplacez par votre mot de passe d'application

            // Créer une session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Pour le débogage
            // session.setDebug(true);

            // Créer le contenu de l'email
            String subject = "Nouvelle réclamation: " + typeName;

            StringBuilder messageContent = new StringBuilder();
            messageContent.append("Une nouvelle réclamation a été ajoutée dans le système SAHATECK.\n\n");
            messageContent.append("Détails de la réclamation:\n");
            messageContent.append("---------------------------\n");
            messageContent.append("Type: ").append(typeName).append("\n");
            messageContent.append("Médecin concerné: ").append(medecinName).append("\n");
            messageContent.append("Date: ").append(date).append("\n");
            messageContent.append("Description: ").append(description).append("\n\n");
            messageContent.append("Veuillez consulter l'application SAHATECK pour traiter cette réclamation.");

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("sourournajjar2@gmail.com"));
            message.setSubject(subject);
            message.setText(messageContent.toString());

            // Envoyer le message
            Transport.send(message);
            System.out.println("Email envoyé avec succès!");
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeComboBoxes() {
        try {
            // Initialisation ComboBox types
            List<TypeReclamation> typesList = reclamationService.getAllTypes();
            ObservableList<String> types = FXCollections.observableArrayList();

            for (TypeReclamation type : typesList) {
                types.add(type.getId() + " - " + type.getNom());
            }

            typeComboBox.setItems(types);

            // Initialisation ComboBox médecins
            List<Medecin> medecinsList = reclamationService.getAllMedecins();
            ObservableList<String> medecins = FXCollections.observableArrayList();

            for (Medecin medecin : medecinsList) {
                medecins.add(medecin.getId() + " - " + medecin.getNom());
            }

            medecinComboBox.setItems(medecins);

            // Sélection automatique du premier élément
            if (!types.isEmpty()) {
                typeComboBox.getSelectionModel().selectFirst();
            }
            if (!medecins.isEmpty()) {
                medecinComboBox.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            showAlert("Erreur de base de données",
                    "Impossible de charger les listes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void browsePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers image", "*.png", "*.jpg", "*.jpeg")
        );

        selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                photoView.setImage(image);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void clearFields() {
        typeComboBox.getSelectionModel().clearSelection();
        descriptionField.clear();
        datePicker.setValue(null);
        medecinComboBox.getSelectionModel().clearSelection();
        photoView.setImage(null);
        selectedFile = null;
    }

    @FXML
    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/main-view.fxml"));
            Stage stage = (Stage) typeComboBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de bord");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger le tableau de bord: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}