package controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import models.CategorieEvent;
import services.CategorieEventDAO;

import java.util.function.Consumer;

public class CategorieEventFormController {

    @FXML private TextField fieldNom;
    @FXML private TextArea fieldDescription;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private CategorieEvent categorie;
    private Consumer<Void> onFormSubmitted;
    private final CategorieEventDAO categorieDAO = new CategorieEventDAO();

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

        if (onFormSubmitted != null) onFormSubmitted.accept(null);
        closeForm();
    }

    @FXML
    private void handleCancel() {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}