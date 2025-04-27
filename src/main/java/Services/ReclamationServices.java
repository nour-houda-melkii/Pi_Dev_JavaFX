package services;

import entity.Medecin;
import entity.TypeReclamation;
import entity.Reclamation;
import utils.connBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationServices {
    private Connection connection;

    public ReclamationServices() {
        try {
            connection = connBD.getConnection();
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    public boolean reclamationExists(Reclamation reclamation) throws SQLException {
        String query = "SELECT COUNT(*) FROM reclamation WHERE " +
                "type_reclamation_id = ? AND medecin_id = ? AND date_reclamation = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, reclamation.getTypeReclamationId());
            ps.setInt(2, reclamation.getMedecinId());
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean reclamationExistsExcludingCurrent(Reclamation reclamation) throws SQLException {
        String query = "SELECT COUNT(*) FROM reclamation WHERE " +
                "type_reclamation_id = ? AND medecin_id = ? AND date_reclamation = ? " +
                "AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, reclamation.getTypeReclamationId());
            ps.setInt(2, reclamation.getMedecinId());
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            ps.setInt(4, reclamation.getId());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean addReclamation(Reclamation reclamation) throws SQLException {
        String query = "INSERT INTO reclamation (type_reclamation_id, description, date_reclamation, photo, medecin_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reclamation.getTypeReclamationId());
            statement.setString(2, reclamation.getDescription());
            statement.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            statement.setString(4, reclamation.getPhotoPath());
            statement.setInt(5, reclamation.getMedecinId());

            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reclamation.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public List<Reclamation> getAllReclamationsWithNames() throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT r.*, t.nom as type_name, m.nom as medecin_name " +
                "FROM reclamation r " +
                "JOIN type_reclamation t ON r.type_reclamation_id = t.id " +
                "JOIN medecin m ON r.medecin_id = m.id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setId(rs.getInt("id"));
                r.setTypeReclamationId(rs.getInt("type_reclamation_id"));
                r.setTypeReclamationName(rs.getString("type_name"));
                r.setDescription(rs.getString("description"));
                r.setDateReclamation(rs.getDate("date_reclamation").toLocalDate());
                r.setMedecinId(rs.getInt("medecin_id"));
                r.setMedecinName(rs.getString("medecin_name"));
                r.setPhotoPath(rs.getString("photo"));
                reclamations.add(r);
            }
        }
        return reclamations;
    }

    public void updateReclamationStatus(int id, String newStatus) throws SQLException {
        String query = "UPDATE reclamation SET status = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public boolean updateReclamation(Reclamation reclamation) throws SQLException {
        String query = "UPDATE reclamation SET " +
                "type_reclamation_id = ?, description = ?, " +
                "date_reclamation = ?, medecin_id = ?, photo = ? " +
                "WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, reclamation.getTypeReclamationId());
            ps.setString(2, reclamation.getDescription());
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            ps.setInt(4, reclamation.getMedecinId());
            ps.setString(5, reclamation.getPhotoPath());
            ps.setInt(6, reclamation.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteReclamation(int id) throws SQLException {
        String query = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Reclamation getReclamationByIdWithNames(int id) throws SQLException {
        String query = "SELECT r.*, t.nom AS type_nom, m.nom AS medecin_nom " +
                "FROM reclamation r " +
                "JOIN type_reclamation t ON r.type_reclamation_id = t.id " +
                "JOIN medecin m ON r.medecin_id = m.id " +
                "WHERE r.id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reclamation reclamation = new Reclamation();
                    reclamation.setId(rs.getInt("id"));
                    reclamation.setTypeReclamationId(rs.getInt("type_reclamation_id"));
                    reclamation.setTypeReclamationName(rs.getString("type_nom"));
                    reclamation.setDescription(rs.getString("description"));
                    reclamation.setDateReclamation(rs.getDate("date_reclamation").toLocalDate());
                    reclamation.setMedecinId(rs.getInt("medecin_id"));
                    reclamation.setMedecinName(rs.getString("medecin_nom"));
                    reclamation.setPhotoPath(rs.getString("photo"));
                    return reclamation;
                }
            }
        }
        return null;
    }

    public List<TypeReclamation> getAllTypes() throws SQLException {
        List<TypeReclamation> types = new ArrayList<>();
        String sql = "SELECT id, nom FROM type_reclamation ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                types.add(new TypeReclamation(
                        rs.getInt("id"),
                        rs.getString("nom")
                ));
            }
        }
        return types;
    }

    public TypeReclamation getTypeById(int id) throws SQLException {
        String query = "SELECT * FROM type_reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TypeReclamation(
                            rs.getInt("id"),
                            rs.getString("nom")
                    );
                }
            }
        }
        return null;
    }

    public List<Medecin> getAllMedecins() throws SQLException {
        List<Medecin> medecins = new ArrayList<>();
        String query = "SELECT id, nom FROM medecin ORDER BY nom";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                medecins.add(new Medecin(
                        rs.getInt("id"),
                        rs.getString("nom")
                ));
            }
        }
        return medecins;
    }

    public Medecin getMedecinById(int id) throws SQLException {
        String query = "SELECT * FROM medecin WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Medecin(
                            rs.getInt("id"),
                            rs.getString("nom")
                    );
                }
            }
        }
        return null;
    }

    public boolean typeExists(int typeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM type_reclamation WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, typeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean medecinExists(int medecinId) throws SQLException {
        String query = "SELECT COUNT(*) FROM medecin WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void addType(TypeReclamation type) throws SQLException {
        String query = "INSERT INTO type_reclamation (nom) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type.getNom());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    type.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public boolean updateType(TypeReclamation type) throws SQLException {
        String query = "UPDATE type_reclamation SET nom = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, type.getNom());
            ps.setInt(2, type.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteType(int id) throws SQLException {
        String query = "DELETE FROM type_reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }


    public void updateStatus(int id, String newStatus) throws SQLException {
        String query = "UPDATE reclamation SET status = ? WHERE id = ?";
        try (Connection conn = connBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void executeUpdate(String sql) throws SQLException {
        try (Connection conn = connBD.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }


}