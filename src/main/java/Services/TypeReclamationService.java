package services;

import entity.TypeReclamation;
import utils.connBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TypeReclamationService {
    public List<TypeReclamation> getAllTypes() throws SQLException {
        List<TypeReclamation> types = new ArrayList<>();
        String sql = "SELECT * FROM type_reclamation";

        try (Connection conn = connBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TypeReclamation type = new TypeReclamation(
                        rs.getInt("id"),
                        rs.getString("nom")
                );
                types.add(type);
            }
        }
        return types;
    }

    public void addType(TypeReclamation type) throws SQLException {
        String sql = "INSERT INTO type_reclamation (nom) VALUES (?)";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, type.getNom());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    type.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean updateType(TypeReclamation type) throws SQLException {
        String sql = "UPDATE type_reclamation SET nom = ? WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.getNom());
            stmt.setInt(2, type.getId());
            stmt.executeUpdate();
        }
        return false;
    }

    public void deleteType(int id) throws SQLException {
        String sql = "DELETE FROM type_reclamation WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }




}