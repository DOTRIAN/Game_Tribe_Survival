package ui;

import build.AssetManager;
import buildsystem.core.BuildManager;
import core.GameState;
import entity.Enemy;
import entity.Player;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * UIManager:
 * - Dieu phoi toan bo JavaFX overlay cua game.
 * - Bao gom menu, nhap ten, HUD, minimap, hotbar, shop, inventory va settings.
 */
public class UIManager {
    private final AssetManager assetManager;
    private final MainMenuScreen mainMenuScreen;
    private final NameInputScreen nameInputScreen;
    private final SettingsScreen settingsScreen;
    private final HudOverlay hudOverlay;
    private final ResourcePanel resourcePanel;
    private final MinimapOverlay minimapOverlay;
    private final HotbarOverlay hotbarOverlay;
    private final HBox hotbarContainer;
    private final ShopOverlay shopOverlay;
    private final InventoryOverlay inventoryOverlay;
    private final StackPane guideOverlay;
    private final StackPane pauseOverlay;
    private final StackPane gameOverOverlay;
    private final StackPane victoryOverlay;
    private final Label toastLabel;
    private final HBox toastContainer;
    private final Map<String, ItemUiMeta> itemMetaMap;
    private final List<ItemUiMeta> shopItems;
    private final GameSettings settings;

    public UIManager(Stage stage, Scene scene, AnchorPane root, AssetManager assetManager, GameSettings settings) {
        this.assetManager = assetManager;
        this.settings = settings;
        this.itemMetaMap = createItemMetaMap(assetManager);
        this.shopItems = List.of(
                itemMetaMap.get("stone_wall"),
                itemMetaMap.get("wood_wall"),
                itemMetaMap.get("potion"),
                itemMetaMap.get("torch"),
                itemMetaMap.get("basic_sword"),
                itemMetaMap.get("pickaxe"),
                itemMetaMap.get("carrot")
        );

        this.mainMenuScreen = new MainMenuScreen();
        this.nameInputScreen = new NameInputScreen();
        this.settingsScreen = new SettingsScreen();
        this.hudOverlay = new HudOverlay();
        this.resourcePanel = new ResourcePanel();
        this.minimapOverlay = new MinimapOverlay();
        this.hotbarOverlay = new HotbarOverlay();
        this.hotbarContainer = new HBox(hotbarOverlay);
        this.shopOverlay = new ShopOverlay();
        this.inventoryOverlay = new InventoryOverlay();
        this.guideOverlay = buildSimpleOverlay("Guide", "WASD move\nSPACE run\nB shop\nI inventory\nM minimap\nQ rotate wall\nESC close overlay");
        this.pauseOverlay = buildSimpleOverlay("Paused", "Press P to resume\nESC returns to menu");
        this.gameOverOverlay = buildSimpleOverlay("Game Over", "Press R to restart");
        this.victoryOverlay = buildSimpleOverlay("Victory", "ENTER start new world\nESC save and back to menu");
        this.toastLabel = new Label("");
        this.toastContainer = new HBox(toastLabel);

        toastLabel.getStyleClass().add("toast-label");
        toastLabel.setVisible(false);
        toastContainer.setAlignment(Pos.CENTER);
        toastContainer.setVisible(false);
        toastContainer.setMouseTransparent(true);

        root.getChildren().addAll(
                mainMenuScreen,
                nameInputScreen,
                settingsScreen,
                hudOverlay,
                resourcePanel,
                minimapOverlay,
                hotbarContainer,
                shopOverlay,
                inventoryOverlay,
                guideOverlay,
                pauseOverlay,
                gameOverOverlay,
                victoryOverlay,
                toastContainer
        );
        root.setPickOnBounds(false);
        nameInputScreen.setVisible(false);
        settingsScreen.setVisible(false);
        hudOverlay.setVisible(false);
        resourcePanel.setVisible(false);
        minimapOverlay.setVisible(false);
        hotbarContainer.setVisible(false);
        shopOverlay.setVisible(false);
        inventoryOverlay.setVisible(false);
        guideOverlay.setVisible(false);
        pauseOverlay.setVisible(false);
        gameOverOverlay.setVisible(false);
        victoryOverlay.setVisible(false);
        toastContainer.setVisible(false);

        hotbarContainer.setAlignment(Pos.CENTER);
        hotbarContainer.setMouseTransparent(false);

        ResponsiveLayoutManager.anchorTopLeft(hudOverlay, 20, 20);
        ResponsiveLayoutManager.anchorTopRight(resourcePanel, 20, 20);
        ResponsiveLayoutManager.anchorBottomRight(minimapOverlay, 20, 20);
        ResponsiveLayoutManager.anchorBottomCenter(hotbarContainer, 20);
        AnchorPane.setLeftAnchor(hotbarContainer, 0.0);
        AnchorPane.setRightAnchor(hotbarContainer, 0.0);
        ResponsiveLayoutManager.anchorBottomCenter(toastContainer, 88);
        AnchorPane.setLeftAnchor(toastContainer, 0.0);
        AnchorPane.setRightAnchor(toastContainer, 0.0);
        bindOverlayToRoot(root, mainMenuScreen);
        bindOverlayToRoot(root, nameInputScreen);
        bindOverlayToRoot(root, settingsScreen);
        bindOverlayToRoot(root, shopOverlay);
        bindOverlayToRoot(root, inventoryOverlay);
        bindOverlayToRoot(root, guideOverlay);
        bindOverlayToRoot(root, pauseOverlay);
        bindOverlayToRoot(root, gameOverOverlay);
        bindOverlayToRoot(root, victoryOverlay);

        String cssUri = resolveCssUri();
        if (cssUri != null && !scene.getStylesheets().contains(cssUri)) {
            scene.getStylesheets().add(cssUri);
        }

        settingsScreen.applySettings(settings);
        stage.setFullScreen(settings.isFullscreen());
    }

    public void configureMenuActions(Runnable onPlay, Runnable onContinue, Runnable onGuide, Runnable onSettings, Runnable onExit) {
        mainMenuScreen.getPlayButton().setOnAction(event -> onPlay.run());
        mainMenuScreen.getContinueButton().setOnAction(event -> onContinue.run());
        mainMenuScreen.getGuideButton().setOnAction(event -> onGuide.run());
        mainMenuScreen.getSettingsButton().setOnAction(event -> onSettings.run());
        mainMenuScreen.getExitButton().setOnAction(event -> onExit.run());
    }

    private String resolveCssUri() {
        URL classpathCss = UIManager.class.getClassLoader().getResource("styles/game-ui.css");
        if (classpathCss != null) {
            return classpathCss.toExternalForm();
        }

        Path localCss = Path.of("resources", "styles", "game-ui.css");
        if (Files.exists(localCss)) {
            return localCss.toUri().toString();
        }

        return null;
    }

    public void configureNameActions(Runnable onConfirm, Runnable onBack) {
        nameInputScreen.getConfirmButton().setOnAction(event -> onConfirm.run());
        nameInputScreen.getBackButton().setOnAction(event -> onBack.run());
    }

    public void configureSettingsAction(Runnable onBack) {
        settingsScreen.getBackButton().setOnAction(event -> onBack.run());
    }

    public void configureHotbarAction(IntConsumer onSelect) {
        hotbarOverlay.setSelectionListener(onSelect);
    }

    public void configureShopAction(Consumer<String> onBuy) {
        shopOverlay.setBuyListener(onBuy);
        hudOverlay.getShopButton().setOnAction(event -> setShopVisible(true));
        shopOverlay.getCloseButton().setOnAction(event -> setShopVisible(false));
    }

    public void configureInventoryClose(Runnable onClose) {
        inventoryOverlay.getCloseButton().setOnAction(event -> onClose.run());
    }

    public void setContinueEnabled(boolean enabled) {
        mainMenuScreen.setContinueEnabled(enabled);
    }

    public void setNameDraft(String draftName, int maxLength) {
        nameInputScreen.setDraftName(draftName, maxLength);
        nameInputScreen.updateLength(draftName == null ? 0 : draftName.length(), maxLength);
    }

    public String getEnteredName() {
        return nameInputScreen.getEnteredName();
    }

    public void updateNameLength(int currentLength, int maxLength) {
        nameInputScreen.updateLength(currentLength, maxLength);
    }

    public void showNameError(String message) {
        nameInputScreen.showError(message);
    }

    /**
     * updateHud:
     * - Dong bo overlay gameplay voi state hien tai.
     * - Input: player, inventory, world viewport va enemy list.
     */
    public void updateHud(Player player,
                          Map<String, Integer> inventorySnapshot,
                          int selectedHotbarIndex,
                          BuildManager buildManager,
                          double worldWidth,
                          double worldHeight,
                          double cameraX,
                          double cameraY,
                          double cameraZoom,
                          double viewportWidth,
                          double viewportHeight,
                          List<Enemy> enemies) {
        hudOverlay.update(player);
        resourcePanel.updateResources(inventorySnapshot, itemMetaMap);
        hotbarOverlay.setSelectedIndex(selectedHotbarIndex);
        hotbarOverlay.update(buildManager == null ? null : buildManager.getToolbar(), assetManager);
        minimapOverlay.setVisible(settings.isMinimapVisible());
        minimapOverlay.setManaged(settings.isMinimapVisible());
        if (settings.isMinimapVisible()) {
            minimapOverlay.render(worldWidth, worldHeight, cameraX, cameraY, cameraZoom, viewportWidth, viewportHeight, player, enemies);
        }
        shopOverlay.updateShop(inventorySnapshot, shopItems);
        inventoryOverlay.updateInventory(inventorySnapshot, itemMetaMap);
    }

    public void applyGameState(GameState gameState) {
        hideAllScreens();
        boolean gameplayHudVisible = gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_COMPLETE;
        hudOverlay.setVisible(gameplayHudVisible);
        resourcePanel.setVisible(gameplayHudVisible);
        hotbarContainer.setVisible(gameplayHudVisible);
        minimapOverlay.setVisible(gameplayHudVisible && settings.isMinimapVisible());

        if (!gameplayHudVisible) {
            shopOverlay.setVisible(false);
            inventoryOverlay.setVisible(false);
        }

        switch (gameState) {
            case WELCOME -> mainMenuScreen.setVisible(true);
            case NAME_INPUT -> nameInputScreen.setVisible(true);
            case GUIDE -> guideOverlay.setVisible(true);
            case PAUSED -> pauseOverlay.setVisible(true);
            case GAME_OVER -> gameOverOverlay.setVisible(true);
            case LEVEL_COMPLETE -> victoryOverlay.setVisible(true);
            default -> {
            }
        }
    }

    public void setSettingsVisible(boolean visible) {
        settingsScreen.setVisible(visible);
    }

    public boolean isSettingsVisible() {
        return settingsScreen.isVisible();
    }

    public void setShopVisible(boolean visible) {
        shopOverlay.setVisible(visible);
    }

    public boolean isShopVisible() {
        return shopOverlay.isVisible();
    }

    public void setInventoryVisible(boolean visible) {
        inventoryOverlay.setVisible(visible);
    }

    public boolean isInventoryVisible() {
        return inventoryOverlay.isVisible();
    }

    public void toggleMinimap() {
        settings.setMinimapVisible(!settings.isMinimapVisible());
        minimapOverlay.setVisible(settings.isMinimapVisible());
        minimapOverlay.setManaged(settings.isMinimapVisible());
    }

    public boolean closeTopOverlay() {
        if (settingsScreen.isVisible()) {
            settingsScreen.setVisible(false);
            return true;
        }
        if (shopOverlay.isVisible()) {
            shopOverlay.setVisible(false);
            return true;
        }
        if (inventoryOverlay.isVisible()) {
            inventoryOverlay.setVisible(false);
            return true;
        }
        if (guideOverlay.isVisible()) {
            guideOverlay.setVisible(false);
            return true;
        }
        return false;
    }

    public boolean isBlockingOverlayVisible() {
        return settingsScreen.isVisible() || shopOverlay.isVisible() || inventoryOverlay.isVisible();
    }

    public boolean isMouseOverUi(double sceneX, double sceneY) {
        if (hudOverlay.isVisible() && hudOverlay.localToScene(hudOverlay.getBoundsInLocal()).contains(sceneX, sceneY)) {
            return true;
        }
        if (resourcePanel.isVisible() && resourcePanel.localToScene(resourcePanel.getBoundsInLocal()).contains(sceneX, sceneY)) {
            return true;
        }
        if (minimapOverlay.isVisible() && minimapOverlay.localToScene(minimapOverlay.getBoundsInLocal()).contains(sceneX, sceneY)) {
            return true;
        }
        if (hotbarContainer.isVisible() && hotbarOverlay.containsScenePoint(sceneX, sceneY)) {
            return true;
        }
        return isBlockingOverlayVisible();
    }

    public int findHotbarSlotAt(double sceneX, double sceneY) {
        return hotbarOverlay.findSlotIndexAt(sceneX, sceneY);
    }

    public void showToast(String message) {
        toastLabel.setText(message);
        boolean visible = message != null && !message.isBlank();
        toastLabel.setVisible(visible);
        toastContainer.setVisible(visible);
    }

    public void hideToast() {
        toastLabel.setText("");
        toastLabel.setVisible(false);
        toastContainer.setVisible(false);
    }

    public GameSettings getSettings() {
        settingsScreen.copyValuesInto(settings);
        return settings;
    }

    public SettingsScreen getSettingsScreen() {
        return settingsScreen;
    }

    private void hideAllScreens() {
        mainMenuScreen.setVisible(false);
        nameInputScreen.setVisible(false);
        guideOverlay.setVisible(false);
        pauseOverlay.setVisible(false);
        gameOverOverlay.setVisible(false);
        victoryOverlay.setVisible(false);
    }

    private void bindOverlayToRoot(AnchorPane root, StackPane overlay) {
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
    }

    private StackPane buildSimpleOverlay(String titleText, String bodyText) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("screen-overlay");

        javafx.scene.layout.VBox panel = new javafx.scene.layout.VBox(12);
        panel.getStyleClass().add("menu-panel");
        panel.setMaxWidth(380);
        panel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add("menu-logo");
        Label body = new Label(bodyText);
        body.getStyleClass().add("hud-value");
        body.setWrapText(true);
        body.setAlignment(Pos.CENTER);
        body.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        panel.getChildren().addAll(title, body);
        overlay.getChildren().add(panel);
        return overlay;
    }

    private Map<String, ItemUiMeta> createItemMetaMap(AssetManager assetManager) {
        Map<String, ItemUiMeta> meta = new LinkedHashMap<>();
        Image wallIcon = assetManager.getWallImage("wall_icon");
        meta.put("stone_wall", new ItemUiMeta("stone_wall", "Stone Wall", "Basic stone barrier for base defense.", 6, "SW", wallIcon));
        meta.put("wood_wall", new ItemUiMeta("wood_wall", "Wood Wall", "Cheaper wooden wall for quick builds.", 4, "WW", null));
        meta.put("potion", new ItemUiMeta("potion", "Potion", "Emergency heal during survival runs.", 12, "PT", null));
        meta.put("torch", new ItemUiMeta("torch", "Torch", "Portable light for night scouting.", 8, "TR", null));
        meta.put("basic_sword", new ItemUiMeta("basic_sword", "Basic Sword", "Starter melee weapon.", 18, "SD", null));
        meta.put("pickaxe", new ItemUiMeta("pickaxe", "Pickaxe", "Useful for mining and gathering.", 14, "PX", null));
        meta.put("coin", new ItemUiMeta("coin", "Coin", "Common shop currency.", 0, "CN", null));
        meta.put("wood", new ItemUiMeta("wood", "Wood", "Core building resource.", 0, "WD", null));
        meta.put("stone", new ItemUiMeta("stone", "Stone", "Solid building material.", 0, "ST", null));
        meta.put("fiber", new ItemUiMeta("fiber", "Fiber", "Soft crafting material.", 0, "FB", null));
        meta.put("carrot", new ItemUiMeta("carrot", "Carrot", "Simple food item.", 3, "CR", null));
        return meta;
    }
}
