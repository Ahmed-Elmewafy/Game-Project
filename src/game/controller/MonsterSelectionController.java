package game.controller;

import game.Main;
import game.engine.Game;
import game.engine.monsters.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the MonsterSelection screen.
 *
 * Design Pattern: MVC — Controller layer.
 * Animation chain (chained via setOnFinished / PauseTransition):
 *   1. Player panel slides in from LEFT → plays Ally voice line
 *   2. 1-second pause
 *   3. Opponent panel slides in from RIGHT → plays Ally voice line
 *   4. "Click anywhere to continue" fades in bottom-right
 *
 * Changes vs previous version:
 *  - Removed PLAYER/OPPONENT banner text (background image already has it)
 *  - Panels are CENTER-aligned vertically
 *  - Energy text has no emoji symbol prefix
 *  - continueBox is bottom-right, white, high-visibility
 *  - 1-second PauseTransition between player slide and opponent slide
 *  - Both player AND opponent use Ally voice lines
 *  - Voice lines already preloaded by SplashScreenController via ResourceManager
 */
public class MonsterSelectionController implements Initializable {

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML private VBox      playerPanel;
    @FXML private VBox      opponentPanel;
    @FXML private ImageView playerImage;
    @FXML private ImageView opponentImage;
    @FXML private Text      playerName;
    @FXML private Text      playerRole;
    @FXML private Text      playerType;
    @FXML private Text      playerEnergy;
    @FXML private Text      playerDesc;
    @FXML private Text      opponentName;
    @FXML private Text      opponentRole;
    @FXML private Text      opponentType;
    @FXML private Text      opponentEnergy;
    @FXML private Text      opponentDesc;
    @FXML private StackPane continueBox;

    // ── Shared game reference ─────────────────────────────────────────────────
    private static Game game;
    public static void setGame(Game gameInstance) { game = gameInstance; }

    // ── Resource paths ────────────────────────────────────────────────────────
    private static final String VOICE_BASE = "/game/resources/audio/voice lines/";

    /** Always use Ally voice line for both player and opponent. */
    private static String allyVoicePath(Monster monster) {
        return VOICE_BASE + shortName(monster) + "Ally.mp3";
    }

    /** Derive audio filename prefix from monster's full name. */
    private static String shortName(Monster monster) {
        String fullName = monster.getName();
        if (fullName.contains("Sullivan"))   return "Sulley";
        if (fullName.contains("Wazowski"))   return "Mike";
        if (fullName.contains("Randall"))    return "Randall";
        if (fullName.contains("Celia"))      return "Celia";
        if (fullName.contains("Roz"))        return "Roz";
        if (fullName.contains("Fungus"))     return "Fungus";
        if (fullName.contains("Waternoose")) return "Waternoose";
        if (fullName.contains("Yeti"))       return "Yeti";
        return fullName.replaceAll("\\s.*", "");
    }

    /** Monster image path — tries .jpg, falls back to .png. */
    private static String imagePath(Monster monster) {
        return "/game/resources/images/Monsters/" + monster.getName() + ".jpg";
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (game == null) return;

        populatePanel(game.getPlayer(),
                      playerImage, playerName, playerRole,
                      playerType, playerEnergy, playerDesc);

        populatePanel(game.getOpponent(),
                      opponentImage, opponentName, opponentRole,
                      opponentType, opponentEnergy, opponentDesc);

        // Both panels start fully off-screen; continue prompt invisible
        playerPanel.setTranslateX(-1400);
        opponentPanel.setTranslateX(1400);
        continueBox.setOpacity(0.0);

        Platform.runLater(this::animatePlayerIn);
    }

    // ── Panel population ──────────────────────────────────────────────────────

    private void populatePanel(Monster monster,
                               ImageView imageView,
                               Text nameText, Text roleText,
                               Text typeText, Text energyText,
                               Text descText) {

        // Image: try jpg first, then png
        Image monsterImage = ResourceManager.getImage(imagePath(monster));
        if (monsterImage == null)
            monsterImage = ResourceManager.getImage(imagePath(monster).replace(".jpg", ".png"));
        if (monsterImage != null)
            imageView.setImage(monsterImage);

        nameText.setText(monster.getName());

        // Role colour: red for SCARER, cyan for LAUGHER
        String roleLabel = monster.getOriginalRole().toString();
        roleText.setText(roleLabel);
        roleText.setStyle(roleLabel.equals("SCARER")
                ? "-fx-fill: #ef4444;"
                : "-fx-fill: #38c7ff;");

        typeText.setText(monsterTypeName(monster));

        // Change 3: no emoji/symbol before energy text
        energyText.setText("Starting Energy: " + monster.getEnergy());

        descText.setText(monster.getDescription());
    }

    private String monsterTypeName(Monster monster) {
        if (monster instanceof Dasher)      return "Dasher";
        if (monster instanceof Dynamo)      return "Dynamo";
        if (monster instanceof MultiTasker) return "MultiTasker";
        if (monster instanceof Schemer)     return "Schemer";
        return "Unknown";
    }

    // ── Animation chain ───────────────────────────────────────────────────────

    /** Step 1: slide player in from left. */
    private void animatePlayerIn() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), playerPanel);
        slide.setFromX(-1400);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        // After slide → play player's Ally voice line → 1 second pause → opponent
        slide.setOnFinished(event -> playAllyVoiceLine(game.getPlayer(), this::pauseThenOpponent));
        slide.play();
    }

    /** Step 2: 1-second pause between player and opponent appearances. */
    private void pauseThenOpponent() {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> animateOpponentIn());
        pause.play();
    }

    /** Step 3: slide opponent in from right. */
    private void animateOpponentIn() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), opponentPanel);
        slide.setFromX(1400);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        // After slide → play opponent's Ally voice line → show continue prompt
        slide.setOnFinished(event -> playAllyVoiceLine(game.getOpponent(), this::showContinuePrompt));
        slide.play();
    }

    /** Step 4: fade in the continue prompt. */
    private void showContinuePrompt() {
        FadeTransition fade = new FadeTransition(Duration.millis(600), continueBox);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Plays the Ally voice line for the given monster (used for both player and opponent).
     * Fires onDone when finished, or after a short pause if the audio isn't cached.
     */
    private void playAllyVoiceLine(Monster monster, Runnable onDone) {
        String voicePath = allyVoicePath(monster);
        MediaPlayer voiceLine = ResourceManager.getAudio(voicePath);

        if (voiceLine != null) {
            // Wrap onDone so it can only fire once, even if both the MediaPlayer
            // callback and the timeout thread race to call it.
            java.util.concurrent.atomic.AtomicBoolean fired =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            Runnable safeOnDone = () -> {
                if (fired.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        voiceLine.setOnEndOfMedia(null);
                        voiceLine.setOnError(null);
                        onDone.run();
                    });
                }
            };

            voiceLine.stop();
            voiceLine.seek(voiceLine.getStartTime());
            voiceLine.setCycleCount(1);
            voiceLine.setVolume(1.0);
            voiceLine.setOnEndOfMedia(safeOnDone);
            voiceLine.setOnError(safeOnDone);
            voiceLine.play();

            // Safety timeout: if MediaPlayer never fires onEndOfMedia (can happen
            // when reusing MediaPlayer instances across scenes), force-fire after 8s.
            Thread timeout = new Thread(() -> {
                try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
                safeOnDone.run();
            }, "voice-timeout");
            timeout.setDaemon(true);
            timeout.start();

        } else {
            // Audio not cached — short pause then continue
            PauseTransition pause = new PauseTransition(Duration.millis(800));
            pause.setOnFinished(event -> onDone.run());
            pause.play();
        }
    }

    // ── User interaction ──────────────────────────────────────────────────────

    @FXML
    private void onScreenClicked() {
        // Only respond once the continue prompt is fully visible
        if (continueBox.getOpacity() < 0.9) return;

        // Pass the game to the loading screen controller and switch
        GameLoadingController.setGame(game);
        Main.switchScene("/game/view/GameLoading.fxml");
    }
}
