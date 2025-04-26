package com.services;

import com.models.EtatRendezVous;
import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEtatRendezVous implements CrudService<EtatRendezVous> {

    private Connection connection;

    public ServiceEtatRendezVous() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void ajouter(EtatRendezVous etatRendezVous) {
        if (etatRendezVous.getLibelle() == null || etatRendezVous.getLibelle().isEmpty()) {
            throw new IllegalArgumentException("Le libellé ne peut pas être vide.");
        }

        String req = "INSERT INTO `etat_rendez_vous`(`libelle`) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, etatRendezVous.getLibelle());
            ps.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    etatRendezVous.setId(rs.getInt(1));
                }
            }

            System.out.println("Ajout de l'état de rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }
    public EtatRendezVous getById(int id) {
        String req = "SELECT * FROM `etat_rendez_vous` WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EtatRendezVous etat = new EtatRendezVous(rs.getString("libelle"));
                    etat.setId(rs.getInt("id"));
                    return etat;
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de l'état par ID : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void modifer(EtatRendezVous etatRendezVous) {
        if (etatRendezVous.getLibelle() == null || etatRendezVous.getLibelle().isEmpty()) {
            throw new IllegalArgumentException("Le libellé ne peut pas être vide.");
        }

        String req = "UPDATE `etat_rendez_vous` SET `libelle`=? WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, etatRendezVous.getLibelle());
            ps.setInt(2, etatRendezVous.getId());
            ps.executeUpdate();
            System.out.println("Modification de l'état de rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM `etat_rendez_vous` WHERE `id`=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Suppression de l'état de rendez-vous avec succès!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    @Override
    public List<EtatRendezVous> afficher() {
        List<EtatRendezVous> etats = new ArrayList<>();
        String req = "SELECT * FROM `etat_rendez_vous`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {

            while (rs.next()) {
                EtatRendezVous etat = new EtatRendezVous(rs.getString("libelle"));
                etat.setId(rs.getInt("id"));
                etats.add(etat);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des états : " + e.getMessage());
        }
        return etats;
    }
}
