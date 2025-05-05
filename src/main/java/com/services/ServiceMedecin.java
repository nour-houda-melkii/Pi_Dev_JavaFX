package com.services;



import com.demo.enums.Specialite;
import com.models.EtatRendezVous;
import com.models.Medecin;
import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ServiceMedecin implements CrudService<Medecin> {

    private Connection connection;

    public ServiceMedecin(Connection connection) {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(Medecin medecin) {
        String req = "INSERT INTO `medecin`(`user_id`, `types_rendez_vous`) VALUES (?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, medecin.getUserId());
            ps.setString(2, medecin.getTypesRendezVous());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                medecin.setId(rs.getInt(1)); 
            }

            System.out.println("Ajout du médecin avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du médecin : " + e.getMessage());
        }
    }

    @Override
    public void modifer(Medecin medecin) {
        String req = "UPDATE `medecin` SET `types_rendez_vous` = ? WHERE `user_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, medecin.getTypesRendezVous());
            ps.setInt(2, medecin.getUserId());
            ps.executeUpdate();
            System.out.println("Médecin modifié avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification du médecin : " + e.getMessage());
        }
    }





    @Override
    public void supprimer(int userId) {
        String req = "DELETE FROM `medecin` WHERE `user_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.out.println("Médecin supprimé avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression du médecin : " + e.getMessage());
        }
    }

    public Medecin chercherMedecin(String email, String password) {
        Medecin medecin = null;
        String req = "SELECT m.id, m.user_id, m.types_rendez_vous " +
                "FROM medecin m " +
                "INNER JOIN user u ON m.user_id = u.id " +
                "WHERE u.email = ? AND u.password = ?";

        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                medecin = new Medecin(rs.getInt("userId"), rs.getString("typesRendezVous"));
                medecin.setId(rs.getInt("id")); // Définir l'ID généré
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recherche du médecin : " + e.getMessage());
        }
        return medecin;
    }

    public int idmed(Medecin medecin) {
        String req = "SELECT id FROM `medecin` WHERE `user_id` = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, medecin.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recherche du médecin : " + e.getMessage());
        }
        return -1; // Retourne -1 si le médecin n'est pas trouvé ou en cas d'erreur
    }


    @Override
    public List<Medecin> afficher() {
        List<Medecin> medecins = new ArrayList<>();
        String req = "SELECT m.id, m.user_id, m.types_rendez_vous, u.first_name, u.last_name,u.specialite,u.email,u.phone_number " +
                "FROM medecin m " +
                "INNER JOIN user u ON m.user_id = u.id";


        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {

            while (rs.next()) {
                Medecin medecin = new Medecin(rs.getInt("user_id"), rs.getString("types_rendez_vous"));
                medecin.setId(rs.getInt("id"));
                medecin.setFirstName(rs.getString("first_name"));
                medecin.setLastName(rs.getString("last_name"));
                String specialiteStr = rs.getString("specialite");
                medecin.setSpecialite(specialiteStr != null && !specialiteStr.isEmpty()
                        ? Specialite.valueOf(specialiteStr)
                        : null);
                medecin.setEmail(rs.getString("email"));
                medecin.setPhoneNumber(rs.getString("phone_number"));
                medecins.add(medecin);
            }

        } catch (SQLException e) {
            System.out.println("Erreur lors de l'affichage des médecins : " + e.getMessage());
        }
        return medecins;
    }

    public void ajouterEtat(Medecin medecin, List<EtatRendezVous> etats) {
        for (EtatRendezVous etat : etats) {
            String nouveauxTypes = (medecin.getTypesRendezVous() == null || medecin.getTypesRendezVous().isEmpty())
                    ? String.valueOf(etat.getId())
                    : medecin.getTypesRendezVous() + "," + etat.getId();

            String req = "UPDATE medecin SET types_rendez_vous = ? WHERE user_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(req)) {
                ps.setString(1, nouveauxTypes);
                ps.setInt(2, medecin.getUserId());

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    medecin.setTypesRendezVous(nouveauxTypes);
                    System.out.println("États ajoutés avec succès pour le médecin ID: " + medecin.getUserId());
                } else {
                    System.out.println("Aucun état ajouté. Vérifiez l'ID du médecin: " + medecin.getUserId());
                }
            } catch (SQLException e) {
                System.out.println("Erreur lors de l'ajout des états : " + e.getMessage());
            }
        }
    }

    public void supprimerEtat(Medecin medecin, EtatRendezVous etat) {
        if (medecin.getTypesRendezVous() != null && medecin.getTypesRendezVous().contains(String.valueOf(etat.getId()))) {
            String nouveauxTypes = medecin.getTypesRendezVous()
                    .replace(String.valueOf(etat.getId()), "")
                    .replace(",,", ",")
                    .trim();

            String req = "UPDATE medecin SET types_rendez_vous = ? WHERE user_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(req)) {
                ps.setString(1, nouveauxTypes);
                ps.setInt(2, medecin.getUserId());
                ps.executeUpdate();
                medecin.setTypesRendezVous(nouveauxTypes);
            } catch (SQLException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    public List<EtatRendezVous> recupererEtats(Medecin medecin) {
        List<EtatRendezVous> etats = new ArrayList<>();
        String typesRendezVous = medecin.getTypesRendezVous();

        if (typesRendezVous != null && !typesRendezVous.trim().isEmpty()) {
            // Séparer les identifiants et tenter de les parser en entiers
            String[] idsArray = typesRendezVous.split(",");
            List<Integer> idList = new ArrayList<>();
            for (String idStr : idsArray) {
                try {
                    idList.add(Integer.parseInt(idStr.trim()));
                } catch (NumberFormatException e) {
                    System.out.println("Erreur de format pour l'ID : " + idStr);
                }
            }

            if (!idList.isEmpty()) {
                // Création des placeholders pour la requête SQL
                String placeholders = String.join(",", Collections.nCopies(idList.size(), "?"));
                String req = "SELECT * FROM etat_rendez_vous WHERE id IN (" + placeholders + ")";

                try (PreparedStatement ps = connection.prepareStatement(req)) {
                    // Affectation des paramètres
                    for (int i = 0; i < idList.size(); i++) {
                        ps.setInt(i + 1, idList.get(i));
                    }
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        EtatRendezVous etat = new EtatRendezVous(rs.getString("libelle"));
                        etat.setId(rs.getInt("id"));
                        etats.add(etat);
                    }
                } catch (SQLException e) {
                    System.out.println("Erreur lors de la récupération des états : " + e.getMessage());
                }
            }
        }
        return etats;
    }




    public Medecin getMedecinById(int medecinId) {
        String req = "SELECT m.id, m.user_id, m.types_rendez_vous, u.first_name, u.last_name, u.specialite, u.email, u.phone_number " +
                "FROM medecin m " +
                "INNER JOIN user u ON m.user_id = u.id " +
                "WHERE m.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                Medecin medecin = new Medecin(rs.getInt("user_id"), rs.getString("types_rendez_vous"));
                medecin.setId(rs.getInt("id"));
                medecin.setFirstName(rs.getString("first_name"));
                medecin.setLastName(rs.getString("last_name"));
                String specialiteStr = rs.getString("specialite");
                medecin.setSpecialite(specialiteStr != null && !specialiteStr.isEmpty()
                        ? Specialite.valueOf(specialiteStr)
                        : null);
                medecin.setEmail(rs.getString("email"));
                medecin.setPhoneNumber(rs.getString("phone_number"));
                return medecin;
            }
        } catch(SQLException e){
            System.out.println("Erreur lors de getMedecinById : " + e.getMessage());
        }
        return null;
    }

//    public boolean possedeEtat(Medecin medecin, EtatRendezVous etat) {
//        if (medecin.getTypesRendezVous() == null || medecin.getTypesRendezVous().isEmpty()) {
//            return false;
//        }
//        return Arrays.stream(medecin.getTypesRendezVous().split(","))
//                .map(String::trim)
//                .anyMatch(s -> s.equals(String.valueOf(etat.getId())));
//    }




    public boolean possedeEtat(Medecin medecin, EtatRendezVous etat) {
        if (medecin.getTypesRendezVous() == null || medecin.getTypesRendezVous().isEmpty()) {
            return false;
        }
        return Arrays.stream(medecin.getTypesRendezVous().split(","))
                .map(String::trim)
                .anyMatch(s -> s.equals(String.valueOf(etat.getId())));
    }


    public void toggleEtat(Medecin medecin, EtatRendezVous etat) {
        List<String> etats = new ArrayList<>();
        if(medecin.getTypesRendezVous() != null) {
            etats = new ArrayList<>(Arrays.asList(medecin.getTypesRendezVous().split(",")));
        }

        String idEtat = String.valueOf(etat.getId());
        if(etats.contains(idEtat)) {
            etats.remove(idEtat);
        } else {
            etats.add(idEtat);
        }

        String nouveauxTypes = String.join(",", etats);

        String req = "UPDATE medecin SET types_rendez_vous = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, nouveauxTypes);
            ps.setInt(2, medecin.getUserId());
            ps.executeUpdate();
            medecin.setTypesRendezVous(nouveauxTypes);
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}
