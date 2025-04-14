package controllers;

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

public class EditCategoryController {
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private Button generateDescriptionButton;

    private final CategoryServices categoryService = new CategoryServices();
    private final AIDescriptionService aiService = new AIDescriptionService();
    private Category categoryToEdit;

    @FXML
    public void initialize() {
        // Add listener to name field to enable generate button only when name is entered
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            generateDescriptionButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
    }

    public void setCategoryToEdit(Category category) {
        this.categoryToEdit = category;
        populateFields();
    }

    private void populateFields() {
        if (categoryToEdit != null) {
            nameField.setText(categoryToEdit.getName());
            descriptionField.setText(categoryToEdit.getDescription());

            // Enable or disable the generate button based on whether the name is filled
            generateDescriptionButton.setDisable(nameField.getText() == null ||
                    nameField.getText().trim().isEmpty());
        }
    }

    @FXML
    private void generateAIDescription() {
        String categoryName = nameField.getText().trim();
        if (!categoryName.isEmpty()) {
            // Show loading indicator
            descriptionField.setText("Generating description...");

            // In a real application, you would want to do this in a background thread
            // to avoid freezing the UI during API calls
            new Thread(() -> {
                String generatedDescription = aiService.generateCategoryDescription(categoryName);

                // Update UI on the JavaFX application thread
                javafx.application.Platform.runLater(() -> {
                    descriptionField.setText(generatedDescription);
                });
            }).start();
        }
    }

    @FXML
    private void updateCategory() {
        String newName = nameField.getText().trim();
        String newDescription = descriptionField.getText().trim();

        if (newName.isEmpty()) {
            showAlert("Input Error", "Category name cannot be empty");
            return;
        }

        // Check category name length - minimum 5 characters
        if (newName.length() < 5) {
            showAlert("Input Error", "Category name must be at least 5 characters long");
            return;
        }

        if (newName.length() > 100) {
            showAlert("Input Error", "Category name is too long (max 100 characters)");
            return;
        }

        try {
            categoryToEdit.setName(newName);
            categoryToEdit.setDescription(newDescription);
            categoryService.update(categoryToEdit);
            showSuccessAlert("Category Updated", "Category has been successfully updated!");
            navigateBack();
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists")) {
                showAlert("Duplicate Error", e.getMessage());
            } else {
                showAlert("Database Error", "Failed to update category: " + e.getMessage());
            }
        }
    }

    @FXML
    private void navigateBack() {
        try {
            // Load the category management FXML file
            Parent root = FXMLLoader.load(getClass().getResource("/view_categories.fxml"));

            // Get the current stage
            Stage stage = (Stage) nameField.getScene().getWindow();

            // Set the new scene
            stage.setScene(new Scene(root));
            stage.setTitle("Category Management");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load category management: " + e.getMessage());
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