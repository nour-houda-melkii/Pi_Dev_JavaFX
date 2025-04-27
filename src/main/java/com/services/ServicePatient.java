package com.services;

import com.models.*;
import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePatient implements CrudService<Patient> {

    private Connection connection;
    private Patient currentPatient;

    public ServicePatient() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Patient patient) {
        String req = "INSERT INTO `patient`(`user_id`) VALUES (?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patient.getUserId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                patient.setId(rs.getInt(1));
            }

            System.out.println("Ajout du patient avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du patient : " + e.getMessage());
        }
    }

    @Override
    public void modifer(Patient patient) {
        String req = "UPDATE `patient` SET `user_id`=? WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, patient.getUserId());
            ps.setInt(2, patient.getId());
            ps.executeUpdate();
            System.out.println("Modification du patient avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification du patient : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM `patient` WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Suppression du patient avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression du patient : " + e.getMessage());
        }
    }

    @Override
    public List<Patient> afficher() {
        List<Patient> patients = new ArrayList<>();
        String req = "SELECT * FROM `patient`";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(req);
            while (rs.next()) {
                Patient patient = new Patient(rs.getInt("user_id"));
                patient.setId(rs.getInt("id"));
                patients.add(patient);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'affichage des patients : " + e.getMessage());
        }
        return patients;
    }


    public Patient getPatientById(int patientId) {
        String req = "SELECT p.*, u.first_name, u.last_name, u.age, u.email " +
                "FROM patient p " +
                "JOIN user u ON p.user_id = u.id " +
                "WHERE p.id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Patient patient = new Patient(rs.getInt("user_id"));
                patient.setId(rs.getInt("id"));
                // Récupérer les informations de la table user
                patient.setFirstName(rs.getString("first_name"));
                patient.setLastName(rs.getString("last_name"));
                // Utiliser getObject pour gérer le cas où l'âge pourrait être null
                Integer age = rs.getObject("age") != null ? rs.getInt("age") : null;
                patient.setAge(age);
                return patient;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération du patient : " + e.getMessage());
        }
        return null;
    }



    ////LES RDV


    public void ajouterRendezVous(RendezVous rendezVous, int medecinId, int patientId) {
        // Définir les IDs après la création de l'objet
        rendezVous.setMedecinId(medecinId);
        rendezVous.setPatientId(patientId);

        String req = "INSERT INTO `rendez_vous`(`medecin_id`, `patient_id`, `lien_jitsi`, `date`, `heure`, `statut`, `annule`, `cause`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, rendezVous.getMedecinId());
            ps.setInt(2, rendezVous.getPatientId());
            ps.setString(3, rendezVous.getLienJitsi());
            ps.setDate(4, rendezVous.getDate());
            ps.setTime(5, rendezVous.getHeure());
            ps.setBoolean(6, rendezVous.isStatut());
            ps.setBoolean(7, rendezVous.isAnnule());
            ps.setString(8, rendezVous.getCause());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                rendezVous.setId(rs.getInt(1)); // Mettre à jour l'ID du rendez-vous
            }

            System.out.println("Ajout du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du rendez-vous : " + e.getMessage());
        }
    }

    public void modifierRendezVous(RendezVous rendezVous) {
        String req = "UPDATE `rendez_vous` SET `medecin_id`=?, `lien_jitsi`=?, `date`=?, `heure`=?, `statut`=?, `annule`=?, `cause`=? WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, rendezVous.getMedecinId());
            ps.setString(2, rendezVous.getLienJitsi());
            ps.setDate(3, rendezVous.getDate());
            ps.setTime(4, rendezVous.getHeure());
            ps.setBoolean(5, rendezVous.isStatut());
            ps.setBoolean(6, rendezVous.isAnnule());
            ps.setString(7, rendezVous.getCause());
            ps.setInt(8, rendezVous.getId());
            ps.executeUpdate();
            System.out.println("Modification du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification du rendez-vous : " + e.getMessage());
        }
    }

    public void annulerRendezVous(int rendezVousId) {
        String req = "UPDATE `rendez_vous` SET `annule`=? WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setBoolean(1, true);
            ps.setInt(2, rendezVousId);
            ps.executeUpdate();
            System.out.println("Annulation du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'annulation du rendez-vous : " + e.getMessage());
        }
    }

    public List<RendezVous> afficherRendezVous(int patientId) {
        List<RendezVous> rendezVousList = new ArrayList<>();
        String req = "SELECT * FROM `rendez_vous` WHERE `patient_id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVous rendezVous = new RendezVous(
                        rs.getString("lien_jitsi"),
                        rs.getDate("date"),
                        rs.getTime("heure"),
                        rs.getBoolean("statut"),
                        rs.getBoolean("annule"),
                        rs.getString("cause"),
                        rs.getInt("etat_id")
                );

                rendezVous.setId(rs.getInt("id"));
                rendezVous.setMedecinId(rs.getInt("medecin_id"));
                rendezVous.setPatientId(rs.getInt("patient_id"));
                rendezVousList.add(rendezVous);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'affichage des rendez-vous : " + e.getMessage());
        }
        return rendezVousList;
    }


    public Integer getPatientIdByUserId(int userId) {
        String req = "SELECT id FROM patient WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de l'ID patient : " + e.getMessage());
        }
        return null; // Retourne null si aucun patient trouvé avec ce user_id
    }

    public Patient getPatientByUserId(int userId) {
        Integer patientId = getPatientIdByUserId(userId);
        if (patientId != null) {
            return getPatientById(patientId);
        }
        return null; // Retourne null si aucun patient trouvé avec ce user_id
    }

}
