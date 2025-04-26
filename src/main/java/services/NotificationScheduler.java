package services;

import entities.NotificationHistory;
import models.Event;
import models.Inscription;
import models.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service pour planifier les notifications automatiques avant les événements
 */
public class NotificationScheduler {
    private static final Logger LOGGER = Logger.getLogger(NotificationScheduler.class.getName());
    private static final long CHECK_INTERVAL = 60 * 60 * 1000; // Vérifier toutes les heures
    
    private final EventService eventService;
    private final InscriptionService inscriptionService;
    private final NotificationService notificationService;
    private Timer timer;
    private final AtomicBoolean isChecking = new AtomicBoolean(false);
    
    /**
     * Constructeur qui initialise les services nécessaires
     */
    public NotificationScheduler() {
        this.eventService = new EventService();
        this.inscriptionService = new InscriptionService();
        this.notificationService = new NotificationService();
    }
    
    /**
     * Démarre le planificateur de notifications
     */
    public void start() {
        if (timer != null) {
            stop(); // Arrêter l'ancien timer s'il existe pour éviter les doublons
            LOGGER.info("Ancien planificateur arrêté avant redémarrage");
        }
        
        timer = new Timer(true); // Crée un timer en tant que thread daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkUpcomingEvents();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la vérification des événements", e);
                }
            }
        }, 0, CHECK_INTERVAL); // Commence immédiatement, puis toutes les heures
        
        LOGGER.info("Planificateur de notifications démarré");
        
        // Forcer une vérification immédiate au démarrage
        forceCheck();
    }
    
    /**
     * Arrête le planificateur de notifications
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            LOGGER.info("Planificateur de notifications arrêté");
        }
    }
    
    /**
     * Vérifie tous les événements à venir et crée des notifications le cas échéant
     */
    private void checkUpcomingEvents() {
        // Vérifier si déjà en cours d'exécution pour éviter les doublons
        if (!isChecking.compareAndSet(false, true)) {
            LOGGER.info("Une vérification des événements est déjà en cours, ignoré");
            return;
        }
        
        try {
            LOGGER.info("Vérification des événements à venir pour notifications...");
            
            // Vérifier la connexion aux bases de données
            boolean dbConnected = checkConnections();
            if (!dbConnected) {
                LOGGER.warning("Impossible de vérifier les événements: problème de connexion à la base de données");
                return;
            }
            
            // Récupération de tous les événements non archivés
            List<Event> upcomingEvents = eventService.findAllNonArchived();
            if (upcomingEvents.isEmpty()) {
                LOGGER.info("Aucun événement à venir trouvé");
                return;
            }
            
            LOGGER.info("Trouvé " + upcomingEvents.size() + " événements à vérifier");
            
            LocalDateTime now = LocalDateTime.now();
            
            for (Event event : upcomingEvents) {
                try {
                    if (event.getStartDate() == null) continue;
                    
                    long hoursUntilStart = ChronoUnit.HOURS.between(now, event.getStartDate());
                    
                    LOGGER.info("Événement #" + event.getId() + " - " + event.getTitle() + 
                            " commence dans " + hoursUntilStart + " heures");
                    
                    // Envoyer des notifications si la date est exactement à 24h (plus ou moins 1h pour la tolérance)
                    if (hoursUntilStart <= 25 && hoursUntilStart >= 23) {
                        LOGGER.info("L'événement #" + event.getId() + " est à 24h, envoi des notifications de rappel");
                        sendScheduledReminders(event);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la vérification de l'événement #" + event.getId(), e);
                    // Continue avec le prochain événement
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification des événements à venir", e);
        } finally {
            isChecking.set(false);
        }
    }
    
    /**
     * Vérifie un événement spécifique et crée des notifications si nécessaire
     * @param eventId L'ID de l'événement à vérifier
     */
    public void checkSpecificEvent(int eventId) {
        try {
            LOGGER.info("Vérification de l'événement #" + eventId + " pour notifications...");
            
            // Vérifier la connexion aux bases de données
            boolean dbConnected = checkConnections();
            if (!dbConnected) {
                LOGGER.warning("Impossible de vérifier l'événement #" + eventId + ": problème de connexion à la base de données");
                return;
            }
            
            Event event = eventService.findById(eventId);
            if (event == null || event.isArchived()) {
                LOGGER.warning("Événement #" + eventId + " non trouvé ou archivé");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = event.getStartDate();
            
            if (startDate == null) {
                LOGGER.warning("Événement #" + eventId + " a une date de début nulle");
                return;
            }
            
            long hoursUntilStart = ChronoUnit.HOURS.between(now, startDate);
            
            LOGGER.info("Événement #" + eventId + " - " + event.getTitle() + 
                    " commence dans " + hoursUntilStart + " heures");
            
            // Envoyer des notifications si la date est exactement à 24h (plus ou moins 1h pour la tolérance)
            if (hoursUntilStart <= 25 && hoursUntilStart >= 23) {
                LOGGER.info("L'événement #" + eventId + " est à 24h, envoi des notifications de rappel");
                sendScheduledReminders(event);
            } else {
                LOGGER.info("L'événement #" + eventId + " n'est pas à 24h avant le début (" + hoursUntilStart + 
                        " heures), pas d'envoi de notification maintenant");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de l'événement #" + eventId, e);
        }
    }
    
    /**
     * Vérifie que toutes les connexions à la base de données sont actives
     * @return true si toutes les connexions sont actives, false sinon
     */
    private boolean checkConnections() {
        boolean eventServiceConnected = eventService.checkConnection();
        boolean inscriptionServiceConnected = inscriptionService.checkConnection();
        boolean notificationServiceConnected = notificationService.checkConnection();
        
        if (!eventServiceConnected) {
            LOGGER.warning("EventService: connexion à la base de données non disponible");
        }
        
        if (!inscriptionServiceConnected) {
            LOGGER.warning("InscriptionService: connexion à la base de données non disponible");
        }
        
        if (!notificationServiceConnected) {
            LOGGER.warning("NotificationService: connexion à la base de données non disponible");
        }
        
        return eventServiceConnected && inscriptionServiceConnected && notificationServiceConnected;
    }
    
    /**
     * Envoie les notifications de rappel stockées pour un événement
     * @param event L'événement pour lequel envoyer les rappels
     */
    private void sendScheduledReminders(Event event) {
        LOGGER.info("Envoi des notifications de rappel stockées pour l'événement: " + event.getTitle());
        
        try {
            // Vérifier à nouveau les connexions
            if (!checkConnections()) {
                LOGGER.warning("Impossible d'envoyer les notifications: problème de connexion à la base de données");
                return;
            }
            
            // Récupérer toutes les inscriptions pour cet événement
            List<Inscription> inscriptions = inscriptionService.getInscriptionsByEventId(event.getId());
            
            if (inscriptions.isEmpty()) {
                LOGGER.info("Aucun utilisateur inscrit à l'événement #" + event.getId());
                return;
            }
            
            for (Inscription inscription : inscriptions) {
                User user = inscription.getUser();
                if (user == null) {
                    continue;
                }
                
                try {
                    // Rechercher les notifications de rappel stockées mais non envoyées
                    String sql = "SELECT * FROM notification_history WHERE user_id = ? AND event_id = ? " +
                                "AND notification_type = ? AND sent_date IS NULL";
                    
                    try (java.sql.PreparedStatement stmt = notificationService.getConnection().prepareStatement(sql)) {
                        stmt.setInt(1, user.getId());
                        stmt.setInt(2, event.getId());
                        stmt.setString(3, NotificationHistory.TYPE_REMINDER);
                        
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int notificationId = rs.getInt("id");
                                
                                // Mettre à jour la date d'envoi pour indiquer que la notification a été envoyée
                                String updateSql = "UPDATE notification_history SET sent_date = NOW() WHERE id = ?";
                                try (java.sql.PreparedStatement updateStmt = notificationService.getConnection().prepareStatement(updateSql)) {
                                    updateStmt.setInt(1, notificationId);
                                    updateStmt.executeUpdate();
                                    
                                    LOGGER.info("Notification de rappel #" + notificationId + 
                                               " pour l'utilisateur #" + user.getId() + 
                                               " et l'événement #" + event.getId() + 
                                               " envoyée avec succès (24h avant l'événement)");
                                }
                            } else {
                                // Si pas de notification stockée, en créer une nouvelle et l'envoyer immédiatement
                                // (cela ne devrait normalement pas se produire, mais au cas où)
                                LOGGER.info("Aucune notification de rappel stockée trouvée pour l'utilisateur #" + 
                                           user.getId() + " et l'événement #" + event.getId() + 
                                           ", création d'une nouvelle notification");
                                
                                String details = "RAPPEL: L'événement \"" + event.getTitle() + "\" commence " + 
                                        getTimeDescription(event.getStartDate()) + " à " + 
                                        event.getStartDate().getHour() + "h" + 
                                        (event.getStartDate().getMinute() > 0 ? event.getStartDate().getMinute() : "") +
                                        ". Lieu: " + event.getLocation();
                                
                                notificationService.sendEventReminder(user.getId(), event.getId(), details);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de l'envoi des notifications de rappel pour l'utilisateur #" + 
                            user.getId() + ", événement #" + event.getId(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'envoi des notifications de rappel pour l'événement #" + 
                      event.getId(), e);
        }
    }
    
    /**
     * Obtient une description textuelle du temps avant un événement
     */
    private String getTimeDescription(LocalDateTime eventTime) {
        LocalDateTime now = LocalDateTime.now();
        long hours = ChronoUnit.HOURS.between(now, eventTime);
        
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
    
    /**
     * Force la vérification immédiate des événements à venir
     * Utile pour tester ou après création d'un événement
     */
    public void forceCheck() {
        // Utiliser un nouveau thread pour éviter de bloquer l'interface utilisateur
        Thread checkThread = new Thread(() -> {
            try {
                // Si une vérification est déjà en cours, attendre qu'elle se termine
                if (isChecking.get()) {
                    LOGGER.info("Une vérification est déjà en cours, attente avant le forçage");
                    Thread.sleep(1000); // Attendre 1 seconde
                    if (isChecking.get()) {
                        LOGGER.info("Vérification toujours en cours, le forçage va être ignoré");
                        return;
                    }
                }
                
                // Forcer la vérification
                checkUpcomingEvents();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du forçage de la vérification", e);
            }
        });
        
        checkThread.setDaemon(true);
        checkThread.start();
    }
    
    /**
     * Crée des notifications de test pour vérifier le système
     * Cette méthode est utile uniquement pour tester le système
     * 
     * @param userId ID de l'utilisateur pour lequel créer la notification de test
     * @param eventId ID de l'événement pour lequel créer la notification de test
     */
    public void createTestNotification(int userId, int eventId) {
        try {
            if (!checkConnections()) {
                LOGGER.warning("Impossible de créer une notification de test: problème de connexion à la base de données");
                return;
            }
            
            Event event = eventService.findById(eventId);
            if (event == null) {
                LOGGER.warning("Impossible de créer une notification de test: Événement #" + eventId + " non trouvé");
                return;
            }
            
            String details = "NOTIFICATION DE TEST: L'événement \"" + event.getTitle() + 
                    "\" commence demain. Cette notification a été créée manuellement à des fins de test.";
            
            notificationService.sendEventReminder(userId, eventId, details);
            LOGGER.info("Notification de TEST créée pour l'utilisateur #" + userId + " et l'événement #" + eventId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'une notification de test", e);
        }
    }
} 