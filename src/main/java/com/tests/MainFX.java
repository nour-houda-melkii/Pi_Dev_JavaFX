package com.tests;

import com.controllers.LoginController;
import com.services.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le fichier FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/frontoffice.fxml"));
        Parent root = loader.load();

        // Créer la scène avec la taille 1000x700
        Scene scene = new Scene(root, 1000, 700);

        // Appliquer la feuille de style CSS
        scene.getStylesheets().add(getClass().getResource("/styles/styleFrontOffice/styles.css").toExternalForm());

        // Configurer la fenêtre principale
        primaryStage.setTitle("SAHATECH - Solutions Technologiques");
        primaryStage.setScene(scene);

        // Empêcher le redimensionnement si nécessaire
        // primaryStage.setResizable(false);

        // Afficher la fenêtre
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}