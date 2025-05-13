package com.controllers.front;

import com.exceptions.AuthException;
import com.models.*;
import com.services.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class CreatePostController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label imageNameLabel;
    @FXML private Button browseButton;
    @FXML private Button submitButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    private User currentUser;

    private File selectedImageFile;

    private PostCategoryService postcategoryService;
    private WebPurifyModerationService moderationService;
    private ClarifaiImageValidationService imageValidationService;
    private PostService postService;
    private AuthService authService = new AuthService();;

    // Flag to control submission state
    private boolean isSubmitting = false;
    private PostListController postListController;

    /*
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    public void setPostListController(PostListController controller) {
        this.postListController = controller;
    }
    */


    public void setToken(String token) throws AuthException {
        if (token != null) {
            currentUser = authService.getUserFromToken(token);
            initialize();
        }
    }


    @FXML
    public void initialize() throws AuthException {
        // Initialize services
        postcategoryService = new PostCategoryService();
        moderationService = new WebPurifyModerationService();
        imageValidationService = new ClarifaiImageValidationService();
        postService = new PostService();

        // Initial UI setup
        progressIndicator.setVisible(false);
        statusLabel.setVisible(false);

        // Corrige la duplication : vide d'abord les items
        typeComboBox.getItems().clear();
        typeComboBox.getItems().addAll("Offre", "Demande");

        // Load categories from database
        loadCategories();

        // We'll manage the submit button state manually instead of using binding
        updateSubmitButtonState();

        // Add listeners to update button state when fields change
        titleField.textProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
    }

    private void updateSubmitButtonState() {
        boolean fieldsCompleted = !titleField.getText().isEmpty() &&
                !descriptionField.getText().isEmpty() &&
                typeComboBox.getValue() != null &&
                categoryComboBox.getValue() != null;

        submitButton.setDisable(isSubmitting || !fieldsCompleted);
    }

    private void loadCategories() {
        try {
            categoryComboBox.getItems().clear(); // Corrige la duplication
            java.util.Set<String> uniqueNames = new java.util.HashSet<>();
            List<PostCategory> categories = postcategoryService.getAll();
            for (PostCategory category : categories) {
                if (uniqueNames.add(category.getName())) {
                    categoryComboBox.getItems().add(category.getName());
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to load categories: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleImageBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Post Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedImageFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedImageFile != null) {
            imageNameLabel.setText(selectedImageFile.getName());

        }
    }

    @FXML
    private void handleSubmit() {
        // Update UI for processing state
        isSubmitting = true;
        updateUI(true, "Checking content...");

        // Create background task for content validation
        Task<ValidationResult> validationTask = new Task<ValidationResult>() {
            @Override
            protected ValidationResult call() throws Exception {
                ValidationResult result = new ValidationResult();

                try {
                    // Step 1: Text content moderation
                    updateProgress(0.2, 1.0);
                    updateMessage("Checking text content...");

                    try {
                        // Try using WebPurify API
                        result.isTextAppropriate = moderationService.isAppropriateContent(descriptionField.getText());
                        result.textUsedApi = true;
                    } catch (Exception e) {
                        System.err.println("WebPurify API error: " + e.getMessage());
                        e.printStackTrace();
                        // Fallback to local check if API fails
                        result.isTextAppropriate = moderationService.checkContentLocally(descriptionField.getText());
                        result.textUsedApi = false;
                        result.textApiErrorMessage = e.getMessage();
                    }

                    if (!result.isTextAppropriate) {
                        return result; // Early exit if text is inappropriate
                    }

                    // Step 2: Image validation (if an image was uploaded)
                    updateProgress(0.6, 1.0);
                    updateMessage("Checking image content...");

                    if (selectedImageFile != null) {
                        try {
                            result.isImageAppropriate = imageValidationService.isHealthcareRelated(selectedImageFile);
                            result.imageUsedApi = true;
                        } catch (Exception e) {
                            System.err.println("Clarifai API error: " + e.getMessage());
                            e.printStackTrace();
                            // Be lenient if API fails
                            result.isImageAppropriate = true;
                            result.imageUsedApi = false;
                            result.imageApiErrorMessage = e.getMessage();
                        }
                    } else {
                        // No image to validate
                        result.isImageAppropriate = true;
                    }

                    updateProgress(1.0, 1.0);
                    return result;

                } catch (Exception e) {
                    System.err.println("General validation error: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void updateMessage(String message) {
                super.updateMessage(message);
                Platform.runLater(() -> statusLabel.setText(message));
            }
        };

        // Bind progress indicator to task progress
        progressIndicator.progressProperty().bind(validationTask.progressProperty());

        // Handle task completion
        validationTask.setOnSucceeded(event -> {
            ValidationResult result = validationTask.getValue();
            progressIndicator.progressProperty().unbind();

            // Check text content results
            if (!result.isTextAppropriate) {
                isSubmitting = false;
                updateUI(false, "Inappropriate text content detected");
                showAlert(Alert.AlertType.WARNING, "Inappropriate Content",
                        "Your post contains inappropriate language or content. Please revise your description.");
                updateSubmitButtonState();
                return;
            }

            // Check image content results
            if (!result.isImageAppropriate) {
                isSubmitting = false;
                updateUI(false, "Inappropriate image detected");
                showAlert(Alert.AlertType.WARNING, "Inappropriate Image",
                        "The image you uploaded doesn't appear to be healthcare-related. Please select an other image.");
                updateSubmitButtonState();
                return;
            }

            // All checks passed
            updateUI(true, "Content approved. Creating post...");
            createPost();
        });

        validationTask.setOnFailed(event -> {
            isSubmitting = false;
            progressIndicator.progressProperty().unbind();
            updateUI(false, "Content check failed");
            Throwable exception = validationTask.getException();
            System.err.println("Validation task error: " + exception.getMessage());
            exception.printStackTrace();

            // Show error but allow submission (lenient approach)
            showAlert(Alert.AlertType.WARNING, "Content Check Failed",
                    "We couldn't verify the content. You may continue, but please ensure your post follows community guidelines.");
            updateSubmitButtonState();
        });

        // Start the task in a new thread
        new Thread(validationTask).start();
    }

    private void updateUI(boolean processing, String statusText) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(processing);
            statusLabel.setText(statusText);
            statusLabel.setVisible(true);
            // Don't directly set submitButton.setDisable() here
            // The updateSubmitButtonState() will handle that
        });
    }

    private void createPost() {
        Platform.runLater(() -> {
            try {
                // Create new post object
                Post newPost = new Post();
                newPost.setTitle(titleField.getText());
                newPost.setDescription(descriptionField.getText());
                newPost.setType(typeComboBox.getValue());
                newPost.setCategoryId(postcategoryService.getCategoryIdByName(categoryComboBox.getValue()));
                newPost.setAuthorId(currentUser.getId());
                newPost.setImage(selectedImageFile != null ? selectedImageFile.getAbsolutePath() : null);
                newPost.setCreatedAt(LocalDateTime.now());
                newPost.setEnabled(true);
                newPost.setStatus("active");

                // Save to database
                postService.add(newPost);

                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success", "Post created successfully!");

                // Refresh the post list
                if (postListController != null) {
                    postListController.refreshPosts();
                }

                // Close the form
                ((Stage) submitButton.getScene().getWindow()).close();

            } catch (Exception e) {
                isSubmitting = false;
                updateUI(false, "Error creating post");
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to create post: " + e.getMessage());
                e.printStackTrace();
                updateSubmitButtonState();
            }
        });
    }

    @FXML
    private void handleCancel() {
        ((Stage) submitButton.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Helper class to store validation results
    private static class ValidationResult {
        // Text validation results
        boolean isTextAppropriate = false;
        boolean textUsedApi = false;
        String textApiErrorMessage = null;

        // Image validation results
        boolean isImageAppropriate = false;
        boolean imageUsedApi = false;
        String imageApiErrorMessage = null;
    }
}