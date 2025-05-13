package com.controllers.front;

import com.models.*;
import com.services.AuthService;
import com.services.PostService;
import com.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.services.FirebaseService;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ConversationsListController {
    @FXML private ListView<Conversation> convListView;
    @FXML private VBox loadingBox;
    @FXML private VBox contentBox;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private Button newMessageButton;

    private User currentUser;
    private final PostService postService = new PostService();
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

    // This method will be called by JavaFX when the FXML is loaded
    @FXML
    public void initialize() {
        setupListView();
        // Set placeholder text when no conversations are available
        convListView.setPlaceholder(new Label("No conversations found"));
    }

    // Call this method when setting the controller
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            // Now that we have a user, load conversations
            loadConversations();
        }
    }

    private void setupListView() {
        convListView.setCellFactory(lv -> new ConversationCell());

        convListView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {  // Double-click to open chat
                Conversation convo = convListView.getSelectionModel().getSelectedItem();
                if (convo != null) openChat(convo.conversationId);
            }
        });

        // Add context menu to the list view
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("Open Chat");
        MenuItem deleteItem = new MenuItem("Delete Conversation");

        openItem.setOnAction(e -> {
            Conversation convo = convListView.getSelectionModel().getSelectedItem();
            if (convo != null) openChat(convo.conversationId);
        });

        deleteItem.setOnAction(e -> {
            Conversation convo = convListView.getSelectionModel().getSelectedItem();
            if (convo != null) deleteConversation(convo);
        });

        contextMenu.getItems().addAll(openItem, deleteItem);
        convListView.setContextMenu(contextMenu);
    }

    @FXML
    public void refreshConversations() {
        if (currentUser != null) {
            loadConversations();
        }
    }

    @FXML
    public void startNewConversation() {
        // Implement the functionality to start a new conversation
        System.out.println("Starting new conversation");

        // Here you would typically open a dialog to select a user or post
        // For now, we'll just show a placeholder
        statusLabel.setText("New conversation feature coming soon!");
    }

    private void loadConversations() {
        if (currentUser == null) {
            System.err.println("Cannot load conversations: currentUser is null");
            return;
        }

        int userId = currentUser.getId();
        System.out.println("Loading conversations for user ID: " + userId);

        // Show loading indicator
        loadingBox.setVisible(true);
        contentBox.setVisible(false);
        statusLabel.setText("Loading conversations...");

        new Thread(() -> {
            try {
                List<Conversation> convos = FirebaseService.getConversationsForUser(userId);
                Platform.runLater(() -> {
                    convListView.getItems().setAll(convos);
                    loadingBox.setVisible(false);
                    contentBox.setVisible(true);
                    statusLabel.setText(convos.size() + " conversation(s) found");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    contentBox.setVisible(true);
                    statusLabel.setText("Error loading conversations");

                    // Show error alert
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load conversations");
                    alert.setContentText("An error occurred: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void openChat(String conversationId) {
        System.out.println("Opening chat for conversation: " + conversationId);
        statusLabel.setText("Opening chat...");

        Platform.runLater(() -> {
            try {
                // Load the ChatWindow FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/ChatWindow.fxml"));
                Parent root = loader.load();

                // Get the controller and set up the conversation
                ChatWindowController controller = loader.getController();
                controller.setConversation(conversationId, currentUser);

                // Set up the stage
                Stage stage = new Stage();
                stage.setTitle("Chat");
                stage.setScene(new Scene(root));

                // Handle window close to clean up resources
                stage.setOnCloseRequest(event -> controller.onWindowClose());

                // Show the window
                stage.show();
                statusLabel.setText("Chat opened");
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Error opening chat");
                System.err.println("Failed to open chat window: " + e.getMessage());
            }
        });
    }

    private void deleteConversation(Conversation conversation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Conversation");
        confirm.setHeaderText("Delete Conversation");
        confirm.setContentText("Are you sure you want to delete this conversation?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Show loading state
                statusLabel.setText("Deleting conversation...");

                // Implement deletion logic here
                // For now, just remove from the list
                convListView.getItems().remove(conversation);
                statusLabel.setText("Conversation deleted");

                // Here you would typically call Firebase to delete the conversation
                // FirebaseService.deleteConversation(conversation.conversationId);
            }
        });
    }

    // Custom cell implementation for conversation items
    private class ConversationCell extends ListCell<Conversation> {
        private final HBox content;
        private final Label usernameLabel;
        private final Label postLabel;

        public ConversationCell() {
            content = new HBox(10);
            content.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

            VBox infoBox = new VBox(5);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            usernameLabel = new Label();
            usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            postLabel = new Label();
            postLabel.setStyle("-fx-text-fill: #7f8c8d;");

            HBox detailsBox = new HBox(10);

            infoBox.getChildren().addAll(usernameLabel, postLabel, detailsBox);

            // Profile circle placeholder (can be replaced with actual profile image)
            Region profileCircle = new Region();
            profileCircle.setPrefSize(40, 40);
            profileCircle.setMinSize(40, 40);
            profileCircle.setMaxSize(40, 40);
            profileCircle.setStyle("-fx-background-color: #3498db; -fx-background-radius: 20;");

            content.getChildren().addAll(profileCircle, infoBox);
        }

        @Override
        protected void updateItem(Conversation conversation, boolean empty) {
            super.updateItem(conversation, empty);

            if (empty || conversation == null) {
                setGraphic(null);
                setText(null);
            } else {
                try {
                    int otherUserId = conversation.postOwnerId == currentUser.getId()
                            ? conversation.interestedUserId
                            : conversation.postOwnerId;

                    String otherUsername = authService.getUsernameById(otherUserId);
                    String postName = postService.getPostNameById(conversation.postId);

                    usernameLabel.setText(otherUsername);
                    postLabel.setText("About: " + postName);
                    setGraphic(content);
                } catch (SQLException e) {
                    e.printStackTrace();
                    setText("Error loading conversation");
                }
            }
        }
    }
}