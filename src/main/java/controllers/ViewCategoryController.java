package controllers;

import entities.Category;
import Services.CategoryServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class ViewCategoryController {
    @FXML private ListView<Category> categoryListView;

    private final CategoryServices categoryService = new CategoryServices();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up custom cell factory for the ListView
        categoryListView.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);

                if (empty || category == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create components for each row
                    Label nameLabel = new Label(category.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    Label descLabel = new Label(category.getDescription());
                    descLabel.setMaxWidth(300);
                    descLabel.setWrapText(true);

                    // Create a spacer
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Create buttons
                    Button editButton = new Button("Edit");
                    editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    editButton.setOnAction(event -> navigateToEditCategory(category));

                    Button deleteButton = new Button("Delete");
                    deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    deleteButton.setOnAction(event -> confirmDeleteCategory(category));

                    // Create details VBox
                    VBox detailsBox = new VBox(5, nameLabel, descLabel);

                    // Create buttons HBox
                    HBox buttonsBox = new HBox(10, editButton, deleteButton);

                    // Main HBox for the cell
                    HBox cellLayout = new HBox(15, detailsBox, spacer, buttonsBox);
                    cellLayout.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

                    setGraphic(cellLayout);
                }
            }
        });

        // Load data
        try {
            loadCategories();
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void loadCategories() throws SQLException {
        categories.setAll(categoryService.showAll());
        categoryListView.setItems(categories);
    }

    private void confirmDeleteCategory(Category category) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Category");
        confirmDialog.setContentText("Are you sure you want to delete category: " + category.getName() + "?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    categoryService.delete(category);
                    loadCategories();
                    showSuccessAlert("Category Deleted", "Category has been successfully deleted!");
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to delete category: " + e.getMessage());
                }
            }
        });
    }

    private void navigateToEditCategory(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edit_category.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the category to edit
            EditCategoryController controller = loader.getController();
            controller.setCategoryToEdit(category);

            // Get the current stage
            Stage stage = (Stage) categoryListView.getScene().getWindow();

            // Set the new scene
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Category");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load edit category page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/admin_dashboard.fxml"));
            Stage stage = (Stage) categoryListView.getScene().getWindow();
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