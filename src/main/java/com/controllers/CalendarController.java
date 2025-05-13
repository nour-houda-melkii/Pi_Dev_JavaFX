package com.controllers;

import com.utils.DataSource;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import com.models.*;
import com.services.*;


import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarController {

    private MedecinController medecinController;
    private ServiceRendezVous serviceRendezVous = new ServiceRendezVous();
    private ServicePatient servicePatient = new ServicePatient();
    private Medecin medecinConnecte;

    // Méthode appelée depuis MedecinController pour passer la référence
    public void setMedecinConnecte(Medecin medecin) {
        this.medecinConnecte = medecin;

        // Initialize services if they're null
        if (this.serviceRendezVous == null) {
            this.serviceRendezVous = new ServiceRendezVous();
        }
        if (this.servicePatient == null) {
            this.servicePatient = new ServicePatient();
        }

        // Now it's safe to call these methods
        chargerRendezVousMedecin();
        afficherCalendrierPourMois(yearMonthActuel);
    }


    @FXML
    private VBox calendrierSection;

    @FXML
    private Label moisAnneeCourant;

    @FXML
    private GridPane calendrierGrid;

    @FXML
    private Button prevMonthButton;

    @FXML
    private Button nextMonthButton;

    private YearMonth yearMonthActuel;
    private List<RendezVousWrapper> listeRendezVous = new ArrayList<>();
    private static final Locale LOCALE_FR = Locale.FRANCE;
    private ServiceMedecin serviceMedecin = new ServiceMedecin(DataSource.getInstance().getConnection());

    // Classe interne pour enrichir RendezVous avec des infos patient
    private class RendezVousWrapper {
        private RendezVous rendezVous;
        private String nomPatient;

        public RendezVousWrapper(RendezVous rendezVous, String nomPatient) {
            this.rendezVous = rendezVous;
            this.nomPatient = nomPatient;
        }

        public RendezVous getRendezVous() { return rendezVous; }
        public String getNomPatient() { return nomPatient; }
        public LocalDate getDate() {
            return rendezVous.getDate() != null ?
                    rendezVous.getDate().toLocalDate() : LocalDate.now();
        }
        public String getHeureFormatee() {
            return rendezVous.getHeure() != null ?
                    rendezVous.getHeure().toString().substring(0, 5) : "00:00"; // Format HH:MM
        }
        public boolean isAccepte() { return rendezVous.isStatut(); }
    }

    // Méthode appelée automatiquement par JavaFX après le chargement du FXML
    @FXML
    public void initialize() {
        this.yearMonthActuel = YearMonth.now();
        this.serviceRendezVous = new ServiceRendezVous();
        this.servicePatient = new ServicePatient();

        if (calendrierSection != null) {
            // Only load appointments if a doctor is already connected
            if (medecinConnecte != null) {
                chargerRendezVousMedecin();
            } else {
                // Just add test data for initial display
                ajouterDonneesTest();
            }
            afficherCalendrierPourMois(yearMonthActuel);
        } else {
            System.err.println("calendrierSection est null à l'initialisation");
        }
    }

    public void afficherCalendrier() {
        calendrierSection.setVisible(true);
        chargerRendezVousMedecin();
        afficherCalendrierPourMois(yearMonthActuel);
    }

    private void afficherCalendrierPourMois(YearMonth yearMonth) {
        // Vérifier si la grille est null
        if (calendrierGrid == null) {
            System.err.println("calendrierGrid est null");
            return;
        }

        // Nettoyer la grille existante
        calendrierGrid.getChildren().clear();

        // Mettre à jour le label du mois et année
        String moisNom = yearMonth.getMonth().getDisplayName(TextStyle.FULL, LOCALE_FR);
        moisAnneeCourant.setText(moisNom + " " + yearMonth.getYear());

        LocalDate premierJourDuMois = yearMonth.atDay(1);
        // Jour de la semaine (1 = Lundi, 7 = Dimanche dans la norme ISO)
        int jourDeLaSemaine = premierJourDuMois.getDayOfWeek().getValue();

        // Regrouper les rendez-vous par date
        Map<LocalDate, List<RendezVousWrapper>> rdvParDate = listeRendezVous.stream()
                .filter(rdv -> rdv.isAccepte() &&
                        rdv.getDate().getYear() == yearMonth.getYear() &&
                        rdv.getDate().getMonthValue() == yearMonth.getMonthValue())
                .collect(Collectors.groupingBy(RendezVousWrapper::getDate));

        // Créer et placer les cellules pour chaque jour du mois
        for (int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
            LocalDate date = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), i);

            // Créer un conteneur VBox pour le jour et ses rendez-vous
            VBox jourContainer = new VBox(5);
            jourContainer.setAlignment(Pos.TOP_CENTER);
            jourContainer.setMinSize(100, 100);
            jourContainer.setPrefSize(100, 100);
            jourContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            // Label pour le numéro du jour
            Label jourLabel = new Label(String.valueOf(i));
            jourLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            jourLabel.setPadding(new Insets(2));
            jourLabel.setMinWidth(90);
            jourLabel.setAlignment(Pos.CENTER);

            // Marquer le jour actuel
            if (date.equals(LocalDate.now())) {
                jourLabel.setStyle("-fx-background-color: lightskyblue; -fx-background-radius: 15;");
            }

            jourContainer.getChildren().add(jourLabel);

            // Ajouter les rendez-vous du jour s'il y en a
            if (rdvParDate.containsKey(date)) {
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefHeight(80);
                scrollPane.setMaxHeight(80);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

                VBox rdvContainer = new VBox(2);
                rdvContainer.setPadding(new Insets(2));

                for (RendezVousWrapper rdv : rdvParDate.get(date)) {
                    HBox rdvBox = new HBox(2);

                    Label heureLabel = new Label(rdv.getHeureFormatee());
                    heureLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
                    heureLabel.setTextFill(Color.BLUE);
                    heureLabel.setPrefWidth(35);

                    Label patientLabel = new Label(rdv.getNomPatient());
                    patientLabel.setFont(Font.font("System", 10));
                    patientLabel.setWrapText(true);

                    rdvBox.getChildren().addAll(heureLabel, patientLabel);
                    rdvContainer.getChildren().add(rdvBox);
                }

                scrollPane.setContent(rdvContainer);
                jourContainer.getChildren().add(scrollPane);
                jourContainer.setStyle("-fx-border-color: lightgreen; -fx-border-width: 2; -fx-background-color: #f8fff8;");
            } else {
                // Stylisation pour les jours sans rendez-vous
                jourContainer.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
            }

            // Définir l'action sur clic
            final LocalDate dateFinale = date;
            jourContainer.setOnMouseClicked(e -> afficherRendezVousPourDate(dateFinale));

            // Calculer la position dans la grille (0-indexé)
            int colonne = (jourDeLaSemaine - 1 + i - 1) % 7;
            int ligne = (jourDeLaSemaine - 1 + i - 1) / 7;
            calendrierGrid.add(jourContainer, colonne, ligne);
        }
    }

    private void afficherRendezVousPourDate(LocalDate date) {
        System.out.println("Rendez-vous pour le " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        List<RendezVousWrapper> rdvDuJour = listeRendezVous.stream()
                .filter(rdv -> rdv.isAccepte() && rdv.getDate().equals(date))
                .collect(Collectors.toList());

        if (rdvDuJour.isEmpty()) {
            System.out.println("Aucun rendez-vous pour cette date");
        } else {
            for (RendezVousWrapper rdv : rdvDuJour) {
                System.out.println("- " + rdv.getHeureFormatee() + " : " + rdv.getNomPatient());
            }
        }

        if (medecinController != null) {
            // Appeler une méthode de medecinController pour afficher les détails
            // des rendez-vous pour la date sélectionnée dans une autre vue
            // medecinController.afficherDetailsRendezVous(date, rdvDuJour);
        }
    }
    public void chargerRendezVousMedecin() {
        listeRendezVous.clear();

        if (medecinConnecte != null) {
            int medid = serviceMedecin.idmed(medecinConnecte);

            List<RendezVous> rendezVousFromDB = serviceRendezVous.afficherPourMedecin(medecinConnecte, medid);

            for (RendezVous rdv : rendezVousFromDB) {
                Patient patient = servicePatient.getPatientById(rdv.getPatientId());
                String nomComplet = "Patient inconnu";

                if (patient != null) {
                    String prenom = patient.getFirstName() != null ? patient.getFirstName() : "";
                    nomComplet = prenom ;
                }

                listeRendezVous.add(new RendezVousWrapper(rdv, nomComplet));
            }
        } else {
            // Si aucun médecin n'est connecté, on peut ajouter des données de test
            ajouterDonneesTest();
            // REMOVE THIS LINE OR ADD NULL CHECK:
            // System.out.println(medecinConnecte.getTypesRendezVous());
        }
    }

    private void ajouterDonneesTest() {
        LocalDate aujourdhui = LocalDate.now();

        // Conversion pour les tests
        Date date1 = Date.valueOf(aujourdhui);
        Date date2 = Date.valueOf(aujourdhui.plusDays(2));
        Date date3 = Date.valueOf(aujourdhui.plusDays(5));

        // Crée des rendez-vous fictifs pour les tests
        RendezVous rdv1 = new RendezVous("lien1", date1, Time.valueOf(LocalTime.of(9, 0)), true, false, "", 1);
        rdv1.setId(1);
        rdv1.setMedecinId(1);
        rdv1.setPatientId(1);

        RendezVous rdv2 = new RendezVous("lien2", date1, Time.valueOf(LocalTime.of(10, 30)), true, false, "", 1);
        rdv2.setId(2);
        rdv2.setMedecinId(1);
        rdv2.setPatientId(2);

        RendezVous rdv3 = new RendezVous("lien3", date2, Time.valueOf(LocalTime.of(14, 0)), true, false, "", 1);
        rdv3.setId(3);
        rdv3.setMedecinId(1);
        rdv3.setPatientId(3);

        RendezVous rdv4 = new RendezVous("lien4", date3, Time.valueOf(LocalTime.of(11, 15)), true, false, "", 1);
        rdv4.setId(4);
        rdv4.setMedecinId(1);
        rdv4.setPatientId(4);

        // Ajoute des wrappers de rendez-vous à la liste
        listeRendezVous.add(new RendezVousWrapper(rdv1, "Martin Dupont"));
        listeRendezVous.add(new RendezVousWrapper(rdv2, "Sophie Lefebvre"));
        listeRendezVous.add(new RendezVousWrapper(rdv3, "Jean Petit"));
        listeRendezVous.add(new RendezVousWrapper(rdv4, "Marie Durand"));
    }

    @FXML
    public void naviguerMoisPrecedent() {
        yearMonthActuel = yearMonthActuel.minusMonths(1);
        afficherCalendrierPourMois(yearMonthActuel);
    }

    @FXML
    public void naviguerMoisSuivant() {
        yearMonthActuel = yearMonthActuel.plusMonths(1);
        afficherCalendrierPourMois(yearMonthActuel);
    }
}