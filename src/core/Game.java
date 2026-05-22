package core;

import buildsystem.core.BuildManager;
import buildsystem.core.BuildMode;
import buildsystem.core.BuildController;
import buildsystem.core.CollisionManager;
import buildsystem.core.BuildDamageResult;
import buildsystem.object.ArcherTower;
import buildsystem.object.BuildObject;
import buildsystem.sprite.AssetManager;
import entity.BaseCamp;
import entity.CollectibleDrop;
import entity.DropType;
import entity.DroppedItem;
import entity.ArrowProjectile;
import entity.BossEnemy;
import entity.Enemy;
import entity.OrcEnemy;
import entity.Player;
import entity.SkeletonEnemy;
import event.GameEvent;
import event.GameEventBus;
import event.GameEventType;
import input.InputHandler;
import inventory.Inventory;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import map.MapData;
import map.MapObjectData;
import map.TileCollisionResolver;
import map.TiledMapLoader;
import system.CollisionSystem;
import system.DamageResult;
import system.DamageSystem;
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceHitResult;
import system.resource.ResourceManager;
import system.resource.ResourceType;
import system.resource.TileResourceAdapter;
import system.save.WorldSaveService;
import ui.FloatingDamageText;
import ui.Renderer;
import world.InfiniteWorldManager;
import world.WorldChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Game:
 * - Core game loop state cho mode sinh ton vo han.
 * - Khong con choi theo man; nguoi choi sinh ton trong 1 world lien tuc.
 * - Dieu kien thang: giet boss.
 * - Dieu kien thua: player chet hoac base camp bi pha.
 * - Co save/load session de thoat game vao lai choi tiep.
 */
public class Game {
    private enum GameOverReason {
        PLAYER_DIED,
        BASE_CAMP_DESTROYED
    }
    // Zoom phai dong bo voi Renderer de tinh camera dung.
    private static final double CAMERA_ZOOM = 1.5;

    // Cac moc spawn theo gameplay sinh ton.
    private static final long ENEMY_SPAWN_INTERVAL_NS = 600_000_000L;
    private static final long AUTOSAVE_INTERVAL_NS = 20_000_000_000L;
    private static final int BOSS_SPAWN_DAY = 6;

    // World save file cho mode sinh ton.
    private static final String SURVIVAL_SAVE_FILE = "data/survival_world.json";

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final AssetManager wallAssetManager;
    private final BuildManager buildManager;
    private final BuildController buildController;
    private final CollisionManager buildCollisionManager;
    private final Player player;
    private final BaseCamp baseCamp;
    private final List<Enemy> enemies;
    private BossEnemy bossEnemy;

    private final List<MapObjectData> mapCollisions;
    private final ResourceManager resourceManager;
    private final TileCollisionResolver tileCollisionResolver;
    private final DayNightCycle dayNightCycle;
    private final List<FloatingDamageText> floatingDamageTexts;
    private final List<DroppedItem> droppedItems;
    private final List<ArrowProjectile> arrowProjectiles;
    private final List<CollectibleDrop> droppedCollectibles;

    // Inventory la state gameplay chinh cho he thu thap/craft.
    private final Inventory inventory;
    private final GameEventBus eventBus;
    private final WorldSaveService worldSaveService;
    private final InfiniteWorldManager infiniteWorldManager;

    private GameState gameState;
    private MapData mapData;
    private long worldStartedAtNs;
    private long lastEnemySpawnAtNs;
    private long lastAutoSaveAtNs;
    private long lastUpdateNowNs;
    private long victoryAtNs;
    private GameOverReason gameOverReason;

    private double cameraX;
    private double cameraY;
    private double worldWidth;
    private double worldHeight;

    private final Random random;

    // Menu state de giu tuong thich UI hien tai.
    private static final int MENU_PLAY = 0;
    private static final int MENU_GUIDE = 1;
    private static final int MENU_EXIT = 2;
    private static final int MENU_COUNT = 3;
    private int menuIndex;
    private long welcomeFlashUntilNs;
    private int pendingWelcomeAction;

    // Name input state.
    private static final int MAX_PLAYER_NAME_LENGTH = 14;
    private final StringBuilder playerNameBuffer;

    // Stats energy cho loop di chuyen/sinh ton.
    private static final double MOVE_ENERGY_DRAIN_PER_SECOND = 2.2;
    private static final double IDLE_ENERGY_REGEN_PER_SECOND = 0.9;
    private static final double SKILL_F_ENERGY_COST = 3.0;
    private static final double FOOD_ENERGY_BONUS = 15.0;
    private static final long DAMAGE_TEXT_LIFETIME_NS = 650_000_000L;
    // Hotbar hien tai de mo rong dan:
    // - Slot 0: stone_wall.
    // - Cac slot khac de trong cho item build/craft sau nay.
    private static final String STONE_WALL_ITEM_ID = "stone_wall";
    private static final String WALL_ITEM_ALIAS = "wall";
    private static final String COIN_ITEM_ID = "coin";
    private static final String WOOD_WALL_ITEM_ID = "wood_wall";
    private static final String POTION_ITEM_ID = "potion";
    private static final String TORCH_ITEM_ID = "torch";
    private static final String ARCHER_TOWER_ITEM_ID = "archer_tower";
    private static final String BASIC_SWORD_ITEM_ID = "basic_sword";
    private static final String PICKAXE_ITEM_ID = "pickaxe";
    private static final String CARROT_ITEM_ID = "carrot";
    private static final int DROP_STACK_MIN = 1;
    private static final int DROP_STACK_MAX = 5;
    private static final int COIN_VALUE_PER_ITEM = 5;
    private static final int XP_VALUE_PER_ITEM = 2;
    private static final double COLLECTIBLE_SIZE = 18.0;
    private static final double DROP_MIN_RADIUS = 20.0;
    private static final double DROP_MAX_RADIUS = 60.0;
    private static final double DROP_MIN_DISTANCE = 16.0;
    private static final int DROP_POSITION_MAX_ATTEMPTS = 24;
    private static final boolean DEBUG_DROP_LOGS = false;
    private static final int STONE_WALL_PRICE = GameBalance.STONE_WALL_PRICE;
    private static final int WOOD_WALL_PRICE = GameBalance.WOOD_WALL_PRICE;
    private static final int TORCH_PRICE = GameBalance.TORCH_PRICE;
    private static final int ARCHER_TOWER_PRICE = GameBalance.ARCHER_TOWER_PRICE;

    // selectedHotbarIndex:
    // - Luu slot nguoi choi dang chon tren thanh hotbar.
    // - Tac dong gameplay: sau nay build mode va item use se dua vao slot nay.
    private int selectedHotbarIndex;
    private boolean hasLoadedSaveSnapshot;
    private boolean pendingStartFreshWorld;

    public Game(Stage stage) {
        // Constructor:
        // - Khoi tao tat ca subsystem runtime cho 1 session sinh ton.
        this.inputHandler = new InputHandler();
        this.wallAssetManager = new AssetManager();
        CollectibleDrop.preloadAssets();
        this.player = new Player(100, 100, 58, 58, 1, 100);
        this.baseCamp = new BaseCamp(0, 0, 116, 116, 500);
        this.gameLoop = new GameLoop(this);
        this.enemies = new ArrayList<>();
        this.resourceManager = new ResourceManager();
        this.dayNightCycle = new DayNightCycle();
        this.floatingDamageTexts = new ArrayList<>();
        this.droppedItems = new ArrayList<>();
        this.arrowProjectiles = new ArrayList<>();
        this.droppedCollectibles = new ArrayList<>();
        this.inventory = new Inventory();
        this.eventBus = new GameEventBus();
        this.worldSaveService = new WorldSaveService(SURVIVAL_SAVE_FILE);
        this.random = new Random();
        this.infiniteWorldManager = new InfiniteWorldManager(20260520L);

        this.gameState = GameState.WELCOME;
        this.menuIndex = 0;
        this.welcomeFlashUntilNs = 0L;
        this.pendingWelcomeAction = -1;
        this.playerNameBuffer = new StringBuilder("Player");

        // World size tam thoi; neu load duoc map se bi ghi de bang kich thuoc map pixel that.
        this.worldWidth = 1_000_000;
        this.worldHeight = 1_000_000;
        this.worldStartedAtNs = System.nanoTime();
        this.lastEnemySpawnAtNs = 0L;
        this.lastAutoSaveAtNs = 0L;
        this.lastUpdateNowNs = -1L;
        this.victoryAtNs = -1L;
        this.gameOverReason = GameOverReason.PLAYER_DIED;
        this.selectedHotbarIndex = 0;
        this.hasLoadedSaveSnapshot = false;
        this.pendingStartFreshWorld = false;

        MapData loadedMap = tryLoadMap();
        this.mapData = loadedMap;
        if (loadedMap != null) {
            // Neu map load thanh cong, world boundary phai khop map de camera/player nam dung vi tri.
            this.worldWidth = loadedMap.getPixelWidth();
            this.worldHeight = loadedMap.getPixelHeight();
        }
        List<MapObjectData> runtimeObjects = new ArrayList<>();
        if (loadedMap != null) {
            runtimeObjects.addAll(loadedMap.getCollisionObjects());
            runtimeObjects.addAll(new TileResourceAdapter().buildResourceObjects(loadedMap));
        }
        this.mapCollisions = runtimeObjects;
        this.resourceManager.loadFromMapObjects(this.mapCollisions);
        this.tileCollisionResolver = loadedMap == null ? null : new TileCollisionResolver(loadedMap, resourceManager);
        this.buildCollisionManager = new CollisionManager(
                loadedMap,
                this.mapCollisions,
                this.tileCollisionResolver,
                this.resourceManager,
                this.worldWidth,
                this.worldHeight
        );
        this.buildCollisionManager.setDynamicEntitySupplier(() -> enemies);
        this.buildManager = new BuildManager(wallAssetManager, buildCollisionManager);
        this.buildController = new BuildController(buildManager);
        this.renderer = new Renderer(stage, inputHandler, wallAssetManager);
        this.renderer.setMapData(loadedMap);
        wireUiCallbacks();

        validateResourceContracts();
        resetWorldPosition();
        boolean loadedFromSave = applyLoadedSaveIfAny();
        this.hasLoadedSaveSnapshot = loadedFromSave;
        if (!loadedFromSave) {
            seedStartingBuildItems();
        }
        buildManager.syncToolbar(inventory.snapshot());
        setSelectedHotbarIndex(selectedHotbarIndex);
        renderer.setContinueAvailable(hasLoadedSaveSnapshot);
        renderer.setSettingsBackAction(this::closeSettingsFromUi);

        // Event system: hien tai chi log cac event quan trong de debug gameplay.
        eventBus.subscribe(event -> {
            if (event.getType() == GameEventType.WORLD_SAVED || event.getType() == GameEventType.BOSS_KILLED) {
                System.out.println("[Event] " + event.getType() + " " + event.getPayload());
            }
        });
    }

    public void start() {
        gameLoop.start();
    }

    /**
     * wireUiCallbacks:
     * - Noi button/menu/shop/hotbar JavaFX vao gameplay state that.
     * - Vi sao can: UI moi khong con ve tay tren Canvas, nen event click phai day thang vao Game.
     */
    private void wireUiCallbacks() {
        renderer.setMenuActions(
                this::openNewGameNameScreen,
                this::continueSavedWorld,
                () -> gameState = GameState.GUIDE,
                this::openSettingsFromMenu,
                Platform::exit
        );
        renderer.setNameActions(this::confirmEnteredNameAndStart, this::backToWelcomeMenu);
        renderer.setHotbarSelectionListener(this::setSelectedHotbarIndex);
        renderer.setShopUiActions(
                itemId -> {
                    inputHandler.consumeMouseLeftClick();
                    purchaseShopItem(itemId);
                },
                () -> {
                    inputHandler.consumeMouseLeftClick();
                    renderer.setShopVisible(true);
                },
                () -> {
                    inputHandler.consumeMouseLeftClick();
                    renderer.setShopVisible(false);
                }
        );
        renderer.setInventoryCloseAction(() -> {
            inputHandler.consumeMouseLeftClick();
            renderer.setInventoryVisible(false);
        });
    }

    public void update(long now) {
        if (lastUpdateNowNs < 0) {
            lastUpdateNowNs = now;
        }

        switch (gameState) {
            case WELCOME:
                handleWelcomeState(now);
                break;
            case GUIDE:
                handleGuideState();
                break;
            case NAME_INPUT:
                handleNameInputState();
                break;
            case PLAYING:
                if (handlePlayingState(now)) {
                    return;
                }
                break;
            case PAUSED:
                handlePausedState(now);
                break;
            case GAME_OVER:
                handleGameOverState();
                break;
            case LEVEL_COMPLETE:
                handleVictoryState(now);
                break;
            default:
                break;
        }

        inputHandler.update();
    }

    public void render(long now) {
        // objectiveStatus dung lai slot hien level objective de hien mission sinh ton.
        String objectiveStatus = buildSurvivalObjectiveStatus(now);

        renderer.render(
                gameState,
                player,
                enemies,
                now,
                cameraX,
                cameraY,
                menuIndex,
                now < welcomeFlashUntilNs,
                playerNameBuffer.toString(),
                MAX_PLAYER_NAME_LENGTH,
                null,
                null,
                0,
                null,
                objectiveStatus,
                null,
                resourceManager.getAllResources(),
                inventory.snapshot(),
                selectedHotbarIndex,
                inventory.getAmount(STONE_WALL_ITEM_ID),
                buildManager,
                arrowProjectiles,
                droppedItems,
                droppedCollectibles,
                floatingDamageTexts,
                dayNightCycle.getDarknessAlpha(now),
                dayNightCycle.isNight(now),
                dayNightCycle.getPhaseName(now),
                worldWidth,
                worldHeight
        );
    }

    public GameState getGameState() {
        return gameState;
    }

    public Player getPlayer() {
        return player;
    }

    private MapData tryLoadMap() {
        try {
            // Yeu cau moi: uu tien map chinh trong assets/Map_Game.
            return new TiledMapLoader().load("assets/Map_Game/map.tmx");
        } catch (Exception firstError) {
            // Giu fallback de game khong vo ngay ca khi map team dang sua.
            System.out.println("Cannot load assets/Map_Game/map.tmx: " + firstError.getMessage());
            try {
                return new TiledMapLoader().load("assets/maps/mapdemo.tmx");
            } catch (Exception secondError) {
                System.out.println("Cannot load fallback map: " + secondError.getMessage());
                return null;
            }
        }
    }

    private void validateResourceContracts() {
        List<String> errors = new ResourceContractValidator().validate(mapCollisions);
        if (errors.isEmpty()) {
            return;
        }
        System.out.println("=== Resource contract warnings ===");
        for (String error : errors) {
            System.out.println(error);
        }
    }

    private void resetWorldPosition() {
        // Dat player va base camp vao trung tam world de luong spawn nhat quan.
        // Spawn player va base camp vao trung tam world hien tai.
        // Neu world = map tiled thi day chinh la trung tam map trong assets/Map_Game.
        double centerX = worldWidth * 0.5;
        double centerY = worldHeight * 0.5;
        double[] safeSpawn = findNearestSafeSpawn(centerX - player.getWidth() * 0.5, centerY - player.getHeight() * 0.5);
        player.reset(safeSpawn[0], safeSpawn[1]);
        baseCamp.setPosition(safeSpawn[0] - 72, safeSpawn[1] - 72);
        baseCamp.clampPosition(0, 0, worldWidth, worldHeight);
        dayNightCycle.reset(System.nanoTime());
        updateCamera();
    }

    // findNearestSafeSpawn:
    // - Input: vi tri spawn mong muon.
    // - Output: vi tri khong bi block collision de player di duoc ngay.
    private double[] findNearestSafeSpawn(double preferredX, double preferredY) {
        double baseX = Math.max(0, Math.min(worldWidth - player.getWidth(), preferredX));
        double baseY = Math.max(0, Math.min(worldHeight - player.getHeight(), preferredY));
        if (!isBlockedAt(baseX, baseY)) {
            return new double[]{baseX, baseY};
        }

        double step = 32;
        int radiusSteps = 16;
        for (int r = 1; r <= radiusSteps; r++) {
            for (int ix = -r; ix <= r; ix++) {
                for (int iy = -r; iy <= r; iy++) {
                    if (Math.abs(ix) != r && Math.abs(iy) != r) {
                        continue;
                    }
                    double testX = Math.max(0, Math.min(worldWidth - player.getWidth(), baseX + ix * step));
                    double testY = Math.max(0, Math.min(worldHeight - player.getHeight(), baseY + iy * step));
                    if (!isBlockedAt(testX, testY)) {
                        return new double[]{testX, testY};
                    }
                }
            }
        }
        return new double[]{baseX, baseY};
    }

    // isBlockedAt:
    // - Input: toa do player test.
    // - Output: true neu vi tri khong hop le do va cham collision object/tile.
    private boolean isBlockedAt(double x, double y) {
        double px = x + player.getWidth() * 0.22;
        double py = y + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;

        for (MapObjectData object : mapCollisions) {
            if (!"Collision".equalsIgnoreCase(object.getType())) {
                continue;
            }
            if (object.intersects(px, py, pw, ph)) {
                return true;
            }
        }

        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(px, py, pw, ph)) {
            return true;
        }
        return buildCollisionManager.intersectsPlacedBuildObject(px, py, pw, ph, buildManager.getPlacedObjects());
    }

    private void handleWelcomeState(long now) {
        if (inputHandler.isJustPressed(KeyCode.ESCAPE) && renderer.closeTopOverlay()) {
            renderer.saveSettings();
            return;
        }
        if (renderer.isSettingsVisible()) {
            if (inputHandler.isJustPressed(KeyCode.F11)) {
                renderer.toggleFullscreen();
            }
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            openNewGameNameScreen();
        }
        if (inputHandler.isJustPressed(KeyCode.H)) {
            gameState = GameState.GUIDE;
        }
    }

    private void handleGuideState() {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
        } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            gameState = GameState.NAME_INPUT;
        }
    }

    private void handleNameInputState() {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            backToWelcomeMenu();
            return;
        }

        String enteredName = renderer.getEnteredName();
        playerNameBuffer.setLength(0);
        playerNameBuffer.append(enteredName == null ? "" : enteredName);
        renderer.updateNameLength(playerNameBuffer.length(), MAX_PLAYER_NAME_LENGTH);

        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            confirmEnteredNameAndStart();
        }
    }

    private boolean handlePlayingState(long now) {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.M)) {
            renderer.toggleMinimap();
        }
        if (inputHandler.isJustPressed(KeyCode.B)) {
            renderer.toggleShop();
        }
        if (inputHandler.isJustPressed(KeyCode.I)) {
            renderer.toggleInventory();
        }

        if (inputHandler.isJustPressed(KeyCode.ESCAPE) && renderer.closeTopOverlay()) {
            renderer.hideToast();
            inputHandler.update();
            return true;
        }

        if (buildManager.getBuildMode() == BuildMode.BUILDING && inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            buildManager.cancelBuildMode();
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            saveWorldSnapshot();
            gameState = GameState.PAUSED;
            inputHandler.update();
            return true;
        }

        // Hotbar input:
        // - Cho phep doi slot bang phim so va click truc tiep len thanh item.
        // - Vi sao can: user dang choi game sinh ton nay, khong phai demo rieng.
        boolean hotbarClickConsumed = updateHotbarSelectionInput();
        // Q rotate:
        // - M峄梚 l岷 b岷 Q se doi huong N -> E -> S -> W.
        // - Preview se cap nhat ngay sau do trong cung frame.
        if (inputHandler.isJustPressed(KeyCode.Q)) {
            buildController.onRotatePressed();
        }
        boolean mouseOverUi = renderer.isMouseOverUi(inputHandler.getMouseX(), inputHandler.getMouseY());
        boolean blockingOverlayVisible = renderer.isBlockingOverlayVisible();
        // Build preview:
        // - Chuyen mouse screen-space sang world-space qua camera/zoom.
        // - Snap ve grid de preview va wall that nam dung tren tile map.
        // - Chuot de len UI thi an preview de khong dat nham vao hotbar/minimap/HUD.
        buildController.onCursorMoved(
                inputHandler.getMouseX(),
                inputHandler.getMouseY(),
                cameraX,
                cameraY,
                CAMERA_ZOOM,
                mouseOverUi,
                player,
                inventory
        );

        // Update loop chinh:
        // 1) Xu ly input/di chuyen
        // 2) Xu ly combat, resource, AI
        // 3) Xu ly spawn/event/save
        double oldX = player.getX();
        double oldY = player.getY();

        boolean moveLeft = !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.A);
        boolean moveRight = !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.D);
        boolean moveUp = !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.W);
        boolean moveDown = !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.S);
        boolean movingByInput = moveLeft || moveRight || moveUp || moveDown;
        // SPACE + WASD => Running, con WASD thuong => Walking.
        boolean sprinting = movingByInput && inputHandler.isPressed(KeyCode.SPACE);
        player.setSprinting(sprinting);

        if (moveLeft) {
            player.moveLeft();
        }
        if (moveRight) {
            player.moveRight();
        }
        if (moveUp) {
            player.moveUp();
        }
        if (moveDown) {
            player.moveDown();
        }

        if (!blockingOverlayVisible && !hotbarClickConsumed && inputHandler.isMouseLeftJustClicked() && !mouseOverUi) {
            if (buildManager.getBuildMode() == BuildMode.BUILDING) {
                // Dat wall:
                // - Chi dat khi click tren world, khong de len UI.
                // - BuildManager se validate occupied tile/collision truoc khi tao wall.
                // - Dat thanh cong moi tru 1 stone_wall trong inventory.
                if (buildController.onPrimaryClickPlace(player, inventory)) {
                    logPlacedBuild(buildManager.getLastPlacedObject());
                    refreshBuildInventoryUi();
                }
            } else {
                performPlayerAttack(now, Player.AttackAnimationType.HIT);
            }
        } else if (!blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.F)) {
            if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
                performPlayerAttack(now, Player.AttackAnimationType.SLICE);
            }
        }

        player.clampPosition(0, 0, worldWidth, worldHeight);
        if (isPlayerCollidingWithMapCollision()) {
            player.setPosition(oldX, oldY);
        }

        boolean moving = oldX != player.getX() || oldY != player.getY();
        player.updateAnimation(now, moving, moveUp, moveDown, moveLeft, moveRight);
        updateEnergyByMovement(now, moving, sprinting);

        // Chunk map update:
        // - Moi chunk moi vao tam nhin se sinh them resource de world "vo han".
        for (WorldChunk chunk : infiniteWorldManager.updateAndGetNewChunks(player.getX(), player.getY())) {
            injectChunkResources(chunk);
        }

        resourceManager.update(now);
        updateDroppedItemPickup();
        checkCollectDroppedItems();
        cleanupExpiredDamageTexts(now);
        updateEnemySpawning(now);
        updateEnemies(now);
        updateArcherTowers(now);
        updateArrowProjectiles(now);

        if (bossEnemy != null && !bossEnemy.isAlive()) {
            victoryAtNs = now;
            gameState = GameState.LEVEL_COMPLETE;
            eventBus.publish(new GameEvent(GameEventType.BOSS_KILLED, Map.of("day", getCurrentSurvivalDay(now))));
        }

        if (!player.isAlive() || !baseCamp.isAlive()) {
            // Luu ly do thua de UI hien dung thong diep thay vi mac dinh "player chet".
            gameOverReason = !player.isAlive() ? GameOverReason.PLAYER_DIED : GameOverReason.BASE_CAMP_DESTROYED;
            gameState = GameState.GAME_OVER;
        }

        if (now - lastAutoSaveAtNs >= AUTOSAVE_INTERVAL_NS) {
            saveWorldSnapshot();
            lastAutoSaveAtNs = now;
        }

        updateCamera();
        return false;
    }

    private void handlePausedState(long now) {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.P)) {
            gameState = GameState.PLAYING;
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            saveWorldSnapshot();
            gameState = GameState.WELCOME;
        } else if (inputHandler.isJustPressed(KeyCode.F5)) {
            saveWorldSnapshot();
            lastAutoSaveAtNs = now;
        }
    }

    private void handleGameOverState() {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.R)) {
            restartSurvival();
        }
    }

    private void handleVictoryState(long now) {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        // Sau khi thang boss: Enter de tao world moi, ESC de quay menu.
        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            restartSurvival();
        }
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            saveWorldSnapshot();
            gameState = GameState.WELCOME;
        }

        // Auto-save ngay sau khi thang de luu moc chien thang.
        if (victoryAtNs > 0 && now - victoryAtNs < 1_000_000_000L) {
            saveWorldSnapshot();
            victoryAtNs = -1L;
        }
    }

    private void openNewGameNameScreen() {
        pendingStartFreshWorld = true;
        renderer.hideToast();
        renderer.setInventoryVisible(false);
        renderer.setShopVisible(false);
        gameState = GameState.NAME_INPUT;
    }

    private void continueSavedWorld() {
        if (!hasLoadedSaveSnapshot) {
            renderer.showToast("No saved world");
            return;
        }
        pendingStartFreshWorld = false;
        renderer.hideToast();
        gameState = GameState.PLAYING;
    }

    private void openSettingsFromMenu() {
        renderer.setSettingsVisible(true);
    }

    private void closeSettingsFromUi() {
        renderer.setSettingsVisible(false);
        renderer.saveSettings();
    }

    private void backToWelcomeMenu() {
        renderer.showNameError("");
        pendingStartFreshWorld = false;
        gameState = GameState.WELCOME;
    }

    /**
     * confirmEnteredNameAndStart:
     * - Validate ten nhan vat tu TextField JavaFX.
     * - Neu hop le thi vao world; PLAY tao world moi, CONTINUE giu save hien tai.
     */
    private void confirmEnteredNameAndStart() {
        String normalizedName = normalizePlayerName(renderer.getEnteredName());
        if (normalizedName == null || normalizedName.isBlank()) {
            renderer.showNameError("Name must not be empty.");
            return;
        }
        if (normalizedName.length() > MAX_PLAYER_NAME_LENGTH) {
            renderer.showNameError("Name must be at most " + MAX_PLAYER_NAME_LENGTH + " characters.");
            return;
        }

        playerNameBuffer.setLength(0);
        playerNameBuffer.append(normalizedName);
        player.setPlayerName(normalizedName);
        renderer.showNameError("");

        if (pendingStartFreshWorld) {
            restartSurvival();
            pendingStartFreshWorld = false;
            hasLoadedSaveSnapshot = true;
            renderer.setContinueAvailable(true);
            return;
        }
        gameState = GameState.PLAYING;
    }

    /**
     * purchaseShopItem:
     * - Input: itemId tu nut Buy.
     * - Output: update inventory neu du coin.
     * - Tac dong gameplay: shop tang item that, panel inventory/hotbar se tu cap nhat frame sau.
     */
    private void purchaseShopItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }

        String requestedItemId = itemId.trim().toLowerCase();
        String resolvedItemId = normalizeShopItemId(requestedItemId);
        int currentCoin = inventory.getAmount(COIN_ITEM_ID);
        int price = switch (resolvedItemId) {
            case STONE_WALL_ITEM_ID -> STONE_WALL_PRICE;
            case WOOD_WALL_ITEM_ID -> WOOD_WALL_PRICE;
            case POTION_ITEM_ID -> 12;
            case TORCH_ITEM_ID -> TORCH_PRICE;
            case ARCHER_TOWER_ITEM_ID -> ARCHER_TOWER_PRICE;
            case BASIC_SWORD_ITEM_ID -> 18;
            case PICKAXE_ITEM_ID -> 14;
            case CARROT_ITEM_ID -> 3;
            default -> -1;
        };

        if (price < 0) {
            logShopPurchase(requestedItemId, resolvedItemId, price, currentCoin, false, "unknown-item");
            renderer.showToast("Unknown item");
            return;
        }
        if (currentCoin < price) {
            logShopPurchase(requestedItemId, resolvedItemId, price, currentCoin, false, "not-enough-coin");
            renderer.showToast("Not enough coins");
            return;
        }

        if (!inventory.consumeItem(COIN_ITEM_ID, price)) {
            logShopPurchase(requestedItemId, resolvedItemId, price, currentCoin, false, "coin-consume-failed");
            renderer.showToast("Coin sync failed");
            return;
        }

        int beforeAmount = inventory.getAmount(resolvedItemId);
        inventory.addItem(resolvedItemId, 1);
        boolean inventoryAddResult = inventory.getAmount(resolvedItemId) == beforeAmount + 1;
        if (!inventoryAddResult) {
            inventory.addItem(COIN_ITEM_ID, price);
            logShopPurchase(requestedItemId, resolvedItemId, price, currentCoin, false, "inventory-add-failed");
            renderer.showToast("Inventory add failed");
            return;
        }

        refreshBuildInventoryUi();
        logShopPurchase(requestedItemId, resolvedItemId, price, currentCoin, true, "success");
        if (TORCH_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought TORCH quantity = " + inventory.getAmount(TORCH_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        } else if (ARCHER_TOWER_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought ARCHER_TOWER quantity = " + inventory.getAmount(ARCHER_TOWER_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        }
        renderer.showToast("Bought " + prettifyItemName(resolvedItemId));
    }

    private void restartSurvival() {
        // Reset world moi: clear enemy/resource procedural va inventory.
        enemies.clear();
        bossEnemy = null;
        buildManager.clearObjects();
        inventory.restore(Map.of());
        seedStartingBuildItems();
        selectedHotbarIndex = 0;
        refreshBuildInventoryUi();
        floatingDamageTexts.clear();
        droppedItems.clear();
        arrowProjectiles.clear();
        droppedCollectibles.clear();
        resourceManager.loadFromMapObjects(mapCollisions);
        // Hoi mau day cho player va nha chinh de thoat khoi vong lap GAME_OVER.
        player.setHpForLoad(player.getMaxHp());
        player.setEnergyForLoad(player.getMaxEnergy());
        baseCamp.setHpForLoad(baseCamp.getMaxHp());
        worldStartedAtNs = System.nanoTime();
        lastEnemySpawnAtNs = 0L;
        lastUpdateNowNs = -1L;
        selectedHotbarIndex = 0;
        setSelectedHotbarIndex(selectedHotbarIndex);

        resetWorldPosition();
        gameState = GameState.PLAYING;
    }

    private void updateEnemySpawning(long now) {
        if (now - lastEnemySpawnAtNs < ENEMY_SPAWN_INTERVAL_NS) {
            return;
        }

        boolean isNight = dayNightCycle.isNight(now);
        int targetOrc = isNight ? 8 : 2;
        int targetSkeleton = isNight ? 8 : 0;

        int aliveOrc = countAliveEnemyByType("ORC");
        int aliveSkeleton = countAliveEnemyByType("SKELETON");

        if (aliveOrc < targetOrc) {
            double[] spawn = randomEdgeSpawnPoint();
            enemies.add(new OrcEnemy(spawn[0], spawn[1]));
        }
        if (aliveSkeleton < targetSkeleton) {
            double[] spawn = randomEdgeSpawnPoint();
            enemies.add(new SkeletonEnemy(spawn[0], spawn[1]));
        }

        // Spawn boss 1 lan khi dat moc ngay.
        if (bossEnemy == null && getCurrentSurvivalDay(now) >= BOSS_SPAWN_DAY) {
            double[] spawn = randomEdgeSpawnPoint();
            bossEnemy = new BossEnemy(spawn[0], spawn[1]);
            enemies.add(bossEnemy);
            eventBus.publish(new GameEvent(GameEventType.BOSS_SPAWNED, Map.of("x", spawn[0], "y", spawn[1])));
        }

        lastEnemySpawnAtNs = now;
    }

    private void updateEnemies(long now) {
        List<Enemy> dead = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy == null) {
                continue;
            }
            if (!enemy.isAlive()) {
                dead.add(enemy);
                continue;
            }

            // AI quai:
            // - Enemy duoi muc tieu gan nhat giua player va base camp.
            boolean chaseCamp = distance(enemy.getCenterX(), enemy.getCenterY(), baseCamp.getCenterX(), baseCamp.getCenterY())
                    < distance(enemy.getCenterX(), enemy.getCenterY(), player.getCenterX(), player.getCenterY());
            double oldEnemyX = enemy.getX();
            double oldEnemyY = enemy.getY();
            if (chaseCamp) {
                moveEnemyToward(enemy, baseCamp);
            } else {
                enemy.update(now, player, worldWidth, worldHeight);
            }
            if (isEnemyCollidingWithPlacedWall(enemy)) {
                enemy.setPosition(oldEnemyX, oldEnemyY);
            }

            enemy.tryAttackPlayer(player, now);
            tryAttackBaseCamp(enemy, now);
        }

        if (!dead.isEmpty()) {
            enemies.removeAll(dead);
        }
    }

    private void updateArcherTowers(long now) {
        for (BuildObject object : buildManager.getPlacedObjects()) {
            if (!(object instanceof ArcherTower tower) || !tower.isAlive()) {
                continue;
            }
            Enemy target = findNearestEnemyInRange(tower.getCenterX(), tower.getCenterY(), tower.getRange());
            tower.setAttacking(target != null);
            if (target == null) {
                continue;
            }
            double dx = target.getCenterX() - tower.getCenterX();
            double dy = target.getCenterY() - tower.getCenterY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= 0.001) {
                continue;
            }
            if (!tower.canAttack(now)) {
                continue;
            }
            double speed = GameBalance.ARCHER_ARROW_SPEED;
            arrowProjectiles.add(new ArrowProjectile(
                    tower.getCenterX(),
                    tower.getCenterY(),
                    (dx / distance) * speed,
                    (dy / distance) * speed,
                    tower.getDamage(),
                    now,
                    GameBalance.ARCHER_ARROW_LIFETIME_NS,
                    "archer_arrow"
            ));
            tower.markAttack(now);
            System.out.println("ArcherTower attack enemy id " + System.identityHashCode(target));
        }
    }

    private void updateArrowProjectiles(long now) {
        if (arrowProjectiles.isEmpty()) {
            return;
        }
        List<ArrowProjectile> expired = new ArrayList<>();
        for (ArrowProjectile arrow : arrowProjectiles) {
            if (arrow == null || !arrow.isAlive()) {
                expired.add(arrow);
                continue;
            }
            arrow.update();
            if (arrow.isExpired(now)
                    || arrow.getX() < -40
                    || arrow.getY() < -40
                    || arrow.getX() > worldWidth + 40
                    || arrow.getY() > worldHeight + 40) {
                arrow.takeDamage(1);
                expired.add(arrow);
                continue;
            }
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                if (!CollisionSystem.intersects(arrow, enemy)) {
                    continue;
                }
                DamageResult result = DamageSystem.applyDamage(null, enemy, arrow.getDamage(), now);
                spawnEnemyDamageText(enemy, result, now);
                arrow.takeDamage(1);
                expired.add(arrow);
                break;
            }
        }
        if (!expired.isEmpty()) {
            arrowProjectiles.removeAll(expired);
        }
    }

    private Enemy findNearestEnemyInRange(double x, double y, double range) {
        Enemy nearest = null;
        double minDistance = range;
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            double dist = distance(x, y, enemy.getCenterX(), enemy.getCenterY());
            if (dist > range || dist >= minDistance) {
                continue;
            }
            minDistance = dist;
            nearest = enemy;
        }
        return nearest;
    }

    private void moveEnemyToward(Enemy enemy, BaseCamp target) {
        double dx = target.getCenterX() - enemy.getCenterX();
        double dy = target.getCenterY() - enemy.getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1) {
            return;
        }
        enemy.setPosition(
                enemy.getX() + (dx / distance) * enemy.getSpeed(),
                enemy.getY() + (dy / distance) * enemy.getSpeed()
        );
        enemy.clampPosition(0, 0, worldWidth, worldHeight);
    }

    private void tryAttackBaseCamp(Enemy enemy, long now) {
        if (enemy == null || !enemy.isAlive() || !baseCamp.isAlive()) {
            return;
        }
        if (!CollisionSystem.intersects(enemy, baseCamp)) {
            return;
        }

        // Damage base camp theo loai quai de tao khac biet threat.
        int damage = "BOSS".equalsIgnoreCase(enemy.getEnemyType()) ? 6 : 1;
        DamageSystem.applyDamage(enemy, baseCamp, damage);
        eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", damage, "hp", baseCamp.getHp())));
    }

    private void performPlayerAttack(long now, Player.AttackAnimationType attackType) {
        if (!player.startAttack(now, attackType)) {
            return;
        }

        int attackDamage = attackType == Player.AttackAnimationType.SLICE ? 2 : 1;
        double[] attackBox = player.buildAttackHitbox();

        boolean hitEnemy = applyAttackToFirstEnemy(attackBox[0], attackBox[1], attackBox[2], attackBox[3], attackDamage, now);
        if (hitEnemy) {
            return;
        }

        BuildDamageResult buildHitResult = buildManager.hitFirstDamageableIntersecting(
                attackBox[0],
                attackBox[1],
                attackBox[2],
                attackBox[3],
                attackDamage,
                now
        );
        if (buildHitResult != null) {
            spawnBuildDamageText(buildHitResult, now);
            if (buildHitResult.isDestroyed() && buildHitResult.getDropAmount() > 0 && !buildHitResult.getDropItemId().isBlank()) {
                spawnDroppedItem(
                        buildHitResult.getDropItemId(),
                        buildHitResult.getDropAmount(),
                        buildHitResult.getObject().getCenterX(),
                        buildHitResult.getObject().getCenterY()
                );
            }
            return;
        }

        ResourceHitResult hitResult = resourceManager.hitFirstResourceIntersecting(
                attackBox[0],
                attackBox[1],
                attackBox[2],
                attackBox[3],
                attackDamage,
                now
        );

        if (hitResult == null) {
            return;
        }
        spawnResourceDamageText(hitResult, now);

        if (!hitResult.isDestroyed() || hitResult.getDropResult() == null) {
            return;
        }
        ResourceType destroyedType = hitResult.getResourceNode() == null ? ResourceType.UNKNOWN : hitResult.getResourceNode().getResourceType();
        DropResult drop = hitResult.getDropResult();
        if (destroyedType == ResourceType.TREE || destroyedType == ResourceType.ROCK) {
            onResourceDestroyed(hitResult.getResourceNode());
        } else {
            inventory.addItem(drop.getItemId(), drop.getAmount());
            refreshBuildInventoryUi();
            if (isFoodItem(drop.getItemId())) {
                player.recoverEnergy(FOOD_ENERGY_BONUS);
            }
        }

        eventBus.publish(new GameEvent(GameEventType.RESOURCE_COLLECTED,
                Map.of("item", drop.getItemId(), "amount", drop.getAmount())));
    }

    private boolean applyAttackToFirstEnemy(double x, double y, double w, double h, int damage, long nowNs) {
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (!CollisionSystem.intersects(enemy, x, y, w, h)) {
                continue;
            }
            DamageResult result = DamageSystem.applyDamage(player, enemy, damage, nowNs);
            spawnEnemyDamageText(enemy, result, nowNs);
            return true;
        }
        return false;
    }

    private void updateEnergyByMovement(long now, boolean moving, boolean sprinting) {
        double deltaSeconds = (now - lastUpdateNowNs) / 1_000_000_000.0;
        if (deltaSeconds < 0) {
            deltaSeconds = 0;
        }
        if (deltaSeconds > 0.25) {
            deltaSeconds = 0.25;
        }
        lastUpdateNowNs = now;

        if (moving) {
            double drainRate = sprinting ? MOVE_ENERGY_DRAIN_PER_SECOND * 2.0 : MOVE_ENERGY_DRAIN_PER_SECOND;
            player.consumeEnergy(drainRate * deltaSeconds);
        } else {
            player.recoverEnergy(IDLE_ENERGY_REGEN_PER_SECOND * deltaSeconds);
        }
    }

    private void injectChunkResources(WorldChunk chunk) {
        for (WorldChunk.GeneratedResource generated : chunk.getResources()) {
            resourceManager.addGeneratedResource(
                    generated.objectId,
                    generated.kind,
                    generated.type,
                    generated.x,
                    generated.y,
                    generated.width,
                    generated.height
            );
        }
    }

    private boolean applyLoadedSaveIfAny() {
        Map<String, Object> save = worldSaveService.load();
        if (save == null) {
            return false;
        }

        // Load player/base camp/inventory state de tiep tuc session cu.
        player.setPosition(
                WorldSaveService.toDouble(save.get("playerX"), player.getX()),
                WorldSaveService.toDouble(save.get("playerY"), player.getY())
        );
        // Clamp vi tri save cu vao world hien tai de tranh camera bi "bay" ra ngoai map.
        player.clampPosition(0, 0, worldWidth, worldHeight);
        player.setHpForLoad(WorldSaveService.toInt(save.get("playerHp"), player.getMaxHp()));
        player.setEnergyForLoad(WorldSaveService.toDouble(save.get("playerEnergy"), player.getMaxEnergy()));

        baseCamp.setHpForLoad(WorldSaveService.toInt(save.get("baseCampHp"), baseCamp.getMaxHp()));
        inventory.restore(WorldSaveService.parseInventory(save.get("inventory")));
        buildManager.restoreFromSaveData(save.get("buildObjects"));
        restoreDroppedItems(save.get("droppedItems"));
        refreshBuildInventoryUi();

        long elapsedNs = WorldSaveService.toLong(save.get("elapsedNs"), 0L);
        worldStartedAtNs = System.nanoTime() - Math.max(0L, elapsedNs);

        boolean bossDefeated = WorldSaveService.toInt(save.get("bossDefeated"), 0) == 1;
        if (!bossDefeated && getCurrentSurvivalDay(System.nanoTime()) >= BOSS_SPAWN_DAY) {
            double[] spawn = randomEdgeSpawnPoint();
            bossEnemy = new BossEnemy(spawn[0], spawn[1]);
            enemies.add(bossEnemy);
        }

        eventBus.publish(new GameEvent(GameEventType.WORLD_LOADED, Map.of("file", SURVIVAL_SAVE_FILE)));
        return true;
    }

    private void saveWorldSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("playerX", player.getX());
        snapshot.put("playerY", player.getY());
        snapshot.put("playerHp", player.getHp());
        snapshot.put("playerEnergy", player.getEnergy());
        snapshot.put("baseCampHp", baseCamp.getHp());
        snapshot.put("elapsedNs", Math.max(0L, System.nanoTime() - worldStartedAtNs));
        snapshot.put("bossDefeated", (bossEnemy != null && !bossEnemy.isAlive()) ? 1 : 0);
        snapshot.put("inventory", WorldSaveService.buildInventorySnapshot(inventory));
        snapshot.put("buildObjects", buildManager.exportSaveData());
        snapshot.put("droppedItems", exportDroppedItems());

        if (worldSaveService.save(snapshot)) {
            eventBus.publish(new GameEvent(GameEventType.WORLD_SAVED, Map.of("file", SURVIVAL_SAVE_FILE)));
        }
    }

    private void updateCamera() {
        double viewWidth = renderer.getViewportWidth() / CAMERA_ZOOM;
        double viewHeight = renderer.getViewportHeight() / CAMERA_ZOOM;

        cameraX = player.getX() + player.getWidth() * 0.5 - viewWidth * 0.5;
        cameraY = player.getY() + player.getHeight() * 0.5 - viewHeight * 0.5;

        if (cameraX < 0) {
            cameraX = 0;
        }
        if (cameraY < 0) {
            cameraY = 0;
        }

        double maxCameraX = Math.max(0, worldWidth - viewWidth);
        double maxCameraY = Math.max(0, worldHeight - viewHeight);
        if (cameraX > maxCameraX) {
            cameraX = maxCameraX;
        }
        if (cameraY > maxCameraY) {
            cameraY = maxCameraY;
        }
    }

    private boolean isPlayerCollidingWithMapCollision() {
        double px = player.getX() + player.getWidth() * 0.22;
        double py = player.getY() + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;

        for (MapObjectData object : mapCollisions) {
            if (!"Collision".equalsIgnoreCase(object.getType())) {
                continue;
            }
            if (object.intersects(px, py, pw, ph)) {
                return true;
            }
        }
        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(px, py, pw, ph)) {
            return true;
        }
        return buildCollisionManager.intersectsPlacedBuildObject(px, py, pw, ph, buildManager.getPlacedObjects());
    }

    private boolean isEnemyCollidingWithPlacedWall(Enemy enemy) {
        if (enemy == null) {
            return false;
        }
        return buildCollisionManager.intersectsPlacedBuildObject(
                enemy.getX(),
                enemy.getY(),
                enemy.getWidth(),
                enemy.getHeight(),
                buildManager.getPlacedObjects()
        );
    }

    private int countAliveEnemyByType(String type) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive() && type.equalsIgnoreCase(enemy.getEnemyType())) {
                count++;
            }
        }
        return count;
    }

    private double[] randomEdgeSpawnPoint() {
        int side = random.nextInt(4);
        double margin = 12;
        double x;
        double y;

        if (side == 0) {
            x = random.nextDouble() * Math.max(1, worldWidth - 64);
            y = margin;
        } else if (side == 1) {
            x = Math.max(0, worldWidth - 64 - margin);
            y = random.nextDouble() * Math.max(1, worldHeight - 64);
        } else if (side == 2) {
            x = random.nextDouble() * Math.max(1, worldWidth - 64);
            y = Math.max(0, worldHeight - 64 - margin);
        } else {
            x = margin;
            y = random.nextDouble() * Math.max(1, worldHeight - 64);
        }
        return new double[]{x, y};
    }

    private String buildSurvivalObjectiveStatus(long now) {
        int day = getCurrentSurvivalDay(now);
        String bossStatus;
        if (bossEnemy == null) {
            bossStatus = day >= BOSS_SPAWN_DAY ? "Boss: spawning" : "Boss unlock day " + BOSS_SPAWN_DAY;
        } else {
            bossStatus = bossEnemy.isAlive() ? "Boss HP: " + bossEnemy.getHp() : "Boss defeated";
        }
        return "Day " + day
                + " | Base HP " + baseCamp.getHp() + "/" + baseCamp.getMaxHp()
                + " | " + bossStatus;
    }

    // getGameOverMessage:
    // - Output: thong diep GAME OVER theo dung nguyen nhan thua.
    public String getGameOverMessage() {
        if (gameOverReason == GameOverReason.BASE_CAMP_DESTROYED) {
            return "Base camp is destroyed.";
        }
        return "Player is dead.";
    }

    private int getCurrentSurvivalDay(long now) {
        long elapsedNs = Math.max(0L, now - worldStartedAtNs);
        long dayLengthNs = (120L + 20L + 80L + 25L) * 1_000_000_000L;
        return (int) (elapsedNs / dayLengthNs) + 1;
    }

    private int findWelcomeMenuIndexAt(double mx, double my) {
        double buttonX = 350;
        double buttonY = 246;
        double buttonW = 260;
        double buttonH = 42;
        double gap = 58;

        for (int i = 0; i < MENU_COUNT; i++) {
            double top = buttonY + i * gap;
            if (mx >= buttonX && mx <= buttonX + buttonW && my >= top && my <= top + buttonH) {
                return i;
            }
        }
        return -1;
    }

    private void updateWelcomeMenuHoverByMouse() {
        int hovered = findWelcomeMenuIndexAt(inputHandler.getMouseX(), inputHandler.getMouseY());
        if (hovered != -1 && pendingWelcomeAction == -1) {
            menuIndex = hovered;
        }
    }

    private String normalizePlayerName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() > MAX_PLAYER_NAME_LENGTH) {
            return trimmed.substring(0, MAX_PLAYER_NAME_LENGTH);
        }
        return trimmed;
    }

    private void appendTypedCharacters() {
        if (playerNameBuffer.length() >= MAX_PLAYER_NAME_LENGTH) {
            return;
        }

        String typed = inputHandler.consumeTypedCharacters();
        if (typed == null || typed.isEmpty()) {
            return;
        }

        for (int i = 0; i < typed.length(); i++) {
            if (playerNameBuffer.length() >= MAX_PLAYER_NAME_LENGTH) {
                break;
            }
            char ch = typed.charAt(i);
            if (Character.isISOControl(ch)) {
                continue;
            }
            if (Character.isLetterOrDigit(ch)) {
                playerNameBuffer.append(Character.toUpperCase(ch));
                continue;
            }
            if (ch == ' '
                    && playerNameBuffer.length() > 0
                    && playerNameBuffer.charAt(playerNameBuffer.length() - 1) != ' ') {
                playerNameBuffer.append(' ');
            }
        }
    }

    private void spawnEnemyDamageText(Enemy enemy, DamageResult result, long nowNs) {
        if (enemy == null || result == null || result.getFinalDamage() <= 0) {
            return;
        }
        floatingDamageTexts.add(new FloatingDamageText(
                "-" + result.getFinalDamage(),
                enemy.getCenterX(),
                enemy.getY() - 8,
                nowNs,
                DAMAGE_TEXT_LIFETIME_NS,
                result.isCritical()
        ));
    }

    private void spawnResourceDamageText(ResourceHitResult hitResult, long nowNs) {
        if (hitResult == null || hitResult.getDamageApplied() <= 0 || hitResult.getResourceNode() == null) {
            return;
        }
        floatingDamageTexts.add(new FloatingDamageText(
                "-" + hitResult.getDamageApplied(),
                hitResult.getResourceNode().getCenterX(),
                hitResult.getResourceNode().getY() - 6,
                nowNs,
                DAMAGE_TEXT_LIFETIME_NS,
                false
        ));
    }

    private void spawnBuildDamageText(BuildDamageResult hitResult, long nowNs) {
        if (hitResult == null || hitResult.getDamageApplied() <= 0 || hitResult.getObject() == null) {
            return;
        }
        floatingDamageTexts.add(new FloatingDamageText(
                "-" + hitResult.getDamageApplied(),
                hitResult.getObject().getCenterX(),
                hitResult.getObject().getRenderY() - 6,
                nowNs,
                DAMAGE_TEXT_LIFETIME_NS,
                false
        ));
    }

    private void cleanupExpiredDamageTexts(long nowNs) {
        floatingDamageTexts.removeIf(text -> text == null || text.isExpired(nowNs));
    }

    private boolean isFoodItem(String itemId) {
        String normalized = itemId == null ? "" : itemId.trim().toLowerCase();
        return normalized.contains("meat")
                || normalized.contains("carrot")
                || normalized.contains("vegetable")
                || normalized.contains("food");
    }

    private String prettifyItemName(String itemId) {
        return switch (itemId) {
            case STONE_WALL_ITEM_ID -> "Stone Wall";
            case WOOD_WALL_ITEM_ID -> "Wood Wall";
            case POTION_ITEM_ID -> "Potion";
            case TORCH_ITEM_ID -> "Torch";
            case ARCHER_TOWER_ITEM_ID -> "Archer Tower";
            case BASIC_SWORD_ITEM_ID -> "Basic Sword";
            case PICKAXE_ITEM_ID -> "Pickaxe";
            case COIN_ITEM_ID -> "Coin";
            case CARROT_ITEM_ID -> "Carrot";
            default -> itemId;
        };
    }

    private void spawnDroppedItem(String itemId, int amount, double centerX, double centerY) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        String spriteKey = resolveDroppedSpriteKey(itemId);
        double[] size = resolveDroppedItemSize(itemId);
        double x = centerX - size[0] / 2.0;
        double y = centerY - size[1] / 2.0;
        droppedItems.add(new DroppedItem(itemId, spriteKey, amount, x, y, size[0], size[1]));
    }

    private String resolveDroppedSpriteKey(String itemId) {
        if (TORCH_ITEM_ID.equals(itemId)) {
            return "torch_icon";
        }
        if (ARCHER_TOWER_ITEM_ID.equals(itemId)) {
            return "archer_tower_icon";
        }
        if ("stone".equalsIgnoreCase(itemId)) {
            return "";
        }
        return "wall_icon";
    }

    private double[] resolveDroppedItemSize(String itemId) {
        if (TORCH_ITEM_ID.equals(itemId)) {
            return new double[]{GameBalance.DROPPED_TORCH_WIDTH, GameBalance.DROPPED_TORCH_HEIGHT};
        }
        if (ARCHER_TOWER_ITEM_ID.equals(itemId)) {
            return new double[]{GameBalance.DROPPED_ARCHER_TOWER_WIDTH, GameBalance.DROPPED_ARCHER_TOWER_HEIGHT};
        }
        if ("stone".equalsIgnoreCase(itemId)) {
            return new double[]{GameBalance.DROPPED_STONE_WIDTH, GameBalance.DROPPED_STONE_HEIGHT};
        }
        return new double[]{GameBalance.DROPPED_ITEM_SIZE, GameBalance.DROPPED_ITEM_SIZE};
    }

    private double distance(double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // seedStartingBuildItems:
    // - World moi chi nhan coin de mua build item trong shop.
    private void seedStartingBuildItems() {
        if (inventory.getAmount(COIN_ITEM_ID) <= 0) {
            inventory.addItem(COIN_ITEM_ID, GameBalance.STARTING_COIN_AMOUNT);
        }
    }

    // updateHotbarSelectionInput:
    // - Doc phim 1..9 va click hotbar de doi slot dang chon.
    // - Input: state input frame hien tai.
    // - Output: cap nhat selectedHotbarIndex.
    // - Tac dong gameplay: chuan bi nen cho build mode va su dung item theo slot.
    private boolean updateHotbarSelectionInput() {
        if (inputHandler.isJustPressed(KeyCode.DIGIT1)) {
            setSelectedHotbarIndex(0);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT2)) {
            setSelectedHotbarIndex(1);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT3)) {
            setSelectedHotbarIndex(2);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT4)) {
            setSelectedHotbarIndex(3);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT5)) {
            setSelectedHotbarIndex(4);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT6)) {
            setSelectedHotbarIndex(5);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT7)) {
            setSelectedHotbarIndex(6);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT8)) {
            setSelectedHotbarIndex(7);
        } else if (inputHandler.isJustPressed(KeyCode.DIGIT9)) {
            setSelectedHotbarIndex(8);
        }

        if (!inputHandler.isMouseLeftJustClicked()) {
            return false;
        }

        int clickedSlot = renderer.findHotbarSlotAt(inputHandler.getMouseX(), inputHandler.getMouseY());
        if (clickedSlot >= 0) {
            setSelectedHotbarIndex(clickedSlot);
            return true;
        }
        return false;
    }

    private void setSelectedHotbarIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 9) {
            return;
        }
        // Slot -> item:
        // - Hien tai chi slot 1 chua stone_wall.
        // - Chon slot nay se thong bao cho BuildManager bat BUILD_WALL_MODE.
        // - Cac slot khac de null de game thoat khoi che do xay.
        selectedHotbarIndex = slotIndex;
        buildController.onToolbarSlotSelected(slotIndex, inventory);
    }

    private void updateDroppedItemPickup() {
        if (droppedItems.isEmpty()) {
            return;
        }
        boolean pickedAny = false;
        double px = player.getX() + player.getWidth() * 0.22;
        double py = player.getY() + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;
        List<DroppedItem> picked = new ArrayList<>();
        for (DroppedItem droppedItem : droppedItems) {
            if (droppedItem == null) {
                continue;
            }
            if (CollisionSystem.intersects(px, py, pw, ph, droppedItem.getX(), droppedItem.getY(), droppedItem.getWidth(), droppedItem.getHeight())) {
                inventory.addItem(droppedItem.getItemId(), droppedItem.getAmount());
                picked.add(droppedItem);
                pickedAny = true;
            }
        }
        if (!picked.isEmpty()) {
            droppedItems.removeAll(picked);
        }
        if (pickedAny) {
            refreshBuildInventoryUi();
        }
    }

    private void refreshBuildInventoryUi() {
        buildManager.syncToolbar(inventory.snapshot());
        setSelectedHotbarIndex(Math.max(0, Math.min(selectedHotbarIndex, 8)));
    }

    private void onResourceDestroyed(system.resource.ResourceNode resource) {
        if (resource == null) {
            return;
        }
        if (resource.getResourceType() != ResourceType.TREE && resource.getResourceType() != ResourceType.ROCK) {
            return;
        }
        if (DEBUG_DROP_LOGS) {
            System.out.println("Resource destroyed at: " + resource.getCenterX() + ", " + resource.getCenterY());
        }
        spawnDrops(resource.getCenterX(), resource.getCenterY());
    }

    private void spawnDrops(double x, double y) {
        int goldCount = DROP_STACK_MIN + random.nextInt(DROP_STACK_MAX - DROP_STACK_MIN + 1);
        int xpCount = DROP_STACK_MIN + random.nextInt(DROP_STACK_MAX - DROP_STACK_MIN + 1);
        List<Point2D> usedPositions = new ArrayList<>();

        for (int i = 0; i < goldCount; i++) {
            Point2D pos = getNonOverlappingDropPosition(x, y, usedPositions);
            spawnDropItem(DropType.COIN, pos.getX(), pos.getY());
            usedPositions.add(pos);
        }
        for (int i = 0; i < xpCount; i++) {
            Point2D pos = getNonOverlappingDropPosition(x, y, usedPositions);
            spawnDropItem(DropType.XP, pos.getX(), pos.getY());
            usedPositions.add(pos);
        }
    }

    private void spawnDropItem(DropType type, double x, double y) {
        int value = type == DropType.COIN ? COIN_VALUE_PER_ITEM : XP_VALUE_PER_ITEM;
        droppedCollectibles.add(new CollectibleDrop(type, x, y, value, COLLECTIBLE_SIZE));
        if (DEBUG_DROP_LOGS) {
            System.out.println("Spawn drop: " + type + " at " + x + ", " + y);
        }
    }

    private Point2D getNonOverlappingDropPosition(double centerX, double centerY, List<Point2D> usedPositions) {
        Point2D fallback = new Point2D(
                centerX - COLLECTIBLE_SIZE * 0.5,
                centerY - COLLECTIBLE_SIZE * 0.5
        );
        if (usedPositions == null) {
            return fallback;
        }

        for (int attempt = 0; attempt < DROP_POSITION_MAX_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = DROP_MIN_RADIUS + random.nextDouble() * (DROP_MAX_RADIUS - DROP_MIN_RADIUS);
            double px = centerX + Math.cos(angle) * radius - COLLECTIBLE_SIZE * 0.5;
            double py = centerY + Math.sin(angle) * radius - COLLECTIBLE_SIZE * 0.5;
            Point2D candidate = new Point2D(px, py);

            boolean overlaps = false;
            for (Point2D used : usedPositions) {
                if (used != null && distance(candidate.getX(), candidate.getY(), used.getX(), used.getY()) < DROP_MIN_DISTANCE) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                return candidate;
            }
        }
        return fallback;
    }

    private void checkCollectDroppedItems() {
        if (droppedCollectibles.isEmpty()) {
            return;
        }
        double px = player.getX() + player.getWidth() * 0.22;
        double py = player.getY() + player.getHeight() * 0.30;
        double pw = player.getWidth() * 0.56;
        double ph = player.getHeight() * 0.62;

        List<CollectibleDrop> picked = new ArrayList<>();
        for (CollectibleDrop item : droppedCollectibles) {
            if (item == null) {
                continue;
            }
            if (!CollisionSystem.intersects(px, py, pw, ph, item.getX(), item.getY(), item.getWidth(), item.getHeight())) {
                continue;
            }
            if (item.getType() == DropType.COIN) {
                addCoin(item.getValue());
            } else {
                addXp(item.getValue());
            }
            if (DEBUG_DROP_LOGS) {
                System.out.println("Collected: " + item.getType() + " value=" + item.getValue());
            }
            picked.add(item);
        }
        if (!picked.isEmpty()) {
            droppedCollectibles.removeAll(picked);
        }
    }

    private void addCoin(int amount) {
        if (amount <= 0) {
            return;
        }
        inventory.addItem(COIN_ITEM_ID, amount);
        refreshBuildInventoryUi();
    }

    private void addXp(int amount) {
        if (amount <= 0) {
            return;
        }
        player.addExperience(amount);
    }

    private String normalizeShopItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return switch (itemId.trim().toLowerCase()) {
            case WALL_ITEM_ALIAS -> STONE_WALL_ITEM_ID;
            case "cung", "archer", "archer_tower" -> ARCHER_TOWER_ITEM_ID;
            default -> itemId.trim().toLowerCase();
        };
    }

    private void logShopPurchase(String requestedItemId,
                                 String resolvedItemId,
                                 int price,
                                 int currentCoin,
                                 boolean inventoryAddResult,
                                 String reasonFailed) {
        System.out.println("[ShopDebug] itemId=" + requestedItemId
                + " resolvedItemId=" + resolvedItemId
                + " price=" + price
                + " currentCoin=" + currentCoin
                + " inventoryAddResult=" + inventoryAddResult
                + " reasonFailed=" + reasonFailed);
    }

    private void logPlacedBuild(BuildObject object) {
        if (object == null) {
            return;
        }
        int x = (int) Math.round(object.getRenderX());
        int y = (int) Math.round(object.getRenderY());
        if (object.getType() == buildsystem.core.BuildType.TORCH) {
            System.out.println("Placed TORCH at " + x + "," + y);
        } else if (object.getType() == buildsystem.core.BuildType.ARCHER_TOWER) {
            System.out.println("Placed ARCHER_TOWER at " + x + "," + y);
        }
    }

    private List<Map<String, Object>> exportDroppedItems() {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (DroppedItem droppedItem : droppedItems) {
            if (droppedItem != null) {
                snapshot.add(droppedItem.toSaveMap());
            }
        }
        return snapshot;
    }

    private void restoreDroppedItems(Object rawValue) {
        droppedItems.clear();
        if (!(rawValue instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            DroppedItem droppedItem = DroppedItem.fromSaveMap(map);
            if (droppedItem != null) {
                droppedItems.add(droppedItem);
            }
        }
    }
}

