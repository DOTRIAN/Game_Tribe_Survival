package ui;

import build.AssetManager;
import build.BuildManager;
import build.Wall;
import build.WallPreview;
import core.GameState;
import entity.Enemy;
import entity.Player;
import input.InputHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import map.MapData;
import map.MapRenderer;
import system.level.Level;
import system.level.LevelResult;
import system.level.PlayerProgress;
import system.resource.ResourceNode;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Renderer:
 * - Render world len Canvas, UI len JavaFX overlay nodes.
 * - Vi sao can: de fullscreen/resize fill kin cua so, trong khi UI van dung CSS va layout responsive.
 */
public class Renderer {
    private static final double CAMERA_ZOOM = 1.5;
    private static final boolean DEBUG_DRAW_WALL_BOUNDS = false;
    private static final boolean DEBUG_DRAW_WALL_INFO = true;

    private final Stage stage;
    private final Canvas canvas;
    private final GraphicsContext graphicsContext;
    private final UIManager uiManager;
    private final SettingsManager settingsManager;
    private final GameSettings settings;
    private final Image welcomeBackgroundImage;
    private final Image gameBackgroundImage;
    private final LabelFpsTracker fpsTracker;
    private MapRenderer mapRenderer;

    public Renderer(Stage stage, InputHandler inputHandler, AssetManager buildAssetManager) {
        this.stage = stage;
        this.canvas = new Canvas();
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.welcomeBackgroundImage = new Image("file:assets/backgrounds/menu_bg1.png");
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");
        this.fpsTracker = new LabelFpsTracker();

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f1114;");

        AnchorPane overlayLayer = new AnchorPane();
        overlayLayer.setPickOnBounds(false);
        root.getChildren().addAll(canvas, overlayLayer);

        Scene scene = new Scene(root, 1280, 720);
        inputHandler.attach(scene);

        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        this.uiManager = new UIManager(stage, scene, overlayLayer, buildAssetManager, settings);

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(540);
        stage.setMaximized(true);
        stage.show();
        canvas.setFocusTraversable(true);
        canvas.requestFocus();
        stage.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                canvas.requestFocus();
            }
        });
    }

    public void setMapData(MapData mapData) {
        this.mapRenderer = (mapData == null) ? null : new MapRenderer(mapData);
    }

    public double getViewportWidth() {
        return Math.max(1.0, canvas.getWidth());
    }

    public double getViewportHeight() {
        return Math.max(1.0, canvas.getHeight());
    }

    public void render(GameState gameState, Player player, List<Enemy> enemies, long now,
                       double cameraX, double cameraY, int menuIndex, boolean welcomeFlashing,
                       String playerNameDraft, int maxNameLength,
                       List<Level> levels, PlayerProgress playerProgress, int selectedLevelIndex,
                       Level currentLevel, String objectiveStatus, LevelResult lastLevelResult,
                       List<ResourceNode> allResources, Map<String, Integer> collectedResources,
                       int selectedHotbarIndex, int stoneWallCount,
                       BuildManager buildManager,
                       List<FloatingDamageText> floatingDamageTexts,
                       double darknessAlpha, boolean isNight, String dayNightPhase,
                       double worldWidth, double worldHeight) {
        double viewportWidth = getViewportWidth();
        double viewportHeight = getViewportHeight();

        uiManager.applyGameState(gameState);
        if (gameState == GameState.NAME_INPUT) {
            uiManager.setNameDraft(playerNameDraft, maxNameLength);
        }
        uiManager.updateHud(
                player,
                collectedResources,
                selectedHotbarIndex,
                worldWidth,
                worldHeight,
                cameraX,
                cameraY,
                CAMERA_ZOOM,
                viewportWidth,
                viewportHeight,
                enemies
        );

        graphicsContext.setFill(Color.web("#121416"));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);

        if (gameState == GameState.WELCOME || gameState == GameState.NAME_INPUT || gameState == GameState.GUIDE || uiManager.isSettingsVisible()) {
            drawBackgroundCover(welcomeBackgroundImage, viewportWidth, viewportHeight);
            graphicsContext.setFill(Color.color(0, 0, 0, welcomeFlashing ? 0.42 : 0.28));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            return;
        }

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_COMPLETE) {
            renderGameplay(player, enemies, now, cameraX, cameraY, currentLevel, objectiveStatus,
                    allResources, collectedResources, buildManager, floatingDamageTexts,
                    darknessAlpha, isNight, dayNightPhase, worldWidth, worldHeight, viewportWidth, viewportHeight);
            return;
        }

        drawBackgroundCover(gameBackgroundImage, viewportWidth, viewportHeight);
    }

    public void setContinueAvailable(boolean enabled) {
        uiManager.setContinueEnabled(enabled);
    }

    public void setMenuActions(Runnable onPlay, Runnable onContinue, Runnable onGuide, Runnable onSettings, Runnable onExit) {
        uiManager.configureMenuActions(onPlay, onContinue, onGuide, onSettings, onExit);
    }

    public void setNameActions(Runnable onConfirm, Runnable onBack) {
        uiManager.configureNameActions(onConfirm, onBack);
    }

    public void setSettingsBackAction(Runnable onBack) {
        uiManager.configureSettingsAction(onBack);
    }

    public void setHotbarSelectionListener(IntConsumer listener) {
        uiManager.configureHotbarAction(listener);
    }

    public void setShopBuyListener(Consumer<String> listener) {
        uiManager.configureShopAction(listener);
    }

    public void setInventoryCloseAction(Runnable listener) {
        uiManager.configureInventoryClose(listener);
    }

    public String getEnteredName() {
        return uiManager.getEnteredName();
    }

    public void updateNameLength(int currentLength, int maxLength) {
        uiManager.updateNameLength(currentLength, maxLength);
    }

    public void showNameError(String message) {
        uiManager.showNameError(message);
    }

    public int findHotbarSlotAt(double mouseX, double mouseY) {
        return uiManager.findHotbarSlotAt(mouseX, mouseY);
    }

    public boolean isMouseOverUi(double mouseX, double mouseY) {
        return uiManager.isMouseOverUi(mouseX, mouseY);
    }

    public boolean closeTopOverlay() {
        return uiManager.closeTopOverlay();
    }

    public boolean isBlockingOverlayVisible() {
        return uiManager.isBlockingOverlayVisible();
    }

    public void setShopVisible(boolean visible) {
        uiManager.setShopVisible(visible);
    }

    public void toggleShop() {
        uiManager.setShopVisible(!uiManager.isShopVisible());
    }

    public void setInventoryVisible(boolean visible) {
        uiManager.setInventoryVisible(visible);
    }

    public void toggleInventory() {
        uiManager.setInventoryVisible(!uiManager.isInventoryVisible());
    }

    public void toggleMinimap() {
        uiManager.toggleMinimap();
        settingsManager.save(uiManager.getSettings());
    }

    public void setSettingsVisible(boolean visible) {
        if (visible) {
            GameSettings current = uiManager.getSettings();
            current.setFullscreen(stage.isFullScreen());
            uiManager.getSettingsScreen().applySettings(current);
        }
        uiManager.setSettingsVisible(visible);
    }

    public boolean isSettingsVisible() {
        return uiManager.isSettingsVisible();
    }

    public void toggleFullscreen() {
        stage.setFullScreen(!stage.isFullScreen());
        GameSettings current = uiManager.getSettings();
        current.setFullscreen(stage.isFullScreen());
        settingsManager.save(current);
    }

    public void saveSettings() {
        GameSettings current = uiManager.getSettings();
        if (stage.isFullScreen() != current.isFullscreen()) {
            stage.setFullScreen(current.isFullscreen());
        }
        settingsManager.save(current);
    }

    public GameSettings getSettings() {
        GameSettings current = uiManager.getSettings();
        current.setFullscreen(stage.isFullScreen());
        return current;
    }

    public void showToast(String message) {
        uiManager.showToast(message);
    }

    public void hideToast() {
        uiManager.hideToast();
    }

    private void renderGameplay(Player player,
                                List<Enemy> enemies,
                                long now,
                                double cameraX,
                                double cameraY,
                                Level currentLevel,
                                String objectiveStatus,
                                List<ResourceNode> allResources,
                                Map<String, Integer> collectedResources,
                                BuildManager buildManager,
                                List<FloatingDamageText> floatingDamageTexts,
                                double darknessAlpha,
                                boolean isNight,
                                String dayNightPhase,
                                double worldWidth,
                                double worldHeight,
                                double viewportWidth,
                                double viewportHeight) {
        graphicsContext.save();
        graphicsContext.scale(CAMERA_ZOOM, CAMERA_ZOOM);

        if (mapRenderer != null) {
            mapRenderer.setResources(allResources);
            mapRenderer.renderBelowEntities(graphicsContext, cameraX, cameraY, now);
        } else {
            drawBackgroundCover(gameBackgroundImage, viewportWidth / CAMERA_ZOOM, viewportHeight / CAMERA_ZOOM);
        }

        renderWallPreview(buildManager, cameraX, cameraY);
        renderPlacedWalls(buildManager, cameraX, cameraY);

        player.draw(graphicsContext, cameraX, cameraY);
        renderLevelUpEffect(player, cameraX, cameraY, now);
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    enemy.draw(graphicsContext, cameraX, cameraY, now);
                }
            }
        }

        if (mapRenderer != null) {
            mapRenderer.renderAboveEntities(graphicsContext, cameraX, cameraY, now);
        }

        renderFloatingDamageTexts(floatingDamageTexts, cameraX, cameraY, now);
        if (settings.isDebugGrid()) {
            drawDebugGrid(cameraX, cameraY, viewportWidth / CAMERA_ZOOM, viewportHeight / CAMERA_ZOOM);
        }
        graphicsContext.restore();

        renderNightOverlayAndLights(player, cameraX, cameraY, darknessAlpha, isNight);

        graphicsContext.setFill(Color.color(1, 1, 1, 0.82));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText("Light: " + dayNightPhase + " alpha=" + String.format("%.2f", darknessAlpha), 16, viewportHeight - 18);

        if (currentLevel != null && objectiveStatus != null && !objectiveStatus.isBlank()) {
            graphicsContext.setFill(Color.color(0, 0, 0, 0.30));
            graphicsContext.fillRoundRect(20, 134, 360, 40, 12, 12);
            graphicsContext.setFill(Color.web("#f4efe1"));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
            graphicsContext.fillText("L" + currentLevel.getId() + " " + currentLevel.getName(), 30, 150);
            graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
            graphicsContext.fillText(objectiveStatus.length() > 58 ? objectiveStatus.substring(0, 58) + "..." : objectiveStatus, 30, 166);
        }

        if (settings.isShowFps()) {
            fpsTracker.update(now);
            graphicsContext.setFill(Color.color(1, 1, 1, 0.90));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            graphicsContext.fillText("FPS " + fpsTracker.getLastFps(), viewportWidth - 92, viewportHeight - 18);
        }
    }

    private void renderPlacedWalls(BuildManager buildManager, double cameraX, double cameraY) {
        if (buildManager == null) {
            return;
        }
        for (Wall wall : buildManager.getWalls()) {
            if (wall == null) {
                continue;
            }
            double screenX = wall.getRenderX() - cameraX;
            double screenY = wall.getRenderY() - cameraY;
            if (wall.getImage() != null && !wall.getImage().isError()) {
                drawRotatedImage(wall.getImage(), screenX, screenY, wall.getWidth(), wall.getHeight(), wall.getRotationDegrees());
                if (DEBUG_DRAW_WALL_BOUNDS) {
                    graphicsContext.setStroke(Color.color(0.0, 1.0, 1.0, 0.75));
                    graphicsContext.strokeRect(screenX, screenY, wall.getWidth(), wall.getHeight());
                }
                continue;
            }
            graphicsContext.setFill(Color.color(0.75, 0.75, 0.75, 0.85));
            graphicsContext.fillRect(screenX, screenY, wall.getWidth(), wall.getHeight());
        }
    }

    private void renderWallPreview(BuildManager buildManager, double cameraX, double cameraY) {
        if (buildManager == null || !buildManager.isPreviewVisible()) {
            return;
        }
        WallPreview preview = buildManager.getWallPreview();
        if (preview == null || !preview.isVisible()) {
            return;
        }

        double screenX = preview.getRenderX() - cameraX;
        double screenY = preview.getRenderY() - cameraY;

        graphicsContext.save();
        graphicsContext.setGlobalAlpha(preview.getOpacity());
        if (preview.getImage() != null && !preview.getImage().isError()) {
            drawRotatedImage(preview.getImage(), screenX, screenY, preview.getWidth(), preview.getHeight(), preview.getRotationDegrees());
        } else {
            graphicsContext.setFill(Color.color(0.85, 0.85, 0.85, 0.60));
            graphicsContext.fillRect(screenX, screenY, preview.getWidth(), preview.getHeight());
        }
        graphicsContext.restore();

        if (!preview.isValid()) {
            graphicsContext.setFill(Color.color(1.0, 0.15, 0.15, 0.18));
            graphicsContext.fillRect(screenX, screenY, preview.getWidth(), preview.getHeight());
            graphicsContext.setStroke(Color.color(1.0, 0.12, 0.12, 0.92));
            graphicsContext.strokeRect(screenX, screenY, preview.getWidth(), preview.getHeight());
        }

        if (DEBUG_DRAW_WALL_INFO) {
            graphicsContext.setFill(Color.color(1, 1, 1, 0.86));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            graphicsContext.fillText("tileX=" + preview.getTileX() + " tileY=" + preview.getTileY(), 16, getViewportHeight() - 102);
            graphicsContext.fillText("rotation=" + preview.getRotationLabel() + " (" + (int) preview.getRotationDegrees() + ")", 16, getViewportHeight() - 86);
            graphicsContext.fillText("spriteKey=" + preview.getSpriteKey(), 16, getViewportHeight() - 70);
            graphicsContext.fillText("neighborMask=" + preview.getNeighborMask(), 16, getViewportHeight() - 54);
        }
    }

    private void drawRotatedImage(Image image, double x, double y, double width, double height, double rotationDegrees) {
        if (image == null) {
            return;
        }
        graphicsContext.save();
        graphicsContext.translate(x + width / 2.0, y + height / 2.0);
        graphicsContext.rotate(rotationDegrees);
        graphicsContext.drawImage(image, -width / 2.0, -height / 2.0, width, height);
        graphicsContext.restore();
    }

    private void drawBackgroundCover(Image image, double viewportWidth, double viewportHeight) {
        if (image == null || image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            graphicsContext.setFill(Color.web("#1a1f24"));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            return;
        }

        double imageRatio = image.getWidth() / image.getHeight();
        double viewportRatio = viewportWidth / viewportHeight;
        double drawWidth;
        double drawHeight;
        if (viewportRatio > imageRatio) {
            drawWidth = viewportWidth;
            drawHeight = viewportWidth / imageRatio;
        } else {
            drawHeight = viewportHeight;
            drawWidth = viewportHeight * imageRatio;
        }

        double drawX = (viewportWidth - drawWidth) / 2.0;
        double drawY = (viewportHeight - drawHeight) / 2.0;
        graphicsContext.drawImage(image, drawX, drawY, drawWidth, drawHeight);
    }

    private void drawDebugGrid(double cameraX, double cameraY, double viewWidth, double viewHeight) {
        double tileSize = 16.0;
        double startX = Math.floor(cameraX / tileSize) * tileSize;
        double startY = Math.floor(cameraY / tileSize) * tileSize;
        graphicsContext.setStroke(Color.color(1, 1, 1, 0.08));
        graphicsContext.setLineWidth(0.6);
        for (double x = startX; x <= cameraX + viewWidth; x += tileSize) {
            graphicsContext.strokeLine(x - cameraX, 0, x - cameraX, viewHeight);
        }
        for (double y = startY; y <= cameraY + viewHeight; y += tileSize) {
            graphicsContext.strokeLine(0, y - cameraY, viewWidth, y - cameraY);
        }
    }

    private void renderNightOverlayAndLights(Player player, double cameraX, double cameraY, double darknessAlpha, boolean isNight) {
        if (darknessAlpha <= 0.001) {
            return;
        }
        double viewportWidth = getViewportWidth();
        double viewportHeight = getViewportHeight();

        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphicsContext.setFill(Color.color(0, 0, 0, darknessAlpha));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);

        graphicsContext.save();
        graphicsContext.setGlobalBlendMode(BlendMode.SCREEN);
        double playerWorldX = player.getX() + player.getWidth() / 2.0;
        double playerWorldY = player.getY() + player.getHeight() / 2.0;
        double playerScreenX = (playerWorldX - cameraX) * CAMERA_ZOOM;
        double playerScreenY = (playerWorldY - cameraY) * CAMERA_ZOOM;
        double playerLightRadius = isNight ? 120 : 170;
        drawRadialLight(playerScreenX, playerScreenY, playerLightRadius, Color.color(1.0, 0.96, 0.86, 0.58));

        double bonfireWorldX = 1060;
        double bonfireWorldY = 520;
        drawRadialLight((bonfireWorldX - cameraX) * CAMERA_ZOOM, (bonfireWorldY - cameraY) * CAMERA_ZOOM, 180, Color.color(1.0, 0.80, 0.40, 0.62));
        graphicsContext.restore();
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    private void drawRadialLight(double x, double y, double radius, Color centerColor) {
        RadialGradient gradient = new RadialGradient(
                0,
                0,
                x,
                y,
                radius,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, centerColor),
                new Stop(0.55, Color.color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getOpacity() * 0.36)),
                new Stop(1.0, Color.color(0, 0, 0, 0.0))
        );
        graphicsContext.setFill(gradient);
        graphicsContext.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private void renderLevelUpEffect(Player player, double cameraX, double cameraY, long now) {
        if (player == null || !player.isLevelUpEffectActive(now)) {
            return;
        }
        double progress = player.getLevelUpEffectProgress(now);
        double screenX = player.getX() - cameraX + player.getWidth() / 2.0;
        double screenY = player.getY() - cameraY - 12 - progress * 16;
        double alpha = Math.max(0.0, 1.0 - progress);

        String text = "LEVEL UP " + player.getLastLeveledUpTo();
        graphicsContext.save();
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
        double textWidth = measureTextWidth(graphicsContext, text);
        double textX = screenX - textWidth / 2.0;
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.75 * alpha));
        graphicsContext.strokeText(text, textX, screenY);
        graphicsContext.setFill(Color.color(1.0, 0.93, 0.46, 0.96 * alpha));
        graphicsContext.fillText(text, textX, screenY);
        graphicsContext.restore();
    }

    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }

    private void renderFloatingDamageTexts(List<FloatingDamageText> texts, double cameraX, double cameraY, long nowNs) {
        if (texts == null || texts.isEmpty()) {
            return;
        }
        for (FloatingDamageText text : texts) {
            if (text == null || text.isExpired(nowNs)) {
                continue;
            }
            double progress = text.getProgress(nowNs);
            double rise = 22.0 * progress;
            double alpha = 1.0 - progress;
            double drawX = text.getWorldX() - cameraX;
            double drawY = text.getWorldY() - cameraY - rise;

            graphicsContext.save();
            graphicsContext.setGlobalAlpha(Math.max(0.0, alpha));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, text.isCritical() ? 14 : 12));
            graphicsContext.setStroke(Color.color(0, 0, 0, 0.82));
            graphicsContext.strokeText(text.getText(), drawX, drawY);
            graphicsContext.setFill(text.isCritical() ? Color.web("#ffd24d") : Color.web("#ffebe6"));
            graphicsContext.fillText(text.getText(), drawX, drawY);
            graphicsContext.restore();
        }
    }

    private static final class LabelFpsTracker {
        private long lastFrameNs;
        private int lastFps;

        private void update(long now) {
            if (lastFrameNs <= 0L) {
                lastFrameNs = now;
                lastFps = 0;
                return;
            }
            long delta = now - lastFrameNs;
            lastFrameNs = now;
            if (delta <= 0L) {
                return;
            }
            lastFps = (int) Math.round(1_000_000_000.0 / delta);
        }

        private int getLastFps() {
            return lastFps;
        }
    }
}
