package ui;

/**
 * GameSettings:
 * - Luu state UI/runtime co the thay doi tu man hinh Settings.
 * - Tach rieng class nay de Renderer va Game doc/ghi cung mot nguon cau hinh.
 */
public class GameSettings {
    private double musicVolume;
    private double sfxVolume;
    private boolean fullscreen;
    private boolean showFps;
    private boolean debugGrid;
    private boolean minimapVisible;

    public GameSettings() {
        // Gia tri mac dinh:
        // - Du vua de vao game khong bi tat minimap/khong bat debug ngoai y muon.
        this.musicVolume = 0.70;
        this.sfxVolume = 0.80;
        this.fullscreen = false;
        this.showFps = false;
        this.debugGrid = false;
        this.minimapVisible = true;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(double musicVolume) {
        this.musicVolume = clamp01(musicVolume);
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = clamp01(sfxVolume);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public boolean isDebugGrid() {
        return debugGrid;
    }

    public void setDebugGrid(boolean debugGrid) {
        this.debugGrid = debugGrid;
    }

    public boolean isMinimapVisible() {
        return minimapVisible;
    }

    public void setMinimapVisible(boolean minimapVisible) {
        this.minimapVisible = minimapVisible;
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
