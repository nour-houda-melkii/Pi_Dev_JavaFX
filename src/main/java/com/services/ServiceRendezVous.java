package com.services;

import com.models.*;
import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous implements CrudService<RendezVous> {

    private Connection connection;

    public ServiceRendezVous() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(RendezVous rendezVous) {
        String req = "INSERT INTO `rendez_vous`(`medecin_id`, `patient_id`, `etat_id`, `lien_jitsi`, `date`, `heure`, `statut`, `annule`, `cause`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, rendezVous.getMedecinId());
            ps.setInt(2, rendezVous.getPatientId());
            ps.setInt(3, rendezVous.getEtatId()); // Ajout de l'etatId

            ps.setString(4, rendezVous.getLienJitsi());
            ps.setDate(5, rendezVous.getDate());
            ps.setTime(6, rendezVous.getHeure());
            ps.setBoolean(7, rendezVous.isStatut());
            ps.setBoolean(8, rendezVous.isAnnule());
            ps.setString(9, rendezVous.getCause());

            ps.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                rendezVous.setId(rs.getInt(1));
            }

            System.out.println("Ajout du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }


    @Override
    public void modifer(RendezVous rendezVous) {
        String req = "UPDATE `rendez_vous` SET `medecin_id`=?, `patient_id`=?, `lien_jitsi`=?, `date`=?, `heure`=?, `statut`=?, `annule`=?, `cause`=? WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, rendezVous.getMedecinId());
            ps.setInt(2, rendezVous.getPatientId());
            ps.setString(3, rendezVous.getLienJitsi()); // Correction ici : utiliser lienJitsi
            ps.setDate(4, rendezVous.getDate());
            ps.setTime(5, rendezVous.getHeure());
            ps.setBoolean(6, rendezVous.isStatut());
            ps.setBoolean(7, rendezVous.isAnnule());
            ps.setString(8, rendezVous.getCause());
            ps.setInt(9, rendezVous.getId());
            ps.executeUpdate();
            System.out.println("Modification du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM `rendez_vous` WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Suppression du rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    @Override
    public List<RendezVous> afficher() {
        List<RendezVous> rendezVousList = new ArrayList<>();
        String req = "SELECT * FROM `rendez_vous`";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(req);
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
            System.out.println("Erreur lors de l'affichage : " + e.getMessage());
        }
        return rendezVousList;
    }




    public List<RendezVous> afficherPourMedecin(Medecin medecin,int id) {
        List<RendezVous> rendezVousList = new ArrayList<>();



        String req = "SELECT * FROM `rendez_vous` WHERE `medecin_id` = ?";

        try {

            PreparedStatement ps = connection.prepareStatement(req);
          ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            int resultCount = 0;

            while (rs.next()) {
                resultCount++;
                int patid = rs.getInt("patient_id");
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
                rendezVous.setPatientId(patid);

                ServicePatient servicePatient = new ServicePatient();
                Patient patient = servicePatient.getPatientById(patid);

                if (patient == null) {
                    System.out.println("Patient avec l'ID " + patid + " introuvable.");
                } else {
                    String patientNom = (patient.getFirstName() != null) ? patient.getFirstName() : "Inconnu";
                    String patientPrenom = (patient.getLastName() != null) ? patient.getLastName() : "Inconnu";
                    Integer patientAge = patient.getAge();
                    if (patientAge == null) {
                        patientAge = 0;
                    }

                    String dateString = (rendezVous.getDate() != null) ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(rendezVous.getDate()) : "Non spécifiée";
                    String heureString = (rendezVous.getHeure() != null) ? new java.text.SimpleDateFormat("HH:mm").format(rendezVous.getHeure()) : "Non spécifiée";

                    String rendezVousInfo = String.format(
                            " %s %s Age: %d  prévu le %s à %s.",
                            patientPrenom, patientNom, patientAge, dateString, heureString
                    );
                    //System.out.println(rendezVousInfo);
                }

                rendezVousList.add(rendezVous);
            }

            System.out.println("Nombre de résultats trouvés : " + resultCount);
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'affichage pour le médecin : " + e.getMessage());
        }
        return rendezVousList;
    }




    public boolean medecinAUnRendezVous(int medecinId, Date date, Time time) {
        String req = "SELECT COUNT(*) AS count FROM rendez_vous WHERE medecin_id = ? AND date = ? AND heure = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, medecinId);
            ps.setDate(2, date);
            ps.setTime(3, time);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la vérification des rendez-vous : " + e.getMessage());
        }
        return false;
    }

    public List<RendezVous> afficherPourPatient(int patientId) {
        List<RendezVous> rendezVousList = new ArrayList<>();
        String req = "SELECT * FROM `rendez_vous` WHERE `patient_id` = ?";

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
            System.out.println("Erreur lors de l'affichage pour le patient : " + e.getMessage());
        }
        return rendezVousList;
    }







    public void traiterRendezVous(RendezVous rendezVous, boolean accepte) {
        String req = "UPDATE `rendez_vous` SET `statut`=?, `lien_jitsi`=? WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setBoolean(1, accepte);
            ps.setString(2, rendezVous.getLienJitsi());
            ps.setInt(3, rendezVous.getId());
            ps.executeUpdate();
            System.out.println("Traitement du rendez-vous effectué avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors du traitement du rendez-vous : " + e.getMessage());
        }
    }



    public List<RendezVous> afficherRendezVousEnLignePourPatient(int patientId) {
        List<RendezVous> allRendezVous = afficherPourPatient(patientId);
        List<RendezVous> rendezVousEnLigne = new ArrayList<>();

        for (RendezVous rdv : allRendezVous) {
            if (rdv.getEtatId() == 3 && rdv.isStatut() && !rdv.isAnnule()) {
                rendezVousEnLigne.add(rdv);
                System.out.println("Rendez-vous en ligne trouvé: ID=" + rdv.getId() +
                        ", Date=" + rdv.getDate() +
                        ", Heure=" + rdv.getHeure() +
                        ", Lien Jitsi=" + rdv.getLienJitsi());
            }
        }

        return rendezVousEnLigne;
    }
    public List<RendezVous> afficherRendezVousEnLignePourMedecin(int medecinId) {
        List<RendezVous> allRendezVous = afficherPourMedecin(null, medecinId);
        List<RendezVous> rendezVousEnLigne = new ArrayList<>();

        for (RendezVous rdv : allRendezVous) {
            if (rdv.getEtatId() == 3 && rdv.isStatut() && !rdv.isAnnule()) {
                rendezVousEnLigne.add(rdv);
                System.out.println("Rendez-vous en ligne trouvé: ID=" + rdv.getId() +
                        ", Date=" + rdv.getDate() +
                        ", Heure=" + rdv.getHeure() +
                        ", Lien Jitsi=" + rdv.getLienJitsi());
            }
        }

        return rendezVousEnLigne;
    }

}
