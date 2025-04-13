package services;

import models.CategorieEvent;
import models.Event;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public List<Event> getAllActifs() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event e JOIN categorie_event c ON e.categorie_id = c.id WHERE e.is_archived = 0";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(extractEvent(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    public List<Event> getAllArchives() {
        List<Event> list = new ArrayList<>();
        try {
            Connection conn = MyConnection.getInstance();
            if (conn == null || conn.isClosed()) {
                System.out.println("❌ Connexion est fermée.");
                return list;
            }

            System.out.println("📥 Chargement des événements archivés...");

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM event e JOIN categorie_event c ON e.categorie_id = c.id WHERE e.is_archived = 1"
            );
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(extractEvent(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


    public void insert(Event e) {
        String sql = "INSERT INTO event (title, description, start_date, end_date, location, latitude, longitude, places_disponibles, affiche, is_archived, updated_at, categorie_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getStartDate()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getEndDate()));
            ps.setString(5, e.getLocation());
            ps.setDouble(6, e.getLatitude());
            ps.setDouble(7, e.getLongitude());
            ps.setInt(8, e.getPlacesDisponibles());
            ps.setString(9, e.getAffiche());
            ps.setBoolean(10, e.isArchived());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(12, e.getCategorie().getId());

            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void update(Event e) {
        String sql = "UPDATE event SET title = ?, description = ?, start_date = ?, end_date = ?, location = ?, latitude = ?, longitude = ?, places_disponibles = ?, affiche = ?, updated_at = ?, categorie_id = ? WHERE id = ?";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getStartDate()));
            ps.setTimestamp(4, Timestamp.valueOf(e.getEndDate()));
            ps.setString(5, e.getLocation());
            ps.setDouble(6, e.getLatitude());
            ps.setDouble(7, e.getLongitude());
            ps.setInt(8, e.getPlacesDisponibles());
            ps.setString(9, e.getAffiche());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(11, e.getCategorie().getId());
            ps.setInt(12, e.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void updateEventArchiveStatus(int id, boolean archive) {
        String sql = "UPDATE event SET is_archived = ? WHERE id = ?";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, archive);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteEvent(int id) {
        String sql = "DELETE FROM event WHERE id = ?";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT nom FROM categorie_event WHERE is_archived = 0";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("nom"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
    }

    private Event extractEvent(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        e.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
        e.setLocation(rs.getString("location"));
        e.setLatitude(rs.getDouble("latitude"));
        e.setLongitude(rs.getDouble("longitude"));
        e.setPlacesDisponibles(rs.getInt("places_disponibles"));
        e.setAffiche(rs.getString("affiche"));
        e.setArchived(rs.getBoolean("is_archived"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) {
            e.setUpdatedAt(ts.toLocalDateTime());
        }

        CategorieEvent c = new CategorieEvent();
        c.setId(rs.getInt("categorie_id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        c.setArchived(rs.getBoolean("c.is_archived"));

        e.setCategorie(c);
        return e;
    }
}