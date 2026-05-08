package game.controller;

import javafx.scene.image.Image;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {

    private static final Map<String, MediaPlayer> audioPlayers = new HashMap<>();
    private static final Map<String, Image> images = new HashMap<>();
    private static MediaPlayer currentOST = null;

    private ResourceManager() {}

    public static void storeAudio(String key, MediaPlayer player) {
        audioPlayers.put(key, player);
    }

    public static MediaPlayer getAudio(String key) {
        return audioPlayers.get(key);
    }

    public static void storeImage(String key, Image image) {
        images.put(key, image);
    }

    public static Image getImage(String key) {
        return images.get(key);
    }

    public static void playOST(String key, double volume) {
        MediaPlayer player = audioPlayers.get(key);
        if (player != null) {
            stopCurrentOST();
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(volume);
            player.seek(player.getStartTime());
            player.play();
            currentOST = player;
        }
    }

    public static void playSoundEffect(String key) {
        MediaPlayer player = audioPlayers.get(key);
        if (player != null) {
            player.stop();
            player.seek(player.getStartTime());
            player.setCycleCount(1);
            player.play();
        }
    }

    public static MediaPlayer getCurrentOST() {
        return currentOST;
    }

    public static void stopCurrentOST() {
        if (currentOST != null) {
            currentOST.stop();
            currentOST = null;
        }
    }
}
