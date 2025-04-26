package com.controllers.nour;

import com.models.*;
import com.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FavoritesStatisticsController {
    @FXML
    private BarChart<String, Number> favoritesChart;

    @FXML
    private PieChart categoriesChart;

    @FXML
    private Label discountInfoLabel;

    // Colors for the bar chart
    private final String[] colors = {
            "#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6",
            "#1abc9c", "#e67e22", "#34495e", "#d35400", "#27ae60"
    };

    public void initialize() {
        // Load statistics when the view initializes
        loadStatistics();
    }

    public void loadStatistics() {
        try {
            // Load favorite product statistics
            loadProductFavoritesStatistics();

            // Load category statistics
            loadCategoryStatistics();

            // Display discount information
            discountInfoLabel.setText("Top favorited products automatically get 5% discount in client view");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error loading statistics: " + e.getMessage());
        }
    }

    private void loadProductFavoritesStatistics() throws SQLException {
        FavoriServices favoriService = new FavoriServices();
        Map<Produit, Integer> favoritesStats = favoriService.getMostFavoritedProducts();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Favorite Count");

        int colorIndex = 0;
        for (Map.Entry<Produit, Integer> entry : favoritesStats.entrySet()) {
            Produit produit = entry.getKey();
            int count = entry.getValue();

            XYChart.Data<String, Number> data = new XYChart.Data<>(produit.getName(), count);
            series.getData().add(data);

            // Increase color index and reset if needed
            colorIndex = (colorIndex + 1) % colors.length;
        }

        favoritesChart.getData().clear();
        favoritesChart.getData().add(series);

        // Apply colors to bars after they're rendered
        applyColorsToBarChart(series);
    }

    private void loadCategoryStatistics() throws SQLException {
        FavoriServices favoriService = new FavoriServices();
        Map<Produit, Integer> favoritesStats = favoriService.getMostFavoritedProducts();

        // Map to store category favorites count
        Map<Integer, Integer> categoryCountMap = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();

        // Calculate favorites by category
        for (Map.Entry<Produit, Integer> entry : favoritesStats.entrySet()) {
            Produit produit = entry.getKey();
            int count = entry.getValue();
            int categoryId = produit.getCategoryId();

            // Get category name
            String categoryName = getCategoryName(categoryId);
            categoryNames.put(categoryId, categoryName);

            // Accumulate counts
            categoryCountMap.put(categoryId,
                    categoryCountMap.getOrDefault(categoryId, 0) + count);
        }

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<Integer, Integer> entry : categoryCountMap.entrySet()) {
            int categoryId = entry.getKey();
            int count = entry.getValue();
            String categoryName = categoryNames.getOrDefault(categoryId, "Category " + categoryId);

            PieChart.Data slice = new PieChart.Data(categoryName + " (" + count + ")", count);
            pieChartData.add(slice);
        }

        categoriesChart.setData(pieChartData);

        // Apply colors to pie chart
        applyColorsToPieChart(pieChartData);
    }

    private String getCategoryName(int categoryId) {
        // Placeholder implementation - should be replaced with actual DB lookup
        return "Category " + categoryId;
    }

    private void applyColorsToBarChart(XYChart.Series<String, Number> series) {
        for (int i = 0; i < series.getData().size(); i++) {
            final String color = colors[i % colors.length];
            XYChart.Data<String, Number> data = series.getData().get(i);

            // Need to apply the CSS after the node is created
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + color + ";");
                }
            });
        }
    }

    private void applyColorsToPieChart(ObservableList<PieChart.Data> pieChartData) {
        for (int i = 0; i < pieChartData.size(); i++) {
            final int colorIndex = i;
            PieChart.Data slice = pieChartData.get(i);

            // Apply styles to each slice when the node is available
            if (slice.getNode() != null) {
                slice.getNode().setStyle("-fx-pie-color: " + colors[colorIndex % colors.length] + ";");
            } else {
                // For nodes that might not be created yet
                pieChartData.get(i).nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-pie-color: " + colors[colorIndex % colors.length] + ";");
                    }
                });
            }
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) favoritesChart.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error returning to dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}