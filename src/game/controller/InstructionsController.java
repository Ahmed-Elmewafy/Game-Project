package game.controller;

import game.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * InstructionsController — handles the Instructions screen.
 *
 * Design Pattern: MVC Controller (minimal — this is a static view screen).
 * The only action is navigating back to the Main Menu.
 */
public class InstructionsController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Register ESC key to go back to the main menu
        Platform.runLater(() -> {
            if (Main.getCurrentContent() != null && Main.getCurrentContent().getScene() != null) {
                Main.getCurrentContent().getScene().setOnKeyPressed(ev -> {
                    if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        ev.consume();
                        onBackToMenu();
                    }
                });
            }
        });
    }

    /**
     * Navigates back to the Main Menu when the Back button is clicked.
     */
    @FXML
    private void onBackToMenu() {
        if (Main.getCurrentContent() != null && Main.getCurrentContent().getScene() != null) {
            Main.getCurrentContent().getScene().setOnKeyPressed(null);
        }
        ResourceManager.playOSTWithMode("StartMenu.mp3", 0.15);
        Main.switchScene("/game/view/MainMenu.fxml");
    }
}
