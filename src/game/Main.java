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
    private static Parent currentContent;

    private static final double DESIGN_WIDTH  = 1280.0;
    private static final double DESIGN_HEIGHT = 720.0;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("DooR DasH: Scare vs Laugh Touchdown");
        java.io.InputStream iconStream = Main.class.getResourceAsStream("/game/resources/images/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        switchScene("/game/view/SplashScreen.fxml");
        primaryStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    public static Parent getCurrentContent() {
        return currentContent;
    }

    private static Scene mainScene;
    private static Pane rootWrapper;

    public static void switchScene(String fxmlPath) {
        try {
            if (mainScene != null) {
                mainScene.setOnKeyPressed(null);
            }
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent content = loader.load();
            currentContent = content;
            updateScene(content);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
            primaryStage.setFullScreen(true);
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static <T> T switchSceneAndGetController(String fxmlPath) {
        try {
            if (mainScene != null) {
                mainScene.setOnKeyPressed(null);
            }
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent content = loader.load();
            currentContent = content;
            updateScene(content);	
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
            primaryStage.setFullScreen(true);
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    private static void updateScene(Parent content) {
        Rectangle2D screen = Screen.getPrimary().getBounds();
        double screenW = screen.getWidth();
        double screenH = screen.getHeight();

        double scale  = Math.min(screenW / DESIGN_WIDTH, screenH / DESIGN_HEIGHT);

        double scaledW = DESIGN_WIDTH  * scale;
        double scaledH = DESIGN_HEIGHT * scale;
        double offsetX = (screenW - scaledW) / 2.0;
        double offsetY = (screenH - scaledH) / 2.0;

        content.getTransforms().setAll(new Scale(scale, scale, 0, 0));
        content.setTranslateX(offsetX);
        content.setTranslateY(offsetY);

        if (mainScene == null) {
            // Use a plain Pane as wrapper — unlike StackPane it does not
            // re-center children, so our manual translate offsets are respected
            rootWrapper = new Pane(content);
            rootWrapper.setStyle("-fx-background-color: black;");
            rootWrapper.setPrefSize(screenW, screenH);
            mainScene = new Scene(rootWrapper, screenW, screenH);
            primaryStage.setScene(mainScene);
        } else {
            rootWrapper.getChildren().setAll(content);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
