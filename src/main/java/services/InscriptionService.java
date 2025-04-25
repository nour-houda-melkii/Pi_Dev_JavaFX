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
    
    public boolean inscrireUtilisateur(int userId, int eventId) {
        String sql = "INSERT INTO inscription (user_id, event_id, date_inscription, has_unsubscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, eventId);
            statement.setObject(3, LocalDateTime.now());
            statement.setBoolean(4, false);
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'inscription à l'événement: " + e.getMessage());
            return false;
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
} 