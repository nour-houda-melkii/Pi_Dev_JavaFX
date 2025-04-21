package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import models.CategorieEvent;
import services.CategorieEventDAO;

import java.io.IOException;
import java.util.function.Consumer;

public class CategorieEventFormController {

    @FXML private TextField fieldNom;
    @FXML private TextArea fieldDescription;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label nomError;
    @FXML private Label descriptionError;

    private CategorieEvent categorie;
    private Consumer<Void> onFormSubmitted;
    private final CategorieEventDAO categorieDAO = new CategorieEventDAO();

    @FXML
    public void initialize() {
        // Initialiser les validations
        setupValidations();
    }

    private void setupValidations() {
        // Validation du nom (ne doit pas être vide)
        fieldNom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.trim().isEmpty()) {
                fieldNom.setStyle("-fx-border-color: red;");
                if (nomError != null) {
                    nomError.setText("Le nom est obligatoire");
                    nomError.setVisible(true);
                }
            } else {
                fieldNom.setStyle("");
                if (nomError != null) {
                    nomError.setVisible(false);
                }
            }
        });
    }

    public void setCategorie(CategorieEvent categorie) {
        this.categorie = categorie;

        if (categorie != null) {
            fieldNom.setText(categorie.getNom());
            fieldDescription.setText(categorie.getDescription());
        }
    }

    public void setOnFormSubmitted(Consumer<Void> callback) {
        this.onFormSubmitted = callback;
    }

    @FXML
    private void handleSave() {
        // Valider le formulaire
        if (!validateForm()) {
            return;
        }

        String nom = fieldNom.getText();
        String description = fieldDescription.getText();

        if (categorie == null) {
            categorie = new CategorieEvent();
        }

        categorie.setNom(nom);
        categorie.setDescription(description);
        categorie.setArchived(false);

        if (categorie.getId() == 0) {
            categorieDAO.insert(categorie);
        } else {
            categorieDAO.update(categorie);
        }

        if (onFormSubmitted != null) {
            onFormSubmitted.accept(null);
        }
        
        handleRetourListe();
    }

    private boolean validateForm() {
        boolean isValid = true;
        
        // Effacer les styles d'erreur précédents
        fieldNom.setStyle("");
        if (nomError != null) nomError.setVisible(false);
        
        // Vérifier que le nom n'est pas vide
        if (fieldNom.getText() == null || fieldNom.getText().trim().isEmpty()) {
            fieldNom.setStyle("-fx-border-color: red;");
            if (nomError != null) {
                nomError.setText("Le nom est obligatoire");
                nomError.setVisible(true);
            } else {
                // Si le label d'erreur n'est pas disponible, afficher une alerte
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Erreur de validation");
                alert.setHeaderText("Formulaire incomplet");
                alert.setContentText("Le nom de la catégorie est obligatoire.");
                alert.showAndWait();
            }
            isValid = false;
        }
        
        return isValid;
    }

    @FXML
    private void handleCancel() {
        handleRetourListe();
    }

    @FXML
    private void handleRetourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/categorieEvent_list.fxml"));
            Parent listPage = loader.load();
            
            // Obtenir la référence au BorderPane principal
            BorderPane mainContent = (BorderPane) fieldNom.getScene().getRoot().lookup("#contentArea");
            if (mainContent != null) {
                mainContent.setCenter(listPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}