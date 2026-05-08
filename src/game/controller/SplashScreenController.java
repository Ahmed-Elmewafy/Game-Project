package game.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

import game.Main;

public class SplashScreenController implements Initializable {

    @FXML
    private ProgressBar SplashScreenLoadingBar;

    private static final String[] FONTS = {
        "/game/view/FuturaExtraBold.ttf",
        "/game/view/MonsterAG.ttf"
    };

    private static final String[] FXMLS = {
        "/game/view/MainMenu.fxml",
        "/game/view/PlayerType.fxml",
        "/game/view/OpponentType.fxml",
        "/game/view/Instructions.fxml"
    };

    private static final String[] IMAGES = {
        "/game/resources/images/Background/MainMenuBackground.png",
        "/game/resources/images/Background/SplashScreenBackground.png",
        "/game/resources/images/Background/LoadingScreenBackground.png",
        "/game/resources/images/Background/ScarersClosedDoor.png",
        "/game/resources/images/Background/ScarersOpenedDoor.png",
        "/game/resources/images/Background/LaughersClosedDoor.png",
        "/game/resources/images/Background/LaughersOpenedDoor.png",
        "/game/resources/images/Monsters/James P. Sullivan.jpg",
        "/game/resources/images/Monsters/Mike Wazowski.jpg",
        "/game/resources/images/Monsters/Randall Boggs.png",
        "/game/resources/images/Monsters/Celia Mae.png",
        "/game/resources/images/Monsters/Roz.png",
        "/game/resources/images/Monsters/Fungus.jpg",
        "/game/resources/images/Monsters/Henry J. Waternoose.png",
        "/game/resources/images/Monsters/Yeti.png"
    };

    private static final String[] OST = {
        "/game/resources/audio/ost/StartMenu.mp3",
        "/game/resources/audio/ost/MonsterSelection.mp3"
    };

    private static final String[] SFX = {
        "/game/resources/audio/sound effects/OpenDoor.mp3",
        "/game/resources/audio/sound effects/CloseDoor.mp3",
        "/game/resources/audio/sound effects/ScarerDoorScream.mp3",
        "/game/resources/audio/sound effects/LaugherDoorLaugh.mp3"
    };

    private static final String[] VOICE_LINES = {
        "/game/resources/audio/voice lines/SulleyAlly.mp3",
        "/game/resources/audio/voice lines/SulleyEnemy.mp3",
        "/game/resources/audio/voice lines/MikeAlly.mp3",
        "/game/resources/audio/voice lines/MikeEnemy.mp3",
        "/game/resources/audio/voice lines/RandallAlly.mp3",
        "/game/resources/audio/voice lines/RandallEnemy.mp3",
        "/game/resources/audio/voice lines/CeliaAlly.mp3",
        "/game/resources/audio/voice lines/CeliaEnemy.mp3",
        "/game/resources/audio/voice lines/RozAlly.mp3",
        "/game/resources/audio/voice lines/RozEnemy.mp3",
        "/game/resources/audio/voice lines/FungusAlly.mp3",
        "/game/resources/audio/voice lines/FungusEnemy.mp3",
        "/game/resources/audio/voice lines/WaternooseAlly.mp3",
        "/game/resources/audio/voice lines/WaternooseEnemy.mp3",
        "/game/resources/audio/voice lines/YetiAlly.mp3",
        "/game/resources/audio/voice lines/YetiEnemy.mp3"
    };

    private static final int TOTAL_ASSETS =
        FONTS.length + FXMLS.length + IMAGES.length + OST.length + SFX.length + VOICE_LINES.length;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SplashScreenLoadingBar.setProgress(0.0);

        // Fonts MUST be loaded on the JavaFX Application Thread before
        // any scene that uses them is created. We do this synchronously
        // here in initialize() which always runs on the FX thread.
        for (String path : FONTS) {
            loadFont(path);
        }

        startLoadingTask();
    }

    private void startLoadingTask() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int loaded = 0;

                // Fonts already loaded in initialize() — count them as done
                loaded += FONTS.length;
                updateProgress(loaded, TOTAL_ASSETS);

                for (String path : FXMLS) {
                    preloadFXML(path);
                    loaded++;
                    updateProgress(loaded, TOTAL_ASSETS);
                    Thread.sleep(40);
                }

                for (String path : IMAGES) {
                    loadImage(path);
                    loaded++;
                    updateProgress(loaded, TOTAL_ASSETS);
                    Thread.sleep(25);
                }

                for (String path : OST) {
                    loadAudio(path);
                    loaded++;
                    updateProgress(loaded, TOTAL_ASSETS);
                    Thread.sleep(60);
                }

                for (String path : SFX) {
                    loadAudio(path);
                    loaded++;
                    updateProgress(loaded, TOTAL_ASSETS);
                    Thread.sleep(40);
                }

                for (String path : VOICE_LINES) {
                    loadAudio(path);
                    loaded++;
                    updateProgress(loaded, TOTAL_ASSETS);
                    Thread.sleep(30);
                }

                return null;
            }
        };

        SplashScreenLoadingBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> onLoadingComplete());
        task.setOnFailed(e -> {
            if (task.getException() != null)
                System.err.println("Splash loading error: " + task.getException().getMessage());
            transitionToMainMenu();
        });

        Thread thread = new Thread(task, "splash-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void onLoadingComplete() {
        SplashScreenLoadingBar.progressProperty().unbind();
        SplashScreenLoadingBar.setProgress(1.0);
        ResourceManager.playOST("/game/resources/audio/ost/StartMenu.mp3", 0.35);

        Thread delay = new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            Platform.runLater(this::transitionToMainMenu);
        });
        delay.setDaemon(true);
        delay.start();
    }

    private void transitionToMainMenu() {
        FadeTransition fade = new FadeTransition(
            Duration.millis(800),
            SplashScreenLoadingBar.getScene().getRoot()
        );
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> Main.switchScene("/game/view/MainMenu.fxml"));
        fade.play();
    }

    private void loadFont(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("WARN: Font not found: " + path);
                return;
            }
            Font font = Font.loadFont(url.toExternalForm(), 14);
            if (font == null)
                System.err.println("WARN: Font.loadFont returned null for: " + path);
            else
                System.out.println("Font loaded: " + font.getFamily() + " / " + font.getName());
        } catch (Exception e) {
            System.err.println("WARN: Could not load font " + path + ": " + e.getMessage());
        }
    }

    private void preloadFXML(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("WARN: FXML not found: " + path);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            loader.load();
        } catch (Exception e) {
            System.err.println("WARN: Could not preload FXML " + path + ": " + e.getMessage());
        }
    }

    private void loadImage(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("WARN: Image not found: " + path);
                return;
            }
            Image image = new Image(url.toExternalForm(), 0, 0, true, true, false);
            ResourceManager.storeImage(path, image);
        } catch (Exception e) {
            System.err.println("WARN: Could not load image " + path + ": " + e.getMessage());
        }
    }

    private void loadAudio(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("WARN: Audio not found: " + path);
                return;
            }
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            ResourceManager.storeAudio(path, player);
        } catch (Exception e) {
            System.err.println("WARN: Could not load audio " + path + ": " + e.getMessage());
        }
    }
}
