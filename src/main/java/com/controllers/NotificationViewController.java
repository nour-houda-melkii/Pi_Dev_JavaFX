package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.List;

public class NotificationViewController {

    @FXML
    private ListView<String> notificationListView;

    @FXML
    private Button closeButton;

    public void initialize() {
        if (closeButton != null) {
            closeButton.setOnAction(e -> {
                Stage stage = (Stage) closeButton.getScene().getWindow();
                stage.close();
            });
        }
    }

    public void setNotifications(List<String> notifications) {
        if (notificationListView != null) {
            notificationListView.getItems().clear();
            notificationListView.getItems().addAll(notifications);
        }
    }
}