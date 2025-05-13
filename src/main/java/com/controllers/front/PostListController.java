package com.controllers.front;

import com.exceptions.AuthException;
import com.models.*;
import com.services.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;

public class PostListController {
    @FXML private VBox postsContainer;
    @FXML private Label postsCountLabel;
    @FXML private ComboBox<PostCategory> categoryFilterComboBox;
    @FXML private ComboBox<String> dateSortComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;


    private final PostService postService = new PostService();
    private final AuthService authService = new AuthService();
    private final PostCategoryService categoryService = new PostCategoryService();
    private final ReactionsService reactionsService = new ReactionsService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private List<Post> allPosts;
    private List<PostCategory> allCategories;

    private User currentUser; // To track the logged-in user

    // Call this method when setting the controller
    /*public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            initialize();
        }
    }*/

    public void setToken(String token) throws AuthException {
        if (token != null) {
            currentUser = authService.getUserFromToken(token);
            initialize();
        }
    }


    @FXML
    public void initialize() {
        setupFilters();
        loadPosts();
    }

    /**
     * Sets up a listener to monitor window width changes and adjust sidebar visibility.
     */

    private void setupFilters() {
        try {
            // Initialize type filter
            typeFilterComboBox.setItems(FXCollections.observableArrayList(
                    "All Types", "Offre", "Demande"
            ));
            typeFilterComboBox.getSelectionModel().selectFirst();
            typeFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Load all categories for filtering
            allCategories = categoryService.getAll();

            // Create "All Categories" option
            PostCategory allCategoriesOption = new PostCategory(-1, "All Categories");
            allCategories.add(0, allCategoriesOption); // Add at beginning

            categoryFilterComboBox.setItems(FXCollections.observableArrayList(allCategories));
            categoryFilterComboBox.getSelectionModel().selectFirst(); // Select "All Categories" by default

            // Set cell factory to show category names
            categoryFilterComboBox.setCellFactory(param -> new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            // Set button cell to show selected category name
            categoryFilterComboBox.setButtonCell(new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Filter by category..." : item.getName());
                }
            });

            // Initialize date sort options
            dateSortComboBox.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First"
            ));
            dateSortComboBox.getSelectionModel().selectFirst();

            // Set up listeners
            categoryFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize filters: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadPosts() {
        try {
            allPosts = postService.getAllByEnabled();
            filterPosts();
        } catch (Exception e) {
            showMessage("Error loading posts: " + e.getMessage(), "error-message");
            e.printStackTrace();
        }
    }

    private void filterPosts() throws SQLException {
        List<Post> filtered = allPosts;

        // Apply type filter
        String selectedType = typeFilterComboBox.getValue();
        if (selectedType != null && !selectedType.equals("All Types")) {
            filtered = filtered.stream()
                    .filter(post -> post.getType().equalsIgnoreCase(selectedType))
                    .collect(Collectors.toList());
        }

        // Apply category filter (skip if "All Categories" is selected)
        PostCategory selectedCategory = categoryFilterComboBox.getValue();
        if (selectedCategory != null && selectedCategory.getId() != -1) {
            filtered = filtered.stream()
                    .filter(post -> post.getCategoryId() == selectedCategory.getId())
                    .collect(Collectors.toList());
        }

        // Apply date sorting
        String sortOption = dateSortComboBox.getValue();
        if (sortOption != null) {
            filtered.sort(sortOption.equals("Newest First")
                    ? Comparator.comparing(Post::getCreatedAt).reversed()
                    : Comparator.comparing(Post::getCreatedAt));
        }

        displayPosts(filtered);
    }

    private void displayPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        postsCountLabel.setText(String.valueOf(posts.size()) + " posts");

        if (posts.isEmpty()) {
            Label noPostsLabel = new Label("No posts found matching your filters");
            noPostsLabel.getStyleClass().add("no-posts-label");
            postsContainer.getChildren().add(noPostsLabel);
            return;
        }

        for (Post post : posts) {
            try {
                String authorUsername = authService.getUsernameById(post.getAuthorId());
                String categoryName = categoryService.getCategoryNameById(post.getCategoryId());
                Node postCard = createPostCard(post, authorUsername, categoryName);

                // Add a fade-in animation for smooth loading
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), postCard);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1.0);
                fadeIn.play();

                postsContainer.getChildren().add(postCard);
            } catch (SQLException e) {
                System.err.println("Error loading post details: " + e.getMessage());
            }
        }
    }

    private Node createPostCard(Post post, String authorUsername, String categoryName) throws SQLException {
        // Main card container
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("post-card");
        card.setPadding(new Insets(15));
        card.setSpacing(12);
        card.setMaxWidth(700);
        card.setMinWidth(700);

        // 1. POST HEADER (Author info, date, type)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar (circular with initial)
        StackPane avatar = createAvatar(authorUsername);

        // Author details (name and date)
        VBox authorDetails = new VBox(2);
        Label nameLabel = new Label(authorUsername);
        nameLabel.getStyleClass().add("author-name");

        Label dateLabel = new Label(formatPostDate(post.getCreatedAt().format(dateFormatter)));
        dateLabel.getStyleClass().add("post-date");

        authorDetails.getChildren().addAll(nameLabel, dateLabel);

        // Type badge (right aligned)
        Label typeLabel = new Label(post.getType());
        typeLabel.getStyleClass().addAll("post-type-badge", post.getType().toLowerCase() + "-badge");

        // Put together the header with spacer for alignment
        header.getChildren().addAll(avatar, authorDetails);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        header.getChildren().addAll(headerSpacer);

        // Add type label
        header.getChildren().add(typeLabel);

        // Add the options menu (three dots) - only for post owner
        if (currentUser != null && post.getAuthorId() == currentUser.getId()) {
            Button optionsButton = createOptionsButton(post);
            header.getChildren().add(optionsButton);
        }

        // 2. POST CONTENT
        // Title
        Label titleLabel = new Label(post.getTitle());
        titleLabel.getStyleClass().add("post-title");
        titleLabel.setWrapText(true);

        // Category
        Label categoryLabel = new Label("#" + categoryName);
        categoryLabel.getStyleClass().add("category-label");

        // Description with proper text flow
        TextFlow description = new TextFlow();
        Text descText = new Text(post.getDescription());
        descText.getStyleClass().add("post-description");
        description.getChildren().add(descText);
        description.setMaxWidth(670);

        // 3. IMAGE (if present)
        Node imageView = createPostImage(post);

        // 4. STATUS INDICATOR
        HBox statusContainer = new HBox(8);
        statusContainer.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(5);
        statusDot.getStyleClass().addAll("status-indicator", "status-" + post.getStatus().toLowerCase());
        Label statusLabel = new Label(post.getStatus());
        statusLabel.getStyleClass().add("status-text");
        statusContainer.getChildren().addAll(statusDot, statusLabel);

        // 5. SEPARATOR
        Separator separator = new Separator();

        // 6. REACTIONS AND ACTIONS SECTION
        HBox bottomSection = createBottomSection(post);

        // Assemble the card
        card.getChildren().addAll(
                header,
                titleLabel,
                categoryLabel,
                description,
                imageView,
                statusContainer,
                separator,
                bottomSection
        );

        // Add hover effect animation
        addCardHoverEffect(card);

        return card;
    }

    // Create the three dots options menu button
    private Button createOptionsButton(Post post) {
        // Create the options button (three dots)
        Button optionsButton = new Button("⋮"); // Unicode character for vertical ellipsis
        optionsButton.getStyleClass().add("options-button");

        // Style the button
        optionsButton.setStyle("-fx-background-color: transparent; -fx-font-size: 25px; -fx-font-weight: bold;");

        // Create options menu popup
        optionsButton.setOnAction(e -> {
            showOptionsMenu(optionsButton, post);
        });

        return optionsButton;
    }

    // Show the options menu with Edit and Delete options
    private void showOptionsMenu(Button optionsButton, Post post) {
        // Create the popup menu
        VBox optionsMenu = new VBox(5);
        optionsMenu.getStyleClass().add("options-menu");
        optionsMenu.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 4px; -fx-padding: 5px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");

        // Create menu items without icons
        HBox editOption = createMenuOption("Edit", e -> {
            handleEditPost(post);
        });

        HBox deleteOption = createMenuOption("Delete", e -> {
            handleDeletePost(post);
        });

        // Add menu items to the container
        optionsMenu.getChildren().addAll(editOption, deleteOption);

        // Create and show the popup
        Popup popup = new Popup();
        popup.setAutoHide(true); // Close when clicked outside
        popup.getContent().add(optionsMenu);

        // Position the popup next to the button
        Node node = optionsButton;
        Window window = node.getScene().getWindow();

        // Calculate position (right below the button)
        Point2D point = node.localToScreen(node.getBoundsInLocal().getMaxX() - 100,
                node.getBoundsInLocal().getMaxY());

        popup.show(window, point.getX(), point.getY());

        // Add animation for smooth appearance
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), optionsMenu);
        fadeIn.setFromValue(0.5);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // Create a menu option without icon
    private HBox createMenuOption(String text, EventHandler<MouseEvent> handler) {
        HBox option = new HBox(10);
        option.setPadding(new Insets(8, 15, 8, 10));
        option.setAlignment(Pos.CENTER_LEFT);

        // Set style and hover effects
        option.setStyle("-fx-background-color: transparent;");
        option.setCursor(Cursor.HAND);

        // Create just the text label
        Label textLabel = new Label(text);
        option.getChildren().add(textLabel);

        // Add hover effect
        option.setOnMouseEntered(e -> {
            option.setStyle("-fx-background-color: #f0f0f0;");
        });

        option.setOnMouseExited(e -> {
            option.setStyle("-fx-background-color: transparent;");
        });

        // Set the action handler
        option.setOnMouseClicked(handler);

        return option;
    }

    private StackPane createAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar");

        // Get first letter of username for avatar
        String initial = username.substring(0, 1).toUpperCase();
        Label avatarLabel = new Label(initial);
        avatarLabel.getStyleClass().add("avatar-text");

        avatar.getChildren().add(avatarLabel);
        return avatar;
    }

    private String formatPostDate(String dateStr) {
        // Format can be enhanced here if needed
        return dateStr;
    }

    private Node createPostImage(Post post) {
        // If no image, return an empty node
        if (post.getImage() == null || post.getImage().isEmpty()) {
            return new Region();
        }

        // Create image container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setMinHeight(300);
        imageContainer.setMaxHeight(300);
        imageContainer.setPadding(new Insets(10, 0, 10, 0));

        try {
            ImageView imageView = new ImageView(new Image("file:" + post.getImage(), true));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(670);
            imageView.setFitHeight(300);

            // Add mouse hover zoom effect
            imageView.setOnMouseEntered(e -> {
                ScaleTransition scale = new ScaleTransition(Duration.millis(300), imageView);
                scale.setToX(1.05);
                scale.setToY(1.05);
                scale.play();
            });

            imageView.setOnMouseExited(e -> {
                ScaleTransition scale = new ScaleTransition(Duration.millis(300), imageView);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
            });

            imageContainer.getChildren().add(imageView);
            return imageContainer;

        } catch (Exception e) {
            Label errorLabel = new Label("Image unavailable");
            errorLabel.getStyleClass().add("image-error");
            imageContainer.getChildren().add(errorLabel);
            return imageContainer;
        }
    }

    private HBox createBottomSection(Post post) throws SQLException {
        HBox container = new HBox();

        // 1. Reactions area (left side)
        HBox reactionsArea = createEnhancedReactionsArea(post);

        // Create the action buttons container
        HBox actionButtons = new HBox(10); // 10px spacing between buttons
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        if (currentUser != null && post.getAuthorId() != currentUser.getId() && post.getStatus().equals("active")) {
            // Create button with initial loading state
            Button msgButton = createActionButton("Checking...", "edit-button");
            msgButton.setDisable(true); // Initially disabled while checking

            // Add button to UI immediately so there's no layout shift
            actionButtons.getChildren().add(msgButton);

            // Calculate the conversation ID
            String conversationId = post.getId() + "_" + currentUser.getId();

            // Check if a conversation already exists and update button state accordingly
            new Thread(() -> {
                try {
                    boolean conversationExists = FirebaseService.checkConversationExists(conversationId);

                    Platform.runLater(() -> {
                        if (conversationExists) {
                            // Conversation exists - permanently disable
                            msgButton.setDisable(true);
                            msgButton.setText("Message Sent");
                            msgButton.getStyleClass().clear();
                            msgButton.getStyleClass().addAll("button", "edit-button");
                        } else {
                            // No conversation - enable for messaging
                            msgButton.setDisable(false);
                            msgButton.setText("Send Message");
                            msgButton.getStyleClass().clear();
                            msgButton.getStyleClass().addAll("button", "edit-button");
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();

                    // In case of error, show the button as active
                    Platform.runLater(() -> {
                        msgButton.setDisable(false);
                        msgButton.setText("Send Message");
                        msgButton.getStyleClass().clear();
                        msgButton.getStyleClass().addAll("button", "edit-button");
                    });
                }
            }).start();

            // Button click handler
            msgButton.setOnAction(e -> {
                // Disable button and show processing state
                msgButton.setDisable(true);
                msgButton.setText("Sending...");

                new Thread(() -> {
                    try {
                        // 1. Create or overwrite the conversation object
                        Conversation convo = new Conversation(
                                conversationId,
                                post.getId(),
                                post.getAuthorId(),
                                currentUser.getId(),
                                "open"
                        );
                        FirebaseService.createConversation(convo);

                        // 2. Auto‑generate the first message
                        String baseItem = post.getTitle(); // e.g. "wheelchair"
                        String initialText;
                        if (post.getType().equals("demande")) {
                            initialText = "Hi, I can help you with the " + baseItem;
                        } else {
                            initialText = "Hi, I really need that " + baseItem;
                        }
                        Message initial = new Message(
                                null,
                                currentUser.getId(),
                                initialText,
                                System.currentTimeMillis()
                        );
                        FirebaseService.sendMessage(conversationId, initial);

                        // 3. Show a pretty alert on the JavaFX application thread
                        Platform.runLater(() -> {
                            showSuccessAlert("Message Sent", "Your message has been sent to the owner of this post.");

                            // Permanently disable the button and update its appearance
                            msgButton.setDisable(true);
                            msgButton.setText("Message Sent");
                            msgButton.getStyleClass().clear();
                            msgButton.getStyleClass().addAll("button", "edit-button");
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();

                        // Re-enable the button if there was an error
                        Platform.runLater(() -> {
                            msgButton.setDisable(false);
                            msgButton.setText("Send Message");
                            msgButton.getStyleClass().clear();
                            msgButton.getStyleClass().addAll("button", "message-button");
                            showErrorAlert("Error", "Failed to send message. Please try again.");
                        });
                    }
                }).start();
            });
        }

        // Add spacer for alignment
        container.getChildren().add(reactionsArea);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        container.getChildren().addAll(spacer,actionButtons);

        return container;
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);

        // Add hover effect animation
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return button;
    }

    private HBox createEnhancedReactionsArea(Post post) throws SQLException {
        VBox container = new VBox(5);
        container.setAlignment(Pos.CENTER_LEFT);

        // Get reaction data for this post
        Map<String, Integer> reactionCounts = reactionsService.getReactionCountsForPost(post.getId());
        String userReaction = null;
        if (currentUser != null) {
            userReaction = reactionsService.getUserReactionToPost(post.getId(), currentUser.getId());
        }
        int totalReactions = reactionCounts.values().stream().mapToInt(Integer::intValue).sum();

        // 1. REACTION SUMMARY (only if there are reactions)
        if (totalReactions > 0) {
            HBox reactionSummary = createReactionSummary(reactionCounts, totalReactions);
            container.getChildren().add(reactionSummary);
        }

        // 2. REACTION POPUP (hidden by default)
        HBox reactionPopup = createReactionPopup(post.getId(), userReaction);
        reactionPopup.setVisible(false);
        reactionPopup.setOpacity(0);
        reactionPopup.setTranslateY(-60);

        // 3. REACTION BUTTON
        Button mainReactionBtn = createReactionButton(userReaction);
        mainReactionBtn.setOnAction(e -> {
            // Toggle visibility of reaction popup
            if (!reactionPopup.isVisible()) {
                showReactionPopup(reactionPopup);
            } else {
                hideReactionPopup(reactionPopup);
            }
        });

        // Show popup on hover
        setupReactionHoverBehavior(mainReactionBtn, reactionPopup);

        // Add both to container
        container.getChildren().addAll(reactionPopup, mainReactionBtn);

        // Wrap in HBox for layout purposes
        HBox wrapper = new HBox();
        wrapper.getChildren().add(container);
        return wrapper;
    }

    private HBox createReactionSummary(Map<String, Integer> reactionCounts, int totalReactions) {
        HBox summary = new HBox(5);

        summary.setAlignment(Pos.CENTER_LEFT);
        summary.getStyleClass().add("reaction-summary");

        // Create overlapping emoji bubbles for up to 3 reaction types
        HBox emojiStack = new HBox(-8); // Negative spacing for overlap effect
        emojiStack.setAlignment(Pos.CENTER_LEFT);

        // Sort reactions by count (highest first) and take top 3
        List<String> topReactions = reactionCounts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Create bubbles for each reaction type
        for (String reactionType : topReactions) {
            StackPane bubble = new StackPane();
            bubble.getStyleClass().addAll("reaction-bubble", "reaction-" + reactionType.toLowerCase());

            Label emoji = new Label(getEmojiForReaction(reactionType));
            emoji.getStyleClass().add("bubble-emoji");

            bubble.getChildren().add(emoji);
            emojiStack.getChildren().add(bubble);
        }

        // Total reactions count
        Label countLabel = new Label(Integer.toString(totalReactions));
        countLabel.getStyleClass().add("reaction-count");

        summary.getChildren().addAll(emojiStack, countLabel);
        return summary;
    }
    private HBox createReactionPopup(int postId, String currentUserReaction) {
        HBox panel = new HBox(15);
        panel.getStyleClass().add("reaction-popup-panel");
        panel.setAlignment(Pos.CENTER);

        // Add temporary background color for visibility
        panel.setStyle("-fx-background-color: pink;");

        // Create the reaction options
        StackPane likeOption = createReactionOption("LIKE", "👍", "Like",
                "LIKE".equals(currentUserReaction), postId);
        StackPane loveOption = createReactionOption("LOVE", "❤️", "Love",
                "LOVE".equals(currentUserReaction), postId);
        StackPane sadOption = createReactionOption("SAD", "😢", "Sad",
                "SAD".equals(currentUserReaction), postId);

        panel.getChildren().addAll(likeOption, loveOption, sadOption);
        return panel;
    }

    private StackPane createReactionOption(String type, String emoji, String tooltip,
                                           boolean isActive, int postId) {
        StackPane option = new StackPane();
        option.getStyleClass().add("reaction-option-container");


        // If this is user's current reaction, highlight it
        if (isActive) {
            option.getStyleClass().add("active-reaction");
        }

        // Set tooltip
        Tooltip tip = new Tooltip(tooltip);
        Tooltip.install(option, tip);

        // Add emoji
        Label emojiLabel = new Label(emoji);
        emojiLabel.getStyleClass().add("reaction-option-emoji");
        option.getChildren().add(emojiLabel);

        // Add hover animations
        option.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), option);
            scale.setToX(1.3);
            scale.setToY(1.3);

            TranslateTransition bounce = new TranslateTransition(Duration.millis(150), option);
            bounce.setByY(-10);

            ParallelTransition animation = new ParallelTransition(scale, bounce);
            animation.play();
        });

        option.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), option);
            scale.setToX(1.0);
            scale.setToY(1.0);

            TranslateTransition bounce = new TranslateTransition(Duration.millis(150), option);
            bounce.setByY(10);

            ParallelTransition animation = new ParallelTransition(scale, bounce);
            animation.play();
        });

        // Click handler to add/update reaction
        option.setOnMouseClicked(e -> {
            if (currentUser == null) {
                showAlert("Connexion requise", "Vous devez être connecté pour réagir à un post.", Alert.AlertType.WARNING);
                return;
            }
            try {
                // Click animation
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(80), option);
                scaleDown.setToX(0.9);
                scaleDown.setToY(0.9);

                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), option);
                scaleUp.setToX(1.3);
                scaleUp.setToY(1.3);

                SequentialTransition clickAnim = new SequentialTransition(scaleDown, scaleUp);

                clickAnim.setOnFinished(event -> {
                    try {
                        // Add or update reaction
                        Reactions reaction = new Reactions(postId, currentUser.getId(), type);
                        reactionsService.addReaction(reaction);

                        // Refresh posts to update UI
                        loadPosts();
                    } catch (SQLException ex) {
                        showAlert("Error", "Failed to add reaction: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                });

                clickAnim.play();
            } catch (Exception ex) {
                showAlert("Error", "Failed to record reaction: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        return option;
    }

    private Button createReactionButton(String userReaction) {
        Button button = new Button();
        button.getStyleClass().add("reaction-main-button");
        button.setStyle("-fx-background-color: pink;");

        // Create content with emoji + text
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);

        // Get appropriate emoji and text based on user's reaction
        String emoji = userReaction != null ? getEmojiForReaction(userReaction) : "👍";
        String text = userReaction != null ? getLabelForReaction(userReaction) : "Like";

        Label emojiLabel = new Label(emoji);
        emojiLabel.getStyleClass().add("reaction-button-emoji");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("reaction-button-text");

        // If user has already reacted, add special styling
        if (userReaction != null) {
            button.getStyleClass().add("reacted-" + userReaction.toLowerCase());
            textLabel.getStyleClass().add("reacted-text-" + userReaction.toLowerCase());
        }

        content.getChildren().addAll(emojiLabel, textLabel);
        button.setGraphic(content);

        return button;
    }

    private void setupReactionHoverBehavior(Button mainButton, HBox reactionPopup) {
        final boolean[] isOverPanel = {false};

        // Show popup when mouse enters main button
        mainButton.setOnMouseEntered(e -> {
            showReactionPopup(reactionPopup);
        });

        // Track when mouse is over popup panel
        reactionPopup.setOnMouseEntered(e -> {
            isOverPanel[0] = true;
        });

        reactionPopup.setOnMouseExited(e -> {
            isOverPanel[0] = false;
            hideReactionPopup(reactionPopup);
        });

        // Hide popup when leaving button (with delay to check if moved to popup)
        mainButton.setOnMouseExited(e -> {
            PauseTransition delay = new PauseTransition(Duration.millis(100));
            delay.setOnFinished(event -> {
                if (!isOverPanel[0]) {
                    hideReactionPopup(reactionPopup);
                }
            });
            delay.play();
        });
    }

    private void showReactionPopup(HBox popup) {
        popup.setVisible(true);

        // Animation to show popup
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), popup);
        slideUp.setFromY(-50);
        slideUp.setToY(-70);

        ParallelTransition showAnim = new ParallelTransition(fadeIn, slideUp);
        showAnim.play();
    }

    private void hideReactionPopup(HBox popup) {
        // Animation to hide popup
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), popup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(200), popup);
        slideDown.setFromY(-70);
        slideDown.setToY(-50);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideDown);
        hideAnim.setOnFinished(event -> popup.setVisible(false));
        hideAnim.play();
    }

    private String getEmojiForReaction(String reaction) {
        if (reaction == null) return "👍"; // Default

        switch (reaction) {
            case "LIKE": return "👍";
            case "LOVE": return "❤️";
            case "SAD": return "😢";
            default: return "👍";
        }
    }

    private String getLabelForReaction(String reaction) {
        if (reaction == null) return "Like"; // Default

        switch (reaction) {
            case "LIKE": return "Like";
            case "LOVE": return "Love";
            case "SAD": return "Sad";
            default: return "Like";
        }
    }

    private void addCardHoverEffect(VBox card) {
        card.setOnMouseEntered(e -> {
            card.getStyleClass().add("post-card-hover");
        });

        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("post-card-hover");
        });
    }

    private void handleEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/EditFormPost.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the post to edit
            EditPostController controller = loader.getController();
            controller.setPostToEdit(post);
            controller.setPostListController(this); // For refreshing after edit

            // Show in modal dialog
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Post");
            stage.setScene(new Scene(root));

            // Add some animations
            stage.setOnShown(e -> {
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            stage.showAndWait();

        } catch (IOException e) {
            showAlert("Error", "Could not load edit form", Alert.AlertType.ERROR);
        }
    }

    private void handleDeletePost(Post post) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Post");
        confirmation.setContentText("Are you sure you want to delete this post?");

        DialogPane dialogPane = confirmation.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Front/posts.css").toExternalForm());

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.delete(post);

                // Reload posts with animation
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), postsContainer);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.3);
                fadeOut.setOnFinished(e -> {
                    try {
                        loadPosts();
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), postsContainer);
                        fadeIn.setFromValue(0.3);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                fadeOut.play();

                showAlert("Success", "Post deleted successfully", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete post", Alert.AlertType.ERROR);
            }
        }
    }

    public void refreshPosts() {
        // Add smooth transition when refreshing
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), postsContainer);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);
        fadeOut.setOnFinished(e -> {
            loadPosts();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), postsContainer);
            fadeIn.setFromValue(0.7);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/Front/posts.css").toExternalForm());

        alert.showAndWait();
    }

    private void showError(String message) {
        postsContainer.getChildren().clear();
        Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add("error-message");
        postsContainer.getChildren().add(errorLabel);
    }

    private void showMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        postsContainer.getChildren().add(label);
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply custom styling to the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/front/alert.css").toExternalForm());
        dialogPane.getStyleClass().add("success-alert");

        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply custom styling to the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/front/alert.css").toExternalForm());
        dialogPane.getStyleClass().add("error-alert");

        alert.showAndWait();
    }
}