package services;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    private static NotificationService instance;
    private final List<Consumer<String>> notificationListeners = new ArrayList<>();
    private final List<Consumer<String>> backendNotificationListeners = new ArrayList<>();

    private NotificationService() {
        // Constructeur privé pour Singleton
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    // Pour les notifications générales (utilisées par l'interface principale)
    public void addNotificationListener(Consumer<String> listener) {
        notificationListeners.add(listener);
    }

    // Pour les notifications backend spécifiques
    public void addBackendNotificationListener(Consumer<String> listener) {
        backendNotificationListeners.add(listener);
    }

    // Envoie une notification à tous les écouteurs
    public void sendNotification(String message) {
        System.out.println("Envoi de notification: " + message);

        for (Consumer<String> listener : notificationListeners) {
            // Utiliser Platform.runLater pour les mises à jour d'UI
            Platform.runLater(() -> listener.accept(message));
        }
    }

    // Envoie une notification UNIQUEMENT aux écouteurs backend
    public void sendBackendNotification(String message) {
        System.out.println("Envoi de notification backend UNIQUEMENT: " + message);

        // Vérifions qu'il y a bien des écouteurs backend
        System.out.println("Nombre d'écouteurs backend: " + backendNotificationListeners.size());

        for (Consumer<String> listener : backendNotificationListeners) {
            // Utiliser Platform.runLater pour les mises à jour d'UI
            Platform.runLater(() -> {
                System.out.println("Notification envoyée à un écouteur backend");
                listener.accept(message);
            });
        }
    }

    // Envoie une notification à TOUS les écouteurs (backend et principal)
    public void sendGlobalNotification(String message) {
        sendNotification(message);
        sendBackendNotification(message);
    }


    // Ajouter cette méthode à NotificationService
    public void removeBackendNotificationListener(Consumer<String> listener) {
        backendNotificationListeners.remove(listener);
    }
}