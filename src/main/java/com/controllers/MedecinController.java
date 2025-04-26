package com.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import com.models.*;
import com.services.*;
import com.utils.DataSource;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MedecinController implements Initializable {

    // Composants de la partie login (connexion)
    @FXML
    private TextField emailFieldMedecin;
    @FXML
    private PasswordField passwordFieldMedecin;
    @FXML
    private Label loginMessageMedecin;
    @FXML
    private Pane loginPaneMedecin;

    // Pane du dashboard (gestion des rendez-vous)
    @FXML
    private Pane dashboardPaneMedecin;

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox vboxContainer;
    @FXML
    private ListView<EtatRendezVous> listViewEtatsDisponibles;
    @FXML
    private ListView<EtatRendezVous> listViewEtatsMedecin;
    @FXML
    private ListView<EtatRendezVous> listViewEtats;

    @FXML
    private VBox calendrierSection; //

    @FXML
    private DatePicker filterDatePicker;
    @FXML
    private ComboBox<String> filterHeureComboBox;
    @FXML
    private ComboBox<EtatRendezVous> filterEtatComboBox;

    @FXML
    private VBox rendezVousSection;
    @FXML
    private VBox etatsSection;

    private User loggedInMedecin;
    private Medecin medecin;

    //// notif
    @FXML
    private Button notificationsButton;

    @FXML
    private Label notificationCountLabel;

    private Timer notificationTimer;
/// jitsi
@FXML
private VBox onlineAppointmentsContainer;

    @FXML
    private Button refreshOnlineAppointmentsButton;
    @FXML
    private VBox rendezVousEnLigneSection;
    @FXML
    private Button rdvEnLigneButton;
///


    // Services
    private UserService serviceUser = new UserService();
    private ServiceMedecin serviceMedecin = new ServiceMedecin(DataSource.getInstance().getConnection());
    private ServiceRendezVous serviceRendezVous = new ServiceRendezVous();
    private ServicePatient servicePatient = new ServicePatient();
    private ServiceEtatRendezVous serviceEtatRendezVous = new ServiceEtatRendezVous();
    private ServiceNotification serviceNotification;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Afficher d'abord la pane de login et masquer le dashboard.
        loginPaneMedecin.setVisible(true);
        dashboardPaneMedecin.setVisible(false);
        serviceNotification = new ServiceNotification();
        rendezVousEnLigneSection.setVisible(false);
        // Initialiser les heures disponibles par défaut pour le filtre (de 09:00 à 20:30 par tranches de 30 minutes).
        List<String> heures = new ArrayList<>();
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(20, 30);
        while (!start.isAfter(end)) {
            heures.add(start.toString());
            start = start.plusMinutes(30);
        }
        filterHeureComboBox.setItems(FXCollections.observableArrayList(heures));

        // Initialiser les états disponibles pour le filtre.
        List<EtatRendezVous> etats = serviceEtatRendezVous.afficher();
        filterEtatComboBox.setItems(FXCollections.observableArrayList(etats));

        // Configurer l'affichage des items dans la ComboBox des états.
        filterEtatComboBox.setCellFactory(param -> new ListCell<EtatRendezVous>() {
            @Override
            protected void updateItem(EtatRendezVous item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLibelle());
                }
            }
        });

        // Configurer l'affichage de l'élément sélectionné.
        filterEtatComboBox.setButtonCell(new ListCell<EtatRendezVous>() {
            @Override
            protected void updateItem(EtatRendezVous item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLibelle());
                }
            }
        });

        // Listener sur le DatePicker pour mettre à jour les créneaux horaires disponibles selon la date sélectionnée.
        // Vérifie aussi que le médecin est connecté (médecin != null) pour pouvoir récupérer son identifiant.
        filterDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && medecin != null) {
                Date selectedDate = Date.valueOf(newVal);
                int medecinId = serviceMedecin.idmed(medecin);
                List<String> dispoHeures = genererCreneauxDisponibles(selectedDate, medecinId);
                filterHeureComboBox.setItems(FXCollections.observableArrayList(dispoHeures));
            }
        });


        ///notif
        serviceNotification = new ServiceNotification();

        // Initialiser le timer pour les notifications
        notificationTimer = new Timer(true);
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (medecin != null) {
                        updateNotificationCount();
                    }
                });
            }
        }, 0, 60000); // Vérifier toutes les minutes


        refreshOnlineAppointmentsButton.setOnAction(event -> loadRendezVousEnLigne());

    }


//    @FXML
//    private void handleLoginMedecin() {
//        String medecinEmail = emailFieldMedecin.getText().trim();
//        String medecinPassword = passwordFieldMedecin.getText().trim();
//CalendarController calendarController = new CalendarController();
//        if (medecinEmail.isEmpty() || medecinPassword.isEmpty()) {
//            loginMessageMedecin.setText("Veuillez renseigner l'email et le mot de passe.");
//            return;
//        }
//
//        loggedInMedecin = serviceUser.authentifier(medecinEmail, medecinPassword);
//        if (loggedInMedecin != null) {
//            loginMessageMedecin.setText("Connexion réussie en tant que " + loggedInMedecin.getFirstName());
//
//            medecin = new Medecin(loggedInMedecin.getId(), "");
//
//            // Récupérer l'id du med
//            int medid = serviceMedecin.idmed(medecin);
//
//            // Masquer la pane de connexion et afficher le dashboard
//            loginPaneMedecin.setVisible(false);
//            dashboardPaneMedecin.setVisible(true);
//
//            // Charger la liste des rdv pour le médecin connecté
//            loadData(medid);
//            chargerEtatsMedecin(medid);
//            calendarController.setMedecinConnecte(medecin);
//            updateNotificationCount();
//            loadRendezVousEnLigne();
//
//        } else {
//            loginMessageMedecin.setText("Échec de la connexion. Vérifiez vos identifiants.");
//        }
//    }


//// les pat + le crud des rdvs
@FXML
private void loadData(int medecinId) {
    // Récupérer la liste des rendez-vous du médecin
    List<RendezVous> rdvs = serviceRendezVous.afficherPourMedecin(medecin, medecinId);

    // Effacer le contenu précédent
    vboxContainer.getChildren().clear();

    // Pour chaque rendez-vous, créer un panneau d'affichage
    for (RendezVous rdv : rdvs) {
        // Ne pas afficher les rendez-vous refusés (par exemple archivés)
        if ("Refusé".equals(rdv.getCause())) {
            continue;
        }

        // Récupérer les informations du patient associé
        Patient patient = servicePatient.getPatientById(rdv.getPatientId());
        String nom = (patient != null && patient.getLastName() != null) ? patient.getLastName() : "Inconnu";
        String prenom = (patient != null && patient.getFirstName() != null) ? patient.getFirstName() : "Inconnu";
        int age = (patient != null) ? patient.getAge() : 0;

        // Récupérer le libellé de l'état du rendez-vous
        EtatRendezVous etatRendezVous = serviceEtatRendezVous.getById(rdv.getEtatId());
        String libelleEtat = (etatRendezVous != null) ? etatRendezVous.getLibelle() : "Inconnu";

        // Formater la date et l'heure du rendez-vous
        String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate());
        String heureStr = new SimpleDateFormat("HH:mm").format(rdv.getHeure());

        // Création d'un panneau pour afficher le rendez-vous
        BorderPane rdvPane = new BorderPane();
        rdvPane.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-background-color: #f9f9f9;");
        rdvPane.setPadding(new Insets(10));

        Label infoLabel = new Label(String.format("Patient(e) : %s %s  |  Âge : %d\nÉtat : %s\nDate : %s   Heure : %s",
                prenom, nom, age, libelleEtat, dateStr, heureStr));
        rdvPane.setLeft(infoLabel);
        BorderPane.setMargin(infoLabel, new Insets(0, 10, 0, 0));

        // Création du conteneur pour les boutons/actions du rendez-vous
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        if (!rdv.isStatut() && !rdv.isAnnule()) {
            // Dans le gestionnaire d'événements du bouton d'acceptation
            Button btnAccept = new Button("Accepter");
            btnAccept.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAccept.setOnAction(e -> {
                rdv.setStatut(true);

                // Vérifier si l'état du rendez-vous est "En ligne"
                EtatRendezVous etat = serviceEtatRendezVous.getById(rdv.getEtatId());
                if (etatRendezVous != null && "En ligne".equals(etat.getLibelle())) {
                    // Générer un lien Jitsi vraiment unique en utilisant l'ID du rendez-vous
                    String lienJitsi = "https://meet.jit.si/consultation" + rdv.getId();

                    // Stocker le lien dans l'objet rdv
                    rdv.setLienJitsi(lienJitsi);
                }

                // Enregistrer le rendez-vous avec le lien Jitsi
                serviceRendezVous.traiterRendezVous(rdv, true);

                // Envoyer une notification au patient
                if (rdv.getLienJitsi() != null && !rdv.getLienJitsi().isEmpty()) {
                    serviceNotification.notifierRendezVousAccepteEnLigne(rdv);
                } else {
                    serviceNotification.notifierRendezVousAccepte(rdv);
                }

                loadData(medecinId);
            });

            Button btnRefuse = new Button("Refuser");
            btnRefuse.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            btnRefuse.setOnAction(e -> {
                rdv.setStatut(false);
                rdv.setCause("Refusé");
                serviceRendezVous.traiterRendezVous(rdv, false);

                // Envoyer une notification au patient
                serviceNotification.notifierRendezVousAnnule(rdv);

                loadData(medecinId);
            });
            buttonBox.getChildren().addAll(btnAccept, btnRefuse);
        } else if (rdv.isStatut() && !rdv.isAnnule()) {
            Button btnAnnuler = new Button("Annuler");
            btnAnnuler.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            btnAnnuler.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Annulation du rendez-vous");
                dialog.setHeaderText("Veuillez entrer la raison de l'annulation :");
                dialog.setContentText("Motif :");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent() && !result.get().trim().isEmpty()) {
                    rdv.setAnnule(true);
                    rdv.setCause(result.get());
                    serviceRendezVous.modifer(rdv);

                    // Envoyer une notification au patient
                    serviceNotification.notifierRendezVousAnnule(rdv);

                    loadData(medecinId);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Le motif est obligatoire.");
                    alert.showAndWait();
                }
            });
            Button btnModifier = new Button("Modifier");
            btnModifier.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnModifier.setOnAction(e -> {
                Dialog<RendezVous> dialog = new Dialog<>();
                dialog.setTitle("Modifier le rendez-vous");
                dialog.setHeaderText("Modifier la date, l'heure et indiquer la cause de modification");

                DatePicker datePicker = new DatePicker(rdv.getDate().toLocalDate());
                datePicker.setDayCellFactory(new Callback<>() {
                    @Override
                    public DateCell call(DatePicker picker) {
                        return new DateCell() {
                            @Override
                            public void updateItem(LocalDate date, boolean empty) {
                                super.updateItem(date, empty);
                                setDisable(empty || date.isBefore(LocalDate.now()) ||
                                        date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                        date.getDayOfWeek() == DayOfWeek.SUNDAY);
                            }
                        };
                    }
                });

                ComboBox<String> heureComboBox = new ComboBox<>();
                Date initialDate = Date.valueOf(datePicker.getValue());
                heureComboBox.setItems(FXCollections.observableArrayList(genererCreneauxDisponibles(initialDate, medecinId)));
                datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
                    if (newDate != null) {
                        Date sqlDate = Date.valueOf(newDate);
                        heureComboBox.setItems(FXCollections.observableArrayList(genererCreneauxDisponibles(sqlDate, medecinId)));
                    }
                });

                TextField causeField = new TextField();
                causeField.setPromptText("Cause de la modification");

                VBox vbox = new VBox(10);
                vbox.getChildren().addAll(
                        new Label("Nouvelle date :"), datePicker,
                        new Label("Nouvel horaire disponible :"), heureComboBox,
                        new Label("Cause :"), causeField
                );
                dialog.getDialogPane().setContent(vbox);

                ButtonType modifierButton = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(modifierButton, ButtonType.CANCEL);

                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == modifierButton) {
                        if (datePicker.getValue() == null) {
                            new Alert(Alert.AlertType.ERROR, "La date est obligatoire.").showAndWait();
                            return null;
                        }
                        if (heureComboBox.getValue() == null || heureComboBox.getValue().trim().isEmpty()) {
                            new Alert(Alert.AlertType.ERROR, "Veuillez sélectionner un horaire.").showAndWait();
                            return null;
                        }
                        if (causeField.getText().trim().isEmpty()) {
                            new Alert(Alert.AlertType.ERROR, "La cause est obligatoire.").showAndWait();
                            return null;
                        }
                        try {
                            // Sauvegarde des anciennes valeurs pour le message de notification
                            Date oldDate = rdv.getDate();
                            Time oldTime = rdv.getHeure();

                            LocalDate newDate = datePicker.getValue();
                            LocalTime newTime = LocalTime.parse(heureComboBox.getValue());
                            rdv.setDate(Date.valueOf(newDate));
                            rdv.setHeure(Time.valueOf(newTime));
                            rdv.setCause(causeField.getText());
                            serviceRendezVous.modifer(rdv);

                            // Création d'un message personnalisé pour la modification
                            String dateOldStr = new SimpleDateFormat("dd/MM/yyyy").format(oldDate);
                            String heureOldStr = new SimpleDateFormat("HH:mm").format(oldTime);
                            String dateNewStr = new SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate());
                            String heureNewStr = new SimpleDateFormat("HH:mm").format(rdv.getHeure());

                            // Récupérer le médecin
                            Medecin medecin = serviceMedecin.getMedecinById(rdv.getMedecinId());
                            String nomMedecin = (medecin != null) ?
                                    medecin.getLastName() + " " + medecin.getFirstName() : "Inconnu";

                            String message = "Votre rendez-vous initialement prévu le " + dateOldStr +
                                    " à " + heureOldStr + " avec Dr. " + nomMedecin +
                                    " a été modifié au " + dateNewStr + " à " + heureNewStr +
                                    ". Motif : " + rdv.getCause();

                            // Créer la notification
                            Notification notification = new Notification(
                                    rdv.getPatientId(),
                                    null,
                                    message);
                            serviceNotification.ajouter(notification);

                            return rdv;
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "Erreur lors de la modification des valeurs.").showAndWait();
                        }
                    }
                    return null;
                });

                Optional<RendezVous> result = dialog.showAndWait();
                result.ifPresent(r -> loadData(medecinId));
            });
            buttonBox.getChildren().addAll(btnAnnuler, btnModifier);
        } else if (rdv.isAnnule()) {
            Label canceledLabel = new Label("Annulé");
            buttonBox.getChildren().add(canceledLabel);
        }

        rdvPane.setRight(buttonBox);
        vboxContainer.getChildren().add(rdvPane);
    }

    // Afficher un badge de notification si le médecin a des notifications non lues
    int unreadCount = serviceNotification.countUnreadNotificationsForMedecin(medecinId);
    if (unreadCount > 0) {
        // Vous pouvez implémenter cette partie selon votre interface utilisateur
        // Par exemple, mettre à jour un badge dans le menu ou afficher une alerte
        System.out.println("Vous avez " + unreadCount + " notifications non lues");
        // Exemple: updateNotificationBadge(unreadCount);
    }
}

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






    ////////////////////////// les etats



    @FXML
    private void chargerEtatsMedecin(int medecinId) {
        // Récupérer le médecin
        medecin = serviceMedecin.getMedecinById(medecinId);
        if (medecin == null) {
            System.out.println("Médecin non trouvé avec l'ID: " + medecinId);
            return;
        }

        // Récupérer la liste de tous les états
        List<EtatRendezVous> tousEtats = serviceEtatRendezVous.afficher();
        listViewEtats.setItems(FXCollections.observableArrayList(tousEtats));

        // Configurer l'affichage des cellules
        listViewEtats.setCellFactory(lv -> new ListCell<EtatRendezVous>() {
            private final Button btnToggle = new Button();
            private final Label labelEtat = new Label();
            private final HBox hbox = new HBox(10, labelEtat, btnToggle);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                // Donner plus d'espace au label
                HBox.setHgrow(labelEtat, Priority.ALWAYS);

                // Styliser les éléments
                labelEtat.setMaxWidth(Double.MAX_VALUE);
                btnToggle.setPrefWidth(100); // Largeur fixe pour tous les boutons

                // Au clic, basculer l'association de l'état pour le médecin
                btnToggle.setOnAction(e -> {
                    EtatRendezVous etat = getItem();
                    if (etat != null) {
                        serviceMedecin.toggleEtat(medecin, etat);
                        // Rafraîchir l'affichage de la cellule
                        updateItem(etat, false);
                    }
                });
            }



            @Override
            protected void updateItem(EtatRendezVous etat, boolean empty) {
                super.updateItem(etat, empty);

                if (empty || etat == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Vérifier si le médecin possède cet état
                    boolean possede = serviceMedecin.possedeEtat(medecin, etat);

                    // Mettre à jour le texte et le style du label
                    labelEtat.setText(etat.getLibelle());
                    labelEtat.setStyle(possede ?
                            "-fx-font-weight: bold;" :
                            "-fx-font-weight: normal;");

                    // Mettre à jour le texte et le style du bouton
                    btnToggle.setText(possede ? "Désactiver" : "Activer");
                    btnToggle.setStyle(possede
                            ? "-fx-background-color: #ff4444; -fx-text-fill: white;"
                            : "-fx-background-color: #4CAF50; -fx-text-fill: white;");

                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
    }





    ////////////////////////filtre

    @FXML
    private void reinitialiserFiltres() {
        // Réinitialiser les valeurs des filtres
        filterDatePicker.setValue(null);
        filterHeureComboBox.setValue(null);
        filterEtatComboBox.setValue(null);

        // Recharger tous les rendez-vous
        int medecinId = serviceMedecin.idmed(medecin);
        loadData(medecinId);
    }

    // Méthode pour afficher les rendez-vous filtrés (similaire à loadData mais avec une liste déjà filtrée)
    private void afficherRendezVousFiltres(List<RendezVous> rdvs, int medecinId) {
        // Effacer le contenu précédent
        vboxContainer.getChildren().clear();

        // Pour chaque rendez-vous, créer un panneau d'affichage
        for (RendezVous rdv : rdvs) {
            // Ne pas afficher les rendez-vous refusés (par exemple archivés)
            if ("Refusé".equals(rdv.getCause())) {
                continue;
            }

            // Récupérer les informations du patient associé
            Patient patient = servicePatient.getPatientById(rdv.getPatientId());
            String nom = (patient != null && patient.getLastName() != null) ? patient.getLastName() : "Inconnu";
            String prenom = (patient != null && patient.getFirstName() != null) ? patient.getFirstName() : "Inconnu";
            int age = (patient != null) ? patient.getAge() : 0;

            // Récupérer le libellé de l'état du rendez-vous
            EtatRendezVous etatRendezVous = serviceEtatRendezVous.getById(rdv.getEtatId());
            String libelleEtat = (etatRendezVous != null) ? etatRendezVous.getLibelle() : "Inconnu";

            // Formater la date et l'heure du rendez-vous
            String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate());
            String heureStr = new SimpleDateFormat("HH:mm").format(rdv.getHeure());

            // Création d'un panneau pour afficher le rendez-vous
            BorderPane rdvPane = new BorderPane();
            rdvPane.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-background-color: #f9f9f9;");
            rdvPane.setPadding(new Insets(10));

            Label infoLabel = new Label(String.format("Patient(e) : %s %s  |  Âge : %d\nÉtat : %s\nDate : %s   Heure : %s",
                    prenom, nom, age, libelleEtat, dateStr, heureStr));
            rdvPane.setLeft(infoLabel);
            BorderPane.setMargin(infoLabel, new Insets(0, 10, 0, 0));

            // Création du conteneur pour les boutons/actions du rendez-vous
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            if (!rdv.isStatut() && !rdv.isAnnule()) {
                Button btnAccept = new Button("Accepter");
                btnAccept.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                btnAccept.setOnAction(e -> {
                    rdv.setStatut(true);
                    serviceRendezVous.traiterRendezVous(rdv, true);
                    loadData(medecinId);
                });
                Button btnRefuse = new Button("Refuser");
                btnRefuse.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                btnRefuse.setOnAction(e -> {
                    rdv.setStatut(false);
                    rdv.setCause("Refusé");
                    serviceRendezVous.traiterRendezVous(rdv, false);
                    loadData(medecinId);
                });
                buttonBox.getChildren().addAll(btnAccept, btnRefuse);
            } else if (rdv.isStatut() && !rdv.isAnnule()) {
                Button btnAnnuler = new Button("Annuler");
                btnAnnuler.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                btnAnnuler.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Annulation du rendez-vous");
                    dialog.setHeaderText("Veuillez entrer la raison de l'annulation :");
                    dialog.setContentText("Motif :");
                    Optional<String> result = dialog.showAndWait();
                    if (result.isPresent() && !result.get().trim().isEmpty()) {
                        rdv.setAnnule(true);
                        rdv.setCause(result.get());
                        serviceRendezVous.modifer(rdv);
                        loadData(medecinId);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText(null);
                        alert.setContentText("Le motif est obligatoire.");
                        alert.showAndWait();
                    }
                });
                Button btnModifier = new Button("Modifier");
                btnModifier.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                btnModifier.setOnAction(e -> {
                    Dialog<RendezVous> dialog = new Dialog<>();
                    dialog.setTitle("Modifier le rendez-vous");
                    dialog.setHeaderText("Modifier la date, l'heure et indiquer la cause de modification");

                    DatePicker datePicker = new DatePicker(rdv.getDate().toLocalDate());
                    datePicker.setDayCellFactory(new Callback<>() {
                        @Override
                        public DateCell call(DatePicker picker) {
                            return new DateCell() {
                                @Override
                                public void updateItem(LocalDate date, boolean empty) {
                                    super.updateItem(date, empty);
                                    setDisable(empty || date.isBefore(LocalDate.now()) ||
                                            date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                            date.getDayOfWeek() == DayOfWeek.SUNDAY);
                                }
                            };
                        }
                    });

                    ComboBox<String> heureComboBox = new ComboBox<>();
                    Date initialDate = Date.valueOf(datePicker.getValue());
                    heureComboBox.setItems(FXCollections.observableArrayList(genererCreneauxDisponibles(initialDate, medecinId)));
                    datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
                        if (newDate != null) {
                            Date sqlDate = Date.valueOf(newDate);
                            heureComboBox.setItems(FXCollections.observableArrayList(genererCreneauxDisponibles(sqlDate, medecinId)));
                        }
                    });

                    TextField causeField = new TextField();
                    causeField.setPromptText("Cause de la modification");

                    VBox vbox = new VBox(10);
                    vbox.getChildren().addAll(
                            new Label("Nouvelle date :"), datePicker,
                            new Label("Nouvel horaire disponible :"), heureComboBox,
                            new Label("Cause :"), causeField
                    );
                    dialog.getDialogPane().setContent(vbox);

                    ButtonType modifierButton = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(modifierButton, ButtonType.CANCEL);

                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == modifierButton) {
                            if (datePicker.getValue() == null) {
                                new Alert(Alert.AlertType.ERROR, "La date est obligatoire.").showAndWait();
                                return null;
                            }
                            if (heureComboBox.getValue() == null || heureComboBox.getValue().trim().isEmpty()) {
                                new Alert(Alert.AlertType.ERROR, "Veuillez sélectionner un horaire.").showAndWait();
                                return null;
                            }
                            if (causeField.getText().trim().isEmpty()) {
                                new Alert(Alert.AlertType.ERROR, "La cause est obligatoire.").showAndWait();
                                return null;
                            }
                            try {
                                LocalDate newDate = datePicker.getValue();
                                LocalTime newTime = LocalTime.parse(heureComboBox.getValue());
                                rdv.setDate(Date.valueOf(newDate));
                                rdv.setHeure(Time.valueOf(newTime));
                                rdv.setCause(causeField.getText());
                                serviceRendezVous.modifer(rdv);
                                return rdv;
                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR, "Erreur lors de la modification des valeurs.").showAndWait();
                            }
                        }
                        return null;
                    });

                    Optional<RendezVous> result = dialog.showAndWait();
                    result.ifPresent(r -> loadData(medecinId));
                });
                buttonBox.getChildren().addAll(btnAnnuler, btnModifier);
            } else if (rdv.isAnnule()) {
                Label canceledLabel = new Label("Annulé");
                buttonBox.getChildren().add(canceledLabel);
            }

            rdvPane.setRight(buttonBox);
            vboxContainer.getChildren().add(rdvPane);
        }
    }
    @FXML
    private void appliquerFiltres() {
        if (medecin == null) return;

        // Récupérer les valeurs des filtres
        LocalDate dateFiltre = filterDatePicker.getValue();
        String heureFiltre = filterHeureComboBox.getValue();
        EtatRendezVous etatFiltre = filterEtatComboBox.getValue();

        // Récupérer tous les rendez-vous du médecin
        int medecinId = serviceMedecin.idmed(medecin);
        List<RendezVous> tousRdvs = serviceRendezVous.afficherPourMedecin(medecin, medecinId);

        // Appliquer les filtres
        List<RendezVous> rdvsFiltres = tousRdvs.stream()
                .filter(rdv -> {
                    boolean matchDate = dateFiltre == null || rdv.getDate().toLocalDate().equals(dateFiltre);
                    boolean matchHeure = heureFiltre == null || heureFiltre.isEmpty() ||
                            rdv.getHeure().toString().startsWith(heureFiltre);
                    boolean matchEtat = etatFiltre == null || rdv.getEtatId() == etatFiltre.getId();

                    return matchDate && matchHeure && matchEtat;
                })
                .toList();

        // Afficher les rendez-vous filtrés
        afficherRendezVousFiltres(rdvsFiltres, medecinId);
    }






    @FXML
    private void loadData() {
        int medid = serviceMedecin.idmed(medecin);
        loadData(medid);
        rendezVousSection.setVisible(true);
        etatsSection.setVisible(false);
        rendezVousEnLigneSection.setVisible(false);

    }

    @FXML
    private void chargerEtatsMedecin() {
        int medid = serviceMedecin.idmed(medecin);
        System.out.println("medid: " + medid);
        chargerEtatsMedecin(medid);
        rendezVousSection.setVisible(false);
        etatsSection.setVisible(true);
        rendezVousEnLigneSection.setVisible(false);
        filterEtatComboBox.setVisible(false);
        filterDatePicker.setVisible(false);
        filterHeureComboBox.setVisible(false);

    }
    @FXML
    public void afficherCalendrier(ActionEvent event) {
        try {
            // Utiliser le bon nom de fichier et chemin d'accès
            URL fxmlUrl = getClass().getResource("/calender.fxml");
            if (fxmlUrl == null) {
                System.err.println("Fichier calendar.fxml introuvable");
                // Essayer l'autre orthographe si la première tentative échoue
                fxmlUrl = getClass().getResource("/fxml/calender.fxml");
                if (fxmlUrl == null) {
                    System.err.println("Fichier calender.fxml introuvable également");
                    // Rechercher dans d'autres répertoires potentiels
                    System.err.println("Chemins possibles à vérifier:");
                    System.err.println("- /calendar.fxml");
                    System.err.println("- /calender.fxml");
                    System.err.println("- /org/example/fxml/calendar.fxml");

                    // Afficher une alerte à l'utilisateur
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText("Impossible d'ouvrir le calendrier");
                    alert.setContentText("Le fichier FXML du calendrier est introuvable.");
                    alert.showAndWait();
                    return;
                }
            }

            // Création d'une nouvelle Stage (fenêtre)
            Stage calendarStage = new Stage();
            calendarStage.setTitle("Calendrier");

            // Définition des dimensions de la fenêtre
            calendarStage.setWidth(800);
            calendarStage.setHeight(650);

            // Chargement du FXML
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent calendarRoot = loader.load();

            // Récupération du contrôleur associé et transmission de la référence
            CalendarController calendarController = loader.getController();
            calendarController.setMedecinConnecte(medecin);

            // Création d'une nouvelle scène
            Scene scene = new Scene(calendarRoot);
            calendarStage.setScene(scene);

            // Configuration de la fenêtre comme modale (optionnel)
            // Cela bloque l'interaction avec la fenêtre principale tant que celle-ci est ouverte
            calendarStage.initModality(Modality.APPLICATION_MODAL);

            // Si vous avez besoin de référencer la fenêtre principale comme parent
            // calendarStage.initOwner((Stage) ((Node) event.getSource()).getScene().getWindow());

            // Affichage de la fenêtre
            calendarStage.show();

            // Afficher explicitement le calendrier dans son contrôleur si nécessaire
            calendarController.afficherCalendrier();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier FXML : " + e.getMessage());
            e.printStackTrace();

            // Afficher une alerte à l'utilisateur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le calendrier");
            alert.setContentText("Une erreur s'est produite: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /////// les notif

    @FXML
    private void afficherNotifications() {
        if (medecin == null) return;
int medid = serviceMedecin.idmed(medecin);
        List<Notification> notifications = serviceNotification.getNotificationsForMedecin(medid);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vos notifications");
        dialog.setHeaderText("Liste des notifications");

        VBox notificationsBox = new VBox(10);
        notificationsBox.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(notificationsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        if (notifications.isEmpty()) {
            notificationsBox.getChildren().add(new Label("Aucune notification"));
        } else {
            for (Notification notification : notifications) {
                HBox notifBox = new HBox(10);
                notifBox.setAlignment(Pos.CENTER_LEFT);
                notifBox.setPadding(new Insets(5));
                notifBox.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

                if (!notification.isRead()) {
                    Circle indicator = new Circle(5);
                    indicator.setFill(Color.BLUE);
                    notifBox.getChildren().add(indicator);
                }

                Label messageLabel = new Label(notification.getMessage());
                messageLabel.setWrapText(true);

                Button markAsReadBtn = new Button("Marquer comme lu");
                markAsReadBtn.setDisable(notification.isRead());
                markAsReadBtn.setOnAction(e -> {
                    serviceNotification.markAsRead(notification.getId());
                    notifBox.getChildren().remove(0); // Supprimer l'indicateur
                    markAsReadBtn.setDisable(true);
                });

                HBox.setHgrow(messageLabel, Priority.ALWAYS);
                notifBox.getChildren().addAll(messageLabel, markAsReadBtn);
                notificationsBox.getChildren().add(notifBox);
            }
        }

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }


    private void updateNotificationCount() {
        if (medecin != null) {
            int medid=serviceMedecin.idmed(medecin);
            int count = serviceNotification.countUnreadNotificationsForMedecin(medid);
            Platform.runLater(() -> {
                if (count > 0) {
                    notificationCountLabel.setText(String.valueOf(count));
                    notificationCountLabel.setVisible(true);
                } else {
                    notificationCountLabel.setVisible(false);
                }
            });
        }
    }



    ////les api
    /**
     * Génère un lien Jitsi unique pour un rendez-vous en ligne
     * @param rdvId ID du rendez-vous
     * @param medecinId ID du médecin
     * @param patientId ID du patient
     * @return le lien Jitsi généré
     */
    @NotNull
    private String genererLienJitsi(int rdvId, int medecinId, int patientId) {
        // Générer un identifiant unique pour la salle de consultation
        String roomId = "med" + medecinId + "_pat" + patientId + "_rdv" + rdvId + "_" + System.currentTimeMillis();

        // Encoder l'identifiant pour éviter les caractères spéciaux
        try {
            roomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Fallback si l'encodage échoue
            roomId = "consultation_" + rdvId + "_" + System.currentTimeMillis();
        }

        // Construire le lien Jitsi
        return "https://meet.jit.si/" + roomId;
    }
    /**
     * Chargement des rendez-vous en ligne (etatId=3, statut=true, annule=false)
     * pour le médecin connecté
     */
    @FXML
    public void loadRendezVousEnLigne() {
        // First check if the doctor is logged in
        if (medecin == null) {
            System.out.println("Aucun médecin connecté");
            return;
        }

        // Clear previous content
        onlineAppointmentsContainer.getChildren().clear();

        // Get doctor's ID
        int medecinId = serviceMedecin.idmed(medecin);

        // Get online rendez-vous for this doctor
        List<RendezVous> rendezVousList = serviceRendezVous.afficherRendezVousEnLignePourMedecin(medecinId);

        if (rendezVousList.isEmpty()) {
            Label emptyLabel = new Label("Aucun rendez-vous en ligne disponible");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555; -fx-padding: 10px;");
            onlineAppointmentsContainer.getChildren().add(emptyLabel);
            return;
        }

        // Header for the list
        HBox header = createHeader();
        onlineAppointmentsContainer.getChildren().add(header);

        // Create a container for each appointment
        for (RendezVous rdv : rendezVousList) {
            Patient patient = servicePatient.getPatientById(rdv.getPatientId());

            if (patient != null) {
                // Format date and time
                String dateStr = rdv.getDate() != null ?
                        new SimpleDateFormat("dd/MM/yyyy").format(rdv.getDate()) : "Non spécifiée";
                String heureStr = rdv.getHeure() != null ?
                        new SimpleDateFormat("HH:mm").format(rdv.getHeure()) : "Non spécifiée";

                // Create appointment row
                HBox appointmentRow = createAppointmentRow(
                        patient.getLastName(),
                        patient.getFirstName(),
                        dateStr,
                        heureStr,
                        "Dossier_" + patient.getId() + ".pdf",
                        rdv.getLienJitsi()
                );

                onlineAppointmentsContainer.getChildren().add(appointmentRow);
            }
        }
    }

    /**
     * Création d'une ligne d'en-tête pour les rendez-vous en ligne
     */
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 5, 10, 5));
        header.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        Label nomLabel = createHeaderLabel("Nom", 120);
        Label prenomLabel = createHeaderLabel("Prénom", 120);
        Label dateLabel = createHeaderLabel("Date", 100);
        Label heureLabel = createHeaderLabel("Heure", 80);
        Label dossierLabel = createHeaderLabel("Dossier", 120);
        Label jitsiLabel = createHeaderLabel("Lien Meeting", 120);

        header.getChildren().addAll(nomLabel, prenomLabel, dateLabel, heureLabel, dossierLabel, jitsiLabel);

        return header;
    }

    /**
     * Création d'un label d'en-tête avec largeur fixe
     */
    private Label createHeaderLabel(String text, int width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    /**
     * Création d'une ligne pour un rendez-vous en ligne
     */
    private HBox createAppointmentRow(String nom, String prenom, String date, String heure, String dossierMedical, String lienJitsi) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 5, 10, 5));
        row.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Patient info
        Label nomLabel = new Label(nom);
        nomLabel.setPrefWidth(120);

        Label prenomLabel = new Label(prenom);
        prenomLabel.setPrefWidth(120);

        // Date and time
        Label dateLabel = new Label(date);
        dateLabel.setPrefWidth(100);

        Label heureLabel = new Label(heure);
        heureLabel.setPrefWidth(80);

        // Dossier medical button
        Button dossierButton = new Button("Ouvrir");
        dossierButton.setPrefWidth(120);
        dossierButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        dossierButton.setOnAction(event -> openDossierMedical(dossierMedical));

        // Jitsi button
        Button jitsiButton = new Button("Rejoindre");
        jitsiButton.setPrefWidth(120);
        jitsiButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        jitsiButton.setOnAction(event -> openJitsiMeeting(lienJitsi));

        row.getChildren().addAll(nomLabel, prenomLabel, dateLabel, heureLabel, dossierButton, jitsiButton);

        return row;
    }

    /**
     * Ouverture du dossier médical du patient
     */
    private void openDossierMedical(String fileName) {
        try {
            // Assuming the medical files are stored in a specific directory
            File dossierFile = new File("./dossiers_medicaux/" + fileName);
            if (dossierFile.exists()) {
                Desktop.getDesktop().open(dossierFile);
            } else {
                System.out.println("Le fichier du dossier médical n'existe pas: " + dossierFile.getAbsolutePath());
                // Optionally, show an alert or notification to the user
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de l'ouverture du dossier médical: " + e.getMessage());
            // Optionally, show an alert or notification to the user
        }
    }

    /**
     * Ouverture du lien Jitsi pour la consultation en ligne
     */
    private void openJitsiMeeting(String lienJitsi) {
        if (lienJitsi != null && !lienJitsi.isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI(lienJitsi));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Erreur lors de l'ouverture du lien Jitsi: " + e.getMessage());
                // Optionally, show an alert or notification to the user
            }
        }
    }

    /**
     * Action pour le bouton de rafraîchissement des rendez-vous en ligne
     */
    @FXML
    private void handleRefreshOnlineAppointments() {
        loadRendezVousEnLigne();
    }
    @FXML
    private void afficherRendezVousEnLigne() {
        // Hide other sections
        rendezVousSection.setVisible(false);
        etatsSection.setVisible(false);
        calendrierSection.setVisible(false);

        // Show online appointments section
        rendezVousEnLigneSection.setVisible(true);

        // Load online appointments data
        loadRendezVousEnLigne();

        // Update navigation buttons style
        rdvEnLigneButton.setStyle("-fx-background-color: white; -fx-text-fill: #3498db; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12;");

        notificationsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: white; -fx-border-radius: 8; -fx-padding: 12;");
    }

}