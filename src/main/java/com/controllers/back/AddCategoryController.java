package com.controllers.back;

import com.models.PostCategory;
import com.services.PostCategoryService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AddCategoryController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private Label messageLabel;

    private final PostCategoryService categoryService = new PostCategoryService();

    /**
     * Handles the "Add Category" button click event.
     * Validates inputs and checks for duplicate category names before saving.
     */

    @FXML
    private void handleAddCategory() {
        try {
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();

            // Validate required fields
            if (!validateInputs(name, description)) {
                return;
            }

            // Check for duplicate category name
            if (categoryService.categoryExists(name)) {
                showMessage("Category name already exists! Please choose a different name.", "error");
                return;
            }

            // Create and save category if validation passes
            PostCategory category = new PostCategory(name, description);
            categoryService.add(category);

            showMessage("Category added successfully!", "success");
            clearFields();

        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Validates the input fields
     * @param name Category name
     * @param description Category description
     * @return true if validation passes, false otherwise
     */

    private boolean validateInputs(String name, String description) {
        if (name.isEmpty()) {
            showMessage("Category name is required!", "error");
            return false;
        }

        if (description.isEmpty()) {
            showMessage("Category description is required!", "error");
            return false;
        }

        // Additional validation if needed (e.g., name length)
        if (name.length() > 50) {
            showMessage("Category name cannot exceed 50 characters!", "error");
            return false;
        }

        return true;
    }

    @FXML
    private void handleCancel() {
        clearFields();
        messageLabel.setText("");
    }

    /**
     * Displays a message with appropriate styling
     * @param message The message to display
     * @param type The message type ("error" or "success")
     */
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error", "success");
        messageLabel.getStyleClass().add(type);
    }

    /**
     * Clears all input fields
     */
    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
    }
}