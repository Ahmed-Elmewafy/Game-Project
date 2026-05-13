package game.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private ImageView ScarerDoorClosed;
    @FXML private ImageView LaugherClosedDoor;

    private static final String SCARER_CLOSED  = "/game/resources/images/Background/ScarersClosedDoor.png";
    private static final String SCARER_OPENED  = "/game/resources/images/Background/ScarersOpenedDoor.png";
    private static final String LAUGHER_CLOSED = "/game/resources/images/Background/LaughersClosedDoor.png";
    private static final String LAUGHER_OPENED = "/game/resources/images/Background/LaughersOpenedDoor.png";

    private static final String SFX_OPEN  = "/game/resources/audio/sound effects/OpenDoor.mp3";
    private static final String SFX_CLOSE = "/game/resources/audio/sound effects/CloseDoor.mp3";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_CLOSED));
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_CLOSED));
    }

    @FXML
    private void onScarerDoorEntered() {
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_OPENED));
        ResourceManager.playSoundEffect(SFX_OPEN);
    }

    @FXML
    private void onScarerDoorExited() {
        ScarerDoorClosed.setImage(ResourceManager.getImage(SCARER_CLOSED));
        ResourceManager.playSoundEffect(SFX_CLOSE);
    }

    @FXML
    private void onLaugherDoorEntered() {
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_OPENED));
        ResourceManager.playSoundEffect(SFX_OPEN);
    }

    @FXML
    private void onLaugherDoorExited() {
        LaugherClosedDoor.setImage(ResourceManager.getImage(LAUGHER_CLOSED));
        ResourceManager.playSoundEffect(SFX_CLOSE);
    }
}
