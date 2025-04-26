package services;

import models.Event;
import models.CategorieEvent;
import utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    private Connection connection;

    public EventService() {
        connection = MyConnection.getInstance();
    }

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String query = "SELECT e.*, c.id as cat_id, c.nom as cat_nom, c.description as cat_description, c.is_archived as cat_is_archived " +
                      "FROM event e " +
                      "LEFT JOIN categorie_event c ON e.categorie_id = c.id";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                Event event = new Event();
                event.setId(resultSet.getInt("id"));
                event.setTitle(resultSet.getString("title"));
                event.setDescription(resultSet.getString("description"));
                event.setLocation(resultSet.getString("location"));
                event.setStartDate(resultSet.getTimestamp("start_date").toLocalDateTime());
                event.setEndDate(resultSet.getTimestamp("end_date").toLocalDateTime());
                event.setLatitude(resultSet.getDouble("latitude"));
                event.setLongitude(resultSet.getDouble("longitude"));
                event.setPlacesDisponibles(resultSet.getInt("places_disponibles"));
                event.setAffiche(resultSet.getString("affiche"));
                event.setArchived(resultSet.getBoolean("is_archived"));
                
                // Ajout de la catégorie
                int catId = resultSet.getInt("cat_id");
                if (!resultSet.wasNull()) {  // Vérifie si categorie_id n'est pas NULL
                    CategorieEvent categorie = new CategorieEvent();
                    categorie.setId(catId);
                    categorie.setNom(resultSet.getString("cat_nom"));
                    categorie.setDescription(resultSet.getString("cat_description"));
                    categorie.setArchived(resultSet.getBoolean("cat_is_archived"));
                    event.setCategorie(categorie);
                }
                
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return events;
    }
    
    public Event findById(int id) {
        String query = "SELECT e.*, c.id as cat_id, c.nom as cat_nom, c.description as cat_description, c.is_archived as cat_is_archived " +
                      "FROM event e " +
                      "LEFT JOIN categorie_event c ON e.categorie_id = c.id " +
                      "WHERE e.id = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                Event event = new Event();
                event.setId(resultSet.getInt("id"));
                event.setTitle(resultSet.getString("title"));
                event.setDescription(resultSet.getString("description"));
                event.setLocation(resultSet.getString("location"));
                event.setStartDate(resultSet.getTimestamp("start_date").toLocalDateTime());
                event.setEndDate(resultSet.getTimestamp("end_date").toLocalDateTime());
                event.setLatitude(resultSet.getDouble("latitude"));
                event.setLongitude(resultSet.getDouble("longitude"));
                event.setPlacesDisponibles(resultSet.getInt("places_disponibles"));
                event.setAffiche(resultSet.getString("affiche"));
                event.setArchived(resultSet.getBoolean("is_archived"));
                
                // Ajout de la catégorie
                int catId = resultSet.getInt("cat_id");
                if (!resultSet.wasNull()) {  // Vérifie si categorie_id n'est pas NULL
                    CategorieEvent categorie = new CategorieEvent();
                    categorie.setId(catId);
                    categorie.setNom(resultSet.getString("cat_nom"));
                    categorie.setDescription(resultSet.getString("cat_description"));
                    categorie.setArchived(resultSet.getBoolean("cat_is_archived"));
                    event.setCategorie(categorie);
                }
                
                return event;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Met à jour le nombre de places disponibles pour un événement spécifique
     * @param eventId L'identifiant de l'événement
     * @param placesDisponibles Le nouveau nombre de places disponibles
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean updatePlacesDisponibles(int eventId, int placesDisponibles) {
        String query = "UPDATE event SET places_disponibles = ? WHERE id = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, placesDisponibles);
            preparedStatement.setInt(2, eventId);
            
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère le titre d'un événement à partir de son ID
     * @param eventId L'identifiant de l'événement
     * @return Le titre de l'événement ou null si non trouvé
     */
    public String getEventTitle(int eventId) {
        String query = "SELECT title FROM event WHERE id = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, eventId);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getString("title");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Vérifie que la connexion à la base de données est active
     * @return true si la connexion est active, false sinon
     */
    public boolean checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = MyConnection.getInstance();
                System.out.println("🔄 Connexion à la base de données réinitialisée dans EventService");
                return connection != null && !connection.isClosed();
            }
            
            // Tester la connexion avec une requête simple
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                stmt.executeQuery();
            } catch (SQLException e) {
                System.out.println("🔄 Connexion à la base de données perdue, tentative de reconnexion");
                connection = MyConnection.getInstance();
            }
            
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Échec de connexion à la base de données dans EventService: " + e.getMessage());
            try {
                connection = MyConnection.getInstance();
                return connection != null && !connection.isClosed();
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }
    
    /**
     * Récupère tous les événements non archivés
     * @return Liste des événements non archivés
     */
    public List<Event> findAllNonArchived() {
        List<Event> events = new ArrayList<>();
        String query = "SELECT e.*, c.id as cat_id, c.nom as cat_nom, c.description as cat_description, c.is_archived as cat_is_archived " +
                       "FROM event e " +
                       "LEFT JOIN categorie_event c ON e.categorie_id = c.id " +
                       "WHERE e.is_archived = 0";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                Event event = new Event();
                event.setId(resultSet.getInt("id"));
                event.setTitle(resultSet.getString("title"));
                event.setDescription(resultSet.getString("description"));
                event.setLocation(resultSet.getString("location"));
                event.setStartDate(resultSet.getTimestamp("start_date").toLocalDateTime());
                event.setEndDate(resultSet.getTimestamp("end_date").toLocalDateTime());
                event.setLatitude(resultSet.getDouble("latitude"));
                event.setLongitude(resultSet.getDouble("longitude"));
                event.setPlacesDisponibles(resultSet.getInt("places_disponibles"));
                event.setAffiche(resultSet.getString("affiche"));
                event.setArchived(resultSet.getBoolean("is_archived"));
                
                // Ajout de la catégorie
                int catId = resultSet.getInt("cat_id");
                if (!resultSet.wasNull()) {  // Vérifie si categorie_id n'est pas NULL
                    CategorieEvent categorie = new CategorieEvent();
                    categorie.setId(catId);
                    categorie.setNom(resultSet.getString("cat_nom"));
                    categorie.setDescription(resultSet.getString("cat_description"));
                    categorie.setArchived(resultSet.getBoolean("cat_is_archived"));
                    event.setCategorie(categorie);
                }
                
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return events;
    }
    
    /**
     * Archive un événement
     * @param eventId L'identifiant de l'événement à archiver
     * @param reason La raison de l'archivage (pour journalisation)
     * @return true si l'archivage a réussi, false sinon
     */
    public boolean archiveEvent(int eventId, String reason) {
        checkConnection();
        String query = "UPDATE event SET is_archived = 1 WHERE id = ?";
        
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, eventId);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Événement #" + eventId + " archivé avec succès. Raison: " + reason);
                return true;
            } else {
                System.out.println("❌ Échec de l'archivage de l'événement #" + eventId + ". Aucune ligne affectée.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Erreur lors de l'archivage de l'événement #" + eventId + ": " + e.getMessage());
            return false;
        }
    }
} 