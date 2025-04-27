package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Rating;
import utils.MyConnection;

public class RatingService {
    private Connection conn;
    
    public RatingService() {
        conn = MyConnection.getInstance();
    }
    
    /**
     * Ajoute une nouvelle évaluation pour un événement
     * @param rating L'objet Rating à ajouter
     * @return true si l'opération a réussi, false sinon
     */
    public boolean addRating(Rating rating) {
        String query = "INSERT INTO rating (user_id, event_id, rating) VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, rating.getUserId());
            ps.setInt(2, rating.getEventId());
            ps.setDouble(3, rating.getRating());
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout d'une évaluation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Met à jour une évaluation existante
     * @param rating L'objet Rating avec les nouvelles valeurs
     * @return true si l'opération a réussi, false sinon
     */
    public boolean updateRating(Rating rating) {
        String query = "UPDATE rating SET rating = ? WHERE user_id = ? AND event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setDouble(1, rating.getRating());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getEventId());
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour d'une évaluation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Supprime une évaluation
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @return true si l'opération a réussi, false sinon
     */
    public boolean deleteRating(int userId, int eventId) {
        String query = "DELETE FROM rating WHERE user_id = ? AND event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression d'une évaluation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Vérifie si un utilisateur a déjà évalué un événement
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @return true si l'utilisateur a déjà évalué l'événement, false sinon
     */
    public boolean hasUserRatedEvent(int userId, int eventId) {
        String query = "SELECT COUNT(*) FROM rating WHERE user_id = ? AND event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification d'une évaluation: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère l'évaluation d'un utilisateur pour un événement
     * @param userId L'ID de l'utilisateur
     * @param eventId L'ID de l'événement
     * @return L'objet Rating ou null si aucune évaluation n'existe
     */
    public Rating getUserRatingForEvent(int userId, int eventId) {
        String query = "SELECT * FROM rating WHERE user_id = ? AND event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Rating rating = new Rating();
                    rating.setId(rs.getInt("id"));
                    rating.setUserId(rs.getInt("user_id"));
                    rating.setEventId(rs.getInt("event_id"));
                    rating.setRating(rs.getDouble("rating"));
                    return rating;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération d'une évaluation: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Calcule la note moyenne d'un événement
     * @param eventId L'ID de l'événement
     * @return La note moyenne ou 0 si aucune évaluation n'existe
     */
    public double getAverageRatingForEvent(int eventId) {
        String query = "SELECT AVG(rating) FROM rating WHERE event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, eventId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul de la note moyenne: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Récupère toutes les évaluations pour un événement
     * @param eventId L'ID de l'événement
     * @return Une liste d'objets Rating
     */
    public List<Rating> getRatingsForEvent(int eventId) {
        List<Rating> ratings = new ArrayList<>();
        String query = "SELECT * FROM rating WHERE event_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, eventId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rating rating = new Rating();
                    rating.setId(rs.getInt("id"));
                    rating.setUserId(rs.getInt("user_id"));
                    rating.setEventId(rs.getInt("event_id"));
                    rating.setRating(rs.getDouble("rating"));
                    ratings.add(rating);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des évaluations: " + e.getMessage());
        }
        
        return ratings;
    }
    
    /**
     * Vérifie si la connexion à la base de données est active
     * @return true si la connexion est active, false sinon
     */
    public boolean checkConnection() {
        return conn != null;
    }
} 