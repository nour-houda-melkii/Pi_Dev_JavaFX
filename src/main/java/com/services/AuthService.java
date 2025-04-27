
package com.services;

import com.demo.enums.Gender;
import com.demo.enums.Specialite;
import com.exceptions.AuthException;
import com.exceptions.SmsException;
import com.models.Medecin;
import com.models.User;
import com.demo.enums.Role;
import com.utils.*;
import com.utils.EmailService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.mail.MessagingException;
import java.io.*;
import java.sql.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static javax.crypto.Cipher.SECRET_KEY;

public class AuthService {
    private final Connection connection;
    private final SmsService smsService;
    private final EmailService emailService;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOCK_TIME_MINUTES = 30;
    private ServiceMedecin serviceMedecin;

    public AuthService() {
        this.connection = DataSource.getInstance().getConnection();
        this.smsService = new SmsService();
        this.emailService = new EmailService();
        serviceMedecin = new ServiceMedecin(connection);
    }

    /**
     * Authentifie un utilisateur
     * @param email Email de l'utilisateur
     * @param password Mot de passe en clair
     * @return User authentifié
     * @throws AuthException Si l'email ou le mot de passe est incorrect
     */
    public String login(String email, String password) throws AuthException {
        // Vérifier si le compte est bloqué
        checkIfAccountIsLocked(email);

        String query = "SELECT * FROM user WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    incrementLoginAttempts(email);
                    throw new AuthException("Email ou mot de passe incorrect");
                }

                // Vérifier le statut de vérification
                boolean isVerified = rs.getBoolean("is_verified");
                String status = rs.getString("status");

                if (!isVerified || !"verifie".equals(status)) {
                    throw new AuthException("Votre compte n'est pas encore vérifié. Veuillez vérifier votre email ou contacter l'administrateur.");
                }

                String storedHash = rs.getString("password");
                System.out.println("Stored hash: " + storedHash); // Log pour débogage

                // Vérifier que le hash stocké est valide
                if (storedHash == null || storedHash.isEmpty()) {
                    throw new AuthException("Invalid password hash in database");
                }

                // Vérifier le mot de passe avec gestion d'erreur améliorée
                boolean passwordValid;
                try {
                    passwordValid = PasswordHasher.checkPassword(password, storedHash);
                } catch (Exception e) {
                    System.err.println("Password verification error: " + e.getMessage());
                    throw new AuthException("Erreur de vérification du mot de passe");
                }

                if (!passwordValid) {
                    incrementLoginAttempts(email);
                    throw new AuthException("Email ou mot de passe incorrect");
                }

                // Si le mot de passe est correct, réinitialiser les tentatives
                resetLoginAttempts(email);

                User user = mapResultSetToUser(rs);
                System.out.println("✅ Connexion réussie pour: " + email);
                return JwtUtil.generateToken(user);
            }
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de l'authentification: " + e.getMessage());
        }
    }

////////////////////////////////////////////////////////////
    public User getUserFromToken(String token) throws AuthException {
        try {
            // 1. Parser le token avec JwtUtil
            Claims claims = JwtUtil.parseToken(token);

            // 2. Récupérer l'ID
            Long userId = claims.get("userId", Long.class);

            // 3. Requête SQL
            String sql = "SELECT * FROM user WHERE id = ?";

            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setLong(1, userId);

                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
            throw new AuthException("Utilisateur non trouvé");

        } catch (JwtException e) {
            throw new AuthException("Token invalide: " + e.getMessage());
        } catch (SQLException e) {
            throw new AuthException("Erreur base de données");
        }
    }


    public void logout(String token) throws AuthException {
        try {
            // Valide d'abord le token (vérifie signature et expiration)
            Claims claims = JwtUtil.parseToken(token);

            // Ajoute à la blacklist
            JwtBlacklist.invalidateToken(token);

            System.out.println("✅ Déconnexion réussie");

        } catch (JwtException e) {
            throw new AuthException("Échec de la déconnexion: " + e.getMessage());
        }
    }

    private void checkIfAccountIsLocked(String email) throws AuthException {
        String query = "SELECT is_blocked, blocked_until FROM user WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    boolean isLocked = rs.getBoolean("is_blocked");
                    Timestamp lockUntil = rs.getTimestamp("blocked_until");

                    if (isLocked && lockUntil != null && lockUntil.after(new Timestamp(System.currentTimeMillis()))) {
                        throw new AuthException("Compte temporairement bloqué. Réessayez plus tard.");
                    } else if (isLocked) {
                        // Débloquer le compte si la période de blocage est expirée
                        unlockAccount(email);
                    }
                }
            }
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la vérification du statut du compte");
        }
    }

    private void incrementLoginAttempts(String email) throws AuthException {
        String query = "UPDATE user SET failed_login_attempts = failed_login_attempts + 1 WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            pst.executeUpdate();

            // Vérifier si on doit bloquer le compte
            checkAndLockAccountIfNeeded(email);
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la mise à jour des tentatives de connexion");
        }
    }

    private void checkAndLockAccountIfNeeded(String email) throws AuthException {
        String query = "SELECT failed_login_attempts FROM user WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next() && rs.getInt("login_attempts") >= MAX_LOGIN_ATTEMPTS) {
                    lockAccount(email);
                    sendLockNotificationEmail(email);
                }
            }
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la vérification des tentatives de connexion");
        }
    }

    private void lockAccount(String email) throws SQLException {
        Timestamp lockUntil = new Timestamp(System.currentTimeMillis() + (LOCK_TIME_MINUTES * 60 * 1000));

        String query = "UPDATE user SET is_blocked = TRUE, blocked_until = ? WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setTimestamp(1, lockUntil);
            pst.setString(2, email);
            pst.executeUpdate();
        }
    }

    private void unlockAccount(String email) throws SQLException {
        String query = "UPDATE user SET is_blocked = FALSE, blocked_until = NULL, failed_login_attempts = 0 WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            pst.executeUpdate();
        }
    }

    private void resetLoginAttempts(String email) throws SQLException {
        String query = "UPDATE user SET failed_login_attempts = 0 WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            pst.executeUpdate();
        }
    }

    private void sendLockNotificationEmail(String email) {
        try {
            // Récupérer l'adresse IP (c'est un exemple, à adapter selon votre configuration)
            String ipAddress = getClientIpAddress(); // Méthode à implémenter

            // Créer le lien Google Maps (note: c'est une approximation basée sur IP)
            String mapsLink = "https://www.google.com/maps/search/?api=1&query=" + ipAddress;

            String subject = "Sécurité de votre compte - Tentatives de connexion échouées";
            String content = "Bonjour,\n\n" +
                    "Nous avons détecté plusieurs tentatives de connexion infructueuses sur votre compte.\n" +
                    "Pour des raisons de sécurité, votre compte a été temporairement bloqué pour " +
                    LOCK_TIME_MINUTES + " minutes.\n\n" +
                    "Localisation approximative de la tentative: " + mapsLink + "\n\n" +
                    "Si vous êtes à l'origine de ces tentatives, veuillez réessayer plus tard.\n" +
                    "Si vous n'êtes pas à l'origine de ces tentatives, nous vous recommandons de:\n" +
                    "1. Changer immédiatement votre mot de passe\n" +
                    "2. Activer l'authentification à deux facteurs\n\n" +
                    "Cordialement,\nL'équipe de support";

            EmailService.sendEmail("noreply@votredomaine.com", email, subject, content);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de notification: " + e.getMessage());
        }
    }

    // Méthode exemple pour récupérer l'IP (à adapter)
    private String getClientIpAddress() {
        // Implémentation basique - en réalité cela dépend de votre framework
        // Par exemple dans une webapp:
        // return HttpContext.Current.Request.UserHostAddress;
        return "Unknown"; // Remplacez par la vraie implémentation
    }

    /**
     * Vérifie si l'utilisateur a le rôle requis
     * @param user Utilisateur à vérifier
     * @param requiredRole Rôle requis
     * @return boolean true si le rôle correspond
     */
    public boolean checkRole(User user, String requiredRole) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains(requiredRole);
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));

        user.setRolesFromJson(rs.getString("roles"));

        user.setAddress(rs.getString("adress"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setAge(rs.getInt("age"));

        // Gestion du genre avec vérification de null
        String genderStr = rs.getString("gender");
        if (genderStr != null && !genderStr.isEmpty()) {
            user.setGender(Gender.valueOf(genderStr));
        }

        user.setVerificationCode(rs.getString("verification_code"));
        user.setVerificationCodeExpiration(rs.getTimestamp("verification_code_expiration"));
        user.setMedicalFile(rs.getString("medical_file"));

        // Gestion des champs spécifiques aux médecins
        if (user.getRoles() != null && user.getRoles().contains(User.ROLE_MEDECIN)) {
            user.setNumeroLicence(rs.getString("numero_licence"));

            // Gestion de la spécialité avec vérification de null
            String specialiteStr = rs.getString("specialite");
            if (specialiteStr != null && !specialiteStr.isEmpty()) {
                user.setSpecialite(Specialite.valueOf(specialiteStr));
            }
        }
        user.setVerified(rs.getBoolean("is_verified"));
        user.setStatus(rs.getString("status"));

        return user;
    }


    /**
     * Enregistre un nouveau médecin
     * @param medecin Objet User avec role=MEDECIN et ses attributs spécifiques
     * @return User enregistré avec son ID
     * @throws SQLException Si l'email existe déjà ou erreur SQL
     */
    public User registerMedecin(User medecin) throws SQLException {
        // Vérification du rôle
        if (medecin.getRoles() == null || !medecin.getRoles().contains(User.ROLE_MEDECIN)) {
            throw new IllegalArgumentException("L'utilisateur doit avoir le rôle ROLE_MEDECIN");
        }

        // Vérification des champs obligatoires
        if (medecin.getNumeroLicence() == null || medecin.getNumeroLicence().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de licence est obligatoire");
        }
        if (medecin.getSpecialite() == null) {
            throw new IllegalArgumentException("La spécialité est obligatoire");
        }

        return registerUser(medecin);
    }

    /**
     * Enregistre un nouveau patient
     * @param patient Objet User avec role=PATIENT
     * @return User enregistré avec son ID
     * @throws SQLException Si l'email existe déjà ou erreur SQL
     */
    public User registerPatient(User patient) throws SQLException {
        if (patient.getRoles() == null || !patient.getRoles().contains(User.ROLE_USER)) {
            throw new IllegalArgumentException("Le rôle doit être PATIENT");
        }

        return registerUser(patient);
    }

    /**
     * Méthode commune d'enregistrement
     */
    private User registerUser(User user) throws SQLException {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(List.of(user.ROLE_USER)); // Valeur par défaut
        }

        // Vérifier si l'email existe déjà
        if (emailExists(user.getEmail())) {
            throw new SQLException("This email is already in use.");
        }

        if (user.isMedecin() && licenseExists(user.getNumeroLicence())) {
            throw new SQLException("This license number is already in use.");
        }

        // Hasher le mot de passe
        String hashedPassword = PasswordHasher.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);

        // Définir le statut de vérification selon le rôle
        if (user.isMedecin()) {
            // Médecins doivent être vérifiés manuellement
            user.setStatus("non_verifie");
        } else {
            // Autres utilisateurs sont vérifiés automatiquement
            user.setStatus("verifie");
        }

        Connection conn = null;
        try {
            conn = connection; // Utilisez votre connexion existante ou créez-en une nouvelle
            conn.setAutoCommit(false); // Désactivez l'auto-commit pour gérer la transaction manuellement

            // 1. Insertion dans la table user
            String userQuery = "INSERT INTO user (email, password, first_name, last_name, roles, " +
                    "adress, phone_number, age, gender, numero_licence, specialite, is_verified, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement userStmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS)) {
                int paramIndex = 1;
                userStmt.setString(paramIndex++, user.getEmail());
                userStmt.setString(paramIndex++, user.getPassword());
                userStmt.setString(paramIndex++, user.getFirstName());
                userStmt.setString(paramIndex++, user.getLastName());
                userStmt.setString(paramIndex++, user.getRolesAsJson());
                userStmt.setString(paramIndex++, user.getAddress());
                userStmt.setString(paramIndex++, user.getPhoneNumber());
                userStmt.setInt(paramIndex++, user.getAge());
                userStmt.setString(paramIndex++, user.getGender().toString());

                if (user.isMedecin()) {
                    userStmt.setString(paramIndex++, user.getNumeroLicence());
                    userStmt.setString(paramIndex++, user.getSpecialite().toString());
                } else {
                    userStmt.setNull(paramIndex++, Types.VARCHAR); // numero_licence
                    userStmt.setNull(paramIndex++, Types.VARCHAR); // specialite
                }
                userStmt.setBoolean(paramIndex++, true); // is_verified
                userStmt.setString(paramIndex++, user.getStatus());

                int affectedRows = userStmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Échec de l'enregistrement, aucune ligne affectée");
                }

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        user.setId(userId);

                        // 2. Insertion dans la table appropriée selon le rôle
                        if (user.getRoles().contains("ROLE_MEDECIN") || user.getRoles().contains("MEDECIN")) {
                            Medecin medecin = new Medecin( user.getId(), "1");
                            serviceMedecin.ajouter(medecin);
                        }
                        else if (user.getRoles().contains("ROLE_USER") || user.getRoles().contains("PATIENT")) {
                            String patientQuery = "INSERT INTO patient (user_id) VALUES (?)";
                            try (PreparedStatement patientStmt = conn.prepareStatement(patientQuery)) {
                                patientStmt.setInt(1, userId);
                                patientStmt.executeUpdate();
                            }
                        }

                        conn.commit(); // Validez la transaction
                        return user;
                    } else {
                        throw new SQLException("Échec de l'enregistrement, aucun ID obtenu");
                    }
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Annulez la transaction en cas d'erreur
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Relancez l'exception
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Réactivez l'auto-commit
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean licenseExists(String numeroLicence) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE numero_licence = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, numeroLicence);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }



    /**
     * Génère et sauvegarde un code de vérification pour la réinitialisation du mot de passe
     * @param email Email de l'utilisateur
     * @return Le code généré (pour l'envoyer par email/SMS)
     * @throws AuthException Si l'utilisateur n'existe pas ou erreur SQL
     */
    public String generateAndSaveVerificationCode(String email) throws AuthException, SQLException {
        // 1. Vérifier d'abord si l'email existe
        if (!emailExists(email)) {
            throw new AuthException("User not found");
        }

        // 2. Récupérer le numéro de téléphone
        String phoneNumber = "+216" + getPhoneNumberByEmail(email);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new AuthException("No phone number associated with this user");
        }

        // 3. Générer le code
        String verificationCode = String.format("%06d", new java.util.Random().nextInt(999999));
        Timestamp expiration = new Timestamp(System.currentTimeMillis() + 15 * 60 * 1000);

        // 4. Sauvegarder en base
        String query = "UPDATE user SET verification_code = ?, verification_code_expiration = ? WHERE email = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, verificationCode);
            pst.setTimestamp(2, expiration);
            pst.setString(3, email);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new AuthException("Error saving verification code: " + e.getMessage());
        }

        // 5. Envoyer par SMS
        try {
            smsService.sendVerificationSms(phoneNumber, verificationCode);
            return verificationCode;
        } catch (SmsException e) {
            // En cas d'échec d'envoi SMS, nettoyer le code en base
            try (PreparedStatement pst = connection.prepareStatement(
                    "UPDATE user SET verification_code = NULL, verification_code_expiration = NULL WHERE email = ?")) {
                pst.setString(1, email);
                pst.executeUpdate();
            } catch (SQLException sqlEx) {
                e.addSuppressed(sqlEx);
            }
            throw new AuthException("Failed to send SMS: " + e.getMessage());
        }
    }

    private String getPhoneNumberByEmail(String email) throws SQLException {
        String query = "SELECT phone_number FROM user WHERE email = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getString("phone_number") : null;
            }
        }
    }


    /**
     * Vérifie si le code de vérification est valide
     * @param email Email de l'utilisateur
     * @param code Code saisi par l'utilisateur
     * @return boolean true si le code est valide
     * @throws AuthException Si erreur SQL ou code invalide
     */
    public boolean verifyCode(String email, String code) throws AuthException {
        String query = "SELECT verification_code, verification_code_expiration FROM user WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Aucun utilisateur trouvé avec cet email");
                }

                String dbCode = rs.getString("verification_code");
                Timestamp expiration = rs.getTimestamp("verification_code_expiration");

                // Vérifier le code et l'expiration
                if (dbCode == null || !dbCode.equals(code)) {
                    throw new AuthException("Code de vérification incorrect");
                }

                if (expiration.before(new Timestamp(System.currentTimeMillis()))) {
                    throw new AuthException("Code expiré");
                }

                return true; // Code valide
            }
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la vérification du code: " + e.getMessage());
        }
    }

    /**
     * Réinitialise le mot de passe après vérification du code
     * @param email Email de l'utilisateur
     * @param code Code de vérification
     * @param newPassword Nouveau mot de passe
     * @throws AuthException Si le code est invalide ou erreur SQL
     */
    public void resetPasswordWithCode(String email, String code, String newPassword) throws AuthException {
        // Vérifier d'abord le code
        if (!verifyCode(email, code)) {
            throw new AuthException("Code invalide ou expiré");
        }

        // Hasher le nouveau mot de passe
        String hashedPassword = PasswordHasher.hashPassword(newPassword);

        String query = "UPDATE user SET password = ?, verification_code = NULL, verification_code_expiration = NULL WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, hashedPassword);
            pst.setString(2, email);

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                throw new AuthException("Échec de la réinitialisation du mot de passe");
            }

            System.out.println("✅ Mot de passe réinitialisé avec succès pour: " + email);
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la réinitialisation: " + e.getMessage());
        }
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
        User currentUser = getUserFromToken(token);
        if (currentUser == null) {
            throw new AuthException("Utilisateur non authentifié");
        }

        // 2. Vérifier si l'email est modifié et valider le nouvel email
        if (userUpdates.getEmail() != null && !userUpdates.getEmail().isEmpty()) {
            if (!userUpdates.getEmail().equals(currentUser.getEmail())) {
                // Valider le format de l'email
                if (!isValidEmail(userUpdates.getEmail())) {
                    throw new AuthException("Format d'email invalide");
                }

                // Vérifier si le nouvel email existe déjà (pour un autre utilisateur)
                if (emailExists(userUpdates.getEmail(), currentUser.getId())) {
                    throw new AuthException("Cet email est déjà utilisé par un autre compte");
                }
            }
        } else {
            throw new AuthException("L'email ne peut pas être vide");
        }

        // 2. Préparer la requête SQL avec les champs à mettre à jour
        StringBuilder queryBuilder = new StringBuilder("UPDATE user SET ");

        // Liste pour stocker les paramètres et leurs valeurs
        java.util.List<Object> params = new java.util.ArrayList<>();

        // Ajouter les champs modifiables à la requête
        queryBuilder.append("email = ?, ");
        params.add(userUpdates.getEmail());

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
            queryBuilder.append("adress = ?, ");
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

        if (userUpdates.getGender() != null) {
            queryBuilder.append("gender = ?, ");
            params.add(userUpdates.getGender().toString());
        }


        // Si c'est un médecin, autoriser la mise à jour des informations spécifiques
        if (currentUser.isMedecin()) {
            if (userUpdates.getSpecialite() != null) {
                queryBuilder.append("specialite = ?, ");
                params.add(userUpdates.getSpecialite().toString());
            }

            // Le numéro de licence est généralement fixe, mais on peut permettre de le corriger
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
        }

        // 4. Récupérer l'utilisateur mis à jour
        return getUserFromToken(token);
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
        User user = getUserFromToken(token);
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
     * Vérifie si un email existe déjà (pour un autre utilisateur)
     */
    private boolean emailExists(String email, int excludeUserId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ? AND id != ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            pst.setInt(2, excludeUserId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Upload a medical file for a user (similar to Symfony implementation)
     * @param token JWT token for authentication
     * @param fileBytes Byte array of the file content
     * @param originalFilename Original filename
     * @return The generated filename
     * @throws AuthException If authentication fails
     * @throws SQLException If database error occurs
     */
    public String uploadMedicalFile(String token, byte[] fileBytes, String originalFilename)
            throws AuthException, SQLException {

        // 1. Verify user from token
        User user = getUserFromToken(token);
        if (user == null) {
            throw new AuthException("User not authenticated");
        }

        // 2. Generate unique filename
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // 3. Define upload directory (configure this properly)
        String uploadDir = "uploads/medical_files/";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 4. Save file to filesystem
        try (FileOutputStream fos = new FileOutputStream(uploadDir + uniqueFilename)) {
            fos.write(fileBytes);
        } catch (IOException e) {
            throw new AuthException("Failed to save file: " + e.getMessage());
        }

        // 5. Update user in database
        String sql = "UPDATE user SET medical_file = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, uniqueFilename);
            pst.setInt(2, user.getId());
            pst.executeUpdate();
        }

        return uniqueFilename;
    }


    /**
     * Récupère le contenu du fichier médical d'un utilisateur
     * @param token JWT token de l'utilisateur
     * @return Pair contenant le nom du fichier et son contenu en bytes
     * @throws AuthException Si l'utilisateur n'est pas authentifié ou n'a pas de fichier
     * @throws IOException Si erreur de lecture du fichier
     */
    public AbstractMap.SimpleEntry<String, byte[]> viewMedicalFile(String token) throws AuthException, IOException {
        // 1. Vérifier l'utilisateur
        User user = getUserFromToken(token);
        if (user == null) {
            throw new AuthException("Utilisateur non authentifié");
        }

        // 2. Vérifier si un fichier existe
        if (user.getMedicalFile() == null || user.getMedicalFile().isEmpty()) {
            throw new AuthException("Aucun fichier médical trouvé pour cet utilisateur");
        }

        // 3. Chemin du fichier (même chemin que pour l'upload)
        String uploadDir = "uploads/medical_files/";
        String filePath = uploadDir + user.getMedicalFile();

        // 4. Lire le fichier
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] fileContent = fis.readAllBytes();
            return new AbstractMap.SimpleEntry<>(user.getMedicalFile(), fileContent);
        } catch (FileNotFoundException e) {
            throw new AuthException("Fichier introuvable sur le serveur");
        }
    }


    /**
     * Télécharge le fichier médical sur le PC local
     * @param token JWT token de l'utilisateur
     * @param destinationPath Chemin local où sauvegarder le fichier
     * @throws AuthException Si l'utilisateur n'est pas authentifié ou n'a pas de fichier
     * @throws IOException Si erreur de lecture/écriture des fichiers
     */
    public void downloadMedicalFile(String token, String destinationPath) throws AuthException, IOException {
        // 1. Récupérer le fichier
        AbstractMap.SimpleEntry<String, byte[]> fileEntry = viewMedicalFile(token);

        // 2. Créer le chemin de destination complet
        File destinationFile = new File(destinationPath);
        if (destinationFile.isDirectory()) {
            // Si c'est un dossier, ajouter le nom du fichier
            destinationFile = new File(destinationPath + File.separator + fileEntry.getKey());
        }

        // 3. Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            fos.write(fileEntry.getValue());
        }

        System.out.println("✅ Fichier téléchargé avec succès: " + destinationFile.getAbsolutePath());
    }

    /**
     * Valide le format d'un email
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }


}