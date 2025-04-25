package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.NotificationScheduler;

public class App extends Application {
    private static NotificationScheduler notificationScheduler;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Démarrer le service de notifications
        notificationScheduler = new NotificationScheduler();
        notificationScheduler.start();
        
        // Vérifier immédiatement les événements à venir (pour le test)
        notificationScheduler.forceCheck();
        
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
        super.stop();
    }

    public static NotificationScheduler getNotificationScheduler() {
        return notificationScheduler;
    }

    public static void main(String[] args) {
        launch(args);
    }
}