package services;

import entity.Reaction;
import entity.Reclamation;
import utils.connBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactionService {
    private Connection connection;

    public ReactionService() {
        try {
            connection = connBD.getConnection();
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    public boolean addReaction(Reaction reaction) throws SQLException {
        // Vérifier si la réaction est valide
        if (reaction == null) {
            System.err.println("Erreur: tentative d'ajout d'une réaction null");
            return false;
        }

        // Si la réaction est associée à une réponse
        if (reaction.getReponseId() > 0) {
            // Créer une notification
            NotificationService.getInstance().sendBackendNotification(
                    "Nouvelle réaction " + getEmojiForType(reaction.getType()) + " sur une réponse à la réclamation");

            return addReponseReaction(reaction);
        }

        // Pour les réactions sur les réclamations directement
        // Vérifier si l'utilisateur a déjà réagi avec ce type
        if (userHasReacted(reaction.getReclamationId(), reaction.getUtilisateur(), reaction.getType())) {
            // Si oui, supprimer cette réaction (toggle)
            return removeUserReactionByType(reaction.getReclamationId(), reaction.getUtilisateur(), reaction.getType());
        }

        // Sinon, ajouter la nouvelle réaction
        String query = "INSERT INTO reaction (reclamation_id, type, utilisateur, date_reaction) " +
                "VALUES (?, ?, ?, ?)";

        Connection connection = connBD.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reaction.getReclamationId());
            statement.setString(2, reaction.getType());
            statement.setString(3, reaction.getUtilisateur());
            statement.setDate(4, Date.valueOf(reaction.getDateReaction()));

            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reaction.setId(generatedKeys.getInt(1));
                    }
                }


                // Notifier après ajout réussi
                NotificationService.getInstance().sendNotification(
                        "Nouvelle réaction " + getEmojiForType(reaction.getType()) + " sur une réclamation");

                return true;
            }
            return false;
        }
    }

    private String getEmojiForType(String type) {
        if (type == null) {
            return "👍";
        }

        switch (type) {
            case "LIKE": return "👍";
            case "LOVE": return "❤️";
            case "HAHA": return "😂";
            case "WOW": return "😮";
            case "SAD": return "😢";
            case "ANGRY": return "😡";
            default: return "👍";
        }
    }

    public Map<String, Integer> getReactionCountsForReponse(int reponseId) throws SQLException {
        Map<String, Integer> reactionCounts = new HashMap<>();
        String query = "SELECT type, COUNT(*) as count FROM reaction WHERE reponse_id = ? GROUP BY type";

        try (Connection connection = connBD.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("type");
                    int count = resultSet.getInt("count");
                    reactionCounts.put(type, count);
                }
            }
        }
        return reactionCounts;
    }

   /* public boolean removeReaction(int reponseId, String utilisateur, String type) throws SQLException {
        String query = "DELETE FROM reaction WHERE reponse_id = ? AND utilisateur = ? AND type = ?";

        try (Connection connection = connBD.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        }
    }*/

    public boolean removeUserReactionByType(int reclamationId, String utilisateur, String type) throws SQLException {
        String query = "DELETE FROM reaction WHERE reclamation_id = ? AND utilisateur = ? AND type = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reclamationId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            return statement.executeUpdate() > 0;
        }
    }

    public List<Reaction> getReactionsForReclamation(int reclamationId) throws SQLException {
        List<Reaction> reactions = new ArrayList<>();
        String query = "SELECT * FROM reaction WHERE reclamation_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reclamationId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Reaction reaction = new Reaction();
                    reaction.setId(rs.getInt("id"));
                    reaction.setReclamationId(rs.getInt("reclamation_id"));
                    reaction.setType(rs.getString("type"));
                    reaction.setUtilisateur(rs.getString("utilisateur"));
                    reaction.setDateReaction(rs.getDate("date_reaction").toLocalDate());
                    reactions.add(reaction);
                }
            }
        }
        return reactions;
    }

    public Map<String, Integer> getReactionCountByType(int reclamationId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        String query = "SELECT type, COUNT(*) as count FROM reaction WHERE reclamation_id = ? GROUP BY type";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reclamationId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("type"), rs.getInt("count"));
                }
            }
        }
        return counts;
    }

    public boolean userHasReacted(int reclamationId, String utilisateur, String type) throws SQLException {
        String query = "SELECT COUNT(*) FROM reaction WHERE reclamation_id = ? AND utilisateur = ? AND type = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reclamationId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Ajoute une réaction à une réponse
     * @param reaction L'objet Reaction à ajouter
     * @return true si l'ajout a réussi, false sinon
     * @throws SQLException En cas d'erreur SQL
     */
    private boolean addReponseReaction(Reaction reaction) throws SQLException {
        // Vérifier si l'utilisateur a déjà réagi avec ce type
        if (userHasReactedToReponse(reaction.getReponseId(), reaction.getUtilisateur(), reaction.getType())) {
            // Si oui, supprimer cette réaction (toggle)
            return removeUserReactionToReponseByType(reaction.getReponseId(), reaction.getUtilisateur(), reaction.getType());
        }

        // D'abord, récupérer l'ID de la réclamation associée à cette réponse
        int reclamationId = getReclamationIdForReponse(reaction.getReponseId());

        if (reclamationId <= 0) {
            System.err.println("Erreur: impossible de trouver la réclamation associée à la réponse ID: " + reaction.getReponseId());
            return false;
        }

        // Sinon, ajouter la nouvelle réaction
        String query = "INSERT INTO reaction (reponse_id, reclamation_id, type, utilisateur, date_reaction) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection connection = connBD.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reaction.getReponseId());
            statement.setInt(2, reclamationId);  // Ajouter l'ID de la réclamation
            statement.setString(3, reaction.getType());
            statement.setString(4, reaction.getUtilisateur());
            statement.setDate(5, Date.valueOf(reaction.getDateReaction()));

            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reaction.setId(generatedKeys.getInt(1));
                    }
                }

                // Notifier après ajout réussi
                NotificationService.getInstance().sendNotification(
                        "Nouvelle réaction " + getEmojiForType(reaction.getType()) + " sur une réponse");

                return true;
            }
            return false;
        }
    }

    /**
     * Récupère l'ID de la réclamation associée à une réponse
     * @param reponseId L'ID de la réponse
     * @return L'ID de la réclamation, ou -1 si non trouvé
     * @throws SQLException En cas d'erreur SQL
     */
    private int getReclamationIdForReponse(int reponseId) throws SQLException {
        String query = "SELECT reclamation_id FROM reponse WHERE id = ?";

        Connection connection = connBD.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("reclamation_id");
                }
            }
        }
        return -1;
    }

    public boolean userHasReactedToReponse(int reponseId, String utilisateur, String type) throws SQLException {
        String query = "SELECT COUNT(*) FROM reaction WHERE reponse_id = ? AND utilisateur = ? AND type = ?";

        try (Connection connection = connBD.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean removeUserReactionToReponseByType(int reponseId, String utilisateur, String type) throws SQLException {
        String query = "DELETE FROM reaction WHERE reponse_id = ? AND utilisateur = ? AND type = ?";

        try (Connection connection = connBD.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        }
    }

   /* public boolean removeUserReactionsForReponse(int reponseId, String utilisateur) throws SQLException {
        String query = "DELETE FROM reaction WHERE reponse_id = ? AND utilisateur = ?";  // Utilisation de la table reaction

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);

            return statement.executeUpdate() >= 0; // Succès même si aucune ligne n'est supprimée
        }
    }*/

    public List<Reaction> getReactionsForReponse(int reponseId) throws SQLException {
        List<Reaction> reactions = new ArrayList<>();
        String query = "SELECT * FROM reaction WHERE reponse_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Reaction reaction = new Reaction();
                    reaction.setId(rs.getInt("id"));
                    reaction.setReponseId(rs.getInt("reponse_id"));
                    reaction.setReclamationId(rs.getInt("reclamation_id"));
                    reaction.setType(rs.getString("type"));
                    reaction.setUtilisateur(rs.getString("utilisateur"));
                    reaction.setDateReaction(rs.getDate("date_reaction").toLocalDate());
                    reactions.add(reaction);
                }
            }
        }
        return reactions;
    }

    /*public boolean removeReponseReaction(int reactionId) throws SQLException {
        String query = "DELETE FROM reaction WHERE id = ? AND reponse_id IS NOT NULL";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reactionId);

            return statement.executeUpdate() > 0;
        }
    }*/

    public Map<String, Integer> getReactionCountByTypeForReponse(int reponseId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        String query = "SELECT type, COUNT(*) as count FROM reaction WHERE reponse_id = ? GROUP BY type";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("type"), rs.getInt("count"));
                }
            }
        }
        return counts;
    }

    public String getUserReactionTypeForReponse(int reponseId, String utilisateur) throws SQLException {
        // Vérifier l'existence de la table reaction plutôt que reponse_reaction
        if (!tableExists("reaction")) {
            throw new SQLException("Table 'reaction' doesn't exist");
        }

        String query = "SELECT type FROM reaction WHERE reponse_id = ? AND utilisateur = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("type");
                }
            }
        }
        return null; // Aucune réaction trouvée
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

    public boolean tableExists(String tableName) {
        boolean exists = false;
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, null);
            exists = rs.next();
            rs.close();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'existence de la table: " + e.getMessage());
        }
        return exists;
    }



    // Dans la classe NotificationService
    public void sendNotificationToBackend(String message) {
        // Chercher tous les contrôleurs backend enregistrés et leur envoyer la notification
        for (BackendNotificationListener listener : backendListeners) {
            listener.receiveNotification(message);
        }
    }

    // Ajoutez une liste pour stocker les écouteurs de notifications backend
    private final List<BackendNotificationListener> backendListeners = new ArrayList<>();

    // Méthode pour ajouter un écouteur de notifications backend
    public void addBackendNotificationListener(BackendNotificationListener listener) {
        backendListeners.add(listener);
    }

    public boolean addReaction(int reponseId, String utilisateur, String type) throws SQLException {
        // First get the associated reclamation_id
        int reclamationId = getReclamationIdForReponse(reponseId);
        if (reclamationId <= 0) {
            throw new SQLException("Could not find associated reclamation for response: " + reponseId);
        }

        String query = "INSERT INTO reaction (reponse_id, reclamation_id, utilisateur, type, date_reaction) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setInt(2, reclamationId);
            statement.setString(3, utilisateur);
            statement.setString(4, type);
            statement.setDate(5, Date.valueOf(LocalDate.now()));

            return statement.executeUpdate() > 0;
        }
    }

    public boolean removeReaction(int reponseId, String utilisateur, String type) throws SQLException {
        String query = "DELETE FROM reaction WHERE reponse_id = ? AND utilisateur = ? AND type = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);
            statement.setString(3, type);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean removeUserReactionsForReponse(int reponseId, String utilisateur) throws SQLException {
        String query = "DELETE FROM reaction WHERE reponse_id = ? AND utilisateur = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reponseId);
            statement.setString(2, utilisateur);

            return statement.executeUpdate() > 0;
        }
    }

    // Interface pour les écouteurs de notifications backend
    public interface BackendNotificationListener {
        void receiveNotification(String message);
    }
    public boolean removeReaction(int reactionId, String currentUser) throws SQLException {
        String query = "DELETE FROM reaction WHERE id = ? AND utilisateur = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, reactionId);
            statement.setString(2, currentUser);

            return statement.executeUpdate() > 0;
        }
    }
}