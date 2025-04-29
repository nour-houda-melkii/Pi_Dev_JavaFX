package utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationManager {

    public enum NotificationType {
        SUCCESS("#2ecc71", "white"),
        ERROR("#e74c3c", "white"),
        INFO("#3498db", "white"),
        WARNING("#f39c12", "white");

        private final String backgroundColor;
        private final String textColor;

        NotificationType(String backgroundColor, String textColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public String getTextColor() {
            return textColor;
        }
    }

    private static final int NOTIFICATION_DURATION = 3000; // ms
    private static Popup currentPopup;

    // Méthode pour afficher une notification dans un coin de la fenêtre principale
    public static void showPopupNotification(Stage ownerStage, String message, NotificationType type) {
        Platform.runLater(() -> {
            if (currentPopup != null && currentPopup.isShowing()) {
                currentPopup.hide();
            }

            Label label = new Label(message);
            label.setStyle(
                    "-fx-background-color: " + type.getBackgroundColor() + ";" +
                            "-fx-text-fill: " + type.getTextColor() + ";" +
                            "-fx-padding: 10px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-min-width: 250px;" +
                            "-fx-max-width: 250px;" +
                            "-fx-alignment: center-left;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);"
            );
            label.setWrapText(true);

            StackPane pane = new StackPane(label);
            pane.setAlignment(Pos.CENTER);
            pane.setStyle("-fx-background-color: transparent;");

            currentPopup = new Popup();
            currentPopup.getContent().add(pane);
            currentPopup.setAutoHide(true);

            // Positionnement en haut à droite
            double centerX = ownerStage.getX() + ownerStage.getWidth() - 275;
            double centerY = ownerStage.getY() + 50;

            currentPopup.show(ownerStage, centerX, centerY);

            // Programmation de la disparition
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(NOTIFICATION_DURATION),
                            event -> currentPopup.hide())
            );
            timeline.play();
        });
    }

    // Méthode pour afficher une notification dans le label existant
    public static void showInlineNotification(Label notificationLabel, String message, NotificationType type) {
        Platform.runLater(() -> {
            if (notificationLabel == null) {
                System.err.println("notificationLabel est null!");
                return;
            }

            notificationLabel.setText(message);
            notificationLabel.setStyle(
                    "-fx-background-color: " + type.getBackgroundColor() + ";" +
                            "-fx-text-fill: " + type.getTextColor() + ";" +
                            "-fx-padding: 10px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 5px;"
            );
            notificationLabel.setVisible(true);
            notificationLabel.setManaged(true);

            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.seconds(0), new KeyValue(notificationLabel.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(3), e -> {}),
                    new KeyFrame(Duration.seconds(5),
                            e -> {
                                notificationLabel.setVisible(false);
                                notificationLabel.setManaged(false);
                            },
                            new KeyValue(notificationLabel.opacityProperty(), 0.0))
            );
            fadeOut.play();
        });
    }

    // Méthode pour afficher une notification dans une nouvelle fenêtre
    public static void showToastNotification(String message, NotificationType type) {
        Platform.runLater(() -> {
            Stage toastStage = new Stage();
            toastStage.initStyle(StageStyle.TRANSPARENT);

            Label label = new Label(message);
            label.setStyle(
                    "-fx-background-color: " + type.getBackgroundColor() + ";" +
                            "-fx-text-fill: " + type.getTextColor() + ";" +
                            "-fx-padding: 15px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-min-width: 250px;" +
                            "-fx-max-width: 350px;" +
                            "-fx-alignment: center;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);"
            );
            label.setWrapText(true);

            StackPane root = new StackPane(label);
            root.setStyle("-fx-background-color: transparent;");
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            toastStage.setScene(scene);
            toastStage.setAlwaysOnTop(true);
            toastStage.show();

            // Position de la notification
            toastStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 350);
            toastStage.setY(50);

            // Programmer la fermeture avec animation
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(0), new KeyValue(root.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.seconds(0.5), new KeyValue(root.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(3), new KeyValue(root.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(4),
                            e -> toastStage.close(),
                            new KeyValue(root.opacityProperty(), 0.0))
            );
            timeline.play();
        });
    }
}