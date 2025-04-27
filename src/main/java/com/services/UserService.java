package com.services;

import com.models.Medecin;
import com.models.Patient;
import com.models.User;
import com.demo.enums.Role;
import com.demo.enums.Specialite;
import com.utils.*;
import com.demo.enums.Gender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserService implements IServiceUser<User> {

    private final Connection connection;
    private ServicePatient servicePatient;
    private ServiceMedecin serviceMedecin;

    public UserService() {
        this.connection = DataSource.getInstance().getConnection();
        servicePatient = new ServicePatient();
        serviceMedecin = new ServiceMedecin(connection);
    }

    // ============ MÉTHODES COMMUNES ============

    @Override
    public void ajouterUser(User user) {
        Connection conn = null;
        try {
            conn = connection; // Utilisez votre connexion existante ou obtenez-en une nouvelle
            conn.setAutoCommit(false); // Désactive le mode auto-commit

            // 1. Insertion dans la table user
            user.setVerified(true);
            user.setStatus("verifie");
            String userReq = "INSERT INTO user (email, password, first_name, last_name, roles, adress, phone_number, age, gender, numero_licence, specialite, is_verified, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement userStmt = conn.prepareStatement(userReq, Statement.RETURN_GENERATED_KEYS)) {
                setUserParameters(userStmt, user);
                userStmt.executeUpdate();

                try (ResultSet rs = userStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        user.setId(userId);

                        // 2. Insertion dans la table appropriée selon le rôle
                        if (user.getRoles().contains("ROLE_MEDECIN") || user.getRoles().contains("MEDECIN")) {
                            Medecin medecin = new Medecin( user.getId(), "1");
                            serviceMedecin.ajouter(medecin); // Ajouter le Médecin
                        }
                        else if (user.getRoles().contains("ROLE_USER") || user.getRoles().contains("PATIENT")) {
                            String patientReq = "INSERT INTO patient (user_id) VALUES (?)";
                            try (PreparedStatement patientStmt = conn.prepareStatement(patientReq)) {
                                patientStmt.setInt(1, userId);
                                patientStmt.executeUpdate();
                            }
                        }
                    }
                }
                conn.commit(); // Valide la transaction
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Annule la transaction en cas d'erreur
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            handleSQLException("Erreur lors de l'ajout", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Réactive le mode auto-commit
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void modifierUser(User user) {
        String req = "UPDATE user SET email=?, password=?, first_name=?, last_name=?, roles=?, "
                + "adress=?, phone_number=?, age=?, gender=?, numero_licence=?, specialite=? WHERE id=?";

        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getPassword());
            pst.setString(3, user.getFirstName());
            pst.setString(4, user.getLastName());
            pst.setString(5, user.getRolesAsJson());
            pst.setString(6, user.getAddress());
            pst.setString(7, user.getPhoneNumber());
            pst.setInt(8, user.getAge());
            pst.setString(9, user.getGender() != null ? user.getGender().name() : null);

            if (user.getRoles().contains(User.ROLE_MEDECIN)) {
                pst.setString(10, user.getNumeroLicence());
                pst.setString(11, user.getSpecialite() != null ? user.getSpecialite().name() : null);
            } else {
                pst.setNull(10, Types.VARCHAR);
                pst.setNull(11, Types.VARCHAR);
            }

            pst.setInt(12, user.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("Erreur lors de la modification", e);
        }
    }

    @Override
    public void supprimerUser(User user) {
        supprimerUserById(user.getId());
    }

    private void supprimerUserById(int id) {
        String req = "DELETE FROM user WHERE id=?";
        executeDelete(req, id, "Utilisateur");
    }

    @Override
    public List<User> rechercherTousUsers() {
        return executeUserQuery("SELECT * FROM user");
    }

    @Override
    public User rechercherUserParId(int id) {
        return executeSingleUserQuery("SELECT * FROM user WHERE id=?", id);
    }

    // ============ MÉTHODES MÉDECINS ============
    @Override
    public void ajouterMedecin(User medecin) {
        try {
            String plainPassword = PasswordGenerator.generateSecurePassword();
            medecin.setPassword(PasswordHasher.hashPassword(plainPassword));
            validateMedecin(medecin);

            // Remplacer setRole() par setRoles() avec la liste des rôles
            medecin.setRoles(List.of(User.ROLE_MEDECIN));

            ajouterUser(medecin);
            WelcomeEmailService.sendWelcomeEmail(medecin, plainPassword);
            System.out.println("✅ Médecin " + medecin.getFirstName() + " " + medecin.getLastName()
                    + " ajouté avec succès. ID: " + medecin.getId());
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ajout du médecin: " + e.getMessage());
            throw e; // Re-lancer l'exception pour la gestion d'erreur
        }
    }

    @Override
    public void modifierMedecin(User medecin) {
        try {
            validateMedecin(medecin);
            System.out.println(medecin.getRoles());
            if (!medecin.isMedecin()) {
                throw new IllegalArgumentException("L'utilisateur doit avoir le rôle ROLE_MEDECIN");
            }
            modifierUser(medecin);
            System.out.println("✅ Médecin ID " + medecin.getId() + " modifié avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur modification médecin: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void supprimerMedecin(int id) {
        try {
            executeDelete("DELETE FROM user WHERE id=? AND roles LIKE '%\"ROLE_MEDECIN\"%'", id, "Médecin");
            System.out.println("✅ Médecin ID " + id + " supprimé avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression médecin: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<User> rechercherTousMedecins() {
        try {
            List<User> medecins = executeUserQuery("SELECT * FROM user WHERE roles LIKE '%\"ROLE_MEDECIN\"%'");

            System.out.println("🔍 " + medecins.size() + " medecin(s) trouvé(s)");

            return medecins;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la recherche des medecins: " + e.getMessage());
            throw new RuntimeException("Erreur d'accès aux données des medecins", e);
        }
    }

    @Override
    public User rechercherMedecinParId(int id) {
        try {
            User user = executeSingleUserQuery("SELECT * FROM user WHERE id=? AND roles LIKE '%\"ROLE_MEDECIN\"%'", id);
            if (user == null) {
                throw new IllegalArgumentException("Aucun médecin trouvé avec l'ID: " + id);
            }
            System.out.println("🔍 Médecin trouvé: " + user.getFirstName() + " " + user.getLastName());
            return user;
        } catch (Exception e) {
            System.err.println("❌ Erreur recherche médecin: " + e.getMessage());
            throw e;
        }
    }

    // ============ MÉTHODES PATIENTS ============
    @Override
    public void ajouterPatient(User patient) {
        try {
            // 1. Génération du mot de passe sécurisé
            String plainPassword = PasswordGenerator.generateSecurePassword();

            // 2. Hashage et stockage du mot de passe
            patient.setPassword(PasswordHasher.hashPassword(plainPassword)); // Hash le mdp généré

            // 3. Configuration du patient
            patient.setRoles(List.of(User.ROLE_USER));
            patient.setNumeroLicence(null);
            patient.setSpecialite(null);

            // 4. Enregistrement en base
            ajouterUser(patient);

            // 5. Envoi de l'email avec le mot de passe en clair
            WelcomeEmailService.sendWelcomeEmail(patient, plainPassword);

            System.out.println("✅ Patient " + patient.getFirstName() + " ajouté. ID: " + patient.getId());
        } catch (Exception e) {
            System.err.println("❌ Erreur ajout patient: " + e.getMessage());
            throw e;
        }
    }

    public boolean modifierPatient(User patient) {
        try {
            // Validation du rôle
            if (!patient.getRoles().contains(User.ROLE_USER)) {
                String errorMsg = "Tentative de modification avec un rôle incorrect. Rôle actuel: " + patient.getRoles();
                System.err.println("❌ " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Nettoyage des champs spécifiques aux médecins
            patient.setNumeroLicence(null);
            patient.setSpecialite(null);

            // Modification effective
            modifierUser(patient);

            System.out.println("✅ Patient ID " + patient.getId() + " (" + patient.getFirstName()
                    + " " + patient.getLastName() + ") modifié avec succès");

            return true; // ← Retourner true en cas de succès

        } catch (Exception e) {
            System.err.println("❌ Échec de la modification du patient: " + e.getMessage());
            e.printStackTrace();
            return false; // ← Retourner false seulement en cas d'erreur
        }
    }

    @Override
    public void supprimerPatient(int id) {
        try {
            // D'abord récupérer le patient pour le log
            User patient = rechercherPatientParId(id);

            // Exécution de la suppression
            executeDelete("DELETE FROM user WHERE id=? AND roles LIKE '%\"ROLE_USER\"%'", id, "Patient");

            System.out.println("✅ Patient ID " + id + " (" + patient.getFirstName()
                    + " " + patient.getLastName() + ") supprimé avec succès");

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Suppression impossible - " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue lors de la suppression du patient ID " + id);
            throw new RuntimeException("Erreur système lors de la suppression", e);
        }
    }

    @Override
    public List<User> rechercherTousPatients() {
        try {
            List<User> patients = executeUserQuery("SELECT * FROM user WHERE roles LIKE '%\"ROLE_USER\"%'");

            System.out.println("🔍 " + patients.size() + " patient(s) trouvé(s)");

            return patients;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la recherche des patients: " + e.getMessage());
            throw new RuntimeException("Erreur d'accès aux données des patients", e);
        }
    }

    @Override
    public User rechercherPatientParId(int id) {
        try {
            User user = executeSingleUserQuery("SELECT * FROM user WHERE id=? AND roles LIKE '%\"ROLE_USER\"%'", id);

            if (user == null) {
                String errorMsg = "Aucun patient trouvé avec l'ID: " + id;
                System.err.println("🔍 " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            System.out.println("🔍 Patient trouvé: " + user.getFirstName()
                    + " " + user.getLastName() + " (ID: " + id + ")");

            return user;

        } catch (IllegalArgumentException e) {
            throw e; // On propage les erreurs métier
        } catch (Exception e) {
            System.err.println("❌ Erreur technique lors de la recherche du patient ID " + id);
            throw new RuntimeException("Erreur système lors de la recherche", e);
        }
    }


    // Dans UserService.java

    /**
     * Récupère le contenu du fichier médical d'un patient
     *
     * @param patientId ID du patient dont on veut récupérer le dossier médical
     * @return Pair contenant le nom du fichier et son contenu en bytes
     * @throws IOException              Si erreur de lecture du fichier
     * @throws IllegalArgumentException Si le patient n'existe pas ou n'a pas de fichier médical
     */
    public AbstractMap.SimpleEntry<String, byte[]> getMedicalFileContent(int patientId) throws IOException {
        // 1. Vérifier que le patient existe
        User patient = rechercherPatientParId(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient introuvable avec l'ID: " + patientId);
        }

        // 2. Vérifier si un fichier existe
        if (patient.getMedicalFile() == null || patient.getMedicalFile().isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier médical trouvé pour ce patient");
        }

        // 3. Chemin du fichier
        String medicalFilesDir = "uploads/medical_files/";
        String filePath = medicalFilesDir + patient.getMedicalFile();

        // 4. Vérifier que le fichier existe physiquement
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Fichier médical introuvable sur le serveur: " + filePath);
        }

        // 5. Lire le fichier
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] fileContent = fis.readAllBytes();
            return new AbstractMap.SimpleEntry<>(patient.getMedicalFile(), fileContent);
        }
    }

    // ============ MÉTHODES UTILITAIRES ============
    private void setUserParameters(PreparedStatement pst, User user) throws SQLException {
        pst.setString(1, user.getEmail());
        pst.setString(2, user.getPassword());
        pst.setString(3, user.getFirstName());
        pst.setString(4, user.getLastName());

        // Conversion de la liste des rôles en String (format JSON ou séparé par des virgules)
        pst.setString(5, user.getRolesAsJson());

        pst.setString(6, user.getAddress());
        pst.setString(7, user.getPhoneNumber());
        pst.setInt(8, user.getAge());
        pst.setString(9, user.getGender() != null ? user.getGender().name() : null);

        // Vérification si l'utilisateur est un médecin
        if (user.getRoles().contains(User.ROLE_MEDECIN)) {
            pst.setString(10, user.getNumeroLicence());
            pst.setString(11, user.getSpecialite() != null ? user.getSpecialite().name() : null);
        } else {
            pst.setNull(10, Types.VARCHAR);
            pst.setNull(11, Types.VARCHAR);
        }
        pst.setBoolean(12, user.isVerified());
        pst.setString(13, user.getStatus());
    }

    private void validateMedecin(User medecin) {
        if (medecin.getNumeroLicence() == null || medecin.getSpecialite() == null) {
            throw new IllegalArgumentException("Un médecin doit avoir un numéro de licence et une spécialité");
        }
    }

    private List<User> executeUserQuery(String query, Object... params) {
        List<User> users = new ArrayList<>();
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            setParameters(pst, params);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors de la recherche", e);
        }
        return users;
    }

    private User executeSingleUserQuery(String query, Object... params) {
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            setParameters(pst, params);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors de la recherche", e);
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        String rolesStr = rs.getString("roles");
        user.setRolesFromJson(rolesStr);

        user.setAddress(rs.getString("adress"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setAge(rs.getInt("age"));

        String genderStr = rs.getString("gender");
        user.setGender(genderStr != null ? Gender.valueOf(genderStr) : null);

        user.setMedicalFile(rs.getString("medical_file"));
        // Vérification si c'est un médecin
        if (user.getRoles().contains(User.ROLE_MEDECIN)) {
            user.setNumeroLicence(rs.getString("numero_licence"));
            String specialiteStr = rs.getString("specialite");
            user.setSpecialite(specialiteStr != null && !specialiteStr.isEmpty()
                    ? Specialite.valueOf(specialiteStr)
                    : null);
        }

        return user;
    }


    private void executeDelete(String query, int id, String entityName) {
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            int rows = pst.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException(entityName + " non trouvé avec l'ID: " + id);
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors de la suppression", e);
        }
    }

    private void setParameters(PreparedStatement pst, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            pst.setObject(i + 1, params[i]);
        }
    }

    private void handleSQLException(String message, SQLException e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Erreur de base de données", e);
    }


    // ============ FONCTIONS DE COMPTAGE ============

    /**
     * Compte le nombre total de médecins dans la base de données
     *
     * @return Le nombre de médecins
     */
    public int countTotalMedecins() {
        return countUsersByRole(User.ROLE_MEDECIN);
    }

    /**
     * Compte le nombre total de patients dans la base de données
     *
     * @return Le nombre de patients
     */
    public int countTotalPatients() {
        return countUsersByRole(User.ROLE_USER);
    }

    /**
     * Compte le nombre total d'utilisateurs dans la base de données
     *
     * @return Le nombre total d'utilisateurs (tous rôles confondus)
     */
    public int countTotalUsers() {
        String query = "SELECT COUNT(*) FROM user " +
                "WHERE (roles LIKE ? OR roles LIKE ?) " +
                "AND status = ? " +
                "AND is_verified = 1";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, "%\"" + User.ROLE_USER + "\"%");
            pst.setString(2, "%\"" + User.ROLE_MEDECIN + "\"%");
            pst.setString(3, "verifie");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors du comptage des utilisateurs vérifiés", e);
        }
        return 0;
    }

    // Méthode utilitaire pour compter les utilisateurs par rôle
    private int countUsersByRole(String role) {
        String query = "SELECT COUNT(*) FROM user WHERE roles LIKE ? AND status = ? AND is_verified = 1";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, "%\"" + role + "\"%");
            pst.setString(2, "verifie");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors du comptage des utilisateurs vérifiés avec le rôle " + role, e);
        }
        return 0;
    }


    /**
     * Recherche tous les médecins non vérifiés (avec status = "non_verifie")
     * @return Liste des médecins non vérifiés
     */
    public List<User> rechercherMedecinsNonVerifies() {
        try {
            String query = "SELECT * FROM user WHERE roles LIKE ? AND status = ?";
            List<User> medecins = executeUserQuery(query,
                    "%\"ROLE_MEDECIN\"%",
                    "non_verifie");

            System.out.println("🔍 " + medecins.size() + " médecin(s) non vérifié(s) trouvé(s)");
            return medecins;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la recherche des médecins non vérifiés: " + e.getMessage());
            throw new RuntimeException("Erreur d'accès aux données des médecins non vérifiés", e);
        }
    }


    /**
     * Vérifie un médecin en mettant à jour son statut à "verifie" et is_verified à true
     * @param medecinId ID du médecin à vérifier
     * @return true si la vérification a réussi, false sinon
     */
    public boolean verifierMedecin(int medecinId) {
        String updateReq = "UPDATE user SET status=? WHERE id=? AND roles LIKE ?";
        String selectReq = "SELECT phone_number FROM user WHERE id=?";

        try (PreparedStatement pstUpdate = connection.prepareStatement(updateReq);
             PreparedStatement pstSelect = connection.prepareStatement(selectReq)) {

            // 1. Récupérer et formater le téléphone
            pstSelect.setInt(1, medecinId);
            ResultSet rs = pstSelect.executeQuery();

            String telephone = null;
            if (rs.next()) {
                telephone = rs.getString("phone_number");

                // Formater le numéro avec +216 si nécessaire
                if (telephone != null && !telephone.startsWith("+216") && !telephone.startsWith("216")) {
                    telephone = telephone.trim().replaceAll("[^0-9]", ""); // Nettoyer le numéro
                    if (telephone.startsWith("0")) {
                        telephone = "+216" + telephone.substring(1); // Remplacer 0 par +216
                    } else if (!telephone.isEmpty()) {
                        telephone = "+216" + telephone; // Ajouter +216 devant
                    }
                }
            }

            // 2. Mettre à jour le statut
            pstUpdate.setString(1, "verifie");
            pstUpdate.setInt(2, medecinId);
            pstUpdate.setString(3, "%\"ROLE_MEDECIN\"%");

            int rowsAffected = pstUpdate.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Médecin ID " + medecinId + " vérifié avec succès");

                // 3. Envoyer WhatsApp si le téléphone est valide
                if (telephone != null && !telephone.trim().isEmpty()) {
                    String message = "Votre compte médecin a été vérifié. Vous pouvez vous connecter.";
                    TwilioWhatsAppService.sendWhatsAppMessage(telephone, message);
                }

                return true;
            } else {
                System.out.println("⚠️ Aucun médecin trouvé avec l'ID " + medecinId + " ou déjà vérifié");
                return false;
            }
        } catch (SQLException e) {
            handleSQLException("Erreur lors de la vérification du médecin", e);
            return false;
        }
    }


}