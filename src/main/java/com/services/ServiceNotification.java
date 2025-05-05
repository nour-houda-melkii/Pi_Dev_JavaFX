package com.services;

import com.models.*;
import com.utils.DataSource;


import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ServiceNotification implements CrudService<Notification> {

    private Connection connection;
    private ServiceMedecin serviceMedecin;
    private ServicePatient servicePatient;

    public ServiceNotification() {
        connection = DataSource.getInstance().getConnection();
        serviceMedecin = new ServiceMedecin(connection);
        servicePatient = new ServicePatient();
    }

    @Override
    public void ajouter(Notification notification) {
        String req = "INSERT INTO `notification`(`patient_id`, `medecin_id`, `message`, `is_read`) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            // patient_id
            if (notification.getPatientId() != null) {
                ps.setInt(1, notification.getPatientId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            // medecin_id
            if (notification.getMedecinId() != null) {
                ps.setInt(2, notification.getMedecinId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            // message et is_read
            ps.setString(3, notification.getMessage());
            ps.setBoolean(4, notification.isRead());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    notification.setId(rs.getInt(1));
                }
            }
            System.out.println("Notification ajoutée avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de la notification : " + e.getMessage());
        }
    }


    @Override
    public void modifer(Notification notification) {
        String req = "UPDATE `notification` SET `patient_id`=?, `medecin_id`=?, `message`=?, `is_read`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            if (notification.getPatientId() != null) ps.setInt(1, notification.getPatientId());
            else ps.setNull(1, Types.INTEGER);

            if (notification.getMedecinId() != null) ps.setInt(2, notification.getMedecinId());
            else ps.setNull(2, Types.INTEGER);

            ps.setString(3, notification.getMessage());
            ps.setBoolean(4, notification.isRead());
            ps.setInt(5, notification.getId());

            ps.executeUpdate();
            System.out.println("Modification de la notification avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification de la notification : " + e.getMessage());
        }
    }


    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM `notification` WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Suppression de la notification avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression de la notification : " + e.getMessage());
        }
    }

    @Override
    public List<Notification> afficher() {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM `notification`";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(req);
            while (rs.next()) {
                Notification notification = new Notification();
                notification.setId(rs.getInt("id"));
                notification.setPatientId(rs.getInt("patient_id"));
                notification.setMedecinId(rs.getInt("medecin_id"));
                notification.setMessage(rs.getString("message"));
                notification.setRead(rs.getBoolean("is_read"));
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'affichage des notifications : " + e.getMessage());
        }
        return notifications;
    }

    // Récupérer les notifications pour un patient spécifique
    public List<Notification> getNotificationsForPatient(int patientId) {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM `notification` WHERE `patient_id`=? ORDER BY `id` DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification notification = new Notification();
                notification.setId(rs.getInt("id"));
                notification.setPatientId(rs.getInt("patient_id"));
                notification.setMedecinId(rs.getInt("medecin_id"));
                notification.setMessage(rs.getString("message"));
                notification.setRead(rs.getBoolean("is_read"));
                notifications.add(notification);

            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des notifications du patient : " + e.getMessage());
        }
        return notifications;
    }

    // Récupérer les notifications pour un médecin spécifique
    public List<Notification> getNotificationsForMedecin(int medecinId) {
        List<Notification> notifications = new ArrayList<>();
        String req = "SELECT * FROM `notification` WHERE `medecin_id`=? ORDER BY `id` DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification notification = new Notification();
                notification.setId(rs.getInt("id"));
                notification.setPatientId(rs.getInt("patient_id"));
                notification.setMedecinId(rs.getInt("medecin_id"));
                notification.setMessage(rs.getString("message"));
                notification.setRead(rs.getBoolean("is_read"));
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des notifications du médecin : " + e.getMessage());
        }
        return notifications;
    }

    // Dans votre classe ServiceNotification

    public void markAsRead(int notificationId) {
        System.out.println("Tentative de marquer comme lu la notification ID: " + notificationId);
        // Appeler la méthode markAsRead du gestionnaire de base de données
        // Assurez-vous que cette méthode exécute bien la requête SQL pour mettre à jour is_read à 1
        String req = "UPDATE `notification` SET `is_read`= 1 WHERE `id`=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, notificationId);
            ps.executeUpdate();
            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("Notification marquée comme lue avec succès!");
            } else {
                System.out.println("Aucune notification trouvée avec cet ID.");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors du marquage de la notification comme lue : " + e.getMessage());
        }
    }

    // Créer une notification pour un rendez-vous accepté
    public void notifierRendezVousAccepte(RendezVous rendezVous) {
        try {
            Patient patient = servicePatient.getPatientById(rendezVous.getPatientId());
            Medecin medecin = serviceMedecin.getMedecinById(rendezVous.getMedecinId());

            if (patient != null && medecin != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

                String dateStr = dateFormat.format(rendezVous.getDate());
                String heureStr = timeFormat.format(rendezVous.getHeure());

                String message = "Votre rendez-vous du " + dateStr + " à " + heureStr +
                        " avec Dr. " + medecin.getLastName() + " " + medecin.getFirstName() + " a été accepté.";

                Notification notification = new Notification(patient.getId(), null, message);
                this.ajouter(notification);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de la notification d'acceptation : " + e.getMessage());
        }
    }

    // Créer une notification pour un rendez-vous refusé/annulé
    public void notifierRendezVousAnnule(RendezVous rendezVous) {
        try {
            Patient patient = servicePatient.getPatientById(rendezVous.getPatientId());
            Medecin medecin = serviceMedecin.getMedecinById(rendezVous.getMedecinId());

            if (patient != null ) {

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

                String dateStr = dateFormat.format(rendezVous.getDate());
                String heureStr = timeFormat.format(rendezVous.getHeure());

                String message = "Votre rendez-vous du " + dateStr + " à " + heureStr +
                        " avec Dr. " + medecin.getLastName() + " " + medecin.getFirstName() +
                        " a été annulé. Cause: " + rendezVous.getCause();

                Notification notification = new Notification(patient.getId(),null , message);
                this.ajouter(notification);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de la notification d'annulation : " + e.getMessage());
        }
    }

    // Créer une notification pour un nouveau rendez-vous demandé
    public void notifierNouveauRendezVous(RendezVous rendezVous) {
        try {
            Patient patient = servicePatient.getPatientById(rendezVous.getPatientId());
            Medecin medecin = serviceMedecin.getMedecinById(rendezVous.getMedecinId());

            if (patient != null && medecin != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

                String dateStr = dateFormat.format(rendezVous.getDate());
                String heureStr = timeFormat.format(rendezVous.getHeure());

                String message = "Un nouveau rendez-vous a été demandé par  :" +
                        patient.getFirstName() + "  " + patient.getLastName() +
                        " pour le  : " + dateStr + " à " + heureStr + ".";

                Notification notification = new Notification(null, medecin.getId(), message);
                this.ajouter(notification);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de la notification de nouvelle demande : " + e.getMessage());
        }
    }

    // Compter les notifications non lues pour un patient
    public int countUnreadNotificationsForPatient(int patientId) {
        int count = 0;
        String req = "SELECT COUNT(*) as total FROM `notification` WHERE `patient_id`=? AND `is_read`=false";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors du comptage des notifications non lues : " + e.getMessage());
        }
        return count;
    }

    // Compter les notifications non lues pour un médecin
    public int countUnreadNotificationsForMedecin(int medecinId) {
        int count = 0;
        String req = "SELECT COUNT(*) as total FROM `notification` WHERE `medecin_id`=? AND `is_read`=false";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors du comptage des notifications non lues : " + e.getMessage());
        }
        return count;
    }

    public void notifierRendezVousAccepteEnLigne(RendezVous rdv) {
        // Récupérer les informations du médecin
        Medecin medecin = serviceMedecin.getMedecinById(rdv.getMedecinId());
        String nomMedecin = (medecin != null) ?
                "Dr. " + medecin.getLastName() + " " + medecin.getFirstName() : "Inconnu";

        // Formatage de la date et heure pour l'affichage
        String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate());
        String heureStr = new SimpleDateFormat("HH:mm").format(rdv.getHeure());

        // Créer le message avec le lien Jitsi
        String message = "Votre rendez-vous en ligne du " + dateStr + " à " + heureStr +
                " avec " + nomMedecin + " a été accepté.\n\n" +
                "Lien pour la consultation en ligne : " + rdv.getLienJitsi() + "\n\n" +
                "Connectez-vous 5 minutes avant l'heure prévue.";

        // Créer et envoyer la notification
        Notification notification = new Notification(
                rdv.getPatientId(),
                null,
                message);
        ajouter(notification);
    }
}