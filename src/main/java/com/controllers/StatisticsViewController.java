package com.controllers;

import com.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class StatisticsViewController implements Initializable {

    @FXML private VBox ageDistributionContainer;
    @FXML private VBox genderDistributionContainer;
    @FXML private VBox activeUsersContainer;

    // Ajout des couleurs pour le graphique circulaire comme constante statique
    private static final String[] GENDER_COLORS = {"#e74c3c", "#f39c12", "#2ecc71", "#3498db"};

    private final UserService userService = new UserService();
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAgeDistribution();
        loadGenderDistribution();
        loadActiveUsersChart();
    }

    private void loadAgeDistribution() {
        try {
            // Vérification du conteneur
            if (ageDistributionContainer == null) {
                System.err.println("Le conteneur ageDistributionContainer est null");
                return;
            }

            // Créer les axes avec du style
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Tranche d'âge");
            yAxis.setLabel("Nombre d'utilisateurs");

            // Créer le graphique avec un style moderne
            BarChart<String, Number> ageChart = new BarChart<>(xAxis, yAxis);
            ageChart.setTitle("Distribution par âge");  // Ajouter un titre directement
            ageChart.setAnimated(true);
            ageChart.setLegendSide(Side.TOP);
            ageChart.setLegendVisible(true);
            ageChart.setAlternativeRowFillVisible(false);
            ageChart.setHorizontalGridLinesVisible(true);
            ageChart.setVerticalGridLinesVisible(false);

            // Définir des dimensions minimales
            ageChart.setPrefSize(600, 400);
            ageChart.setMinSize(400, 300);

            // Obtenir les données et vérifier qu'elles ne sont pas vides
            Map<String, Integer> patientAgeGroups = userService.countPatientsByAgeGroup();
            Map<String, Integer> doctorAgeGroups = userService.countMedecinsByAgeGroup();

            if (patientAgeGroups == null) {
                patientAgeGroups = new HashMap<>();
                System.err.println("patientAgeGroups est null");
            }

            if (doctorAgeGroups == null) {
                doctorAgeGroups = new HashMap<>();
                System.err.println("doctorAgeGroups est null");
            }

            // Log pour le debug
            System.out.println("Données patients: " + patientAgeGroups);
            System.out.println("Données médecins: " + doctorAgeGroups);

            // Données pour les patients avec style
            XYChart.Series<String, Number> patientsSeries = new XYChart.Series<>();
            patientsSeries.setName("Patients");

            // Trier les tranches d'âge dans l'ordre logique
            String[] ageRanges = {"18-25", "26-35", "36-45", "46-55", "56-65", "66+"};

            // Ajouter des valeurs factices pour le test (à retirer plus tard)
            boolean hasData = false;
            for (String ageRange : ageRanges) {
                int patientValue = patientAgeGroups.getOrDefault(ageRange, 0);
                if (patientValue == 0) {
                    // Ajouter des données factices pour le test
                    patientValue = (int)(Math.random() * 10) + 1;
                    System.out.println("Ajout de données factices pour le test: " + ageRange + " = " + patientValue);
                } else {
                    hasData = true;
                }
                patientsSeries.getData().add(new XYChart.Data<>(ageRange, patientValue));
            }

            // Données pour les médecins
            XYChart.Series<String, Number> doctorsSeries = new XYChart.Series<>();
            doctorsSeries.setName("Médecins");
            for (String ageRange : ageRanges) {
                int doctorValue = doctorAgeGroups.getOrDefault(ageRange, 0);
                if (doctorValue == 0 && !hasData) {
                    // Ajouter des données factices pour le test
                    doctorValue = (int)(Math.random() * 5) + 1;
                    System.out.println("Ajout de données factices pour le test: " + ageRange + " = " + doctorValue);
                }
                doctorsSeries.getData().add(new XYChart.Data<>(ageRange, doctorValue));
            }

            // Ajouter les séries au graphique
            ageChart.getData().addAll(patientsSeries, doctorsSeries);

            // Définir des couleurs uniques pour chaque série
            String patientColor = "#2ecc71";  // Vert pour patients
            String doctorColor = "#3498db";   // Bleu pour médecins

            // Reporte l'application des styles après que le graphique soit rendu
            Platform.runLater(() -> {
                try {
                    // Appliquer une couleur unique pour tous les patients
                    for (XYChart.Data<String, Number> data : patientsSeries.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-bar-fill: " + patientColor + ";");

                            final Tooltip tooltip = new Tooltip(
                                    data.getXValue() + ": " + data.getYValue() + " patients"
                            );
                            Tooltip.install(data.getNode(), tooltip);
                        }
                    }

                    // Appliquer une couleur unique pour tous les médecins
                    for (XYChart.Data<String, Number> data : doctorsSeries.getData()) {
                        if (data.getNode() != null) {
                            data.getNode().setStyle("-fx-bar-fill: " + doctorColor + ";");

                            final Tooltip tooltip = new Tooltip(
                                    data.getXValue() + ": " + data.getYValue() + " médecins"
                            );
                            Tooltip.install(data.getNode(), tooltip);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'application des styles: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Ajouter le graphique au container après l'avoir vidé
            ageDistributionContainer.getChildren().clear();
            ageDistributionContainer.getChildren().add(ageChart);

            System.out.println("Graphique ajouté au conteneur avec succès");

        } catch (Exception e) {
            System.err.println("Erreur dans loadAgeDistribution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadGenderDistribution() {
        // Créer le graphique circulaire avec un style moderne
        final PieChart genderChart = new PieChart();
        genderChart.setTitle("");  // Le titre est dans le FXML
        genderChart.setLegendSide(Side.RIGHT);
        genderChart.setLabelsVisible(true);
        genderChart.setStartAngle(90);
        genderChart.setAnimated(true);
        genderChart.setPrefSize(400, 400); // Largeur et hauteur souhaitées
        genderChart.getStyleClass().add("gender-chart");

        // Obtenir les données
        Map<String, Integer> patientGenders = userService.countPatientsByGender();
        Map<String, Integer> doctorGenders = userService.countMedecinsByGender();

        // Calculer les totaux pour les pourcentages
        int totalPatients = patientGenders.getOrDefault("male", 0) + patientGenders.getOrDefault("female", 0);
        int totalDoctors = doctorGenders.getOrDefault("male", 0) + doctorGenders.getOrDefault("female", 0);

        // Données avec labels clairs indiquant les pourcentages
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                createPieData("Male Patients", patientGenders.getOrDefault("male", 0), totalPatients),
                createPieData("Female Patients", patientGenders.getOrDefault("female", 0), totalPatients),
                createPieData("Male Doctors", doctorGenders.getOrDefault("male", 0), totalDoctors),
                createPieData(" Female Doctors", doctorGenders.getOrDefault("female", 0), totalDoctors)
        );

        genderChart.setData(pieChartData);

        // Personnaliser les couleurs et ajouter des tooltips
        for (int i = 0; i < pieChartData.size(); i++) {
            final PieChart.Data data = pieChartData.get(i);
            final String color = GENDER_COLORS[i % GENDER_COLORS.length];
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Ajouter des infobulles détaillées
            final Tooltip tooltip = new Tooltip(
                    data.getName() + ": " + (int)data.getPieValue() + " personnes"
            );
            Tooltip.install(data.getNode(), tooltip);

            // Ajouter un gestionnaire pour l'animation au survol
            final String baseColor = GENDER_COLORS[i % GENDER_COLORS.length];
            data.getNode().setOnMouseEntered(e ->
                    data.getNode().setStyle("-fx-pie-color: derive(" + baseColor + ", 20%);")
            );
            data.getNode().setOnMouseExited(e ->
                    data.getNode().setStyle("-fx-pie-color: " + baseColor + ";")
            );
        }

        // Ajouter le graphique au container
        genderDistributionContainer.getChildren().add(genderChart);

    }

    private PieChart.Data createPieData(String name, int value, int total) {
        double percentage = total > 0 ? (double) value / total : 0;
        String label = name + " (" + percentFormat.format(percentage) + ")";
        return new PieChart.Data(label, value);
    }


    private void loadActiveUsersChart() {
        // Créer les axes avec style
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Users");
        yAxis.setLabel("Logins (Last 30 Days)");

        // Créer un graphique à barres horizontales pour une meilleure lisibilité
        StackedBarChart<Number, String> activeUsersChart = new StackedBarChart<>(yAxis, xAxis);
        activeUsersChart.setTitle("");  // Le titre est dans le FXML
        activeUsersChart.setAnimated(true);
        activeUsersChart.setLegendVisible(true);
        activeUsersChart.setLegendSide(Side.TOP);
        activeUsersChart.setCategoryGap(10);
        activeUsersChart.getStyleClass().add("active-users-chart");

        // Définir les couleurs pour patients (bleu) et médecins (vert)
        final String PATIENT_COLOR = "#2ecc71"; // Vert pour patients
        final String DOCTOR_COLOR = "#3498db"; // Bleu pour médecins

        // Données pour les patients
        XYChart.Series<Number, String> patientsSeries = new XYChart.Series<>();
        patientsSeries.setName("Patients");
        Map<String, Integer> activePatients = userService.getMostActivePatients(5);

        // Trier par activité et limiter à 5 entrées
        activePatients.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    final String name = entry.getKey();
                    final Integer value = entry.getValue();
                    patientsSeries.getData().add(new XYChart.Data<>(value, name));
                });

        // Données pour les médecins
        XYChart.Series<Number, String> doctorsSeries = new XYChart.Series<>();
        doctorsSeries.setName("Doctors");
        Map<String, Integer> activeDoctors = userService.getMostActiveMedecins(5);

        // Trier par activité et limiter à 5 entrées
        activeDoctors.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    final String name = entry.getKey();
                    final Integer value = entry.getValue();
                    doctorsSeries.getData().add(new XYChart.Data<>(value, name));
                });

        // Ajouter les séries au graphique
        activeUsersChart.getData().addAll(patientsSeries, doctorsSeries);

        // Personnaliser les couleurs et ajouter des tooltips
        for (XYChart.Data<Number, String> data : patientsSeries.getData()) {
            data.getNode().setStyle("-fx-bar-fill: " + PATIENT_COLOR + ";");

            final Tooltip tooltip = new Tooltip(
                    data.getYValue() + ": " + data.getXValue() + " Logins"
            );
            Tooltip.install(data.getNode(), tooltip);
        }

        for (XYChart.Data<Number, String> data : doctorsSeries.getData()) {
            data.getNode().setStyle("-fx-bar-fill: " + DOCTOR_COLOR + ";");

            final Tooltip tooltip = new Tooltip(
                    data.getYValue() + ": " + data.getXValue() + " Logins"
            );
            Tooltip.install(data.getNode(), tooltip);
        }

        // Ajouter le graphique au container
        activeUsersContainer.getChildren().add(activeUsersChart);

        // Ajouter un label résumé avec le total des connexions
        int totalConnections = activePatients.values().stream().mapToInt(Integer::intValue).sum() +
                activeDoctors.values().stream().mapToInt(Integer::intValue).sum();
        Label summaryLabel = new Label("Total Logins: " + totalConnections);
        summaryLabel.setStyle("-fx-font-style: italic; -fx-padding: 10 0 0 0;");
        activeUsersContainer.getChildren().add(summaryLabel);
    }
}