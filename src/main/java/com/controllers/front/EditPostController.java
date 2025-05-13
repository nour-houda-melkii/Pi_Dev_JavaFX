package com.controllers.front;

import com.models.Post;
import com.models.PostCategory;
import com.services.PostCategoryService;
import com.services.PostService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class EditPostController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<PostCategory> categoryComboBox;
    @FXML private Label imageNameLabel;
    @FXML private ImageView imagePreview;
    @FXML private Button browseButton;
    @FXML private Button saveButton;

    private Post postToEdit;
    private PostListController postListController;
    private String imagePath;

    public void setPostToEdit(Post post) {
        this.postToEdit = post;
        populateFields();
    }

    public void setPostListController(PostListController controller) {
        this.postListController = controller;
    }

    @FXML
    public void initialize() {
        try {
            // Initialize type combo box
            typeComboBox.getItems().addAll("Offre", "Demande");

            // Initialize category combo box
            List<PostCategory> categories = new PostCategoryService().getAll();
            categoryComboBox.getItems().addAll(categories);
            categoryComboBox.setCellFactory(param -> new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            categoryComboBox.setButtonCell(new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Select Category" : item.getName());
                }
            });
        } catch (Exception e) {
            showAlert("Error", "Failed to load categories", Alert.AlertType.ERROR);
        }
    }

    private void populateFields() {
        if (postToEdit != null) {
            titleField.setText(postToEdit.getTitle());
            descriptionField.setText(postToEdit.getDescription());
            typeComboBox.getSelectionModel().select(postToEdit.getType());

            // Select the correct category
            categoryComboBox.getItems().stream()
                    .filter(c -> c.getId() == postToEdit.getCategoryId())
                    .findFirst()
                    .ifPresent(c -> categoryComboBox.getSelectionModel().select(c));

            // Load image if exists
            if (postToEdit.getImage() != null && !postToEdit.getImage().isEmpty()) {
                try {
                    imagePath = postToEdit.getImage();
                    File imageFile = new File(imagePath);
                    imageNameLabel.setText(imageFile.getName());
                    imagePreview.setImage(new Image(imageFile.toURI().toString()));
                    imagePreview.setVisible(true);
                } catch (Exception e) {
                    imageNameLabel.setText("Image unavailable");
                }
            }
        }
    }

    @FXML
    private void handleImageBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Post Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            imagePath = selectedFile.getAbsolutePath();
            imageNameLabel.setText(selectedFile.getName());
            imagePreview.setImage(new Image(selectedFile.toURI().toString()));
            imagePreview.setVisible(true);
        }
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            try {
                // Update the post object
                postToEdit.setTitle(titleField.getText());
                postToEdit.setDescription(descriptionField.getText());
                postToEdit.setType(typeComboBox.getValue());
                postToEdit.setCategoryId(categoryComboBox.getValue().getId());

                // Only update image if a new one was selected
                if (imagePath != null && !imagePath.equals(postToEdit.getImage())) {
                    postToEdit.setImage(imagePath);
                }

                // Save to database
                new PostService().update(postToEdit);

                // Refresh the post list
                if (postListController != null) {
                    postListController.refreshPosts();
                }

                // Close the window
                ((Stage) saveButton.getScene().getWindow()).close();

            } catch (SQLException e) {
                showAlert("Error", "Failed to update post", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private boolean validateInput() {
        if (titleField.getText().isEmpty()) {
            showAlert("Validation Error", "Title cannot be empty", Alert.AlertType.WARNING);
            return false;
        }
        if (descriptionField.getText().isEmpty()) {
            showAlert("Validation Error", "Description cannot be empty", Alert.AlertType.WARNING);
            return false;
        }
        if (typeComboBox.getValue() == null) {
            showAlert("Validation Error", "Please select a type", Alert.AlertType.WARNING);
            return false;
        }
        if (categoryComboBox.getValue() == null) {
            showAlert("Validation Error", "Please select a category", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}