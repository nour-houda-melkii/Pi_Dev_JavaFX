package com.controllers.nour;

import com.models.Commentaire;
import com.models.Produit;
import com.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.ResourceBundle;

public class CommentsDialogController implements Initializable {
    @FXML
    private VBox commentsContainer;
    @FXML
    private TextArea newCommentText;
    @FXML
    private Button addCommentButton;

    private Produit product;
    private int currentUserId;
    private CommentaireService commentaireService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        commentaireService = new CommentaireService();

        addCommentButton.setOnAction(event -> addComment());
    }

    public void setProduct(Produit product) {
        this.product = product;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void loadComments() {
        commentsContainer.getChildren().clear();

        try {
            List<Commentaire> comments = commentaireService.getCommentsForProduct(product.getId());

            if (comments.isEmpty()) {
                Label noCommentsLabel = new Label("No comments yet. Be the first to comment!");
                noCommentsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                commentsContainer.getChildren().add(noCommentsLabel);
            } else {
                for (Commentaire comment : comments) {
                    commentsContainer.getChildren().add(createCommentNode(comment));
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load comments: " + e.getMessage());
        }
    }

    private VBox createCommentNode(Commentaire comment) {
        VBox commentBox = new VBox(5);
        commentBox.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        // Comment header with user ID and date
        Label headerLabel = new Label("User #" + comment.getUserId() + " - " +
                comment.getCreatedAt().toString());
        headerLabel.setStyle("-fx-font-weight: bold;");

        // Comment content
        TextFlow contentFlow = new TextFlow();
        Text contentText = new Text(comment.getContent());
        contentFlow.getChildren().add(contentText);

        commentBox.getChildren().addAll(headerLabel, contentFlow);

        // Add edit/delete buttons if this is the current user's comment
        if (comment.getUserId() == currentUserId) {
            HBox buttonBox = new HBox(5);

            Button editButton = new Button("Edit");
            editButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white;");
            editButton.setOnAction(event -> editComment(comment));

            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: #ff3333; -fx-text-fill: white;");
            deleteButton.setOnAction(event -> deleteComment(comment));

            buttonBox.getChildren().addAll(editButton, deleteButton);
            commentBox.getChildren().add(buttonBox);
        }

        return commentBox;
    }
    private void addComment() {
        String content = newCommentText.getText().trim();
        if (content.isEmpty()) {
            showAlert("Error", "Comment cannot be empty");
            return;
        }

        try {
            Commentaire comment = new Commentaire(product.getId(), currentUserId, content);
            commentaireService.addComment(comment);
            newCommentText.clear();
            loadComments(); // Refresh the comments list
        } catch (SQLException e) {
            showAlert("Error", "Failed to add comment: " + e.getMessage());
        }
    }

    private void editComment(Commentaire comment) {
        TextInputDialog dialog = new TextInputDialog(comment.getContent());
        dialog.setTitle("Edit Comment");
        dialog.setHeaderText("Edit your comment");
        dialog.setContentText("Comment:");

        dialog.showAndWait().ifPresent(newContent -> {
            if (!newContent.trim().isEmpty()) {
                comment.setContent(newContent);
                try {
                    commentaireService.updateComment(comment);
                    loadComments(); // Refresh the comments list
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update comment: " + e.getMessage());
                }
            }
        });
    }

    private void deleteComment(Commentaire comment) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Comment");
        confirmation.setHeaderText("Are you sure you want to delete this comment?");
        confirmation.setContentText("This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentaireService.deleteComment(comment.getId(), currentUserId);
                    loadComments(); // Refresh the comments list
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete comment: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}