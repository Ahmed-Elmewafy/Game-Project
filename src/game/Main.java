package game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

public class Main extends Application {

    private static Stage primaryStage;

    private static final double DESIGN_WIDTH  = 1280.0;
    private static final double DESIGN_HEIGHT = 720.0;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("DooR DasH: Scare vs Laugh Touchdown");
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        switchScene("/game/view/SplashScreen.fxml");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent content = loader.load();
            primaryStage.setScene(buildScene(content));
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static <T> T switchSceneAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent content = loader.load();
            primaryStage.setScene(buildScene(content));
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    private static Scene buildScene(Parent content) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double screenW = screen.getWidth();
        double screenH = screen.getHeight();

        double scale  = Math.min(screenW / DESIGN_WIDTH, screenH / DESIGN_HEIGHT);

        double scaledW = DESIGN_WIDTH  * scale;
        double scaledH = DESIGN_HEIGHT * scale;
        double offsetX = (screenW - scaledW) / 2.0;
        double offsetY = (screenH - scaledH) / 2.0;

        content.getTransforms().setAll(new Scale(scale, scale, 0, 0));

        // Use a plain Pane as wrapper — unlike StackPane it does not
        // re-center children, so our manual translate offsets are respected
        Pane wrapper = new Pane(content);
        wrapper.setStyle("-fx-background-color: black;");
        wrapper.setPrefSize(screenW, screenH);

        content.setTranslateX(offsetX);
        content.setTranslateY(offsetY);

        return new Scene(wrapper, screenW, screenH);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
