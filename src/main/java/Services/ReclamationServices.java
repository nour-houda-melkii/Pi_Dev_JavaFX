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
        }
    }

    /**
     * Vérifie si une réclamation similaire existe déjà
     */
    public boolean reclamationExists(Reclamation reclamation) throws SQLException {
        String query = "SELECT COUNT(*) FROM reclamation WHERE " +
                "type_reclamation_id = ? AND medecin_id = ? AND date_reclamation = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(reclamation.getTypeReclamation()));
            ps.setInt(2, Integer.parseInt(reclamation.getMedecin()));
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Vérifie si une réclamation similaire existe déjà (pour la mise à jour)
     */
    public boolean reclamationExistsExcludingCurrent(Reclamation reclamation) throws SQLException {
        String query = "SELECT COUNT(*) FROM reclamation WHERE " +
                "type_reclamation_id = ? AND medecin_id = ? AND date_reclamation = ? " +
                "AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(reclamation.getTypeReclamation()));
            ps.setInt(2, Integer.parseInt(reclamation.getMedecin()));
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            ps.setInt(4, Integer.parseInt(reclamation.getId()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Ajoute une nouvelle réclamation
     */
    public void addReclamation(Reclamation reclamation) throws SQLException {
        System.out.println("Tentative d'ajout pour le type: " + reclamation.getTypeReclamation());

        String query = "INSERT INTO reclamation (type_reclamation_id, description, date_reclamation, medecin_id, photo) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, Integer.parseInt(reclamation.getTypeReclamation()));
            ps.setString(2, reclamation.getDescription());
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            ps.setInt(4, Integer.parseInt(reclamation.getMedecin()));
            ps.setString(5, reclamation.getPhotoPath() != null ? reclamation.getPhotoPath() : "");

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("L'ajout a échoué, aucune ligne affectée");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reclamation.setId(String.valueOf(generatedKeys.getInt(1)));
                } else {
                    throw new SQLException("Échec de récupération de l'ID généré");
                }
            }

            System.out.println("Réclamation ajoutée avec ID: " + reclamation.getId());
        }
    }

    /**
     * Met à jour une réclamation existante
     *
     * @return
     */
    public boolean updateReclamation(Reclamation reclamation) throws SQLException {
        String query = "UPDATE reclamation SET type_reclamation_id = ?, description = ?, "
                + "date_reclamation = ?, medecin_id = ?, photo = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(reclamation.getTypeReclamation()));
            ps.setString(2, reclamation.getDescription());
            ps.setDate(3, Date.valueOf(reclamation.getDateReclamation()));
            ps.setInt(4, Integer.parseInt(reclamation.getMedecin()));
            ps.setString(5, reclamation.getPhotoPath());
            ps.setInt(6, Integer.parseInt(reclamation.getId()));

            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        }
    }

    /**
     * Supprime une réclamation
     */
    public void deleteReclamation(int id) throws SQLException {
        String query = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Récupère toutes les réclamations
     */
    public List<Reclamation> getAllReclamations() throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Reclamation rec = new Reclamation(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("type_reclamation_id"),
                        rs.getString("description"),
                        rs.getDate("date_reclamation").toLocalDate(),
                        rs.getString("medecin_id"),
                        rs.getString("photo")
                );
                reclamations.add(rec);
            }
        }
        return reclamations;
    }

    /**
     * Récupère une réclamation par son ID
     */
    public Reclamation getReclamationById(int id) throws SQLException {
        String query = "SELECT * FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Reclamation(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("type_reclamation_id"),
                            rs.getString("description"),
                            rs.getDate("date_reclamation").toLocalDate(),
                            rs.getString("medecin_id"),
                            rs.getString("photo")
                    );
                }
            }
        }
        return null;
    }

    // Méthodes pour les types de réclamation
    public List<TypeReclamation> getAllTypes() throws SQLException {
        List<TypeReclamation> types = new ArrayList<>();
        String sql = "SELECT id, nom FROM type_reclamation ORDER BY id";

        try (Connection conn = connBD.getConnection();
             Statement stmt = conn.createStatement();
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

    // Méthodes pour les médecins
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

    public boolean typeExists(String typeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM type_reclamation WHERE id = ?";
        try (Connection conn = connBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(typeId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean medecinExists(String medecinId) throws SQLException {
        String query = "SELECT COUNT(*) FROM medecin WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(medecinId));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }


}