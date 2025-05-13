package com.controllers.back;

import com.models.PostCategory;
import com.services.PostCategoryService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EditCategoryController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private Label errorLabel;  // Add this

    private PostCategory categoryToEdit;
    private final PostCategoryService categoryService = new PostCategoryService();

    public void setCategoryToEdit(PostCategory category) {
        this.categoryToEdit = category;
        // Populate fields with current values
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription());
    }

    @FXML
    private void handleSave() throws Exception {

        // Clear previous errors
        errorLabel.setVisible(false);
        nameField.setStyle("");
        descriptionField.setStyle("");

        String newName = nameField.getText().trim();
        String newDescription = descriptionField.getText().trim();

        // Validation
        if (newName.isEmpty()) {
            showError("Category name cannot be empty!");
            return;
        }

        if (newDescription.isEmpty()) {
            showError("Category description cannot be empty!");
            return;
        }

        // Check if name was changed and now conflicts with existing
        if (!newName.equals(categoryToEdit.getName()))
        {
            if (categoryService.categoryExists(newName)) {
                showError("Category name already exists!");
                nameField.requestFocus();
                return;
            }
        }

        // Update the category
        categoryToEdit.setName(newName);
        categoryToEdit.setDescription(newDescription);

        try {
            categoryService.update(categoryToEdit);
            closeWindow();
        } catch (Exception e) {
            showError("Failed to update category: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        // Highlight problematic fields
        if (message.contains("name")) {
            nameField.setStyle("-fx-border-color: #e74c3c;");
        } else {
            nameField.setStyle("");
        }

        if (message.contains("description")) {
            descriptionField.setStyle("-fx-border-color: #e74c3c;");
        } else {
            descriptionField.setStyle("");
        }

        // Auto-hide after 5 seconds
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(5));
        visiblePause.setOnFinished(event -> errorLabel.setVisible(false));
        visiblePause.play();
    }

    private void closeWindow() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}