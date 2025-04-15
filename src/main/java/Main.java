import controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            Parent root = loader.load();

            // Vérifiez que le contrôleur est bien chargé
            MainController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller not initialized");
            }

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Gestion des Réclamations");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur au démarrage: " + e.getMessage());
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}