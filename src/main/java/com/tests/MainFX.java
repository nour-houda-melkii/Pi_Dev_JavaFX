package com.tests;

import com.controllers.LoginController;
import com.services.AuthService;
import com.utils.ImageSynchronizer;
import com.event.services.EventArchiverService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    // Service d'archivage automatique des événements
    private EventArchiverService eventArchiverService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize the image synchronizer for event posters
        ImageSynchronizer.initialize();
        
        // Initialiser et démarrer le service d'archivage des événements
        eventArchiverService = new EventArchiverService();
        eventArchiverService.start();
        System.out.println("Service d'archivage des événements démarré avec succès");
        
        // Charger le fichier FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
        Parent root = loader.load();

        // Créer la scène avec la taille 1000x700
        Scene scene = new Scene(root, 1000, 700);

        // Appliquer la feuille de style CSS
        scene.getStylesheets().add(getClass().getResource("/styles/styleLogin/login.css").toExternalForm());

        // Configurer la fenêtre principale
        primaryStage.setTitle("SAHATECH - Solutions Technologiques");
        primaryStage.setScene(scene);

        // Empêcher le redimensionnement si nécessaire
        // primaryStage.setResizable(false);

        // Afficher la fenêtre
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        // Arrêter le service d'archivage des événements
        if (eventArchiverService != null) {
            eventArchiverService.stop();
            System.out.println("Service d'archivage des événements arrêté avec succès");
        }
        
        // Shutdown the image synchronizer when application closes
        ImageSynchronizer.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}