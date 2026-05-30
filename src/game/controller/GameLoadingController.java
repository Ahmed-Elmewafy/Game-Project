package game.controller;

import game.Main;
import game.engine.Game;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * GameLoadingController
 *
 * Changes:
 *  - Only text shown during loading: "Loading…" (no step labels)
 *  - Loading bar is short (280px) and sits bottom-right
 *  - Total loading time ~6 seconds so the player can read the rules
 *  - On complete: loadingGroup (bar + text) fades OUT, then continueBox fades IN
 *  - Click anywhere after loading → fade out → Board
 */
public class GameLoadingController implements Initializable {

    @FXML private HBox        loadingGroup;     // wraps bar + "Loading…" text
    @FXML private ProgressBar loadingBar;
    @FXML private Text        loadingStatusText;
    @FXML private StackPane   continueBox;
    @FXML private TextFlow    controlsTextFlow;

    private static Game game;
    public static void setGame(Game gameInstance) { game = gameInstance; }

    private boolean loadingDone = false;

    // 20 equal steps × 300ms each = 6 seconds total
    private static final int STEP_COUNT  = 20;
    private static final int STEP_SLEEP  = 300; // ms per step

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadingBar.setProgress(0.0);
        loadingGroup.setOpacity(1.0);
        continueBox.setOpacity(0.0);
        setupKeyboardControlsTips();
        startLoadingTask();
    }

    // ── Loading Task ───────────────────────────────────────────────────────

    private void startLoadingTask() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                preloadBoardResources();
                for (int i = 1; i <= STEP_COUNT; i++) {
                    Thread.sleep(STEP_SLEEP);
                    updateProgress(i, STEP_COUNT);
                }
                return null;
            }
        };

        loadingBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> onLoadingComplete());
        task.setOnFailed(e -> {
            if (task.getException() != null)
                System.err.println("GameLoading error: " + task.getException().getMessage());
            onLoadingComplete();
        });

        Thread t = new Thread(task, "game-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── On complete: fade out bar, then fade in continue prompt ───────────

    private void onLoadingComplete() {
        Platform.runLater(() -> {
            loadingBar.progressProperty().unbind();
            loadingBar.setProgress(1.0);

            // Fade OUT the loading group (bar + "Loading…" text)
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), loadingGroup);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                loadingGroup.setVisible(false);
                loadingDone = true;
                showContinuePrompt();
            });
            fadeOut.play();
        });
    }

    private void showContinuePrompt() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), continueBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // ── Click to continue ──────────────────────────────────────────────────

    @FXML
    private void onScreenClicked() {
        if (!loadingDone) return;

        BoardController.setGame(game);

        FadeTransition fade = new FadeTransition(Duration.millis(500),
                Main.getCurrentContent());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> Main.switchScene("/game/view/Board.fxml"));
        fade.play();
    }

    private void setupKeyboardControlsTips() {
        if (controlsTextFlow == null) return;
        boolean vsCpu = BoardController.isVsComputer();
        
        Text heading = new Text("Keyboard Controls:\n");
        heading.getStyleClass().add("hl-gold");
        
        Text bullet1 = new Text("• ");
        bullet1.getStyleClass().add("hl-gold");
        
        controlsTextFlow.getChildren().addAll(heading, bullet1);
        
        if (vsCpu) {
            Text p1Label = new Text("Player (Blue): ");
            p1Label.getStyleClass().add("body");
            Text p1Keys = new Text("Space / A");
            p1Keys.getStyleClass().add("hl");
            Text p1Mid = new Text(" to play turn, ");
            p1Mid.getStyleClass().add("body");
            Text p1Power = new Text("S");
            p1Power.getStyleClass().add("hl");
            Text p1End = new Text(" to use Powerup.");
            p1End.getStyleClass().add("body");
            controlsTextFlow.getChildren().addAll(p1Label, p1Keys, p1Mid, p1Power, p1End);
        } else {
            Text p1Label = new Text("Player 1 (Blue): ");
            p1Label.getStyleClass().add("body");
            Text p1Keys = new Text("Space / A");
            p1Keys.getStyleClass().add("hl");
            Text p1Mid = new Text(" to play turn, ");
            p1Mid.getStyleClass().add("body");
            Text p1Power = new Text("S");
            p1Power.getStyleClass().add("hl");
            Text p1End = new Text(" to use Powerup.\n");
            p1End.getStyleClass().add("body");
            
            Text bullet2 = new Text("• ");
            bullet2.getStyleClass().add("hl-gold");
            
            Text p2Label = new Text("Player 2 (Red):  ");
            p2Label.getStyleClass().add("body");
            Text p2Keys = new Text("Enter / K");
            p2Keys.getStyleClass().add("hl");
            Text p2Mid = new Text(" to play turn, ");
            p2Mid.getStyleClass().add("body");
            Text p2Power = new Text("L");
            p2Power.getStyleClass().add("hl");
            Text p2End = new Text(" to use Powerup.");
            p2End.getStyleClass().add("body");
            
            controlsTextFlow.getChildren().addAll(p1Label, p1Keys, p1Mid, p1Power, p1End, bullet2, p2Label, p2Keys, p2Mid, p2Power, p2End);
        }
    }

    private void preloadBoardResources() {
        // Closed doors
        String[] closedDoors = {
            "Orange1.png","BabyBlue3.png","Blue5.png","DarkGreen7.png","DarkGrey9.png",
            "BabyBlue11.png","Purple13.png","Blue15.png","SemiGrey17.png","Orange19.png",
            "Yellow21.png","BabyBlue23.png","Blue25.png","Green27.png","DarkGrey29.png",
            "DarkGrey31.png","Green33.png","Blue35.png","Pink37.png","Yellow39.png",
            "Orange41.png","SemiGrey43.png","Blue45.png","Pink47.png","Purple49.png",
            "Purple51.png","Pink53.png","Blue55.png","SemiGrey57.png","Orange59.png",
            "Orange61.png","SemiGrey63.png","Blue65.png","Pink67.png","Purple69.png",
            "DarkGrey71.png","Green73.png","Blue75.png","BabyBlue77.png","Yellow79.png",
            "Yellow81.png","Purple83.png","Blue85.png","Green87.png","DarkGrey89.png",
            "Orange91.png","SemiGrey93.png","Blue95.png","BabyBlue97.png","Boo99.png"
        };
        for (String file : closedDoors) {
            preloadImage("/game/resources/images/Board/doors/Closed/" + file);
        }

        // Opened doors
        String[] openedDoors = {
            "LighterBlue.png", "Blue.png", "Green.png", "DarkGrey.png", "Orange.png",
            "Pink.png", "Purple.png", "SemiGray.png", "Yellow.png", "Boo.png"
        };
        for (String file : openedDoors) {
            preloadImage("/game/resources/images/Board/doors/Opened/" + file);
        }

        // Conveyor belts & transport
        preloadImage("/game/resources/images/Board/transportation/ConveyorBeltShort.png");
        preloadImage("/game/resources/images/Board/transportation/ConveyorBeltLong.png");
        preloadImage("/game/resources/images/Board/UI/Cannister.png");
        preloadImage("/game/resources/images/Board/Cards/Unkown card.png");

        // Socks
        String[] socks = {"32_26.png", "42_30.png", "74_64.png", "95_70.png", "97_76.png"};
        for (String file : socks) {
            preloadImage("/game/resources/images/Board/transportation/" + file);
        }
    }

    private void preloadImage(String path) {
        try {
            if (ResourceManager.getImage(path) != null) return;
            URL url = getClass().getResource(path);
            if (url != null) {
                javafx.scene.image.Image img = new javafx.scene.image.Image(url.toExternalForm(), 0, 0, true, true, false);
                ResourceManager.storeImage(path, img);
            }
        } catch (Exception ignored) {}
    }
}
