package services;

import entity.Reponse;
import utils.connBD;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReponseService {

    public void addResponse(Reponse reponse) {
        String sql = "INSERT INTO reponse (contenu, date_reponse, reclamation_id) VALUES (?, ?, ?)";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reponse.getContenu());
            stmt.setDate(2, Date.valueOf(reponse.getDateReponse()));
            stmt.setInt(3, reponse.getReclamationId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reponse.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout de la réponse", e);
        }
    }

    public List<Reponse> getAllResponses() throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse ORDER BY date_reponse DESC";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Reponse reponse = new Reponse(
                        rs.getString("contenu"),
                        rs.getDate("date_reponse").toLocalDate(),
                        rs.getInt("reclamation_id")
                );
                reponse.setId(rs.getInt("id"));
                reponses.add(reponse);
            }
        }
        return reponses;
    }

    public List<Reponse> getResponsesForReclamation(int reclamationId) {
        List<Reponse> reponses = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE reclamation_id = ? ORDER BY date_reponse DESC";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reclamationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Reponse reponse = new Reponse(
                        rs.getString("contenu"),
                        rs.getDate("date_reponse").toLocalDate(),
                        rs.getInt("reclamation_id") // Changed column name
                );
                reponse.setId(rs.getInt("id"));
                reponses.add(reponse);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des réponses", e);
        }
        return reponses;
    }


    public void updateResponse(Reponse reponse) {
        String sql = "UPDATE reponse SET contenu = ?, date_reponse = ? WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reponse.getContenu());
            stmt.setDate(2, Date.valueOf(reponse.getDateReponse()));
            stmt.setInt(3, reponse.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de la réponse", e);
        }
    }

    public void deleteResponse(int id) throws SQLException{
        String sql = "DELETE FROM reponse WHERE id = ?";

        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de la réponse", e);
        }
    }
}