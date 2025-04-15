package controller;

import entity.TypeReclamation;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.TypeReclamationService;
import java.sql.SQLException;

public class EditTypeController {
    @FXML private TextField nomField;

    private TypeReclamation typeToEdit;
    private final TypeReclamationService service = new TypeReclamationService();
    private TypeReclamationController parentController;

    public void setTypeToEdit(TypeReclamation type) {
        this.typeToEdit = type;
        nomField.setText(type.getNom());
    }

    public void setParentController(TypeReclamationController controller) {
        this.parentController = controller;
    }

    @FXML
    private void handleSave() {
        if (typeToEdit == null) return;

        typeToEdit.setNom(nomField.getText());

        try {
            service.updateType(typeToEdit);
            if (parentController != null) {
                parentController.refreshTable();
            }

            closeWindow();
        } catch (SQLException e) {
            showAlert("Erreur", "Échec de la mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}