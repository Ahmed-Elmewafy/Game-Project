package game.controller;

import game.Main;
import game.engine.Game;
import game.engine.Role;
import game.engine.monsters.*;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * EndScreenController — displays winner, final energies, and navigation.
 *
 * Data is passed via static setters before the scene is loaded,
 * matching the pattern used by MonsterSelectionController and BoardController.
 */
public class EndScreenController implements Initializable {

    // ── FXML nodes ────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private ImageView winnerImage;
    @FXML private Text winnerName;
    @FXML private Text winnerType;
    @FXML private Text winnerRole;
    @FXML private Text winnerEnergy;

    @FXML private ImageView playerImage;
    @FXML private Text playerName;
    @FXML private Text playerEnergyLabel;
    @FXML private Text playerRoleLabel;

    @FXML private ImageView opponentImage;
    @FXML private Text opponentName;
    @FXML private Text opponentEnergyLabel;
    @FXML private Text opponentRoleLabel;

    // ── Static data (set by BoardController before switching scene) ───────
    private static Game gameData;

    public static void setGame(Game game) { gameData = game; }

    // ── Resource paths ────────────────────────────────────────────────────
    private static final String MONSTER_BASE = "/game/resources/images/Monsters/";
    private static final String OST_BASE     = "/game/resources/audio/ost/";

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (gameData == null) return;

        Monster winner   = gameData.getWinner();
        Monster player   = gameData.getPlayer();
        Monster opponent = gameData.getOpponent();
        if (winner == null) {
            winner = player.getEnergy() >= opponent.getEnergy() ? player : opponent;
        }

        populateWinner(winner);
        populateScore(player,   playerImage,   playerName,   playerEnergyLabel,   playerRoleLabel);
        populateScore(opponent, opponentImage, opponentName, opponentEnergyLabel, opponentRoleLabel);

        // Highlight winner's score card
        highlightWinnerCard(winner, player, opponent);

        // Play EndCredits OST
        ResourceManager.stopCurrentOST();
        ResourceManager.playOSTWithMode("EndCredits.mp3", 0.25);

        // Entrance animation — fade in the whole screen
        Platform.runLater(this::playEntranceAnimation);
    }

    // ── Population ────────────────────────────────────────────────────────

    private void populateWinner(Monster winner) {
        Image monsterImage = loadImage(monsterImagePath(winner));
        if (monsterImage != null) winnerImage.setImage(monsterImage);

        winnerName.setText(winner.getName());

        winnerType.setText(monsterTypeName(winner));

        String role = winner.getOriginalRole().toString();
        winnerRole.setText(role);
        winnerRole.setStyle(role.equals("SCARER")
            ? "-fx-fill: #ff6060;" : "-fx-fill: #38c7ff;");

        winnerEnergy.setText(winner.getEnergy() + " Energy");
    }

    private void populateScore(Monster monster, ImageView imageView, Text name,
                               Text energyLabel, Text roleLabel) {
        Image monsterImage = loadImage(monsterImagePath(monster));
        if (monsterImage != null) imageView.setImage(monsterImage);
        name.setText(monster.getName());
        energyLabel.setText(String.valueOf(monster.getEnergy()));
        String role = monster.getOriginalRole().toString();
        roleLabel.setText(monsterTypeName(monster) + " · " + role);
        roleLabel.setStyle(role.equals("SCARER")
            ? "-fx-fill: #ff6060;" : "-fx-fill: #38c7ff;");
    }

    private void highlightWinnerCard(Monster winner, Monster player, Monster opponent) {
        // The winner's score card gets a gold border tint
        boolean playerWon = winner == player;
        if (playerImage.getParent() instanceof javafx.scene.layout.VBox) {
            javafx.scene.layout.VBox card = (javafx.scene.layout.VBox) playerImage.getParent();
            if (playerWon) {
                String s = card.getStyle();
                if (s == null) s = "";
                if (!s.isEmpty() && !s.endsWith(";")) s += ";";
                card.setStyle(s + " -fx-border-color: #f5c518; -fx-border-width: 2;");
            }
        }
        if (opponentImage.getParent() instanceof javafx.scene.layout.VBox) {
            javafx.scene.layout.VBox card = (javafx.scene.layout.VBox) opponentImage.getParent();
            if (!playerWon) {
                String s = card.getStyle();
                if (s == null) s = "";
                if (!s.isEmpty() && !s.endsWith(";")) s += ";";
                card.setStyle(s + " -fx-border-color: #f5c518; -fx-border-width: 2;");
            }
        }
    }

    // ── Entrance animation ────────────────────────────────────────────────

    private void playEntranceAnimation() {
        if (rootPane == null) return;
        rootPane.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(1200), rootPane);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setDelay(Duration.millis(300));
        fade.play();
    }

    // ── Button handlers ────────────────────────────────────────────────────

    @FXML private void onMainMenu() {
        ResourceManager.stopCurrentOST();
        ResourceManager.playOSTWithMode("StartMenu.mp3", 0.35);
        Main.switchScene("/game/view/MainMenu.fxml");
    }

    @FXML private void onExit() {
        ResourceManager.stopCurrentOST();
        javafx.application.Platform.exit();
        System.exit(0);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String monsterImagePath(Monster monster) {
        String jpg = MONSTER_BASE + monster.getName() + ".jpg";
        if (getClass().getResource(jpg) != null) return jpg;
        return MONSTER_BASE + monster.getName() + ".png";
    }

    private String monsterTypeName(Monster monster) {
        if (monster instanceof Dasher)      return "Dasher";
        if (monster instanceof Dynamo)      return "Dynamo";
        if (monster instanceof MultiTasker) return "MultiTasker";
        if (monster instanceof Schemer)     return "Schemer";
        return "Unknown";
    }

    private Image loadImage(String path) {
        Image cached = ResourceManager.getImage(path);
        if (cached != null) return cached;
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 0, 0, true, true, false);
                ResourceManager.storeImage(path, img);
                return img;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
