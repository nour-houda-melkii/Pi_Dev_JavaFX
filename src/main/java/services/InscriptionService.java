package services;

import models.Inscription;
import models.User;
import models.Event;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InscriptionService {
    private Connection connection;
    private UserService userService;
    private EventService eventService;
    
    public InscriptionService() {
        try {
            connection = DatabaseConnection.getConnection();
            userService = new UserService();
            eventService = new EventService();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la connexion à la base de données: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie que la connexion à la base de données est active
     * @return true si la connexion est active, false sinon
     */
    public boolean checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DatabaseConnection.getConnection();
                System.out.println("🔄 Connexion à la base de données réinitialisée dans InscriptionService");
                return connection != null && !connection.isClosed();
            }
            
            // Tester la connexion avec une requête simple
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                stmt.executeQuery();
            } catch (SQLException e) {
                System.out.println("🔄 Connexion à la base de données perdue, tentative de reconnexion");
                connection = DatabaseConnection.getConnection();
            }
            
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Échec de connexion à la base de données dans InscriptionService: " + e.getMessage());
            try {
                connection = DatabaseConnection.getConnection();
                return connection != null && !connection.isClosed();
            } catch (SQLException ex) {
                System.err.println("❌ Échec de reconnexion à la base de données: " + ex.getMessage());
                return false;
            }
        }
    }
    
    public boolean inscrireUtilisateur(int userId, int eventId) {
        if (!checkConnection()) {
            System.err.println("Impossible d'inscrire l'utilisateur: connexion à la base de données fermée");
            return false;
        }
        
        String sql = "INSERT INTO inscription (user_id, event_id, date_inscription, has_unsubscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, eventId);
            statement.setObject(3, LocalDateTime.now());
            statement.setBoolean(4, false);
            int rowsInserted = statement.executeUpdate();
            
            if (rowsInserted > 0) {
                // Inscription réussie, stocker une notification de rappel (à envoyer 24h avant l'événement)
                System.out.println("✅ Inscription réussie, stockage d'une notification de rappel pour envoi 24h avant l'événement");
                
                try {
                    // Récupérer le titre de l'événement
                    Event event = eventService.findById(eventId);
                    if (event != null) {
                        // Créer une notification de rappel
                        NotificationService notificationService = new NotificationService();
                        
                        // Préparer un message de rappel
                        String reminderDetails = "RAPPEL: L'événement \"" + event.getTitle() + "\" commence " + 
                                               "le " + event.getStartDate().toLocalDate() + 
                                               " à " + event.getStartDate().getHour() + "h" + 
                                               (event.getStartDate().getMinute() > 0 ? event.getStartDate().getMinute() : "") +
                                               ". Lieu: " + event.getLocation();
                        
                        // Stocker la notification de rappel sans la date d'envoi (sera envoyée 24h avant l'événement)
                        notificationService.storeScheduledReminder(userId, eventId, reminderDetails, event.getStartDate());
                        System.out.println("📝 Notification de rappel stockée pour envoi 24h avant l'événement");
                        
                        // Déclencher également un check immédiat via le planificateur global
                        try {
                            if (org.example.App.getNotificationScheduler() != null) {
                                org.example.App.getNotificationScheduler().checkSpecificEvent(eventId);
                            }
                        } catch (Exception ex) {
                            System.err.println("Erreur lors de la vérification des notifications après inscription: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du stockage de notification de rappel: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'inscription à l'événement: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtient une description textuelle du temps avant un événement
     */
    private String getTimeDescription(LocalDateTime eventTime) {
        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.temporal.ChronoUnit.HOURS.between(now, eventTime);
        
        if (hours < 1) {
            return "très bientôt";
        } else if (hours < 2) {
            return "dans moins d'une heure";
        } else if (hours < 24) {
            return "aujourd'hui";
        } else if (hours < 48) {
            return "demain";
        } else {
            return "bientôt";
        }
    }
    
    public boolean addInscription(Inscription inscription) {
        if (connection == null) {
            // Mode démo - toujours retourner true si pas de connexion
            System.out.println("Mode démo : ajout d'inscription sans base de données");
            return true;
        }
        
        String sql = "INSERT INTO inscription (user_id, event_id, date_inscription, has_unsubscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, inscription.getUserId());
            statement.setInt(2, inscription.getEventId());
            statement.setObject(3, inscription.getDateInscription());
            statement.setBoolean(4, inscription.isHasUnsubscribed());
            int rowsInserted = statement.executeUpdate();
            
            if (rowsInserted > 0) {
                // Appeler la méthode d'inscription qui gère les notifications
                try {
                    // Récupérer les IDs
                    int userId = inscription.getUserId();
                    int eventId = inscription.getEventId();
                    
                    // Créer une notification de rappel
                    Event event = eventService.findById(eventId);
                    if (event != null) {
                        // Préparer un message de rappel
                        NotificationService notificationService = new NotificationService();
                        String reminderDetails = "RAPPEL: L'événement \"" + event.getTitle() + "\" commence " + 
                                               "le " + event.getStartDate().toLocalDate() + 
                                               " à " + event.getStartDate().getHour() + "h" + 
                                               (event.getStartDate().getMinute() > 0 ? event.getStartDate().getMinute() : "") +
                                               ". Lieu: " + event.getLocation();
                        
                        // Stocker la notification de rappel sans la date d'envoi
                        notificationService.storeScheduledReminder(userId, eventId, reminderDetails, event.getStartDate());
                        System.out.println("📝 Notification de rappel stockée pour envoi 24h avant l'événement");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du stockage de notification de rappel: " + e.getMessage());
                }
            }
            
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'inscription: " + e.getMessage());
            return false;
        }
    }
    
    public boolean desinscrireUtilisateur(int userId, int eventId) {
        String sql = "UPDATE inscription SET has_unsubscribed = true WHERE user_id = ? AND event_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, eventId);
            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la désinscription de l'événement: " + e.getMessage());
            return false;
        }
    }
    
    public boolean unsubscribeFromEvent(int userId, int eventId) {
        if (connection == null) {
            // Mode démo - toujours retourner true si pas de connexion
            System.out.println("Mode démo : désinscription sans base de données");
            return true;
        }
        
        return desinscrireUtilisateur(userId, eventId);
    }
    
    public boolean isUserRegistered(int userId, int eventId) {
        if (connection == null) {
            // Mode démo - toujours retourner false si pas de connexion
            System.out.println("Mode démo : vérification d'inscription sans base de données");
            return false;
        }
        
        String sql = "SELECT * FROM inscription WHERE user_id = ? AND event_id = ? AND has_unsubscribed = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, eventId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'inscription: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isUserRegisteredForEvent(int userId, int eventId) {
        return isUserRegistered(userId, eventId);
    }
    
    public List<Inscription> getInscriptionsByEventId(int eventId) {
        List<Inscription> inscriptions = new ArrayList<>();
        String sql = "SELECT * FROM inscription WHERE event_id = ? AND has_unsubscribed = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Inscription inscription = new Inscription(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("event_id"),
                    rs.getObject("date_inscription", LocalDateTime.class),
                    rs.getBoolean("has_unsubscribed")
                );
                
                // Charger l'utilisateur associé
                User user = userService.getUserById(inscription.getUserId());
                inscription.setUser(user);
                
                // Charger l'événement associé (optionnel)
                Event event = eventService.findById(eventId);
                inscription.setEvent(event);
                
                inscriptions.add(inscription);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des inscriptions: " + e.getMessage());
        }
        return inscriptions;
    }
    
    public List<Inscription> getInscriptionsByUserId(int userId) {
        List<Inscription> inscriptions = new ArrayList<>();
        String sql = "SELECT * FROM inscription WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Inscription inscription = new Inscription(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("event_id"),
                    rs.getObject("date_inscription", LocalDateTime.class),
                    rs.getBoolean("has_unsubscribed")
                );
                inscriptions.add(inscription);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des inscriptions: " + e.getMessage());
        }
        return inscriptions;
    }
    
    /**
     * Vérifie si un utilisateur a déjà une inscription à un événement qui chevauche
     * dans le temps avec l'événement spécifié.
     * 
     * @param userId ID de l'utilisateur
     * @param eventId ID de l'événement auquel l'utilisateur souhaite s'inscrire
     * @return Un objet contenant le résultat de la vérification et l'événement en conflit si trouvé
     */
    public EventOverlapResult checkEventTimeOverlap(int userId, int eventId) {
        if (connection == null) {
            // Mode démo - toujours retourner pas de chevauchement si pas de connexion
            System.out.println("Mode démo : vérification de chevauchement sans base de données");
            return new EventOverlapResult(false, null);
        }
        
        try {
            // Récupérer l'événement auquel l'utilisateur souhaite s'inscrire
            Event newEvent = eventService.findById(eventId);
            if (newEvent == null) {
                return new EventOverlapResult(false, null);
            }
            
            LocalDateTime newEventStart = newEvent.getStartDate();
            LocalDateTime newEventEnd = newEvent.getEndDate();
            
            // Récupérer tous les événements auxquels l'utilisateur est inscrit
            List<Inscription> userInscriptions = getInscriptionsByUserId(userId);
            
            // Filtrer pour garder seulement les inscriptions actives (not unsubscribed)
            userInscriptions = userInscriptions.stream()
                .filter(inscription -> !inscription.isHasUnsubscribed())
                .toList();
            
            for (Inscription inscription : userInscriptions) {
                // Éviter de vérifier le même événement
                if (inscription.getEventId() == eventId) {
                    continue;
                }
                
                // Obtenir les détails de l'événement inscrit
                Event existingEvent = eventService.findById(inscription.getEventId());
                if (existingEvent == null) {
                    continue;
                }
                
                LocalDateTime existingEventStart = existingEvent.getStartDate();
                LocalDateTime existingEventEnd = existingEvent.getEndDate();
                
                // Vérifier si les événements se chevauchent dans le temps
                boolean overlap = (newEventStart.isBefore(existingEventEnd) || newEventStart.isEqual(existingEventEnd)) 
                               && (newEventEnd.isAfter(existingEventStart) || newEventEnd.isEqual(existingEventStart));
                
                if (overlap) {
                    return new EventOverlapResult(true, existingEvent);
                }
            }
            
            // Aucun chevauchement trouvé
            return new EventOverlapResult(false, null);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de chevauchement d'événements: " + e.getMessage());
            e.printStackTrace();
            return new EventOverlapResult(false, null);
        }
    }
    
    /**
     * Classe pour retourner le résultat de la vérification de chevauchement
     */
    public static class EventOverlapResult {
        private final boolean hasOverlap;
        private final Event conflictingEvent;
        
        public EventOverlapResult(boolean hasOverlap, Event conflictingEvent) {
            this.hasOverlap = hasOverlap;
            this.conflictingEvent = conflictingEvent;
        }
        
        public boolean hasOverlap() {
            return hasOverlap;
        }
        
        public Event getConflictingEvent() {
            return conflictingEvent;
        }
    }
} 