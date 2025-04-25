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
            return; // Le planificateur est déjà démarré
        }
        
        timer = new Timer(true); // Crée un timer en tant que thread daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkUpcomingEvents();
            }
        }, 0, CHECK_INTERVAL); // Commence immédiatement, puis toutes les heures
        
        LOGGER.info("Planificateur de notifications démarré");
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
     * Vérifie les événements à venir et crée des notifications si nécessaire
     */
    private void checkUpcomingEvents() {
        try {
            LOGGER.info("Vérification des événements à venir pour les notifications...");
            
            // Récupérer tous les événements non archivés
            List<Event> events = eventService.getAllEvents();
            
            // Pour chaque événement, vérifier s'il commence dans les 24 heures
            LocalDateTime now = LocalDateTime.now();
            
            for (Event event : events) {
                if (event.isArchived()) {
                    continue; // Ignorer les événements archivés
                }
                
                LocalDateTime startDate = event.getStartDate();
                long hoursUntilStart = ChronoUnit.HOURS.between(now, startDate);
                
                // Si l'événement commence dans 23-25 heures (intervalle pour éviter les doublons)
                if (hoursUntilStart >= 23 && hoursUntilStart <= 25) {
                    // Récupérer tous les utilisateurs inscrits à cet événement
                    createReminderNotifications(event);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification des événements à venir", e);
        }
    }
    
    /**
     * Crée des notifications de rappel pour tous les utilisateurs inscrits à un événement
     * 
     * @param event L'événement pour lequel créer des rappels
     */
    private void createReminderNotifications(Event event) {
        LOGGER.info("Création de notifications de rappel pour l'événement: " + event.getTitle());
        
        try {
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
                
                // Vérifier si une notification similaire existe déjà
                List<NotificationHistory> existingNotifications = notificationService.getNotificationsForUser(user.getId());
                boolean alreadyNotified = existingNotifications.stream()
                        .anyMatch(n -> n.getEventId() == event.getId() && 
                                  NotificationHistory.TYPE_REMINDER.equals(n.getNotificationType()));
                
                if (!alreadyNotified) {
                    // Créer un message de rappel
                    String details = "L'événement \"" + event.getTitle() + "\" commence demain à " + 
                            event.getStartDate().getHour() + "h" + 
                            (event.getStartDate().getMinute() > 0 ? event.getStartDate().getMinute() : "") +
                            ". Lieu: " + event.getLocation();
                    
                    // Créer et enregistrer la notification
                    notificationService.sendEventReminder(user.getId(), event.getId(), details);
                    LOGGER.info("Notification de rappel créée pour l'utilisateur #" + user.getId() + " pour l'événement #" + event.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création des notifications de rappel pour l'événement #" + event.getId(), e);
        }
    }
    
    /**
     * Force la vérification immédiate des événements à venir
     * Utile pour tester ou après création d'un événement
     */
    public void forceCheck() {
        checkUpcomingEvents();
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