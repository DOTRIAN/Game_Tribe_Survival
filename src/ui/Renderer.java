package ui;

import buildsystem.core.BuildManager;
import buildsystem.object.BuildObject;
import buildsystem.fence.FenceEntity;
import buildsystem.object.ArcherTower;
import buildsystem.object.BombTrap;
import buildsystem.object.Chest;
import buildsystem.core.BuildPreview;
import buildsystem.sprite.AssetManager;
import core.GameBalance;
import core.GameState;
import dialogue.runtime.DialogueRunner;
import drop.DroppedItem;
import entity.Enemy;
import entity.FriendlyArcher;
import entity.Player;
import entity.ArrowProjectile;
import entity.BaseCamp;
import entity.ThrownBomb;
import input.InputHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import map.MapData;
import map.MapObjectData;
import map.MapRenderer;
import system.level.Level;
import system.level.LevelResult;
import system.level.PlayerProgress;
import system.bomb.ExplosionEffect;
import system.bomb.FireBombBurnZone;
import system.resource.ResourceNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.nio.file.Path;

/**
 * Renderer:
 * - Render world len Canvas, UI len JavaFX overlay nodes.
 * - Vi sao can: de fullscreen/resize fill kin cua so, trong khi UI van dung CSS va layout responsive.
 */
public class Renderer {
    private static final double CAMERA_ZOOM = 1.5;
    private static final boolean DEBUG_DRAW_WALL_BOUNDS = false;
    private static final boolean DEBUG_DRAW_WALL_INFO = true;
    private static final double NIGHT_OVERLAY_ALPHA_SCALE = 0.94;
    private static final LightRenderProfile PLAYER_NIGHT_LIGHT = new LightRenderProfile(
            0.48,
            0.46,
            Color.color(1.0, 0.98, 0.93, 0.25),
            0.20,
            0.82,
            Color.color(1.0, 0.96, 0.88, 0.07)
    );
    private static final LightRenderProfile PLAYER_DAY_LIGHT = new LightRenderProfile(
            0.42,
            0.28,
            Color.color(1.0, 0.98, 0.93, 0.18),
            0.16,
            0.76,
            Color.color(1.0, 0.96, 0.90, 0.05)
    );
    private static final LightRenderProfile TORCH_LIGHT_PROFILE = new LightRenderProfile(
            0.36,
            0.34,
            Color.color(1.0, 0.89, 0.62, 0.20),
            0.18,
            0.72,
            Color.color(1.0, 0.78, 0.42, 0.06)
    );
    private static final LightRenderProfile BONFIRE_LIGHT_PROFILE = new LightRenderProfile(
            0.44,
            0.40,
            Color.color(1.0, 0.88, 0.60, 0.22),
            0.22,
            0.82,
            Color.color(1.0, 0.76, 0.38, 0.08)
    );

    private final Stage stage;
    private final Canvas canvas;
    private final GraphicsContext graphicsContext;
    private final UIManager uiManager;
    private final SettingsManager settingsManager;
    private final GameSettings settings;
    private final AssetManager buildAssetManager;
    private final LightManager lightManager;
    private final Image welcomeBackgroundImage;
    private final Image introBackgroundImage;
    private final Image gameBackgroundImage;
    private final LabelFpsTracker fpsTracker;
    private final Map<String, Image> tintedBuildImageCache;
    private final Image sealGemImage;
    private DialogueRunner introDialogueRunner;
    private String skillUnlockCelebrationTitle;
    private String skillUnlockCelebrationText;
    private Player.AttackAnimationType skillUnlockCelebrationType;
    private boolean gemRewardAnimationActive;
    private double gemRewardAnimationProgress;
    private MapRenderer mapRenderer;
    private Image worldBackgroundImage;
    private boolean showBaseCamp;

    public Renderer(Stage stage, InputHandler inputHandler, AssetManager buildAssetManager) {
        this.stage = stage;
        this.canvas = new Canvas();
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.graphicsContext.setImageSmoothing(false);
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.buildAssetManager = buildAssetManager;
        this.lightManager = new LightManager();
        this.welcomeBackgroundImage = new Image(Path.of("assets", "anhintro.jpg").toUri().toString(), false);
        this.introBackgroundImage = new Image(Path.of("assets", "anhintro.jpg").toUri().toString(), false);
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");
        this.fpsTracker = new LabelFpsTracker();
        this.tintedBuildImageCache = new LinkedHashMap<>();
        this.sealGemImage = new Image(Path.of("assets", "vien ngoc.png").toUri().toString(), false);
        this.introDialogueRunner = null;
        this.skillUnlockCelebrationTitle = null;
        this.skillUnlockCelebrationText = null;
        this.skillUnlockCelebrationType = null;
        this.gemRewardAnimationActive = false;
        this.gemRewardAnimationProgress = 0.0;
        this.worldBackgroundImage = null;
        this.showBaseCamp = true;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f1114;");

        AnchorPane overlayLayer = new AnchorPane();
        overlayLayer.setPickOnBounds(false);
        root.getChildren().addAll(canvas, overlayLayer);

        double initialWidth = 1280;
        double initialHeight = 720;
        Scene scene = new Scene(root, initialWidth, initialHeight);
        inputHandler.attach(scene);

        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        this.uiManager = new UIManager(stage, scene, overlayLayer, buildAssetManager, settings);

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(540);
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMaximized(false);
        stage.show();
        stage.centerOnScreen();
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

    public void setWorldBackgroundImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            this.worldBackgroundImage = null;
            return;
        }
        this.worldBackgroundImage = new Image(Path.of(imagePath).toUri().toString(), false);
    }

    public void setShowBaseCamp(boolean showBaseCamp) {
        this.showBaseCamp = showBaseCamp;
    }

    public double getViewportWidth() {
        return Math.max(1.0, canvas.getWidth());
    }

    public double getViewportHeight() {
        return Math.max(1.0, canvas.getHeight());
    }

    public void render(GameState gameState, Player player, BaseCamp baseCamp, List<Enemy> enemies, List<FriendlyArcher> friendlyArchers, long now,
                       double cameraX, double cameraY, int menuIndex, boolean welcomeFlashing,
                       String playerNameDraft, int maxNameLength,
                       List<Level> levels, PlayerProgress playerProgress, int selectedLevelIndex,
                       Level currentLevel, String objectiveStatus, LevelResult lastLevelResult,
                       boolean debugCollisionOverlayEnabled, List<MapObjectData> mapCollisions,
                       Rectangle2D mapTransitionTrigger,
                       List<ResourceNode> allResources, Map<String, Integer> collectedResources,
                       int selectedHotbarIndex, int stoneWallCount,
                       boolean bossMode,
                       BuildManager buildManager,
                       List<HotbarItemStack> hotbarItems,
                       List<ArrowProjectile> arrowProjectiles,
                       List<ThrownBomb> thrownBombs,
                       List<DroppedItem> droppedItems,
                       List<ExplosionEffect> explosionEffects,
                       List<FireBombBurnZone> fireBombBurnZones,
                       List<FloatingDamageText> floatingDamageTexts,
                       double cameraShakeX,
                       double cameraShakeY,
                       double screenFlashAlpha,
                       double mapFadeAlpha,
                       double darknessAlpha, boolean isNight, String dayNightPhase,
                       String timeIcon, String timeTitle, String timeClock, String timeAnnouncement,
                       double worldWidth, double worldHeight) {
        double viewportWidth = getViewportWidth();
        double viewportHeight = getViewportHeight();

        uiManager.applyGameState(gameState);
        if (gameState == GameState.NAME_INPUT) {
            uiManager.setNameDraft(playerNameDraft, maxNameLength);
        }
        if (gameState == GameState.INTRO || gameState == GameState.DIALOGUE) {
            uiManager.updateIntroDialogue(introDialogueRunner, now);
        }
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_COMPLETE) {
            uiManager.updateHud(
                    player,
                    collectedResources,
                    selectedHotbarIndex,
                    hotbarItems,
                    bossMode,
                    worldWidth,
                    worldHeight,
                    cameraX,
                    cameraY,
                    CAMERA_ZOOM,
                    viewportWidth,
                    viewportHeight,
                    enemies
            );
        }

        graphicsContext.setFill(Color.web("#121416"));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
        graphicsContext.setImageSmoothing(false);

        if (gameState == GameState.INTRO) {
            drawBackgroundCover(introBackgroundImage, viewportWidth, viewportHeight);
            graphicsContext.setFill(Color.color(0, 0, 0, welcomeFlashing ? 0.42 : 0.28));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            return;
        }

        if (gameState == GameState.WELCOME || gameState == GameState.NAME_INPUT || gameState == GameState.GUIDE || uiManager.isSettingsVisible()) {
            drawBackgroundCover(welcomeBackgroundImage, viewportWidth, viewportHeight);
            graphicsContext.setFill(Color.color(0, 0, 0, welcomeFlashing ? 0.42 : 0.28));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            return;
        }

        if (gameState == GameState.DIALOGUE || gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_COMPLETE) {
            renderGameplay(player, baseCamp, enemies, friendlyArchers, now, cameraX, cameraY, currentLevel, objectiveStatus,
                    debugCollisionOverlayEnabled, mapCollisions, mapTransitionTrigger, allResources, collectedResources, buildManager, arrowProjectiles, thrownBombs, droppedItems, explosionEffects, fireBombBurnZones, floatingDamageTexts,
                    cameraShakeX, cameraShakeY, screenFlashAlpha,
                    darknessAlpha, isNight, dayNightPhase, timeIcon, timeTitle, timeClock, timeAnnouncement,
                    worldWidth, worldHeight, viewportWidth, viewportHeight);
        } else {
            drawBackgroundCover(gameBackgroundImage, viewportWidth, viewportHeight);
        }

        if (mapFadeAlpha > 0.001) {
            graphicsContext.save();
            graphicsContext.setFill(Color.color(0.0, 0.0, 0.0, Math.min(1.0, mapFadeAlpha)));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            graphicsContext.restore();
        }
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

    public void setIntroDialogueRunner(DialogueRunner introDialogueRunner) {
        this.introDialogueRunner = introDialogueRunner;
    }

    public void setSkillUnlockCelebration(String skillUnlockCelebrationTitle,
                                          String skillUnlockCelebrationText,
                                          Player.AttackAnimationType skillUnlockCelebrationType) {
        this.skillUnlockCelebrationTitle = skillUnlockCelebrationTitle;
        this.skillUnlockCelebrationText = skillUnlockCelebrationText;
        this.skillUnlockCelebrationType = skillUnlockCelebrationType;
    }

    public void setGemRewardAnimation(boolean active, double progress) {
        this.gemRewardAnimationActive = active;
        this.gemRewardAnimationProgress = Math.max(0.0, Math.min(1.0, progress));
    }

    public boolean isIntroPageFullyRevealed(long nowNs) {
        return uiManager.isIntroPageFullyRevealed(nowNs);
    }

    public void revealIntroPageImmediately() {
        uiManager.revealIntroPageImmediately();
    }

    public void setSettingsBackAction(Runnable onBack) {
        uiManager.configureSettingsAction(onBack);
    }

    public void setHotbarSelectionListener(IntConsumer listener) {
        uiManager.configureHotbarAction(listener);
    }

    public void setShopUiActions(Consumer<String> onBuy, Runnable onOpen, Runnable onClose) {
        uiManager.configureShopAction(onBuy, onOpen, onClose);
    }

    public void setInventoryCloseAction(Runnable listener) {
        uiManager.configureInventoryClose(listener);
    }

    public void setChestCloseAction(Runnable listener) {
        uiManager.configureChestClose(listener);
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

    public void setChestVisible(boolean visible) {
        uiManager.setChestVisible(visible);
    }

    public boolean isChestVisible() {
        return uiManager.isChestVisible();
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

    public ItemUiMeta getItemMeta(String itemId) {
        return uiManager.getItemMeta(itemId);
    }

    private void renderGameplay(Player player,
                                BaseCamp baseCamp,
                                List<Enemy> enemies,
                                List<FriendlyArcher> friendlyArchers,
                                long now,
                                double cameraX,
                                double cameraY,
                                Level currentLevel,
                                String objectiveStatus,
                                boolean debugCollisionOverlayEnabled,
                                List<MapObjectData> mapCollisions,
                                Rectangle2D mapTransitionTrigger,
                                List<ResourceNode> allResources,
                                Map<String, Integer> collectedResources,
                                BuildManager buildManager,
                                List<ArrowProjectile> arrowProjectiles,
                                List<ThrownBomb> thrownBombs,
                                List<DroppedItem> droppedItems,
                                List<ExplosionEffect> explosionEffects,
                                List<FireBombBurnZone> fireBombBurnZones,
                                List<FloatingDamageText> floatingDamageTexts,
                                double cameraShakeX,
                                double cameraShakeY,
                                double screenFlashAlpha,
                                double darknessAlpha,
                                boolean isNight,
                                String dayNightPhase,
                                String timeIcon,
                                String timeTitle,
                                String timeClock,
                                String timeAnnouncement,
                                double worldWidth,
                                double worldHeight,
                                double viewportWidth,
                                double viewportHeight) {
        graphicsContext.save();
        graphicsContext.translate(cameraShakeX, cameraShakeY);
        graphicsContext.scale(CAMERA_ZOOM, CAMERA_ZOOM);

        if (mapRenderer != null) {
            mapRenderer.setResources(allResources);
            mapRenderer.renderBelowEntities(graphicsContext, cameraX, cameraY, now);
        } else if (worldBackgroundImage != null && !worldBackgroundImage.isError()) {
            graphicsContext.drawImage(worldBackgroundImage, -cameraX, -cameraY);
        } else {
            drawBackgroundCover(gameBackgroundImage, viewportWidth / CAMERA_ZOOM, viewportHeight / CAMERA_ZOOM);
        }

        renderBuildPreview(buildManager, cameraX, cameraY);
        List<ArcherTower> archerTowers = renderPlacedBuildObjects(buildManager, cameraX, cameraY, now);
        renderArrowProjectiles(arrowProjectiles, cameraX, cameraY);
        renderThrownBombs(thrownBombs, cameraX, cameraY, now);
        renderDroppedItems(droppedItems, cameraX, cameraY, now);
        renderExplosionEffects(explosionEffects, cameraX, cameraY, now);
        renderFireBombBurnZones(fireBombBurnZones, cameraX, cameraY, now);
        if (showBaseCamp) {
            renderBaseCampHpBar(baseCamp, cameraX, cameraY);
        }

        player.draw(graphicsContext, cameraX, cameraY);
        renderArcherTowers(archerTowers, cameraX, cameraY, now);
        renderFriendlyArchers(friendlyArchers, cameraX, cameraY, now);
        renderLevelUpEffect(player, cameraX, cameraY, now);
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.shouldRender(now)) {
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
        if (debugCollisionOverlayEnabled) {
            renderCollisionOverlay(player, baseCamp, enemies, friendlyArchers, mapCollisions, mapTransitionTrigger, allResources, buildManager, cameraX, cameraY);
        }
        graphicsContext.restore();

        if (screenFlashAlpha > 0.001) {
            graphicsContext.save();
            graphicsContext.setFill(Color.color(1.0, 1.0, 1.0, Math.min(0.9, screenFlashAlpha)));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            graphicsContext.restore();
        }

        renderNightOverlayAndLights(buildManager, cameraX, cameraY, darknessAlpha, now);

        graphicsContext.setFill(Color.color(1, 1, 1, 0.82));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText(dayNightPhase, 16, viewportHeight - 18);
        renderNightAnnouncement(viewportWidth, timeAnnouncement);

        if (objectiveStatus != null && !objectiveStatus.isBlank()) {
            renderObjectivePanel(currentLevel, objectiveStatus);
        }
        renderSealGemBadge(collectedResources, viewportWidth);
        renderGemRewardAnimation(collectedResources, viewportWidth);
        renderSkillUnlockCelebration(player, now, viewportWidth);

        if (settings.isShowFps()) {
            fpsTracker.update(now);
            graphicsContext.setFill(Color.color(1, 1, 1, 0.90));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            graphicsContext.fillText("FPS " + fpsTracker.getLastFps(), viewportWidth - 92, viewportHeight - 18);
        }
        if (debugCollisionOverlayEnabled) {
            graphicsContext.setFill(Color.color(1.0, 0.98, 0.78, 0.95));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            graphicsContext.fillText("Collision Debug [F3]", 16, viewportHeight - 36);
        }
    }

    private void renderNightAnnouncement(double viewportWidth, String announcement) {
        if (announcement == null || announcement.isBlank()) {
            return;
        }
        double warningWidth = 360.0;
        double warningHeight = 68.0;
        double warningX = Math.max(20.0, (viewportWidth - warningWidth) * 0.5);
        double warningY = 82.0;
        graphicsContext.save();
        graphicsContext.setFill(Color.color(0.02, 0.02, 0.02, 0.68));
        graphicsContext.fillRoundRect(warningX, warningY, warningWidth, warningHeight, 18, 18);
        graphicsContext.setStroke(Color.color(1.0, 0.35, 0.22, 0.72));
        graphicsContext.strokeRoundRect(warningX + 0.5, warningY + 0.5, warningWidth - 1, warningHeight - 1, 18, 18);
        graphicsContext.setFill(Color.web("#fff0cc"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        String[] lines = announcement.split("\\R", 3);
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            graphicsContext.fillText(lines[i], warningX + 18, warningY + 27 + i * 22);
        }
        graphicsContext.restore();
    }

    private void renderObjectivePanel(Level currentLevel, String objectiveStatus) {
        String title = currentLevel != null
                ? "L" + currentLevel.getId() + " " + currentLevel.getName()
                : "Nhiệm vụ hiện tại";
        String[] lines = objectiveStatus.split("\\R");
        int lineCount = Math.max(1, lines.length);

        double panelWidth = 280;
        double panelX = getViewportWidth() - panelWidth - 252;
        double titleY = 24;
        double titleWidth = 154;
        double titleHeight = 26;
        double panelY = 40;
        double panelHeight = 22 + (lineCount * 22);

        graphicsContext.setFill(Color.color(0.12, 0.09, 0.07, 0.92));
        graphicsContext.fillRoundRect(panelX, titleY, titleWidth, titleHeight, 10, 10);
        graphicsContext.setStroke(Color.color(0.87, 0.72, 0.47, 0.75));
        graphicsContext.setLineWidth(1.2);
        graphicsContext.strokeRoundRect(panelX, titleY, titleWidth, titleHeight, 10, 10);

        graphicsContext.setFill(Color.color(0, 0, 0, 0.34));
        graphicsContext.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 12, 12);
        graphicsContext.setStroke(Color.color(0.87, 0.72, 0.47, 0.28));
        graphicsContext.strokeRoundRect(panelX, panelY, panelWidth, panelHeight, 12, 12);

        graphicsContext.setFill(Color.web("#f4efe1"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        graphicsContext.fillText(title, panelX + 14, titleY + 17);

        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        double textY = panelY + 22;
        for (String line : lines) {
            graphicsContext.fillText(line, panelX + 14, textY);
            textY += 20;
        }
    }

    private void renderSkillUnlockCelebration(Player player, long now, double viewportWidth) {
        if (skillUnlockCelebrationText == null || skillUnlockCelebrationText.isBlank() || player == null) {
            return;
        }
        Image frame = player.getSkillUnlockPreviewFrame(now, skillUnlockCelebrationType);
        double panelWidth = 430;
        double panelHeight = 110;
        double panelX = (viewportWidth - panelWidth) * 0.5;
        double panelY = 82.0;

        graphicsContext.setFill(Color.color(0.08, 0.06, 0.04, 0.88));
        graphicsContext.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
        graphicsContext.setStroke(Color.color(0.88, 0.72, 0.40, 0.9));
        graphicsContext.setLineWidth(1.5);
        graphicsContext.strokeRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

        if (frame != null && !frame.isError()) {
            graphicsContext.drawImage(frame, panelX + 18, panelY + 14, 72, 72);
        }

        graphicsContext.setFill(Color.web("#f5e7c8"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        graphicsContext.fillText(
                skillUnlockCelebrationTitle == null || skillUnlockCelebrationTitle.isBlank()
                        ? "SKILL UNLOCK"
                        : skillUnlockCelebrationTitle,
                panelX + 104,
                panelY + 34
        );
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText(skillUnlockCelebrationText, panelX + 104, panelY + 58);
        String keyHint;
        if (skillUnlockCelebrationType == null) {
            keyHint = "Nhấn F để dùng kỹ năng.";
        } else {
            keyHint = switch (skillUnlockCelebrationType) {
                case CRUSH -> "Nhấn J để dùng kỹ năng.";
                case PIERCE -> "Nhấn K để dùng kỹ năng.";
                case HIT, SLICE -> "Nhấn F để dùng kỹ năng.";
            };
        }
        graphicsContext.fillText(keyHint, panelX + 104, panelY + 80);
    }

    private void renderSealGemBadge(Map<String, Integer> collectedResources, double viewportWidth) {
        int gemCount = collectedResources == null ? 0 : Math.max(0, collectedResources.getOrDefault("seal_gem", 0));
        if (gemCount <= 0 && !gemRewardAnimationActive) {
            return;
        }
        if (sealGemImage == null || sealGemImage.isError()) {
            return;
        }

        double badgeWidth = 220.0;
        double badgeHeight = 54.0;
        double badgeX = viewportWidth - badgeWidth - 20.0;
        double badgeY = 204.0;

        graphicsContext.save();
        graphicsContext.setFill(Color.color(0.10, 0.08, 0.06, 0.90));
        graphicsContext.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 12, 12);
        graphicsContext.setStroke(Color.color(0.88, 0.74, 0.45, 0.78));
        graphicsContext.setLineWidth(1.2);
        graphicsContext.strokeRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 12, 12);
        graphicsContext.drawImage(sealGemImage, badgeX + 12.0, badgeY + 9.0, 36.0, 36.0);
        graphicsContext.setFill(Color.web("#f6edc7"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        graphicsContext.fillText("NGỌC PHONG ẤN", badgeX + 58.0, badgeY + 22.0);
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText("Số lượng: " + gemCount, badgeX + 58.0, badgeY + 40.0);
        graphicsContext.restore();
    }

    private void renderGemRewardAnimation(Map<String, Integer> collectedResources, double viewportWidth) {
        if (!gemRewardAnimationActive || sealGemImage == null || sealGemImage.isError()) {
            return;
        }

        double progress = gemRewardAnimationProgress;
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        double startX = (viewportWidth * 0.5) - 36.0;
        double startY = 138.0;
        double badgeWidth = 220.0;
        double badgeX = viewportWidth - badgeWidth - 20.0;
        double badgeY = 204.0;
        double endX = badgeX + 12.0;
        double endY = badgeY + 9.0;
        double x = startX + ((endX - startX) * eased);
        double y = startY + ((endY - startY) * eased);
        double size = 72.0 - (20.0 * eased);

        graphicsContext.save();
        graphicsContext.setGlobalAlpha(0.92);
        graphicsContext.setFill(Color.color(0.93, 0.82, 0.42, 0.22 * (1.0 - progress)));
        graphicsContext.fillOval(x - 10.0, y - 10.0, size + 20.0, size + 20.0);
        graphicsContext.drawImage(sealGemImage, x, y, size, size);
        graphicsContext.setFill(Color.web("#f6edc7"));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        graphicsContext.fillText("Ngọc Phong Ấn", startX - 28.0, startY - 14.0);
        graphicsContext.restore();
    }

    private void renderCollisionOverlay(Player player,
                                        BaseCamp baseCamp,
                                        List<Enemy> enemies,
                                        List<FriendlyArcher> friendlyArchers,
                                        List<MapObjectData> mapCollisions,
                                        Rectangle2D mapTransitionTrigger,
                                        List<ResourceNode> resources,
                                        BuildManager buildManager,
                                        double cameraX,
                                        double cameraY) {
        graphicsContext.save();
        graphicsContext.setLineWidth(1.0);

        if (mapCollisions != null) {
            graphicsContext.setStroke(Color.color(1.0, 0.2, 0.2, 0.70));
            for (MapObjectData object : mapCollisions) {
                if (object == null || !"Collision".equalsIgnoreCase(object.getType())) {
                    continue;
                }
                graphicsContext.strokeRect(object.getX() - cameraX, object.getY() - cameraY, object.getWidth(), object.getHeight());
            }
        }

        if (mapTransitionTrigger != null && mapTransitionTrigger.getWidth() > 0 && mapTransitionTrigger.getHeight() > 0) {
            graphicsContext.setStroke(Color.color(1.0, 0.78, 0.12, 0.95));
            graphicsContext.setLineWidth(1.6);
            graphicsContext.strokeRect(
                    mapTransitionTrigger.getMinX() - cameraX,
                    mapTransitionTrigger.getMinY() - cameraY,
                    mapTransitionTrigger.getWidth(),
                    mapTransitionTrigger.getHeight()
            );
            graphicsContext.setFill(Color.color(1.0, 0.78, 0.12, 0.16));
            graphicsContext.fillRect(
                    mapTransitionTrigger.getMinX() - cameraX,
                    mapTransitionTrigger.getMinY() - cameraY,
                    mapTransitionTrigger.getWidth(),
                    mapTransitionTrigger.getHeight()
            );
            graphicsContext.setFill(Color.color(1.0, 0.95, 0.72, 0.96));
            graphicsContext.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            graphicsContext.fillText(
                    "MAP TRIGGER",
                    mapTransitionTrigger.getMinX() - cameraX,
                    mapTransitionTrigger.getMinY() - cameraY - 4.0
            );
            graphicsContext.setLineWidth(1.0);
        }

        if (buildManager != null) {
            graphicsContext.setStroke(Color.color(0.25, 1.0, 1.0, 0.90));
            for (BuildObject object : buildManager.getPlacedObjects()) {
                if (object == null) {
                    continue;
                }
                strokeWorldRect(object.getCollisionX(), object.getCollisionY(), object.getCollisionWidth(), object.getCollisionHeight(), cameraX, cameraY);
            }
        }

        if (baseCamp != null) {
            graphicsContext.setStroke(Color.color(1.0, 0.25, 0.9, 0.95));
            strokeWorldRect(baseCamp.getCollisionX(), baseCamp.getCollisionY(), baseCamp.getCollisionWidth(), baseCamp.getCollisionHeight(), cameraX, cameraY);
        }

        if (player != null) {
            graphicsContext.setStroke(Color.color(0.2, 1.0, 0.25, 0.95));
            strokeWorldRect(player.getCollisionX(), player.getCollisionY(), player.getCollisionWidth(), player.getCollisionHeight(), cameraX, cameraY);
        }

        if (friendlyArchers != null) {
            graphicsContext.setStroke(Color.color(0.2, 0.85, 1.0, 0.95));
            for (FriendlyArcher archer : friendlyArchers) {
                if (archer == null || !archer.isAlive()) {
                    continue;
                }
                strokeWorldRect(archer.getCollisionX(), archer.getCollisionY(), archer.getCollisionWidth(), archer.getCollisionHeight(), cameraX, cameraY);
            }
        }

        if (enemies != null) {
            graphicsContext.setStroke(Color.color(1.0, 0.15, 0.15, 0.95));
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive() || enemy.shouldRemoveFromWorld()) {
                    continue;
                }
                strokeWorldRect(enemy.getCollisionX(), enemy.getCollisionY(), enemy.getCollisionWidth(), enemy.getCollisionHeight(), cameraX, cameraY);
            }
        }

        graphicsContext.restore();
    }

    private void strokeWorldRect(double worldX, double worldY, double width, double height, double cameraX, double cameraY) {
        graphicsContext.strokeRect(worldX - cameraX, worldY - cameraY, width, height);
    }

    private List<ArcherTower> renderPlacedBuildObjects(BuildManager buildManager, double cameraX, double cameraY, long nowNs) {
        if (buildManager == null) {
            return List.of();
        }
        List<FenceEntity> fences = new ArrayList<>();
        List<ArcherTower> archerTowers = new ArrayList<>();
        double margin = 128.0;
        double worldViewWidth = getViewportWidth() / CAMERA_ZOOM;
        double worldViewHeight = getViewportHeight() / CAMERA_ZOOM;
        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                cameraX - margin,
                cameraY - margin,
                worldViewWidth + margin * 2.0,
                worldViewHeight + margin * 2.0
        )) {
            if (object == null) {
                continue;
            }
            if (object instanceof FenceEntity fence) {
                fences.add(fence);
                continue;
            }
            if (object instanceof ArcherTower archerTower) {
                archerTowers.add(archerTower);
                continue;
            }
            double screenX = object.getRenderX() - cameraX;
            double screenY = object.getRenderY() - cameraY;
            Image objectImage = buildImageFor(object, nowNs);
            if (objectImage != null && !objectImage.isError()) {
                drawRotatedImage(objectImage, screenX, screenY, object.getRenderWidth(), object.getRenderHeight(), object.getRotationDegrees());
                if (object.isHitFlashActive(nowNs)) {
                    Image tinted = buildTintedBySourceAlpha(objectImage, object.getHitFlashColor());
                    if (tinted != null) {
                        graphicsContext.save();
                        graphicsContext.setGlobalAlpha(0.58);
                        drawRotatedImage(tinted, screenX, screenY, object.getRenderWidth(), object.getRenderHeight(), object.getRotationDegrees());
                        graphicsContext.restore();
                    }
                }
                if (DEBUG_DRAW_WALL_BOUNDS) {
                    graphicsContext.setStroke(Color.color(0.0, 1.0, 1.0, 0.75));
                    graphicsContext.strokeRect(screenX, screenY, object.getRenderWidth(), object.getRenderHeight());
                }
                continue;
            }
            graphicsContext.setFill(Color.color(0.75, 0.75, 0.75, 0.85));
            graphicsContext.fillRect(screenX, screenY, object.getRenderWidth(), object.getRenderHeight());
        }
        fences.sort(Comparator.comparingDouble(FenceEntity::getFootY));
        renderFenceHorizontalConnectors(fences, buildManager, cameraX, cameraY);
        for (FenceEntity fence : fences) {
            renderBuildObject(fence, cameraX, cameraY, nowNs);
        }
        return archerTowers;
    }

    private void renderArcherTowers(List<ArcherTower> archerTowers, double cameraX, double cameraY, long nowNs) {
        if (archerTowers == null || archerTowers.isEmpty()) {
            return;
        }
        for (ArcherTower archerTower : archerTowers) {
            renderBuildObject(archerTower, cameraX, cameraY, nowNs);
        }
    }

    private void renderFriendlyArchers(List<FriendlyArcher> friendlyArchers, double cameraX, double cameraY, long nowNs) {
        if (friendlyArchers == null || friendlyArchers.isEmpty()) {
            return;
        }
        for (FriendlyArcher archer : friendlyArchers) {
            if (archer == null) {
                continue;
            }
            Image[] frames = buildAssetManager.getAnimationFrames(archer.getAnimationKey());
            int frameIndex = Math.max(0, Math.min(archer.getFrameIndex(), Math.max(0, frames.length - 1)));
            Image frame = (frames.length == 0) ? buildAssetManager.getSprite("friendly_archer_icon") : frames[frameIndex];
            double screenX = Math.round(archer.getRenderX() - cameraX);
            double screenY = Math.round(archer.getRenderY() - cameraY);
            double renderWidth = archer.getRenderWidth();
            double renderHeight = archer.getRenderHeight();
            if (frame == null || frame.isError()) {
                graphicsContext.setFill(Color.DARKSEAGREEN);
                graphicsContext.fillRect(screenX, screenY, renderWidth, renderHeight);
                continue;
            }
            graphicsContext.save();
            if (archer.isFacingRight()) {
                graphicsContext.drawImage(frame, screenX, screenY, renderWidth, renderHeight);
            } else {
                graphicsContext.translate(screenX + renderWidth, screenY);
                graphicsContext.scale(-1, 1);
                graphicsContext.drawImage(frame, 0, 0, renderWidth, renderHeight);
            }
            graphicsContext.restore();
        }
    }

    private void renderFenceHorizontalConnectors(List<FenceEntity> fences, BuildManager buildManager, double cameraX, double cameraY) {
        Image connector = buildAssetManager.getSprite(buildsystem.fence.FenceRenderer.CONNECTOR_SPRITE_KEY);
        if (buildManager == null || connector == null || connector.isError() || fences == null || fences.isEmpty()) {
            return;
        }
        for (FenceEntity fence : fences) {
            if (fence == null) {
                continue;
            }
            BuildObject rightNeighbor = buildManager.getPlacedObjectAt(fence.getTileX() + 1, fence.getTileY());
            if (!(rightNeighbor instanceof FenceEntity)) {
                continue;
            }

            double leftCenterX = fence.getRenderX() + fence.getRenderWidth() / 2.0;
            double rightCenterX = rightNeighbor.getRenderX() + rightNeighbor.getRenderWidth() / 2.0;
            double connectorWidth = Math.max(1.0, rightCenterX - leftCenterX);
            double connectorHeight = fence.getRenderHeight() * 0.24;
            double drawX = leftCenterX - cameraX;
            double drawY = fence.getRenderY() - cameraY + fence.getRenderHeight() * 0.43;
            graphicsContext.drawImage(connector, drawX, drawY, connectorWidth, connectorHeight);
        }
    }

    private void renderBuildObject(BuildObject object, double cameraX, double cameraY, long nowNs) {
        if (object == null) {
            return;
        }
        double screenX = object.getRenderX() - cameraX;
        double screenY = object.getRenderY() - cameraY;
        Image objectImage = buildImageFor(object, nowNs);
        if (objectImage != null && !objectImage.isError()) {
            drawRotatedImage(objectImage, screenX, screenY, object.getRenderWidth(), object.getRenderHeight(), object.getRotationDegrees());
            if (object.isHitFlashActive(nowNs)) {
                Image tinted = buildTintedBySourceAlpha(objectImage, object.getHitFlashColor());
                if (tinted != null) {
                    graphicsContext.save();
                    graphicsContext.setGlobalAlpha(0.58);
                    drawRotatedImage(tinted, screenX, screenY, object.getRenderWidth(), object.getRenderHeight(), object.getRotationDegrees());
                    graphicsContext.restore();
                }
            }
            if (DEBUG_DRAW_WALL_BOUNDS) {
                graphicsContext.setStroke(Color.color(0.0, 1.0, 1.0, 0.75));
                graphicsContext.strokeRect(screenX, screenY, object.getRenderWidth(), object.getRenderHeight());
            }
            return;
        }
        graphicsContext.setFill(Color.color(0.75, 0.75, 0.75, 0.85));
        graphicsContext.fillRect(screenX, screenY, object.getRenderWidth(), object.getRenderHeight());
    }

    private void renderDroppedItems(List<DroppedItem> droppedItems, double cameraX, double cameraY, long nowNs) {
        if (droppedItems == null) {
            return;
        }
        for (DroppedItem droppedItem : droppedItems) {
            if (droppedItem == null) {
                continue;
            }
            double bobOffset = Math.sin(nowNs / 140_000_000.0) * 1.25;
            Image image = droppedItem.getCurrentImage(nowNs);
            if (image != null && !image.isError()) {
                graphicsContext.drawImage(
                        image,
                        droppedItem.getX() - cameraX,
                        droppedItem.getY() - cameraY + bobOffset,
                        droppedItem.getRenderWidth(),
                        droppedItem.getRenderHeight()
                );
                continue;
            }
        }
    }

    private void renderExplosionEffects(List<ExplosionEffect> explosionEffects, double cameraX, double cameraY, long nowNs) {
        if (explosionEffects == null || explosionEffects.isEmpty()) {
            return;
        }
        for (ExplosionEffect effect : explosionEffects) {
            if (effect == null || !effect.isAlive(nowNs)) {
                continue;
            }
            effect.render(graphicsContext, cameraX, cameraY, 1.0, nowNs);
        }
    }

    private void renderArrowProjectiles(List<ArrowProjectile> arrowProjectiles, double cameraX, double cameraY) {
        if (arrowProjectiles == null) {
            return;
        }
        for (ArrowProjectile arrow : arrowProjectiles) {
            if (arrow == null || !arrow.isAlive()) {
                continue;
            }
            double screenX = arrow.getX() - cameraX;
            double screenY = arrow.getY() - cameraY;
            Image arrowSprite = buildAssetManager.getSprite(arrow.getSpriteKey());
            if (arrowSprite == null || arrowSprite.isError()) {
                arrowSprite = buildAssetManager.getSprite("archer_arrow");
            }
            if (arrowSprite != null && !arrowSprite.isError()) {
                double drawWidth = arrow.getWidth();
                double drawHeight = arrow.getHeight();
                if ("friendly_archer_arrow".equals(arrow.getSpriteKey())) {
                    drawWidth = Math.max(1.0, arrowSprite.getWidth());
                    drawHeight = Math.max(1.0, arrowSprite.getHeight());
                    screenX = arrow.getCenterX() - cameraX - drawWidth / 2.0;
                    screenY = arrow.getCenterY() - cameraY - drawHeight / 2.0;
                }
                drawRotatedImage(arrowSprite, screenX, screenY, drawWidth, drawHeight, arrow.getRotationDegrees());
                continue;
            }
            graphicsContext.save();
            graphicsContext.translate(screenX + arrow.getWidth() / 2.0, screenY + arrow.getHeight() / 2.0);
            graphicsContext.rotate(arrow.getRotationDegrees());
            graphicsContext.setFill(Color.web("#c8aa6a"));
            graphicsContext.fillRect(-arrow.getWidth() / 2.0, -arrow.getHeight() / 2.0, arrow.getWidth(), arrow.getHeight());
            graphicsContext.restore();
        }
    }

    private void renderFireBombBurnZones(List<FireBombBurnZone> fireBombBurnZones, double cameraX, double cameraY, long nowNs) {
        if (fireBombBurnZones == null || fireBombBurnZones.isEmpty()) {
            return;
        }
        for (FireBombBurnZone zone : fireBombBurnZones) {
            if (zone == null || zone.isExpired(nowNs)) {
                continue;
            }
            zone.render(graphicsContext, cameraX, cameraY, 1.0, nowNs);
        }
    }

    private void renderThrownBombs(List<ThrownBomb> thrownBombs, double cameraX, double cameraY, long nowNs) {
        if (thrownBombs == null || thrownBombs.isEmpty()) {
            return;
        }
        for (ThrownBomb bomb : thrownBombs) {
            if (bomb == null) {
                continue;
            }
            double size = bomb.getRenderSize();
            double screenX = bomb.getX() - cameraX;
            double screenY = bomb.getY() - cameraY - bomb.getArcHeight();
            Image bombSprite = resolveThrownBombSprite(bomb, nowNs);
            if (bombSprite != null && !bombSprite.isError()) {
                graphicsContext.drawImage(bombSprite, screenX - size * 0.5, screenY - size * 0.5, size, size);
            } else {
                graphicsContext.setFill(Color.web("#ffb74d"));
                graphicsContext.fillOval(screenX - size * 0.5, screenY - size * 0.5, size, size);
            }
        }
    }

    private void renderBaseCampHpBar(BaseCamp baseCamp, double cameraX, double cameraY) {
        if (baseCamp == null || baseCamp.getMaxHp() <= 0) {
            return;
        }
        double barWidth = Math.max(48.0, baseCamp.getWidth() * 0.72);
        double barHeight = 7.0;
        double screenX = Math.round(baseCamp.getCenterX() - cameraX - barWidth * 0.5);
        double screenY = baseCamp.getHpBarScreenY(cameraY);
        double hpRatio = Math.max(0.0, Math.min(1.0, baseCamp.getHp() / (double) baseCamp.getMaxHp()));

        graphicsContext.setFill(Color.color(0.08, 0.08, 0.08, 0.88));
        graphicsContext.fillRoundRect(screenX - 2, screenY - 2, barWidth + 4, barHeight + 4, 8, 8);
        graphicsContext.setFill(Color.color(0.24, 0.07, 0.07, 0.92));
        graphicsContext.fillRoundRect(screenX, screenY, barWidth, barHeight, 6, 6);
        graphicsContext.setFill(Color.web("#d95f5f"));
        graphicsContext.fillRoundRect(screenX, screenY, barWidth * hpRatio, barHeight, 6, 6);
        graphicsContext.setStroke(Color.color(1.0, 0.92, 0.76, 0.85));
        graphicsContext.setLineWidth(1.0);
        graphicsContext.strokeRoundRect(screenX - 1, screenY - 1, barWidth + 2, barHeight + 2, 7, 7);
    }

    private Image resolveThrownBombSprite(ThrownBomb bomb, long nowNs) {
        if (bomb == null) {
            return null;
        }
        if ("bomb_trap".equalsIgnoreCase(bomb.getBombItemId())) {
            Image[] frames = buildAssetManager.getAnimationFrames("bomb_trap");
            if (frames.length > 0) {
                int frameIndex = bomb.resolveBombTrapFrameIndex(nowNs, frames.length);
                if (frameIndex >= 0 && frameIndex < frames.length) {
                    return frames[frameIndex];
                }
                return frames[0];
            }
            return buildAssetManager.getSprite("bomb_trap_icon");
        }

        Image[] bombFrames = buildAssetManager.getAnimationFrames("fire_bomb_throw");
        if (bombFrames.length > 0) {
            return bombFrames[(int) ((nowNs / 90_000_000L) % bombFrames.length)];
        }
        return buildAssetManager.getSprite("fire_bomb_projectile");
    }

    private void renderBuildPreview(BuildManager buildManager, double cameraX, double cameraY) {
        if (buildManager == null || !buildManager.isPreviewVisible()) {
            return;
        }
        BuildPreview preview = buildManager.getPreview();
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
        if (preview.getType() == buildsystem.core.BuildType.BOMB_TRAP) {
            BombPreviewRenderer.renderPlacementTint(graphicsContext, preview, screenX, screenY);
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

    private Image buildImageFor(BuildObject object, long nowNs) {
        if (object == null || object.getSpriteKey() == null) {
            return null;
        }
        String buildType = object.getType().name().toLowerCase();
        if ("torch".equalsIgnoreCase(buildType)) {
            Image animatedFrame = buildAssetManager.getAnimationFrame("torch", nowNs, GameBalance.TORCH_ANIMATION_FRAME_NS);
            if (animatedFrame != null && !animatedFrame.isError()) {
                return animatedFrame;
            }
        }
        if ("archer_tower".equalsIgnoreCase(buildType)) {
            Image animatedFrame = buildAssetManager.getAnimationFrame("archer_tower_idle", nowNs, GameBalance.ARCHER_TOWER_ANIMATION_FRAME_NS);
            if (animatedFrame != null && !animatedFrame.isError()) {
                return animatedFrame;
            }
        }
        if ("bomb_trap".equalsIgnoreCase(buildType) && object instanceof BombTrap bombTrap) {
            Image[] frames = buildAssetManager.getAnimationFrames("bomb_trap");
            if (frames.length > 0) {
                int frameIndex = bombTrap.resolveAnimationFrameIndex(nowNs, frames.length);
                if (frameIndex >= 0 && frameIndex < frames.length) {
                    return frames[frameIndex];
                }
                return frames[0];
            }
        }
        if ("chest".equalsIgnoreCase(buildType) && object instanceof Chest chest) {
            if (chest.isAjar(nowNs)) {
                Image frame = buildAssetManager.getSprite("chest_ajar");
                if (frame != null && !frame.isError()) {
                    return frame;
                }
            }
            if (chest.isOpen(nowNs)) {
                Image frame = buildAssetManager.getSprite("chest_open");
                if (frame != null && !frame.isError()) {
                    return frame;
                }
            }
            Image closed = buildAssetManager.getSprite("chest_closed");
            if (closed != null && !closed.isError()) {
                return closed;
            }
        }
        try {
            return buildAssetManager.getSprite(object.getSpriteKey());
        } catch (Exception ignored) {
            return null;
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

    private void renderNightOverlayAndLights(BuildManager buildManager, double cameraX, double cameraY, double darknessAlpha, long nowNs) {
        lightManager.render(
                graphicsContext,
                buildManager,
                cameraX,
                cameraY,
                CAMERA_ZOOM,
                getViewportWidth(),
                getViewportHeight(),
                darknessAlpha,
                nowNs
        );
    }

    private Image buildTintedBySourceAlpha(Image source, Color tint) {
        if (source == null || source.isError() || tint == null) {
            return null;
        }
        int w = (int) source.getWidth();
        int h = (int) source.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        int tintR = (int) Math.round(tint.getRed() * 255.0);
        int tintG = (int) Math.round(tint.getGreen() * 255.0);
        int tintB = (int) Math.round(tint.getBlue() * 255.0);
        String key = System.identityHashCode(source) + ":" + tintR + ":" + tintG + ":" + tintB;
        Image cached = tintedBuildImageCache.get(key);
        if (cached != null) {
            return cached;
        }
        javafx.scene.image.PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        javafx.scene.image.WritableImage tinted = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelWriter writer = tinted.getPixelWriter();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                Color src = reader.getColor(px, py);
                double alpha = src.getOpacity();
                if (alpha <= 0.001) {
                    writer.setColor(px, py, Color.TRANSPARENT);
                    continue;
                }
                writer.setColor(px, py, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha));
            }
        }
        tintedBuildImageCache.put(key, tinted);
        return tinted;
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

    private record LightRenderProfile(double innerRadiusScale,
                                      double innerMidAlphaScale,
                                      Color innerColor,
                                      double outerRadiusScale,
                                      double outerMidAlphaScale,
                                      Color outerColor) {
        private LightRenderProfile scale(double factor) {
            return new LightRenderProfile(
                    innerRadiusScale,
                    innerMidAlphaScale,
                    scaleColor(innerColor, factor),
                    outerRadiusScale,
                    outerMidAlphaScale,
                    scaleColor(outerColor, factor)
            );
        }

        private static Color scaleColor(Color color, double factor) {
            if (color == null) {
                return Color.TRANSPARENT;
            }
            return Color.color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    Math.max(0.0, Math.min(1.0, color.getOpacity() * factor))
            );
        }
    }
}

