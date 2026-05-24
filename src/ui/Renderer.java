package ui;

import buildsystem.component.LightComponent;
import buildsystem.core.BuildManager;
import buildsystem.object.BuildObject;
import buildsystem.fence.FenceEntity;
import buildsystem.object.ArcherTower;
import buildsystem.object.BombTrap;
import buildsystem.core.BuildPreview;
import buildsystem.sprite.AssetManager;
import core.GameBalance;
import core.GameState;
import entity.CollectibleDrop;
import entity.DroppedItem;
import entity.Enemy;
import entity.FriendlyArcher;
import entity.Player;
import entity.ArrowProjectile;
import entity.BaseCamp;
import entity.ThrownBomb;
import input.InputHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final AssetManager buildAssetManager;
    private final Image welcomeBackgroundImage;
    private final Image gameBackgroundImage;
    private final LabelFpsTracker fpsTracker;
    private final Map<String, Image> tintedBuildImageCache;
    private MapRenderer mapRenderer;

    public Renderer(Stage stage, InputHandler inputHandler, AssetManager buildAssetManager) {
        this.stage = stage;
        this.canvas = new Canvas();
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.graphicsContext.setImageSmoothing(false);
        this.settingsManager = new SettingsManager();
        this.settings = settingsManager.load();
        this.buildAssetManager = buildAssetManager;
        this.welcomeBackgroundImage = new Image("file:assets/backgrounds/menu_bg1.png");
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");
        this.fpsTracker = new LabelFpsTracker();
        this.tintedBuildImageCache = new LinkedHashMap<>();

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

    public void render(GameState gameState, Player player, BaseCamp baseCamp, List<Enemy> enemies, List<FriendlyArcher> friendlyArchers, long now,
                       double cameraX, double cameraY, int menuIndex, boolean welcomeFlashing,
                       String playerNameDraft, int maxNameLength,
                       List<Level> levels, PlayerProgress playerProgress, int selectedLevelIndex,
                       Level currentLevel, String objectiveStatus, LevelResult lastLevelResult,
                       List<ResourceNode> allResources, Map<String, Integer> collectedResources,
                       int selectedHotbarIndex, int stoneWallCount,
                       BuildManager buildManager,
                       List<HotbarItemStack> hotbarItems,
                       List<ArrowProjectile> arrowProjectiles,
                       List<ThrownBomb> thrownBombs,
                       List<DroppedItem> droppedItems,
                       List<ExplosionEffect> explosionEffects,
                       List<FireBombBurnZone> fireBombBurnZones,
                       List<CollectibleDrop> droppedCollectibles,
                       List<FloatingDamageText> floatingDamageTexts,
                       double cameraShakeX,
                       double cameraShakeY,
                       double screenFlashAlpha,
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
                hotbarItems,
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
        graphicsContext.setImageSmoothing(false);

        if (gameState == GameState.WELCOME || gameState == GameState.NAME_INPUT || gameState == GameState.GUIDE || uiManager.isSettingsVisible()) {
            drawBackgroundCover(welcomeBackgroundImage, viewportWidth, viewportHeight);
            graphicsContext.setFill(Color.color(0, 0, 0, welcomeFlashing ? 0.42 : 0.28));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            return;
        }

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_COMPLETE) {
            renderGameplay(player, baseCamp, enemies, friendlyArchers, now, cameraX, cameraY, currentLevel, objectiveStatus,
                    allResources, collectedResources, buildManager, arrowProjectiles, thrownBombs, droppedItems, explosionEffects, fireBombBurnZones, droppedCollectibles, floatingDamageTexts,
                    cameraShakeX, cameraShakeY, screenFlashAlpha,
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

    public void setShopUiActions(Consumer<String> onBuy, Runnable onOpen, Runnable onClose) {
        uiManager.configureShopAction(onBuy, onOpen, onClose);
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
                                List<ResourceNode> allResources,
                                Map<String, Integer> collectedResources,
                                BuildManager buildManager,
                                List<ArrowProjectile> arrowProjectiles,
                                List<ThrownBomb> thrownBombs,
                                List<DroppedItem> droppedItems,
                                List<ExplosionEffect> explosionEffects,
                                List<FireBombBurnZone> fireBombBurnZones,
                                List<CollectibleDrop> droppedCollectibles,
                                List<FloatingDamageText> floatingDamageTexts,
                                double cameraShakeX,
                                double cameraShakeY,
                                double screenFlashAlpha,
                                double darknessAlpha,
                                boolean isNight,
                                String dayNightPhase,
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
        renderCollectibleItems(droppedCollectibles, cameraX, cameraY, now);
        renderBaseCampHpBar(baseCamp, cameraX, cameraY);

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
        graphicsContext.restore();

        if (screenFlashAlpha > 0.001) {
            graphicsContext.save();
            graphicsContext.setFill(Color.color(1.0, 1.0, 1.0, Math.min(0.9, screenFlashAlpha)));
            graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
            graphicsContext.restore();
        }

        renderNightOverlayAndLights(player, buildManager, cameraX, cameraY, darknessAlpha, isNight);

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

    private List<ArcherTower> renderPlacedBuildObjects(BuildManager buildManager, double cameraX, double cameraY, long nowNs) {
        if (buildManager == null) {
            return List.of();
        }
        List<FenceEntity> fences = new ArrayList<>();
        List<ArcherTower> archerTowers = new ArrayList<>();
        for (BuildObject object : buildManager.getPlacedObjects()) {
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
            Image image = buildAssetManager.getSprite(droppedItem.getSpriteKey());
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
        double screenY = Math.round(baseCamp.getY() - cameraY - 14.0);
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

    private void renderCollectibleItems(List<CollectibleDrop> droppedCollectibles, double cameraX, double cameraY, long nowNs) {
        if (droppedCollectibles == null) {
            return;
        }
        for (CollectibleDrop collectible : droppedCollectibles) {
            if (collectible == null) {
                continue;
            }
            collectible.updateAnimation(nowNs);
            ImageView sprite = collectible.getImageView();
            if (sprite == null || sprite.getImage() == null || sprite.getImage().isError()) {
                continue;
            }
            double bobOffset = Math.sin(nowNs / 140_000_000.0 + collectible.getX() * 0.03) * 1.0;
            graphicsContext.drawImage(
                    sprite.getImage(),
                    collectible.getX() - cameraX,
                    collectible.getY() - cameraY + bobOffset,
                    collectible.getWidth(),
                    collectible.getHeight()
            );
        }
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

    private void renderNightOverlayAndLights(Player player, BuildManager buildManager, double cameraX, double cameraY, double darknessAlpha, boolean isNight) {
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

        if (buildManager != null) {
            for (BuildObject object : buildManager.getPlacedObjects()) {
                if (object == null) {
                    continue;
                }
                LightComponent lightComponent = object.getComponent(LightComponent.class);
                if (lightComponent == null) {
                    continue;
                }
                double screenX = (object.getCenterX() - cameraX) * CAMERA_ZOOM;
                double screenY = (object.getCenterY() - cameraY) * CAMERA_ZOOM;
                double radius = lightComponent.getRadius() * CAMERA_ZOOM;
                double alpha = Math.max(0.18, Math.min(0.85, lightComponent.getIntensity() * 0.72));
                drawRadialLight(screenX, screenY, radius, Color.color(1.0, 0.82, 0.46, alpha));
            }
        }

        double bonfireWorldX = 1060;
        double bonfireWorldY = 520;
        drawRadialLight((bonfireWorldX - cameraX) * CAMERA_ZOOM, (bonfireWorldY - cameraY) * CAMERA_ZOOM, 180, Color.color(1.0, 0.80, 0.40, 0.62));
        graphicsContext.restore();
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
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

