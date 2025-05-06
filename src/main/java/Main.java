import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controller.MainController;

public class main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Démarrage de l'application...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            if (controller == null) {
                System.err.println("ERREUR: Le contrôleur n'a pas été chargé!");
            } else {
                System.out.println("Contrôleur principal chargé avec succès");
            }

            Scene scene = new Scene(root);
            primaryStage.setTitle("SAHATECK - Gestion des Réclamations");
            primaryStage.setScene(scene);
            primaryStage.setFullScreen(true);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("ERREUR lors du démarrage de l'application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}