package controllers;

import entities.Category;
import Services.CategoryServices;
import Services.ProduitServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ViewCategoryController {
    @FXML private ListView<Category> categoryListView;
    @FXML private Label categoryCountLabel;

    private final CategoryServices categoryService = new CategoryServices();
    private final ProduitServices produitService = new ProduitServices();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up custom cell factory for the ListView to display categories as cards with thick borders
        categoryListView.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);

                if (empty || category == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create card container with THICK BORDERS
                    VBox cardContainer = new VBox();
                    cardContainer.getStyleClass().add("category-card");
                    cardContainer.setStyle("-fx-background-color: white; " +
                            "-fx-border-color: #33ccff; " + // Border color matching the back button
                            "-fx-border-width: 3; " + // THICK border
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 3); " +
                            "-fx-padding: 18; " +
                            "-fx-spacing: 12;");

                    // Header with category name
                    Label nameLabel = new Label(category.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #333333;");

                    // Description
                    Label descLabel = new Label(category.getDescription());
                    descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
                    descLabel.setWrapText(true);
                    descLabel.setMaxWidth(600);

                    // Product count
                    int productCount = 0;
                    try {
                        List<?> products = produitService.getByCategory(category.getId());
                        productCount = products.size();
                    } catch (SQLException e) {
                        System.err.println("Error getting product count: " + e.getMessage());
                    }

                    HBox productCountBox = new HBox();
                    productCountBox.setAlignment(Pos.CENTER_LEFT);
                    productCountBox.setSpacing(5);
                    productCountBox.setPadding(new Insets(5, 0, 5, 0));

                    // Product count section with enhanced styling
                    Label productIcon = new Label("📦");
                    productIcon.setStyle("-fx-font-size: 18px;");

                    Label productCountLabel = new Label(productCount + " Products");
                    productCountLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #33ccff; -fx-font-weight: bold;");

                    productCountBox.getChildren().addAll(productIcon, productCountLabel);

                    // Create a spacer
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Action buttons with consistent styling
                    Button editButton = new Button("Edit");
                    editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                            "-fx-background-radius: 4; -fx-padding: 8 15; -fx-font-weight: bold;");
                    editButton.setOnAction(event -> navigateToEditCategory(category));

                    Button deleteButton = new Button("Delete");
                    deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-background-radius: 4; -fx-padding: 8 15; -fx-font-weight: bold;");
                    deleteButton.setOnAction(event -> confirmDeleteCategory(category));

                    HBox buttonBox = new HBox(10, editButton, deleteButton);
                    buttonBox.setAlignment(Pos.CENTER_RIGHT);

                    // Add separator above the footer for visual separation
                    Separator separator = new Separator();
                    separator.setStyle("-fx-background-color: #e0e0e0;");
                    separator.setPadding(new Insets(5, 0, 5, 0));

                    // Footer with buttons and product count
                    HBox footer = new HBox();
                    footer.setAlignment(Pos.CENTER_LEFT);
                    footer.getChildren().addAll(productCountBox, spacer, buttonBox);

                    // Add all elements to card
                    cardContainer.getChildren().addAll(nameLabel, descLabel, separator, footer);

                    // Set padding between cards
                    setPadding(new Insets(8, 0, 8, 0));

                    setGraphic(cardContainer);
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

        // Update category count label
        updateCategoryCountLabel();
    }

    private void updateCategoryCountLabel() {
        int count = categories.size();
        categoryCountLabel.setText("Showing " + count + " " + (count == 1 ? "category" : "categories"));
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