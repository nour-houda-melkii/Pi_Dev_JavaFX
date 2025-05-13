package com.controllers;

import com.exceptions.AuthException;
import com.utils.AuthManager;
import com.utils.DataSource;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.models.*;
import com.services.*;

import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;

public class Ajout {

    @FXML
    private HBox navbar;
    @FXML
    private VBox loginPane;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label loginMessage;

    @FXML
    private VBox doctorPane;
    @FXML
    private GridPane doctorGrid;
    @FXML
    private Label doctorMessage;
    @FXML
    private TextField specialitySearchField; // Nouveau champ pour la recherche par spécialité

    @FXML
    private VBox appointmentPane;
    @FXML
    private ChoiceBox<EtatRendezVous> stateChoiceBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> timeComboBox;
    @FXML
    private Label appointmentMessage;

    @FXML
    private VBox appointmentsListPane;
    @FXML
    private GridPane appointmentsGrid;
    @FXML
    private Label appointmentsMessage;  // Make sure this exists in your FXML
   @FXML
    private VBox notificationsPane;


    @FXML
    private GridPane notificationsGrid;

    @FXML
    private Label notificationsMessage;
    @FXML
    private VBox onlineAppointmentsPane;

    @FXML
    private GridPane onlineAppointmentsGrid;

    @FXML
    private Label onlineAppointmentsMessage;

    @FXML
    private Button joinMeetingButton;

    private RendezVous currentSelectedOnlineAppointment;
    private ServiceNotification serviceNotification;

    // Instances de services
    private UserService serviceUser = new UserService();
    private AuthService authService;
    private ServicePatient servicePatient = new ServicePatient();
    private ServiceMedecin serviceMedecin = new ServiceMedecin(DataSource.getInstance().getConnection());
    private ServiceRendezVous serviceRendezVous = new ServiceRendezVous();

    // Stocke les entités connectées et sélectionnées
    private User loggedInPatient;
    private Patient patient;
    private Medecin selectedMedecin;

    // Liste complète des médecins et rendez-vous pour filtrage et tri
    private List<Medecin> allDoctors;
    private List<RendezVous> patientAppointments;
    private String token;
    private User currentUser;




    public boolean isUserConnected() {
        try {
            if (token == null || token.isEmpty()) return false;
            currentUser = authService.getUserFromToken(token);
            return currentUser != null;
        } catch (Exception e) {
            return false;
        }
    }


    public void setToken(String token) {
        this.token = token;
        System.out.println("Nouveau token reçu: " + token);

        // Si AuthService n'est pas encore initialisé
        if (authService == null) {
            authService = new AuthService();
        }

        if (!isUserConnected()) {
            loginMessage.setText("Connexion requise");
            loginPane.setVisible(true);
            doctorPane.setVisible(false);
        } else {
            initialiserUserFromToken();
        }
    }
    @FXML
    private void initialize() {
        chatbotAPI=new MedicalChatbotAPI();
        authService = new AuthService();
        serviceNotification = new ServiceNotification();
        // Configuration du DatePicker pour bloquer les dates passées et le week-end
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()) ||
                        date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                        date.getDayOfWeek() == DayOfWeek.SUNDAY);
            }
        });

        // Mise à jour des créneaux horaires dès qu'une date est sélectionnée et si un médecin est sélectionné
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && selectedMedecin != null) {
                Date sqlDate = Date.valueOf(newDate);
                List<String> horaires = genererCreneauxDisponibles(sqlDate, selectedMedecin.getId());
                timeComboBox.setItems(FXCollections.observableArrayList(horaires));
                timeComboBox.setDisable(horaires.isEmpty());
            }
        });
    }


    @FXML
    private void initialiserUserFromToken() {
        try {
            System.out.println("Initialisation avec token: " + token);

            if (token == null || token.isEmpty()) {
                System.out.println("Token non fourni");
                return;
            }

            currentUser = authService.getUserFromToken(token);
            System.out.println("Utilisateur récupéré: " + (currentUser != null ? currentUser.getEmail() : "null"));

            if (currentUser == null) {
                System.out.println("Session invalide");
                return;
            }

            // Récupération du patient associé à l'utilisateur connecté
            patient = servicePatient.afficher().stream()
                    .filter(p -> p.getUserId() == currentUser.getId())
                    .findFirst()
                    .orElse(null);

            if (patient == null) {
                System.out.println("Aucun patient associé à cet utilisateur");
                // Gérer le cas où l'utilisateur n'est pas associé à un patient
            } else {
                System.out.println("Patient récupéré: ID=" + patient.getId());
            }

            // Ajout de vérifications de nullité pour tous les éléments d'interface
            if (loginMessage != null) {
                loginMessage.setText("Bienvenue " + currentUser.getFirstName());
            }

            if (loginPane != null) {
                loginPane.setVisible(false);
            }

            if (doctorPane != null) {
                doctorPane.setVisible(true);
            }

            if (navbar != null) {
                navbar.setVisible(true);
            }

            populateDoctorGrid();
            checkNewNotifications();

        } catch (Exception e) {
            System.out.println("Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Gestion de la recherche par spécialité
    @FXML
    private void searchDoctorsBySpeciality(ActionEvent event) {
        String searchSpeciality = specialitySearchField.getText().trim().toLowerCase();
        if (searchSpeciality.isEmpty()) {
            doctorMessage.setText("Veuillez entrer une spécialité à rechercher.");
            return;
        }

        // Filtrer les médecins selon la spécialité
        List<Medecin> filteredDoctors = allDoctors.stream()
                .filter(m -> m.getSpecialite().toString().toLowerCase().contains(searchSpeciality))
                .collect(Collectors.toList());

        if (filteredDoctors.isEmpty()) {
            doctorMessage.setText("Aucun médecin trouvé avec cette spécialité.");
        } else {
            doctorMessage.setText(filteredDoctors.size() + " médecin(s) trouvé(s).");
            displayDoctorsInGrid(filteredDoctors);
        }
    }

    // Réinitialiser la recherche pour afficher tous les médecins
    @FXML
    private void resetDoctorSearch(ActionEvent event) {
        specialitySearchField.clear();
        doctorMessage.setText("");
        displayDoctorsInGrid(allDoctors);
    }

    // Tri des rendez-vous par date (croissant)
    @FXML
    private void sortAppointmentsByDateAsc(ActionEvent event) {
        if (patientAppointments == null || patientAppointments.isEmpty()) {
            appointmentsMessage.setText("Aucun rendez-vous à trier.");
            return;
        }

        List<RendezVous> sortedList = new ArrayList<>(patientAppointments);
        sortedList.sort((rdv1, rdv2) -> {
            int dateCompare = rdv1.getDate().compareTo(rdv2.getDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            return rdv1.getHeure().compareTo(rdv2.getHeure());
        });

        displayAppointmentsInGrid(sortedList);
        appointmentsMessage.setText("Rendez-vous triés par date (croissant)");
    }

    // Tri des rendez-vous par date (décroissant)
    @FXML
    private void sortAppointmentsByDateDesc(ActionEvent event) {
        if (patientAppointments == null || patientAppointments.isEmpty()) {
            appointmentsMessage.setText("Aucun rendez-vous à trier.");
            return;
        }

        List<RendezVous> sortedList = new ArrayList<>(patientAppointments);
        sortedList.sort((rdv1, rdv2) -> {
            int dateCompare = rdv2.getDate().compareTo(rdv1.getDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            return rdv2.getHeure().compareTo(rdv1.getHeure());
        });

        displayAppointmentsInGrid(sortedList);
        appointmentsMessage.setText("Rendez-vous triés par date (décroissant)");
    }

    /// Génération des créneaux disponibles
    private List<String> genererCreneauxDisponibles(Date date, int medecinId) {
        List<String> creneaux = new ArrayList<>();
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(20, 30);
        while (!start.isAfter(end)) {
            Time time = Time.valueOf(start);
            if (!serviceRendezVous.medecinAUnRendezVous(medecinId, date, time)) {
                creneaux.add(start.toString());
            }
            start = start.plusMinutes(30);
        }
        return creneaux;
    }


    // Méthode pour afficher la liste des médecins
    @FXML
    public  void populateDoctorGrid() {
        allDoctors = serviceMedecin.afficher();
        displayDoctorsInGrid(allDoctors);
        doctorPane.setVisible(true);
        appointmentPane.setVisible(false);
        appointmentsListPane.setVisible(false);
        onlineAppointmentsPane.setVisible(false);
        chatbotPane.setVisible(false);
    }

    // Méthode pour afficher des médecins spécifiques dans la grille
    ///-----------------------HETHI
    // Méthode pour afficher des médecins spécifiques dans un affichage type liste
    private void displayDoctorsInGrid(List<Medecin> medecins) {
        // Conteneur principal qui contiendra tous les éléments
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(10);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        mainContainer.setPadding(new Insets(15));

        // En-tête de la liste avec style amélioré
        HBox headerRow = new HBox();
        headerRow.setSpacing(10);
        headerRow.setPadding(new Insets(5));
        headerRow.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0;");

        // Style pour les en-têtes
        String headerStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;";

        // Création des labels d'en-tête avec leur largeur respective
        Label headerNom = new Label("Nom");
        headerNom.setPrefWidth(100);
        headerNom.setStyle(headerStyle);

        Label headerPrenom = new Label("Prénom");
        headerPrenom.setPrefWidth(100);
        headerPrenom.setStyle(headerStyle);

        Label headerSpec = new Label("Spécialité");
        headerSpec.setPrefWidth(160);
        headerSpec.setStyle(headerStyle);

        Label headerTel = new Label("Téléphone");
        headerTel.setPrefWidth(120);
        headerTel.setStyle(headerStyle);

        Label headerMail = new Label("Email");
        headerMail.setPrefWidth(200);
        headerMail.setStyle(headerStyle);

        Label headerAction = new Label("Action");
        headerAction.setPrefWidth(120);
        headerAction.setStyle(headerStyle);

        // Ajout des en-têtes à la ligne d'en-tête
        headerRow.getChildren().addAll(headerNom, headerPrenom, headerSpec, headerTel, headerMail, headerAction);

        // Ajout de la ligne d'en-tête au conteneur principal
        mainContainer.getChildren().add(headerRow);

        // Conteneur pour la liste des médecins
        VBox doctorsListContainer = new VBox();
        doctorsListContainer.setSpacing(8);

        // Vérification si la liste est vide
        if (medecins.isEmpty()) {
            Label noDocLabel = new Label("Aucun médecin correspondant trouvé");
            noDocLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666; -fx-padding: 20px;");
            doctorsListContainer.getChildren().add(noDocLabel);
        } else {
            // Boucle pour ajouter chaque médecin à la liste
            for (Medecin med : medecins) {
                // Création d'un HBox pour chaque ligne de médecin
                HBox doctorRow = new HBox();
                doctorRow.setSpacing(10);
                doctorRow.setPadding(new Insets(10, 5, 10, 5));
                doctorRow.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

                // Style commun pour les cellules
                String cellStyle = "-fx-font-size: 13px; -fx-text-fill: #444444;";

                // Création des labels pour chaque information du médecin
                Label nomLabel = new Label(med.getLastName());
                nomLabel.setPrefWidth(100);
                nomLabel.setStyle(cellStyle);

                Label prenomLabel = new Label(med.getFirstName());
                prenomLabel.setPrefWidth(100);
                prenomLabel.setStyle(cellStyle);

                Label specialiteLabel = new Label(med.getSpecialite().toString());
                specialiteLabel.setPrefWidth(160);
                specialiteLabel.setStyle(cellStyle);
                specialiteLabel.setWrapText(true);

                Label telLabel = new Label(med.getPhoneNumber());
                telLabel.setPrefWidth(120);
                telLabel.setStyle(cellStyle);

                Label mailLabel = new Label(med.getEmail());
                mailLabel.setPrefWidth(200);
                mailLabel.setStyle(cellStyle);
                mailLabel.setWrapText(true);

                // Bouton avec style amélioré
                Button selectButton = new Button("Sélectionner");
                selectButton.setPrefWidth(120);
                selectButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 7 15; -fx-cursor: hand; -fx-border-radius: 3;");

                // Effet de survol
                selectButton.setOnMouseEntered(e -> selectButton.setStyle("-fx-background-color: #5CACEE; -fx-text-fill: white; -fx-padding: 7 15; -fx-cursor: hand; -fx-border-radius: 3;"));
                selectButton.setOnMouseExited(e -> selectButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 7 15; -fx-cursor: hand; -fx-border-radius: 3;"));

                // Action du bouton
                selectButton.setOnAction(e -> handleMedecinSelect(med));

                // Ajout des éléments à la ligne
                doctorRow.getChildren().addAll(nomLabel, prenomLabel, specialiteLabel, telLabel, mailLabel, selectButton);

                // Ajout de l'effet de surbrillance lors du survol de la ligne
                doctorRow.setOnMouseEntered(e -> {
                    if (!doctorRow.getStyle().contains("-fx-background-color: #f5f5f5")) {
                        doctorRow.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
                    }
                });
                doctorRow.setOnMouseExited(e -> {
                    doctorRow.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
                });

                // Ajout de la ligne à la liste
                doctorsListContainer.getChildren().add(doctorRow);
            }
        }

        // Création d'un ScrollPane pour gérer les listes longues
        ScrollPane scrollPane = new ScrollPane(doctorsListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350); // Hauteur fixe pour le ScrollPane
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        // Ajout du ScrollPane au conteneur principal
        mainContainer.getChildren().add(scrollPane);

        // Vider le doctorGrid et ajouter le nouveau conteneur
        doctorGrid.getChildren().clear();
        doctorGrid.add(mainContainer, 0, 0);

        // S'assurer que la grille s'adapte bien à l'espace disponible
        doctorGrid.setMinWidth(800);
        VBox.setVgrow(doctorGrid, Priority.ALWAYS);
    }
    private void handleMedecinSelect(Medecin med) {
        selectedMedecin = med;
        // Charger les états de rendez-vous pour ce médecin
        List<EtatRendezVous> etats = serviceMedecin.recupererEtats(selectedMedecin);
        stateChoiceBox.setItems(FXCollections.observableArrayList(etats));
        doctorPane.setVisible(false);
        appointmentPane.setVisible(true); // Affi le formu de rdv
        onlineAppointmentsPane.setVisible(false);
        chatbotPane.setVisible(false);
        notificationsPane.setVisible(false)
        ;


    }

















    @FXML
    private void handleAppointmentCreation(ActionEvent event) {
        // Vérification renforcée de la connexion
        if (token == null || token.isEmpty()) {
            appointmentMessage.setText("Erreur: Token d'authentification manquant");
            return;
        }

        // S'assurer que le service est initialisé
        if (authService == null) {
            authService = new AuthService();
        }

        try {
            // Recharge l'utilisateur à partir du token à chaque fois
            currentUser = authService.getUserFromToken(token);

            if (currentUser == null) {
                appointmentMessage.setText("Erreur: Session expirée. Veuillez vous reconnecter.");
                return;
            }

            // Vérification des champs obligatoires
            if (stateChoiceBox.getValue() == null || datePicker.getValue() == null || timeComboBox.getValue() == null) {
                appointmentMessage.setText("Veuillez remplir tous les champs.");
                return;
            }

            // Création du rendez-vous
            RendezVous rdv = new RendezVous(
                    "",
                    Date.valueOf(datePicker.getValue()),
                    Time.valueOf(timeComboBox.getValue() + ":00"),
                    false,
                    false,
                    null,
                    stateChoiceBox.getValue().getId()
            );

            rdv.setMedecinId(selectedMedecin.getId());
            rdv.setPatientId(patient.getId()); // Utilisation directe de l'ID utilisateur

            // Debug
            System.out.println("Création RDV - User: " + patient.getId()
                    + ", Médecin: " + selectedMedecin.getId()
                    + ", Date: " + datePicker.getValue());

            // Enregistrement
            serviceRendezVous.ajouter(rdv);
            new ServiceNotification().notifierNouveauRendezVous(rdv);

            appointmentMessage.setText("Rendez-vous créé avec succès !");
            resetAppointmentForm();

        } catch (Exception e) {
            appointmentMessage.setText("Erreur technique: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetAppointmentForm() {
        stateChoiceBox.setValue(null);
        datePicker.setValue(null);
        timeComboBox.getItems().clear();
        appointmentPane.setVisible(false);
        doctorPane.setVisible(true);
    }

    // Chargement des rdv du patient
    // Chargement des rdv du patient
    @FXML
    private void loadPatientAppointments() {
        patientAppointments = serviceRendezVous.afficherPourPatient(patient.getId());

        if (patientAppointments.isEmpty()) {
            appointmentsMessage.setText("Vous n'avez pas encore de rendez-vous.");
        } else {
            // Par défaut, trier par date croissante
            List<RendezVous> sortedList = new ArrayList<>(patientAppointments);
            sortedList.sort((rdv1, rdv2) -> {
                int dateCompare = rdv1.getDate().compareTo(rdv2.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return rdv1.getHeure().compareTo(rdv2.getHeure());
            });

            displayAppointmentsInGrid(sortedList);
            appointmentsMessage.setText("Vos rendez-vous (triés par date croissante)");
        }

        // Rendre visible le tableau
        appointmentsListPane.setVisible(true);
        notificationsPane.setVisible(false);
  onlineAppointmentsPane.setVisible(false);
        chatbotPane.setVisible(false);


    }

    // Méthode pour afficher les rendez-vous dans la grille
    private void displayAppointmentsInGrid(List<RendezVous> rendezVousList) {
        // Conteneur principal qui contiendra tous les éléments
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(10);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        mainContainer.setPadding(new Insets(15));

        // En-tête de la liste avec style amélioré
        HBox headerRow = new HBox();
        headerRow.setSpacing(10);
        headerRow.setPadding(new Insets(5));
        headerRow.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0;");

        // Style pour les en-têtes
        String headerStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;";

        // Création des labels d'en-tête avec leur largeur respective
        Label headerMedecin = new Label("Nom");
        headerMedecin.setPrefWidth(180);
        headerMedecin.setStyle(headerStyle);

        Label headerDate = new Label("Date");
        headerDate.setPrefWidth(120);
        headerDate.setStyle(headerStyle);

        Label headerHeure = new Label("Heure");
        headerHeure.setPrefWidth(120);
        headerHeure.setStyle(headerStyle);

        Label headerStatut = new Label("Statut");
        headerStatut.setPrefWidth(120);
        headerStatut.setStyle(headerStyle);

        Label headerActions = new Label("Actions");
        headerActions.setPrefWidth(120);
        headerActions.setStyle(headerStyle);

        // Ajout des en-têtes à la ligne d'en-tête
        headerRow.getChildren().addAll(headerMedecin, headerDate, headerHeure, headerStatut, headerActions);

        // Ajout de la ligne d'en-tête au conteneur principal
        mainContainer.getChildren().add(headerRow);

        // Séparateur en dessous des en-têtes
        Separator headerSeparator = new Separator();
        headerSeparator.setStyle("-fx-background-color: #000000;"); // Ligne noire comme demandé
        mainContainer.getChildren().add(headerSeparator);

        // Conteneur pour la liste des rendez-vous
        VBox appointmentsListContainer = new VBox();
        appointmentsListContainer.setSpacing(0); // Pas d'espacement entre les lignes pour un effet tableau

        // Formatage des dates
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfHeure = new SimpleDateFormat("HH:mm");

        // Vérification si la liste est vide
        if (rendezVousList.isEmpty()) {
            Label noAppointmentLabel = new Label("Vous n'avez aucun rendez-vous programmé.");
            noAppointmentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666; -fx-padding: 20px;");
            appointmentsListContainer.getChildren().add(noAppointmentLabel);
        } else {
            // Boucle pour ajouter chaque rendez-vous à la liste
            for (RendezVous rdv : rendezVousList) {
                // Récupération du médecin associé
                Medecin medecin = serviceMedecin.getMedecinById(rdv.getMedecinId());
                String medecinNom = (medecin != null) ? medecin.getFirstName() + " " + medecin.getLastName() : "Médecin inconnu";
                String statut = rdv.isStatut() ? "Accepté" : "En attente";

                // Création d'un HBox pour chaque ligne de rendez-vous
                HBox appointmentRow = new HBox();
                appointmentRow.setSpacing(10);
                appointmentRow.setPadding(new Insets(10, 5, 10, 5));
                appointmentRow.setStyle("-fx-border-color: #000000; -fx-border-width: 0 0 1 0;"); // Ligne noire en bas

                // Style commun pour les cellules
                String cellStyle = "-fx-font-size: 13px; -fx-text-fill: #444444;";

                // Création des labels pour chaque information du rendez-vous
                Label medecinLabel = new Label(medecinNom);
                medecinLabel.setPrefWidth(180);
                medecinLabel.setStyle(cellStyle);

                Label dateLabel = new Label(sdfDate.format(rdv.getDate()));
                dateLabel.setPrefWidth(120);
                dateLabel.setStyle(cellStyle);

                Label heureLabel = new Label(sdfHeure.format(rdv.getHeure()));
                heureLabel.setPrefWidth(120);
                heureLabel.setStyle(cellStyle);

                // Label de statut avec couleur spécifique
                Label statutLabel = new Label(statut);
                statutLabel.setPrefWidth(120);
                if (rdv.isStatut()) {
                    statutLabel.setStyle(cellStyle + "-fx-text-fill: #87CEEB;"); // Couleur demandée (RGB 135, 206, 235)
                } else {
                    statutLabel.setStyle(cellStyle + "-fx-text-fill: #FFA500;"); // Orange pour les rendez-vous en attente
                }

                // Bouton d'annulation avec style amélioré
                Button cancelButton = new Button("Annuler");
                cancelButton.setPrefWidth(120);
                cancelButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;");

                // Effet de survol pour le bouton
                cancelButton.setOnMouseEntered(e ->
                        cancelButton.setStyle("-fx-background-color: #5CACEE; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;"));
                cancelButton.setOnMouseExited(e ->
                        cancelButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;"));

                // Action du bouton d'annulation
                final RendezVous currentRdv = rdv;
                cancelButton.setOnAction(e -> {
                    // Logique d'annulation à implémenter
                    appointmentsMessage.setText("Annulation du rendez-vous en cours...");
                    // serviceRendezVous.cancelRendezVous(currentRdv.getId());
                    // refreshAppointmentsList();
                });

                // Ajout des éléments à la ligne
                appointmentRow.getChildren().addAll(medecinLabel, dateLabel, heureLabel, statutLabel, cancelButton);

                // Ajout de l'effet de surbrillance lors du survol de la ligne
                appointmentRow.setOnMouseEntered(e -> {
                    if (!appointmentRow.getStyle().contains("-fx-background-color")) {
                        appointmentRow.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #000000; -fx-border-width: 0 0 1 0;");
                    }
                });
                appointmentRow.setOnMouseExited(e -> {
                    appointmentRow.setStyle("-fx-border-color: #000000; -fx-border-width: 0 0 1 0;");
                });

                // Ajout de la ligne à la liste
                appointmentsListContainer.getChildren().add(appointmentRow);
            }
        }

        // Création d'un ScrollPane pour gérer les listes longues
        ScrollPane scrollPane = new ScrollPane(appointmentsListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300); // Hauteur fixe pour le ScrollPane
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        // Ajout du ScrollPane au conteneur principal
        mainContainer.getChildren().add(scrollPane);

        // Vider le appointmentsGrid et ajouter le nouveau conteneur
        appointmentsGrid.getChildren().clear();
        appointmentsGrid.add(mainContainer, 0, 0);

        // S'assurer que la grille s'adapte bien à l'espace disponible
        appointmentsGrid.setPrefWidth(700);
        VBox.setVgrow(appointmentsGrid, Priority.ALWAYS);
    }


    @FXML
    public void showAppointmentsListPane(ActionEvent event) {
        doctorPane.setVisible(false);
        appointmentPane.setVisible(false);
        loadPatientAppointments(); // Recharger les rendez-vous avant d'afficher
        appointmentsListPane.setVisible(true);
        notificationsPane.setVisible(false);
        onlineAppointmentsPane.setVisible(false);
        chatbotPane.setVisible(false);

    }





    /**
     * Loads and displays notifications for the currently logged-in patient
     */
    private void loadPatientNotifications() {
        // Clear the grid
        notificationsGrid.getChildren().clear();
        notificationsGrid.getRowConstraints().clear();
        notificationsGrid.getColumnConstraints().clear();

        // Ensure grid lines are visible with light gray color for a more formal look
        notificationsGrid.setGridLinesVisible(true);
        notificationsGrid.setStyle("-fx-grid-lines-color: #dddddd;");

        // Fixed width for the grid to avoid resizing with window
        notificationsGrid.setPrefWidth(860);
        notificationsGrid.setMaxWidth(860);
        notificationsGrid.setMinWidth(860);

        // Configure column constraints for better layout
        ColumnConstraints messageColumn = new ColumnConstraints();
        messageColumn.setPercentWidth(60);
        messageColumn.setHgrow(Priority.NEVER); // Change to NEVER to prevent auto-expansion

        ColumnConstraints statusColumn = new ColumnConstraints();
        statusColumn.setPercentWidth(20);
        statusColumn.setHalignment(HPos.CENTER);
        statusColumn.setHgrow(Priority.NEVER); // Change to NEVER to prevent auto-expansion

        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setPercentWidth(20);
        actionColumn.setHalignment(HPos.CENTER);
        actionColumn.setHgrow(Priority.NEVER); // Change to NEVER to prevent auto-expansion

        notificationsGrid.getColumnConstraints().addAll(messageColumn, statusColumn, actionColumn);

        // Add headers with clean styling to match the screenshot
        Label messageHeader = new Label("Message");
        messageHeader.setAlignment(Pos.CENTER_LEFT);
        messageHeader.setMaxWidth(Double.MAX_VALUE);
        messageHeader.setPadding(new Insets(12));
        messageHeader.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        Label statusHeader = new Label("Statut");
        statusHeader.setAlignment(Pos.CENTER);
        statusHeader.setMaxWidth(Double.MAX_VALUE);
        statusHeader.setPadding(new Insets(12));
        statusHeader.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        Label actionHeader = new Label("Action");
        actionHeader.setAlignment(Pos.CENTER);
        actionHeader.setMaxWidth(Double.MAX_VALUE);
        actionHeader.setPadding(new Insets(12));
        actionHeader.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        notificationsGrid.add(messageHeader, 0, 0);
        notificationsGrid.add(statusHeader, 1, 0);
        notificationsGrid.add(actionHeader, 2, 0);

        // Get the current patient
        patient = servicePatient.afficher().stream()
                .filter(p -> p.getUserId() == loggedInPatient.getId())
                .findFirst()
                .orElse(null);

        // Get notifications for the current patient
        List<Notification> notifications = serviceNotification.getNotificationsForPatient(patient.getId());

        if (notifications.isEmpty()) {
            notificationsMessage.setText("Vous n'avez aucune notification.");
            return;
        }

        // Display notifications
        int rowIndex = 1;
        for (Notification notification : notifications) {
            // Alternate row colors for better readability, matching the screenshot
            String rowColor = (rowIndex % 2 == 0) ? "#ffffff" : "#f8f9fa";

            // Message
            Label messageLabel = new Label(notification.getMessage());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(Control.USE_COMPUTED_SIZE);
            messageLabel.setPadding(new Insets(12));
            messageLabel.setStyle("-fx-background-color: " + rowColor + "; -fx-text-fill: #333333; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

            // Status
            String status = notification.isRead() ? "Lu" : "Non lu";
            Label statusLabel = new Label(status);
            statusLabel.setAlignment(Pos.CENTER);
            statusLabel.setMaxWidth(Double.MAX_VALUE);
            statusLabel.setPadding(new Insets(12));

            // Color status to match the screenshot
            String statusColor = notification.isRead() ? "#6c757d" : "#17a2b8";
            statusLabel.setStyle("-fx-background-color: " + rowColor + "; -fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

            // Action button styled to match the screenshot
            Button markAsReadBtn = new Button("Marquer comme lu");
            markAsReadBtn.setDisable(notification.isRead());
            markAsReadBtn.setStyle("-fx-background-color: #8CD3F5; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
            markAsReadBtn.setMaxWidth(Double.MAX_VALUE);
            markAsReadBtn.setPadding(new Insets(8));

            markAsReadBtn.setOnAction(e -> {
                serviceNotification.markAsRead(notification.getId());
                notification.setRead(true);
                statusLabel.setText("Lu");
                statusLabel.setStyle("-fx-background-color: " + rowColor + "; -fx-text-fill: #6c757d; -fx-font-weight: bold; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");
                markAsReadBtn.setDisable(true);
                markAsReadBtn.setStyle("-fx-background-color: #adb5bd; -fx-text-fill: white; -fx-background-radius: 4;");
                notificationsMessage.setText("Notification marquÃ©e comme lue.");
            });

            // Create container for the action button to ensure proper centering
            HBox actionContainer = new HBox();
            actionContainer.setAlignment(Pos.CENTER);
            actionContainer.getChildren().add(markAsReadBtn);
            actionContainer.setStyle("-fx-background-color: " + rowColor + "; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");
            actionContainer.setMaxWidth(Double.MAX_VALUE);
            actionContainer.setPadding(new Insets(6));

            // Add to grid
            notificationsGrid.add(messageLabel, 0, rowIndex);
            notificationsGrid.add(statusLabel, 1, rowIndex);
            notificationsGrid.add(actionContainer, 2, rowIndex);

            // Add row constraints for consistent height
            RowConstraints rowConstraint = new RowConstraints();
            rowConstraint.setMinHeight(50);
            rowConstraint.setPrefHeight(50);
            rowConstraint.setMaxHeight(50);
            rowConstraint.setVgrow(Priority.NEVER); // Change to NEVER to prevent auto-expansion
            notificationsGrid.getRowConstraints().add(rowConstraint);

            rowIndex++;
        }

        // Position the notifications pane in the center but with fixed size
        notificationsPane.setMaxWidth(900);
        notificationsPane.setPrefWidth(900);
        notificationsPane.setMinWidth(900);

        // Center the pane horizontally when window is resized
        StackPane.setAlignment(notificationsPane, Pos.TOP_CENTER);
        AnchorPane.setLeftAnchor(notificationsPane, null);
        AnchorPane.setRightAnchor(notificationsPane, null);

        // Calculate center position
        AnchorPane parent = (AnchorPane) notificationsPane.getParent();
        if (parent != null) {
            double leftAnchor = (parent.getWidth() - 900) / 2;
            if (leftAnchor > 20) {
                AnchorPane.setLeftAnchor(notificationsPane, leftAnchor);
            } else {
                AnchorPane.setLeftAnchor(notificationsPane, 20.0);
                AnchorPane.setRightAnchor(notificationsPane, 20.0);
            }
        }
    }
    /**
     * Creates a styled header label for the notifications grid
     */
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: rgb(135, 206, 235); -fx-text-fill: white; -fx-font-weight: bold;");
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(10));
        return label;
    }


    // Add a method to check for new notifications that can be called after login
    public void checkNewNotifications() {
        if (loggedInPatient != null) {
            int unreadCount = serviceNotification.countUnreadNotificationsForPatient(loggedInPatient.getId());
            if (unreadCount > 0) {
                // You can display this information somewhere in your UI
                // For example, add a badge or indicator to the notifications button
                // Or show a popup notification
                showNotificationAlert(unreadCount);
            }
        }
    }

    // Optional: Show a notification alert
    private void showNotificationAlert(int count) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nouvelles notifications");
        alert.setHeaderText(null);
        alert.setContentText("Vous avez " + count + " nouvelle(s) notification(s) non lue(s).");
        alert.showAndWait();
    }

    @FXML
    public void showNotificationsPane() {
        // Hide other panes
        doctorPane.setVisible(false);
        appointmentPane.setVisible(false);
        appointmentsListPane.setVisible(false);
chatbotPane.setVisible(false);
onlineAppointmentsPane.setVisible(false);
        // Show notifications pane
        notificationsPane.setVisible(true);

        // Load notifications
        loadPatientNotifications();
    }



    @FXML
    private void showOnlineAppointmentsPane() {
        // Cacher tous les autres panneaux
        doctorPane.setVisible(false);
        appointmentPane.setVisible(false);
        appointmentsListPane.setVisible(false);
        notificationsPane.setVisible(false);
        onlineAppointmentsPane.setVisible(true);
        chatbotPane.setVisible(false);


        // Charger les rendez-vous en ligne
        loadPatientOnlineAppointments();
    }

    @FXML
    private void loadPatientOnlineAppointments() {
        // Utiliser le service pour récupérer les rendez-vous en ligne du patient
        List<RendezVous> onlineAppointments = serviceRendezVous.afficherRendezVousEnLignePourPatient(patient.getId());

        // Vérifier si la liste est vide
        if (onlineAppointments.isEmpty()) {
            onlineAppointmentsMessage.setText("Vous n'avez pas de rendez-vous en ligne.");
            // Vider la grille au cas où
            onlineAppointmentsGrid.getChildren().clear();
            joinMeetingButton.setDisable(true);
        } else {
            // Trier par date croissante
            List<RendezVous> sortedList = new ArrayList<>(onlineAppointments);
            sortedList.sort((rdv1, rdv2) -> {
                int dateCompare = rdv1.getDate().compareTo(rdv2.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return rdv1.getHeure().compareTo(rdv2.getHeure());
            });

            // Afficher les rendez-vous dans la grille
            displayOnlineAppointmentsInGrid(sortedList);
            onlineAppointmentsMessage.setText("Vos rendez-vous en ligne (triés par date croissante)");
        }
    }

    @FXML
    private void displayOnlineAppointmentsInGrid(List<RendezVous> appointments) {
        // Conteneur principal qui contiendra tous les éléments
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(10);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        mainContainer.setPadding(new Insets(15));

        // En-tête de la liste avec style amélioré
        HBox headerRow = new HBox();
        headerRow.setSpacing(10);
        headerRow.setPadding(new Insets(5));
        headerRow.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 2 0;");

        // Style pour les en-têtes
        String headerStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;";

        // Création des labels d'en-tête avec leur largeur respective
        Label headerDate = new Label("Date");
        headerDate.setPrefWidth(100);
        headerDate.setStyle(headerStyle);

        Label headerHeure = new Label("Heure");
        headerHeure.setPrefWidth(80);
        headerHeure.setStyle(headerStyle);

        Label headerMedecin = new Label("Médecin");
        headerMedecin.setPrefWidth(180);
        headerMedecin.setStyle(headerStyle);

        Label headerEtat = new Label("État");
        headerEtat.setPrefWidth(80);
        headerEtat.setStyle(headerStyle);

        Label headerLien = new Label("Lien");
        headerLien.setPrefWidth(100);
        headerLien.setStyle(headerStyle);

        Label headerActions = new Label("Actions");
        headerActions.setPrefWidth(120);
        headerActions.setStyle(headerStyle);

        // Ajout des en-têtes à la ligne d'en-tête
        headerRow.getChildren().addAll(headerDate, headerHeure, headerMedecin, headerEtat, headerLien, headerActions);

        // Ajout de la ligne d'en-tête au conteneur principal
        mainContainer.getChildren().add(headerRow);

        // Séparateur en dessous des en-têtes
        Separator headerSeparator = new Separator();
        headerSeparator.setStyle("-fx-background-color: #000000;"); // Ligne noire comme demandé
        mainContainer.getChildren().add(headerSeparator);

        // Conteneur pour la liste des rendez-vous
        VBox appointmentsListContainer = new VBox();
        appointmentsListContainer.setSpacing(0); // Pas d'espacement entre les lignes pour un effet tableau

        // Service pour récupérer les informations du médecin
        ServiceMedecin serviceMedecin = new ServiceMedecin(DataSource.getInstance().getConnection());

        // Vérification si la liste est vide
        if (appointments.isEmpty()) {
            Label noAppointmentLabel = new Label("Vous n'avez aucun rendez-vous en ligne programmé.");
            noAppointmentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666; -fx-padding: 20px;");
            appointmentsListContainer.getChildren().add(noAppointmentLabel);
        } else {
            // Boucle pour ajouter chaque rendez-vous à la liste
            for (RendezVous rdv : appointments) {
                // Récupérer les informations du médecin
                Medecin medecin = serviceMedecin.getMedecinById(rdv.getMedecinId());
                String medecinName = medecin != null ?
                        medecin.getFirstName() + " " + medecin.getLastName() :
                        "Médecin #" + rdv.getMedecinId();

                // Format de la date et de l'heure
                String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate());
                String heureStr = new java.text.SimpleDateFormat("HH:mm").format(rdv.getHeure());

                // État du rendez-vous
                String etatStr = "En ligne";

                // Vérifier si le lien Jitsi est disponible
                String lienJitsi = rdv.getLienJitsi();
                boolean hasValidLink = lienJitsi != null && !lienJitsi.isEmpty();

                // Création d'un HBox pour chaque ligne de rendez-vous
                HBox appointmentRow = new HBox();
                appointmentRow.setSpacing(10);
                appointmentRow.setPadding(new Insets(10, 5, 10, 5));
                appointmentRow.setStyle("-fx-border-color: #000000; -fx-border-width: 0 0 1 0;"); // Ligne noire en bas

                // Style commun pour les cellules
                String cellStyle = "-fx-font-size: 13px; -fx-text-fill: #444444;";

                // Création des labels pour chaque information du rendez-vous
                Label dateLabel = new Label(dateStr);
                dateLabel.setPrefWidth(100);
                dateLabel.setStyle(cellStyle);

                Label heureLabel = new Label(heureStr);
                heureLabel.setPrefWidth(80);
                heureLabel.setStyle(cellStyle);

                Label medecinLabel = new Label(medecinName);
                medecinLabel.setPrefWidth(180);
                medecinLabel.setStyle(cellStyle);
                medecinLabel.setWrapText(true);

                Label etatLabel = new Label(etatStr);
                etatLabel.setPrefWidth(80);
                etatLabel.setStyle(cellStyle + "-fx-text-fill: #87CEEB;"); // Couleur demandée

                Label lienLabel = new Label(hasValidLink ? "Disponible" : "Non disponible");
                lienLabel.setPrefWidth(100);
                lienLabel.setStyle(cellStyle);
                if (hasValidLink) {
                    lienLabel.setStyle(cellStyle + "-fx-text-fill: green;");
                } else {
                    lienLabel.setStyle(cellStyle + "-fx-text-fill: orange;");
                }

                // Bouton de sélection avec style amélioré
                Button selectButton = new Button("Sélectionner");
                selectButton.setPrefWidth(120);
                selectButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;");

                // Effet de survol pour le bouton
                selectButton.setOnMouseEntered(e ->
                        selectButton.setStyle("-fx-background-color: #5CACEE; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;"));
                selectButton.setOnMouseExited(e ->
                        selectButton.setStyle("-fx-background-color: #87CEEB; -fx-text-fill: white; -fx-padding: 5 10; -fx-cursor: hand; -fx-border-radius: 3;"));

                // Action du bouton de sélection
                final RendezVous selectedRdv = rdv;
                selectButton.setOnAction(event -> {
                    currentSelectedOnlineAppointment = selectedRdv;
                    onlineAppointmentsMessage.setText("Rendez-vous sélectionné: " +
                            dateStr + " à " + heureStr + " avec Dr. " + medecinName);

                    // Activer le bouton de connexion si le lien existe
                    joinMeetingButton.setDisable(!hasValidLink);

                    if (!hasValidLink) {
                        onlineAppointmentsMessage.setText(onlineAppointmentsMessage.getText() +
                                " - Le lien de consultation n'est pas encore disponible.");
                    }
                });

                // Ajout des éléments à la ligne
                appointmentRow.getChildren().addAll(dateLabel, heureLabel, medecinLabel, etatLabel, lienLabel, selectButton);

                // Ajout de l'effet de surbrillance lors du survol de la ligne
                appointmentRow.setOnMouseEntered(e -> {
                    if (!appointmentRow.getStyle().contains("-fx-background-color")) {
                        appointmentRow.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #000000; -fx-border-width: 0 0 1 0;");
                    }
                });
                appointmentRow.setOnMouseExited(e -> {
                    appointmentRow.setStyle("-fx-border-color: #000000; -fx-border-width: 0 0 1 0;");
                });

                // Ajout de la ligne à la liste
                appointmentsListContainer.getChildren().add(appointmentRow);
            }
        }

        // Création d'un ScrollPane pour gérer les listes longues
        ScrollPane scrollPane = new ScrollPane(appointmentsListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300); // Hauteur fixe pour le ScrollPane
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");

        // Ajout du ScrollPane au conteneur principal
        mainContainer.getChildren().add(scrollPane);

        // Vider le onlineAppointmentsGrid et ajouter le nouveau conteneur
        onlineAppointmentsGrid.getChildren().clear();
        onlineAppointmentsGrid.add(mainContainer, 0, 0);

        // S'assurer que la grille s'adapte bien à l'espace disponible
        onlineAppointmentsGrid.setPrefWidth(700);
        VBox.setVgrow(onlineAppointmentsGrid, Priority.ALWAYS);

        // Désactiver le bouton de connexion par défaut
        joinMeetingButton.setDisable(true);
    }

    @FXML
    private void joinOnlineMeeting() {
        if (currentSelectedOnlineAppointment == null) {
            onlineAppointmentsMessage.setText("Veuillez d'abord sélectionner un rendez-vous.");
            return;
        }

        String lienJitsi = currentSelectedOnlineAppointment.getLienJitsi();
        if (lienJitsi == null || lienJitsi.isEmpty()) {
            onlineAppointmentsMessage.setText("Le lien de consultation n'est pas disponible pour ce rendez-vous.");
            return;
        }

        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(lienJitsi));
            onlineAppointmentsMessage.setText("Connexion à la consultation en ligne...");
        } catch (Exception e) {
            onlineAppointmentsMessage.setText("Erreur lors de l'ouverture du lien: " + e.getMessage());
            e.printStackTrace();
        }
    }



    /////PARTIE CHAT
    // Attributs pour le chatbot
    @FXML
    private VBox chatbotPane;
    @FXML
    private VBox chatHistoryBox;
    @FXML
    private TextField chatInputField;
    @FXML
    private Label chatbotMessage;
    @FXML
    private ProgressIndicator chatLoadingIndicator;

    // API du chatbot médical
//    private MedicalChatbotAPI chatbotAPI;

private  MedicalChatbotAPI chatbotAPI;

    @FXML
    private void showChatbotPane() {
        // Cacher tous les autres panneaux
        doctorPane.setVisible(false);
        appointmentPane.setVisible(false);
        appointmentsListPane.setVisible(false);
        notificationsPane.setVisible(false);
        onlineAppointmentsPane.setVisible(false);

        // Afficher le panneau du chatbot
        chatbotPane.setVisible(true);

        // Message d'accueil si l'historique est vide
        if (chatHistoryBox.getChildren().isEmpty()) {
            addBotMessage("Bonjour ! Je suis votre assistant médical virtuel. Comment puis-je vous aider aujourd'hui ?");
        }
    }

    @FXML
    private void sendChatMessage() {
        String userQuestion = chatInputField.getText().trim();

        if (userQuestion.isEmpty()) {
            return;
        }

        // Ajouter le message de l'utilisateur à l'historique
        addUserMessage(userQuestion);

        // Vider le champ de saisie
        chatInputField.clear();

        // Afficher l'indicateur de chargement
        chatLoadingIndicator.setVisible(true);
        chatbotMessage.setText("Recherche d'une réponse...");

        // Envoyer la question à l'API et obtenir la réponse de façon asynchrone
        chatbotAPI.sendQuestion(userQuestion)
                .thenAccept(response -> {
                    // Cette partie s'exécute sur un thread non-UI
                    Platform.runLater(() -> {
                        // Revenir au thread UI pour mettre à jour l'interface
                        chatLoadingIndicator.setVisible(false);
                        chatbotMessage.setText("");
                        addBotMessage(response);
                    });
                })
                .exceptionally(e -> {
                    // Gestion des erreurs
                    Platform.runLater(() -> {
                        chatLoadingIndicator.setVisible(false);
                        chatbotMessage.setText("Erreur de communication avec le service: " + e.getMessage());
                        addBotMessage("Désolé, je n'ai pas pu traiter votre demande. Veuillez réessayer plus tard.");
                    });
                    return null;
                });
    }

    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 5, 5, 15));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setPadding(new Insets(10));
        messageLabel.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-text-fill: #000000;");

        messageBox.getChildren().add(messageLabel);
        chatHistoryBox.getChildren().add(messageBox);

        // Auto-scroll vers le bas
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 15, 5, 5));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setPadding(new Insets(10));
        messageLabel.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-text-fill: #000000; -fx-border-color: #E0E0E0; -fx-border-radius: 10;");

        messageBox.getChildren().add(messageLabel);
        chatHistoryBox.getChildren().add(messageBox);

        // Auto-scroll vers le bas
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (!chatHistoryBox.getChildren().isEmpty()) {
                Node lastMessage = chatHistoryBox.getChildren().get(chatHistoryBox.getChildren().size() - 1);
                lastMessage.requestFocus();
            }
        });
    }


    @FXML
    public void insertSuggestion(ActionEvent event) {
        Button button = (Button) event.getSource();
        String suggestion = button.getText();
        chatInputField.setText(suggestion);
        chatInputField.requestFocus();
    }
}


//
//```
//@FXML
//public  void populateDoctorGrid() {
//    allDoctors = serviceMedecin.afficher();
//    displayDoctorsInGrid(allDoctors);
//    doctorPane.setVisible(true);
//    appointmentPane.setVisible(false);
//    appointmentsListPane.setVisible(false);
//    onlineAppointmentsPane.setVisible(false);
//    chatbotPane.setVisible(false);
//}
//@FXML
//public void showAppointmentsListPane(ActionEvent event) {
//    doctorPane.setVisible(false);
//    appointmentPane.setVisible(false);
//    loadPatientAppointments(); // Recharger les rendez-vous avant d'afficher
//    appointmentsListPane.setVisible(true);
//    notificationsPane.setVisible(false);
//    onlineAppointmentsPane.setVisible(false);
//    chatbotPane.setVisible(false);
//}
//@FXML
//private void showOnlineAppointmentsPane() {
//    // Cacher tous les autres panneaux
//    doctorPane.setVisible(false);
//    appointmentPane.setVisible(false);
//    appointmentsListPane.setVisible(false);
//    notificationsPane.setVisible(false);
//    onlineAppointmentsPane.setVisible(true);
//    chatbotPane.setVisible(false);
//
//
//    // Charger les rendez-vous en ligne
//    loadPatientOnlineAppointments();
//}
//@FXML
//public void showNotificationsPane() {
//    // Hide other panes
//    doctorPane.setVisible(false);
//    appointmentPane.setVisible(false);
//    appointmentsListPane.setVisible(false);
//    chatbotPane.setVisible(false);
//    onlineAppointmentsPane.setVisible(false);
//    // Show notifications pane
//    notificationsPane.setVisible(true);
//
//    // Load notifications
//    loadPatientNotifications();
//}
//
//@FXML
//private void showChatbotPane() {
//    // Cacher tous les autres panneaux
//    doctorPane.setVisible(false);
//    appointmentPane.setVisible(false);
//    appointmentsListPane.setVisible(false);
//    notificationsPane.setVisible(false);
//    onlineAppointmentsPane.setVisible(false);
//
//    // Afficher le panneau du chatbot
//    chatbotPane.setVisible(true);
//
//    // Message d'accueil si l'historique est vide
//    if (chatHistoryBox.getChildren().isEmpty()) {
//        addBotMessage("Bonjour ! Je suis votre assistant médical virtuel. Comment puis-je vous aider aujourd'hui ?");
//    }
//}
//
//```