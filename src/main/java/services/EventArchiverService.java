package services;

import models.Event;
import models.Inscription;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service pour archiver automatiquement les événements selon des critères spécifiques:
 * 1. Les événements dont la date de début est passée
 * 2. Les événements qui n'ont pas atteint 50% de remplissage à 2 jours du début
 */
public class EventArchiverService {
    private static final Logger LOGGER = Logger.getLogger(EventArchiverService.class.getName());
    private static final long CHECK_INTERVAL = 60 * 60 * 1000; // Vérifier toutes les heures
    
    private final EventService eventService;
    private final InscriptionService inscriptionService;
    private Timer timer;
    private final AtomicBoolean isChecking = new AtomicBoolean(false);
    
    /**
     * Constructeur qui initialise les services nécessaires
     */
    public EventArchiverService() {
        this.eventService = new EventService();
        this.inscriptionService = new InscriptionService();
        LOGGER.info("EventArchiverService initialisé");
    }
    
    /**
     * Démarre le planificateur d'archivage
     */
    public void start() {
        if (timer != null) {
            stop(); // Arrêter l'ancien timer s'il existe pour éviter les doublons
            LOGGER.info("Ancien planificateur d'archivage arrêté avant redémarrage");
        }
        
        timer = new Timer(true); // Crée un timer en tant que thread daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkEventsForArchiving();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la vérification des événements pour archivage", e);
                }
            }
        }, 0, CHECK_INTERVAL); // Commence immédiatement, puis toutes les heures
        
        LOGGER.info("Planificateur d'archivage démarré");
        
        // Forcer une vérification immédiate au démarrage
        forceCheck();
    }
    
    /**
     * Arrête le planificateur d'archivage
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            LOGGER.info("Planificateur d'archivage arrêté");
        }
    }
    
    /**
     * Vérifie tous les événements non archivés et les archive selon les critères
     */
    private void checkEventsForArchiving() {
        // Vérifier si déjà en cours d'exécution pour éviter les doublons
        if (!isChecking.compareAndSet(false, true)) {
            LOGGER.info("Une vérification d'archivage est déjà en cours, ignoré");
            return;
        }
        
        try {
            LOGGER.info("Vérification des événements pour archivage...");
            
            // Vérifier la connexion aux bases de données
            boolean eventServiceConnected = eventService.checkConnection();
            boolean inscriptionServiceConnected = inscriptionService.checkConnection();
            
            if (!eventServiceConnected || !inscriptionServiceConnected) {
                LOGGER.warning("Impossible de vérifier les événements: problème de connexion à la base de données");
                return;
            }
            
            // Récupération de tous les événements non archivés
            List<Event> nonArchivedEvents = eventService.findAllNonArchived();
            if (nonArchivedEvents.isEmpty()) {
                LOGGER.info("Aucun événement non-archivé trouvé");
                return;
            }
            
            LOGGER.info("Trouvé " + nonArchivedEvents.size() + " événements non-archivés à vérifier");
            LocalDateTime now = LocalDateTime.now();
            
            // Vérifier chaque événement
            for (Event event : nonArchivedEvents) {
                try {
                    // 1. Vérifier si l'événement a dépassé sa date de début
                    if (event.getStartDate().isBefore(now)) {
                        eventService.archiveEvent(event.getId(), "Date de début dépassée");
                        LOGGER.info("Événement #" + event.getId() + " (" + event.getTitle() + ") archivé car sa date de début est passée");
                        continue; // Passer à l'événement suivant
                    }
                    
                    // 2. Vérifier si l'événement est à moins de 2 jours du début et n'a pas atteint 50% de remplissage
                    // Définir exactement 2 jours avant l'événement en ignorant l'heure
                    LocalDateTime eventStartDay = event.getStartDate().toLocalDate().atStartOfDay();
                    LocalDateTime twoDaysBeforeStart = eventStartDay.minusDays(2);
                    
                    // Pour la comparaison, utiliser aussi la date du jour sans l'heure
                    LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
                    
                    // Ajouter des logs de debug pour mieux comprendre les dates
                    LOGGER.info("DEBUG - Événement #" + event.getId() + ": " +
                               "Date début: " + event.getStartDate() + ", " +
                               "StartDay: " + eventStartDay + ", " +
                               "J-2: " + twoDaysBeforeStart + ", " +
                               "Aujourd'hui: " + todayStart + ", " +
                               "Condition J-2 satisfaite: " + (todayStart.isAfter(twoDaysBeforeStart) || todayStart.isEqual(twoDaysBeforeStart)));
                    
                    // Vérifier si on est à 2 jours ou moins du début (aujourd'hui est après ou égal à J-2)
                    if (todayStart.isAfter(twoDaysBeforeStart) || todayStart.isEqual(twoDaysBeforeStart)) {
                        LOGGER.info("Événement #" + event.getId() + " (" + event.getTitle() + ") à moins de 2 jours du début, vérification du taux de remplissage");
                        
                        // Vérifier le taux de remplissage
                        List<Inscription> inscriptions = inscriptionService.getInscriptionsByEventId(event.getId());
                        int placesOccupees = inscriptions.size();
                        int totalPlaces = placesOccupees + event.getPlacesDisponibles();
                        
                        // Calculer le taux d'occupation
                        double tauxOccupation = totalPlaces > 0 ? (double) placesOccupees / totalPlaces : 0;
                        LOGGER.info("Événement #" + event.getId() + " - Taux d'occupation: " + String.format("%.1f%%", tauxOccupation * 100) + 
                                   " (" + placesOccupees + " inscrits sur " + totalPlaces + " places totales)");
                        
                        if (tauxOccupation < 0.5) { // Moins de 50% de remplissage
                            String message = String.format("Taux d'occupation insuffisant à moins de 2 jours (%.1f%%)", tauxOccupation * 100);
                            eventService.archiveEvent(event.getId(), message);
                            LOGGER.info("Événement #" + event.getId() + " (" + event.getTitle() + ") archivé car " + message);
                        } else {
                            LOGGER.info("Événement #" + event.getId() + " (" + event.getTitle() + ") non archivé car taux d'occupation suffisant (≥ 50%)");
                        }
                    } else {
                        // Calculer le temps restant avant les 2 jours
                        long daysUntilCheck = ChronoUnit.DAYS.between(todayStart, twoDaysBeforeStart);
                        LOGGER.info("Événement #" + event.getId() + " (" + event.getTitle() + ") - " + 
                                   daysUntilCheck + " jours avant vérification du taux de remplissage (à J-2)");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur lors du traitement de l'événement #" + event.getId(), e);
                }
            }
            
            LOGGER.info("Vérification pour archivage terminée");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification des événements pour archivage", e);
        } finally {
            isChecking.set(false);
        }
    }
    
    /**
     * Force la vérification immédiate des événements
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
                checkEventsForArchiving();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du forçage de la vérification", e);
            }
        });
        
        checkThread.setDaemon(true);
        checkThread.start();
    }
} 