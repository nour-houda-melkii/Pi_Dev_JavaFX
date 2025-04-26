package services;

import entities.NotificationHistory;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;

/**
 * Service pour gérer les notifications des utilisateurs.
 */
public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private Connection connection;

    /**
     * Enum pour les types de notifications
     */
    public enum NotificationType {
        REMINDER, 
        EVENT_CHANGE,
        REGISTRATION,
        CANCELLATION
    }

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
    public boolean checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.info("Réinitialisation de la connexion à la base de données");
                connection = DatabaseConnection.getConnection();
                return true;
            }
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la connexion", e);
            try {
                connection = DatabaseConnection.getConnection();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Impossible de rétablir la connexion", ex);
            }
            return false;
        }
    }

    /**
     * Crée une notification à partir d'un objet NotificationHistory
     * @param notification L'objet NotificationHistory à créer
     * @return La notification créée avec son ID généré
     */
    public NotificationHistory createNotification(NotificationHistory notification) {
        try {
            // Vérifier la connexion
            if (!checkConnection()) {
                LOGGER.severe("Impossible de créer la notification: connexion à la base de données fermée");
                return null;
            }
            
            // Vérifier si cette notification existe déjà pour éviter les doublons
            String checkSql = "SELECT id FROM notification_history " +
                             "WHERE user_id = ? AND event_id = ? AND notification_type = ? " +
                             "AND sent_date >= DATE_SUB(NOW(), INTERVAL 24 HOUR)";
            
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, notification.getUserId());
                checkStmt.setInt(2, notification.getEventId());
                checkStmt.setString(3, notification.getNotificationType());
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Notification similaire trouvée, utiliser son ID
                        int existingId = rs.getInt("id");
                        LOGGER.info("Notification similaire déjà envoyée récemment (ID=" + existingId + ") pour user_id=" + 
                                   notification.getUserId() + ", event_id=" + notification.getEventId() + 
                                   ", type=" + notification.getNotificationType());
                        notification.setId(existingId);
                        return notification;
                    }
                }
            }
            
            // Créer la notification puisqu'elle n'existe pas encore
            String sql = "INSERT INTO notification_history (user_id, event_id, notification_type, details, sent_date, is_read) " +
                        "VALUES (?, ?, ?, ?, NOW(), ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, notification.getUserId());
                stmt.setInt(2, notification.getEventId());
                stmt.setString(3, notification.getNotificationType());
                stmt.setString(4, notification.getDetails());
                stmt.setBoolean(5, false);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    LOGGER.warning("La création de la notification a échoué, aucune ligne affectée");
                    return null;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        notification.setId(id);
                        notification.setSentDate(new Timestamp(System.currentTimeMillis()));
                        LOGGER.info("Notification créée avec succès: " + notification);
                        return notification;
                    } else {
                        LOGGER.warning("La création de la notification a échoué, impossible d'obtenir l'ID");
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'une notification", e);
            reconnect();
            return null;
        }
    }

    /**
     * Reconnecte à la base de données en cas d'erreur
     */
    private void reconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
            connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Échec de la reconnexion à la base de données", e);
        }
    }

    /**
     * Obtient la connexion à la base de données
     * @return La connexion à la base de données
     */
    public Connection getConnection() {
        checkConnection();
        return connection;
    }

    /**
     * Crée une nouvelle notification
     * @param userId ID de l'utilisateur
     * @param eventId ID de l'événement
     * @param type Type de notification
     * @return true si la notification a été créée, false sinon
     */
    public boolean createNotification(int userId, int eventId, NotificationType type) {
        NotificationHistory notification = new NotificationHistory(userId, eventId, type.name(), "");
        NotificationHistory result = createNotification(notification);
        return result != null;
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
        String query = "UPDATE notification_history SET is_read = true, read_date = NOW() WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setInt(1, notificationId);
            
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
                    stmt.setInt(1, notificationId);
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
     * Envoie une notification d'inscription à un événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails de l'inscription
     * @return La notification créée
     */
    public NotificationHistory sendEventRegistrationNotification(int userId, int eventId, String details) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_REGISTRATION, 
                details
        );
        
        return createNotification(notification);
    }
    
    /**
     * Envoie une notification d'annulation d'inscription à un événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails de l'annulation
     * @return La notification créée
     */
    public NotificationHistory sendEventCancellationNotification(int userId, int eventId, String details) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_CANCELLATION, 
                details
        );
        
        return createNotification(notification);
    }
    
    /**
     * Stocke une notification d'inscription dans la base de données sans l'envoyer immédiatement.
     * Cette notification sera envoyée 24 heures avant le début de l'événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails de l'inscription
     * @param eventStartDate La date de début de l'événement pour calculer quand envoyer la notification
     * @return La notification créée et stockée
     */
    public NotificationHistory storeEventRegistrationNotification(int userId, int eventId, String details, LocalDateTime eventStartDate) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_REGISTRATION, 
                details
        );
        
        // Lors de la création, on ne définit pas encore de sent_date pour indiquer que 
        // la notification n'a pas été envoyée, seulement stockée
        try {
            // Vérifier la connexion
            if (!checkConnection()) {
                LOGGER.severe("Impossible de stocker la notification: connexion à la base de données fermée");
                return null;
            }
            
            // Vérifier si cette notification existe déjà pour éviter les doublons
            String checkSql = "SELECT id FROM notification_history " +
                             "WHERE user_id = ? AND event_id = ? AND notification_type = ? " +
                             "AND sent_date IS NULL"; // Chercher les notifications non envoyées
            
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, notification.getUserId());
                checkStmt.setInt(2, notification.getEventId());
                checkStmt.setString(3, notification.getNotificationType());
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Notification similaire trouvée, utiliser son ID
                        int existingId = rs.getInt("id");
                        LOGGER.info("Notification similaire déjà stockée (ID=" + existingId + ") pour user_id=" + 
                                   notification.getUserId() + ", event_id=" + notification.getEventId());
                        notification.setId(existingId);
                        return notification;
                    }
                }
            }
            
            // Créer la notification puisqu'elle n'existe pas encore
            String sql = "INSERT INTO notification_history (user_id, event_id, notification_type, details, is_read) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, notification.getUserId());
                stmt.setInt(2, notification.getEventId());
                stmt.setString(3, notification.getNotificationType());
                stmt.setString(4, notification.getDetails());
                stmt.setBoolean(5, false);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    LOGGER.warning("Le stockage de la notification a échoué, aucune ligne affectée");
                    return null;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        notification.setId(id);
                        LOGGER.info("Notification stockée avec succès (ID=" + id + "), sera envoyée 24h avant l'événement");
                        return notification;
                    } else {
                        LOGGER.warning("Le stockage de la notification a échoué, impossible d'obtenir l'ID");
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du stockage d'une notification", e);
            reconnect();
            return null;
        }
    }
    
    /**
     * Stocke une notification de rappel dans la base de données sans l'envoyer immédiatement.
     * Cette notification sera envoyée exactement 24 heures avant le début de l'événement.
     * 
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @param details Les détails du rappel
     * @param eventStartDate La date de début de l'événement
     * @return La notification stockée
     */
    public NotificationHistory storeScheduledReminder(int userId, int eventId, String details, LocalDateTime eventStartDate) {
        NotificationHistory notification = new NotificationHistory(
                userId, 
                eventId, 
                NotificationHistory.TYPE_REMINDER, 
                details
        );
        
        // Lors de la création, on ne définit pas encore de sent_date pour indiquer que 
        // la notification n'a pas été envoyée, seulement stockée
        try {
            // Vérifier la connexion
            if (!checkConnection()) {
                LOGGER.severe("Impossible de stocker la notification de rappel: connexion à la base de données fermée");
                return null;
            }
            
            // Vérifier si cette notification existe déjà pour éviter les doublons
            String checkSql = "SELECT id FROM notification_history " +
                             "WHERE user_id = ? AND event_id = ? AND notification_type = ? " +
                             "AND sent_date IS NULL"; // Chercher les notifications non envoyées
            
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, notification.getUserId());
                checkStmt.setInt(2, notification.getEventId());
                checkStmt.setString(3, notification.getNotificationType());
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Notification similaire trouvée, utiliser son ID
                        int existingId = rs.getInt("id");
                        LOGGER.info("Notification de rappel similaire déjà stockée (ID=" + existingId + ") pour user_id=" + 
                                   notification.getUserId() + ", event_id=" + notification.getEventId());
                        notification.setId(existingId);
                        return notification;
                    }
                }
            }
            
            // Créer la notification puisqu'elle n'existe pas encore
            String sql = "INSERT INTO notification_history (user_id, event_id, notification_type, details, is_read) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, notification.getUserId());
                stmt.setInt(2, notification.getEventId());
                stmt.setString(3, notification.getNotificationType());
                stmt.setString(4, notification.getDetails());
                stmt.setBoolean(5, false);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    LOGGER.warning("Le stockage de la notification de rappel a échoué, aucune ligne affectée");
                    return null;
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        notification.setId(id);
                        LOGGER.info("Notification de rappel stockée avec succès (ID=" + id + "), sera envoyée exactement 24h avant l'événement");
                        return notification;
                    } else {
                        LOGGER.warning("Le stockage de la notification de rappel a échoué, impossible d'obtenir l'ID");
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du stockage d'une notification de rappel", e);
            reconnect();
            return null;
        }
    }
    
    /**
     * Convertit un ResultSet en objet NotificationHistory.
     * 
     * @param rs Le ResultSet à convertir
     * @return L'objet NotificationHistory résultant
     * @throws SQLException En cas d'erreur SQL
     */
    private NotificationHistory mapResultSetToNotification(ResultSet rs) throws SQLException {
        NotificationHistory notification = new NotificationHistory();
        
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setEventId(rs.getInt("event_id"));
        notification.setNotificationType(rs.getString("notification_type"));
        notification.setDetails(rs.getString("details"));
        notification.setRead(rs.getBoolean("is_read"));
        
        // Gérer de manière sécurisée les timestamps qui pourraient être nulls ou des dates zéro
        try {
            Timestamp sentDate = rs.getTimestamp("sent_date");
            if (sentDate != null) {
                notification.setSentDate(sentDate);
            } else {
                notification.setSentDate(new Timestamp(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            // En cas d'erreur (date zéro), utiliser la date actuelle
            LOGGER.log(Level.WARNING, "Date d'envoi invalide, utilisation de la date actuelle", e);
            notification.setSentDate(new Timestamp(System.currentTimeMillis()));
        }
        
        // Gérer de manière sécurisée le read_date qui pourrait être null ou une date zéro
        try {
            if (notification.isRead()) {
                Timestamp readDate = rs.getTimestamp("read_date");
                if (readDate != null) {
                    notification.setReadDate(readDate);
                }
            }
        } catch (SQLException e) {
            // Ignorer l'erreur si la date de lecture est invalide
            LOGGER.log(Level.WARNING, "Date de lecture invalide, ignorée", e);
        }
        
        return notification;
    }
} 