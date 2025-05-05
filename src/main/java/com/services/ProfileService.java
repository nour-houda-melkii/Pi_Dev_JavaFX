package com.services;

import com.exceptions.AuthException;
import com.models.User;
import com.utils.DataSource;
import com.utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileService {
    private final Connection connection;
    private final AuthService authService;

    public ProfileService() {
        this.connection = DataSource.getInstance().getConnection();
        this.authService = new AuthService();
    }

    /**
     * Met à jour les informations personnelles d'un utilisateur
     * @param token JWT token de l'utilisateur connecté
     * @param userUpdates Objet User contenant les nouvelles informations
     * @return User mis à jour
     * @throws AuthException Si token invalide ou utilisateur non trouvé
     * @throws SQLException En cas d'erreur lors de la mise à jour
     */
    public User updateUserInfo(String token, User userUpdates) throws AuthException, SQLException {
        // 1. Vérifier le token et obtenir l'utilisateur actuel
        User currentUser = authService.getUserFromToken(token);
        if (currentUser == null) {
            throw new AuthException("Utilisateur non authentifié");
        }

        // 2. Préparer la requête SQL avec les champs à mettre à jour
        StringBuilder queryBuilder = new StringBuilder("UPDATE user SET ");

        // Liste pour stocker les paramètres et leurs valeurs
        List<Object> params = new ArrayList<>();

        // Ajouter les champs modifiables à la requête
        if (userUpdates.getFirstName() != null && !userUpdates.getFirstName().isEmpty()) {
            queryBuilder.append("first_name = ?, ");
            params.add(userUpdates.getFirstName());
        }

        if (userUpdates.getLastName() != null && !userUpdates.getLastName().isEmpty()) {
            queryBuilder.append("last_name = ?, ");
            params.add(userUpdates.getLastName());
        }

        if (userUpdates.getAddress() != null) {
            queryBuilder.append("address = ?, ");
            params.add(userUpdates.getAddress());
        }

        if (userUpdates.getPhoneNumber() != null && !userUpdates.getPhoneNumber().isEmpty()) {
            queryBuilder.append("phone_number = ?, ");
            params.add(userUpdates.getPhoneNumber());
        }

        if (userUpdates.getAge() > 0) {
            queryBuilder.append("age = ?, ");
            params.add(userUpdates.getAge());
        }

        // Si c'est un médecin, autoriser la mise à jour des informations spécifiques
        if (currentUser.isMedecin()) {
            if (userUpdates.getSpecialite() != null) {
                queryBuilder.append("specialite = ?, ");
                params.add(userUpdates.getSpecialite().toString());
            }

            if (userUpdates.getNumeroLicence() != null && !userUpdates.getNumeroLicence().isEmpty()) {
                queryBuilder.append("numero_licence = ?, ");
                params.add(userUpdates.getNumeroLicence());
            }
        }

        // Vérifier si des champs ont été mis à jour
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Aucune information à mettre à jour");
        }

        // Supprimer la dernière virgule et espace
        String query = queryBuilder.substring(0, queryBuilder.length() - 2);

        // Ajouter la clause WHERE
        query += " WHERE id = ?";
        params.add(currentUser.getId());

        // 3. Exécuter la requête
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            // Définir les paramètres
            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            int rowsUpdated = pst.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("La mise à jour a échoué, aucune ligne modifiée");
            }

            System.out.println("✅ Informations utilisateur mises à jour pour: " + currentUser.getEmail());
        }

        // 4. Récupérer l'utilisateur mis à jour
        return authService.getUserFromToken(token);
    }

    /**
     * Permet de changer le mot de passe d'un utilisateur connecté
     * @param token JWT token de l'utilisateur
     * @param currentPassword Mot de passe actuel
     * @param newPassword Nouveau mot de passe
     * @throws AuthException Si mot de passe incorrect ou token invalide
     * @throws SQLException En cas d'erreur lors de la mise à jour
     */
    public void changePassword(String token, String currentPassword, String newPassword)
            throws AuthException, SQLException {
        // 1. Vérifier le token et obtenir l'utilisateur
        User user = authService.getUserFromToken(token);
        if (user == null) {
            throw new AuthException("Utilisateur non authentifié");
        }

        // 2. Vérifier le mot de passe actuel
        String query = "SELECT password FROM user WHERE id = ?";
        String storedPassword;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, user.getId());
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Utilisateur introuvable");
                }
                storedPassword = rs.getString("password");
            }
        }

        // Vérification du mot de passe actuel
        if (!PasswordHasher.checkPassword(currentPassword, storedPassword)) {
            throw new AuthException("Mot de passe actuel incorrect");
        }

        // 3. Hasher et mettre à jour le nouveau mot de passe
        String hashedNewPassword = PasswordHasher.hashPassword(newPassword);

        query = "UPDATE user SET password = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, hashedNewPassword);
            pst.setInt(2, user.getId());

            int rowsUpdated = pst.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("La mise à jour du mot de passe a échoué");
            }

            System.out.println("✅ Mot de passe changé avec succès pour: " + user.getEmail());
        }
    }

    /**
     * Récupère le profil complet d'un utilisateur
     * @param token JWT token de l'utilisateur
     * @return User objet complet avec toutes les informations
     * @throws AuthException Si token invalide
     */
    public User getUserProfile(String token) throws AuthException {
        return authService.getUserFromToken(token);
    }

    /**
     * Supprimer le compte utilisateur (méthode supplémentaire)
     *
     * @param token    JWT token de l'utilisateur
     * @param password Mot de passe pour confirmation
     * @return
     * @throws AuthException Si mot de passe incorrect ou token invalide
     * @throws SQLException  En cas d'erreur lors de la suppression
     */
    public boolean deleteUserAccount(String token, String password) throws AuthException, SQLException {
        // 1. Vérifier le token et obtenir l'utilisateur
        User user = authService.getUserFromToken(token);
        if (user == null) {
            throw new AuthException("Utilisateur non authentifié");
        }

        // 2. Vérifier le mot de passe pour confirmation
        String query = "SELECT password FROM user WHERE id = ?";
        String storedPassword;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, user.getId());
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Utilisateur introuvable");
                }
                storedPassword = rs.getString("password");
            }
        }

        // Vérification du mot de passe
        if (!PasswordHasher.checkPassword(password, storedPassword)) {
            throw new AuthException("Mot de passe incorrect pour la suppression du compte");
        }

        // 3. Supprimer le compte
        query = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, user.getId());

            int rowsDeleted = pst.executeUpdate();
            if (rowsDeleted == 0) {
                return false; // Aucune ligne supprimée
            }

            // 4. Invalider le token
            authService.logout(token);

            System.out.println("✅ Compte supprimé avec succès pour: " + user.getEmail());
            return true; // Suppression réussie
        }
    }
}