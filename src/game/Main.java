package game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("DooR DasH: Scare vs Laugh Touchdown");
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        switchScene("/game/view/SplashScreen.fxml");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void switchScene(String fxmlPath) {
        try {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static <T> T switchSceneAndGetController(String fxmlPath) {
        try {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            primaryStage.setScene(scene);
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
