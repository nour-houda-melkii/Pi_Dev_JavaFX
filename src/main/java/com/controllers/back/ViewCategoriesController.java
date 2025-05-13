package com.controllers.back;

import com.models.PostCategory;
import com.services.PostCategoryService;
import com.services.PostService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewCategoriesController {
    @FXML private VBox categoriesList;
    @FXML private Label categoryCountLabel;  // New label for total count
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    private final PostCategoryService categoryService = new PostCategoryService();
    private final PostService postService = new PostService();  // New service for post counts
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private List<PostCategory> allCategories;

    @FXML
    public void initialize() {
        setupSearchAndFilter();
        loadCategories();
    }

    private void setupSearchAndFilter() {
        // Initialize filter options
        filterComboBox.setItems(FXCollections.observableArrayList(
                "All Categories",
                "Most Posts",
                "Fewest Posts",
                "Newest First",
                "Oldest First"
        ));
        filterComboBox.getSelectionModel().selectFirst();

        // Set up listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCategories());
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> filterCategories());
    }

    private void loadCategories() {
        try {
            allCategories = categoryService.getAll();
            filterCategories();
        } catch (Exception e) {
            showMessage("Error loading categories: " + e.getMessage(), "error-message");
            e.printStackTrace();
        }
    }

    private void filterCategories() {
        List<PostCategory> filtered = allCategories;

        // Apply search filter
        String searchText = searchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            filtered = filtered.stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchText) ||
                            c.getDescription().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        String filter = filterComboBox.getValue();
        if (filter != null) {
            switch (filter) {
                case "Most Posts":
                    filtered.sort(Comparator.comparingInt(
                            c -> -postService.getPostCountByCategory(c.getId())));
                    break;
                case "Fewest Posts":
                    filtered.sort(Comparator.comparingInt(
                            c -> postService.getPostCountByCategory(c.getId())));
                    break;
                case "Newest First":
                    filtered.sort(Comparator.comparing(PostCategory::getCreatedAt).reversed());
                    break;
                case "Oldest First":
                    filtered.sort(Comparator.comparing(PostCategory::getCreatedAt));
                    break;
            }
        }

        displayCategories(filtered);
    }

    private void displayCategories(List<PostCategory> categories) {
        categoriesList.getChildren().clear();
        categoryCountLabel.setText(categories.size() + " categories");
        categoryCountLabel.getStyleClass().add("count-label");

        if (categories.isEmpty()) {
            showMessage("No categories found matching your criteria", "empty-message");
            return;
        }

        for (PostCategory category : categories) {
            int postCount = postService.getPostCountByCategory(category.getId());
            categoriesList.getChildren().add(createCategoryCard(category, postCount));
        }
    }

    private VBox createCategoryCard(PostCategory category, int postCount) {
        VBox card = new VBox();
        card.getStyleClass().add("category-card");

        // Top row with name and post count
        HBox topRow = new HBox(10);
        topRow.getStyleClass().add("category-header");

        Label nameLabel = new Label(category.getName());
        nameLabel.getStyleClass().add("category-name");

        Label countLabel = new Label(postCount + (postCount == 1 ? " post" : " posts"));
        countLabel.getStyleClass().add("post-count");

        topRow.getChildren().addAll(nameLabel, countLabel);

        // Description
        TextFlow descriptionFlow = new TextFlow(new Text(category.getDescription()));
        descriptionFlow.getStyleClass().add("category-description");

        // Bottom row with date and buttons
        HBox bottomRow = createBottomRow(category);

        card.getChildren().addAll(topRow, descriptionFlow, bottomRow);
        return card;
    }

    private HBox createBottomRow(PostCategory category) {
        HBox row = new HBox(10);
        row.getStyleClass().add("category-bottom-row");

        Label dateLabel = new Label("Created: " + category.getCreatedAt().format(dateFormatter));
        dateLabel.getStyleClass().add("category-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(
                createButton("Edit", "edit-btn", () -> handleEditCategory(category)),
                createButton("Delete", "delete-btn", () -> handleDeleteCategory(category))
        );

        row.getChildren().addAll(dateLabel, spacer, buttonBox);
        return row;
    }

    /**
     * Helper method to create styled buttons consistently.
     * @param text The text to display on the button
     * @param styleClass The CSS class to apply for styling
     * @param action The action to perform when the button is clicked
     * @return A fully configured Button instance
     */
    private Button createButton(String text, String styleClass, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);  // Apply styling

        // Set up the click handler - converts ActionEvent to parameterless Runnable
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * Displays a message in the categories list area.
     * Used for showing status messages like errors or empty states.
     * @param text The message text to display
     * @param styleClass The CSS class to apply for styling
     */
    private void showMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        categoriesList.getChildren().add(label);
    }

    private void handleEditCategory(PostCategory category) {
        try {
            // Load the edit dialog FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Back/EditCategory.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the category to edit
            EditCategoryController controller = loader.getController();
            controller.setCategoryToEdit(category);

            // Create and show the dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit Category");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            // Refresh after editing
            loadCategories();

        } catch (IOException e) {
            showAlert("Error", "Could not load edit dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Utility method to show alerts
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleDeleteCategory(PostCategory category) {
        // First check if category has posts
        int postCount = postService.getPostCountByCategory(category.getId());
        if (postCount > 0) {
            showAlert("Cannot Delete",
                    "This category contains " + postCount + " posts. Please remove or reassign them first.",
                    Alert.AlertType.WARNING);
            return;
        }

        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Category");
        confirmation.setContentText("Are you sure you want to delete '" + category.getName() + "'?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categoryService.delete(category);
                showMessage("Category deleted successfully!", "success-message");
                loadCategories(); // Refresh the list
            } catch (Exception e) {
                showAlert("Error", "Failed to delete category: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }


    /**
     * Handles the refresh action triggered by the UI button.
     * Simply reloads all categories from the database.
     */
    @FXML
    private void handleRefresh() {
        loadCategories();  // Re-fetch and re-display all categories
    }

}