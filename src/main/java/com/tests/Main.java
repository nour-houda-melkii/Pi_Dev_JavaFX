package com.tests;

import com.exceptions.AuthException;
import com.models.User;
import com.services.AuthService;
import com.services.ProfileService;
import com.services.UserService;
import com.demo.enums.Role;
import com.demo.enums.Specialite;
import com.utils.EnvLoader;
import com.utils.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    public static void main(String[] args) throws AuthException, SQLException {
        EnvLoader.load();

        AuthService authService = new AuthService();
        UserService userService = new UserService();
        ProfileService profileService = new ProfileService();

//        String email = "medecin@gmail.com";
//        String password = "DmtFU3JwADp";
//
//        String token = null;
//        try {
//            // 1. Login (obtention du token)
//            token = authService.login(email, password);
//            System.out.println("Token reçu: " + token);

//            // 2. Récupération de l'utilisateur à partir du token
//            User currentUser = authService.getUserFromToken(token);
//            System.out.println("Utilisateur connecté: " + currentUser);

//            // 2. Afficher le profil actuel
//            System.out.println("\n===== PROFIL ACTUEL =====");
//            User currentUser = profileService.getUserProfile(token);
//            System.out.println("Prénom: " + currentUser.getFirstName());
//            System.out.println("Nom: " + currentUser.getLastName());
//            System.out.println("Email: " + currentUser.getEmail());
//            System.out.println("Adresse: " + currentUser.getAddress());
//            System.out.println("Téléphone: " + currentUser.getPhoneNumber());
//            System.out.println("Âge: " + currentUser.getAge());
//
//            if (currentUser.isMedecin()) {
//                System.out.println("Numéro Licence: " + currentUser.getNumeroLicence());
//                System.out.println("Spécialité: " + currentUser.getSpecialite());
//            }

//            // 3. Test de mise à jour des informations
//            System.out.println("\n===== TEST DE MISE À JOUR DES INFORMATIONS =====");
//            User userUpdates = new User();
//            userUpdates.setFirstName("Nouveau");
//            userUpdates.setLastName("Nouveau");
//            userUpdates.setAddress("Nouvelle adresse, 1234 Ville");
//            userUpdates.setPhoneNumber("98765432");
//
//            // Si c'est un médecin, on peut aussi mettre à jour la spécialité
//            if (currentUser.isMedecin()) {
//                userUpdates.setSpecialite(Specialite.CARDIOLOGIE);
//            }
//
//            User updatedUser = authService.updateUserInfo(token, userUpdates);
//            System.out.println("Utilisateur après mise à jour: " + updatedUser);


//            // 4. Test de changement de mot de passe
//            System.out.println("\n===== TEST DE CHANGEMENT DE MOT DE PASSE =====");
//            try {
//                authService.changePassword(token, password, "NouveauMotDePasse123");
//                System.out.println("Mot de passe changé avec succès");
//
//                // Test de connexion avec le nouveau mot de passe
//                String newToken = authService.login(email, "NouveauMotDePasse123");
//                System.out.println("Connexion avec nouveau mot de passe réussie, token: " + newToken);
//
//                // Remettre l'ancien mot de passe pour faciliter d'autres tests
//                authService.changePassword(newToken, "NouveauMotDePasse123", password);
//                System.out.println("Ancien mot de passe restauré");
//            } catch (AuthException | SQLException e) {
//                System.out.println("Erreur lors du changement de mot de passe: " + e.getMessage());
//            }


//            // 5. Test d'échec avec mot de passe incorrect
//            System.out.println("\n===== TEST D'ÉCHEC DE CHANGEMENT DE MOT DE PASSE =====");
//            try {
//                authService.changePassword(token, "MotDePasseIncorrect", "AutreMotDePasse");
//                System.out.println("Ce message ne devrait pas apparaître");
//            } catch (AuthException | SQLException e) {
//                System.out.println("Erreur attendue: " + e.getMessage());
//            }

//            // 6. Logout
//            authService.logout(jwtToken);

//        } catch (AuthException e) {
//            System.err.println("Erreur: " + e.getMessage());
//        }


//        try {
//            System.out.println("=== Test du système de réinitialisation de mot de passe ===");
//
//            // 1. Demander l'email de l'utilisateur
//            System.out.print("Entrez l'email de l'utilisateur: ");
//            String email = scanner.nextLine();
//
//            // 2. Générer et sauvegarder le code de vérification
//            System.out.println("\nÉtape 1: Génération du code de vérification...");
//            String verificationCode = authService.generateAndSaveVerificationCode(email);
//            System.out.println("Code généré: " + verificationCode);
//            System.out.println("(En production, ce code serait envoyé par email)");
//
//            // 3. Demander le code à l'utilisateur
//            System.out.print("\nEntrez le code de vérification reçu: ");
//            String userEnteredCode = scanner.nextLine();
//
//            // 4. Vérifier le code
//            System.out.println("\nÉtape 2: Vérification du code...");
//            boolean isValid = authService.verifyCode(email, userEnteredCode);
//            System.out.println("Code valide: " + isValid);
//
//            if (isValid) {
//                // 5. Demander le nouveau mot de passe
//                System.out.print("\nEntrez le nouveau mot de passe: ");
//                String newPassword = scanner.nextLine();
//
//                // 6. Réinitialiser le mot de passe
//                System.out.println("\nÉtape 3: Réinitialisation du mot de passe...");
//                authService.resetPasswordWithCode(email, userEnteredCode, newPassword);
//                System.out.println("Mot de passe réinitialisé avec succès!");
//            }
//        } catch (AuthException | SQLException e) {
//            System.err.println("Erreur: " + e.getMessage());
//        } finally {
//            scanner.close();
//        }


//        // 1. Test création médecin
//        System.out.println("\n=== TEST CREATION MEDECIN ===");
//        User nouveauMedecin = new User();
//        nouveauMedecin.setEmail("zndjze@gmail.com");
//        nouveauMedecin.setFirstName("Jean");
//        nouveauMedecin.setLastName("Dupont");
//        nouveauMedecin.setAddress("123 Rue Médicale, Ville");
//        nouveauMedecin.setPhoneNumber("0612345678");
//        nouveauMedecin.setAge(42);
//        nouveauMedecin.setNumeroLicence("MED12345");
//        nouveauMedecin.setSpecialite(Specialite.CARDIOLOGIE);
//
//        try {
//            userService.ajouterMedecin(nouveauMedecin);
//            System.out.println("✅ Médecin créé avec succès. ID: " + nouveauMedecin.getId());
//        } catch (Exception e) {
//            System.err.println("❌ Erreur création médecin: " + e.getMessage());
//        }
//
//
//
//        // 2. Test création patient
//        System.out.println("\n=== TEST CREATION PATIENT ===");
//        User nouveauPatient = new User();
//        nouveauPatient.setEmail("zdhl@gmail.com");
//        nouveauPatient.setFirstName("Marie");
//        nouveauPatient.setLastName("Durand");
//        nouveauPatient.setAddress("456 Avenue Patient, Ville");
//        nouveauPatient.setPhoneNumber("0698765432");
//        nouveauPatient.setAge(35);
//
//        try {
//            userService.ajouterPatient(nouveauPatient);
//            System.out.println("✅ Patient créé avec succès. ID: " + nouveauPatient.getId());
//        } catch (Exception e) {
//            System.err.println("❌ Erreur création patient: " + e.getMessage());
//        }
//
//
//        // 3. Vérification dans la base
//        System.out.println("\n=== VERIFICATION ===");
//        System.out.println("Médecins enregistrés: " + userService.rechercherTousMedecins().size());
//        System.out.println("Patients enregistrés: " + userService.rechercherTousPatients().size());


//        User patient = new User();
//        patient.setEmail("dde@example.com");
//        patient.setPassword("patient123");
//        patient.setFirstName("Jean");
//        patient.setLastName("Dupont");
//        patient.setRole(Role.PATIENT);
//        patient.setAddress("456 Main St");
//        patient.setPhoneNumber("0698765432");
//        patient.setAge(30);
//
//        try {
//            User registeredPatient = authService.registerPatient(patient);
//            System.out.println("Patient enregistré avec ID: " + registeredPatient.getId());
//        } catch (SQLException e) {
//            System.err.println("Erreur: " + e.getMessage());
//        }
//
//
//        User medecin = new User();
//        medecin.setEmail("eddf@example.com");
//        medecin.setPassword("secure123");
//        medecin.setFirstName("John");
//        medecin.setLastName("Smith");
//        medecin.setRole(Role.MEDECIN);
//        medecin.setAddress("123 Medical St");
//        medecin.setPhoneNumber("0612345678");
//        medecin.setAge(45);
//        medecin.setNumeroLicence("MED54321");
//        medecin.setSpecialite(Specialite.CARDIOLOGIE);
//
//        try {
//            User registeredMedecin = authService.registerMedecin(medecin);
//            System.out.println("Médecin enregistré avec ID: " + registeredMedecin.getId());
//        } catch (SQLException e) {
//            System.err.println("Erreur: " + e.getMessage());
//        }


//        System.out.println("\n===== Suppression du compte actuel =====");
//        profileService.deleteUserAccount(token, password);


    }
}