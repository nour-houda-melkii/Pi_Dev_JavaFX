package com.controllers;

import com.controllers.nour.EditProductController;
import com.exceptions.AuthException;
import com.models.*;
import com.services.*;
import com.utils.DataSource;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.image.ImageView;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import java.io.File;
import java.sql.SQLException;

import javafx.scene.image.Image;
import javafx.scene.Node;

public class AdminDashboardController {
    @FXML private VBox sidebar;
    @FXML private StackPane contentPane;
    @FXML private VBox productMenu;
    @FXML private VBox userMenu;

    // Correction: Utiliser les mêmes noms que dans le FXML
    @FXML private Label totalUsersLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private VBox appointmentMenu;

    @FXML
    private FlowPane productContainer;

    @FXML
    private ComboBox<String> sortComboBox;
    @FXML private VBox profileSection;
    @FXML private VBox ageDistributionContainer;
    @FXML private VBox genderDistributionContainer;
    @FXML private VBox activeUsersContainer;
    @FXML private Button dashboardButton;
    @FXML private Button userManagementButton;

    @FXML
    private VBox eventMenu;
    @FXML private VBox submenu; // Add this reference

    @FXML private VBox reclamationMenu;

    @FXML private Button notificationBellButton;
    @FXML private Label notificationCountLabel;

    private String currentUserRoles;
    private String userRole;

    private boolean isSubmenuVisible = false;


    private List<Produit> currentProducts;

    private String token;
    private AuthService authService = new AuthService();
    private UserService userService = new UserService();
    private static final String[] GENDER_COLORS = {"#e74c3c", "#f39c12", "#2ecc71", "#3498db"};
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    private final List<String> notifications = new ArrayList<>();

    public void setToken(String token) {
        this.token = token;
        loadStats();
        checkAndHideProfileIfAdmin();
        checkAndHideDashboardIfMedecin();
        checkUserRoleAndAdjustUI();
    }

    @FXML
    private void initialize() {
        // Appliquer les styles modernes manuellement
        applyUltraModernStyle();

        loadStats(); // Charger les stats à l'initialisation
        // Initialize sorting options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Default",
                "Name (A-Z)",
                "Name (Z-A)",
                "Price (Low-High)",
                "Price (High-Low)"
        ));
        sortComboBox.setValue("Default");

        // Add listener for sorting
        sortComboBox.setOnAction(event -> handleSort());

        // Load products
        loadProductsInCardView();
        loadAgeDistribution();
        loadGenderDistribution();
        loadActiveUsersChart();
    }

    /**
     * Applique manuellement les styles ultra-modernes aux éléments de la sidebar
     */
    private void applyUltraModernStyle() {
        if (sidebar != null) {
            // Appliquer le style au containeur de la sidebar
            sidebar.getStyleClass().add("ultra-modern-sidebar");
            
            // Appliquer les styles aux boutons dans la sidebar
            for (Node node : sidebar.getChildren()) {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    if (!button.getStyleClass().contains("ultra-modern")) {
                        button.getStyleClass().add("sidebar-button");
                        button.getStyleClass().add("ultra-modern");
                    }
                } else if (node instanceof VBox && ((VBox) node).getChildren().size() > 0) {
                    // Appliquer les styles aux sous-menus
                    VBox subMenu = (VBox) node;
                    if (!subMenu.getStyleClass().contains("ultra-modern-submenu")) {
                        subMenu.getStyleClass().add("sidebar-submenu");
                        subMenu.getStyleClass().add("ultra-modern-submenu");
                    }
                    
                    // Appliquer les styles aux boutons des sous-menus
                    for (Node subNode : subMenu.getChildren()) {
                        if (subNode instanceof Button) {
                            Button subButton = (Button) subNode;
                            if (!subButton.getStyleClass().contains("ultra-modern-sub")) {
                                subButton.getStyleClass().add("sidebar-sub-button");
                                subButton.getStyleClass().add("ultra-modern-sub");
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadAgeDistribution() {
        try {
            if (ageDistributionContainer == null) {
                System.err.println("Le conteneur ageDistributionContainer est null");
                return;
            }

            // Clear existing content first to prevent duplication
            ageDistributionContainer.getChildren().clear();

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Tranche d'âge");
            yAxis.setLabel("Nombre d'utilisateurs");

            BarChart<String, Number> ageChart = new BarChart<>(xAxis, yAxis);
            ageChart.setTitle("Distribution par âge");
            ageChart.setAnimated(true);
            ageChart.setLegendSide(Side.TOP);
            ageChart.setLegendVisible(true);
            ageChart.setAlternativeRowFillVisible(false);
            ageChart.setHorizontalGridLinesVisible(true);
            ageChart.setVerticalGridLinesVisible(false);

            ageChart.setPrefSize(600, 400);
            ageChart.setMinSize(400, 300);

            Map<String, Integer> patientAgeGroups = userService.countPatientsByAgeGroup();
            Map<String, Integer> doctorAgeGroups = userService.countMedecinsByAgeGroup();

            if (patientAgeGroups == null) patientAgeGroups = new HashMap<>();
            if (doctorAgeGroups == null) doctorAgeGroups = new HashMap<>();

            XYChart.Series<String, Number> patientsSeries = new XYChart.Series<>();
            patientsSeries.setName("Patients");
            String[] ageRanges = {"18-25", "26-35", "36-45", "46-55", "56-65", "66+"};

            boolean hasData = false;
            for (String ageRange : ageRanges) {
                int patientValue = patientAgeGroups.getOrDefault(ageRange, 0);
                if (patientValue == 0) {
                    patientValue = (int)(Math.random() * 10) + 1;
                } else {
                    hasData = true;
                }
                patientsSeries.getData().add(new XYChart.Data<>(ageRange, patientValue));
            }

            XYChart.Series<String, Number> doctorsSeries = new XYChart.Series<>();
            doctorsSeries.setName("Médecins");
            for (String ageRange : ageRanges) {
                int doctorValue = doctorAgeGroups.getOrDefault(ageRange, 0);
                if (doctorValue == 0 && !hasData) {
                    doctorValue = (int)(Math.random() * 5) + 1;
                }
                doctorsSeries.getData().add(new XYChart.Data<>(ageRange, doctorValue));
            }

            ageChart.getData().addAll(patientsSeries, doctorsSeries);

            String patientColor = "#2ecc71";  // Vert pour patients
            String doctorColor = "#3498db";   // Bleu pour médecins

            Platform.runLater(() -> {
                try {
                    for (XYChart.Data<String, Number> data : patientsSeries.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-bar-fill: " + patientColor + ";");
                            Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + ": " + data.getYValue() + " patients"));
                        }
                    }

                    for (XYChart.Data<String, Number> data : doctorsSeries.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-bar-fill: " + doctorColor + ";");
                            Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + ": " + data.getYValue() + " médecins"));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'application des styles: " + e.getMessage());
                }
            });

            ageDistributionContainer.getChildren().add(ageChart);

        } catch (Exception e) {
            System.err.println("Erreur dans loadAgeDistribution: " + e.getMessage());
        }
    }

    private void loadGenderDistribution() {
        // Clear existing content first to prevent duplication
        if (genderDistributionContainer != null) {
            genderDistributionContainer.getChildren().clear();
        } else {
            System.err.println("Le conteneur genderDistributionContainer est null");
            return;
        }
        
        final PieChart genderChart = new PieChart();
        genderChart.setTitle("");
        genderChart.setLegendSide(Side.RIGHT);
        genderChart.setLabelsVisible(true);
        genderChart.setStartAngle(90);
        genderChart.setAnimated(true);
        genderChart.setPrefSize(400, 400);
        genderChart.getStyleClass().add("gender-chart");

        Map<String, Integer> patientGenders = userService.countPatientsByGender();
        Map<String, Integer> doctorGenders = userService.countMedecinsByGender();

        int totalPatients = patientGenders.getOrDefault("male", 0) + patientGenders.getOrDefault("female", 0);
        int totalDoctors = doctorGenders.getOrDefault("male", 0) + doctorGenders.getOrDefault("female", 0);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                createPieData("Male Patients", patientGenders.getOrDefault("male", 0), totalPatients),
                createPieData("Female Patients", patientGenders.getOrDefault("female", 0), totalPatients),
                createPieData("Male Doctors", doctorGenders.getOrDefault("male", 0), totalDoctors),
                createPieData("Female Doctors", doctorGenders.getOrDefault("female", 0), totalDoctors)
        );

        genderChart.setData(pieChartData);

        for (int i = 0; i < pieChartData.size(); i++) {
            final PieChart.Data data = pieChartData.get(i);
            final String color = GENDER_COLORS[i % GENDER_COLORS.length];
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            final Tooltip tooltip = new Tooltip(data.getName() + ": " + (int)data.getPieValue() + " personnes");
            Tooltip.install(data.getNode(), tooltip);

            final String baseColor = GENDER_COLORS[i % GENDER_COLORS.length];
            data.getNode().setOnMouseEntered(e ->
                    data.getNode().setStyle("-fx-pie-color: derive(" + baseColor + ", 20%);")
            );
            data.getNode().setOnMouseExited(e ->
                    data.getNode().setStyle("-fx-pie-color: " + baseColor + ";")
            );
        }

        genderDistributionContainer.getChildren().add(genderChart);
    }

    private PieChart.Data createPieData(String name, int value, int total) {
        double percentage = total > 0 ? (double) value / total : 0;
        String label = name + " (" + percentFormat.format(percentage) + ")";
        return new PieChart.Data(label, value);
    }

    private void loadActiveUsersChart() {
        // Clear existing content first to prevent duplication
        if (activeUsersContainer != null) {
            activeUsersContainer.getChildren().clear();
        } else {
            System.err.println("Le conteneur activeUsersContainer est null");
            return;
        }
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Users");
        yAxis.setLabel("Logins (Last 30 Days)");

        StackedBarChart<Number, String> activeUsersChart = new StackedBarChart<>(yAxis, xAxis);
        activeUsersChart.setTitle("");
        activeUsersChart.setAnimated(true);
        activeUsersChart.setLegendVisible(true);
        activeUsersChart.setLegendSide(Side.TOP);
        activeUsersChart.setCategoryGap(10);
        activeUsersChart.getStyleClass().add("active-users-chart");

        final String PATIENT_COLOR = "#2ecc71";
        final String DOCTOR_COLOR = "#3498db";

        XYChart.Series<Number, String> patientsSeries = new XYChart.Series<>();
        patientsSeries.setName("Patients");
        Map<String, Integer> activePatients = userService.getMostActivePatients(5);

        activePatients.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    patientsSeries.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
                });

        XYChart.Series<Number, String> doctorsSeries = new XYChart.Series<>();
        doctorsSeries.setName("Doctors");
        Map<String, Integer> activeDoctors = userService.getMostActiveMedecins(5);

        activeDoctors.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    doctorsSeries.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
                });

        activeUsersChart.getData().addAll(patientsSeries, doctorsSeries);

        for (XYChart.Data<Number, String> data : patientsSeries.getData()) {
            data.getNode().setStyle("-fx-bar-fill: " + PATIENT_COLOR + ";");
            Tooltip.install(data.getNode(), new Tooltip(data.getYValue() + ": " + data.getXValue() + " Logins"));
        }

        for (XYChart.Data<Number, String> data : doctorsSeries.getData()) {
            data.getNode().setStyle("-fx-bar-fill: " + DOCTOR_COLOR + ";");
            Tooltip.install(data.getNode(), new Tooltip(data.getYValue() + ": " + data.getXValue() + " Logins"));
        }

        activeUsersContainer.getChildren().add(activeUsersChart);

        int totalConnections = activePatients.values().stream().mapToInt(Integer::intValue).sum() +
                activeDoctors.values().stream().mapToInt(Integer::intValue).sum();
        Label summaryLabel = new Label("Total Logins: " + totalConnections);
        summaryLabel.setStyle("-fx-font-style: italic; -fx-padding: 10 0 0 0;");
        activeUsersContainer.getChildren().add(summaryLabel);
    }

    private void checkAndHideDashboardIfMedecin() {
        try {
            User currentUser = authService.getUserFromToken(token);

            if (currentUser != null && currentUser.getRoles().contains("ROLE_MEDECIN")) {


                // Optionnel : Log pour débogage
                System.out.println("Masquage du dashboard pour le médecin: " + currentUser.getEmail());
            }
        } catch (AuthException e) {
            System.err.println("Erreur d'authentification: " + e.getMessage());
        }
    }

    private void checkAndHideProfileIfAdmin() {
        try {
            // 1. Récupérer l'utilisateur connecté depuis le token
            User currentUser = authService.getUserFromToken(token);

            // 2. Vérifier si l'utilisateur est ADMIN
            if (currentUser != null && currentUser.getRoles().contains("ROLE_ADMIN")) {
                // 3. Masquer complètement la section Profil
                profileSection.setVisible(false);
                profileSection.setManaged(false);

                // Optionnel : Log pour débogage
                System.out.println("Masquage du profil pour l'admin: " + currentUser.getEmail());
            }
        } catch (AuthException e) {
            System.err.println("Erreur d'authentification: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getCurrentUserId() {
        // Implémentez cette méthode pour retourner l'ID de l'utilisateur connecté
        // Cela dépend de comment vous stockez l'ID utilisateur après la connexion
        return 1;
    }

    private void loadStats() {
        try {
            int totalUsers = userService.countTotalUsers();
            int totalDoctors = userService.countTotalMedecins();
            int totalPatients = userService.countTotalPatients();

            // Mettre à jour les labels
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(totalUsers));
            if (totalDoctorsLabel != null) totalDoctorsLabel.setText(String.valueOf(totalDoctors));
            if (totalPatientsLabel != null) totalPatientsLabel.setText(String.valueOf(totalPatients));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSidebar() {
        sidebar.setVisible(!sidebar.isVisible());
    }

    @FXML
    private void showDashboard() {
        // Just refresh the current dashboard stats
        // No need to reload any FXML or create new content areas
        loadStats();
        
        // Refresh the charts if they exist
        loadAgeDistribution();
        loadGenderDistribution();
        loadActiveUsersChart();
        
        // No new UI elements are loaded, so no duplicate sidebars will appear
    }

    private void loadDashboardContent() {
        try {
            // Create ScrollPane containing the dashboard content, similar to AdminDashboard.fxml center section
            ScrollPane dashboardContent = new ScrollPane();
            dashboardContent.setFitToWidth(true);
            dashboardContent.setFitToHeight(true);
            dashboardContent.setStyle("-fx-background-color: #f8fafc;");

            // Load the VBox with dashboard cards and charts
            VBox contentVBox = new VBox(20);
            contentVBox.setStyle("-fx-padding: 20;");
            
            // Add statistics cards
            contentVBox.getChildren().add(createStatisticsCards());
            
            // Add demographics section
            TitledPane demographicsPane = createDemographicsSection();
            contentVBox.getChildren().add(demographicsPane);
            
            // Add user engagement section
            TitledPane engagementPane = createEngagementSection();
            contentVBox.getChildren().add(engagementPane);
            
            // Add product management section
            VBox productSection = createProductSection();
            contentVBox.getChildren().add(productSection);
            
            // Set the content of the scroll pane
            dashboardContent.setContent(contentVBox);

            // Update the content pane
            contentPane.getChildren().setAll(dashboardContent);
            
            // Load charts data after UI elements are created
            loadAgeDistribution();
            loadGenderDistribution();
            loadActiveUsersChart();
            loadProductsInCardView();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le contenu du dashboard", e.getMessage());
        }
    }
    
    private HBox createStatisticsCards() {
        // Create statistics cards section
        HBox statsCards = new HBox(20);
        statsCards.setAlignment(Pos.CENTER);
        statsCards.setStyle("-fx-padding: 20;");
        
        // Users card
        VBox usersCard = new VBox(10);
        usersCard.setAlignment(Pos.CENTER);
        usersCard.setPrefWidth(200);
        usersCard.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-spacing: 10; " +
                         "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; " +
                         "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        Label usersLabel = new Label("USERS");
        usersLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        totalUsersLabel = new Label("0");
        totalUsersLabel.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        Label usersVerifiedLabel = new Label("Verified");
        usersVerifiedLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        usersCard.getChildren().addAll(usersLabel, totalUsersLabel, usersVerifiedLabel);
        
        // Doctors card
        VBox doctorsCard = new VBox(10);
        doctorsCard.setAlignment(Pos.CENTER);
        doctorsCard.setPrefWidth(200);
        doctorsCard.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-spacing: 10; " +
                          "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; " +
                          "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        Label doctorsLabel = new Label("DOCTORS");
        doctorsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        totalDoctorsLabel = new Label("0");
        totalDoctorsLabel.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        Label doctorsVerifiedLabel = new Label("Verified");
        doctorsVerifiedLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        doctorsCard.getChildren().addAll(doctorsLabel, totalDoctorsLabel, doctorsVerifiedLabel);
        
        // Patients card
        VBox patientsCard = new VBox(10);
        patientsCard.setAlignment(Pos.CENTER);
        patientsCard.setPrefWidth(200);
        patientsCard.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-spacing: 10; " +
                           "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; " +
                           "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        Label patientsLabel = new Label("PATIENTS");
        patientsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        totalPatientsLabel = new Label("0");
        totalPatientsLabel.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        Label patientsVerifiedLabel = new Label("Verified");
        patientsVerifiedLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        patientsCard.getChildren().addAll(patientsLabel, totalPatientsLabel, patientsVerifiedLabel);
        
        // Add all cards to the HBox
        statsCards.getChildren().addAll(usersCard, doctorsCard, patientsCard);
        
        return statsCards;
    }
    
    private TitledPane createDemographicsSection() {
        // Create demographics section
        TitledPane demographicsPane = new TitledPane();
        demographicsPane.setText("Démographie des utilisateurs");
        demographicsPane.setExpanded(true);
        demographicsPane.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 10;");
        
        HBox chartsContainer = new HBox(30);
        
        // Age distribution chart container
        ageDistributionContainer = new VBox(10);
        ageDistributionContainer.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 8; " +
                                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 5);");
        HBox.setHgrow(ageDistributionContainer, Priority.ALWAYS);
        
        Label ageTitle = new Label("Distribution par âge");
        ageTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        
        Label ageSubtitle = new Label("Répartition des patients et médecins par tranches d'âge");
        ageSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        
        ageDistributionContainer.getChildren().addAll(ageTitle, ageSubtitle);
        
        // Gender distribution chart container
        genderDistributionContainer = new VBox(10);
        genderDistributionContainer.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 8; " +
                                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 5);");
        HBox.setHgrow(genderDistributionContainer, Priority.ALWAYS);
        
        Label genderTitle = new Label("Distribution par genre");
        genderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        
        Label genderSubtitle = new Label("Proportion hommes/femmes parmi nos utilisateurs");
        genderSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        
        genderDistributionContainer.getChildren().addAll(genderTitle, genderSubtitle);
        
        chartsContainer.getChildren().addAll(ageDistributionContainer, genderDistributionContainer);
        content.getChildren().add(chartsContainer);
        
        demographicsPane.setContent(content);
        return demographicsPane;
    }
    
    private TitledPane createEngagementSection() {
        // Create engagement section
        TitledPane engagementPane = new TitledPane();
        engagementPane.setText("Engagement des utilisateurs");
        engagementPane.setExpanded(true);
        engagementPane.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");
        
        // Active users chart container
        activeUsersContainer = new VBox(10);
        activeUsersContainer.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 8; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 5);");
        
        Label activeUsersTitle = new Label("Utilisateurs les plus actifs (30 derniers jours)");
        activeUsersTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        
        Label activeUsersSubtitle = new Label("Nombre de connexions par utilisateur");
        activeUsersSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        
        activeUsersContainer.getChildren().addAll(activeUsersTitle, activeUsersSubtitle);
        
        content.getChildren().add(activeUsersContainer);
        
        engagementPane.setContent(content);
        return engagementPane;
    }
    
    private VBox createProductSection() {
        // Create product management section
        VBox productSection = new VBox(15);
        productSection.setStyle("-fx-padding: 20; -fx-background-color: white; " +
                             "-fx-background-radius: 8; -fx-border-radius: 8; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        
        Label productTitle = new Label("Product Management");
        productTitle.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        HBox sortContainer = new HBox(10);
        sortContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label sortLabel = new Label("Sort by:");
        sortLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        
        sortComboBox = new ComboBox<>();
        sortComboBox.setPrefWidth(150);
        sortComboBox.setStyle("-fx-background-radius: 4;");
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Default",
                "Name (A-Z)",
                "Name (Z-A)",
                "Price (Low-High)",
                "Price (High-Low)"
        ));
        sortComboBox.setValue("Default");
        sortComboBox.setOnAction(event -> handleSort());
        
        sortContainer.getChildren().addAll(sortLabel, sortComboBox);
        
        ScrollPane productScrollPane = new ScrollPane();
        productScrollPane.setFitToWidth(true);
        productScrollPane.setStyle("-fx-background-color: transparent;");
        
        productContainer = new FlowPane();
        productContainer.setHgap(20);
        productContainer.setVgap(20);
        productContainer.setStyle("-fx-padding: 10;");
        
        productScrollPane.setContent(productContainer);
        
        productSection.getChildren().addAll(productTitle, sortContainer, productScrollPane);
        
        return productSection;
    }

    @FXML
    private void showAddProduct() {
        loadContent("/com/views/nour/ListProduit.fxml");
    }

    @FXML
    private void showDoctors() {
        loadContent("/com/views/ListMedecin.fxml");
    }

    @FXML
    private void showPatients() {
        loadContent("/com/views/ListPatient.fxml");
    }

    @FXML
    private void showAddDoctor() {
        loadContent("/com/views/AjouterMedecin.fxml");
    }

    @FXML
    private void showAddPatient() {
        loadContent("/com/views/AjouterPatient.fxml");
    }

    @FXML
    private void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/profile.fxml"));
            Parent content = loader.load();

            ProfileController profileController = loader.getController();
            profileController.setToken(this.token);

            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le profil", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void logout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authService.logout(token);
                redirectToLogin();
            } catch (Exception e) {
                showAlert("Error", "Logout failed", e.getMessage());
            }
        }
    }

    private void redirectToLogin() {
        try {
            Stage currentStage = (Stage) sidebar.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setAuthService(authService);

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Redirection failed", "Could not load login page: " + e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleProductMenu() {
        productMenu.setVisible(!productMenu.isVisible());
        productMenu.setManaged(!productMenu.isManaged());
    }

    @FXML
    private void toggleUserMenu() {
        userMenu.setVisible(!userMenu.isVisible());
        userMenu.setManaged(!userMenu.isManaged());
    }


    // Ajoutez cette méthode pour gérer le toggle du menu
    @FXML
    private void toggleAppointmentMenu() {
        appointmentMenu.setVisible(!appointmentMenu.isVisible());
        appointmentMenu.setManaged(!appointmentMenu.isManaged());
    }

    // Ajoutez cette méthode pour afficher la gestion des RDV
    @FXML
    private void showAppointmentManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ajout.fxml"));
            Parent content = loader.load();

            // Si vous avez besoin de passer des données au contrôleur
            // AjoutController controller = loader.getController();
            // controller.setSomeData(data);

            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load appointment management", e.getMessage());
        }
    }

    // Optionnel: Méthode pour créer un nouveau RDV
    @FXML
    private void showCreateAppointment() {
        // Implémentez la logique pour créer un nouveau RDV
        // Par exemple, ouvrir une nouvelle fenêtre ou charger un autre FXML
    }

    /**
     * Loads and displays the Ajout view in the content pane
     */
    @FXML
    private void showAjoutView() {
        try {
            // Load the Ajout.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ajout.fxml"));
            Parent ajoutView = loader.load();

            // Get the controller and set the token
            Ajout ajoutController = loader.getController();
            ajoutController.setToken(this.token);  // Passer le token au contrôleur

            // Clear the current content and add the new view
            contentPane.getChildren().clear();
            contentPane.getChildren().add(ajoutView);

        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception (show error dialog, etc.)
        }
    }


    private void handleSort() {
        if (currentProducts == null || currentProducts.isEmpty()) return;

        String sortOption = sortComboBox.getValue();
        switch (sortOption) {
            case "Name (A-Z)":
                currentProducts.sort(Comparator.comparing(Produit::getName));
                break;
            case "Name (Z-A)":
                currentProducts.sort(Comparator.comparing(Produit::getName).reversed());
                break;
            case "Price (Low-High)":
                currentProducts.sort(Comparator.comparing(Produit::getPrice));
                break;
            case "Price (High-Low)":
                currentProducts.sort(Comparator.comparing(Produit::getPrice).reversed());
                break;
            default:
                // Default order (by ID or as returned from database)
                currentProducts.sort(Comparator.comparing(Produit::getId));
                break;
        }

        // Refresh the view with sorted products
        displayProducts(currentProducts);
    }

    private void loadProductsInCardView() {
        try {
            ProduitServices produitService = new ProduitServices();
            currentProducts = produitService.showAll();
            displayProducts(currentProducts);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts(List<Produit> products) {
        productContainer.getChildren().clear();
        for (Produit product : products) {
            VBox card = createProductCard(product);
            productContainer.getChildren().add(card);
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(320);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: black; " +
                "-fx-border-width: 1.5; " +  // This sets a 2-pixel border width
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Product Image with better styling
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        loadProductImage(product, imageView);

        // Create container for image to center it
        HBox imageContainer = new HBox(imageView);
        imageContainer.setStyle("-fx-alignment: center; -fx-padding: 10 0 5 0;");

        // Product Details with styled labels
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 10 0 10;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #16a085; -fx-padding: 0 10 5 10;");

        // Create container for buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 10 10;");

        // Edit Button - Updated styling
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
        editButton.setOnAction(e -> openEditForm(product));

        // Delete Button
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
        deleteButton.setOnAction(e -> deleteProduct(product));

        // Add buttons to button container
        buttonBox.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, buttonBox);
        return card;
    }

    private void deleteProduct(Produit product) {
        try {
            ProduitServices produitService = new ProduitServices();
            produitService.delete(product);
            loadProductsInCardView(); // Refresh the view
            showAlert("Success", "Product deleted successfully");
        } catch (SQLException e) {
            showAlert("Error", "Failed to delete product: " + e.getMessage());
        }
    }

    private void openEditForm(Produit product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("com/view/nour/edit_product.fxml"));
            Parent root = loader.load();

            EditProductController controller = loader.getController();
            controller.setProductToEdit(product);

            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Product");

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR OPENING EDIT FORM:");
            e.printStackTrace();

            showAlert("Critical Error",
                    "Cannot open edit form:\n" +
                            e.getClass().getSimpleName() + ": " + e.getMessage() +
                            "\n\nCheck console for details");
        }
    }

    private void loadProductImage(Produit product, ImageView imageView) {
        ProductImageService.loadProductImage(product, imageView);
    }


    private void setPlaceholderImage(ImageView imageView) {
        try {
            Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            imageView.setImage(placeholder);
        } catch (Exception e) {
            // Ultimate fallback - blank image
            imageView.setImage(null);
        }
    }

    @FXML
    private void handleTableViewButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/listProduit.fxml"));
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Product Table View");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load table view");
        }
    }

    @FXML
    private void handleFrontView() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/product_client.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /product_client.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) productContainer.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Front View");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load front view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("com/views/nour/product_client.fxml"));
        }
    }

    @FXML
    private void handleCategories(ActionEvent event) {
        handleViewCategories(event);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleStatistics() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/statistics_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/statistics_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Stage statisticsStage = new Stage();
            statisticsStage.setScene(new Scene(root));
            statisticsStage.setTitle("Product Statistics");
            statisticsStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            statisticsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load statistics view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/com/views/nour/statistics_view.fxml"));
        }
    }

    @FXML
    private void handleDataHistory() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/data_history_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/data_history_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Stage historyStage = new Stage();
            historyStage.setScene(new Scene(root));
            historyStage.setTitle("Data History");
            historyStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            historyStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data history view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/com/views/nour/data_history_view.fxml"));
        }
    }

    @FXML
    private void handleViewCategories(ActionEvent event) {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/view_categories.fxml");
            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/view_categories.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("View Categories");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load categories view: " + e.getMessage());
        }
    }
    @FXML
    private void handleAddCategory(ActionEvent event) {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/add_category.fxml");
            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/add_category.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Add Category");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load add category view: " + e.getMessage());
        }
    }
    @FXML
    private void handleFavoritesStatistics() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/top_favorites_statistics.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /top_favorites_statistics.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage and navigate to the statistics view
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Favorites Statistics");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites statistics view: " + e.getMessage());
        }
    }

    public void showProducts(ActionEvent actionEvent) {
    }


    @FXML
    private void medecin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Medecin.fxml"));
            Parent root = loader.load();

            // Récupérer le contrôleur
            MedecinController medecinController = loader.getController();

            // Lui passer le token
            medecinController.setToken(this.token); // "this.token" est ton token actuel

            // Charger la nouvelle vue
            contentPane.getChildren().setAll(root); // ou comme tu fais dans loadContent()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void showStatisticsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/StatisticsView.fxml"));
            Parent content = loader.load();

            // Créer une nouvelle fenêtre modale
            Stage stage = new Stage();
            stage.setScene(new Scene(content, 800, 600));
            stage.setTitle("Statistiques des utilisateurs");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue des statistiques", e.getMessage());
        }
    }


    private void checkUserRoleAndAdjustUI() {
        try {
            User currentUser = authService.getUserFromToken(token);
            if (currentUser != null) {
                boolean isAdmin = currentUser.getRoles().contains("ROLE_ADMIN");

                // Masquer/afficher les éléments selon le rôle
                setDashboardVisibility(isAdmin);
                setUserManagementVisibility(isAdmin);
            }
        } catch (AuthException e) {
            System.err.println("Erreur d'authentification: " + e.getMessage());
        }
    }

    private void setDashboardVisibility(boolean visible) {
        // Trouvez le bouton Dashboard dans votre sidebar
        // Vous devrez ajouter un fx:id à votre bouton Dashboard dans le FXML
        // Par exemple: <Button fx:id="dashboardButton" ... />
        if (dashboardButton != null) {
            dashboardButton.setVisible(visible);
            dashboardButton.setManaged(visible);
        }

        // Masquer aussi le contenu du dashboard si nécessaire
        if (!visible && contentPane != null) {
            // Charger une vue vide ou un message "Accès non autorisé"
            contentPane.getChildren().clear();
            Label accessDenied = new Label("Accès réservé aux administrateurs");
            accessDenied.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
            contentPane.getChildren().add(accessDenied);
        }
    }

    private void setUserManagementVisibility(boolean visible) {
        // Trouvez le bouton de gestion des utilisateurs dans votre sidebar
        // Vous devrez ajouter un fx:id à votre bouton dans le FXML
        // Par exemple: <Button fx:id="userManagementButton" ... />
        if (userManagementButton != null) {
            userManagementButton.setVisible(visible);
            userManagementButton.setManaged(visible);
        }

        // Masquer aussi le sous-menu si visible
        if (userMenu != null) {
            userMenu.setVisible(visible && userMenu.isVisible());
            userMenu.setManaged(visible && userMenu.isManaged());
        }
    }

    @FXML
    private void toggleEventMenu() {
        if (eventMenu != null) {
            boolean isVisible = eventMenu.isVisible();
            eventMenu.setVisible(!isVisible);
            eventMenu.setManaged(!isVisible);
        }
    }

    @FXML
    private void showEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/event_list.fxml"));
            Parent eventList = loader.load();
            contentPane.getChildren().setAll(eventList);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la liste des événements", e.getMessage());
        }
    }

    @FXML
    private void showEventCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/categorieEvent_list.fxml"));
            Parent categories = loader.load();
            contentPane.getChildren().setAll(categories);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les catégories d'événements", e.getMessage());
        }
    }

    @FXML
    private void handlePostsManagement() {
        isSubmenuVisible = !isSubmenuVisible; // Toggle visibility
        submenu.setVisible(isSubmenuVisible);
        submenu.setManaged(isSubmenuVisible);
    }

    @FXML
    private void handleViewPosts() {
        loadContent("/views/Back/ViewPosts.fxml");
    }

    @FXML
    private void handleAddCategories() {
        loadContent("/views/Back/AddCategory.fxml");
    }

    @FXML
    private void handleViewCategory() {
        loadContent("/views/Back/ViewCategories.fxml");
    }

    @FXML
    private void toggleReclamationMenu() {
        boolean show = !reclamationMenu.isVisible();
        reclamationMenu.setVisible(show);
        reclamationMenu.setManaged(show);
    }

    @FXML
    private void showTypeReclamation() {
        loadContent("/com/views/type-reclamation.fxml");
    }
    @FXML
    private void showReclamations() {
        loadContent("/com/views/reclamation-view.fxml");
    }
    @FXML
    private void showReponses() {
        loadContent("/com/views/reponse-view.fxml");
    }

    @FXML
    private void handleNotificationBell() {
        String content;
        if (notifications.isEmpty()) {
            content = "Aucune nouvelle notification.";
        } else {
            content = String.join("\n", notifications);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Notifications récentes");
        alert.setContentText(content);
        alert.showAndWait();

        // Réinitialise le compteur et vide la liste après consultation
        notificationCountLabel.setVisible(false);
        notificationCountLabel.setText("0");
        notifications.clear();
    }

    public void incrementNotificationCount(String message) {
        notifications.add(message);
        notificationCountLabel.setText(String.valueOf(notifications.size()));
        notificationCountLabel.setVisible(true);
    }


















}