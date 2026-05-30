package game.controller;

import game.Main;
import game.engine.Game;
import game.engine.Role;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private ImageView ScarerDoorClosed;
    @FXML private ImageView LaugherClosedDoor;
    @FXML private Button musicBtn;
    @FXML private StackPane popupOverlay;
    @FXML private VBox popupContent;

    private static final String SCARER_CLOSED  = "/game/resources/images/Background/ScarersClosedDoor.png";
    private static final String SCARER_OPENED  = "/game/resources/images/Background/ScarersOpenedDoor.png";
    private static final String LAUGHER_CLOSED = "/game/resources/images/Background/LaughersClosedDoor.png";
    private static final String LAUGHER_OPENED = "/game/resources/images/Background/LaughersOpenedDoor.png";

    private static final String SFX_OPEN   = "/game/resources/audio/sound effects/OpenDoor.mp3";
    private static final String SFX_CLOSE  = "/game/resources/audio/sound effects/CloseDoor.mp3";
    private static final String SFX_SCREAM = "/game/resources/audio/sound effects/ScarerDoorScream.mp3";
    private static final String SFX_LAUGH  = "/game/resources/audio/sound effects/LaugherDoorLaugh.mp3";
    private static final String SFX_CHANGE = "/game/resources/audio/sound effects/ChangeMusic.mp3";

    private boolean transitioning = false;

    // Music popup state
    private Popup musicPopup = null;
    private int currentModeIndex = 0;  // 0=Original, 1=Custom, 2=Off
    private String confirmingMode = null;
    private boolean sliderCooldown = false;
    private Rectangle sliderThumb;
    private Text[] optionLabels;
    private static final String[] OPTION_LABELS = {"Original", "Alternate", "Off"};
    private static final double TRACK_W = 300, TRACK_H = 36, THUMB_W = 100;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_CLOSED));
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_CLOSED));
    }

    // ── Music button ──────────────────────────────────────────────────────

    @FXML private void onMusicButtonClicked() {
        if (musicPopup != null && musicPopup.isShowing()) {
            musicPopup.hide();
            musicPopup = null;
            return;
        }
        showMusicPopup();
    }

    @FXML private void onExitButtonClicked() {
        Platform.exit();
        System.exit(0);
    }

    private void showMusicPopup() {
        // Sync the slider index with the global music state before building the UI
        ResourceManager.MusicMode mode = ResourceManager.getMusicMode();
        if (mode == ResourceManager.MusicMode.ORIGINAL) {
            currentModeIndex = 0;
        } else if (mode == ResourceManager.MusicMode.ALTERNATE) {
            currentModeIndex = 1;
        } else {
            currentModeIndex = 2;
        }

        musicPopup = new Popup();
        musicPopup.setAutoHide(true);

        // ── Popup content ──────────────────────────────────────────────────
        VBox root = new VBox(16);
        root.getStylesheets().add(getClass().getResource("/game/view/MainMenu.css").toExternalForm());
        root.setAlignment(Pos.CENTER);
        root.setMinWidth(356);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setStyle(
            "-fx-background-color: rgba(8, 12, 35, 0.97);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(56, 199, 255, 0.65);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 16;"
        );

        // Title
        Text title = new Text("♪  Audio Settings");
        title.setFont(Font.font("Futura", 18));
        title.setFill(Color.web("#38c7ff"));

        // Current mode label (declared first so slider click handler can reference it)
        Text modeLabel = new Text(getModeDescription());
        modeLabel.setFont(Font.font("Futura", 11));
        modeLabel.setFill(Color.web("#94a3b8"));
        modeLabel.setWrappingWidth(300);
        modeLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 3-way slider — pass modeLabel via userData so click handler can refresh it
        StackPane sliderPane = buildSlider();
        sliderPane.setUserData(modeLabel);

        // Music Volume Slider
        VBox musicVolBox = new VBox(4);
        musicVolBox.setAlignment(Pos.CENTER);
        Label musicVolLabel = new Label("Music Volume");
        musicVolLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        Slider musicSlider = new Slider(0, 1, ResourceManager.getMusicVolume());
        musicSlider.getStyleClass().add("settings-slider");
        musicSlider.setMaxWidth(220);
        musicSlider.valueProperty().addListener((obs, oldVal, newVal) -> ResourceManager.setMusicVolume(newVal.doubleValue()));
        musicVolBox.getChildren().addAll(musicVolLabel, musicSlider);

        // SFX Volume Slider
        VBox sfxVolBox = new VBox(4);
        sfxVolBox.setAlignment(Pos.CENTER);
        Label sfxVolLabel = new Label("SFX Volume");
        sfxVolLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        Slider sfxSlider = new Slider(0, 1, ResourceManager.getSfxVolume());
        sfxSlider.getStyleClass().add("settings-slider");
        sfxSlider.setMaxWidth(220);
        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> ResourceManager.setSfxVolume(newVal.doubleValue()));
        sfxVolBox.getChildren().addAll(sfxVolLabel, sfxSlider);

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
            "-fx-font-family: Futura; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: white; -fx-background-color: rgba(29,78,216,0.8);" +
            "-fx-background-radius: 7; -fx-padding: 6 20 6 20; -fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> { musicPopup.hide(); musicPopup = null; });

        root.getChildren().addAll(title, sliderPane, modeLabel, musicVolBox, sfxVolBox, closeBtn);

        musicPopup.getContent().add(root);

        // Center on screen when shown
        musicPopup.setOnShown(e -> {
            Parent c = Main.getCurrentContent();
            if (c != null && c.getScene() != null && c.getScene().getWindow() != null) {
                Window win = c.getScene().getWindow();
                musicPopup.setX(win.getX() + win.getWidth() / 2 - musicPopup.getWidth() / 2);
                musicPopup.setY(win.getY() + win.getHeight() / 2 - musicPopup.getHeight() / 2);
            }
        });
        // Reset reference on hide
        musicPopup.setOnHidden(e -> musicPopup = null);

        // Fade in (pre-set opacity before show to avoid 1-frame flash)
        root.setOpacity(0);

        // Show on current window
        Parent c = Main.getCurrentContent();
        if (c != null && c.getScene() != null && c.getScene().getWindow() != null) {
            musicPopup.show(c.getScene().getWindow());
            FadeTransition ft = new FadeTransition(Duration.millis(200), root);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        } else {
            musicPopup = null;
        }
    }

    private StackPane buildSlider() {
        // Track background
        Rectangle track = new Rectangle(TRACK_W, TRACK_H);
        track.setArcWidth(TRACK_H); track.setArcHeight(TRACK_H);
        track.setFill(Color.rgb(12, 18, 45));
        track.setStroke(Color.rgb(56, 199, 255, 0.45)); track.setStrokeWidth(1.2);

        // Thumb (sliding indicator)
        sliderThumb = new Rectangle(THUMB_W - 4, TRACK_H - 6);
        sliderThumb.setArcWidth(TRACK_H - 6); sliderThumb.setArcHeight(TRACK_H - 6);
        sliderThumb.setFill(Color.rgb(29, 78, 216, 0.9));
        // Start position based on current mode
        sliderThumb.setTranslateX(thumbTargetX(currentModeIndex));

        // Option labels
        optionLabels = new Text[3];
        HBox labelRow = new HBox(0);
        labelRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Text lbl = new Text(OPTION_LABELS[i]);
            lbl.setFont(Font.font("Futura", i == currentModeIndex ? 11 : 9));
            lbl.setFill(i == currentModeIndex ? Color.WHITE : Color.rgb(140, 170, 210, 0.7));
            lbl.setWrappingWidth(THUMB_W);
            lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            optionLabels[i] = lbl;
            labelRow.getChildren().add(lbl);
        }

        StackPane pane = new StackPane(track, sliderThumb, labelRow);
        pane.setMaxWidth(TRACK_W);

        // Click to switch option
        pane.setOnMouseClicked(ev -> {
            if (sliderCooldown) return;
            int zone = Math.max(0, Math.min(2, (int)(ev.getX() / (TRACK_W / 3))));
            if (zone == currentModeIndex) return;
            currentModeIndex = zone;
            animateThumbTo(zone);
            applyMusicMode(zone);
            // Refresh mode label in the popup if still open
            if (pane.getUserData() instanceof Text) {
                Text lbl = (Text) pane.getUserData();
                lbl.setText(getModeDescription());
            }
        });

        return pane;
    }

    private double thumbTargetX(int pos) {
        // Centre the thumb in its zone; StackPane centres at 0, so offset from centre
        double zoneWidth = TRACK_W / 3.0;
        double zoneCentre = pos * zoneWidth + zoneWidth / 2.0;
        return zoneCentre - TRACK_W / 2.0;
    }

    private void animateThumbTo(int pos) {
        if (sliderThumb == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), sliderThumb);
        tt.setToX(thumbTargetX(pos));
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
        // Update label colours
        if (optionLabels != null) {
            for (int i = 0; i < optionLabels.length; i++) {
                optionLabels[i].setFill(
                    i == pos ? Color.WHITE : Color.rgb(140, 170, 210, 0.7)
                );
                optionLabels[i].setFont(Font.font("Futura", i == pos ? 11 : 9));
            }
        }
    }

    private String getModeDescription() {
        if (currentModeIndex == 0) return "Now playing: Original Monsters Inc. OST";
        if (currentModeIndex == 1) return "Now playing: Custom alternate OST";
        return "Music is off";
    }

    private void applyMusicMode(int zone) {
        sliderCooldown = true;

        ResourceManager.MusicMode mode;
        if (zone == 0) mode = ResourceManager.MusicMode.ORIGINAL;
        else if (zone == 1) mode = ResourceManager.MusicMode.ALTERNATE;
        else mode = ResourceManager.MusicMode.OFF;
        ResourceManager.setMusicMode(mode);

        if (mode == ResourceManager.MusicMode.OFF) {
            ResourceManager.stopCurrentOST();
            endCooldown();
        } else {
            // Try to play ChangeMusic.mp3 transition sound, then switch OST
            MediaPlayer changeMusic = ResourceManager.getAudio(SFX_CHANGE);
            if (changeMusic != null) {
                changeMusic.stop();
                changeMusic.seek(changeMusic.getStartTime());
                changeMusic.setCycleCount(1);
                Runnable cleanUp = () -> Platform.runLater(() -> {
                    changeMusic.setOnEndOfMedia(null);
                    changeMusic.setOnError(null);
                    switchOSTToCurrentMode();
                    endCooldown();
                });
                changeMusic.setOnEndOfMedia(cleanUp);
                // Safety timeout: if ChangeMusic is very short or fails, still switch
                changeMusic.setOnError(cleanUp);
                changeMusic.play();
                // Hard timeout fallback: 4s max wait
                PauseTransition timeout = new PauseTransition(Duration.seconds(4));
                timeout.setOnFinished(e -> {
                    if (sliderCooldown) {
                        changeMusic.setOnEndOfMedia(null);
                        changeMusic.setOnError(null);
                        switchOSTToCurrentMode();
                        endCooldown();
                    }
                });
                timeout.play();
            } else {
                // No ChangeMusic file — switch immediately
                switchOSTToCurrentMode();
                endCooldown();
            }
        }
    }

    private void switchOSTToCurrentMode() {
        ResourceManager.stopCurrentOST();
        ResourceManager.playOSTWithMode("StartMenu.mp3", 0.35);
    }

    private void endCooldown() {
        PauseTransition cooldown = new PauseTransition(Duration.millis(800));
        cooldown.setOnFinished(e -> sliderCooldown = false);
        cooldown.play();
    }

    // ── Door interactions ─────────────────────────────────────────────────

    @FXML private void onScarerDoorEntered() {
        if (transitioning) return;
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_OPENED));
        ResourceManager.playSoundEffect(SFX_OPEN);
    }

    @FXML private void onScarerDoorExited() {
        if (transitioning) return;
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_CLOSED));
        ResourceManager.playSoundEffect(SFX_CLOSE);
    }

    @FXML private void onLaugherDoorEntered() {
        if (transitioning) return;
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_OPENED));
        ResourceManager.playSoundEffect(SFX_OPEN);
    }

    @FXML private void onLaugherDoorExited() {
        if (transitioning) return;
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_CLOSED));
        ResourceManager.playSoundEffect(SFX_CLOSE);
    }

    @FXML private void onScarerDoorClicked() {
        if (transitioning) return;
        transitioning = true;
        showModeSelectionPopup(Role.SCARER, SFX_SCREAM);
    }

    @FXML private void onLaugherDoorClicked() {
        if (transitioning) return;
        transitioning = true;
        showModeSelectionPopup(Role.LAUGHER, SFX_LAUGH);
    }

    private void showModeSelectionPopup(Role role, String sfxKey) {
        popupContent.getChildren().clear();
        popupContent.setSpacing(18);
        popupContent.setPadding(new Insets(28, 32, 28, 32));
        
        popupContent.setStyle(
            "-fx-background-color: rgba(15, 20, 40, 0.96);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + (role == Role.SCARER ? "#ff6060" : "#38c7ff") + ";" +
            "-fx-border-width: 2.0; -fx-border-radius: 16;"
        );
        if (!popupContent.getStyleClass().contains("popup-box")) {
            popupContent.getStyleClass().add("popup-box");
        }

        Text title = new Text(role == Role.SCARER ? "SCARER OPERATIONS PROTOCOL" : "LAUGHER OPERATIONS PROTOCOL");
        title.setFont(Font.font("Futura", 18));
        title.setFill(Color.web(role == Role.SCARER ? "#ff6060" : "#38c7ff"));

        Text subtitle = new Text("Select your game mode to begin:");
        subtitle.setFont(Font.font("Futura", 12));
        subtitle.setFill(Color.web("#94a3b8"));
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitle.setWrappingWidth(340);

        // Buttons
        Button pvpBtn = new Button("Versus Mode (1v1)");
        pvpBtn.getStyleClass().add("settings-btn");
        pvpBtn.setPrefWidth(300);

        Button pvcBtn = new Button("Practice Mode (vs. CPU)");
        pvcBtn.getStyleClass().add("settings-btn");
        pvcBtn.setPrefWidth(300);

        Button cancelBtn = new Button("Back");
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setPrefWidth(140);

        confirmingMode = null;

        pvpBtn.setOnAction(e -> {
            if (!"pvp".equals(confirmingMode)) {
                confirmingMode = "pvp";
                pvpBtn.setText("Confirm Versus (1v1)?");
                pvpBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: black;");
                pvcBtn.setText("Practice Mode (vs. CPU)");
                pvcBtn.setStyle("");
            } else {
                animatePopupOut(() -> launchGame(role, sfxKey, false));
            }
        });

        pvcBtn.setOnAction(e -> {
            if (!"pvc".equals(confirmingMode)) {
                confirmingMode = "pvc";
                pvcBtn.setText("Confirm Practice (vs. CPU)?");
                pvcBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: black;");
                pvpBtn.setText("Versus Mode (1v1)");
                pvpBtn.setStyle("");
            } else {
                animatePopupOut(() -> launchGame(role, sfxKey, true));
            }
        });

        cancelBtn.setOnAction(e -> {
            confirmingMode = null;
            animatePopupOut(() -> {
                transitioning = false;
                ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_CLOSED));
                LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_CLOSED));
                ResourceManager.playSoundEffect(SFX_CLOSE);
            });
        });

        popupContent.getChildren().addAll(title, subtitle, pvpBtn, pvcBtn, cancelBtn);
        popupOverlay.setVisible(true);
        animatePopupIn(null);
    }

    private void animatePopupIn(Runnable onDone) {
        popupContent.setScaleX(0.1); popupContent.setScaleY(0.1); popupContent.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), popupContent);
        st.setFromX(0.1); st.setFromY(0.1); st.setToX(1.0); st.setToY(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), popupContent);
        ft.setFromValue(0); ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(st, ft);
        if (onDone != null) pt.setOnFinished(e -> onDone.run());
        pt.play();
    }

    private void animatePopupOut(Runnable onDone) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), popupContent);
        st.setToX(0.1); st.setToY(0.1);
        FadeTransition ft = new FadeTransition(Duration.millis(200), popupContent);
        ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> { popupOverlay.setVisible(false); if (onDone != null) onDone.run(); });
        pt.play();
    }

    // ── Game launch ───────────────────────────────────────────────────────

    private void launchGame(Role role, String sfxKey, boolean vsComputer) {
        // Close music popup if open
        if (musicPopup != null && musicPopup.isShowing()) {
            musicPopup.hide(); musicPopup = null;
        }
        try {
            Game game = new Game(role);
            BoardController.setVsComputer(vsComputer);
            MonsterSelectionController.setGame(game);
            MediaPlayer sfx = ResourceManager.getAudio(sfxKey);
            if (sfx != null) {
                sfx.stop(); sfx.seek(sfx.getStartTime()); sfx.setCycleCount(1);
                sfx.setVolume(ResourceManager.getSfxVolume());
                final boolean[] called = {false};
                Runnable onFinished = () -> Platform.runLater(() -> {
                    if (!called[0]) {
                        called[0] = true;
                        sfx.setOnEndOfMedia(null);
                        sfx.setOnError(null);
                        fadeOutThenSwitch();
                    }
                });
                sfx.setOnEndOfMedia(onFinished);
                sfx.setOnError(onFinished);
                sfx.play();

                // Safety timeout fallback: 4s max wait
                PauseTransition timeout = new PauseTransition(Duration.seconds(4));
                timeout.setOnFinished(ex -> onFinished.run());
                timeout.play();
            } else {
                fadeOutThenSwitch();
            }
        } catch (Exception e) {
            System.err.println("Failed to create game: " + e.getMessage());
            transitioning = false;
        }
    }

    private void fadeOutThenSwitch() {
        javafx.scene.Parent content = Main.getCurrentContent();
        if (content == null) { doSwitch(); return; }
        FadeTransition fade = new FadeTransition(Duration.millis(800), content);
        fade.setFromValue(1.0); fade.setToValue(0.0);
        fade.setOnFinished(e -> doSwitch());
        fade.play();
    }

    private void doSwitch() {
        ResourceManager.playOSTWithMode("MonsterSelection.mp3", 0.15);
        Main.switchScene("/game/view/MonsterSelection.fxml");
    }
}
