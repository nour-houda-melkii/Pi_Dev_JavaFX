package controllers;

import entities.Category;
import Services.CategoryServices;
import Services.AIDescriptionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class CategoryController {
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private Button generateDescriptionButton;

    private final CategoryServices categoryService = new CategoryServices();
    private final AIDescriptionService aiService = new AIDescriptionService();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Load data
        try {
            loadCategories();
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load categories: " + e.getMessage());
        }

        // Set up selection listener
        categoryTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        nameField.setText(newSelection.getName());
                        descriptionField.setText(newSelection.getDescription());
                    }
                });

        // Add listener to name field to enable generate button only when name is entered
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            generateDescriptionButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });

        // Initially disable the generate button
        generateDescriptionButton.setDisable(true);
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
    private void navigateToDashboard() {
        try {
            // Load the admin dashboard FXML file from the correct path
            Parent root = FXMLLoader.load(getClass().getResource("/admin_dashboard.fxml"));

            // Get the current stage
            Stage stage = (Stage) categoryTable.getScene().getWindow();

            // Set the new scene
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load dashboard: " + e.getMessage());
            e.printStackTrace(); // This will help you debug if there are issues
        }
    }

    private void loadCategories() throws SQLException {
        categories.setAll(categoryService.showAll());
        categoryTable.setItems(categories);
    }

    @FXML
    private void addCategory() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();

        if (name.isEmpty()) {
            showAlert("Input Error", "Category name cannot be empty");
            return;
        }

        if (name.length() > 100) {
            showAlert("Input Error", "Category name is too long (max 100 characters)");
            return;
        }

        try {
            Category category = new Category(name, description);
            categoryService.insert(category);
            loadCategories();
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
    private void updateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a category to update");
            return;
        }

        String newName = nameField.getText().trim();
        String newDescription = descriptionField.getText().trim();

        if (newName.isEmpty()) {
            showAlert("Input Error", "Category name cannot be empty");
            return;
        }

        try {
            selected.setName(newName);
            selected.setDescription(newDescription);
            categoryService.update(selected);
            loadCategories();
            clearFields();
            showSuccessAlert("Category Updated", "Category has been successfully updated!");
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists")) {
                showAlert("Duplicate Error", e.getMessage());
            } else {
                showAlert("Database Error", "Failed to update category: " + e.getMessage());
            }
        }
    }
    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a category to delete");
            return;
        }

        try {
            categoryService.delete(selected);
            loadCategories();
            clearFields();
            showSuccessAlert("Category Deleted", "Category has been successfully deleted!");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete category: " + e.getMessage());
        }
    }

    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
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