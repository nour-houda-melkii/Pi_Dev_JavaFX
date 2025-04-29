package services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    // Singleton pattern
    private static NotificationService instance;

    private List<Consumer<String>> listeners = new ArrayList<>();

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    // Ajouter un listener pour les notifications
    public void addNotificationListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    // Supprimer un listener
    public void removeNotificationListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    // Envoyer une notification à tous les listeners
    public void sendNotification(String message) {
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }
}