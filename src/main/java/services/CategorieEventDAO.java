// CategorieEventDAO.java
package services;

import models.CategorieEvent;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieEventDAO {

    public List<CategorieEvent> getAll() {
        List<CategorieEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM categorie_event WHERE is_archived = 0";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(extractCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<CategorieEvent> getAllActive() {
        return getAll(); // Pour la compatibilité avec EventFormController
    }

    public void insert(CategorieEvent c) {
        String sql = "INSERT INTO categorie_event (nom, description, is_archived) VALUES (?, ?, ?)";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setBoolean(3, c.isArchived());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    c.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(CategorieEvent c) {
        String sql = "UPDATE categorie_event SET nom = ?, description = ?, is_archived = ? WHERE id = ?";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setBoolean(3, c.isArchived());
            ps.setInt(4, c.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasEvents(int categorieId) {
        String sql = "SELECT COUNT(*) FROM event WHERE categorie_id = ?";
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categorieId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void delete(int id) {
        String sql = "DELETE FROM categorie_event WHERE id = ?";
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private CategorieEvent extractCategorie(ResultSet rs) throws SQLException {
        CategorieEvent c = new CategorieEvent();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        c.setArchived(rs.getBoolean("is_archived"));
        return c;
    }
}