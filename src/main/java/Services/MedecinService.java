package services;

import entity.Medecin;
import utils.connBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedecinService {
    public List<Medecin> getAllMedecins() throws SQLException {
        List<Medecin> medecins = new ArrayList<>();
        String sql = "SELECT * FROM medecin";

        try (Connection conn = connBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Medecin med = new Medecin(
                        rs.getInt("id"),
                        rs.getString("nom")
                );
                medecins.add(med);
            }
        }
        return medecins;
    }

    public void addMedecin(Medecin medecin) throws SQLException {
        String sql = "INSERT INTO medecin (nom) VALUES (?)";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, medecin.getNom());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    medecin.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateMedecin(Medecin medecin) throws SQLException {
        String sql = "UPDATE medecin SET nom = ? WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, medecin.getNom());
            stmt.setInt(2, medecin.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteMedecin(int id) throws SQLException {
        String sql = "DELETE FROM medecin WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}