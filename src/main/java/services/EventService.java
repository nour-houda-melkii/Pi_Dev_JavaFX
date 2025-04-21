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
} 