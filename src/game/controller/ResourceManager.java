package game.controller;

import javafx.scene.image.Image;
import javafx.scene.media.MediaPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {

    /** Three-way music mode settable from MainMenu. */
    public enum MusicMode { ORIGINAL, ALTERNATE, OFF }

    private static MusicMode musicMode = MusicMode.ORIGINAL;
    private static double musicVolume = 0.15;
    private static double sfxVolume = 0.65;

    private static final Map<String, MediaPlayer> audioPlayers = new ConcurrentHashMap<>();
    private static final Map<String, Image> images = new ConcurrentHashMap<>();
    private static MediaPlayer currentOST = null;

    private ResourceManager() {}

    // ── Music mode & volume ───────────────────────────────────────────────
    public static MusicMode getMusicMode() { return musicMode; }
    public static void setMusicMode(MusicMode mode) { musicMode = mode; }
    
    public static double getMusicVolume() { return musicVolume; }
    public static void setMusicVolume(double vol) { 
        musicVolume = vol; 
        if (currentOST != null) currentOST.setVolume(vol);
    }
    
    public static double getSfxVolume() { return sfxVolume; }
    public static void setSfxVolume(double vol) { sfxVolume = vol; }

    // ── Audio ─────────────────────────────────────────────────────────────
    public static void storeAudio(String key, MediaPlayer player) {
        audioPlayers.put(key, player);
    }

    public static MediaPlayer getAudio(String key) {
        return audioPlayers.get(key);
    }

    public static void playOST(String key, double volumeMultiplier) {
        if (musicMode == MusicMode.OFF) return;
        MediaPlayer player = audioPlayers.get(key);
        if (player != null) {
            stopCurrentOST();
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(musicVolume * volumeMultiplier);
            player.seek(player.getStartTime());
            player.play();
            currentOST = player;
        }
    }

    /**
     * Plays an OST from the currently selected folder (original or alternate).
     * Falls back to original if alternate key not found.
     */
    public static void playOSTWithMode(String filename, double volumeMultiplier) {
        if (musicMode == MusicMode.OFF) { stopCurrentOST(); return; }
        String folder = musicMode == MusicMode.ALTERNATE
            ? "/game/resources/audio/alternate ost/"
            : "/game/resources/audio/ost/";
        String key = folder + filename;
        MediaPlayer player = audioPlayers.get(key);
        if (player == null) {
            // fallback to original
            key = "/game/resources/audio/ost/" + filename;
            player = audioPlayers.get(key);
        }
        if (player != null) {
            stopCurrentOST();
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(musicVolume * volumeMultiplier);
            player.seek(player.getStartTime());
            player.play();
            currentOST = player;
        }
    }

    public static void playSoundEffect(String key) {
        MediaPlayer player = audioPlayers.get(key);
        if (player != null) {
            player.setVolume(sfxVolume);
            player.stop();
            player.seek(player.getStartTime());
            player.setCycleCount(1);
            player.play();
        }
    }

    public static MediaPlayer getCurrentOST() { return currentOST; }

    /** Registers an externally-created MediaPlayer as the current OST.
     *  Stops any previously tracked OST first. */
    public static void setCurrentOSTPlayer(MediaPlayer player) {
        if (currentOST != null && currentOST != player) {
            currentOST.stop();
        }
        currentOST = player;
    }

    public static void stopCurrentOST() {
        if (currentOST != null) {
            currentOST.stop();
            currentOST = null;
        }
    }

    // ── Images ────────────────────────────────────────────────────────────
    public static void storeImage(String key, Image image) {
        images.put(key, image);
    }

    public static Image getImage(String key) {
        return images.get(key);
    }
}
