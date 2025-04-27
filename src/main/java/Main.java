import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class main extends Application {

    public static void main(String[] args) {
        System.out.println("Démarrage de l'application...");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/main-view.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("SAHATECK");
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("ERREUR: Impossible de charger le FXML");
            e.printStackTrace();
            throw e;
        }
    }
}