package services;

import entities.NotificationHistory;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour gérer les notifications des utilisateurs.
 */
public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private Connection connection;

    public NotificationService() {
        try {
            connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la connexion à la base de données", e);
        }
    }
    
    /**
     * Vérifie et réinitialise la connexion si nécessaire
     */
    private void checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.info("Réinitialisation de la connexion à la base de données");
                connection = DatabaseConnection.getConnection();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la connexion", e);
            try {
                connection = DatabaseConnection.getConnection();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Impossible de rétablir la connexion", ex);
            }
        }
    }

    /**
     * Crée une nouvelle notification dans la base de données.
     * 
     * @param notification La notification à créer
     * @return La notification créée avec son ID
     */
    public NotificationHistory createNotification(NotificationHistory notification) {
        checkConnection();
        String query = "INSERT INTO notification_history (user_id, event_id, notification_type, details, sent_date, is_read) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, notification.getUserId());
            stmt.setInt(2, notification.getEventId());
            stmt.setString(3, notification.getNotificationType());
            stmt.setString(4, notification.getDetails());
            stmt.setTimestamp(5, notification.getSentDate());
            stmt.setBoolean(6, notification.isRead());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("La création de la notification a échoué, aucune ligne affectée.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la notification a échoué, aucun ID obtenu.");
                }
            }
            
            LOGGER.log(Level.INFO, "Notification créée avec succès: {0}", notification);
            return notification;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la notification", e);
            return null;
        }
    }
    
    /**
     * Récupère toutes les notifications d'un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @return Liste des notifications de l'utilisateur
     */
    public List<NotificationHistory> getNotificationsForUser(int userId) {
        checkConnection();
        List<NotificationHistory> notifications = new ArrayList<>();
        String query = "SELECT * FROM notification_history WHERE user_id = ? ORDER BY sent_date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NotificationHistory notification = mapResultSetToNotification(rs);
                    notifications.add(notification);
                }
            }
            
            LOGGER.log(Level.INFO, "Récupération de {0} notifications pour lutilisateur {1}", 
                    new Object[]{notifications.size(), userId});
            return notifications;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des notifications pour l'utilisateur " + userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère les notifications non lues d'un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @return Liste des notifications non lues
     */
    public List<NotificationHistory> getUnreadNotificationsForUser(int userId) {
        checkConnection();
        List<NotificationHistory> notifications = new ArrayList<>();
        String query = "SELECT * FROM notification_history WHERE user_id = ? AND is_read = false ORDER BY sent_date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NotificationHistory notification = mapResultSetToNotification(rs);
                    notifications.add(notification);
                }
            }
            
            LOGGER.log(Level.INFO, "Récupération de {0} notifications non lues pour lutilisateur {1}", 
                    new Object[]{notifications.size(), userId});
            return notifications;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des notifications non lues pour l'utilisateur " + userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Marque une notification comme lue.
     * 
     * @param notificationId L'ID de la notification
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean markNotificationAsRead(int notificationId) {
        checkConnection();
        String query = "UPDATE notification_history SET is_read = true, read_date = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, notificationId);
            
            int affectedRows = stmt.executeUpdate();
            
            LOGGER.log(Level.INFO, "Notification {0} marquée comme lue: {1} ligne(s) affectée(s)", 
                    new Object[]{notificationId, affectedRows});
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du marquage de la notification " + notificationId + " comme lue", e);
            // Tentative de reconnexion et de réessai
            try {
                checkConnection();
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    stmt.setInt(2, notificationId);
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Échec de la deuxième tentative de marquage", ex);
                return false;
            }
        }
    }
    
    /**
     * Supprime une notification.
     * 
     * @param notificationId L'ID de la notification
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteNotification(int notificationId) {
        checkConnection();
        String query = "DELETE FROM notification_history WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setInt(1, notificationId);
            
            int affectedRows = stmt.executeUpdate();
            
            LOGGER.log(Level.INFO, "Notification {0} supprimée: {1} ligne(s) affectée(s)", 
                    new Object[]{notificationId, affectedRows});
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression de la notification " + notificationId, e);
            return false;
        }
    }
    
    /**
     * Envoie une notification de rappel d'événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails du rappel
     * @return La notification créée
     */
    public NotificationHistory sendEventReminder(int userId, int eventId, String details) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_REMINDER, 
                details
        );
        
        return createNotification(notification);
    }
    
    /**
     * Envoie une notification de changement d'événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails du changement
     * @return La notification créée
     */
    public NotificationHistory sendEventChangeNotification(int userId, int eventId, String details) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_EVENT_CHANGE, 
                details
        );
        
        return createNotification(notification);
    }
    
    /**
     * Convertit un ResultSet en objet NotificationHistory.
     * 
     * @param rs Le ResultSet à convertir
     * @return L'objet NotificationHistory créé
     * @throws SQLException Si une erreur survient lors de l'accès aux données
     */
    private NotificationHistory mapResultSetToNotification(ResultSet rs) throws SQLException {
        NotificationHistory notification = new NotificationHistory();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setEventId(rs.getInt("event_id"));
        notification.setNotificationType(rs.getString("notification_type"));
        notification.setDetails(rs.getString("details"));
        notification.setSentDate(rs.getTimestamp("sent_date"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setReadDate(rs.getTimestamp("read_date"));
        
        return notification;
    }
} 