package controller;

import com.models.Reclamation;
import com.models.TypeReclamation;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import services.ReclamationServices;
import services.TypeReclamationService;
import utils.StatisticsExporter;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsController {
    @FXML
    private PieChart typePieChart;
    @FXML
    private BarChart<String, Number> monthlyBarChart;
    @FXML
    private BarChart<String, Number> typeBarChart;
    @FXML
    private ComboBox<Integer> yearComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Button applyFilterButton;
    @FXML
    private Button exportPdfButton;
    @FXML
    private TabPane statisticsTabs;
    @FXML
    private Label totalReclamationsLabel;
    @FXML
    private VBox statsContainer;
    @FXML
    private Label mostFrequentTypeLabel;
    @FXML
    private Label mostActiveMonthLabel;
    @FXML
    private Label monthlyGrowthLabel;

    @FXML
    private TableView<TypeStatistics> statsTableView;
    @FXML
    private TableColumn<TypeStatistics, String> typeColumn;
    @FXML
    private TableColumn<TypeStatistics, Integer> countColumn;
    @FXML
    private TableColumn<TypeStatistics, Double> percentColumn;
    @FXML
    private TableColumn<TypeStatistics, String> trendColumn;

    private final ReclamationServices reclamationService = new ReclamationServices();
    private final TypeReclamationService typeService = new TypeReclamationService();

    // Classe interne pour les statistiques de type
    public static class TypeStatistics {
        private final SimpleStringProperty type;
        private final SimpleIntegerProperty count;
        private final SimpleDoubleProperty percent;
        private final SimpleStringProperty trend;

        public TypeStatistics(String type, int count, double percent, String trend) {
            this.type = new SimpleStringProperty(type);
            this.count = new SimpleIntegerProperty(count);
            this.percent = new SimpleDoubleProperty(percent);
            this.trend = new SimpleStringProperty(trend);
        }

        public String getType() {
            return type.get();
        }

        public int getCount() {
            return count.get();
        }

        public double getPercent() {
            return percent.get();
        }

        public String getTrend() {
            return trend.get();
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public SimpleIntegerProperty countProperty() {
            return count;
        }

        public SimpleDoubleProperty percentProperty() {
            return percent;
        }

        public SimpleStringProperty trendProperty() {
            return trend;
        }
    }

    @FXML
    public void initialize() {
        setupDateControls();
        setupYearComboBox();
        setupTableView();

        // Configurer le bouton d'application du filtre
        if (applyFilterButton != null) {
            applyFilterButton.setOnAction(e -> loadStatistics());
        } else {
            System.err.println("ERREUR: applyFilterButton est null dans StatisticsController");
        }

        // Configurer le bouton d'exportation PDF
        if (exportPdfButton != null) {
            exportPdfButton.setOnAction(e -> exportStatisticsToPdf());
        } else {
            System.err.println("ERREUR: exportPdfButton est null dans StatisticsController");
        }

        // Charger les statistiques initiales
        loadStatistics();
    }

    private void setupTableView() {
        if (statsTableView == null) {
            System.err.println("ERREUR: statsTableView est null dans setupTableView");
            return;
        }

        // Configurer les colonnes
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));

        percentColumn.setCellValueFactory(new PropertyValueFactory<>("percent"));
        percentColumn.setCellFactory(column -> new javafx.scene.control.TableCell<TypeStatistics, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", item));
                }
            }
        });

        trendColumn.setCellValueFactory(new PropertyValueFactory<>("trend"));
        trendColumn.setCellFactory(column -> new javafx.scene.control.TableCell<TypeStatistics, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    if (item.contains("↑")) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else if (item.contains("↓")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupDateControls() {
        // Initialiser avec la date du jour et 30 jours avant
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        startDatePicker.setValue(monthAgo);
        endDatePicker.setValue(today);
    }

    private void setupYearComboBox() {
        // Remplir le ComboBox avec les années disponibles (de l'année actuelle à 3 ans en arrière)
        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList();

        for (int i = 0; i < 4; i++) {
            years.add(currentYear - i);
        }

        yearComboBox.setItems(years);
        yearComboBox.setValue(currentYear); // Année actuelle par défaut
    }

    @FXML
    public void loadStatistics() {
        try {
            List<Reclamation> allReclamations = reclamationService.getAllReclamationsWithNames();
            List<TypeReclamation> allTypes = typeService.getAllTypes();

            // Filtrer les données en fonction des dates sélectionnées
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate != null && endDate != null) {
                allReclamations = allReclamations.stream()
                        .filter(r -> {
                            try {
                                LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                return !date.isBefore(startDate) && !date.isAfter(endDate);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }

            // Mettre à jour le nombre total
            totalReclamationsLabel.setText("Total des réclamations: " + allReclamations.size());

            // Charger les différents graphiques (sans typeBarChart)
            loadPieChartData(allReclamations, allTypes);
            // loadTypeBarChartData(allReclamations, allTypes); // Commentez ou supprimez cette ligne
            loadMonthlyBarChartData(allReclamations);

            // Charger les statistiques avancées
            loadAdvancedStatistics(allReclamations, allTypes);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur de chargement des données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadPieChartData(List<Reclamation> reclamations, List<TypeReclamation> types) {
        // Créer un Map pour compter les réclamations par type
        Map<String, Integer> typeCounts = new HashMap<>();

        // Initialiser tous les types à 0
        for (TypeReclamation type : types) {
            typeCounts.put(type.getNom(), 0);
        }

        // Compter les réclamations par type
        for (Reclamation reclamation : reclamations) {
            String typeName = reclamation.getTypeReclamationName();
            typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
        }

        // Créer les données pour le PieChart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            if (entry.getValue() > 0) { // Ajouter seulement les types qui ont des réclamations
                pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }

        typePieChart.setData(pieChartData);
        typePieChart.setTitle("Répartition des réclamations par type");

        // Ajouter des tooltips pour plus de détails
        for (PieChart.Data data : typePieChart.getData()) {
            String percentage = String.format("%.1f%%",
                    (reclamations.isEmpty() ? 0 : (data.getPieValue() / reclamations.size() * 100)));

            // Créer un tooltips pour chaque section
            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    data.getName() + "\n" +
                            "Nombre: " + (int) data.getPieValue() + "\n" +
                            "Pourcentage: " + percentage
            );

            javafx.scene.Node node = data.getNode();
            if (node != null) {
                javafx.scene.control.Tooltip.install(node, tooltip);
            }
        }
    }

    private void loadTypeBarChartData(List<Reclamation> reclamations, List<TypeReclamation> types) {
        // Créer un Map pour compter les réclamations par type
        Map<String, Integer> typeCounts = new HashMap<>();

        // Initialiser tous les types à 0
        for (TypeReclamation type : types) {
            typeCounts.put(type.getNom(), 0);
        }

        // Compter les réclamations par type
        for (Reclamation reclamation : reclamations) {
            String typeName = reclamation.getTypeReclamationName();
            typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
        }

        // Créer les données pour le BarChart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de réclamations");

        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        typeBarChart.getData().clear();
        typeBarChart.getData().add(series);
        typeBarChart.setTitle("Réclamations par type");
    }

    private void loadMonthlyBarChartData(List<Reclamation> reclamations) {
        int selectedYear = yearComboBox.getValue();

        // Créer un tableau pour compter les réclamations par mois
        int[] monthlyCounts = new int[12];

        // Compter les réclamations par mois pour l'année sélectionnée
        for (Reclamation reclamation : reclamations) {
            try {
                LocalDate date = LocalDate.parse(reclamation.getFormattedDate(),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                if (date.getYear() == selectedYear) {
                    monthlyCounts[date.getMonthValue() - 1]++;
                }
            } catch (Exception e) {
                // Ignorer les dates mal formatées
                System.err.println("Erreur de format de date: " + reclamation.getFormattedDate());
            }
        }

        // Créer les données pour le BarChart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Réclamations en " + selectedYear);

        String[] monthNames = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

        for (int i = 0; i < 12; i++) {
            series.getData().add(new XYChart.Data<>(monthNames[i], monthlyCounts[i]));
        }

        monthlyBarChart.getData().clear();
        monthlyBarChart.getData().add(series);
        monthlyBarChart.setTitle("Réclamations par mois en " + selectedYear);
    }

    // Modification de la méthode loadAdvancedStatistics pour gérer l'exception SQLException

    private void loadAdvancedStatistics(List<Reclamation> reclamations, List<TypeReclamation> types) {
        if (mostFrequentTypeLabel == null || mostActiveMonthLabel == null || monthlyGrowthLabel == null || statsTableView == null) {
            System.err.println("ERREUR: Un ou plusieurs éléments sont null dans loadAdvancedStatistics");
            return;
        }

        if (reclamations.isEmpty()) {
            mostFrequentTypeLabel.setText("Aucune donnée");
            mostActiveMonthLabel.setText("Aucune donnée");
            monthlyGrowthLabel.setText("Aucune donnée");
            statsTableView.getItems().clear();
            return;
        }

        // 1. Trouver le type le plus fréquent
        Map<String, Long> typeCount = reclamations.stream()
                .collect(Collectors.groupingBy(Reclamation::getTypeReclamationName, Collectors.counting()));

        String mostFrequentType = typeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Inconnu");

        long mostFrequentTypeCount = typeCount.getOrDefault(mostFrequentType, 0L);
        double percentage = (double) mostFrequentTypeCount / reclamations.size() * 100;

        mostFrequentTypeLabel.setText(mostFrequentType + " (" +
                mostFrequentTypeCount + " - " +
                String.format("%.1f", percentage) + "%)");

        // 2. Trouver le mois le plus actif
        Map<String, Long> monthCount = new HashMap<>();
        String[] monthNames = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

        for (Reclamation reclamation : reclamations) {
            try {
                LocalDate date = LocalDate.parse(reclamation.getFormattedDate(),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String monthName = monthNames[date.getMonthValue() - 1];
                monthCount.put(monthName, monthCount.getOrDefault(monthName, 0L) + 1);
            } catch (Exception e) {
                // Ignorer les dates mal formatées
            }
        }

        String mostActiveMonth = monthCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Inconnu");

        long mostActiveMonthCount = monthCount.getOrDefault(mostActiveMonth, 0L);
        percentage = (double) mostActiveMonthCount / reclamations.size() * 100;

        mostActiveMonthLabel.setText(mostActiveMonth + " (" +
                mostActiveMonthCount + " - " +
                String.format("%.1f", percentage) + "%)");

        // 3. Calculer la croissance mensuelle
        // Comparer le mois en cours avec le mois précédent
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int previousMonth = currentMonth > 1 ? currentMonth - 1 : 12;
        int currentYear = now.getYear();
        int previousYear = previousMonth == 12 ? currentYear - 1 : currentYear;

        long currentMonthCount = reclamations.stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        return date.getMonthValue() == currentMonth && date.getYear() == currentYear;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        long previousMonthCount = reclamations.stream()
                .filter(r -> {
                    try {
                        LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        return date.getMonthValue() == previousMonth && date.getYear() == previousYear;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        if (previousMonthCount == 0) {
            monthlyGrowthLabel.setText("N/A (pas de données pour le mois précédent)");
        } else {
            double growthPercentage = ((double) currentMonthCount - previousMonthCount) / previousMonthCount * 100;
            String trend = growthPercentage > 0 ? "↑" : (growthPercentage < 0 ? "↓" : "→");
            String color = growthPercentage > 0 ? "green" : (growthPercentage < 0 ? "red" : "black");

            monthlyGrowthLabel.setText(trend + " " + String.format("%.1f", Math.abs(growthPercentage)) + "%");
            monthlyGrowthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }

        // 4. Remplir le tableau des statistiques
        ObservableList<TypeStatistics> tableData = FXCollections.observableArrayList();

        // Calculer les tendances - comparer avec les 30 derniers jours précédents
        LocalDate thirtyDaysAgo = startDatePicker.getValue();
        LocalDate sixtyDaysAgo = thirtyDaysAgo.minusDays(30);

        // Obtenir les réclamations pour la période précédente (pour comparer)
        List<Reclamation> previousPeriodReclamations;
        try {
            previousPeriodReclamations = reclamationService.getAllReclamationsWithNames().stream()
                    .filter(r -> {
                        try {
                            LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            return !date.isBefore(sixtyDaysAgo) && date.isBefore(thirtyDaysAgo);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des données précédentes: " + e.getMessage());
            e.printStackTrace();
            previousPeriodReclamations = new ArrayList<>(); // Liste vide en cas d'erreur
        }

        // Compter par type pour la période précédente
        Map<String, Long> previousTypeCount = previousPeriodReclamations.stream()
                .collect(Collectors.groupingBy(Reclamation::getTypeReclamationName, Collectors.counting()));

        // Pour chaque type, calculer les statistiques et la tendance
        for (TypeReclamation type : types) {
            String typeName = type.getNom();
            int count = typeCount.getOrDefault(typeName, 0L).intValue();
            double typePercentage = reclamations.isEmpty() ? 0 : (count * 100.0 / reclamations.size());

            // Calculer la tendance
            long previousCount = previousTypeCount.getOrDefault(typeName, 0L);
            String trend;

            if (previousCount == 0) {
                if (count == 0) {
                    trend = "→ Stable (0)";
                } else {
                    trend = "↑ Nouveau";
                }
            } else {
                double trendPercentage = ((double)count - previousCount) / previousCount * 100;
                if (Math.abs(trendPercentage) < 1) {
                    trend = "→ Stable";
                } else if (trendPercentage > 0) {
                    trend = "↑ +" + String.format("%.1f", trendPercentage) + "%";
                } else {
                    trend = "↓ " + String.format("%.1f", trendPercentage) + "%";
                }
            }

            tableData.add(new TypeStatistics(typeName, count, typePercentage, trend));
        }

        // Trier par nombre de réclamations (décroissant)
        tableData.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));

        // Mettre à jour le tableau
        statsTableView.setItems(tableData);
    }

    @FXML
    private void exportStatisticsToPdf() {
        try {
            // Ouvrir le dialogue de sauvegarde de fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le rapport statistique");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
            fileChooser.setInitialFileName("statistiques_reclamations.pdf");

            File file = fileChooser.showSaveDialog(typePieChart.getScene().getWindow());

            if (file != null) {
                // Récupérer toutes les données nécessaires
                List<Reclamation> reclamations = reclamationService.getAllReclamationsWithNames();
                List<TypeReclamation> types = typeService.getAllTypes();

                // Filtrer les données en fonction des dates sélectionnées
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                if (startDate != null && endDate != null) {
                    reclamations = reclamations.stream()
                            .filter(r -> {
                                try {
                                    LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                            DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                    return !date.isBefore(startDate) && !date.isAfter(endDate);
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());
                }

                // Exporter vers PDF (méthode texte uniquement)
                StatisticsExporter.exportTextPdf(file.getAbsolutePath(), reclamations, types);

                // Afficher une confirmation
                showAlert("Succès", "Le rapport statistique a été exporté avec succès.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export PDF: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }



    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}