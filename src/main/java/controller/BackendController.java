package controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;


public class BackendController {
    @FXML
    private TableView<?> adminTable;
    @FXML
    private VBox contentArea;

    @FXML
    public void initialize() {
        System.out.println("Interface admin chargée avec succès !");
        loadTypeReclamationView();
    }

    private void loadTypeReclamationView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/type-reclamation.fxml"));
            Parent typeReclamationView = loader.load();
            contentArea.getChildren().setAll(typeReclamationView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}