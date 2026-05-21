package ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import system.level.SimpleJson;

/**
 * SettingsManager:
 * - Luu/nap settings UI bang file JSON don gian.
 * - Vi sao can: fullscreen, FPS, debug grid va volume can duoc giu lai giua cac lan mo game.
 */
public class SettingsManager {
    private static final Path SETTINGS_PATH = Path.of("data", "ui_settings.json");
    private static final Path LEGACY_PROPERTIES_PATH = Path.of("data", "ui_settings.properties");

    public GameSettings load() {
        GameSettings settings = new GameSettings();
        migrateLegacyPropertiesIfPresent(settings);

        if (!Files.exists(SETTINGS_PATH)) {
            return settings;
        }

        try {
            String json = Files.readString(SETTINGS_PATH, StandardCharsets.UTF_8);
            json = stripBom(json);
            if (json == null || json.isBlank()) {
                save(settings);
                return settings;
            }
            Object parsed = SimpleJson.parse(json);
            if (!(parsed instanceof Map<?, ?> map)) {
                save(settings);
                return settings;
            }

            settings.setMusicVolume(parseDouble(map.get("musicVolume"), settings.getMusicVolume()));
            settings.setSfxVolume(parseDouble(map.get("sfxVolume"), settings.getSfxVolume()));
            settings.setFullscreen(parseBoolean(map.get("fullscreen"), settings.isFullscreen()));
            settings.setShowFps(parseBoolean(map.get("showFps"), settings.isShowFps()));
            settings.setDebugGrid(parseBoolean(map.get("debugGrid"), settings.isDebugGrid()));
            settings.setMinimapVisible(parseBoolean(map.get("minimapVisible"), settings.isMinimapVisible()));
        } catch (Exception exception) {
            System.out.println("[SettingsManager] Load failed: " + exception.getMessage());
            save(settings);
        }
        return settings;
    }

    public void save(GameSettings settings) {
        if (settings == null) {
            return;
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("musicVolume", settings.getMusicVolume());
        snapshot.put("sfxVolume", settings.getSfxVolume());
        snapshot.put("fullscreen", settings.isFullscreen());
        snapshot.put("showFps", settings.isShowFps());
        snapshot.put("debugGrid", settings.isDebugGrid());
        snapshot.put("minimapVisible", settings.isMinimapVisible());

        try {
            Files.createDirectories(SETTINGS_PATH.getParent());
            Files.writeString(SETTINGS_PATH, SimpleJson.stringify(snapshot), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.out.println("[SettingsManager] Save failed: " + exception.getMessage());
        }
    }

    private void migrateLegacyPropertiesIfPresent(GameSettings defaults) {
        if (Files.exists(SETTINGS_PATH) || !Files.exists(LEGACY_PROPERTIES_PATH)) {
            return;
        }
        Properties legacy = new Properties();
        try (var inputStream = Files.newInputStream(LEGACY_PROPERTIES_PATH)) {
            legacy.load(inputStream);
            defaults.setMusicVolume(parseDouble(legacy.getProperty("musicVolume"), defaults.getMusicVolume()));
            defaults.setSfxVolume(parseDouble(legacy.getProperty("sfxVolume"), defaults.getSfxVolume()));
            defaults.setFullscreen(Boolean.parseBoolean(legacy.getProperty("fullscreen", String.valueOf(defaults.isFullscreen()))));
            defaults.setShowFps(Boolean.parseBoolean(legacy.getProperty("showFps", String.valueOf(defaults.isShowFps()))));
            defaults.setDebugGrid(Boolean.parseBoolean(legacy.getProperty("debugGrid", String.valueOf(defaults.isDebugGrid()))));
            defaults.setMinimapVisible(Boolean.parseBoolean(legacy.getProperty("minimapVisible", String.valueOf(defaults.isMinimapVisible()))));
            save(defaults);
        } catch (IOException exception) {
            System.out.println("[SettingsManager] Legacy migrate failed: " + exception.getMessage());
        }
    }

    private double parseDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        String raw = value.toString();
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String stripBom(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        if (raw.charAt(0) == '\uFEFF') {
            return raw.substring(1);
        }
        return raw;
    }
}
