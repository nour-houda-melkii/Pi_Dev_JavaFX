package controllers;

import Services.DataPersistenceService;
import entities.Category;
import Services.CategoryServices;
import Services.AIDescriptionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AddCategoryController {
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private Button generateDescriptionButton;

    private final CategoryServices categoryService = new CategoryServices();
    private final AIDescriptionService aiService = new AIDescriptionService();
    private DataPersistenceService dataPersistence = new DataPersistenceService();


    @FXML
    public void initialize() {
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            generateDescriptionButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });

        generateDescriptionButton.setDisable(true);
    }

    @FXML
    private void generateAIDescription() {
        String categoryName = nameField.getText().trim();
        if (!categoryName.isEmpty()) {
            descriptionField.setText("Generating description...");


            new Thread(() -> {
                String generatedDescription = aiService.generateCategoryDescription(categoryName);

                javafx.application.Platform.runLater(() -> {
                    descriptionField.setText(generatedDescription);
                });
            }).start();
        }
    }

    @FXML
    private void addCategory() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();

        if (name.isEmpty()) {
            showAlert("Input Error", "Category name cannot be empty");
            return;
        }

        if (name.length() < 5) {
            showAlert("Input Error", "Category name must be at least 5 characters long");
            return;
        }

        if (name.length() > 100) {
            showAlert("Input Error", "Category name is too long (max 100 characters)");
            return;
        }

        try {
            Category category = new Category(name, description);
            categoryService.insert(category);

            // Record the addition in history
            dataPersistence.recordCategoryAdded(category);

            clearFields();
            showSuccessAlert("Category Added", "Category has been successfully added!");
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists")) {
                showAlert("Duplicate Error", e.getMessage());
            } else {
                showAlert("Database Error", "Failed to add category: " + e.getMessage());
            }
        }
    }

    @FXML
    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
    }

    @FXML
    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/admin_dashboard.fxml"));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}