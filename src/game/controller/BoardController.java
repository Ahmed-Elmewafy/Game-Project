package game.controller;

import game.Main;
import game.engine.*;
import game.engine.cards.*;
import game.engine.cells.*;
import game.engine.cells.Cell;
import game.engine.exceptions.*;
import game.engine.monsters.*;

import javafx.animation.*;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * BoardController — full game loop implementation.
 *
 * Architecture note (Design Pattern: MVC Controller):
 *   The engine's game.playTurn() runs synchronously — it rolls dice, moves the
 *   monster, executes cell effects, and switches turns all at once.
 *   To show sequential animations we capture a full snapshot of ALL state
 *   BEFORE calling playTurn(), then compare AFTER to determine what happened,
 *   and replay everything visually with animations.
 *
 *   Card identification: peek at Board.getCards().get(0) BEFORE playTurn()
 *   since Board.cards is a public static list. After playTurn() the card is gone.
 *
 *   Transport detection: if the monster's final position is NOT one of the
 *   transport cell destinations, it landed directly. If it IS a destination,
 *   we look up which transport cell sent it there.
 */
public class BoardController implements Initializable {

    // ── FXML nodes ─────────────────────────────────────────────────────────
    @FXML private VBox playerPanel;
    @FXML private VBox opponentPanel;
    @FXML private ImageView playerImage;
    @FXML private ImageView opponentImage;
    @FXML private Label playerName;
    @FXML private Label playerType;
    @FXML private Label playerRole;
    @FXML private HBox playerStatusBox;
    @FXML private Label playerEnergy;
    @FXML private Label playerPosition;
    @FXML private Pane playerEnergyFill;
    @FXML private ImageView playerCanisterImage;
    @FXML private ImageView opponentCanisterImage;
    @FXML private StackPane rootPane;
    @FXML private Button playerPlayTurnBtn;
    @FXML private Button playerPowerupBtn;
    @FXML private Button opponentPlayTurnBtn;
    @FXML private Button opponentPowerupBtn;
    @FXML private Label opponentName;
    @FXML private Label opponentType;
    @FXML private Label opponentRole;
    @FXML private HBox opponentStatusBox;
    @FXML private Label opponentEnergy;
    @FXML private Label opponentPosition;
    @FXML private Pane opponentEnergyFill;
    @FXML private GridPane boardGrid;
    @FXML private ScrollPane playerLogScroll;
    @FXML private ScrollPane opponentLogScroll;
    @FXML private VBox playerLogBox;
    @FXML private VBox opponentLogBox;
    @FXML private ImageView playerDiceImage;
    @FXML private ImageView opponentDiceImage;
    @FXML private StackPane popupOverlay;
    @FXML private VBox popupContent;

    // ── Layout constants ────────────────────────────────────────────────────
    private static final double CELL_W          = 70.0;
    private static final double CELL_H          = 72.0;
    private static final double MAX_BAR_H       = 29.0;  // 105 - bottomAnchor(37) - topAnchor(39)
    private static final double WINNING_ENERGY  = 1000.0;
    private static final double BELT_H          = 38.0;
    private static final double SHORT_NATURAL_W = BELT_H * (587.0 / 425.0);

    // ── Transport maps ──────────────────────────────────────────────────────
    private static final Map<Integer,Integer> CONVEYOR_MAP;
    static {
        Map<Integer,Integer> _m = new java.util.LinkedHashMap<Integer,Integer>();
        _m.put(6,31); _m.put(22,37); _m.put(44,55); _m.put(52,62); _m.put(66,68);
        CONVEYOR_MAP = Collections.unmodifiableMap(_m);
    }
    private static final Map<Integer,Integer> SOCK_MAP;
    static {
        Map<Integer,Integer> _m2 = new java.util.LinkedHashMap<Integer,Integer>();
        _m2.put(32,26); _m2.put(42,30); _m2.put(74,64); _m2.put(84,70); _m2.put(98,76);
        SOCK_MAP = Collections.unmodifiableMap(_m2);
    }
    /**
     * Returns true if going forward from 'from', we reach 'waypoint' before 'end'.
     * Used to detect whether a transport cell was on the monster's path.
     */
    private static boolean passesThrough(int from, int waypoint, int end) {
        int pos = from;
        for (int i = 0; i < 100; i++) {
            pos = (pos + 1) % 100;
            if (pos == waypoint) return true;
            if (pos == end)      return false;
        }
        return false;
    }

    /** Returns the conveyor source cell index if the monster was transported, else -1. */
    private int getConveyorSrc(int oldPos, int newPos) {
        for (Map.Entry<Integer,Integer> e : CONVEYOR_MAP.entrySet())
            if (e.getValue() == newPos && passesThrough(oldPos, e.getKey(), newPos))
                return e.getKey();
        return -1;
    }

    /** Returns the sock source cell index if the monster was transported, else -1. */
    private int getSockSrc(int oldPos, int newPos) {
        for (Map.Entry<Integer,Integer> e : SOCK_MAP.entrySet())
            if (e.getValue() == newPos && passesThrough(oldPos, e.getKey(), newPos))
                return e.getKey();
        return -1;
    }

    private static final Map<Integer,String> SOCK_IMAGE_MAP;
    static {
        Map<Integer,String> _m3 = new java.util.LinkedHashMap<Integer,String>();
        _m3.put(32,"32_26.png"); _m3.put(42,"42_30.png"); _m3.put(74,"74_64.png");
        _m3.put(84,"95_70.png"); _m3.put(98,"97_76.png");
        SOCK_IMAGE_MAP = Collections.unmodifiableMap(_m3);
    }
    private static final Map<Integer,String> CONVEYOR_DISPLAY_NAMES;
    static {
        Map<Integer,String> _m4 = new java.util.LinkedHashMap<Integer,String>();
        _m4.put(6,"Mega Conveyor"); _m4.put(22,"Speed Rail"); _m4.put(44,"Power Accelerator");
        _m4.put(52,"Power Lift"); _m4.put(66,"Shortcut");
        CONVEYOR_DISPLAY_NAMES = Collections.unmodifiableMap(_m4);
    }
    private static final Set<Integer> USE_SHORT_BELT;
    static {
        Set<Integer> _s = new java.util.HashSet<Integer>();
        _s.add(22); _s.add(44);
        USE_SHORT_BELT = Collections.unmodifiableSet(_s);
    }
    private static final String BELT_SHORT = "ConveyorBeltShort.png";
    private static final String BELT_LONG  = "ConveyorBeltLong.png";

    // ── Resource paths ──────────────────────────────────────────────────────
    private static final String DOOR_BASE      = "/game/resources/images/Board/doors/";
    private static final String TRANSPORT_BASE = "/game/resources/images/Board/transportation/";
    private static final String CARD_IMG_BASE  = "/game/resources/images/Board/Cards/";
    private static final String MONSTER_BASE   = "/game/resources/images/Monsters/";
    private static final String SFX_BASE       = "/game/resources/audio/sound effects/";
    // OST_BASE is determined at runtime from ResourceManager.getMusicMode()
    private static final String VOICE_BASE     = "/game/resources/audio/voice lines/";
    private static final String UI_BASE        = "/game/resources/images/Board/UI/";

    // ── Card name → image filename ──────────────────────────────────────────
    private static final Map<String,String> CARD_IMAGES;
    static {
        Map<String,String> _c = new java.util.LinkedHashMap<String,String>();
        _c.put("Position Swap","Position Swap.png");
        _c.put("Contamination Code","Contamination Code.png");
        _c.put("2319 Alert","2319 Alert.png");
        _c.put("Small Snatcher","Small Snatcher.png");
        _c.put("Sneaky Thief","Sneaky Theif.png");
        _c.put("Mega Drain","MegaDrain.png");
        _c.put("Super Shield","Super Shield.png");
        _c.put("Mind Scramble","Mind Scrumble.png");
        _c.put("Total Confusion","Total Confusion.png");
        CARD_IMAGES = Collections.unmodifiableMap(_c);
    }

    // ── Door maps ───────────────────────────────────────────────────────────
    private static final Map<Integer,String> DOOR_FILE = buildDoorFileMap();
    private static Map<Integer,String> buildDoorFileMap() {
        String[] f = {
            "Orange1.png","BabyBlue3.png","Blue5.png","DarkGreen7.png","DarkGrey9.png",
            "BabyBlue11.png","Purple13.png","Blue15.png","SemiGrey17.png","Orange19.png",
            "Yellow21.png","BabyBlue23.png","Blue25.png","Green27.png","DarkGrey29.png",
            "DarkGrey31.png","Green33.png","Blue35.png","Pink37.png","Yellow39.png",
            "Orange41.png","SemiGrey43.png","Blue45.png","Pink47.png","Purple49.png",
            "Purple51.png","Pink53.png","Blue55.png","SemiGrey57.png","Orange59.png",
            "Orange61.png","SemiGrey63.png","Blue65.png","Pink67.png","Purple69.png",
            "DarkGrey71.png","Green73.png","Blue75.png","BabyBlue77.png","Yellow79.png",
            "Yellow81.png","Purple83.png","Blue85.png","Green87.png","DarkGrey89.png",
            "Orange91.png","SemiGrey93.png","Blue95.png","BabyBlue97.png","Boo99.png"};
        Map<Integer,String> m = new HashMap<>();
        int i = 1; for (String s : f) { m.put(i, s); i += 2; }
        return Collections.unmodifiableMap(m);
    }
    private static final Map<String,String> COLOR_TO_OPENED;
    static {
        Map<String,String> _o = new java.util.LinkedHashMap<String,String>();
        _o.put("BabyBlue","LighterBlue.png"); _o.put("Blue","Blue.png");
        _o.put("DarkGreen","Green.png");   _o.put("DarkGrey","DarkGrey.png");
        _o.put("Green","Green.png");       _o.put("Orange","Orange.png");
        _o.put("Pink","Pink.png");         _o.put("Purple","Purple.png");
        _o.put("SemiGrey","SemiGray.png"); _o.put("Yellow","Yellow.png");
        _o.put("Boo","Boo.png");
        COLOR_TO_OPENED = Collections.unmodifiableMap(_o);
    }

    // ── Game / animation state ──────────────────────────────────────────────
    private static Game game;
    public static void setGame(Game g) { game = g; }
    private static boolean vsComputer = false;
    public static boolean isVsComputer() { return vsComputer; }
    public static void setVsComputer(boolean val) { vsComputer = val; }

    private final StackPane[] cellNodes = new StackPane[100];
    private ImageView playerToken, opponentToken;
    private boolean animating = false;
    private boolean settingsOpen = false;
    private MediaPlayer moveSfxPlayer, ostPlayer;
    private PauseTransition winDelayTransition;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> escFilter;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyboardFilter;
    private int ostIndex = 0;
    // Track whether we've already logged the "ready to powerup" milestone
    private boolean playerPowerupLogged = false;
    private boolean opponentPowerupLogged = false;
    private int lastDiceRoll = 0;  // effective steps the dice produced this turn

    private final List<MediaPlayer> activeMediaPlayers = new CopyOnWriteArrayList<>();

    // Wait 3 seconds before CPU moves
    private final int CPU_DELAY_MS = 3000;

    private static final String[] OST_FILES = {"Board1.mp3","Board2.mp3","Board3.mp3"};

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL loc, ResourceBundle res) {
        if (game == null) return;
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
        }
        playerDiceImage.managedProperty().bind(playerDiceImage.visibleProperty());
        opponentDiceImage.managedProperty().bind(opponentDiceImage.visibleProperty());
        populatePanel(game.getPlayer(),
            playerImage, playerName, playerType, playerRole,
            playerStatusBox, playerEnergyFill, playerEnergy, playerPosition);
        populatePanel(game.getOpponent(),
            opponentImage, opponentName, opponentType, opponentRole,
            opponentStatusBox, opponentEnergyFill, opponentEnergy, opponentPosition);
        buildBoardGrid();
        createTokens();
        updateTokenPositions();
        setTurnState(game.getCurrent() == game.getPlayer());
        checkPowerupAvailability();
        startBoardOST();
        addLog(true,  game.getPlayer().getName() + " is ready!", "action");
        addLog(false, game.getOpponent().getName() + " enters the Floor!", "action");

        playerPowerupBtn.setTooltip(new javafx.scene.control.Tooltip(getPowerupDescription(game.getPlayer())));
        opponentPowerupBtn.setTooltip(new javafx.scene.control.Tooltip(getPowerupDescription(game.getOpponent())));

        // ESC key to toggle settings popup using Event Filter for instant response
        escFilter = ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                ev.consume();
                if (settingsOpen) {
                    if (!animating) {
                        animating = true;
                        animatePopupOut(() -> {
                            animating = false;
                            settingsOpen = false;
                            setTurnState(game.getCurrent() == game.getPlayer());
                            checkPowerupAvailability();
                        });
                    }
                } else {
                    if (!popupOverlay.isVisible() && !animating) {
                        onSettingsClicked();
                    }
                }
            }
        };
        // Keyboard controls event filter
        keyboardFilter = ev -> {
            if (animating || (popupOverlay != null && popupOverlay.isVisible()) || settingsOpen) {
                return;
            }
            javafx.scene.input.KeyCode code = ev.getCode();
            
            // Player 1 controls (Active when it is Player 1's turn)
            if (code == javafx.scene.input.KeyCode.SPACE || code == javafx.scene.input.KeyCode.A) {
                if (playerPlayTurnBtn != null && !playerPlayTurnBtn.isDisable()) {
                    ev.consume();
                    playerPlayTurnBtn.fire();
                }
            } else if (code == javafx.scene.input.KeyCode.S) {
                if (playerPowerupBtn != null && !playerPowerupBtn.isDisable()) {
                    ev.consume();
                    playerPowerupBtn.fire();
                }
            }
            
            // Player 2 controls (Active when it is Player 2's turn and vsComputer is false)
            else if (code == javafx.scene.input.KeyCode.ENTER || code == javafx.scene.input.KeyCode.K) {
                if (opponentPlayTurnBtn != null && !opponentPlayTurnBtn.isDisable()) {
                    ev.consume();
                    opponentPlayTurnBtn.fire();
                }
            } else if (code == javafx.scene.input.KeyCode.L) {
                if (opponentPowerupBtn != null && !opponentPowerupBtn.isDisable()) {
                    ev.consume();
                    opponentPowerupBtn.fire();
                }
            }
        };

        Platform.runLater(() -> {
            if (boardGrid.getScene() != null) {
                boardGrid.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escFilter);
                boardGrid.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyboardFilter);
            }
            if (rootPane != null) {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(800), rootPane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        });
        if (vsComputer) {
            opponentPlayTurnBtn.setVisible(false);
            opponentPlayTurnBtn.setManaged(false);
            opponentPowerupBtn.setVisible(false);
            opponentPowerupBtn.setManaged(false);
            opponentLogScroll.setMaxHeight(Double.MAX_VALUE);
        }
        refreshAllUI();
    }

    private void setTurnState(boolean playerTurn) {
        if (vsComputer) {
            playerPlayTurnBtn.setDisable(!playerTurn);
            opponentPlayTurnBtn.setDisable(true);
        } else {
            if (playerTurn) {
                playerPlayTurnBtn.setDisable(false);
                opponentPlayTurnBtn.setDisable(true);
            } else {
                playerPlayTurnBtn.setDisable(true);
                opponentPlayTurnBtn.setDisable(false);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PANEL POPULATION
    // ═══════════════════════════════════════════════════════════════════════

    private void populatePanel(Monster monster, ImageView imageView, Label nameLabel, Label typeLabel,
                               Label roleLabel, HBox statusBox, Pane fillPane, Label energyLabel, Label positionLabel) {
        Image img = loadImage(monsterImagePath(monster));
        if (img != null) {
            imageView.setImage(img);
        }
        nameLabel.setText(monster.getName());
        typeLabel.setText(monsterTypeName(monster));
        String roleStr = monster.getOriginalRole().toString();
        roleLabel.setText(roleStr);
        roleLabel.getStyleClass().removeAll("role-scarer", "role-laugher");
        roleLabel.getStyleClass().add(roleStr.equals("SCARER") ? "role-scarer" : "role-laugher");
        updateStatusEffects(monster, statusBox);
        updateEnergyBar(monster, fillPane, energyLabel);
        positionLabel.setText("Cell " + monster.getPosition());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BOARD GRID BUILDING
    // ═══════════════════════════════════════════════════════════════════════

    private void buildBoardGrid() {
        for (int i = 0; i < 100; i++) {
            int[] rc = indexToRowCol(i);
            StackPane cell = buildBaseCell(i);
            cellNodes[i] = cell;
            boardGrid.add(cell, rc[1], 9 - rc[0]);
        }
        drawConveyorBelts();
    }

    private int[] indexToRowCol(int idx) {
        int row = idx / 10, col = idx % 10;
        if (row % 2 == 1) col = 9 - col;
        return new int[]{row, col};
    }

    private StackPane buildBaseCell(int index) {
        StackPane cell = new StackPane();
        cell.setMinSize(CELL_W, CELL_H); cell.setMaxSize(CELL_W, CELL_H);
        Pane bg = new Pane(); bg.setMinSize(CELL_W, CELL_H);
        bg.setStyle(cellBgStyle(index)); cell.getChildren().add(bg);
        addCellContent(cell, index);
        attachTooltip(cell, index);
        Label idx = new Label(String.valueOf(index));
        idx.setStyle("-fx-font-family:'Futura';-fx-font-size:8px;" +
                     "-fx-text-fill:rgba(255,255,255,0.65);-fx-padding:0 2 1 0;");
        StackPane.setAlignment(idx, Pos.BOTTOM_RIGHT);
        cell.getChildren().add(idx);
        cell.setStyle("-fx-border-color:rgba(10,22,40,0.9);-fx-border-width:0.5;");
        return cell;
    }

    private void addCellContent(StackPane cell, int index) {
        if (index == 0) {
            Label startLabel = new Label("START");
            startLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 9px; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");
            cell.getChildren().add(startLabel);
            return;
        }
        if (contains(Constants.MONSTER_CELL_INDICES, index)) {
            addMonsterCellImage(cell, index);
            return;
        }
        if (contains(Constants.CARD_CELL_INDICES, index)) {
            addImage(cell, CARD_IMG_BASE + "Unkown card.png", 44, Pos.CENTER);
            return;
        }
        if (contains(Constants.CONVEYOR_CELL_INDICES, index)) {
            return; // No cell icon — conveyor belt overlay drawn separately
        }
        if (contains(Constants.SOCK_CELL_INDICES, index)) {
            String sockFile = SOCK_IMAGE_MAP.get(index);
            if (sockFile != null) {
                addImage(cell, TRANSPORT_BASE + sockFile, 58, Pos.CENTER);
            }
            return;
        }
        if (index % 2 == 1) {
            String doorFile = DOOR_FILE.get(index);
            if (doorFile != null) {
                addImage(cell, DOOR_BASE + "Closed/" + doorFile, 58, Pos.CENTER);
            }
        }
    }

    private void addMonsterCellImage(StackPane cell, int cellIndex) {
        int monsterIndex = -1;
        for (int i = 0; i < Constants.MONSTER_CELL_INDICES.length; i++) {
            if (Constants.MONSTER_CELL_INDICES[i] == cellIndex) {
                monsterIndex = i;
                break;
            }
        }
        if (monsterIndex < 0) return;
        List<Monster> stationed = Board.getStationedMonsters();
        if (monsterIndex >= stationed.size()) return;
        addImage(cell, monsterImagePath(stationed.get(monsterIndex)), 56, Pos.CENTER);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MONSTER TOKENS
    // ═══════════════════════════════════════════════════════════════════════

    private void createTokens() {
        playerToken   = createToken(game.getPlayer(),   Color.DODGERBLUE);
        opponentToken = createToken(game.getOpponent(), Color.CRIMSON);
    }

    /**
     * Creates a monster image token with a subtle colored glow.
     * Reduced glow spread vs previous implementation for cleaner visuals.
     */
    private ImageView createToken(Monster m, Color glowColor) {
        ImageView tok = new ImageView();
        tok.setFitWidth(36); tok.setFitHeight(36);
        tok.setPreserveRatio(true); tok.setSmooth(true);
        Image img = loadImage(monsterImagePath(m));
        if (img != null) tok.setImage(img);
        // Reduced glow: smaller radius, lower spread than before
        DropShadow glow = new DropShadow(8, glowColor);
        glow.setSpread(0.5);
        tok.setEffect(glow);
        tok.setMouseTransparent(true);
        return tok;
    }

    private void updateTokenPositions() {
        for (StackPane c : cellNodes) if (c != null) {
            c.getChildren().remove(playerToken);
            c.getChildren().remove(opponentToken);
        }
        int pp = game.getPlayer().getPosition(), op = game.getOpponent().getPosition();
        if (cellNodes[pp] != null) {
            StackPane.setAlignment(playerToken, Pos.TOP_LEFT);
            cellNodes[pp].getChildren().add(playerToken);
        }
        if (cellNodes[op] != null) {
            StackPane.setAlignment(opponentToken, Pos.TOP_RIGHT);
            cellNodes[op].getChildren().add(opponentToken);
        }
        boolean playerTurn = game.getCurrent() == game.getPlayer();
        // Active token: brighter glow. Inactive: subtle dim glow.
        DropShadow bright = new DropShadow(10, playerTurn ? Color.DODGERBLUE : Color.CRIMSON);
        bright.setSpread(0.6);
        DropShadow dim   = new DropShadow(6,  playerTurn ? Color.CRIMSON : Color.DODGERBLUE);
        dim.setSpread(0.3);
        (playerTurn ? playerToken : opponentToken).setEffect(bright);
        (playerTurn ? opponentToken : playerToken).setEffect(dim);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVEYOR BELT OVERLAY
    // ═══════════════════════════════════════════════════════════════════════

    private void drawConveyorBelts() {
        boardGrid.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() <= 0) return;
            // Sock destination small indicators
            for (Map.Entry<Integer,Integer> e : SOCK_MAP.entrySet()) {
                int to = e.getValue(); if (cellNodes[to] == null) continue;
                boolean already = cellNodes[to].getChildren().stream()
                    .anyMatch(x -> "sock-dest".equals(x.getUserData()));
                if (already) continue;
                String sf = SOCK_IMAGE_MAP.get(e.getKey()); if (sf == null) continue;
                ImageView sm = makeImageView(TRANSPORT_BASE+sf, 18);
                sm.setUserData("sock-dest");
                StackPane.setAlignment(sm, Pos.BOTTOM_LEFT);
                StackPane.setMargin(sm, new Insets(0,0,2,2));
                cellNodes[to].getChildren().add(sm);
            }
            // Conveyor belt stretched images
            Pane ov = getOrCreateOverlay();
            if (ov == null || !ov.getChildren().isEmpty()) return;
            Image si = loadImage(TRANSPORT_BASE+BELT_SHORT);
            Image li = loadImage(TRANSPORT_BASE+BELT_LONG);
            for (Map.Entry<Integer,Integer> e : CONVEYOR_MAP.entrySet()) {
                int fi = e.getKey(), ti = e.getValue();
                int[] fr = indexToRowCol(fi), tr = indexToRowCol(ti);
                double fx = fr[1]*CELL_W+CELL_W/2.0, fy = (9-fr[0])*CELL_H+CELL_H/2.0;
                double tx = tr[1]*CELL_W+CELL_W/2.0, ty = (9-tr[0])*CELL_H+CELL_H/2.0;
                double dist  = Math.sqrt((tx-fx)*(tx-fx)+(ty-fy)*(ty-fy));
                double angle = Math.toDegrees(Math.atan2(ty-fy, tx-fx));
                boolean fs = USE_SHORT_BELT.contains(fi);
                Image ch = (fs || dist <= SHORT_NATURAL_W) ? (si!=null?si:li) : (li!=null?li:si);
                if (ch == null) continue;
                double bH = fs ? 48.0 : BELT_H;
                ImageView b = new ImageView(ch);
                b.setFitWidth(dist); b.setFitHeight(bH); b.setPreserveRatio(false);
                b.setSmooth(true); b.setOpacity(0.92); b.setMouseTransparent(true);
                double mx = (fx+tx)/2, my = (fy+ty)/2;
                double ratio = fs ? 0.7387 : 0.7495;
                double shiftRatio = fs ? -0.0747 : -0.0250;
                double rotOffset = Math.toDegrees(Math.atan2(bH * ratio, dist));
                double finalRot = angle + rotOffset;
                double rad = Math.toRadians(finalRot);
                double shiftY = shiftRatio * bH;
                double dx = shiftY * Math.sin(rad);
                double dy = -shiftY * Math.cos(rad);
                b.setLayoutX(mx - dist/2 + dx); b.setLayoutY(my - bH/2 + dy); b.setRotate(finalRot);
                ov.getChildren().add(b);
            }
        });
    }

    private Pane getOrCreateOverlay() {
        if (!(boardGrid.getParent() instanceof StackPane)) return null;
        StackPane sp = (StackPane) boardGrid.getParent();
        for (javafx.scene.Node c : sp.getChildren()) {
            if (c instanceof Pane) {
                Pane p = (Pane) c;
                if (p != boardGrid && p.getStyleClass().isEmpty()) return p;
            }
        }
        Pane ov = new Pane();
        ov.setMouseTransparent(true); ov.setMinSize(700, 720); ov.setMaxSize(700, 720); ov.setPrefSize(700, 720);
        sp.getChildren().add(ov); return ov;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GAME LOOP — PLAY TURN
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void onPlayerPlayTurn()   { 
        if (!animating && game.getCurrent()==game.getPlayer()) {
            resetPowerupConfirmations();
            executeTurn(true);
        }
    }
    @FXML private void onOpponentPlayTurn() { 
        if (!animating && game.getCurrent()==game.getOpponent()) {
            resetPowerupConfirmations();
            executeTurn(false);
        }
    }

    private void executeTurn(boolean isPlayer) {
        animating = true;
        disableAllButtons();

        Monster current = game.getCurrent();
        Monster other   = isPlayer ? game.getOpponent() : game.getPlayer();

        // ── Capture FULL state snapshot BEFORE engine runs ────────────────
        int  oldPos         = current.getPosition();
        int  oldEnergy      = current.getEnergy();
        int  oldOtherEnergy = other.getEnergy();
        int  oldOtherPos    = other.getPosition();
        boolean wasShielded = current.isShielded();
        boolean wasOtherShielded = other.isShielded();
        boolean wasFrozen   = current.isFrozen();
        if (Board.getCards().isEmpty()) {
            Board.reloadCards();
        }
        int  oldCardCount   = Board.getCards().size();
        // Peek at the next card BEFORE the engine draws it
        Card nextCard = Board.getCards().isEmpty() ? null : Board.getCards().get(0);
        // Capture Dasher momentum state before playTurn (to infer exact dice roll)
        int momentumBefore = (current instanceof Dasher) ? ((Dasher) current).getMomentumTurns() : 0;
        int focusTurnsBefore = (current instanceof MultiTasker) ? ((MultiTasker) current).getNormalSpeedTurns() : 0;
        // Snapshot ALL activated door indices BEFORE playTurn.
        // We compare after playTurn to detect which door (at initialLandPos) just activated.
        java.util.Set<Integer> activatedDoorsBefore = snapshotActivatedDoors();
        java.util.Map<Monster, Integer> oldStationedEnergy = new java.util.HashMap<>();
        for (Monster sm : Board.getStationedMonsters()) {
            oldStationedEnergy.put(sm, sm.getEnergy());
        }

        // ── Frozen: engine handles skip internally ────────────────────────
        // ── Frozen: engine handles skip internally ────────────────────────
        if (wasFrozen) {
            playSfx("Freeze.mp3");
            addLog(isPlayer, current.getName() + " is FROZEN! Turn skipped.", "warn");
            try { game.playTurn(); } catch (InvalidMoveException ignored) {}
            finishTurn();
            return;
        }

        // ── Run the engine turn ───────────────────────────────────────────
        // Compute where the monster WOULD land (for animation before exception check)
        // We must run the engine first, then check for exception.
        boolean invalidMove = false;
        String invalidMsg   = "";
        try {
            game.playTurn();
        } catch (InvalidMoveException e) {
            invalidMove = true;
            invalidMsg  = e.getMessage();
        }

        if (invalidMove) {
            // Animate token to the intended cell (engine reset position to oldPos already)
            // We compute intended position from dice: effectiveSteps before transport
            // For display only — animate to (oldPos + effectiveSteps) % 100
            // The engine already called current.move(roll) then reset to oldPos on exception
            // So we show the attempted destination then bounce back
            int intendedPos = current.getPosition(); // engine reset to oldPos
            // Re-derive attempted dest: can't know exact roll without engine, so skip pre-anim
            // Just show popup and log
            int roll = game.getLastRoll();
            lastDiceRoll = roll;
            updateDiceDisplay(isPlayer, roll);
            addLog(isPlayer, "Cell occupied by opponent! Must roll again.", "warn");
            final String msg = invalidMsg;
            PauseTransition pt = new PauseTransition(Duration.seconds(1));
            pt.setOnFinished(ev -> {
                showExceptionPopupAutoDismiss("Cell Occupied!", msg, () -> {
                    refreshAllUI();
                    animating = false;
                    setTurnState(game.getCurrent() == game.getPlayer());
                    checkPowerupAvailability();
                    if (vsComputer && game.getCurrent() == game.getOpponent()) {
                        triggerComputerTurn();
                    }
                });
            });
            pt.play();
            return;
        }

        // ── Capture state AFTER engine ran ────────────────────────────────
        int  newPos         = current.getPosition();   // FINAL position (post-transport)
        int  newEnergy      = current.getEnergy();
        int  newOtherEnergy = other.getEnergy();
        int  newOtherPos    = other.getPosition();
        boolean cardWasDrawn = Board.getCards().size() < oldCardCount;

        // Determine if a transport occurred and intermediate landing position:
        int roll = game.getLastRoll();
        int moveDistance = roll;
        if (current instanceof Dasher) {
            moveDistance = (momentumBefore > 0) ? (roll * 3) : (roll * 2);
        } else if (current instanceof MultiTasker) {
            moveDistance = (focusTurnsBefore > 0) ? roll : (roll / 2);
        }

        int initialLandPos = (oldPos + moveDistance) % 100;
        int effectiveSteps = moveDistance;

        boolean wasConveyed = CONVEYOR_MAP.containsKey(initialLandPos);
        boolean wasSocked   = SOCK_MAP.containsKey(initialLandPos);
        int conveyorSrcIdx  = wasConveyed ? initialLandPos : -1;
        int sockSrcIdx      = wasSocked ? initialLandPos : -1;

        // ── Logging ────────────────────────────────────────────────────────
        int rawRoll = roll;
        lastDiceRoll = rawRoll;
        updateDiceDisplay(isPlayer, rawRoll);
        // 🚶🚶 Movement animation: step by step to initialLandPos 🚶🚶🚶🚶🚶🚶🚶🚶🚶🚶🚶🚶
        PauseTransition diceDelay = new PauseTransition(Duration.millis(800));
        diceDelay.setOnFinished(ev -> {
            // Log: show raw roll and effective movement separately for Dasher/MultiTasker
            if (current instanceof Dasher && effectiveSteps != rawRoll) {
                String mult = momentumBefore > 0 ? "3×" : "2×";
                addLog(isPlayer, current.getName() + " rolled " + rawRoll
                    + " (" + mult + " Dasher → moves " + effectiveSteps + " cells)", "action");
            } else if (effectiveSteps == 0) {
                addLog(isPlayer, current.getName() + " rolled " + rawRoll
                    + " (halved → 0 steps, stays at Cell " + oldPos + ")", "action");
            } else {
                addLog(isPlayer, current.getName() + " rolled " + rawRoll
                    + " → Cell " + initialLandPos, "action");
            }

            playMoveSfx();
            final int animStepsCap = Math.max(1, effectiveSteps);
            animateMovement(current, isPlayer, playerToken, opponentToken, oldPos, initialLandPos, animStepsCap, () -> {
                stopMoveSfx();

            // ── Now animate any transport effect ──────────────────────────
            if (wasConveyed) {
                addLog(isPlayer, current.getName() + " transported by conveyor to Cell " + newPos + "!", "action");
                playSfxWithSyncedAnimation("ConveyorBelt.mp3", isPlayer, initialLandPos, newPos, () ->
                    processCellEffects(isPlayer, current, other,
                        oldEnergy, newEnergy, oldOtherEnergy, newOtherEnergy,
                        oldOtherPos, newOtherPos, initialLandPos, newPos,
                        cardWasDrawn, nextCard, wasShielded, wasOtherShielded,
                        activatedDoorsBefore, oldStationedEnergy));
            } else if (wasSocked) {
                playSfx("ContaminationSock.mp3");
                // Energy penalty is logged in processCellEffects via energy diff
                animateSockTeleport(isPlayer, initialLandPos, newPos, () ->
                    processCellEffects(isPlayer, current, other,
                        oldEnergy, newEnergy, oldOtherEnergy, newOtherEnergy,
                        oldOtherPos, newOtherPos, initialLandPos, newPos,
                        cardWasDrawn, nextCard, wasShielded, wasOtherShielded,
                    activatedDoorsBefore, oldStationedEnergy));
            } else {
                processCellEffects(isPlayer, current, other,
                    oldEnergy, newEnergy, oldOtherEnergy, newOtherEnergy,
                    oldOtherPos, newOtherPos, initialLandPos, newPos,
                    cardWasDrawn, nextCard, wasShielded, wasOtherShielded,
                    activatedDoorsBefore, oldStationedEnergy);
            }
        });
        });
        diceDelay.play();
    }

    /**
     * After movement animation completes, process what happened at the landed cell.
     * initialLandPos = where dice brought the monster (before transport).
     * newPos         = final position (after transport if any).
     */
    private void processCellEffects(
            boolean isPlayer, Monster current, Monster other,
            int oldEnergy, int newEnergy, int oldOtherEnergy, int newOtherEnergy,
            int oldOtherPos, int newOtherPos,
            int initialLandPos, int newPos,
            boolean cardWasDrawn, Card nextCard,
            boolean wasShielded, boolean wasOtherShielded,
            java.util.Set<Integer> activatedDoorsBefore,
            java.util.Map<Monster, Integer> oldStationedEnergy) {

        // What cell did the monster INITIALLY land on?
        Cell landedCell = getCellByIndex(initialLandPos);

        // â”€â”€ DOOR CELL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (landedCell instanceof DoorCell) {
            DoorCell door = (DoorCell) landedCell;
            int energyDiff = newEnergy - oldEnergy;
            boolean doorAlreadyActivated = activatedDoorsBefore.contains(initialLandPos);
            if (doorAlreadyActivated) {
                playSfx("CloseDoor.mp3");
                addLog(isPlayer, door.getName() + " is already exhausted - no effect.", "warn");
                animateEnergyChange(isPlayer, oldEnergy, newEnergy, false,
                    () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy, false,
                        this::finishTurn));
                return;
            } else {
                boolean roleMatch = (current.getRole() == door.getRole());
                int rawDoorEnergy = door.getEnergy(); // raw value from door spec
                if (wasShielded && !roleMatch) {
                    playSfx("ShieldBreak.mp3");
                    addLog(isPlayer, current.getName() + "'s shield blocked "
                        + rawDoorEnergy + " energy loss from " + door.getName() + "!", "warn");
                } else {
                    playSfx("OpenDoor.mp3");
                    String gainLoss   = roleMatch ? "gained" : "lost";
                    String logType    = roleMatch ? "gain" : "loss";
                    String sfxKey     = door.getRole().toString().equals("SCARER")
                                        ? "ScarerDoorScream.mp3" : "LaugherDoorLaugh.mp3";
                    
                    playSfx(sfxKey); // Play voice line immediately
                    
                    if (!roleMatch && oldEnergy == 0 && newEnergy == 0) {
                        addLog(isPlayer, current.getName() + " has 0 energy - no energy was lost from " + door.getName(), "warn");
                    } else if (current instanceof MultiTasker && !roleMatch) {
                        String msg = formatMultiTaskerLossBonusMessage(current.getName(), rawDoorEnergy, energyDiff, "from " + door.getName());
                        addLog(isPlayer, msg, energyDiff >= 0 ? "gain" : "loss");
                    } else {
                        String realGainLoss = energyDiff >= 0 ? "gained" : "lost";
                        String realLogType = energyDiff >= 0 ? "gain" : "loss";
                        addLog(isPlayer, current.getName() + " " + realGainLoss + " "
                            + Math.abs(energyDiff) + " energy from " + door.getName(), realLogType);
                    }
                    // Team effects for stationed monsters of same role
                    for (Monster sm : Board.getStationedMonsters()) {
                        if (sm.getRole() == current.getRole()) {
                            int smOldEnergy = oldStationedEnergy.get(sm);
                            int smDiff = sm.getEnergy() - smOldEnergy;
                            if (!roleMatch && smOldEnergy == 0 && sm.getEnergy() == 0) {
                                addLog(isPlayer, sm.getName() + " also has 0 energy - no energy was lost", "warn");
                            } else if (smDiff != 0) {
                                if (sm instanceof MultiTasker && !roleMatch) {
                                    String msg = formatMultiTaskerLossBonusMessage(sm.getName(), rawDoorEnergy, smDiff, "from " + door.getName());
                                    addLog(isPlayer, msg, smDiff >= 0 ? "gain" : "loss");
                                } else {
                                    String smGainLoss = smDiff >= 0 ? "gained" : "lost";
                                    String smLogType = smDiff >= 0 ? "gain" : "loss";
                                    addLog(isPlayer, sm.getName() + " also " + smGainLoss
                                        + " " + Math.abs(smDiff) + " energy", smLogType);
                                }
                            }
                        }
                    }
                    // Replace door image when it becomes activated
                    replaceDoorImage(initialLandPos);
                }
                
                // Delay the energy fill animation and its sound
                PauseTransition p = new PauseTransition(Duration.millis(900));
                p.setOnFinished(ev -> {
                    animateEnergyChange(isPlayer, oldEnergy, newEnergy, false,
                        () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy, false,
                            this::finishTurn));
                });
                p.play();
                return;
            }
        }
        // ── CARD CELL ──────────────────────────────────────────────────────
        if (landedCell instanceof CardCell && cardWasDrawn) {
            addLog(isPlayer, current.getName() + " drew a card!", "action");

            // CardDraw plays first (dispenser sound), then card-specific SFX
            // after card animation completes (handled inside showCardPopup callback)
            String cardName = nextCard != null ? nextCard.getName() : "";
            logCardEffect(isPlayer, cardName, current, other,
                oldEnergy, newEnergy, oldOtherEnergy, newOtherEnergy,
                oldOtherPos, newOtherPos, wasOtherShielded);

            showCardPopup(nextCard, () -> {
                // Animate token movement for position-changing cards
                if (nextCard instanceof StartOverCard) {
                    // Determine which monster was sent to start
                    int currentNewPos = current.getPosition();
                    int otherNewPos   = other.getPosition();
                    boolean currentSentBack = currentNewPos == 0 && initialLandPos != 0;
                    boolean otherSentBack   = otherNewPos   == 0 && oldOtherPos   != 0;
                    // Use teleport (fade) animation for Start Over — same as sock
                    if (currentSentBack) {
                        animateSockTeleport(isPlayer, initialLandPos, 0, () ->
                            animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                                () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                                    this::finishTurn)));
                    } else if (otherSentBack) {
                        animateSockTeleport(!isPlayer, oldOtherPos, 0, () ->
                            animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                                () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                                    this::finishTurn)));
                    } else {
                        updateTokenPositions();
                        animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                            () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                                this::finishTurn));
                    }
                } else if (nextCard instanceof SwapperCard) {
                    // Position swap — animate both tokens
                    int currentNewPos = current.getPosition();
                    int otherNewPos   = other.getPosition();
                    if (currentNewPos != initialLandPos) {
                        animateSockTeleport(isPlayer, initialLandPos, currentNewPos, () ->
                            animateSockTeleport(!isPlayer, oldOtherPos, otherNewPos, () ->
                                animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                                    () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                                        this::finishTurn))));
                    } else {
                        updateTokenPositions();
                        animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                            () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                                this::finishTurn));
                    }
                } else {
                    updateTokenPositions();
                    animateEnergyChange(isPlayer, oldEnergy, newEnergy,
                        () -> animateEnergyChange(!isPlayer, oldOtherEnergy, newOtherEnergy,
                            this::finishTurn));
                }
            });
            return;
        }

        // ── MONSTER CELL ───────────────────────────────────────────────────
        if (landedCell instanceof MonsterCell) {
            MonsterCell mc = (MonsterCell) landedCell;
            Monster stationed = mc.getCellMonster();
            if (stationed != null) {
                boolean sameRole = stationed.getRole() == current.getRole();
                MediaPlayer mp = null;
                int oldStationedE = oldStationedEnergy.get(stationed);
                if (sameRole) {
                    String pwName = getPowerupName(current);
                    addLog(isPlayer, current.getName() + " activated " + pwName + " for FREE!", "gain");
                    mp = playVoiceLine(stationed, true);
                } else {
                    // Always play enemy voice line on opposite-role encounter
                    mp = playVoiceLine(stationed, false);
                    boolean swapOccurred = oldEnergy > oldStationedE;
                    if (swapOccurred) {
                        int newStationedE = stationed.getEnergy();
                        if (wasShielded) {
                            playSfx("ShieldBreak.mp3");
                            addLog(isPlayer, current.getName() + "'s shield blocked the energy loss from swap!", "warn");
                            addLog(isPlayer, stationed.getName() + " still gained energy: " + oldStationedE + " → " + newStationedE, "gain");
                        } else {
                            String lossMsg;
                            if (current instanceof MultiTasker) {
                                int rawLoss = oldEnergy - oldStationedE;
                                int netChange = newEnergy - oldEnergy;
                                lossMsg = formatMultiTaskerLossBonusMessage(current.getName(), rawLoss, netChange, "from swap");
                            } else {
                                lossMsg = current.getName() + " lost " + (oldEnergy - newEnergy) + " energy (now " + newEnergy + ")";
                            }
                            
                            String gainMsg = stationed.getName() + " gained " + (newStationedE - oldStationedE) + " energy (now " + newStationedE + ")";
                            addLog(isPlayer, "Energy swapped! " + lossMsg + ", and " + gainMsg, "warn");
                        }
                    } else {
                        addLog(isPlayer, stationed.getName()
                            + " has more energy — no swap.", "action");
                    }
                }
                int newStationedE = stationed.getEnergy();
                showMonsterCellPopup(current, stationed, sameRole,
                    oldEnergy, newEnergy, oldStationedE, newStationedE, isPlayer, mp, () -> {
                        refreshAllUI();
                        finishTurn();
                    });
                return;
            }
        }

        // ── CONTAMINATION SOCK ────────────────────────────────────────────
        // (Transport animation already played. initialLandPos IS the sock cell.)
        // landedCell here is at initialLandPos which IS the sock cell itself.
        if (landedCell instanceof ContaminationSock) {
            int eDiff = newEnergy - oldEnergy;
            addLog(isPlayer, current.getName() + " hit a contamination sock! Sent to Cell " + newPos, "loss");
            if (wasShielded && eDiff == 0) {
                addLog(isPlayer, "Shield blocked the -100 energy penalty!", "warn");
            } else {
                if (oldEnergy == 0 && newEnergy == 0) {
                    addLog(isPlayer, current.getName() + " has 0 energy - no energy was lost from contamination sock!", "warn");
                } else if (current instanceof MultiTasker) {
                    String msg = formatMultiTaskerLossBonusMessage(current.getName(), Constants.SLIP_PENALTY, eDiff, null);
                    addLog(isPlayer, msg, eDiff >= 0 ? "gain" : "loss");
                } else if (eDiff < 0) {
                    addLog(isPlayer, current.getName() + " lost " + Math.abs(eDiff) + " energy", "loss");
                }
            }
            animateEnergyChange(isPlayer, oldEnergy, newEnergy, this::finishTurn);
            return;
        }

        // ── CONVEYOR BELT ─────────────────────────────────────────────────
        // Transport animation already played in executeTurn. No energy effect.
        // ── NORMAL CELL ───────────────────────────────────────────────────
        int eDiff = newEnergy - oldEnergy;
        if (eDiff != 0) {
            if (current instanceof MultiTasker && (eDiff - Constants.MULTITASKER_BONUS) < 0) {
                int rawLoss = Math.abs(eDiff - Constants.MULTITASKER_BONUS);
                String msg = formatMultiTaskerLossBonusMessage(current.getName(), rawLoss, eDiff, null);
                addLog(isPlayer, msg, eDiff >= 0 ? "gain" : "loss");
            } else {
                addLog(isPlayer, current.getName() + (eDiff>0?" gained ":" lost ") + Math.abs(eDiff) + " energy", eDiff>0?"gain":"loss");
            }
        }
        animateEnergyChange(isPlayer, oldEnergy, newEnergy, this::finishTurn);
    }

    /**
     * Log the effect of a drawn card based on state changes.
     */
    private void logCardEffect(boolean isPlayer, String cardName,
            Monster current, Monster other,
            int oldEnergy, int newEnergy, int oldOtherEnergy, int newOtherEnergy,
            int oldOtherPos, int newOtherPos, boolean wasOtherShielded) {
        if ("Position Swap".equals(cardName)) {
            if (other.getPosition() != newOtherPos) {
                addLog(isPlayer, "Positions swapped!", "action");
            } else {
                addLog(isPlayer, "Position Swap — no effect (already ahead).", "warn");
            }
        } else if ("Contamination Code".equals(cardName)) {
            addLog(isPlayer, current.getName() + " sent back to Start!", "loss");
        } else if ("2319 Alert".equals(cardName)) {
            addLog(isPlayer, other.getName() + " sent back to Start!", "gain");
        } else if ("Small Snatcher".equals(cardName) || "Sneaky Thief".equals(cardName) || "Mega Drain".equals(cardName)) {
            if (wasOtherShielded && newOtherEnergy == oldOtherEnergy) {
                playSfx("ShieldBreak.mp3");
                addLog(isPlayer, other.getName() + "'s shield blocked the steal!", "warn");
            } else if (oldOtherEnergy == 0 && newOtherEnergy == 0) {
                addLog(isPlayer, other.getName() + " has 0 energy - no energy was stolen!", "warn");
            } else {
                int toSteal = (other instanceof MultiTasker) 
                    ? (Constants.MULTITASKER_BONUS - (newOtherEnergy - oldOtherEnergy)) 
                    : (oldOtherEnergy - newOtherEnergy);
                addLog(isPlayer, current.getName() + " stole "
                    + toSteal
                    + " energy from " + other.getName() + "!", "gain");
                if (other instanceof MultiTasker) {
                    int netChange = newOtherEnergy - oldOtherEnergy;
                    String msg = formatMultiTaskerLossBonusMessage(other.getName(), toSteal, netChange, "from being stolen from");
                    addLog(isPlayer, msg, netChange >= 0 ? "gain" : "loss");
                }
            }
        } else if ("Super Shield".equals(cardName)) {
            addLog(isPlayer, current.getName() + " is now shielded!", "gain");
            if (wasOtherShielded) addLog(!isPlayer, other.getName() + "'s shield was removed!", "loss");
        } else if ("Mind Scramble".equals(cardName)) {
            addLog(isPlayer, "Both monsters confused for 2 turns!", "warn");
        } else if ("Total Confusion".equals(cardName)) {
            addLog(isPlayer, "Total confusion! Both monsters confused for 3 turns!", "warn");
        } else {
            addLog(isPlayer, cardName + " card played!", "action");
        }
    }

    private void finishTurn() {
        finishTurn(0);
    }

    private void finishTurn(int attempt) {
        boolean isAudioPlaying = activeMediaPlayers.stream()
            .anyMatch(mp -> mp.getStatus() == MediaPlayer.Status.PLAYING);
            
        if (isAudioPlaying && attempt < 25) { // 25 attempts * 100ms = 2.5s max wait
            PauseTransition pt = new PauseTransition(Duration.millis(100));
            pt.setOnFinished(e -> finishTurn(attempt + 1));
            pt.play();
            return;
        }

        // If we timeout, make sure we clear activeMediaPlayers to release resources
        if (isAudioPlaying && attempt >= 25) {
            for (MediaPlayer mp : activeMediaPlayers) {
                try { mp.stop(); mp.dispose(); } catch (Exception ignored) {}
            }
            activeMediaPlayers.clear();
        }

        Platform.runLater(() -> {
            refreshAllUI();
            updateTokenPositions();


            // Check Powerup milestone (logs only)
            checkPowerupMilestone();

            // Check win condition
            Monster winner = game.getWinner();
            if (winner != null) {
                boolean isP = winner == game.getPlayer();
                addLog(isP, "🏆 " + winner.getName() + " WINS THE GAME!", "gain");
                disableAllButtons();
                animating = false;
                // Transition to EndScreen after a short delay so the last log entry is visible
                winDelayTransition = new PauseTransition(Duration.seconds(2));
                winDelayTransition.setOnFinished(e -> {
                    cleanup();
                    EndScreenController.setGame(game);
                    Main.switchScene("/game/view/EndScreen.fxml");
                });
                winDelayTransition.play();
                return;
            }

            setTurnState(game.getCurrent() == game.getPlayer());
            checkPowerupAvailability();
            animating = false;
            if (vsComputer && game.getCurrent() == game.getOpponent()) {
                triggerComputerTurn();
            }
        });
    }

    private void triggerComputerTurn() {
        animating = true;
        disableAllButtons();

        // Pause 1.5 seconds before starting CPU decision phase
        PauseTransition initialDelay = new PauseTransition(Duration.seconds(1.5));
        initialDelay.setOnFinished(ev -> {
            Monster opponent = game.getOpponent();
            Monster player = game.getPlayer();

            // Decide if powerup should be used
            boolean shouldUsePowerup = false;
            if (opponent.getEnergy() >= Constants.POWERUP_COST && !opponent.isFrozen()) {
                if (opponent instanceof game.engine.monsters.Dynamo) {
                    boolean opponentFrozen = player.isFrozen();
                    if (!opponentFrozen) {
                        boolean opponentCloseToWin = player.getPosition() >= 80 && player.getEnergy() >= Constants.WINNING_ENERGY;
                        boolean opponentSprinting = player.getPosition() >= 90;
                        boolean computerFrozen = opponent.isFrozen();
                        boolean excessEnergy = opponent.getEnergy() >= 1500;
                        if (opponentCloseToWin || opponentSprinting || computerFrozen || excessEnergy) {
                            shouldUsePowerup = true;
                        }
                    }
                } else if (opponent instanceof game.engine.monsters.Dasher) {
                    game.engine.monsters.Dasher dasher = (game.engine.monsters.Dasher) opponent;
                    if (dasher.getMomentumTurns() == 0) {
                        boolean farBehind = (player.getPosition() - dasher.getPosition()) >= 20;
                        boolean inSprintZone = dasher.getPosition() >= 50 && dasher.getPosition() <= 95;
                        boolean winnableWithPowerup = dasher.getEnergy() >= 1500;
                        if (farBehind || inSprintZone || winnableWithPowerup) {
                            shouldUsePowerup = true;
                        }
                    }
                } else if (opponent instanceof game.engine.monsters.MultiTasker) {
                    game.engine.monsters.MultiTasker multitasker = (game.engine.monsters.MultiTasker) opponent;
                    if (multitasker.getNormalSpeedTurns() == 0) {
                        boolean farBehind = (player.getPosition() - multitasker.getPosition()) >= 20;
                        boolean inSprintZone = multitasker.getPosition() >= 60 && multitasker.getPosition() <= 95;
                        boolean winnableWithPowerup = multitasker.getEnergy() >= 1500;
                        if (farBehind || inSprintZone || winnableWithPowerup) {
                            shouldUsePowerup = true;
                        }
                    }
                } else if (opponent instanceof game.engine.monsters.Schemer) {
                    boolean opponentNearWin = player.getPosition() >= 85 && player.getEnergy() >= Constants.WINNING_ENERGY;
                    boolean opponentDroppable = player.getEnergy() >= 1000 && player.getEnergy() < 1000 + Constants.SCHEMER_STEAL;
                    boolean excessEnergy = opponent.getEnergy() >= 1600;
                    if ((opponentNearWin && opponentDroppable) || excessEnergy) {
                        shouldUsePowerup = true;
                    }
                }
            }

            if (shouldUsePowerup) {
                usePowerup(false);
                PauseTransition powerupDelay = new PauseTransition(Duration.seconds(1.5));
                powerupDelay.setOnFinished(e -> {
                    executeTurn(false);
                });
                powerupDelay.play();
            } else {
                executeTurn(false);
            }
        });
        initialDelay.play();
    }

    /** Log + play Powerup.mp3 the first time a monster reaches ≥1000 energy. */
    /** Plays Powerup.mp3 and logs when a monster first reaches 500 energy (powerup cost). */
    private void checkPowerupMilestone() {
        Monster p = game.getPlayer(), o = game.getOpponent();
        if (!playerPowerupLogged && p.getEnergy() >= Constants.POWERUP_COST) {
            playerPowerupLogged = true;
            playSfx("Powerup.mp3");
            addLog(true, p.getName() + " has " + Constants.POWERUP_COST
                + " energy — powerup is ready!", "gain");
        }
        if (!opponentPowerupLogged && o.getEnergy() >= Constants.POWERUP_COST) {
            opponentPowerupLogged = true;
            playSfx("Powerup.mp3");
            addLog(false, o.getName() + " has " + Constants.POWERUP_COST
                + " energy — powerup is ready!", "gain");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POWERUP
    // ═══════════════════════════════════════════════════════════════════════

    private boolean playerPowerupConfirming = false;
    private boolean opponentPowerupConfirming = false;

    @FXML private void onPlayerUsePowerup()   { 
        if (!animating) {
            if (!playerPowerupConfirming) {
                playerPowerupConfirming = true;
                playerPowerupBtn.setText("Confirm? [S]");
                playerPowerupBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: black;");
            } else {
                usePowerup(true); 
            }
        }
    }
    @FXML private void onOpponentUsePowerup() { 
        if (!animating) {
            if (!opponentPowerupConfirming) {
                opponentPowerupConfirming = true;
                opponentPowerupBtn.setText("Confirm? [L]");
                opponentPowerupBtn.setStyle("-fx-background-color: #eab308; -fx-text-fill: black;");
            } else {
                usePowerup(false); 
            }
        }
    }

    private void resetPowerupConfirmations() {
        playerPowerupConfirming = false;
        opponentPowerupConfirming = false;
        playerPowerupBtn.setText("Use Powerup [S]");
        playerPowerupBtn.setStyle("");
        opponentPowerupBtn.setText("Use Powerup [L]");
        opponentPowerupBtn.setStyle("");
    }

    private void usePowerup(boolean isPlayer) {
        Monster current = game.getCurrent();
        int oldEnergy = current.getEnergy();
        try {
            game.usePowerup();
            String pwName = getPowerupName(current);
            playSfx(getPowerupSfx(current));
            addLog(isPlayer, current.getName() + " used " + pwName
                + "! (-" + Constants.POWERUP_COST + " energy)", "action");
            // Enable/disable buttons immediately — don't wait for energy animation
            checkPowerupAvailability();
            animateEnergyChange(isPlayer, oldEnergy, current.getEnergy(), this::refreshAllUI);
        } catch (game.engine.exceptions.OutOfEnergyException e) {
            if (isPlayer) {
                showExceptionPopup("Not Enough Energy", e.getMessage());
            }
            addLog(isPlayer, "Not enough energy for powerup! (Need " + Constants.POWERUP_COST + ")", "warn");
        }
    }

    private void checkPowerupAvailability() {
        resetPowerupConfirmations();
        Monster p = game.getPlayer(), o = game.getOpponent();
        boolean playerTurn   = game.getCurrent() == p;
        boolean opponentTurn = game.getCurrent() == o;
        if (vsComputer) {
            playerPowerupBtn.setDisable(!playerTurn || p.getEnergy() < Constants.POWERUP_COST);
            opponentPowerupBtn.setDisable(true);
        } else {
            playerPowerupBtn.setDisable(!playerTurn   || p.getEnergy() < Constants.POWERUP_COST);
            opponentPowerupBtn.setDisable(!opponentTurn || o.getEnergy() < Constants.POWERUP_COST);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MOVEMENT ANIMATION — step by step through each cell
    // ═══════════════════════════════════════════════════════════════════════

    private void animateMovement(Monster m, boolean isPlayer,
                                 ImageView pTok, ImageView oTok,
                                 int from, int to, Runnable onDone) {
        // Compute actual forward steps for this call (used by SwapperCard teleport etc.)
        int steps = from == to ? 0 : (to - from + 100) % 100;
        animateMovement(m, isPlayer, pTok, oTok, from, to, steps, onDone);
    }

    private void animateMovement(Monster m, boolean isPlayer,
                                 ImageView pTok, ImageView oTok,
                                 int from, int to, int maxSteps, Runnable onDone) {
        ImageView token = isPlayer ? pTok : oTok;
        // CRITICAL: check from==to BEFORE computing steps.
        if (from == to) { onDone.run(); return; }

        // Always use maxSteps as the true number of steps to animate.
        // Do NOT recompute from (to - from + 100) % 100, which gives the wrong
        // answer for wrap-around moves (e.g. MultiTasker at 97 rolling 6 → lands
        // at 0: (0-97+100)%100 = 3, which is correct, but if maxSteps was passed
        // as the pre-wrap distance it could mismatch).
        int steps = maxSteps;
        if (steps <= 0) { onDone.run(); return; }

        SequentialTransition seq = new SequentialTransition();
        for (int s = 1; s <= steps; s++) {
            final int tIdx = (from + s) % 100;
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(e -> {
                for (StackPane c : cellNodes) if (c != null) c.getChildren().remove(token);
                if (cellNodes[tIdx] != null) {
                    StackPane.setAlignment(token, isPlayer ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
                    cellNodes[tIdx].getChildren().add(token);
                }
            });
            seq.getChildren().add(pause);
        }
        seq.setOnFinished(e -> {
            // Brief pause after landing before processing cell effect
            PauseTransition land = new PauseTransition(Duration.millis(400));
            land.setOnFinished(ev -> onDone.run());
            land.play();
        });
        seq.play();
    }

    /**
     * Conveyor belt slide: token smoothly glides across the belt image
     * from 'from' cell to 'to' cell using a TranslateTransition.
     *
     * Since the token is placed inside a cell StackPane (which uses its own layout),
     * we achieve the visual slide by:
     *  1. Keeping the token in its current cell (from)
     *  2. Translating it toward the destination using screen coordinates
     *  3. Snapping it into the destination cell when done
     *
     * Design pattern: Template Method — the slide geometry is computed here,
     * the token placement delegates to the standard remove/add pattern.
     */
    private void animateConveyorSlide(boolean isPlayer, int from, int to, long durationMs, Runnable onDone) {
        ImageView token = isPlayer ? playerToken : opponentToken;
        if (from == to) { onDone.run(); return; }

        // Compute pixel centres of from and to cells within the 700x700 grid
        int[] fromRC = indexToRowCol(from), toRC = indexToRowCol(to);
        double fromX = fromRC[1] * CELL_W + CELL_W / 2.0;
        double fromY = (9 - fromRC[0]) * CELL_H + CELL_H / 2.0;
        double toX   = toRC[1]   * CELL_W + CELL_W / 2.0;
        double toY   = (9 - toRC[0]) * CELL_H + CELL_H / 2.0;

        // Translation delta (relative to token's current position inside its cell)
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;

        // Reset any previous translation
        token.setTranslateX(0); token.setTranslateY(0);

        // Step offset to align token center with cell center
        double startOffsetX = (CELL_W - 36.0) / 2.0;
        double startOffsetY = (CELL_H - 36.0) / 2.0;
        if (!isPlayer) {
            startOffsetX = -startOffsetX;
        }

        Transition mainAnim;
        double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (dist < 200.0) {
            // Smooth single-phase slide for short belts (avoids stuttering over short distances)
            TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), token);
            slide.setFromX(0); slide.setFromY(0);
            slide.setToX(deltaX); slide.setToY(deltaY);
            slide.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            mainAnim = slide;
        } else {
            // Premium 3-phase slide for long belts
            long stepDuration = 200;
            long rideDuration = Math.max(200, durationMs - (stepDuration * 2));

            double rideStartX = startOffsetX;
            double rideStartY = startOffsetY;
            double rideEndX = deltaX + startOffsetX;
            double rideEndY = deltaY + startOffsetY;

            // Stage 1: Step onto center of conveyor belt
            TranslateTransition stepOn = new TranslateTransition(Duration.millis(stepDuration), token);
            stepOn.setFromX(0); stepOn.setFromY(0);
            stepOn.setToX(rideStartX); stepOn.setToY(rideStartY);
            stepOn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            // Stage 2: Smooth ride down the center of the belt
            TranslateTransition ride = new TranslateTransition(Duration.millis(rideDuration), token);
            ride.setFromX(rideStartX); ride.setFromY(rideStartY);
            ride.setToX(rideEndX); ride.setToY(rideEndY);
            ride.setInterpolator(javafx.animation.Interpolator.LINEAR); // Linear for constant speed on belt

            // Stage 3: Step off conveyor belt to destination alignment
            TranslateTransition stepOff = new TranslateTransition(Duration.millis(stepDuration), token);
            stepOff.setFromX(rideEndX); stepOff.setFromY(rideEndY);
            stepOff.setToX(deltaX); stepOff.setToY(deltaY);
            stepOff.setInterpolator(javafx.animation.Interpolator.EASE_IN);

            mainAnim = new SequentialTransition(stepOn, ride, stepOff);
        }

        mainAnim.setOnFinished(e -> {
            // Reset translation and snap token into destination cell
            token.setTranslateX(0); token.setTranslateY(0);
            for (StackPane c : cellNodes) if (c != null) c.getChildren().remove(token);
            if (cellNodes[to] != null) {
                StackPane.setAlignment(token, isPlayer ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
                cellNodes[to].getChildren().add(token);
            }
            PauseTransition pause = new PauseTransition(Duration.millis(350));
            pause.setOnFinished(ev -> onDone.run());
            pause.play();
        });
        mainAnim.play();
    }

    /** Contamination sock teleport: flash effect then appear at destination. */
    private void animateSockTeleport(boolean isPlayer, int from, int to, Runnable onDone) {
        ImageView token = isPlayer ? playerToken : opponentToken;
        // Fade out from current cell
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), token);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            // Teleport token
            for (StackPane c : cellNodes) if (c != null) c.getChildren().remove(token);
            if (cellNodes[to] != null) {
                StackPane.setAlignment(token, isPlayer ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
                cellNodes[to].getChildren().add(token);
            }
            // Fade in at destination
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), token);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(ev -> onDone.run());
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ENERGY BAR ANIMATION
    // ═══════════════════════════════════════════════════════════════════════

    private void animateEnergyChange(boolean isPlayer, int oldE, int newE, Runnable onDone) {
        animateEnergyChange(isPlayer, oldE, newE, false, onDone);
    }

    private void animateEnergyChange(boolean isPlayer, int oldE, int newE, boolean muteSound, Runnable onDone) {
        if (oldE == newE) { onDone.run(); return; }
        if (!muteSound) {
            if (newE > oldE) playSfx("CanisterIncrease.mp3");
            else playSfx("CanisterDecrease.mp3");
        }
        Pane fill = isPlayer ? playerEnergyFill : opponentEnergyFill;
        Label lbl  = isPlayer ? playerEnergy    : opponentEnergy;
        Monster m  = isPlayer ? game.getPlayer() : game.getOpponent();

        Transition t = new Transition() {
            {
                setCycleDuration(Duration.millis(700));
                setInterpolator(Interpolator.EASE_BOTH);
            }
            @Override
            protected void interpolate(double frac) {
                int val = (int)(oldE + (newE - oldE) * frac);
                double ratio = Math.max(0.0, Math.min(1.0, (double) val / WINNING_ENERGY));
                double h = ratio * MAX_BAR_H;
                fill.setPrefHeight(h); fill.setMaxHeight(h);
                lbl.setText(String.valueOf(val));
            }
        };
        t.setOnFinished(e -> { updateEnergyBar(m, fill, lbl); onDone.run(); });
        t.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CARD POPUP
    // ═══════════════════════════════════════════════════════════════════════

    /** Plays an entrance ScaleTransition on popupContent then calls onReady. */
    private void animatePopupIn(Runnable onReady) {
        popupContent.setScaleX(0.1); popupContent.setScaleY(0.1);
        popupContent.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), popupContent);
        st.setFromX(0.1); st.setFromY(0.1); st.setToX(1.0); st.setToY(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), popupContent);
        ft.setFromValue(0); ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> { if (onReady != null) onReady.run(); });
        pt.play();
    }

    /** Hides popup with a scale-out transition then calls onDone. */
    private void animatePopupOut(Runnable onDone) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), popupContent);
        st.setToX(0.1); st.setToY(0.1);
        FadeTransition ft = new FadeTransition(Duration.millis(200), popupContent);
        ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> { popupOverlay.setVisible(false); if (onDone != null) onDone.run(); });
        pt.play();
    }

    private void showCardPopup(Card card, Runnable onDone) {
        popupContent.getChildren().clear();
        // Constrain to content size — wide enough for the card image
        popupContent.setMaxWidth(320);
        popupContent.setMaxHeight(Region.USE_PREF_SIZE);
        popupContent.setPadding(new Insets(16, 20, 16, 20));

        // Step 1: play CardDraw.mp3 immediately (dispenser sound)
        playSfx("CardDraw.mp3");

        // Dispenser image
        ImageView dispenserImg = new ImageView(loadImage(UI_BASE + "CardDispenser.png"));
        dispenserImg.setFitWidth(300); dispenserImg.setFitHeight(300); dispenserImg.setPreserveRatio(true);

        // Card image — no title or description
        String cardName = card != null ? card.getName() : "";
        String imgFile  = CARD_IMAGES.getOrDefault(cardName, "Unkown card.png");
        ImageView cardImg = new ImageView(loadImage(CARD_IMG_BASE + imgFile));
        cardImg.setFitWidth(180); cardImg.setFitHeight(240); cardImg.setPreserveRatio(true);
        cardImg.setOpacity(0); cardImg.setTranslateY(-80);

        popupContent.getChildren().addAll(dispenserImg, cardImg);
        popupOverlay.setVisible(true);
        animatePopupIn(null); // entrance animation

        // Card slides out of dispenser after a short delay
        FadeTransition appear = new FadeTransition(Duration.millis(550), cardImg);
        appear.setFromValue(0); appear.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(550), cardImg);
        slide.setFromY(-80); slide.setToY(0);

        ParallelTransition pt = new ParallelTransition(appear, slide);
        pt.setDelay(Duration.millis(500)); // dispenser sound plays, then card appears
        pt.setOnFinished(e -> {
            // Step 2: play card-specific SFX AFTER card is visible, then wait for it to finish
            playCardSpecificSfxWithCallback(card, () -> {
                // Reset popup sizing back to auto for other popup types
                popupContent.setMaxWidth(Region.USE_PREF_SIZE);
                popupContent.setMaxHeight(Region.USE_PREF_SIZE);
                popupContent.setPadding(new Insets(20, 28, 20, 28));
                animatePopupOut(onDone);
            });
        });
        pt.play();
    }

    /** Plays the card-specific sound effect after the card has been revealed. */
    private void playCardSpecificSfx(Card card) {
        playCardSpecificSfxWithCallback(card, null);
    }

    /**
     * Plays the card-specific SFX then calls onDone after it finishes + 800ms.
     *
     * Robustness guarantees:
     *  1. setOnError: if MediaPlayer errors, onDone fires immediately
     *  2. Hard 3.5s timeout: onDone fires even if onEndOfMedia never triggers
     *  3. Once-flag: ensures onDone is called exactly once regardless of which
     *     path fires first (audio finish, error, or timeout)
     */
    private void playCardSpecificSfxWithCallback(Card card, Runnable onDone) {
        if (card == null) { scheduleCallback(800, onDone); return; }
        String sfxFile;
        String _cn = card.getName();
        if ("2319 Alert".equals(_cn)) sfxFile = "2319.mp3";
        else if ("Contamination Code".equals(_cn)) sfxFile = "ContaminationAlert.mp3";
        else if ("Small Snatcher".equals(_cn)||"Sneaky Thief".equals(_cn)||"Mega Drain".equals(_cn)) sfxFile = "EnergyStealAll.mp3";
        else if ("Super Shield".equals(_cn)) sfxFile = "SuperShield.mp3";
        else if ("Mind Scramble".equals(_cn)) sfxFile = "MindScramble.mp3";
        else if ("Total Confusion".equals(_cn)) sfxFile = "TotalConfusion.mp3";
        else sfxFile = null;
        if (sfxFile == null) { scheduleCallback(800, onDone); return; }

        URL url = null;
        try { url = getClass().getResource(SFX_BASE + sfxFile); }
        catch (Exception ignored) {}
        if (url == null) { scheduleCallback(800, onDone); return; }

        // Once-flag: guarantees onDone is called exactly once
        final boolean[] called = {false};
        final Runnable safeOnDone = () -> Platform.runLater(() -> {
            if (!called[0]) {
                called[0] = true;
                scheduleCallback(800, onDone);
            }
        });

        // Hard 8.0s timeout — fires even if MediaPlayer never calls onEndOfMedia
        PauseTransition timeout = new PauseTransition(Duration.seconds(8.0));
        timeout.setOnFinished(e -> safeOnDone.run());
        timeout.play();

        try {
            MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
            activeMediaPlayers.add(mp);
            mp.setVolume(ResourceManager.getSfxVolume());
            mp.setOnEndOfMedia(() -> { activeMediaPlayers.remove(mp); mp.dispose(); safeOnDone.run(); });
            mp.setOnError(() -> { activeMediaPlayers.remove(mp); mp.dispose(); safeOnDone.run(); });
            mp.setOnStopped(() -> { activeMediaPlayers.remove(mp); safeOnDone.run(); });
            mp.play();
        } catch (Exception ignored) { safeOnDone.run(); }
    }

    /** Schedules a callback after delayMs milliseconds. */
    private void scheduleCallback(long delayMs, Runnable onDone) {
        if (onDone == null) return;
        PauseTransition p = new PauseTransition(Duration.millis(delayMs));
        p.setOnFinished(e -> Platform.runLater(onDone));
        p.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MONSTER CELL POPUP
    // ═══════════════════════════════════════════════════════════════════════

    private void showMonsterCellPopup(Monster landing, Monster stationed, boolean sameRole,
                                      int oldE, int newE, int oldOtherE, int newOtherE,
                                      boolean isPlayer, MediaPlayer mp, Runnable onDone) {
        popupContent.getChildren().clear();
        popupContent.setMaxWidth(600);
        popupContent.setMaxHeight(Region.USE_PREF_SIZE);
        popupContent.setPadding(new Insets(20, 28, 20, 28));

        Label title = new Label(sameRole ? "Ally Encountered!" : "Enemy Encounter!");
        title.getStyleClass().add("popup-title");

        HBox row = new HBox(30);
        row.setAlignment(Pos.CENTER);
        Pane[] bar1 = new Pane[1]; Label[] lbl1 = new Label[1];
        Pane[] bar2 = new Pane[1]; Label[] lbl2 = new Label[1];
        row.getChildren().addAll(
            makeMonsterCard(landing, "You", oldE, bar1, lbl1),
            makeMonsterCard(stationed, "Stationed", oldOtherE, bar2, lbl2)
        );

        String effectText = sameRole
            ? landing.getName() + " used " + getPowerupName(landing) + " for free!"
            : (newE != oldE ? "Energies swapped!" : "No effect — not enough energy.");
        Label effect = new Label(effectText);
        effect.getStyleClass().add("popup-body"); effect.setWrapText(true);

        popupContent.getChildren().addAll(title, row, effect);
        popupOverlay.setVisible(true);

        boolean[] audioFinished = { mp == null };
        boolean[] animFinished = { false };
        boolean[] dismissed = { false };

        Runnable checkAndDismiss = () -> {
            if (audioFinished[0] && animFinished[0] && !dismissed[0]) {
                dismissed[0] = true;
                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(ev -> {
                    popupContent.setMaxWidth(Region.USE_PREF_SIZE);
                    popupContent.setMaxHeight(Region.USE_PREF_SIZE);
                    popupContent.setPadding(new Insets(20, 28, 20, 28));
                    animatePopupOut(onDone);
                });
                pause.play();
            }
        };

        if (mp != null) {
            mp.setOnEndOfMedia(() -> {
                activeMediaPlayers.remove(mp);
                mp.dispose();
                audioFinished[0] = true;
                Platform.runLater(checkAndDismiss);
            });
            mp.setOnError(() -> {
                activeMediaPlayers.remove(mp);
                mp.dispose();
                audioFinished[0] = true;
                Platform.runLater(checkAndDismiss);
            });
        }

        // Fallback in case audio never finishes or hangs
        PauseTransition fallback = new PauseTransition(Duration.millis(9000));
        fallback.setOnFinished(e -> {
            if (mp != null) {
                activeMediaPlayers.remove(mp);
                try { mp.stop(); mp.dispose(); } catch (Exception ignored) {}
            }
            audioFinished[0] = true;
            Platform.runLater(checkAndDismiss);
        });
        fallback.play();

        animatePopupIn(() -> {
            // Play sound for the human player's monster only
            int playerOld = isPlayer ? oldE : oldOtherE;
            int playerNew = isPlayer ? newE : newOtherE;
            if (playerNew > playerOld) playSfx("CanisterIncrease.mp3");
            else if (playerNew < playerOld) playSfx("CanisterDecrease.mp3");

            Transition t = new Transition() {
                {
                    setCycleDuration(Duration.millis(900));
                    setInterpolator(Interpolator.EASE_BOTH);
                }
                @Override
                protected void interpolate(double frac) {
                    int cE = oldE + (int)((newE - oldE) * frac);
                    int cOE = oldOtherE + (int)((newOtherE - oldOtherE) * frac);
                    
                    lbl1[0].setText(String.valueOf(cE));
                    double r1 = Math.min(1.0, cE / 1000.0);
                    bar1[0].setPrefHeight(r1 * 28.0);
                    bar1[0].setMaxHeight(r1 * 28.0);

                    lbl2[0].setText(String.valueOf(cOE));
                    double r2 = Math.min(1.0, cOE / 1000.0);
                    bar2[0].setPrefHeight(r2 * 28.0);
                    bar2[0].setMaxHeight(r2 * 28.0);
                }
            };
            t.setOnFinished(e -> { animFinished[0] = true; Platform.runLater(checkAndDismiss); });
            t.play();
        });
    }

    private VBox makeMonsterCard(Monster m, String label, int startEnergy, Pane[] outBarRef, Label[] outLblRef) {
        VBox c = new VBox(8); c.setAlignment(Pos.CENTER);
        c.setStyle("-fx-background-color:rgba(10,15,35,0.85);-fx-background-radius:12;-fx-padding:15;");

        ImageView iv = new ImageView(loadImage(monsterImagePath(m)));
        iv.setFitWidth(105); iv.setFitHeight(105); iv.setPreserveRatio(true);

        Label name = new Label(m.getName());
        name.setStyle("-fx-font-family:'Futura';-fx-font-size:18px;-fx-text-fill:white;-fx-font-weight:bold;");

        StackPane miniCanister = new StackPane();
        miniCanister.setMinSize(75, 98); miniCanister.setMaxSize(75, 98);
        AnchorPane barPane = new AnchorPane();
        barPane.setMinSize(75, 98);
        Pane barTrack = new Pane();
        AnchorPane.setLeftAnchor(barTrack, 27.0); AnchorPane.setRightAnchor(barTrack, 27.0);
        AnchorPane.setBottomAnchor(barTrack, 34.0); AnchorPane.setTopAnchor(barTrack, 36.0);
        barTrack.setStyle("-fx-background-color:rgba(10,10,30,0.75);-fx-background-radius:4;");
        
        double ratio = Math.min(1.0, startEnergy / 1000.0);
        double fillH = ratio * 28.0;
        Pane barFill = new Pane();
        AnchorPane.setLeftAnchor(barFill, 27.0); AnchorPane.setRightAnchor(barFill, 27.0);
        AnchorPane.setBottomAnchor(barFill, 34.0);
        barFill.setPrefHeight(fillH); barFill.setMaxHeight(fillH);
        barFill.setStyle("-fx-background-color:linear-gradient(to top,#b91c1c,#ef4444,#fca5a5);-fx-background-radius:4;");
        if (outBarRef != null && outBarRef.length > 0) outBarRef[0] = barFill;

        barPane.getChildren().addAll(barTrack, barFill);
        ImageView canImg = new ImageView(loadImage(UI_BASE + "Cannister.png"));
        canImg.setFitWidth(75); canImg.setFitHeight(75); canImg.setPreserveRatio(true);
        canImg.setMouseTransparent(true);
        StackPane.setAlignment(canImg, Pos.CENTER);
        miniCanister.getChildren().addAll(barPane, canImg);

        Label en = new Label(String.valueOf(startEnergy));
        en.setStyle("-fx-font-family:'Futura';-fx-font-size:20px;-fx-text-fill:#00ff99;-fx-font-weight:bold;");
        if (outLblRef != null && outLblRef.length > 0) outLblRef[0] = en;

        Label rl = new Label(label + " (" + m.getOriginalRole() + ")");
        rl.setStyle("-fx-font-family:'Futura';-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        c.getChildren().addAll(iv, name, miniCanister, en, rl);
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXCEPTION POPUP (auto-dismiss)
    // ═══════════════════════════════════════════════════════════════════════

    private void showExceptionPopup(String title, String message) {
        showExceptionPopup(title, message, null);
    }

    private void showExceptionPopup(String title, String message, Runnable onDone) {
        animating = true;
        popupContent.getChildren().clear();
        popupContent.setMaxWidth(360);
        popupContent.setMaxHeight(Region.USE_PREF_SIZE);
        popupContent.setPadding(new Insets(16, 24, 16, 24));
        Label t = new Label(title);
        t.getStyleClass().add("popup-error");
        t.setAlignment(javafx.geometry.Pos.CENTER);
        t.setMaxWidth(Double.MAX_VALUE);
        Label m = new Label(message);
        m.getStyleClass().add("popup-body");
        m.setWrapText(true);
        m.setMaxWidth(320);
        m.setAlignment(javafx.geometry.Pos.CENTER);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("btn-settings-action");
        okBtn.setOnAction(ev -> animatePopupOut(() -> {
            animating = false;
            if (onDone != null) onDone.run();
        }));
        popupContent.getChildren().addAll(t, m, okBtn);
        popupOverlay.setVisible(true);
        animatePopupIn(null);
    }

    private void showExceptionPopupAutoDismiss(String title, String message, Runnable onDone) {
        popupContent.getChildren().clear();
        popupContent.setMaxWidth(360);
        popupContent.setMaxHeight(Region.USE_PREF_SIZE);
        popupContent.setPadding(new Insets(16, 24, 16, 24));
        Label t = new Label(title);
        t.getStyleClass().add("popup-error");
        t.setAlignment(javafx.geometry.Pos.CENTER);
        t.setMaxWidth(Double.MAX_VALUE);
        Label m = new Label(message);
        m.getStyleClass().add("popup-body");
        m.setWrapText(true);
        m.setMaxWidth(320);
        m.setAlignment(javafx.geometry.Pos.CENTER);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        popupContent.getChildren().addAll(t, m);
        popupOverlay.setVisible(true);
        animatePopupIn(() -> {
            PauseTransition pt = new PauseTransition(Duration.seconds(2));
            pt.setOnFinished(ev -> animatePopupOut(onDone));
            pt.play();
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DOOR IMAGE REPLACEMENT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Snapshots all door cell indices that are currently activated.
     * Called BEFORE game.playTurn() so we can detect which door was newly activated.
     */
    private java.util.Set<Integer> snapshotActivatedDoors() {
        java.util.Set<Integer> activated = new java.util.HashSet<>();
        for (int i = 1; i < 100; i += 2) {  // doors at odd indices
            Cell c = getCellByIndex(i);
            if (c instanceof DoorCell && ((DoorCell) c).isActivated()) activated.add(i);
        }
        return activated;
    }

    private void replaceDoorImage(int index) {
        if (cellNodes[index] == null) return;
        String closedFile = DOOR_FILE.get(index); if (closedFile == null) return;
        java.util.regex.Matcher mat = Pattern.compile("^([A-Za-z]+)\\d+\\.png$").matcher(closedFile);
        if (!mat.matches()) return;
        String openedFile = COLOR_TO_OPENED.get(mat.group(1)); if (openedFile == null) return;
        StackPane cell = cellNodes[index];
        cell.getChildren().removeIf(n ->
            n instanceof ImageView && !"sock-dest".equals(n.getUserData())
            && n != playerToken && n != opponentToken);
        addImage(cell, DOOR_BASE+"Opened/"+openedFile, 58, Pos.CENTER);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SOUND SYSTEM
    // ═══════════════════════════════════════════════════════════════════════

    private void playSfx(String filename) {
        try {
            URL url = getClass().getResource(SFX_BASE + filename);
            if (url == null) return;
            MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
            activeMediaPlayers.add(mp);
            mp.setVolume(ResourceManager.getSfxVolume());
            mp.setOnEndOfMedia(() -> { activeMediaPlayers.remove(mp); mp.dispose(); });
            mp.setOnError(() -> { activeMediaPlayers.remove(mp); mp.dispose(); });
            mp.play();
        } catch (Exception ignored) {}
    }

    private void playSfxWithSyncedAnimation(String filename, boolean isPlayer, int from, int to, Runnable onDone) {
        try {
            URL url = getClass().getResource(SFX_BASE + filename);
            if (url == null) {
                animateConveyorSlide(isPlayer, from, to, 1000, onDone);
                return;
            }
            Media media = new Media(url.toExternalForm());
            MediaPlayer mp = new MediaPlayer(media);
            activeMediaPlayers.add(mp);
            mp.setVolume(ResourceManager.getSfxVolume());
            final boolean[] slideStarted = {false};
            
            // Clean-up and stop MediaPlayer once the slide animation finishes
            Runnable cleanOnDone = () -> {
                try {
                    activeMediaPlayers.remove(mp);
                    mp.stop();
                    mp.dispose();
                } catch (Exception ignored) {}
                onDone.run();
            };

            mp.setOnEndOfMedia(() -> { activeMediaPlayers.remove(mp); mp.dispose(); });
            mp.setOnError(() -> { 
                activeMediaPlayers.remove(mp); mp.dispose(); 
                Platform.runLater(() -> {
                    if (!slideStarted[0]) {
                        slideStarted[0] = true;
                        animateConveyorSlide(isPlayer, from, to, 1000, onDone);
                    }
                });
            });
            mp.setOnReady(() -> {
                double dur = media.getDuration().toMillis();
                long durationMs = (long) (Double.isNaN(dur) ? 1000 : dur);
                
                // For short conveyor belts, override slide duration to be faster
                double dist = getConveyorDistance(from, to);
                long slideDurationMs = durationMs;
                if (dist < 200.0) {
                    slideDurationMs = dist <= 80.0 ? 500 : 850;
                }
                
                mp.play();
                final long finalSlideDur = slideDurationMs;
                Platform.runLater(() -> {
                    if (!slideStarted[0]) {
                        slideStarted[0] = true;
                        animateConveyorSlide(isPlayer, from, to, finalSlideDur, cleanOnDone);
                    }
                });
            });
        } catch (Exception e) {
            animateConveyorSlide(isPlayer, from, to, 1000, onDone);
        }
    }

    private double getConveyorDistance(int from, int to) {
        int[] fromRC = indexToRowCol(from), toRC = indexToRowCol(to);
        double fromX = fromRC[1] * CELL_W + CELL_W / 2.0;
        double fromY = (9 - fromRC[0]) * CELL_H + CELL_H / 2.0;
        double toX   = toRC[1]   * CELL_W + CELL_W / 2.0;
        double toY   = (9 - toRC[0]) * CELL_H + CELL_H / 2.0;
        return Math.sqrt((toX - fromX) * (toX - fromX) + (toY - fromY) * (toY - fromY));
    }

    private void playMoveSfx() {
        stopMoveSfx();
        try {
            URL url = getClass().getResource(SFX_BASE + "Running.mp3");
            if (url == null) return;
            moveSfxPlayer = new MediaPlayer(new Media(url.toExternalForm()));
            moveSfxPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            moveSfxPlayer.setVolume(ResourceManager.getSfxVolume() * 0.5); moveSfxPlayer.play();
        } catch (Exception ignored) {}
    }

    private void stopMoveSfx() {
        if (moveSfxPlayer != null) {
            try {
                moveSfxPlayer.stop();
                moveSfxPlayer.dispose();
            } catch (Exception ignored) {}
            moveSfxPlayer = null;
        }
    }

    private MediaPlayer playVoiceLine(Monster m, boolean ally) {
        String file = getShortName(m) + (ally ? "Ally" : "Enemy") + ".mp3";
        try {
            URL url = getClass().getResource(VOICE_BASE + file);
            if (url == null) return null;
            MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
            activeMediaPlayers.add(mp);
            mp.setVolume(ResourceManager.getSfxVolume() * 1.15); // slightly louder
            mp.setOnEndOfMedia(() -> { activeMediaPlayers.remove(mp); mp.dispose(); });
            mp.setOnError(() -> { activeMediaPlayers.remove(mp); mp.dispose(); });
            mp.play();
            return mp;
        } catch (Exception ignored) {}
        return null;
    }

    private void startBoardOST() {
        // Stop everything before starting (clears ResourceManager + local player)
        ResourceManager.stopCurrentOST();
        if (ostPlayer != null) { ostPlayer.stop(); ostPlayer.dispose(); ostPlayer = null; }
        ostIndex = 0;
        playNextOST();
    }

    private void playNextOST() {
        ResourceManager.MusicMode mode = ResourceManager.getMusicMode();
        if (mode == ResourceManager.MusicMode.OFF) {
            if (ostPlayer != null) { ostPlayer.stop(); ostPlayer.dispose(); ostPlayer = null; }
            return;
        }
        String ostFolder = mode == ResourceManager.MusicMode.ALTERNATE
            ? "/game/resources/audio/alternate ost/"
            : "/game/resources/audio/ost/";
        try {
            URL url = getClass().getResource(ostFolder + OST_FILES[ostIndex]);
            if (url == null) return;
            // Stop both the local player AND whatever ResourceManager is tracking
            ResourceManager.stopCurrentOST();
            if (ostPlayer != null) { ostPlayer.stop(); ostPlayer.dispose(); }
            ostPlayer = new MediaPlayer(new Media(url.toExternalForm()));
            ostPlayer.setVolume(ResourceManager.getMusicVolume());
            ostPlayer.setOnEndOfMedia(() -> {
                ostIndex = (ostIndex + 1) % OST_FILES.length;
                Platform.runLater(this::playNextOST);
            });
            ostPlayer.play();
            // Register with ResourceManager so stopCurrentOST() from other screens works
            ResourceManager.setCurrentOSTPlayer(ostPlayer);
        } catch (Exception ignored) {}
    }

    private String getPowerupSfx(Monster m) {
        return "Powerup.mp3";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PLAYER LOG
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Updates the dice ImageView for the active player.
     * Shows Dice1.png–Dice6.png for rolls 1–6.
     * Plays DiceRoll.mp3 on each roll.
     */
    private void updateDiceDisplay(boolean isPlayer, int roll) {
        playSfx("DiceRoll.mp3");
        Platform.runLater(() -> {
            ImageView target = isPlayer ? playerDiceImage : opponentDiceImage;
            if (target == null) return;
            target.setVisible(true);

            // Shuffling effect: 8 frames, every 75ms for 600ms total
            Timeline timeline = new Timeline();
            int numFrames = 8;
            Random rand = new Random();
            for (int i = 0; i < numFrames; i++) {
                final int frameIndex = i;
                KeyFrame kf = new KeyFrame(Duration.millis((i + 1) * 75), event -> {
                    if (frameIndex == numFrames - 1) {
                        // Final roll image
                        if (roll >= 1 && roll <= 6) {
                            Image img = loadImage(UI_BASE + "Dice" + roll + ".png");
                            if (img != null) target.setImage(img);
                        }
                    } else {
                        // Random intermediate dice face
                        int randRoll = rand.nextInt(6) + 1;
                        Image img = loadImage(UI_BASE + "Dice" + randRoll + ".png");
                        if (img != null) target.setImage(img);
                    }
                });
                timeline.getKeyFrames().add(kf);
            }
            timeline.play();
        });
    }

    /**
     * Infers the raw 1-6 dice roll from effective steps and monster type.
     * Dasher normal: effectiveSteps = roll × 2  → roll = steps / 2
     * Dasher momentum: effectiveSteps = roll × 3 → roll = steps / 3
     * MultiTasker: effectiveSteps = floor(roll / 2) — cannot recover exactly,
     *   so show the nearest valid value (steps × 2, capped to 6)
     * Others: effectiveSteps = roll directly
     */
    private int inferRawRoll(Monster m, int effectiveSteps, int momentumBefore) {
        if (m instanceof Dasher) {
            int divisor = momentumBefore > 0 ? 3 : 2;
            int raw = effectiveSteps / divisor;
            return Math.max(1, Math.min(6, raw));
        }
        if (m instanceof MultiTasker) {
            // If Focus Mode is active, it moves normally (1:1)
            if (((MultiTasker) m).getNormalSpeedTurns() > 0) {
                return Math.max(1, Math.min(6, effectiveSteps));
            }
            // Otherwise, steps = floor(roll/2); best estimate: steps*2
            int raw = Math.max(1, Math.min(6, effectiveSteps * 2));
            return raw;
        }
        // Schemer, Dynamo, and others: 1:1
        return Math.max(1, Math.min(6, effectiveSteps));
    }

    private void addLog(boolean isPlayer, String message, String type) {
        // Prefix icons make the log scannable at a glance
        String prefix;
        if ("gain".equals(type))        prefix = "+ ";
        else if ("loss".equals(type))   prefix = "- ";
        else if ("action".equals(type)) prefix = "> ";
        else if ("warn".equals(type))   prefix = "! ";
        else                            prefix = "  ";
        Platform.runLater(() -> {
            VBox logBox       = isPlayer ? playerLogBox    : opponentLogBox;
            ScrollPane scroll = isPlayer ? playerLogScroll : opponentLogScroll;
            Label entry = new Label(prefix + message);
            entry.setWrapText(true); entry.setMaxWidth(240);
            if ("gain".equals(type))        entry.getStyleClass().add("log-entry-gain");
            else if ("loss".equals(type))   entry.getStyleClass().add("log-entry-loss");
            else if ("action".equals(type)) entry.getStyleClass().add("log-entry-action");
            else if ("warn".equals(type))   entry.getStyleClass().add("log-entry-warn");
            else                            entry.getStyleClass().add("log-entry");
            logBox.getChildren().add(entry);
            scroll.applyCss(); scroll.layout();
            scroll.setVvalue(1.0);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOVER TOOLTIPS
    // ═══════════════════════════════════════════════════════════════════════

    private void attachTooltip(StackPane cell, int index) {
        if (game == null) return;
        Popup popup = new Popup(); popup.setAutoHide(true);
        cell.setOnMouseEntered(e -> {
            Cell ec = getCellByIndex(index); if (ec == null) return;
            List<String[]> rows = buildColoredRows(index, ec);
            if (rows == null || rows.isEmpty()) return;
            VBox box = new VBox(4);
            box.setStyle("-fx-background-color:rgba(20,15,10,0.72);-fx-background-radius:8;" +
                "-fx-border-color:#c8a84b;-fx-border-width:1.5;-fx-border-radius:8;-fx-padding:8 12 8 12;");
            for (String[] row : rows) {
                Label l = new Label(row[0]);
                l.setStyle("-fx-text-fill:"+row[1]+";-fx-font-family:'Futura';-fx-font-size:13px;");
                l.setMaxWidth(220); l.setWrapText(true); box.getChildren().add(l);
            }
            popup.getContent().setAll(box);
            popup.show(cell, e.getScreenX()+12, e.getScreenY()+12);
        });
        cell.setOnMouseExited(e -> popup.hide());
    }

    private List<String[]> buildColoredRows(int index, Cell cell) {
        String colorWhite = "#f0f4ff";
        String colorYellow = "#f5c518";
        String colorGreen = "#00ff99";
        String colorCyan = "#38c7ff";
        String colorRed = "#ff6060";

        if (cell instanceof DoorCell) {
            DoorCell door = (DoorCell) cell;
            String roleColor = door.getRole() == Role.SCARER ? colorRed : colorCyan;
            String statusText = door.isActivated() ? " [Exhausted]" : "";
            return java.util.Arrays.asList(
                new String[]{door.getName() + statusText, colorWhite},
                new String[]{"Role: " + door.getRole().toString(), roleColor},
                new String[]{"Energy: " + door.getEnergy(), colorGreen}
            );
        }
        if (cell instanceof MonsterCell) {
            MonsterCell monsterCell = (MonsterCell) cell;
            Monster monster = monsterCell.getCellMonster();
            if (monster == null) return null;
            String roleColor = monster.getOriginalRole() == Role.SCARER ? colorRed : colorCyan;
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{monster.getName(), colorWhite});
            rows.add(new String[]{"Type: " + monsterTypeName(monster), colorYellow});
            rows.add(new String[]{"Role: " + monster.getOriginalRole().toString(), roleColor});
            rows.add(new String[]{"Energy: " + monster.getEnergy(), colorGreen});
            return rows;
        }
        if (cell instanceof CardCell) {
            return null;
        }
        if (cell instanceof ConveyorBelt) {
            String name = CONVEYOR_DISPLAY_NAMES.getOrDefault(index, cell.getName());
            int destination = CONVEYOR_MAP.getOrDefault(index, index);
            return java.util.Arrays.asList(
                new String[]{name, colorWhite},
                new String[]{"Leads to Cell " + destination, colorCyan}
            );
        }
        if (cell instanceof ContaminationSock) {
            ContaminationSock sock = (ContaminationSock) cell;
            int destination = index + sock.getEffect();
            return java.util.Arrays.asList(
                new String[]{sock.getName(), colorWhite},
                new String[]{"Leads to Cell " + destination, colorCyan},
                new String[]{"-100 Energy penalty", colorRed}
            );
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI REFRESH HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void updateEnergyBar(Monster monster, Pane fillPane, Label energyLabel) {
        int actualEnergy = monster.getEnergy();
        double ratio = Math.max(0.0, Math.min(1.0, (double) actualEnergy / WINNING_ENERGY));
        double height = ratio * MAX_BAR_H;
        fillPane.setPrefHeight(height);
        fillPane.setMaxHeight(height);
        energyLabel.setText(String.valueOf(actualEnergy));
    }

    private void refreshAllUI() {
        Monster p = game.getPlayer(), o = game.getOpponent();
        updateEnergyBar(p, playerEnergyFill, playerEnergy);
        updateEnergyBar(o, opponentEnergyFill, opponentEnergy);
        updateStatusEffects(p, playerStatusBox);
        updateStatusEffects(o, opponentStatusBox);
        playerPosition.setText("Cell " + p.getPosition());
        opponentPosition.setText("Cell " + o.getPosition());
        // Swap canister image based on frozen state
        updateCanisterImage(p, playerCanisterImage);
        updateCanisterImage(o, opponentCanisterImage);
    }

    /** Swaps canister image to frozen variant when monster is frozen. */
    private void updateCanisterImage(Monster monster, ImageView imageView) {
        if (imageView == null) return;
        String imgName = monster.isFrozen() ? "CannisterFreezed.png" : "Cannister.png";
        Image img = loadImage(UI_BASE + imgName);
        if (img != null) {
            imageView.setImage(img);
        }
    }

    private void disableAllButtons() {
        playerPlayTurnBtn.setDisable(true);   playerPowerupBtn.setDisable(true);
        opponentPlayTurnBtn.setDisable(true); opponentPowerupBtn.setDisable(true);
    }


    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void updateStatusEffects(Monster m, HBox box) {
        box.getChildren().clear();
        boolean hasEffect = false;
        if (m.isFrozen()) {
            box.getChildren().add(createBadge("Frozen", "#38bdf8", "#0c4a6e"));
            hasEffect = true;
        }
        if (m.isConfused()) {
            String txt = m.getConfusionTurns() > 1 ? "Confused (" + m.getConfusionTurns() + ")" : "Confused";
            box.getChildren().add(createBadge(txt, "#c084fc", "#4c1d95"));
            hasEffect = true;
        }
        if (m.isShielded()) {
            box.getChildren().add(createBadge("Shielded", "#fcd34d", "#78350f"));
            hasEffect = true;
        }
        if (m instanceof Dasher && ((Dasher) m).getMomentumTurns() > 0) {
            int turns = ((Dasher) m).getMomentumTurns();
            String txt = turns > 1 ? "Momentum (" + turns + ")" : "Momentum";
            box.getChildren().add(createBadge(txt, "#34d399", "#064e3b"));
            hasEffect = true;
        }
        if (m instanceof MultiTasker && ((MultiTasker) m).getNormalSpeedTurns() > 0) {
            int turns = ((MultiTasker) m).getNormalSpeedTurns();
            String txt = turns > 1 ? "Focus (" + turns + ")" : "Focus";
            box.getChildren().add(createBadge(txt, "#f472b6", "#831843"));
            hasEffect = true;
        }
        if (!hasEffect) {
            box.getChildren().add(createBadge("Normal", "#94a3b8", "#1e293b"));
        }
    }

    private Label createBadge(String text, String color, String bg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + 
                   "; -fx-font-family: Futura; -fx-font-size: 11px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-border-color: " + color + "44; -fx-border-radius: 4;");
        return l;
    }

    private String monsterTypeName(Monster m) {
        if (m instanceof Dasher)      return "Dasher";
        if (m instanceof Dynamo)      return "Dynamo";
        if (m instanceof MultiTasker) return "MultiTasker";
        if (m instanceof Schemer)     return "Schemer";
        return "Unknown";
    }

    private String getPowerupName(Monster m) {
        if (m instanceof Dasher)      return "Momentum Rush";
        if (m instanceof Dynamo)      return "Energy Freeze";
        if (m instanceof MultiTasker) return "Focus Mode";
        if (m instanceof Schemer)     return "Chain Attack";
        return "Powerup";
    }

    private String getPowerupDescription(Monster m) {
        if (m instanceof Dasher)      return "Gain 3x movement speed for the next 3 turns.";
        if (m instanceof Dynamo)      return "Freezes the opponent, skipping their next turn.";
        if (m instanceof MultiTasker) return "Move at normal speed (not halved) for 2 turns.";
        if (m instanceof Schemer)     return "Steals energy from ALL other monsters present.";
        return "Special Powerup";
    }


    private String getShortName(Monster m) {
        String n = m.getName();
        if (n.contains("Sullivan"))  return "Sulley";
        if (n.contains("Wazowski")) return "Mike";
        if (n.contains("Randall"))  return "Randall";
        if (n.contains("Celia"))    return "Celia";
        if (n.contains("Roz"))      return "Roz";
        if (n.contains("Fungus"))   return "Fungus";
        if (n.contains("Waternoose"))return "Waternoose";
        if (n.contains("Yeti"))     return "Yeti";
        return n.split(" ")[0];
    }

    private String cellBgStyle(int index) {
        if (index == 0) {
            return "-fx-background-color: rgba(100, 80, 0, 0.55);"; // Start cell
        }
        if (index == 99) {
            return "-fx-background-color: rgba(180, 120, 10, 0.65);"; // End cell
        }
        if (contains(Constants.CARD_CELL_INDICES, index)) {
            return "-fx-background-color: rgba(140, 20, 20, 0.50);"; // Card cells
        }
        if (contains(Constants.CONVEYOR_CELL_INDICES, index)) {
            return "-fx-background-color: rgba(10, 100, 30, 0.50);"; // Conveyor cells
        }
        if (contains(Constants.SOCK_CELL_INDICES, index)) {
            return "-fx-background-color: rgba(160, 70, 0, 0.50);"; // Sock cells
        }
        if (contains(Constants.MONSTER_CELL_INDICES, index)) {
            return "-fx-background-color: rgba(20, 60, 140, 0.52);"; // Stationed monster cells
        }
        if (index % 2 == 1) {
            int doorGroupIndex = (index - 1) / 2;
            return doorGroupIndex % 2 == 0 
                ? "-fx-background-color: rgba(80, 20, 120, 0.48);" 
                : "-fx-background-color: rgba(20, 60, 120, 0.48);";
        }
        return "-fx-background-color: rgba(180, 150, 30, 0.42);"; // Normal Corridor
    }

    private String monsterImagePath(Monster monster) {
        String jpg = MONSTER_BASE + monster.getName() + ".jpg";
        if (getClass().getResource(jpg) != null) {
            return jpg;
        }
        return MONSTER_BASE + monster.getName() + ".png";
    }

    private Cell getCellByIndex(int index) {
        if (game == null) {
            return null;
        }
        Cell[][] cells = game.getBoard().getBoardCells();
        if (cells == null) {
            return null;
        }
        int[] rowCol = indexToRowCol(index);
        int row = rowCol[0];
        int col = rowCol[1];
        if (row < 0 || row >= cells.length || col < 0 || col >= cells[row].length) {
            return null;
        }
        return cells[row][col];
    }

    private void addImage(StackPane cell, String path, double fitHeight, Pos pos) {
        ImageView imageView = makeImageView(path, fitHeight);
        StackPane.setAlignment(imageView, pos);
        cell.getChildren().add(imageView);
    }

    private ImageView makeImageView(String path, double fitHeight) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setPickOnBounds(false);
        Image img = loadImage(path);
        if (img != null) {
            imageView.setImage(img);
        }
        return imageView;
    }

    private Image loadImage(String path) {
        Image cached = ResourceManager.getImage(path);
        if (cached != null) {
            return cached;
        }
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 0, 0, true, true, false);
                if (img.isError() && img.getException() != null) {
                    System.err.println("JavaFX Image load error for: " + path);
                    img.getException().printStackTrace();
                }
                ResourceManager.storeImage(path, img);
                return img;
            } else {
                System.err.println("Resource not found in classpath: " + path);
            }
        } catch (Exception e) {
            System.err.println("Exception loading image: " + path);
            e.printStackTrace();
        }
        return null;
    }

    private String formatMultiTaskerLossBonusMessage(String monsterName, int rawLoss, int netChange, String source) {
        String sourceStr = source != null && !source.isEmpty() ? " " + source : "";
        if (netChange >= 0) {
            return monsterName + " was supposed to lose " + rawLoss + " energy" + sourceStr 
                + ", but gained " + netChange + " instead due to its bonus";
        } else {
            return monsterName + " was supposed to lose " + rawLoss + " energy" + sourceStr 
                + ", but lost only " + Math.abs(netChange) + " instead due to its bonus";
        }
    }

    private boolean contains(int[] array, int value) {
        for (int x : array) {
            if (x == value) {
                return true;
            }
        }
        return false;
    }

    private void cleanup() {
        if (winDelayTransition != null) {
            try {
                winDelayTransition.stop();
            } catch (Exception ignored) {}
            winDelayTransition = null;
        }
        stopMoveSfx();
        if (ostPlayer != null) {
            try {
                ostPlayer.stop();
                ostPlayer.setOnEndOfMedia(null);
                ostPlayer.dispose();
            } catch (Exception ignored) {}
            ostPlayer = null;
        }
        for (MediaPlayer mp : activeMediaPlayers) {
            try {
                mp.setOnEndOfMedia(null);
                mp.setOnError(null);
                mp.stop();
                mp.dispose();
            } catch (Exception ignored) {}
        }
        activeMediaPlayers.clear();
        if (boardGrid != null && boardGrid.getScene() != null) {
            if (escFilter != null) {
                boardGrid.getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escFilter);
            }
            if (keyboardFilter != null) {
                boardGrid.getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyboardFilter);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTINGS PANEL & CHEATS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void onSettingsClicked() {
        if (animating) return;
        animating = true;
        disableAllButtons();
        showSettingsPopup();
    }

    private void showSettingsPopup() {
        showSettingsPopup("audio");
    }

    private void showSettingsPopup(String activeTab) {
        popupContent.getChildren().clear();
        popupContent.setMaxWidth(480);
        popupContent.setMaxHeight(Region.USE_PREF_SIZE);
        popupContent.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Game Settings");
        title.getStyleClass().add("popup-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // Tab navigation bar
        HBox tabHeader = new HBox(12);
        tabHeader.setAlignment(Pos.CENTER);
        tabHeader.getStyleClass().add("tab-header-box");

        Button audioTabBtn = new Button("Audio");
        audioTabBtn.getStyleClass().add("audio".equals(activeTab) ? "tab-btn-active" : "tab-btn");
        audioTabBtn.setOnAction(e -> showSettingsPopup("audio"));

        Button cheatsTabBtn = new Button("Cheats");
        cheatsTabBtn.getStyleClass().add("cheats".equals(activeTab) ? "tab-btn-active" : "tab-btn");
        cheatsTabBtn.setOnAction(e -> showSettingsPopup("cheats"));

        Button exitTabBtn = new Button("Leave Game");
        exitTabBtn.getStyleClass().add("exit".equals(activeTab) ? "tab-btn-active" : "tab-btn");
        exitTabBtn.setOnAction(e -> showSettingsPopup("exit"));

        tabHeader.getChildren().addAll(audioTabBtn, cheatsTabBtn, exitTabBtn);

        // Tab Content area
        VBox tabContentArea = new VBox(16);
        tabContentArea.setAlignment(Pos.CENTER);
        tabContentArea.setMinHeight(180);

        if ("audio".equals(activeTab)) {
            buildAudioTab(tabContentArea);
        } else if ("cheats".equals(activeTab)) {
            buildCheatsTab(tabContentArea);
        } else if ("exit".equals(activeTab)) {
            buildExitTab(tabContentArea);
        }

        // Close/Return to Game button
        Button closeBtn = new Button("Return to Game");
        closeBtn.getStyleClass().add("btn-settings-action");
        closeBtn.setOnAction(e -> {
            animatePopupOut(() -> {
                animating = false;
                settingsOpen = false;
                setTurnState(game.getCurrent() == game.getPlayer());
                checkPowerupAvailability();
            });
        });

        popupContent.getChildren().addAll(title, tabHeader, tabContentArea, closeBtn);
        
        boolean wasHidden = !popupOverlay.isVisible();
        popupOverlay.setVisible(true);
        settingsOpen = true;
        if (wasHidden) {
            animatePopupIn(() -> animating = false);
        } else {
            animating = false;
        }
    }

    private void buildAudioTab(VBox container) {
        // Music mode
        ResourceManager.MusicMode mode = ResourceManager.getMusicMode();
        int currentModeIndex;
        if (mode == ResourceManager.MusicMode.ORIGINAL) currentModeIndex = 0;
        else if (mode == ResourceManager.MusicMode.ALTERNATE) currentModeIndex = 1;
        else currentModeIndex = 2;

        Label modeDesc = new Label(getModeDescription(currentModeIndex));
        modeDesc.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        modeDesc.setPrefWidth(300);
        modeDesc.setAlignment(Pos.CENTER);

        // 3-way slider for music options
        StackPane sliderPane = buildSettingsSlider(currentModeIndex, modeDesc);

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

        container.getChildren().addAll(sliderPane, modeDesc, musicVolBox, sfxVolBox);
    }

    private String getModeDescription(int index) {
        if (index == 0) return "Now playing: Original Monsters Inc. OST";
        if (index == 1) return "Now playing: Custom alternate OST";
        return "Music is off";
    }

    private StackPane buildSettingsSlider(int startIdx, Label descText) {
        double trackW = 300, trackH = 36, thumbW = 100;
        
        Rectangle track = new Rectangle(trackW, trackH);
        track.setArcWidth(trackH); track.setArcHeight(trackH);
        track.setFill(Color.rgb(12, 18, 45));
        track.setStroke(Color.rgb(56, 199, 255, 0.45)); track.setStrokeWidth(1.2);

        Rectangle thumb = new Rectangle(thumbW - 4, trackH - 6);
        thumb.setArcWidth(trackH - 6); thumb.setArcHeight(trackH - 6);
        thumb.setFill(Color.rgb(29, 78, 216, 0.9));
        
        double zoneW = trackW / 3.0;
        thumb.setTranslateX(startIdx * zoneW + zoneW / 2.0 - trackW / 2.0);

        String[] labels = {"Original", "Alternate", "Off"};
        Label[] textNodes = new Label[3];
        HBox labelRow = new HBox(0);
        labelRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setFont(Font.font("Futura", 10));
            lbl.setTextFill(i == startIdx ? Color.WHITE : Color.rgb(140, 170, 210, 0.7));
            lbl.setPrefWidth(thumbW);
            lbl.setAlignment(Pos.CENTER);
            textNodes[i] = lbl;
            labelRow.getChildren().add(lbl);
        }

        StackPane pane = new StackPane(track, thumb, labelRow);
        pane.setMaxWidth(trackW);

        final int[] currentIdx = {startIdx};
        final boolean[] cooldown = {false};

        pane.setOnMouseClicked(ev -> {
            if (cooldown[0]) return;
            int zone = Math.max(0, Math.min(2, (int)(ev.getX() / (trackW / 3))));
            if (zone == currentIdx[0]) return;
            currentIdx[0] = zone;
            
            // Animate thumb
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), thumb);
            tt.setToX(zone * zoneW + zoneW / 2.0 - trackW / 2.0);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();

            // Update text styles
            for (int i = 0; i < 3; i++) {
                textNodes[i].setTextFill(i == zone ? Color.WHITE : Color.rgb(140, 170, 210, 0.7));
                textNodes[i].setFont(Font.font("Futura", i == zone ? 11 : 9));
            }

            descText.setText(getModeDescription(zone));

            // Apply Mode
            cooldown[0] = true;
            ResourceManager.MusicMode mode;
            if (zone == 0) mode = ResourceManager.MusicMode.ORIGINAL;
            else if (zone == 1) mode = ResourceManager.MusicMode.ALTERNATE;
            else mode = ResourceManager.MusicMode.OFF;
            ResourceManager.setMusicMode(mode);

            if (mode == ResourceManager.MusicMode.OFF) {
                ResourceManager.stopCurrentOST();
                if (ostPlayer != null) { ostPlayer.stop(); ostPlayer.dispose(); ostPlayer = null; }
                cooldown[0] = false;
            } else {
                MediaPlayer changeMusic = ResourceManager.getAudio(SFX_BASE + "ChangeMusic.mp3");
                if (changeMusic == null) {
                    changeMusic = ResourceManager.getAudio("/game/resources/audio/sound effects/ChangeMusic.mp3");
                }
                final MediaPlayer finalMusic = changeMusic;
                if (finalMusic != null) {
                    finalMusic.stop();
                    finalMusic.seek(finalMusic.getStartTime());
                    finalMusic.setCycleCount(1);
                    Runnable cleanUp = () -> Platform.runLater(() -> {
                        if (finalMusic != null) {
                            finalMusic.setOnEndOfMedia(null);
                            finalMusic.setOnError(null);
                        }
                        startBoardOST();
                        cooldown[0] = false;
                    });
                    finalMusic.setOnEndOfMedia(cleanUp);
                    finalMusic.setOnError(cleanUp);
                    finalMusic.play();

                    // Safety timeout fallback: 4s max wait
                    PauseTransition timeout = new PauseTransition(Duration.seconds(4));
                    timeout.setOnFinished(ex -> {
                        if (cooldown[0]) {
                            finalMusic.setOnEndOfMedia(null);
                            finalMusic.setOnError(null);
                            startBoardOST();
                            cooldown[0] = false;
                        }
                    });
                    timeout.play();
                } else {
                    startBoardOST();
                    cooldown[0] = false;
                }
            }
        });

        return pane;
    }

    private void buildCheatsTab(VBox container) {
        Label headerLabel = new Label("Enter Cheat Code");
        headerLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 13px; -fx-text-fill: white; -fx-font-weight: bold;");

        PasswordField pwField = new PasswordField();
        pwField.getStyleClass().add("settings-input");
        pwField.setMaxWidth(200);
        pwField.setPromptText("Cheat code...");

        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 11px; -fx-text-fill: #ff6060;");
        feedbackLabel.setPrefWidth(200);
        feedbackLabel.setAlignment(Pos.CENTER);

        Button applyBtn = new Button("Apply Code");
        applyBtn.getStyleClass().add("btn-settings-action");
        applyBtn.setOnAction(e -> {
            String input = pwField.getText();
            if ("hany".equalsIgnoreCase(input.trim())) {
                playSfx("Powerup.mp3");
                animatePopupOut(() -> {
                    cleanup();
                    Monster testWinner = game.getPlayer().getEnergy() >= game.getOpponent().getEnergy() 
                        ? game.getPlayer() 
                        : game.getOpponent();
                    game.setCurrent(testWinner);
                    EndScreenController.setGame(game);
                    Main.switchScene("/game/view/EndScreen.fxml");
                });
            } else {
                feedbackLabel.setText("Invalid Cheat Code!");
            }
        });

        container.getChildren().addAll(headerLabel, pwField, feedbackLabel, applyBtn);
    }

    private void buildExitTab(VBox container) {
        Label headerLabel = new Label("Select Leave Option");
        headerLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 13px; -fx-text-fill: white; -fx-font-weight: bold;");

        Button menuBtn = new Button("Exit to Main Menu");
        menuBtn.getStyleClass().add("btn-settings-action");
        menuBtn.setPrefWidth(200);
        menuBtn.setOnAction(e -> showExitConfirmation(container, "menu"));

        Button quitBtn = new Button("Exit Game");
        quitBtn.getStyleClass().add("btn-settings-exit");
        quitBtn.setPrefWidth(200);
        quitBtn.setOnAction(e -> showExitConfirmation(container, "quit"));

        container.getChildren().addAll(headerLabel, menuBtn, quitBtn);
    }

    private void showExitConfirmation(VBox container, String destination) {
        container.getChildren().clear();

        Label questionLabel = new Label();
        questionLabel.setStyle("-fx-font-family: 'Futura'; -fx-font-size: 13px; -fx-text-fill: #fbbf24; -fx-text-alignment: center; -fx-font-weight: bold;");
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(360);
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        if ("menu".equals(destination)) {
            questionLabel.setText("Exit to Main Menu?\nAll current match progress will be lost.");
        } else {
            questionLabel.setText("Are you sure you want to close the game?");
        }

        HBox buttonRow = new HBox(16);
        buttonRow.setAlignment(Pos.CENTER);

        Button yesBtn = new Button("Yes, Confirm");
        yesBtn.getStyleClass().add("btn-settings-exit");
        yesBtn.setOnAction(e -> {
            animatePopupOut(() -> {
                cleanup();
                if ("menu".equals(destination)) {
                    ResourceManager.stopCurrentOST();
                    ResourceManager.playOSTWithMode("StartMenu.mp3", 0.35);
                    Main.switchScene("/game/view/MainMenu.fxml");
                } else {
                    Platform.exit();
                    System.exit(0);
                }
            });
        });

        Button noBtn = new Button("No, Cancel");
        noBtn.getStyleClass().add("btn-settings-action");
        noBtn.setOnAction(e -> showSettingsPopup("exit"));

        buttonRow.getChildren().addAll(yesBtn, noBtn);
        container.getChildren().addAll(questionLabel, buttonRow);
    }
}

