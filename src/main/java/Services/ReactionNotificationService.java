package services;

import entity.Reaction;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ReactionNotificationService {
    private static ReactionNotificationService instance;

    // Séparation en deux listes d'écouteurs distinctes
    private final List<ReactionNotificationListener> userListeners = new ArrayList<>();
    private final List<ReactionNotificationListener> adminListeners = new ArrayList<>();

    public interface ReactionNotificationListener {
        void onReactionAdded(Reaction reaction, String message);
    }

    private ReactionNotificationService() {}

    public static synchronized ReactionNotificationService getInstance() {
        if (instance == null) {
            instance = new ReactionNotificationService();
        }
        return instance;
    }

    // Pour l'interface utilisateur
    public void addUserListener(ReactionNotificationListener listener) {
        userListeners.add(listener);
    }

    public void removeUserListener(ReactionNotificationListener listener) {
        userListeners.remove(listener);
    }

    public void removeAdminListener(ReactionNotificationListener listener) {
        adminListeners.remove(listener);
    }

    // Notifications uniquement pour les utilisateurs
    public void notifyUsersOnly(Reaction reaction, String message) {
        System.out.println("Notification UTILISATEUR uniquement: " + message);

        for (ReactionNotificationListener listener : userListeners) {
            listener.onReactionAdded(reaction, message);
        }
    }

    // Notifications pour tous (ancienne méthode)
    public void notifyAll(Reaction reaction, String message) {
        System.out.println("Notification GLOBALE: " + message);

        // Notifier les utilisateurs
        for (ReactionNotificationListener listener : userListeners) {
            listener.onReactionAdded(reaction, message);
        }

        // Notifier les admins
        for (ReactionNotificationListener listener : adminListeners) {
            listener.onReactionAdded(reaction, message);
        }
    }

    public void notifyBellOfNewReaction(Reaction reaction, String emoji, String userName) {
        System.out.println("Notification UNIQUEMENT BACKEND: " + emoji + " par " + userName);

        // Créer un message formaté pour l'affichage dans la cloche backend
        String message = "Nouvelle réaction " + emoji + " sur une réponse à la réclamation";

        // Notifier UNIQUEMENT les écouteurs admin
        notifyAdminsOnly(reaction, message);

        // Ne PAS appeler d'autres méthodes de notification
    }


    private final List<Pair<Reaction, String>> pendingAdminNotifications = new ArrayList<>();

    public void notifyAdminsOnly(Reaction reaction, String message) {
        System.out.println("Notification ADMIN uniquement: " + message + " (Nombre d'écouteurs: " + adminListeners.size() + ")");

        if (adminListeners.isEmpty()) {
            // Aucun écouteur admin, stocker la notification pour plus tard
            System.out.println("Aucun écouteur admin disponible, stockage de la notification pour plus tard");
            pendingAdminNotifications.add(new Pair<>(reaction, message));
            return;
        }

        // Envoyer la notification à tous les écouteurs admin
        for (ReactionNotificationListener listener : adminListeners) {
            try {
                listener.onReactionAdded(reaction, message);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi d'une notification: " + e.getMessage());
            }
        }
    }

    // Méthode pour vérifier les notifications en attente
    public void checkPendingNotifications() {
        if (!pendingAdminNotifications.isEmpty() && !adminListeners.isEmpty()) {
            System.out.println("Traitement des " + pendingAdminNotifications.size() + " notifications admin en attente");

            for (Pair<Reaction, String> notification : pendingAdminNotifications) {
                for (ReactionNotificationListener listener : adminListeners) {
                    try {
                        listener.onReactionAdded(notification.getKey(), notification.getValue());
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'envoi d'une notification en attente: " + e.getMessage());
                    }
                }
            }

            pendingAdminNotifications.clear();
        }
    }

    // Appeler cette méthode après l'ajout d'un écouteur
    public void addAdminListener(ReactionNotificationListener listener) {
        adminListeners.add(listener);
        System.out.println("Ajout d'un écouteur admin pour les réactions - Total: " + adminListeners.size());

        // Vérifier s'il y a des notifications en attente
        checkPendingNotifications();
    }

}