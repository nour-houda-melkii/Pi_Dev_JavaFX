package com.controllers.nour;

import com.models.Produit;
import com.services.FavoriServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TopFavoritesStatisticsController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(TopFavoritesStatisticsController.class.getName());

    @FXML
    private VBox chartContainer;

    @FXML
    private Label mostPopularLabel;

    @FXML
    private Label avgFavoritesLabel;

    @FXML
    private Label comparisonLabel;

    @FXML
    private VBox summaryContainer;

    private FavoriServices favoriService;

    @FXML
    private void handleBackToDashboard() {
        handleBackToFavorites();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        favoriService = new FavoriServices();
        createFavoritesChart();
        updateSummarySection();
    }

    private void createFavoritesChart() {
        try {
            // Get most favorited products
            Map<Produit, Integer> allFavoritedProducts = favoriService.getMostFavoritedProducts();

            // Convert to list for easier processing
            List<Map.Entry<Produit, Integer>> productsList = new ArrayList<>(allFavoritedProducts.entrySet());

            // Take only top 3 (or less if there are fewer products)
            int topCount = Math.min(3, productsList.size());
            List<Map.Entry<Produit, Integer>> top3Products = productsList.subList(0, topCount);

            // Create axes
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Products");
            yAxis.setLabel("Favorite Count");

            // Create the chart
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setTitle("Top 3 Most Favorited Products");

            // Create a data series
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Favorite Count");

            // Add data points
            for (Map.Entry<Produit, Integer> entry : top3Products) {
                Produit product = entry.getKey();
                int favoriteCount = entry.getValue();
                series.getData().add(new XYChart.Data<>(product.getName(), favoriteCount));
            }

            // Add series to chart
            barChart.getData().add(series);

            // Style the bars with different colors
            String[] colors = {"gold", "silver", "#cd7f32"};
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                String color = colors[Math.min(i, colors.length - 1)];
                data.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }

            // Add chart to container
            chartContainer.getChildren().add(barChart);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating favorites chart", e);
            showAlert("Error", "Failed to create chart: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Get the top 3 most favorited products
     * @return List of the top 3 products with their favorite counts
     */
    public List<Map.Entry<String, Integer>> getTop3FavoriteProducts() {
        try {
            Map<Produit, Integer> allFavoritedProducts = favoriService.getMostFavoritedProducts();

            // Convert to map of product name -> favorite count for simpler return
            Map<String, Integer> productNameToCount = new LinkedHashMap<>();
            for (Map.Entry<Produit, Integer> entry : allFavoritedProducts.entrySet()) {
                productNameToCount.put(entry.getKey().getName(), entry.getValue());
            }

            // Take only top 3
            return productNameToCount.entrySet().stream()
                    .limit(3)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting top 3 favorite products", e);
            return new ArrayList<>();
        }
    }

    /**
     * Update the summary section with calculated statistics
     */
    private void updateSummarySection() {
        try {
            Map<Produit, Integer> allFavoritedProducts = favoriService.getMostFavoritedProducts();
            List<Map.Entry<Produit, Integer>> productsList = new ArrayList<>(allFavoritedProducts.entrySet());

            if (productsList.isEmpty()) {
                mostPopularLabel.setText("Most Popular Product: None (No favorites yet)");
                avgFavoritesLabel.setText("Average Favorites for Top 3: N/A");
                comparisonLabel.setText("Popularity Comparison: N/A");
                return;
            }

            // Most popular product
            Map.Entry<Produit, Integer> mostPopular = productsList.get(0);
            mostPopularLabel.setText(String.format("Most Popular Product: %s (%d favorites)",
                    mostPopular.getKey().getName(), mostPopular.getValue()));

            // Average favorites for top 3
            int topCount = Math.min(3, productsList.size());
            double avgFavorites = productsList.subList(0, topCount).stream()
                    .mapToInt(Map.Entry::getValue)
                    .average()
                    .orElse(0);
            avgFavoritesLabel.setText(String.format("Average Favorites for Top 3: %.1f", avgFavorites));

            // Comparison between most popular and least popular
            if (productsList.size() >= 2) {
                Map.Entry<Produit, Integer> leastPopular = productsList.get(productsList.size() - 1);
                int difference = mostPopular.getValue() - leastPopular.getValue();
                double ratio = (double) mostPopular.getValue() / Math.max(1, leastPopular.getValue());

                comparisonLabel.setText(String.format(
                        "Popularity Comparison: Most popular has %d more favorites (%.1fx) than least popular",
                        difference, ratio));
            } else {
                comparisonLabel.setText("Popularity Comparison: Only one product with favorites");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating summary section", e);
            mostPopularLabel.setText("Most Popular Product: Error loading data");
            avgFavoritesLabel.setText("Average Favorites: Error loading data");
            comparisonLabel.setText("Popularity Comparison: Error loading data");
        }
    }

    @FXML
    private void handleBackToFavorites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/admin_dashboard.fxml"));
            Parent root = loader.load();

            // Get current stage and set the new scene
            Stage stage = (Stage) chartContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Favorites");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to favorites view", e);
            showAlert("Navigation Error", "Failed to navigate back to favorites: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        createFavoritesChart();
        updateSummarySection();
        showAlert("Success", "Statistics refreshed successfully!");
    }
}