package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import model.dto.WaitingListEntryDto;
import model.entities.Event;
import model.entities.User;
import model.entities.WaitingListEntry;
import repository.EventRepository;
import repository.UserRepository;
import repository.WaitingListRepository;
import util.DbConnection;

/**
 * Service responsable de la gestion des listes d'attente pour les événements
 */
public class WaitingListService {
    
    private final WaitingListRepository waitingListRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private Connection connection;
    private ParticipantService participantService;
    
    /**
     * Constructeur du service
     */
    public WaitingListService() {
        this.waitingListRepository = new WaitingListRepository();
        this.eventRepository = new EventRepository();
        this.userRepository = new UserRepository();
        this.mailService = new MailService();
        this.connection = DbConnection.getInstance();
        this.participantService = new ParticipantService();
    }
    
    /**
     * Récupère toutes les entrées de la liste d'attente pour un événement
     * sous forme de DTO pour l'affichage
     * 
     * @param eventId L'identifiant de l'événement
     * @return Liste des entrées formatées pour l'affichage
     */
    public List<WaitingListEntryDto> getWaitingListForEvent(int eventId) {
        List<WaitingListEntry> entries = waitingListRepository.findByEventId(eventId);
        
        // Trier par date de création (FIFO)
        entries.sort((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt()));
        
        // Convertir en DTOs avec la position
        List<WaitingListEntryDto> dtos = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            dtos.add(new WaitingListEntryDto(entries.get(i), i + 1));
        }
        
        return dtos;
    }
    
    /**
     * Vérifie si un utilisateur est déjà dans la liste d'attente d'un événement
     * 
     * @param eventId L'identifiant de l'événement
     * @param userId L'identifiant de l'utilisateur
     * @return true si l'utilisateur est déjà en liste d'attente
     */
    public boolean isUserInWaitingList(int eventId, int userId) {
        return waitingListRepository.findByEventIdAndUserId(eventId, userId) != null;
    }
    
    /**
     * Ajoute un utilisateur à la liste d'attente d'un événement
     * 
     * @param eventId L'identifiant de l'événement
     * @param userId L'identifiant de l'utilisateur
     * @return true si l'ajout a réussi
     */
    public boolean addUserToWaitingList(int eventId, int userId) {
        if (isUserInWaitingList(eventId, userId)) {
            return false;
        }
        
        Event event = eventRepository.findById(eventId);
        User user = userRepository.findById(userId);
        
        if (event == null || user == null) {
            return false;
        }
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEvent(event);
        entry.setUser(user);
        
        return waitingListRepository.save(entry);
    }
    
    /**
     * Retire un utilisateur de la liste d'attente
     * 
     * @param eventId L'identifiant de l'événement
     * @param userId L'identifiant de l'utilisateur
     * @return true si la suppression a réussi
     */
    public boolean removeUserFromWaitingList(int eventId, int userId) {
        WaitingListEntry entry = waitingListRepository.findByEventIdAndUserId(eventId, userId);
        
        if (entry == null) {
            return false;
        }
        
        return waitingListRepository.delete(entry.getId());
    }
    
    /**
     * Notifie un utilisateur qu'il a été promu de la liste d'attente
     * 
     * @param event L'événement concerné
     * @param user L'utilisateur à notifier
     * @return true si la notification a été envoyée
     */
    public boolean notifyUserPromoted(Event event, User user) {
        String subject = "Votre inscription à l'événement " + event.getName() + " est confirmée";
        String content = "Bonjour " + user.getFirstName() + ",\n\n" +
                         "Nous avons le plaisir de vous informer que vous avez été promu(e) de la liste d'attente " +
                         "pour l'événement \"" + event.getName() + "\" prévu le " + event.getDateDebut() + ".\n\n" +
                         "Votre place est maintenant confirmée. Nous sommes impatients de vous y voir.\n\n" +
                         "Cordialement,\nL'équipe de gestion des événements";
        
        try {
            return mailService.sendEmail(user.getEmail(), subject, content);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de promotion: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Notifie tous les utilisateurs de la liste d'attente d'un événement avec un message personnalisé
     * 
     * @param eventId L'identifiant de l'événement
     * @param subject Le sujet du message
     * @param messageTemplate Le modèle de message (peut contenir {firstName} qui sera remplacé)
     * @return Le nombre d'utilisateurs notifiés avec succès
     */
    public int notifyAllUsersInWaitingList(int eventId, String subject, String messageTemplate) {
        List<WaitingListEntry> entries = waitingListRepository.findByEventId(eventId);
        if (entries.isEmpty()) {
            return 0;
        }
        
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return 0;
        }
        
        int successCount = 0;
        
        for (WaitingListEntry entry : entries) {
            User user = entry.getUser();
            String personalizedMessage = messageTemplate.replace("{firstName}", user.getFirstName());
            
            try {
                boolean sent = mailService.sendEmail(user.getEmail(), subject, personalizedMessage);
                if (sent) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi de l'email à " + user.getEmail() + ": " + e.getMessage());
            }
        }
        
        return successCount;
    }
    
    /**
     * Promeut les N premiers utilisateurs de la liste d'attente vers la liste des participants
     * 
     * @param eventId L'identifiant de l'événement
     * @param count Le nombre d'utilisateurs à promouvoir
     * @param sendNotifications Si vrai, envoie des notifications aux utilisateurs promus
     * @return Le nombre d'utilisateurs promus avec succès
     */
    public int promoteUsersFromWaitingList(int eventId, int count, boolean sendNotifications) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return 0;
        }
        
        // Vérifier s'il y a de la capacité disponible
        int availableCapacity = event.getCapacity() - event.getParticipants().size();
        if (availableCapacity <= 0) {
            return 0;
        }
        
        // Limiter le nombre à promouvoir à la capacité disponible
        count = Math.min(count, availableCapacity);
        
        List<WaitingListEntryDto> waitingList = getWaitingListForEvent(eventId);
        if (waitingList.isEmpty()) {
            return 0;
        }
        
        // Limiter le nombre à promouvoir au nombre de personnes en liste d'attente
        count = Math.min(count, waitingList.size());
        
        int promotedCount = 0;
        
        for (int i = 0; i < count; i++) {
            WaitingListEntryDto dto = waitingList.get(i);
            User user = dto.getUser();
            
            // Ajouter l'utilisateur à la liste des participants
            if (event.addParticipant(user) && eventRepository.update(event)) {
                // Supprimer l'utilisateur de la liste d'attente
                if (removeUserFromWaitingList(eventId, user.getId())) {
                    promotedCount++;
                    
                    // Envoyer une notification si demandé
                    if (sendNotifications) {
                        notifyUserPromoted(event, user);
                    }
                }
            }
        }
        
        return promotedCount;
    }
    
    /**
     * Ajoute un participant à la liste d'attente d'un événement
     * 
     * @param eventId ID de l'événement
     * @param participantId ID du participant
     * @param comment Commentaire optionnel
     * @return true si l'ajout a réussi
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public boolean addToWaitingList(int eventId, int participantId, String comment) throws SQLException {
        // Vérifier si le participant est déjà sur la liste d'attente
        if (isParticipantInWaitingList(eventId, participantId)) {
            return false;
        }
        
        String sql = "INSERT INTO waiting_list (evenement_id, participant_id, date_ajout, commentaire, position) "
                   + "VALUES (?, ?, NOW(), ?, (SELECT COALESCE(MAX(position), 0) + 1 FROM waiting_list WHERE evenement_id = ?))";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, participantId);
            stmt.setString(3, comment);
            stmt.setInt(4, eventId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Vérifie si un participant est déjà sur la liste d'attente d'un événement
     * 
     * @param eventId ID de l'événement
     * @param participantId ID du participant
     * @return true si le participant est déjà sur la liste d'attente
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public boolean isParticipantInWaitingList(int eventId, int participantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM waiting_list WHERE evenement_id = ? AND participant_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, participantId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Supprime un participant de la liste d'attente
     * 
     * @param eventId ID de l'événement
     * @param participantId ID du participant
     * @return true si la suppression a réussi
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public boolean removeFromWaitingList(int eventId, int participantId) throws SQLException {
        String sql = "DELETE FROM waiting_list WHERE evenement_id = ? AND participant_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, participantId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Récupère tous les participants sur liste d'attente pour un événement
     * 
     * @param eventId ID de l'événement
     * @return Liste des entrées de la liste d'attente
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public List<WaitingListEntry> getWaitingList(int eventId) throws SQLException {
        List<WaitingListEntry> waitingList = new ArrayList<>();
        
        String sql = "SELECT wl.id, wl.evenement_id, wl.participant_id, wl.date_ajout, wl.commentaire, wl.position, "
                   + "p.nom, p.prenom, p.email, p.telephone "
                   + "FROM waiting_list wl "
                   + "JOIN participant p ON wl.participant_id = p.id "
                   + "WHERE wl.evenement_id = ? "
                   + "ORDER BY wl.position ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Participant participant = new Participant(
                        rs.getInt("participant_id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone")
                    );
                    
                    WaitingListEntry entry = new WaitingListEntry(
                        rs.getInt("id"),
                        rs.getInt("evenement_id"),
                        participant,
                        rs.getTimestamp("date_ajout"),
                        rs.getString("commentaire"),
                        rs.getInt("position")
                    );
                    
                    waitingList.add(entry);
                }
            }
        }
        
        return waitingList;
    }
    
    /**
     * Déplace un participant dans la liste d'attente (change sa position)
     * 
     * @param waitingListId ID de l'entrée dans la liste d'attente
     * @param newPosition Nouvelle position désirée
     * @return true si le déplacement a réussi
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public boolean moveInWaitingList(int waitingListId, int newPosition) throws SQLException {
        // D'abord récupérer l'événement ID et la position actuelle
        int eventId = 0;
        int currentPosition = 0;
        
        String selectSql = "SELECT evenement_id, position FROM waiting_list WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            stmt.setInt(1, waitingListId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    eventId = rs.getInt("evenement_id");
                    currentPosition = rs.getInt("position");
                } else {
                    return false; // Entry not found
                }
            }
        }
        
        // Trouver le nombre maximal de positions
        int maxPosition = 0;
        String countSql = "SELECT MAX(position) FROM waiting_list WHERE evenement_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(countSql)) {
            stmt.setInt(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    maxPosition = rs.getInt(1);
                }
            }
        }
        
        // Valider la nouvelle position
        if (newPosition < 1 || newPosition > maxPosition) {
            return false;
        }
        
        // Commencer une transaction
        connection.setAutoCommit(false);
        
        try {
            // Si on déplace vers le haut (position plus petite)
            if (newPosition < currentPosition) {
                String sql = "UPDATE waiting_list SET position = position + 1 "
                           + "WHERE evenement_id = ? AND position >= ? AND position < ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, eventId);
                    stmt.setInt(2, newPosition);
                    stmt.setInt(3, currentPosition);
                    stmt.executeUpdate();
                }
            } 
            // Si on déplace vers le bas (position plus grande)
            else if (newPosition > currentPosition) {
                String sql = "UPDATE waiting_list SET position = position - 1 "
                           + "WHERE evenement_id = ? AND position > ? AND position <= ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, eventId);
                    stmt.setInt(2, currentPosition);
                    stmt.setInt(3, newPosition);
                    stmt.executeUpdate();
                }
            } else {
                // Même position, rien à faire
                connection.setAutoCommit(true);
                return true;
            }
            
            // Mettre à jour la position de l'entrée spécifiée
            String updateSql = "UPDATE waiting_list SET position = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setInt(1, newPosition);
                stmt.setInt(2, waitingListId);
                stmt.executeUpdate();
            }
            
            connection.commit();
            return true;
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    /**
     * Traite automatiquement la liste d'attente pour un événement en cas d'annulation
     * 
     * @param eventId ID de l'événement
     * @param placesAvailable Nombre de places disponibles
     * @return Liste des participants qui ont été promus de la liste d'attente
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public List<Participant> processWaitingList(int eventId, int placesAvailable) throws SQLException {
        List<Participant> promotedParticipants = new ArrayList<>();
        
        if (placesAvailable <= 0) {
            return promotedParticipants;
        }
        
        // Récupérer les premiers participants de la liste d'attente
        String sql = "SELECT participant_id FROM waiting_list "
                   + "WHERE evenement_id = ? ORDER BY position ASC LIMIT ?";
        
        List<Integer> participantIds = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, placesAvailable);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participantIds.add(rs.getInt("participant_id"));
                }
            }
        }
        
        // Pour chaque participant, l'ajouter à l'événement et le retirer de la liste d'attente
        EvenementService eventService = new EvenementService();
        
        for (Integer participantId : participantIds) {
            if (eventService.addParticipant(eventId, participantId)) {
                removeFromWaitingList(eventId, participantId);
                Participant participant = participantService.getParticipantById(participantId);
                if (participant != null) {
                    promotedParticipants.add(participant);
                }
            }
        }
        
        // Réorganiser les positions restantes après les promotions
        reorderWaitingList(eventId);
        
        return promotedParticipants;
    }
    
    /**
     * Réorganise les positions dans la liste d'attente pour éviter les trous
     * 
     * @param eventId ID de l'événement
     * @throws SQLException En cas d'erreur avec la base de données
     */
    private void reorderWaitingList(int eventId) throws SQLException {
        String sql = "SET @position := 0; "
                   + "UPDATE waiting_list SET position = (@position := @position + 1) "
                   + "WHERE evenement_id = ? ORDER BY position ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Compte le nombre de personnes sur liste d'attente pour un événement
     * 
     * @param eventId ID de l'événement
     * @return Nombre de personnes sur liste d'attente
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public int countWaitingList(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM waiting_list WHERE evenement_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Vérifie s'il y a une liste d'attente pour un événement
     * 
     * @param eventId ID de l'événement
     * @return true s'il y a au moins une personne sur liste d'attente
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public boolean hasWaitingList(int eventId) throws SQLException {
        return countWaitingList(eventId) > 0;
    }
    
    /**
     * Retourne la position d'un participant dans la liste d'attente
     * 
     * @param eventId ID de l'événement
     * @param participantId ID du participant
     * @return Position dans la liste d'attente (0 si pas présent)
     * @throws SQLException En cas d'erreur avec la base de données
     */
    public int getPositionInWaitingList(int eventId, int participantId) throws SQLException {
        String sql = "SELECT position FROM waiting_list WHERE evenement_id = ? AND participant_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, participantId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("position");
                }
            }
        }
        
        return 0; // Pas dans la liste d'attente
    }
} 