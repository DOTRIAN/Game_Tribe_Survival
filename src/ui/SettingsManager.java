package ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * SettingsManager:
 * - Luu/nap settings UI bang file properties don gian.
 * - Vi sao can: fullscreen, FPS, debug grid va volume can duoc giu lai giua cac lan mo game.
 */
public class SettingsManager {
    private static final Path SETTINGS_PATH = Path.of("data", "ui_settings.properties");

    public GameSettings load() {
        GameSettings settings = new GameSettings();
        if (!Files.exists(SETTINGS_PATH)) {
            return settings;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(SETTINGS_PATH)) {
            properties.load(inputStream);
            settings.setMusicVolume(parseDouble(properties.getProperty("musicVolume"), settings.getMusicVolume()));
            settings.setSfxVolume(parseDouble(properties.getProperty("sfxVolume"), settings.getSfxVolume()));
            settings.setFullscreen(Boolean.parseBoolean(properties.getProperty("fullscreen", "false")));
            settings.setShowFps(Boolean.parseBoolean(properties.getProperty("showFps", "false")));
            settings.setDebugGrid(Boolean.parseBoolean(properties.getProperty("debugGrid", "false")));
            settings.setMinimapVisible(Boolean.parseBoolean(properties.getProperty("minimapVisible", "true")));
        } catch (IOException exception) {
            System.out.println("[SettingsManager] Load failed: " + exception.getMessage());
        }
        return settings;
    }

    public void save(GameSettings settings) {
        if (settings == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("musicVolume", String.valueOf(settings.getMusicVolume()));
        properties.setProperty("sfxVolume", String.valueOf(settings.getSfxVolume()));
        properties.setProperty("fullscreen", String.valueOf(settings.isFullscreen()));
        properties.setProperty("showFps", String.valueOf(settings.isShowFps()));
        properties.setProperty("debugGrid", String.valueOf(settings.isDebugGrid()));
        properties.setProperty("minimapVisible", String.valueOf(settings.isMinimapVisible()));

        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(SETTINGS_PATH)) {
                properties.store(outputStream, "Tribe Survival UI Settings");
            }
        } catch (IOException exception) {
            System.out.println("[SettingsManager] Save failed: " + exception.getMessage());
        }
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
