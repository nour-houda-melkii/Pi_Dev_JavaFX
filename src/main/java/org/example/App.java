package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.NotificationScheduler;
import services.EventArchiverService;

public class App extends Application {
    private static NotificationScheduler notificationScheduler;
    private static EventArchiverService eventArchiverService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Démarrer le service de notifications
        notificationScheduler = new NotificationScheduler();
        
        // S'assurer que le scheduler est initialisé correctement avant de l'utiliser
        try {
            notificationScheduler.start();
            // Logger le démarrage du scheduler
            System.out.println("✅ Notification scheduler démarré avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage du scheduler de notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Démarrer le service d'archivage automatique des événements
        eventArchiverService = new EventArchiverService();
        
        try {
            eventArchiverService.start();
            System.out.println("✅ Service d'archivage automatique démarré avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage du service d'archivage: " + e.getMessage());
            e.printStackTrace();
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        
        primaryStage.setTitle("Connexion - Gestion d'Événements");
        primaryStage.setScene(scene);
        primaryStage.setWidth(500);
        primaryStage.setHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Arrêter le service de notifications quand l'application se ferme
        if (notificationScheduler != null) {
            notificationScheduler.stop();
        }
        
        // Arrêter le service d'archivage
        if (eventArchiverService != null) {
            eventArchiverService.stop();
        }
        
        super.stop();
    }

    public static NotificationScheduler getNotificationScheduler() {
        return notificationScheduler;
    }
    
    public static EventArchiverService getEventArchiverService() {
        return eventArchiverService;
    }

    public static void main(String[] args) {
        launch(args);
    }
}