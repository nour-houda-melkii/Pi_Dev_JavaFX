package com.controllers;

import com.exceptions.AuthException;
import com.utils.AuthManager;
import com.utils.DataSource;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.models.*;
import com.services.*;
import javafx.scene.layout.HBox;
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


    ///
    ///
    ///
    ///
    ///
    ///
    ///
    ///
    ///
    /// Gestion de la connexion
//    @FXML
//    private void handleLogin(ActionEvent event) {
//        String email = emailField.getText().trim();
//        String password = passwordField.getText().trim();
//
//        if (email.isEmpty() || password.isEmpty()) {
//            loginMessage.setText("Veuillez renseigner l'email et le mot de passe.");
//            return;
//        }
//
//        loggedInPatient = serviceUser.authentifier(email, password);
//        if (loggedInPatient != null) {
//            loginMessage.setText("Connexion réussie en tant que " + loggedInPatient.getFirstName());
//            patient = servicePatient.afficher().stream()
//                    .filter(p -> p.getUserId() == loggedInPatient.getId())
//                    .findFirst()
//                    .orElse(null);
//            if (patient != null) {
//                loginPane.setVisible(false);
//                doctorPane.setVisible(true);
//                navbar.setVisible(true);
//                populateDoctorGrid();
//            } else {
//                loginMessage.setText("Le patient n'existe pas dans la base de données.");
//            }
//            checkNewNotifications();
//        } else {
//            loginMessage.setText("Échec de la connexion du patient.");
//        }
//    }

    // Méthode pour afficher la liste des médecins
    @FXML
    private void populateDoctorGrid() {
        allDoctors = serviceMedecin.afficher();
        displayDoctorsInGrid(allDoctors);
        doctorPane.setVisible(true);
        appointmentPane.setVisible(false);
        appointmentsListPane.setVisible(false);
    }

    // Méthode pour afficher des médecins spécifiques dans la grille
    private void displayDoctorsInGrid(List<Medecin> medecins) {
        doctorGrid.getChildren().clear();

        // En-tête du tableau
        Label headerNom = new Label("Nom");
        Label headerPrenom = new Label("Prénom");
        Label headerSpec = new Label("Spécialité");
        Label headerTel = new Label("Téléphone");
        Label headerMail = new Label("Email");
        Label headerAction = new Label("Action");

        // Appliquer un style aux en-têtes
        headerNom.setStyle("-fx-font-weight: bold;");
        headerPrenom.setStyle("-fx-font-weight: bold;");
        headerSpec.setStyle("-fx-font-weight: bold;");
        headerTel.setStyle("-fx-font-weight: bold;");
        headerMail.setStyle("-fx-font-weight: bold;");
        headerAction.setStyle("-fx-font-weight: bold;");

        doctorGrid.add(headerNom, 0, 0);
        doctorGrid.add(headerPrenom, 1, 0);
        doctorGrid.add(headerSpec, 2, 0);
        doctorGrid.add(headerTel, 3, 0);
        doctorGrid.add(headerMail, 4, 0);
        doctorGrid.add(headerAction, 5, 0);

        // Remplissage du tableau
        for (int i = 0; i < medecins.size(); i++) {
            Medecin med = medecins.get(i);
            Label nomLabel = new Label(med.getLastName());
            Label prenomLabel = new Label(med.getFirstName());
            Label specialiteLbl = new Label(med.getSpecialite().toString());
            Label telLabel = new Label(med.getPhoneNumber());
            Label mailLabel = new Label(med.getEmail());

            Button selectButton = new Button("Sélectionner");
            selectButton.setOnAction(e -> {
                handleMedecinSelect(med); // Apl la meth pour gérer la sélection
            });

            int rowIndex = i + 1;
            doctorGrid.add(nomLabel, 0, rowIndex);
            doctorGrid.add(prenomLabel, 1, rowIndex);
            doctorGrid.add(specialiteLbl, 2, rowIndex);
            doctorGrid.add(telLabel, 3, rowIndex);
            doctorGrid.add(mailLabel, 4, rowIndex);
            doctorGrid.add(selectButton, 5, rowIndex);
        }
    }

    private void handleMedecinSelect(Medecin med) {
        selectedMedecin = med;
        // Charger les états de rendez-vous pour ce médecin
        List<EtatRendezVous> etats = serviceMedecin.recupererEtats(selectedMedecin);
        stateChoiceBox.setItems(FXCollections.observableArrayList(etats));
        doctorPane.setVisible(false);
        appointmentPane.setVisible(true); // Affi le formu de rdv

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
//        onlineAppointmentsPane.setVisible(false);

    }

    // Méthode pour afficher les rendez-vous dans la grille
    private void displayAppointmentsInGrid(List<RendezVous> rendezVousList) {
        appointmentsGrid.getChildren().clear();

        // En-tête du tableau
        Label headerMedecin = new Label("Médecin");
        Label headerDate = new Label("Date");
        Label headerHeure = new Label("Heure");
        Label headerStatut = new Label("Statut");

        // Appliquer un style aux en-têtes
        headerMedecin.setStyle("-fx-font-weight: bold;");
        headerDate.setStyle("-fx-font-weight: bold;");
        headerHeure.setStyle("-fx-font-weight: bold;");
        headerStatut.setStyle("-fx-font-weight: bold;");

        appointmentsGrid.add(headerMedecin, 0, 0);
        appointmentsGrid.add(headerDate, 1, 0);
        appointmentsGrid.add(headerHeure, 2, 0);
        appointmentsGrid.add(headerStatut, 3, 0);

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfHeure = new SimpleDateFormat("HH:mm");
        int row = 1;

        for (RendezVous rdv : rendezVousList) {
            // Récupération du médecin associé
            Medecin medecin = serviceMedecin.getMedecinById(rdv.getMedecinId());
            String medecinNom = (medecin != null) ? medecin.getFirstName() + " " + medecin.getLastName() : "Médecin inconnu";

            String statut = rdv.isStatut() ? "Accepté" : "En attente";

            Label medecinLabel = new Label(medecinNom);
            Label dateLabel = new Label(sdfDate.format(rdv.getDate()));
            Label heureLabel = new Label(sdfHeure.format(rdv.getHeure()));
            Label statutLabel = new Label(statut);

            appointmentsGrid.add(medecinLabel, 0, row);
            appointmentsGrid.add(dateLabel, 1, row);
            appointmentsGrid.add(heureLabel, 2, row);
            appointmentsGrid.add(statutLabel, 3, row);
            row++;
        }
    }

    @FXML
    private void showAppointmentsListPane(ActionEvent event) {
        doctorPane.setVisible(false);
        appointmentPane.setVisible(false);
        loadPatientAppointments(); // Recharger les rendez-vous avant d'afficher
        appointmentsListPane.setVisible(true);
        notificationsPane.setVisible(false);
    }





    /**
     * Loads and displays notifications for the currently logged-in patient
     */
    private void loadPatientNotifications() {
        // Clear the grid
        notificationsGrid.getChildren().clear();
        notificationsGrid.getRowConstraints().clear();
        notificationsGrid.getColumnConstraints().clear();

        // Add headers
        notificationsGrid.add(createHeaderLabel("Message "), 0, 0);
        notificationsGrid.add(createHeaderLabel("  Statut     "), 1, 0);
        notificationsGrid.add(createHeaderLabel("Action     "), 2, 0);
        // Get notifications for the current patient
        List<Notification> notifications = serviceNotification.getNotificationsForPatient(patient.getId());

        if (notifications.isEmpty()) {
            notificationsMessage.setText("Vous n'avez aucune notification.");
            return;
        }

        // Display notifications
        int rowIndex = 1;
        for (Notification notification : notifications) {

            // Message
            Label messageLabel = new Label(notification.getMessage());
            messageLabel.setWrapText(true);
            messageLabel.setPrefWidth(400);

            // Status
            String status = notification.isRead() ? "Lu" : "Non lu";
            Label statusLabel = new Label(status);

            // Action button

            Button markAsReadBtn = new Button("Marquer comme lu");
            markAsReadBtn.setDisable(notification.isRead());
            markAsReadBtn.setOnAction(e -> {
                serviceNotification.markAsRead(notification.getId());
                notification.setRead(true);
                statusLabel.setText("Lu");
                markAsReadBtn.setDisable(true);
                notificationsMessage.setText("Notification marquée comme lue.");

                // Recharger les notifications pour s'assurer que les changements sont bien reflétés
                loadPatientNotifications();
            });

            // Add to grid
            notificationsGrid.add(messageLabel, 0, rowIndex);
            notificationsGrid.add(statusLabel, 1, rowIndex);
            notificationsGrid.add(markAsReadBtn, 2, rowIndex);

            rowIndex++;
        }


    }

    // Helper method to create styled header labels
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
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

        // Show notifications pane
        notificationsPane.setVisible(true);

        // Load notifications
        loadPatientNotifications();
    }
}