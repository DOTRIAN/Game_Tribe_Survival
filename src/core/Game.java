package core;

import buildsystem.core.BuildManager;
import buildsystem.core.BuildMode;
import buildsystem.core.BuildController;
import buildsystem.core.CollisionManager;
import buildsystem.core.BuildDamageResult;
import buildsystem.core.BuildType;
import buildsystem.fence.FenceEntity;
import buildsystem.object.ArcherTower;
import buildsystem.object.BombTrap;
import buildsystem.object.BuildObject;
import buildsystem.object.Chest;
import buildsystem.sprite.AssetManager;
import drop.AnimatedDropItem;
import drop.BombDropItem;
import drop.DropItemType;
import drop.DropManager;
import drop.DropSpec;
import drop.DroppedItem;
import entity.BaseCamp;
import entity.ArrowProjectile;
import entity.BlackGrouseSpawnManager;
import entity.Enemy;
import entity.Entity;
import entity.FriendlyArcher;
import entity.FriendlyArcherManager;
import entity.GolemEnemy;
import entity.Player;
import entity.ThrownBomb;
import entity.WallJumperEnemy;
import entity.WolfEnemy;
import entity.WolfSpawnManager;
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
import map.TileLayerData;
import map.TileCollisionResolver;
import map.TilePropertyCatalog;
import map.TiledMapLoader;
import system.CollisionSystem;
import system.DamageResult;
import system.DamageSystem;
import system.MovementSlideSystem;
import system.level.LevelManager;
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceHitResult;
import system.resource.ResourceManager;
import system.resource.ResourceType;
import system.resource.TileResourceAdapter;
import system.bomb.BombSystem;
import system.bomb.ExplosionEffect;
import system.bomb.FireBombBurnZone;
import system.save.WorldSaveService;
import ui.FloatingDamageText;
import ui.HotbarItemStack;
import ui.ItemUiMeta;
import ui.Renderer;
import world.InfiniteWorldManager;
import world.WorldChunk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Game:
 * - Core game loop state cho mode sinh ton vo han.
 * - Khong con choi theo man; nguoi choi sinh ton trong 1 world lien tuc.
 * - Dieu kien thua: player chet hoac base camp bi pha.
 * - Co save/load session de thoat game vao lai choi tiep.
 */
public class Game {
    private record TileCoord(int x, int y) {}
    private record TileBounds(int minX, int minY, int maxX, int maxY, int tileCount) {}
    private record FencePerimeter(int left, int top, int right, int bottom, int centerX, int centerY) {}

    private enum GameOverReason {
        PLAYER_DIED,
        BASE_CAMP_DESTROYED
    }
    // Zoom phai dong bo voi Renderer de tinh camera dung.
    private static final double CAMERA_ZOOM = 1.5;

    // Cac moc spawn theo gameplay sinh ton.
    private static final long ENEMY_SPAWN_INTERVAL_NS = 600_000_000L;
    private static final long WALL_JUMPER_SPAWN_MIN_INTERVAL_NS = 2_800_000_000L;
    private static final long WALL_JUMPER_SPAWN_MAX_INTERVAL_NS = 4_300_000_000L;
    private static final long GOLEM_SPAWN_MIN_INTERVAL_NS = 7_500_000_000L;
    private static final long GOLEM_SPAWN_MAX_INTERVAL_NS = 11_500_000_000L;
    private static final long AUTOSAVE_INTERVAL_NS = 20_000_000_000L;
    private static final int WALL_JUMPER_MAX_ALIVE = 5;
    private static final int GOLEM_MAX_ALIVE = 2;
    private static final BuildType[] ATTACKABLE_WALL_TYPES = {
            BuildType.FENCE,
            BuildType.WOOD_WALL,
            BuildType.STONE_WALL,
            BuildType.DOOR
    };

    // World save file cho mode sinh ton.
    private static final String SURVIVAL_SAVE_FILE = "data/survival_world.json";
    private static final int DEFAULT_BASE_CAMP_HP = 100;
    private static final int INITIAL_FENCE_PADDING_X_TILES = 10;
    private static final int INITIAL_FENCE_PADDING_Y_TILES = 8;
    private static final int INITIAL_FENCE_GATE_SIZE_TILES = 3;

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final AssetManager wallAssetManager;
    private final BuildManager buildManager;
    private final BuildController buildController;
    private final CollisionManager buildCollisionManager;
    private final BlackGrouseSpawnManager blackGrouseSpawnManager;
    private final WolfSpawnManager wolfSpawnManager;
    private final Player player;
    private final BaseCamp baseCamp;
    private final List<Enemy> enemies;
    private final FriendlyArcherManager friendlyArcherManager;

    private final List<MapObjectData> mapCollisions;
    private final ResourceManager resourceManager;
    private final TileCollisionResolver tileCollisionResolver;
    private final DayNightManager dayNightManager;
    private final EnemyWaveManager enemyWaveManager;
    private final List<FloatingDamageText> floatingDamageTexts;
    private final List<DroppedItem> droppedItems;
    private final List<ArrowProjectile> arrowProjectiles;
    private final List<ThrownBomb> thrownBombs;
    private final List<ExplosionEffect> explosionEffects;
    private final List<FireBombBurnZone> fireBombBurnZones;
    private final BombSystem bombSystem;
    private final List<HotbarItemStack> hotbarItems;

    // Inventory la state gameplay chinh cho he thu thap/craft.
    private final Inventory inventory;
    private final GameEventBus eventBus;
    private final WorldSaveService worldSaveService;
    private final InfiniteWorldManager infiniteWorldManager;

    private GameState gameState;
    private MapData mapData;
    private long worldStartedAtNs;
    private long lastEnemySpawnAtNs;
    private long nextWallJumperSpawnAtNs;
    private long nextGolemSpawnAtNs;
    private long lastAutoSaveAtNs;
    private long lastUpdateNowNs;
    private long victoryAtNs;
    private long perfLastReportNs;
    private long perfFrames;
    private long perfPreviewNs;
    private long perfCollisionNs;
    private long perfAiNs;
    private long perfRenderNs;
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
    // - Slot dau tien thuong la Wood Fence neu inventory dang co item nay.
    // - Cac slot khac de trong cho item build/craft sau nay.
    private static final String WOOD_FENCE_ITEM_ID = "wood_fence";
    private static final String WALL_ITEM_ALIAS = "wall";
    private static final String COIN_ITEM_ID = "coin";
    private static final String WOOD_WALL_ITEM_ID = "wood_wall";
    private static final String POTION_ITEM_ID = "potion";
    private static final String TORCH_ITEM_ID = "torch";
    private static final String ARCHER_TOWER_ITEM_ID = "archer_tower";
    private static final String FRIENDLY_ARCHER_ITEM_ID = "friendly_archer";
    private static final String BOMB_TRAP_ITEM_ID = "bomb_trap";
    private static final String CHEST_ITEM_ID = "chest";
    private static final String FIRE_BOMB_ITEM_ID = "fire_bomb";
    private static final String BASIC_SWORD_ITEM_ID = "basic_sword";
    private static final String PICKAXE_ITEM_ID = "pickaxe";
    private static final String AXE_ITEM_ID = "axe";
    private static final String CARROT_ITEM_ID = "carrot";
    private static final String NIKU_ITEM_ID = "niku";
    private static final String[] HOTBAR_PRIORITY = {
            WOOD_FENCE_ITEM_ID,
            TORCH_ITEM_ID,
            AXE_ITEM_ID,
            ARCHER_TOWER_ITEM_ID,
            CHEST_ITEM_ID,
            BOMB_TRAP_ITEM_ID,
            FIRE_BOMB_ITEM_ID,
            BASIC_SWORD_ITEM_ID,
            PICKAXE_ITEM_ID,
            POTION_ITEM_ID,
            CARROT_ITEM_ID,
            WOOD_WALL_ITEM_ID
    };
    private static final double DROP_MIN_RADIUS = 20.0;
    private static final double DROP_MAX_RADIUS = 60.0;
    private static final double DROP_MIN_DISTANCE = 16.0;
    private static final int DROP_POSITION_MAX_ATTEMPTS = 24;
    private static final boolean DEBUG_DROP_LOGS = false;
    private static final List<DropSpec> TREE_DROP_TABLE = List.of(
            new DropSpec(DropItemType.WOOD, 2),
            new DropSpec(DropItemType.XP, 2)
    );
    private static final List<DropSpec> ROCK_DROP_TABLE = List.of(
            new DropSpec(DropItemType.ROCK, 2),
            new DropSpec(DropItemType.XP, 2)
    );
    private static final List<DropSpec> ENEMY_DROP_TABLE = List.of(
            new DropSpec(DropItemType.GOLD, 2),
            new DropSpec(DropItemType.XP, 2)
    );
    private static final List<DropSpec> ANIMAL_DROP_TABLE = List.of(
            new DropSpec(DropItemType.NIKU, 2),
            new DropSpec(DropItemType.XP, 2)
    );
    private static final int WOOD_FENCE_PRICE = GameBalance.WOOD_FENCE_PRICE;
    private static final int WOOD_WALL_PRICE = GameBalance.WOOD_WALL_PRICE;
    private static final int TORCH_PRICE = GameBalance.TORCH_PRICE;
    private static final int ARCHER_TOWER_PRICE = GameBalance.ARCHER_TOWER_PRICE;
    private static final int FRIENDLY_ARCHER_PRICE = GameBalance.FRIENDLY_ARCHER_PRICE;
    private static final int BOMB_TRAP_PRICE = GameBalance.BOMB_TRAP_PRICE;
    private static final int FIRE_BOMB_PRICE = GameBalance.FIRE_BOMB_PRICE;
    private static final int CHEST_PRICE = GameBalance.CHEST_PRICE;
    private static final int AXE_WOOD_COST = 5;
    private static final int AXE_STONE_COST = 2;
    private static final double BOMB_TRAP_THROW_SPEED = 7.6;
    private static final double BOMB_TRAP_THROW_RANGE = 240.0;
    private static final double BOMB_TRAP_RENDER_SIZE = 18.0;
    private static final double FIRE_BOMB_THROW_SPEED = 8.2;
    private static final double FIRE_BOMB_THROW_RANGE = 260.0;
    private static final long FIRE_BOMB_FUSE_NS = 900_000_000L;
    private static final double FIRE_BOMB_RENDER_SIZE = 18.0;
    private static final double CHEST_INTERACT_RANGE = 78.0;

    // selectedHotbarIndex:
    // - Luu slot nguoi choi dang chon tren thanh hotbar.
    // - Tac dong gameplay: sau nay build mode va item use se dua vao slot nay.
    private int selectedHotbarIndex;
    private boolean hasLoadedSaveSnapshot;
    private boolean pendingStartFreshWorld;
    private long cameraShakeUntilNs;
    private long screenFlashUntilNs;
    private double screenShakeX;
    private double screenShakeY;
    private boolean suppressWorldPrimaryUntilMouseRelease;
    private boolean skipWorldPrimaryClickOnce;
    private boolean debugCollisionOverlayEnabled;
    private Chest openedChest;

    public Game(Stage stage) {
        // Constructor:
        // - Khoi tao tat ca subsystem runtime cho 1 session sinh ton.
        this.inputHandler = new InputHandler();
        this.wallAssetManager = new AssetManager();
        DropManager.preloadAll();
        this.player = new Player(100, 100, 58, 58, 4, 100);
        this.baseCamp = new BaseCamp(0, 0, 116, 116, DEFAULT_BASE_CAMP_HP);
        this.gameLoop = new GameLoop(this);
        this.enemies = new ArrayList<>();
        this.resourceManager = new ResourceManager();
        this.dayNightManager = new DayNightManager();
        this.enemyWaveManager = new EnemyWaveManager();
        this.floatingDamageTexts = new ArrayList<>();
        this.droppedItems = new ArrayList<>();
        this.arrowProjectiles = new ArrayList<>();
        this.thrownBombs = new ArrayList<>();
        this.explosionEffects = new ArrayList<>();
        this.fireBombBurnZones = new ArrayList<>();
        this.bombSystem = new BombSystem();
        this.hotbarItems = new ArrayList<>();
        this.inventory = new Inventory();
        this.eventBus = new GameEventBus();
        this.worldSaveService = new WorldSaveService(SURVIVAL_SAVE_FILE);
        this.random = new Random();
        this.friendlyArcherManager = new FriendlyArcherManager(this.random);
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
        this.nextWallJumperSpawnAtNs = 0L;
        this.nextGolemSpawnAtNs = 0L;
        this.lastAutoSaveAtNs = 0L;
        this.lastUpdateNowNs = -1L;
        this.victoryAtNs = -1L;
        this.perfLastReportNs = 0L;
        this.perfFrames = 0L;
        this.perfPreviewNs = 0L;
        this.perfCollisionNs = 0L;
        this.perfAiNs = 0L;
        this.perfRenderNs = 0L;
        this.gameOverReason = GameOverReason.PLAYER_DIED;
        this.selectedHotbarIndex = 0;
        this.hasLoadedSaveSnapshot = false;
        this.pendingStartFreshWorld = false;
        this.cameraShakeUntilNs = 0L;
        this.screenFlashUntilNs = 0L;
        this.screenShakeX = 0.0;
        this.screenShakeY = 0.0;
        this.suppressWorldPrimaryUntilMouseRelease = false;
        this.skipWorldPrimaryClickOnce = false;
        this.debugCollisionOverlayEnabled = false;
        this.openedChest = null;

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
        this.buildCollisionManager.setDynamicEntitySupplier(() -> {
            List<entity.Entity> blockingEntities = new ArrayList<>(enemies.size() + friendlyArcherManager.getArchers().size());
            blockingEntities.addAll(enemies);
            blockingEntities.addAll(friendlyArcherManager.getArchers());
            return blockingEntities;
        });
        this.buildManager = new BuildManager(wallAssetManager, buildCollisionManager);
        this.blackGrouseSpawnManager = new BlackGrouseSpawnManager(
                loadedMap,
                buildCollisionManager,
                buildManager,
                player,
                random
        );
        this.wolfSpawnManager = new WolfSpawnManager(
                buildCollisionManager,
                this::canWolfOccupy,
                new WolfEnemy.WorldQuery() {
                    @Override
                    public int getTileWidth() {
                        return buildCollisionManager.getTileWidth();
                    }

                    @Override
                    public int getTileHeight() {
                        return buildCollisionManager.getTileHeight();
                    }

                    @Override
                    public BuildObject findNearestWallToAttack(WolfEnemy enemy, double towardX, double towardY, double maxDistance) {
                        return Game.this.findNearestWolfWallToAttack(enemy, towardX, towardY, maxDistance);
                    }

                    @Override
                    public boolean damageWall(WolfEnemy enemy, BuildObject wall, long nowNs) {
                        return Game.this.damageWolfWall(enemy, wall, nowNs);
                    }

                    @Override
                    public boolean damageBase(WolfEnemy enemy, BaseCamp baseCamp, long nowNs) {
                        return Game.this.damageWolfBase(enemy, baseCamp, nowNs);
                    }
                },
                random
        );
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
        loadMapWoodFences();
        spawnAmbientBlackGrouse();
        refreshBuildInventoryUi();
        renderer.setContinueAvailable(hasLoadedSaveSnapshot);
        renderer.setSettingsBackAction(this::closeSettingsFromUi);

        // Event system: hien tai chi log cac event quan trong de debug gameplay.
        eventBus.subscribe(event -> {
            if (event.getType() == GameEventType.WORLD_SAVED) {
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
                    suppressWorldPrimaryUntilMouseRelease = true;
                    skipWorldPrimaryClickOnce = true;
                    purchaseShopItem(itemId);
                },
                () -> {
                    inputHandler.consumeMouseLeftClick();
                    suppressWorldPrimaryUntilMouseRelease = true;
                    skipWorldPrimaryClickOnce = true;
                    renderer.setShopVisible(true);
                },
                () -> {
                    inputHandler.consumeMouseLeftClick();
                    suppressWorldPrimaryUntilMouseRelease = true;
                    skipWorldPrimaryClickOnce = true;
                    renderer.setShopVisible(false);
                }
        );
        renderer.setInventoryCloseAction(() -> {
            inputHandler.consumeMouseLeftClick();
            suppressWorldPrimaryUntilMouseRelease = true;
            skipWorldPrimaryClickOnce = true;
            renderer.setInventoryVisible(false);
        });
        renderer.setChestCloseAction(() -> {
            inputHandler.consumeMouseLeftClick();
            suppressWorldPrimaryUntilMouseRelease = true;
            skipWorldPrimaryClickOnce = true;
            closeChestOverlay();
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

        long renderStartNs = System.nanoTime();
        renderer.render(
                gameState,
                player,
                baseCamp,
                enemies,
                friendlyArcherManager.getArchers(),
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
                debugCollisionOverlayEnabled,
                mapCollisions,
                resourceManager.getAllResources(),
                inventory.snapshot(),
                selectedHotbarIndex,
                inventory.getAmount(WOOD_FENCE_ITEM_ID),
                buildManager,
                hotbarItems,
                arrowProjectiles,
                thrownBombs,
                droppedItems,
                explosionEffects,
                fireBombBurnZones,
                floatingDamageTexts,
                screenShakeX,
                screenShakeY,
                screenFlashUntilNs > now ? Math.min(1.0, (screenFlashUntilNs - now) / 180_000_000.0) : 0.0,
                dayNightManager.getDarknessAlpha(now),
                dayNightManager.isNight(now),
                dayNightManager.getPhaseName(now),
                dayNightManager.getTimeIcon(now),
                dayNightManager.getTimeTitle(now),
                dayNightManager.getClockText(now),
                dayNightManager.getAnnouncement(now),
                worldWidth,
                worldHeight
        );
        recordPerfRender(System.nanoTime() - renderStartNs, now);
    }

    public GameState getGameState() {
        return gameState;
    }

    public Player getPlayer() {
        return player;
    }

    private MapData tryLoadMap() {
        try {
            // Uu tien map hien tai team dang su dung.
            return new TiledMapLoader().load("assets/Map_Game/mapdep.tmx");
        } catch (Exception firstError) {
            // Fallback theo thu tu de game van khoi dong khi map chinh dang duoc chinh sua.
            System.out.println("Cannot load assets/Map_Game/mapdep.tmx: " + firstError.getMessage());
            try {
                return new TiledMapLoader().load("assets/Map_Game/map.tmx");
            } catch (Exception legacyError) {
                System.out.println("Cannot load legacy map assets/Map_Game/map.tmx: " + legacyError.getMessage());
                try {
                    return new TiledMapLoader().load("assets/maps/mapdemo.tmx");
                } catch (Exception secondError) {
                    System.out.println("Cannot load fallback map: " + secondError.getMessage());
                    return null;
                }
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
        MapObjectData baseCampMarker = resolveBaseCampMarker();
        if (baseCampMarker != null) {
            configureBaseCampFromMarker(baseCampMarker);
            double[] safeSpawn = findPlayerSpawnNearBaseCamp(baseCampMarker);
            player.reset(safeSpawn[0], safeSpawn[1]);
        } else {
            double centerX = worldWidth * 0.5;
            double centerY = worldHeight * 0.5;
            double[] safeSpawn = findNearestSafeSpawn(centerX - player.getWidth() * 0.5, centerY - player.getHeight() * 0.5);
            player.reset(safeSpawn[0], safeSpawn[1]);
            baseCamp.configure(safeSpawn[0] - 72, safeSpawn[1] - 72, 116, 116, DEFAULT_BASE_CAMP_HP);
            baseCamp.clampPosition(0, 0, worldWidth, worldHeight);
        }
        dayNightManager.reset(System.nanoTime());
        enemyWaveManager.reset();
        updateCamera();
    }

    private void configureBaseCampFromMarker(MapObjectData marker) {
        if (marker == null) {
            return;
        }
        // Base camp HP duoc chot ve 100 de giu can bang on dinh giua cac map/session.
        baseCamp.configure(marker.getX(), marker.getY(), marker.getWidth(), marker.getHeight(), DEFAULT_BASE_CAMP_HP);
        baseCamp.clampPosition(0, 0, worldWidth, worldHeight);
    }

    private double[] findPlayerSpawnNearBaseCamp(MapObjectData marker) {
        double gap = Math.max(buildCollisionManager.getTileWidth(), buildCollisionManager.getTileHeight());
        double markerCenterX = marker.getX() + marker.getWidth() * 0.5;
        double markerCenterY = marker.getY() + marker.getHeight() * 0.5;
        double[][] candidates = {
                {markerCenterX - player.getWidth() * 0.5, marker.getY() + marker.getHeight() + gap},
                {markerCenterX - player.getWidth() * 0.5, marker.getY() - player.getHeight() - gap},
                {marker.getX() - player.getWidth() - gap, markerCenterY - player.getHeight() * 0.5},
                {marker.getX() + marker.getWidth() + gap, markerCenterY - player.getHeight() * 0.5}
        };
        for (double[] candidate : candidates) {
            double[] safeSpawn = findNearestSafeSpawn(candidate[0], candidate[1]);
            if (!CollisionSystem.intersects(
                    marker.getX(),
                    marker.getY(),
                    marker.getWidth(),
                    marker.getHeight(),
                    safeSpawn[0],
                    safeSpawn[1],
                    player.getWidth(),
                    player.getHeight())) {
                return safeSpawn;
            }
        }
        return findNearestSafeSpawn(markerCenterX - player.getWidth() * 0.5, marker.getY() + marker.getHeight() + gap);
    }

    private MapObjectData resolveBaseCampMarker() {
        MapObjectData objectMarker = findBaseCampObjectMarker();
        if (objectMarker != null) {
            return objectMarker;
        }
        return detectBaseCampTileBounds();
    }

    private MapObjectData findBaseCampObjectMarker() {
        for (MapObjectData object : mapCollisions) {
            if (object == null) {
                continue;
            }
            if (isBaseCampMarker(object.getName(), object.getType(), object.getProperties())) {
                return object;
            }
        }
        return null;
    }

    private MapObjectData detectBaseCampTileBounds() {
        if (mapData == null) {
            return null;
        }
        TilePropertyCatalog catalog = new TilePropertyCatalog(mapData);
        int tileW = Math.max(1, mapData.getTileWidth());
        int tileH = Math.max(1, mapData.getTileHeight());
        Set<TileCoord> baseCampTiles = new LinkedHashSet<>();

        for (TileLayerData layer : mapData.getTileLayers()) {
            if (layer == null) {
                continue;
            }
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    int gid = layer.getGidAt(x, y);
                    if (gid <= 0) {
                        continue;
                    }
                    if (!isBaseCampTile(catalog.getPropertiesForGid(gid))) {
                        continue;
                    }
                    baseCampTiles.add(new TileCoord(x, y));
                }
            }
        }

        TileBounds bounds = selectBaseCampBounds(baseCampTiles);
        if (bounds == null) {
            return null;
        }

        double x = bounds.minX() * tileW;
        double y = bounds.minY() * tileH;
        double width = (bounds.maxX() - bounds.minX() + 1) * tileW;
        double height = (bounds.maxY() - bounds.minY() + 1) * tileH;
        return new MapObjectData(-999, "BaseCamp", "BaseCamp", x, y, width, height, Map.of("Hp_tent", String.valueOf(DEFAULT_BASE_CAMP_HP)));
    }

    private TileBounds selectBaseCampBounds(Set<TileCoord> baseCampTiles) {
        if (baseCampTiles == null || baseCampTiles.isEmpty()) {
            return null;
        }

        Set<TileCoord> remaining = new LinkedHashSet<>(baseCampTiles);
        TileBounds bestBounds = null;
        double mapCenterX = mapData.getWidthInTiles() * 0.5;
        double mapCenterY = mapData.getHeightInTiles() * 0.5;

        while (!remaining.isEmpty()) {
            TileCoord start = remaining.iterator().next();
            TileBounds candidate = extractBaseCampClusterBounds(start, remaining);
            if (candidate == null) {
                continue;
            }
            if (bestBounds == null || isPreferredBaseCampBounds(candidate, bestBounds, mapCenterX, mapCenterY)) {
                bestBounds = candidate;
            }
        }
        return bestBounds;
    }

    private TileBounds extractBaseCampClusterBounds(TileCoord start, Set<TileCoord> remaining) {
        if (start == null || remaining == null || !remaining.remove(start)) {
            return null;
        }

        List<TileCoord> frontier = new ArrayList<>();
        frontier.add(start);
        int minX = start.x();
        int minY = start.y();
        int maxX = start.x();
        int maxY = start.y();
        int count = 0;

        for (int index = 0; index < frontier.size(); index++) {
            TileCoord current = frontier.get(index);
            count++;
            minX = Math.min(minX, current.x());
            minY = Math.min(minY, current.y());
            maxX = Math.max(maxX, current.x());
            maxY = Math.max(maxY, current.y());

            collectAdjacentBaseCampTile(new TileCoord(current.x() + 1, current.y()), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x() - 1, current.y()), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x(), current.y() + 1), remaining, frontier);
            collectAdjacentBaseCampTile(new TileCoord(current.x(), current.y() - 1), remaining, frontier);
        }

        return new TileBounds(minX, minY, maxX, maxY, count);
    }

    private void collectAdjacentBaseCampTile(TileCoord candidate, Set<TileCoord> remaining, List<TileCoord> frontier) {
        if (candidate == null || remaining == null || frontier == null) {
            return;
        }
        if (remaining.remove(candidate)) {
            frontier.add(candidate);
        }
    }

    private boolean isPreferredBaseCampBounds(TileBounds candidate,
                                              TileBounds currentBest,
                                              double mapCenterX,
                                              double mapCenterY) {
        if (candidate.tileCount() != currentBest.tileCount()) {
            return candidate.tileCount() > currentBest.tileCount();
        }
        return distanceToMapCenter(candidate, mapCenterX, mapCenterY) < distanceToMapCenter(currentBest, mapCenterX, mapCenterY);
    }

    private double distanceToMapCenter(TileBounds bounds, double mapCenterX, double mapCenterY) {
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerY = (bounds.minY() + bounds.maxY()) * 0.5;
        double dx = centerX - mapCenterX;
        double dy = centerY - mapCenterY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private boolean isBaseCampTile(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return false;
        }
        if (hasTruthyProperty(properties, "isBaseCamp", "baseCamp", "BaseCamp")) {
            return true;
        }
        return parsePositiveInt(-1, properties.get("Hp_tent")) > 0;
    }

    private boolean isBaseCampMarker(String name, String type, Map<String, String> properties) {
        if (hasTruthyProperty(properties, "isBaseCamp", "baseCamp", "BaseCamp")) {
            return true;
        }
        return isBaseCampToken(name) || isBaseCampToken(type);
    }

    private boolean isBaseCampToken(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("basecamp")
                || normalized.equals("mainhouse")
                || normalized.equals("main_house")
                || normalized.equals("main-house");
    }

    private boolean hasTruthyProperty(Map<String, String> properties, String... keys) {
        if (properties == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            String raw = properties.get(key);
            if (raw == null) {
                continue;
            }
            String normalized = raw.trim().toLowerCase();
            if (normalized.equals("true") || normalized.equals("on") || normalized.equals("1") || normalized.equals("yes")) {
                return true;
            }
        }
        return false;
    }

    private int parsePositiveInt(int fallback, String... values) {
        if (values == null) {
            return fallback;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                int parsed = Integer.parseInt(value.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // thử key tiếp theo
            }
        }
        return fallback;
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

        if (intersectsBaseCampCollision(px, py, pw, ph)) {
            return true;
        }

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
        return intersectsPlacedBuildObjectFast(px, py, pw, ph);
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
        if (inputHandler.isJustPressed(KeyCode.C)) {
            toggleCampChestOverlay(now);
        }

        if (inputHandler.isJustPressed(KeyCode.ESCAPE) && renderer.closeTopOverlay()) {
            renderer.hideToast();
            suppressWorldPrimaryUntilMouseRelease = true;
            inputHandler.update();
            return true;
        }

        if (suppressWorldPrimaryUntilMouseRelease && !inputHandler.isMouseLeftPressed()) {
            suppressWorldPrimaryUntilMouseRelease = false;
        }
        boolean skipWorldPrimaryClickThisFrame = skipWorldPrimaryClickOnce;
        if (skipWorldPrimaryClickThisFrame) {
            inputHandler.consumeMouseLeftClick();
            skipWorldPrimaryClickOnce = false;
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
        String selectedHotbarItemId = getSelectedHotbarItemId();
        if ((FIRE_BOMB_ITEM_ID.equals(selectedHotbarItemId) || BOMB_TRAP_ITEM_ID.equals(selectedHotbarItemId))
                && inputHandler.isJustPressed(KeyCode.Q)) {
            throwSelectedThrowableBomb(selectedHotbarItemId, now);
        } else if (inputHandler.isJustPressed(KeyCode.Q)) {
            buildController.onRotatePressed();
        }
        boolean mouseOverUi = renderer.isMouseOverUi(inputHandler.getMouseX(), inputHandler.getMouseY());
        boolean blockingOverlayVisible = renderer.isBlockingOverlayVisible();
        if (renderer.isChestVisible() && findNearestUsableChest(player.getCenterX(), player.getCenterY(), CHEST_INTERACT_RANGE) == null) {
            closeChestOverlay();
        }
        // Build preview:
        // - Chuyen mouse screen-space sang world-space qua camera/zoom.
        // - Snap ve grid de preview va wall that nam dung tren tile map.
        // - Chuot de len UI thi an preview de khong dat nham vao hotbar/minimap/HUD.
        long previewStartNs = System.nanoTime();
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
        recordPerfPreview(System.nanoTime() - previewStartNs);

        // Update loop chinh:
        // 1) Xu ly input/di chuyen
        // 2) Xu ly combat, resource, AI
        // 3) Xu ly spawn/event/save
        if (inputHandler.isJustPressed(KeyCode.F3)) {
            debugCollisionOverlayEnabled = !debugCollisionOverlayEnabled;
            renderer.showToast(debugCollisionOverlayEnabled ? "Collision debug ON" : "Collision debug OFF");
        }
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

        double playerDx = 0.0;
        double playerDy = 0.0;
        double playerStep = player.getCurrentMoveSpeed();
        if (moveLeft) {
            playerDx -= playerStep;
        }
        if (moveRight) {
            playerDx += playerStep;
        }
        if (moveUp) {
            playerDy -= playerStep;
        }
        if (moveDown) {
            playerDy += playerStep;
        }
        long collisionStartNs = System.nanoTime();
        movePlayerWithSliding(playerDx, playerDy);
        recordPerfCollision(System.nanoTime() - collisionStartNs);

        if (!suppressWorldPrimaryUntilMouseRelease
                && !skipWorldPrimaryClickThisFrame
                && !blockingOverlayVisible
                && !hotbarClickConsumed
                && inputHandler.isMouseLeftJustClicked()
                && !mouseOverUi) {
            String selectedItemId = getSelectedHotbarItemId();
            if (buildManager.getBuildMode() == BuildMode.BUILDING) {
                // Dat wall:
                // - Chi dat khi click tren world, khong de len UI.
                // - BuildManager se validate occupied tile/collision truoc khi tao wall.
                // - Dat thanh cong moi tru 1 Wood Fence trong inventory.
                if (buildController.onPrimaryClickPlace(player, inventory)) {
                    logPlacedBuild(buildManager.getLastPlacedObject());
                }
            } else {
                performPlayerAttack(now, player.getAttackAnimationTypeForCurrentMode());
            }
        } else if (!blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.F)) {
            if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
                performPlayerAttack(now, player.getAttackAnimationTypeForCurrentMode());
            }
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
        cleanupExpiredDamageTexts(now);
        cleanupExpiredExplosionEffects(now);
        updateScreenImpulse(now);
        long aiStartNs = System.nanoTime();
        updateEnemySpawning(now);
        updateEnemies(now);
        updateFriendlyArchers(now);
        updateArcherTowers(now);
        updateBombTraps(now);
        recordPerfAi(System.nanoTime() - aiStartNs);
        updateThrownBombs(now);
        updateFireBombBurnZones(now);
        updateArrowProjectiles(now);

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
        // Neu co trigger chien thang khac trong tuong lai: Enter tao world moi, ESC quay menu.
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

    private void recordPerfPreview(long elapsedNs) {
        if (debugCollisionOverlayEnabled) {
            perfPreviewNs += Math.max(0L, elapsedNs);
        }
    }

    private void recordPerfCollision(long elapsedNs) {
        if (debugCollisionOverlayEnabled) {
            perfCollisionNs += Math.max(0L, elapsedNs);
        }
    }

    private void recordPerfAi(long elapsedNs) {
        if (debugCollisionOverlayEnabled) {
            perfAiNs += Math.max(0L, elapsedNs);
        }
    }

    private void recordPerfRender(long elapsedNs, long nowNs) {
        if (!debugCollisionOverlayEnabled) {
            return;
        }
        perfRenderNs += Math.max(0L, elapsedNs);
        perfFrames++;
        if (perfLastReportNs == 0L) {
            perfLastReportNs = nowNs;
            return;
        }
        if (nowNs - perfLastReportNs < 2_000_000_000L) {
            return;
        }
        double frames = Math.max(1.0, perfFrames);
        System.out.printf(
                "[Perf] preview=%.3fms collision=%.3fms ai=%.3fms render=%.3fms frames=%d%n",
                perfPreviewNs / frames / 1_000_000.0,
                perfCollisionNs / frames / 1_000_000.0,
                perfAiNs / frames / 1_000_000.0,
                perfRenderNs / frames / 1_000_000.0,
                perfFrames
        );
        perfPreviewNs = 0L;
        perfCollisionNs = 0L;
        perfAiNs = 0L;
        perfRenderNs = 0L;
        perfFrames = 0L;
        perfLastReportNs = nowNs;
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
        Map<String, Integer> purchaseCosts = resolveShopPurchaseCosts(resolvedItemId);
        if (purchaseCosts.isEmpty()) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "unknown-item");
            renderer.showToast("Unknown item");
            return;
        }

        if (!hasEnoughResources(purchaseCosts)) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "not-enough-resources");
            renderer.showToast("Not enough resources");
            return;
        }

        if (!consumePurchaseCosts(purchaseCosts)) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "consume-failed");
            renderer.showToast("Resource sync failed");
            return;
        }

        if (FRIENDLY_ARCHER_ITEM_ID.equals(resolvedItemId)) {
            boolean spawned = spawnFriendlyArcherNearPlayer();
            if (!spawned) {
                refundPurchaseCosts(purchaseCosts);
                logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "spawn-failed");
                renderer.showToast("No valid tile for Archer");
                return;
            }
            refreshBuildInventoryUi();
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, true, "spawned");
            renderer.showToast("Bought Archer");
            return;
        }
        if (CHEST_ITEM_ID.equals(resolvedItemId)) {
            if (hasAnyAliveChest() || inventory.getAmount(CHEST_ITEM_ID) > 0) {
                refundPurchaseCosts(purchaseCosts);
                logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "already-exists");
                renderer.showToast("Chest already exists");
                return;
            }
            inventory.addItem(CHEST_ITEM_ID, 1);
            refreshBuildInventoryUi();
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, true, "success");
            renderer.showToast("Bought Chest");
            return;
        }

        int beforeAmount = inventory.getAmount(resolvedItemId);
        inventory.addItem(resolvedItemId, 1);
        boolean inventoryAddResult = inventory.getAmount(resolvedItemId) == beforeAmount + 1;
        if (!inventoryAddResult) {
            refundPurchaseCosts(purchaseCosts);
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "inventory-add-failed");
            renderer.showToast("Inventory add failed");
            return;
        }

        refreshBuildInventoryUi();
        logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, true, "success");
        if (TORCH_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought TORCH quantity = " + inventory.getAmount(TORCH_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        } else if (ARCHER_TOWER_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought ARCHER_TOWER quantity = " + inventory.getAmount(ARCHER_TOWER_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        } else if (BOMB_TRAP_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought BOMB_TRAP quantity = " + inventory.getAmount(BOMB_TRAP_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        } else if (FIRE_BOMB_ITEM_ID.equals(resolvedItemId)) {
            System.out.println("Bought FIRE_BOMB quantity = " + inventory.getAmount(FIRE_BOMB_ITEM_ID) + " coin = " + inventory.getAmount(COIN_ITEM_ID));
        }
        renderer.showToast("Bought " + prettifyItemName(resolvedItemId));
    }

    private void restartSurvival() {
        clearPersistentProgressForFreshStart();

        // Reset world moi: clear enemy/resource procedural va inventory.
        enemies.clear();
        wolfSpawnManager.clear();
        friendlyArcherManager.clear();
        buildManager.clearObjects();
        inventory.restore(Map.of());
        seedStartingBuildItems();
        selectedHotbarIndex = 0;
        refreshBuildInventoryUi();
        floatingDamageTexts.clear();
        droppedItems.clear();
        arrowProjectiles.clear();
        thrownBombs.clear();
        explosionEffects.clear();
        fireBombBurnZones.clear();
        cameraShakeUntilNs = 0L;
        screenFlashUntilNs = 0L;
        screenShakeX = 0.0;
        screenShakeY = 0.0;
        resourceManager.loadFromMapObjects(mapCollisions);
        // Hoi mau day cho player va nha chinh de thoat khoi vong lap GAME_OVER.
        player.setHpForLoad(player.getMaxHp());
        player.setEnergyForLoad(player.getMaxEnergy());
        baseCamp.setHpForLoad(baseCamp.getMaxHp());
        worldStartedAtNs = System.nanoTime();
        lastEnemySpawnAtNs = 0L;
        nextWallJumperSpawnAtNs = 0L;
        nextGolemSpawnAtNs = 0L;
        lastUpdateNowNs = -1L;
        selectedHotbarIndex = 0;
        setSelectedHotbarIndex(selectedHotbarIndex);
        hasLoadedSaveSnapshot = false;
        renderer.setContinueAvailable(false);

        resetWorldPosition();
        loadMapWoodFences();
        spawnAmbientBlackGrouse();
        gameState = GameState.PLAYING;
    }

    private void clearPersistentProgressForFreshStart() {
        // New Game/Reset phai xoa save world tren disk va dua level progress ve moc ban dau.
        worldSaveService.deleteSave();
        LevelManager.getInstance().resetProgress();
    }

    private void updateEnemySpawning(long now) {
        enemyWaveManager.update(now, dayNightManager, new EnemyWaveManager.WaveActions() {
            @Override
            public void spawnDayWolves(int count) {
                Game.this.spawnWaveWolves(count);
            }

            @Override
            public void spawnNightWolves(int count) {
                Game.this.spawnWaveWolves(count);
            }

            @Override
            public void spawnGolems(int count, GolemEnemy.GolemMode mode) {
                Game.this.spawnGolemWave(count, mode);
            }

            @Override
            public void spawnWallJumpers(int count) {
                Game.this.spawnWallJumperWave(count);
            }

            @Override
            public void showWarning() {
                renderer.showToast("Màn đêm sắp xuống. Hãy dựng rào và chuẩn bị vũ khí.");
            }

            @Override
            public void showDawn() {
                renderer.showToast("Trời sắp sáng. Quái đang rút khỏi trại.");
            }

            @Override
            public void startDawnRetreat() {
                // Spawn da dung o phase DAWN. AI hien tai tiep tuc chay ra/bi despawn khi sang ngay moi.
            }

            @Override
            public void finishDawn() {
                Game.this.despawnHostileEnemiesForMorning();
                renderer.showToast("Trời đã sáng. Quái rút vào rừng.");
            }
        });
    }

    private void updateEnemies(long now) {
        wolfSpawnManager.updateAll(now, dayNightManager.isNight(now), player, baseCamp, debugCollisionOverlayEnabled, worldWidth, worldHeight);
        List<Enemy> dead = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy == null) {
                continue;
            }
            if (enemy.shouldRemoveFromWorld()) {
                handleEnemyDeathDrops(enemy);
                dead.add(enemy);
                continue;
            }
            if (enemy instanceof GolemEnemy golemEnemy) {
                golemEnemy.updateAi(now, baseCamp, player, friendlyArcherManager.getArchers(), worldWidth, worldHeight);
                int baseHpBefore = baseCamp.getHp();
                if (golemEnemy.applyAttackIfReady(now)) {
                    if (golemEnemy.getCurrentTarget() instanceof BaseCamp) {
                        int dealt = Math.max(0, baseHpBefore - baseCamp.getHp());
                        if (dealt > 0) {
                            eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", dealt, "hp", baseCamp.getHp())));
                        }
                    }
                }
                if (golemEnemy.shouldRemoveFromWorld()) {
                    dead.add(golemEnemy);
                }
                continue;
            }
            if (enemy instanceof WallJumperEnemy wallJumperEnemy) {
                wallJumperEnemy.updateTowardBase(now, baseCamp, worldWidth, worldHeight);
                int beforeHp = baseCamp.getHp();
                if (wallJumperEnemy.tryAttackBase(baseCamp, now)) {
                    int dealt = Math.max(0, beforeHp - baseCamp.getHp());
                    if (dealt > 0) {
                        eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", dealt, "hp", baseCamp.getHp())));
                    }
                }
                if (wallJumperEnemy.shouldRemoveFromWorld()) {
                    dead.add(wallJumperEnemy);
                }
                continue;
            }
            if (enemy instanceof WolfEnemy) {
                if (enemy.shouldRemoveFromWorld()) {
                    handleEnemyDeathDrops(enemy);
                    dead.add(enemy);
                }
                continue;
            }
            if (!enemy.isAlive()) {
                handleEnemyDeathDrops(enemy);
                enemy.update(now, player, worldWidth, worldHeight);
                continue;
            }
            if (!enemy.isHostile()) {
                enemy.update(now, player, worldWidth, worldHeight);
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
                moveEnemyToward(enemy, player);
            }

            enemy.tryAttackPlayer(player, now);
            tryAttackBaseCamp(enemy, now);
            FriendlyArcher ally = firstCollidingFriendlyArcher(enemy);
            if (ally != null) {
                enemy.tryAttackEntity(ally, now);
            }
            tryAttackCampChest(enemy, now);
        }

        if (!dead.isEmpty()) {
            enemies.removeAll(dead);
        }
        wolfSpawnManager.cleanupRemoved();
    }

    private void updateArcherTowers(long now) {
        for (BuildObject object : buildManager.getPlacedObjectsByType(BuildType.ARCHER_TOWER)) {
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

    private void updateFriendlyArchers(long now) {
        friendlyArcherManager.update(
                now,
                player,
                enemies,
                arrowProjectiles,
                new FriendlyArcherManager.WorldQuery() {
                    @Override
                    public boolean canOccupy(FriendlyArcher archer, double x, double y, double width, double height) {
                        return Game.this.canFriendlyArcherOccupy(archer, x, y, width, height);
                    }

                    @Override
                    public int getTileWidth() {
                        return buildCollisionManager.getTileWidth();
                    }

                    @Override
                    public int getTileHeight() {
                        return buildCollisionManager.getTileHeight();
                    }

                    @Override
                    public double getWorldWidth() {
                        return worldWidth;
                    }

                    @Override
                    public double getWorldHeight() {
                        return worldHeight;
                    }
                }
        );
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

    private void updateThrownBombs(long now) {
        if (thrownBombs.isEmpty()) {
            return;
        }
        List<ThrownBomb> exploded = new ArrayList<>();
        for (ThrownBomb bomb : thrownBombs) {
            if (bomb == null) {
                continue;
            }
            bomb.update(now);
            if (!bomb.shouldExplode()) {
                continue;
            }
            exploded.add(bomb);
            if (BOMB_TRAP_ITEM_ID.equals(bomb.getBombItemId())) {
                triggerThrownBombTrapExplosion(bomb, now);
            } else {
                triggerFireBombExplosion(bomb, now);
            }
        }
        if (!exploded.isEmpty()) {
            thrownBombs.removeAll(exploded);
        }
    }

    private void triggerFireBombExplosion(ThrownBomb bomb, long now) {
        if (bomb == null) {
            return;
        }
        fireBombBurnZones.add(new FireBombBurnZone(
                bomb.getX(),
                bomb.getY(),
                bomb.getBlastRadius(),
                GameBalance.FIRE_BOMB_BURN_DAMAGE,
                now,
                GameBalance.FIRE_BOMB_BURN_DURATION_NS,
                GameBalance.FIRE_BOMB_FADE_DURATION_NS,
                GameBalance.FIRE_BOMB_DAMAGE_TICK_NS,
                wallAssetManager.getAnimationFrames("fire_bomb_blast"),
                wallAssetManager.getAnimationFrames("fire_bomb_ember")
        ));
        cameraShakeUntilNs = Math.max(cameraShakeUntilNs, now + 180_000_000L);
        screenFlashUntilNs = Math.max(screenFlashUntilNs, now + 90_000_000L);

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (distance(bomb.getX(), bomb.getY(), enemy.getCenterX(), enemy.getCenterY()) > bomb.getBlastRadius()) {
                continue;
            }
            DamageResult damageResult = DamageSystem.applyDamage(player, enemy, GameBalance.FIRE_BOMB_IMPACT_DAMAGE, now);
            spawnEnemyDamageText(enemy, damageResult, now);
        }
    }

    private void triggerThrownBombTrapExplosion(ThrownBomb bomb, long now) {
        if (bomb == null) {
            return;
        }
        applyBombExplosion(new BombSystem.BombExplosionEvent(
                null,
                bomb.getX(),
                bomb.getY(),
                bomb.getBlastRadius(),
                bomb.getDamage()
        ), now);
    }

    private void updateFireBombBurnZones(long now) {
        if (fireBombBurnZones.isEmpty()) {
            return;
        }
        List<FireBombBurnZone> expired = new ArrayList<>();
        for (FireBombBurnZone zone : fireBombBurnZones) {
            if (zone == null) {
                continue;
            }
            if (zone.isExpired(now)) {
                expired.add(zone);
                continue;
            }
            if (!zone.shouldApplyDamage(now)) {
                continue;
            }
            int tickDamage = zone.resolveTickDamage(now);
            if (zone.contains(player.getCenterX(), player.getCenterY())) {
                DamageResult playerDamage = DamageSystem.applyDamage(null, player, tickDamage, now);
                spawnPlayerDamageText(playerDamage, now);
            }
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                if (!zone.contains(enemy.getCenterX(), enemy.getCenterY())) {
                    continue;
                }
                DamageResult damageResult = DamageSystem.applyDamage(player, enemy, tickDamage, now);
                spawnEnemyDamageText(enemy, damageResult, now);
            }
            zone.markDamageApplied(now);
        }
        if (!expired.isEmpty()) {
            fireBombBurnZones.removeAll(expired);
        }
    }

    private void updateBombTraps(long now) {
        List<BombTrap> bombs = new ArrayList<>();
        for (BuildObject object : buildManager.getPlacedObjectsByType(BuildType.BOMB_TRAP)) {
            if (object instanceof BombTrap bombTrap && bombTrap.isAlive()) {
                bombs.add(bombTrap);
            }
        }
        BombSystem.BombUpdateResult result = bombSystem.update(now, bombs);
        if (result.getExplosionEvents().isEmpty()) {
            return;
        }
        for (BombSystem.BombExplosionEvent event : result.getExplosionEvents()) {
            if (event == null) {
                continue;
            }
            applyBombExplosion(event, now);
        }
    }

    private void applyBombExplosion(BombSystem.BombExplosionEvent event, long now) {
        explosionEffects.add(new ExplosionEffect(event.getWorldX(), event.getWorldY(), event.getRadius(), now));
        cameraShakeUntilNs = Math.max(cameraShakeUntilNs, now + 260_000_000L);
        screenFlashUntilNs = Math.max(screenFlashUntilNs, now + 170_000_000L);

        if (distance(event.getWorldX(), event.getWorldY(), player.getCenterX(), player.getCenterY()) <= event.getRadius()) {
            DamageResult playerDamage = DamageSystem.applyDamage(null, player, event.getEnemyDamage(), now);
            spawnPlayerDamageText(playerDamage, now);
        }

        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (distance(event.getWorldX(), event.getWorldY(), enemy.getCenterX(), enemy.getCenterY()) > event.getRadius()) {
                continue;
            }
            DamageResult damageResult = DamageSystem.applyDamage(null, enemy, event.getEnemyDamage(), now);
            spawnEnemyDamageText(enemy, damageResult, now);
        }

        List<BuildDamageResult> buildHits = buildManager.damageObjectsInRadius(
                event.getWorldX(),
                event.getWorldY(),
                event.getRadius(),
                GameBalance.BOMB_TRAP_DAMAGE,
                now,
                object -> object != null
                        && object.getType() != buildsystem.core.BuildType.TORCH
                        && object.getType() != buildsystem.core.BuildType.BOMB_TRAP
        );
        for (BuildDamageResult buildHit : buildHits) {
            if (buildHit == null) {
                continue;
            }
            spawnBuildDamageText(buildHit, now);
            if (buildHit.isDestroyed() && buildHit.getDropAmount() > 0 && !buildHit.getDropItemId().isBlank()) {
                spawnDroppedItem(
                        buildHit.getDropItemId(),
                        buildHit.getDropAmount(),
                        buildHit.getObject().getCenterX(),
                        buildHit.getObject().getCenterY()
                );
            }
        }
        applyResourceExplosionDamage(event.getWorldX(), event.getWorldY(), event.getRadius(), event.getEnemyDamage(), now);

        BuildObject bombObject = event.getBomb();
        if (bombObject != null) {
            buildManager.destroyObject(bombObject);
        }
    }

    private void cleanupExpiredExplosionEffects(long now) {
        explosionEffects.removeIf(effect -> effect == null || !effect.isAlive(now));
    }

    private void updateScreenImpulse(long now) {
        if (now > cameraShakeUntilNs) {
            screenShakeX = 0.0;
            screenShakeY = 0.0;
            return;
        }
        double amplitude = 4.0;
        screenShakeX = (random.nextDouble() - 0.5) * 2.0 * amplitude;
        screenShakeY = (random.nextDouble() - 0.5) * 2.0 * amplitude;
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

    private void moveEnemyToward(Enemy enemy, Entity target) {
        double dx = target.getCenterX() - enemy.getCenterX();
        double dy = target.getCenterY() - enemy.getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1) {
            return;
        }
        MovementSlideSystem.MoveResult result = MovementSlideSystem.steerToward(
                enemy.getX(),
                enemy.getY(),
                enemy.getWidth(),
                enemy.getHeight(),
                (dx / distance) * enemy.getSpeed(),
                (dy / distance) * enemy.getSpeed(),
                (x, y, width, height) -> canHostileEnemyOccupy(enemy, x, y, width, height)
        );
        enemy.setPosition(result.x(), result.y());
    }

    private void tryAttackBaseCamp(Enemy enemy, long now) {
        if (enemy == null || !enemy.isAlive() || !baseCamp.isAlive()) {
            return;
        }
        if (!intersectsBaseCampCollision(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
            return;
        }

        int damage = 1;
        DamageSystem.applyDamage(enemy, baseCamp, damage);
        eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", damage, "hp", baseCamp.getHp())));
    }

    private void tryAttackCampChest(Enemy enemy, long now) {
        if (enemy == null || !enemy.isAlive() || !enemy.canAttackNow(now)) {
            return;
        }
        BuildDamageResult hitResult = buildManager.hitFirstDamageableIntersecting(
                enemy.getCollisionX(),
                enemy.getCollisionY(),
                enemy.getCollisionWidth(),
                enemy.getCollisionHeight(),
                enemy.getDamage(),
                now,
                object -> object != null && object.getType() == buildsystem.core.BuildType.CHEST
        );
        if (hitResult == null) {
            return;
        }
        enemy.markAttackNow(now);
        spawnBuildDamageText(hitResult, now);
        if (hitResult.isDestroyed()) {
            if (openedChest == hitResult.getObject()) {
                openedChest = null;
            }
            renderer.setChestVisible(false);
            renderer.showToast("Chest destroyed");
        }
    }

    private BuildObject findNearestWolfWallToAttack(WolfEnemy enemy, double towardX, double towardY, double maxDistance) {
        if (enemy == null || maxDistance <= 0.0) {
            return null;
        }
        BuildObject nearest = null;
        double bestScore = Double.POSITIVE_INFINITY;
        double targetAngle = Math.atan2(towardY - enemy.getCenterY(), towardX - enemy.getCenterX());
        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                enemy.getCenterX() - maxDistance,
                enemy.getCenterY() - maxDistance,
                maxDistance * 2.0,
                maxDistance * 2.0
        )) {
            if (object == null || !object.isAlive()) {
                continue;
            }
            if (!isAttackableWallType(object.getType())) {
                continue;
            }
            double distance = edgeDistanceBetween(enemy, object);
            if (distance > maxDistance) {
                continue;
            }
            double angle = Math.atan2(object.getCenterY() - enemy.getCenterY(), object.getCenterX() - enemy.getCenterX());
            double anglePenalty = Math.abs(normalizeAngleRadians(angle - targetAngle)) * 18.0;
            double score = distance + anglePenalty;
            if (score >= bestScore) {
                continue;
            }
            nearest = object;
            bestScore = score;
        }
        return nearest;
    }

    private boolean damageWolfWall(WolfEnemy enemy, BuildObject wall, long now) {
        if (enemy == null || wall == null || !wall.isAlive()) {
            return false;
        }
        double attackPadding = 18.0;
        BuildDamageResult hitResult = buildManager.hitFirstDamageableIntersecting(
                enemy.getCollisionX() - attackPadding,
                enemy.getCollisionY() - attackPadding,
                enemy.getCollisionWidth() + attackPadding * 2.0,
                enemy.getCollisionHeight() + attackPadding * 2.0,
                enemy.getDamage(),
                now,
                object -> object == wall
        );
        if (hitResult == null) {
            return false;
        }
        spawnBuildDamageText(hitResult, now);
        if (hitResult.isDestroyed() && hitResult.getDropAmount() > 0 && !hitResult.getDropItemId().isBlank()) {
            spawnDroppedItem(
                    hitResult.getDropItemId(),
                    hitResult.getDropAmount(),
                    hitResult.getObject().getCenterX(),
                    hitResult.getObject().getCenterY()
            );
        }
        return true;
    }

    private boolean damageWolfBase(WolfEnemy enemy, BaseCamp targetBaseCamp, long now) {
        if (enemy == null || targetBaseCamp == null || targetBaseCamp.isDead()) {
            return false;
        }
        int beforeHp = targetBaseCamp.getHp();
        DamageSystem.applyDamage(enemy, targetBaseCamp, enemy.getDamage(), now);
        int dealt = Math.max(0, beforeHp - targetBaseCamp.getHp());
        if (dealt <= 0) {
            return false;
        }
        eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", dealt, "hp", targetBaseCamp.getHp())));
        return true;
    }

    private BuildObject findNearestGolemWallToAttack(GolemEnemy enemy, double towardX, double towardY, double maxDistance) {
        if (enemy == null || maxDistance <= 0.0) {
            return null;
        }
        BuildObject nearest = null;
        double bestScore = Double.POSITIVE_INFINITY;
        double targetAngle = Math.atan2(towardY - enemy.getCenterY(), towardX - enemy.getCenterX());
        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                enemy.getCenterX() - maxDistance,
                enemy.getCenterY() - maxDistance,
                maxDistance * 2.0,
                maxDistance * 2.0
        )) {
            if (object == null || !object.isAlive()) {
                continue;
            }
            if (!isAttackableWallType(object.getType())) {
                continue;
            }
            double distance = edgeDistanceBetween(enemy, object);
            if (distance > maxDistance) {
                continue;
            }
            double angle = Math.atan2(object.getCenterY() - enemy.getCenterY(), object.getCenterX() - enemy.getCenterX());
            double anglePenalty = Math.abs(normalizeAngleRadians(angle - targetAngle)) * 20.0;
            double score = distance + anglePenalty;
            if (score >= bestScore) {
                continue;
            }
            nearest = object;
            bestScore = score;
        }
        return nearest;
    }

    private boolean isAttackableWallType(BuildType type) {
        if (type == null) {
            return false;
        }
        for (BuildType attackableType : ATTACKABLE_WALL_TYPES) {
            if (type == attackableType) {
                return true;
            }
        }
        return false;
    }

    private boolean damageGolemWall(GolemEnemy enemy, BuildObject wall, long now) {
        if (enemy == null || wall == null || !wall.isAlive()) {
            return false;
        }
        BuildDamageResult hitResult = buildManager.hitFirstDamageableIntersecting(
                enemy.getAttackHitboxX(),
                enemy.getAttackHitboxY(),
                enemy.getAttackHitboxWidth(),
                enemy.getAttackHitboxHeight(),
                enemy.getDamage(),
                now,
                object -> object == wall
        );
        if (hitResult == null) {
            return false;
        }
        spawnBuildDamageText(hitResult, now);
        if (hitResult.isDestroyed() && hitResult.getDropAmount() > 0 && !hitResult.getDropItemId().isBlank()) {
            spawnDroppedItem(
                    hitResult.getDropItemId(),
                    hitResult.getDropAmount(),
                    hitResult.getObject().getCenterX(),
                    hitResult.getObject().getCenterY()
            );
        }
        return true;
    }

    private void toggleCampChestOverlay(long now) {
        if (renderer.isChestVisible()) {
            closeChestOverlay();
            return;
        }
        Chest chest = findNearestUsableChest(player.getCenterX(), player.getCenterY(), CHEST_INTERACT_RANGE);
        if (chest == null) {
            renderer.showToast("No chest nearby");
            closeChestOverlay();
            return;
        }
        chest.openTemporarily(now);
        openedChest = chest;
        renderer.setChestVisible(true);
    }

    private void closeChestOverlay() {
        if (openedChest != null) {
            openedChest.close();
            openedChest = null;
        }
        renderer.setChestVisible(false);
    }

    private Chest findNearestUsableChest(double centerX, double centerY, double range) {
        Chest nearest = null;
        double nearestDistance = Math.max(0.0, range);
        for (BuildObject object : buildManager.getPlacedObjectsByType(BuildType.CHEST)) {
            if (!(object instanceof Chest chest) || !chest.isAlive()) {
                continue;
            }
            double dist = distance(centerX, centerY, chest.getCenterX(), chest.getCenterY());
            if (dist > nearestDistance) {
                continue;
            }
            nearest = chest;
            nearestDistance = dist;
        }
        return nearest;
    }

    private void ensureCampChestExists() {
        if (hasAnyAliveChest()) {
            return;
        }
        int tileWidth = Math.max(1, buildCollisionManager.getTileWidth());
        int tileHeight = Math.max(1, buildCollisionManager.getTileHeight());
        int baseTileX = (int) Math.floor(baseCamp.getCenterX() / tileWidth);
        int baseTileY = (int) Math.floor(baseCamp.getCenterY() / tileHeight);
        int[][] offsets = {
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 1}, {-3, 1}, {1, 3}, {-1, -3}
        };
        for (int[] offset : offsets) {
            int tileX = baseTileX + offset[0];
            int tileY = baseTileY + offset[1];
            if (buildManager.spawnWorldObject(CHEST_ITEM_ID, tileX, tileY, player)) {
                BuildObject placed = buildManager.getPlacedObjectAt(tileX, tileY);
                if (placed != null) {
                    placed.setHealth(GameBalance.CHEST_HITS_TO_BREAK);
                }
                return;
            }
        }
    }

    private boolean hasAnyAliveChest() {
        for (BuildObject object : buildManager.getPlacedObjectsByType(BuildType.CHEST)) {
            if (object instanceof Chest chest && chest.isAlive()) {
                return true;
            }
        }
        return false;
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

    private boolean throwSelectedThrowableBomb(String itemId, long now) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        if (inventory.getAmount(itemId) <= 0) {
            renderer.showToast("No " + prettifyItemName(itemId).toLowerCase());
            return false;
        }
        if (!inventory.consumeItem(itemId, 1)) {
            renderer.showToast("Cannot use " + prettifyItemName(itemId).toLowerCase());
            return false;
        }

        double sourceX = player.getCenterX();
        double sourceY = player.getCenterY() - 8.0;
        double targetX = cameraX + (inputHandler.getMouseX() / CAMERA_ZOOM);
        double targetY = cameraY + (inputHandler.getMouseY() / CAMERA_ZOOM);
        double dx = targetX - sourceX;
        double dy = targetY - sourceY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double throwRange = FIRE_BOMB_ITEM_ID.equals(itemId) ? FIRE_BOMB_THROW_RANGE : BOMB_TRAP_THROW_RANGE;
        if (dist > throwRange && dist > 0.0001) {
            targetX = sourceX + dx / dist * throwRange;
            targetY = sourceY + dy / dist * throwRange;
        }

        double throwSpeed = FIRE_BOMB_ITEM_ID.equals(itemId) ? FIRE_BOMB_THROW_SPEED : BOMB_TRAP_THROW_SPEED;
        double blastRadius = FIRE_BOMB_ITEM_ID.equals(itemId) ? GameBalance.FIRE_BOMB_RADIUS : GameBalance.BOMB_TRAP_EXPLOSION_RADIUS_TILES * 16.0;
        int damage = FIRE_BOMB_ITEM_ID.equals(itemId) ? GameBalance.FIRE_BOMB_IMPACT_DAMAGE : GameBalance.BOMB_TRAP_DAMAGE;
        long fuseNs = FIRE_BOMB_ITEM_ID.equals(itemId) ? FIRE_BOMB_FUSE_NS : GameBalance.BOMB_TRAP_FUSE_NS;
        double renderSize = FIRE_BOMB_ITEM_ID.equals(itemId) ? FIRE_BOMB_RENDER_SIZE : BOMB_TRAP_RENDER_SIZE;
        thrownBombs.add(new ThrownBomb(
                itemId,
                sourceX,
                sourceY,
                targetX,
                targetY,
                throwSpeed,
                blastRadius,
                damage,
                now,
                fuseNs,
                renderSize
        ));
        refreshBuildInventoryUi();
        renderer.showToast(prettifyItemName(itemId) + " thrown");
        return true;
    }

    private void updateWallJumperSpawning(long now) {
        if (countAliveEnemyByType("WALL_JUMPER") >= WALL_JUMPER_MAX_ALIVE) {
            return;
        }
        if (nextWallJumperSpawnAtNs <= 0L) {
            nextWallJumperSpawnAtNs = now;
        }
        if (now < nextWallJumperSpawnAtNs) {
            return;
        }

        WallJumperEnemy spawned = spawnWallJumperEnemy();
        nextWallJumperSpawnAtNs = now + randomWallJumperSpawnDelayNs();
        if (spawned != null) {
            enemies.add(spawned);
        }
    }

    private void updateGolemSpawning(long now) {
        if (countAliveEnemyByType("GOLEM") >= GOLEM_MAX_ALIVE) {
            return;
        }
        if (nextGolemSpawnAtNs <= 0L) {
            nextGolemSpawnAtNs = now;
        }
        if (now < nextGolemSpawnAtNs) {
            return;
        }
        GolemEnemy spawned = spawnGolemEnemy();
        nextGolemSpawnAtNs = now + randomGolemSpawnDelayNs();
        if (spawned != null) {
            enemies.add(spawned);
        }
    }

    private void spawnWaveWolves(int count) {
        wolfSpawnManager.spawnAtMapEdges(enemies, count, worldWidth, worldHeight);
    }

    private void spawnGolemWave(int count, GolemEnemy.GolemMode mode) {
        for (int i = 0; i < count; i++) {
            GolemEnemy spawned = spawnGolemEnemy(mode);
            if (spawned != null) {
                enemies.add(spawned);
            }
        }
    }

    private void spawnWallJumperWave(int count) {
        for (int i = 0; i < count; i++) {
            WallJumperEnemy spawned = spawnWallJumperEnemy();
            if (spawned != null) {
                enemies.add(spawned);
            }
        }
    }

    private void despawnHostileEnemiesForMorning() {
        enemies.removeIf(enemy -> enemy != null && enemy.isHostile());
        wolfSpawnManager.clear();
    }

    private GolemEnemy spawnGolemEnemy() {
        return spawnGolemEnemy(GolemEnemy.GolemMode.NORMAL);
    }

    private GolemEnemy spawnGolemEnemy(GolemEnemy.GolemMode mode) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double[] spawn = randomEdgeSpawnPoint(GolemEnemy.RENDER_WIDTH, GolemEnemy.RENDER_HEIGHT);
            GolemEnemy enemy = new GolemEnemy(
                    spawn[0],
                    spawn[1],
                    this::canGolemOccupy,
                    new GolemEnemy.WorldQuery() {
                        @Override
                        public int getTileWidth() {
                            return buildCollisionManager.getTileWidth();
                        }

                        @Override
                        public int getTileHeight() {
                            return buildCollisionManager.getTileHeight();
                        }

                        @Override
                        public BuildObject findNearestWallToAttack(GolemEnemy enemy, double towardX, double towardY, double maxDistance) {
                            return Game.this.findNearestGolemWallToAttack(enemy, towardX, towardY, maxDistance);
                        }

                        @Override
                        public boolean damageWall(GolemEnemy enemy, BuildObject wall, long nowNs) {
                            return Game.this.damageGolemWall(enemy, wall, nowNs);
                        }
                    },
                    mode
            );
            if (canGolemOccupy(enemy, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                return enemy;
            }
        }
        return null;
    }

    private long randomGolemSpawnDelayNs() {
        if (GOLEM_SPAWN_MAX_INTERVAL_NS <= GOLEM_SPAWN_MIN_INTERVAL_NS) {
            return GOLEM_SPAWN_MIN_INTERVAL_NS;
        }
        long span = GOLEM_SPAWN_MAX_INTERVAL_NS - GOLEM_SPAWN_MIN_INTERVAL_NS;
        return GOLEM_SPAWN_MIN_INTERVAL_NS + (long) (random.nextDouble() * span);
    }

    private WallJumperEnemy spawnWallJumperEnemy() {
        int tileSize = Math.max(buildCollisionManager.getTileWidth(), buildCollisionManager.getTileHeight());
        for (int attempt = 0; attempt < 18; attempt++) {
            double[] spawn = randomEdgeSpawnPoint(
                    WallJumperEnemy.defaultRenderWidth(tileSize),
                    WallJumperEnemy.defaultRenderHeight(tileSize)
            );
            WallJumperEnemy enemy = new WallJumperEnemy(spawn[0], spawn[1], tileSize, this::canWallJumperOccupy);
            if (canWallJumperOccupy(enemy, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                return enemy;
            }
        }
        return null;
    }

    private long randomWallJumperSpawnDelayNs() {
        if (WALL_JUMPER_SPAWN_MAX_INTERVAL_NS <= WALL_JUMPER_SPAWN_MIN_INTERVAL_NS) {
            return WALL_JUMPER_SPAWN_MIN_INTERVAL_NS;
        }
        long span = WALL_JUMPER_SPAWN_MAX_INTERVAL_NS - WALL_JUMPER_SPAWN_MIN_INTERVAL_NS;
        return WALL_JUMPER_SPAWN_MIN_INTERVAL_NS + (long) (random.nextDouble() * span);
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

    private void movePlayerWithSliding(double dx, double dy) {
        MovementSlideSystem.MoveResult result = MovementSlideSystem.move(
                player.getX(),
                player.getY(),
                player.getWidth(),
                player.getHeight(),
                dx,
                dy,
                (x, y, width, height) -> canPlayerOccupyAt(x, y, width, height)
        );
        player.setPosition(result.x(), result.y());
    }

    private boolean canPlayerOccupyAt(double x, double y, double width, double height) {
        if (x < 0 || y < 0 || x + width > worldWidth || y + height > worldHeight) {
            return false;
        }
        double px = player.getCollisionXAt(x, width, height);
        double py = player.getCollisionYAt(y, width, height);
        double pw = player.getCollisionWidthAt(width, height);
        double ph = player.getCollisionHeightAt(width, height);

        if (intersectsBaseCampCollision(px, py, pw, ph)) {
            return false;
        }

        for (MapObjectData object : mapCollisions) {
            if (!"Collision".equalsIgnoreCase(object.getType())) {
                continue;
            }
            if (object.intersects(px, py, pw, ph)) {
                return false;
            }
        }
        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(px, py, pw, ph)) {
            return false;
        }
        return !intersectsPlacedBuildObjectFast(px, py, pw, ph);
    }

    private boolean isEnemyCollidingWithFriendlyArcher(Enemy enemy) {
        return firstCollidingFriendlyArcher(enemy) != null;
    }

    private boolean canHostileEnemyOccupy(Enemy enemy, double x, double y, double width, double height) {
        if (enemy == null) {
            return false;
        }
        double collisionX = enemy.getCollisionXAt(x, width, height);
        double collisionY = enemy.getCollisionYAt(y, width, height);
        double collisionWidth = enemy.getCollisionWidthAt(width, height);
        double collisionHeight = enemy.getCollisionHeightAt(width, height);
        if (collisionX < 0 || collisionY < 0 || collisionX + collisionWidth > worldWidth || collisionY + collisionHeight > worldHeight) {
            return false;
        }
        if (intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    archer.getCollisionX(), archer.getCollisionY(), archer.getCollisionWidth(), archer.getCollisionHeight())) {
                return false;
            }
        }
        return true;
    }

    private FriendlyArcher firstCollidingFriendlyArcher(Enemy enemy) {
        if (enemy == null) {
            return null;
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(enemy, archer)) {
                return archer;
            }
        }
        return null;
    }

    private boolean canFriendlyArcherOccupy(FriendlyArcher archer, double x, double y, double width, double height) {
        double collisionX = archer.getCollisionXAt(x, width, height);
        double collisionY = archer.getCollisionYAt(y, width, height);
        double collisionWidth = archer.getCollisionWidthAt(width, height);
        double collisionHeight = archer.getCollisionHeightAt(width, height);
        if (intersectsBaseCampCollision(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        if (intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    enemy.getCollisionX(), enemy.getCollisionY(), enemy.getCollisionWidth(), enemy.getCollisionHeight())) {
                return false;
            }
        }
        for (FriendlyArcher other : friendlyArcherManager.getArchers()) {
            if (other == null || other == archer || !other.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    other.getCollisionX(), other.getCollisionY(), other.getCollisionWidth(), other.getCollisionHeight())) {
                return false;
            }
        }
        return true;
    }

    private boolean intersectsBaseCampCollision(double x, double y, double width, double height) {
        return baseCamp != null
                && CollisionSystem.intersects(
                x,
                y,
                width,
                height,
                baseCamp.getCollisionX(),
                baseCamp.getCollisionY(),
                baseCamp.getCollisionWidth(),
                baseCamp.getCollisionHeight()
        );
    }

    private boolean intersectsPlacedBuildObjectFast(double x, double y, double width, double height) {
        if (buildManager == null || width <= 0.0 || height <= 0.0) {
            return false;
        }
        int tileWidth = Math.max(1, buildCollisionManager.getTileWidth());
        int tileHeight = Math.max(1, buildCollisionManager.getTileHeight());
        int minTileX = (int) Math.floor(x / tileWidth);
        int maxTileX = (int) Math.floor((x + width - 0.001) / tileWidth);
        int minTileY = (int) Math.floor(y / tileHeight);
        int maxTileY = (int) Math.floor((y + height - 0.001) / tileHeight);
        BuildObject lastChecked = null;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                BuildObject object = buildManager.getPlacedObjectAt(tx, ty);
                if (object == null || object == lastChecked) {
                    continue;
                }
                lastChecked = object;
                if (object.getComponent(buildsystem.component.CollisionComponent.class) == null) {
                    continue;
                }
                if (CollisionSystem.intersects(
                        x,
                        y,
                        width,
                        height,
                        object.getCollisionX(),
                        object.getCollisionY(),
                        object.getCollisionWidth(),
                        object.getCollisionHeight())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canWallJumperOccupy(WallJumperEnemy enemy, double x, double y, double width, double height) {
        double collisionX = enemy.getCollisionXAt(x, width, height);
        double collisionY = enemy.getCollisionYAt(y, width, height);
        double collisionWidth = enemy.getCollisionWidthAt(width, height);
        double collisionHeight = enemy.getCollisionHeightAt(width, height);
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                player.getCollisionX(), player.getCollisionY(), player.getCollisionWidth(), player.getCollisionHeight())) {
            return false;
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    archer.getCollisionX(), archer.getCollisionY(), archer.getCollisionWidth(), archer.getCollisionHeight())) {
                return false;
            }
        }
        for (Enemy other : enemies) {
            if (other == null || other == enemy || other.shouldRemoveFromWorld() || !other.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    other.getCollisionX(), other.getCollisionY(), other.getCollisionWidth(), other.getCollisionHeight())) {
                return false;
            }
        }
        return true;
    }

    private boolean canGolemOccupy(GolemEnemy enemy, double x, double y, double width, double height) {
        if (enemy == null) {
            return false;
        }
        double collisionX = enemy.getCollisionXAt(x, width, height);
        double collisionY = enemy.getCollisionYAt(y, width, height);
        double collisionWidth = enemy.getCollisionWidthAt(width, height);
        double collisionHeight = enemy.getCollisionHeightAt(width, height);
        if (collisionX < 0 || collisionY < 0 || collisionX + collisionWidth > worldWidth || collisionY + collisionHeight > worldHeight) {
            return false;
        }
        if (player != null && player.isAlive()
                && CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                player.getCollisionX(), player.getCollisionY(), player.getCollisionWidth(), player.getCollisionHeight())) {
            return false;
        }
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)
                || intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        for (Enemy other : enemies) {
            if (other == null || other == enemy || !other.isAlive() || other.shouldRemoveFromWorld()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    other.getCollisionX(), other.getCollisionY(), other.getCollisionWidth(), other.getCollisionHeight())) {
                return false;
            }
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    archer.getCollisionX(), archer.getCollisionY(), archer.getCollisionWidth(), archer.getCollisionHeight())) {
                return false;
            }
        }
        return true;
    }

    private boolean canWolfOccupy(WolfEnemy enemy, double x, double y, double width, double height) {
        if (enemy == null) {
            return false;
        }
        double collisionX = enemy.getCollisionXAt(x, width, height);
        double collisionY = enemy.getCollisionYAt(y, width, height);
        double collisionWidth = enemy.getCollisionWidthAt(width, height);
        double collisionHeight = enemy.getCollisionHeightAt(width, height);
        if (collisionX < 0 || collisionY < 0 || collisionX + collisionWidth > worldWidth || collisionY + collisionHeight > worldHeight) {
            return false;
        }
        if (intersectsBaseCampCollision(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        if (player != null && player.isAlive()
                && CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                player.getCollisionX(), player.getCollisionY(), player.getCollisionWidth(), player.getCollisionHeight())) {
            return false;
        }
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)
                || intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
        }
        for (Enemy other : enemies) {
            if (other == null || other == enemy || !other.isAlive() || other.shouldRemoveFromWorld()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    other.getCollisionX(), other.getCollisionY(), other.getCollisionWidth(), other.getCollisionHeight())) {
                return false;
            }
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(collisionX, collisionY, collisionWidth, collisionHeight,
                    archer.getCollisionX(), archer.getCollisionY(), archer.getCollisionWidth(), archer.getCollisionHeight())) {
                return false;
            }
        }
        return true;
    }

    private boolean spawnFriendlyArcherNearPlayer() {
        int playerGridX = (int) Math.floor(player.getCenterX() / buildCollisionManager.getTileWidth());
        int playerGridY = (int) Math.floor(player.getCenterY() / buildCollisionManager.getTileHeight());

        for (int radius = 1; radius <= 8; radius++) {
            int[][] priority = {
                    {playerGridX + radius, playerGridY},
                    {playerGridX - radius, playerGridY},
                    {playerGridX, playerGridY + radius},
                    {playerGridX, playerGridY - radius}
            };
            for (int[] tile : priority) {
                if (trySpawnFriendlyArcherAtTile(tile[0], tile[1])) {
                    return true;
                }
            }

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }
                    if (trySpawnFriendlyArcherAtTile(playerGridX + dx, playerGridY + dy)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean trySpawnFriendlyArcherAtTile(int gridX, int gridY) {
        if (!isValidFriendlyArcherTile(gridX, gridY)) {
            return false;
        }
        FriendlyArcher archer = friendlyArcherManager.spawnAtTile(
                gridX,
                gridY,
                buildCollisionManager.getTileWidth(),
                buildCollisionManager.getTileHeight()
        );
        if (!canFriendlyArcherOccupy(archer, archer.getX(), archer.getY(), archer.getWidth(), archer.getHeight())) {
            friendlyArcherManager.getArchers().remove(archer);
            return false;
        }
        return true;
    }

    private boolean isValidFriendlyArcherTile(int gridX, int gridY) {
        if (mapData == null) {
            return false;
        }
        if (gridX < 0 || gridY < 0 || gridX >= mapData.getWidthInTiles() || gridY >= mapData.getHeightInTiles()) {
            return false;
        }
        if (buildManager.getPlacedObjectAt(gridX, gridY) != null) {
            return false;
        }
        if (intersectsPlayerTile(gridX, gridY)) {
            return false;
        }
        int tileWidth = buildCollisionManager.getTileWidth();
        int tileHeight = buildCollisionManager.getTileHeight();
        double x = gridX * tileWidth;
        double y = gridY * tileHeight;
        if (buildCollisionManager.isBlockedByStaticObjects(x, y, tileWidth, tileHeight)
                || buildCollisionManager.isBlockedByTerrain(x, y, tileWidth, tileHeight)
                || buildCollisionManager.isBlockedByWater(x, y, tileWidth, tileHeight)) {
            return false;
        }
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(x, y, tileWidth, tileHeight, enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                return false;
            }
        }
        for (FriendlyArcher archer : friendlyArcherManager.getArchers()) {
            if (archer == null || !archer.isAlive()) {
                continue;
            }
            if (CollisionSystem.intersects(x, y, tileWidth, tileHeight, archer.getX(), archer.getY(), archer.getWidth(), archer.getHeight())) {
                return false;
            }
        }
        return true;
    }

    private void spawnAmbientBlackGrouse() {
        blackGrouseSpawnManager.spawnInitialFlock(enemies);
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
        return randomEdgeSpawnPoint(64.0, 64.0);
    }

    private double[] randomEdgeSpawnPoint(double objectWidth, double objectHeight) {
        int side = random.nextInt(4);
        double margin = 12;
        double x;
        double y;
        double width = Math.max(1.0, objectWidth);
        double height = Math.max(1.0, objectHeight);

        if (side == 0) {
            x = random.nextDouble() * Math.max(1, worldWidth - width);
            y = margin;
        } else if (side == 1) {
            x = Math.max(0, worldWidth - width - margin);
            y = random.nextDouble() * Math.max(1, worldHeight - height);
        } else if (side == 2) {
            x = random.nextDouble() * Math.max(1, worldWidth - width);
            y = Math.max(0, worldHeight - height - margin);
        } else {
            x = margin;
            y = random.nextDouble() * Math.max(1, worldHeight - height);
        }
        return new double[]{x, y};
    }

    private String buildSurvivalObjectiveStatus(long now) {
        int day = getCurrentSurvivalDay(now);
        return "Day " + day
                + " | Base HP " + baseCamp.getHp() + "/" + baseCamp.getMaxHp();
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

    private void spawnPlayerDamageText(DamageResult result, long nowNs) {
        if (result == null || result.getFinalDamage() <= 0) {
            return;
        }
        floatingDamageTexts.add(new FloatingDamageText(
                "-" + result.getFinalDamage(),
                player.getCenterX(),
                player.getY() - 10,
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

    private void applyResourceExplosionDamage(double centerX, double centerY, double radius, int damage, long nowNs) {
        List<ResourceHitResult> resourceHits = resourceManager.damageResourcesInRadius(centerX, centerY, radius, damage, nowNs);
        for (ResourceHitResult hitResult : resourceHits) {
            if (hitResult == null) {
                continue;
            }
            spawnResourceDamageText(hitResult, nowNs);
            if (!hitResult.isDestroyed() || hitResult.getDropResult() == null) {
                continue;
            }
            ResourceType destroyedType = hitResult.getResourceNode() == null ? ResourceType.UNKNOWN : hitResult.getResourceNode().getResourceType();
            DropResult drop = hitResult.getDropResult();
            if (destroyedType == ResourceType.TREE || destroyedType == ResourceType.ROCK) {
                onResourceDestroyed(hitResult.getResourceNode());
            } else {
                inventory.addItem(drop.getItemId(), drop.getAmount());
                if (isFoodItem(drop.getItemId())) {
                    player.recoverEnergy(FOOD_ENERGY_BONUS);
                }
            }
            eventBus.publish(new GameEvent(GameEventType.RESOURCE_COLLECTED,
                    Map.of("item", drop.getItemId(), "amount", drop.getAmount())));
        }
        if (!resourceHits.isEmpty()) {
            refreshBuildInventoryUi();
        }
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
                || normalized.contains("niku")
                || normalized.contains("carrot")
                || normalized.contains("vegetable")
                || normalized.contains("food");
    }

    private String prettifyItemName(String itemId) {
        return switch (itemId) {
            case WOOD_FENCE_ITEM_ID -> "Wood Fence";
            case WOOD_WALL_ITEM_ID -> "Wood Wall";
            case POTION_ITEM_ID -> "Potion";
            case TORCH_ITEM_ID -> "Torch";
            case AXE_ITEM_ID -> "Axe";
            case ARCHER_TOWER_ITEM_ID -> "Archer Tower";
            case FRIENDLY_ARCHER_ITEM_ID -> "Archer";
            case CHEST_ITEM_ID -> "Chest";
            case BOMB_TRAP_ITEM_ID -> "Bomb Trap";
            case FIRE_BOMB_ITEM_ID -> "Fire Bomb";
            case BASIC_SWORD_ITEM_ID -> "Basic Sword";
            case PICKAXE_ITEM_ID -> "Pickaxe";
            case COIN_ITEM_ID -> "Coin";
            case "rock" -> "Rock";
            case "niku" -> "Niku";
            case CARROT_ITEM_ID -> "Carrot";
            default -> itemId;
        };
    }

    private void spawnDroppedItem(String itemId, int amount, double centerX, double centerY) {
        spawnDroppedItem(DropItemType.fromId(itemId), amount, centerX, centerY);
    }

    private void spawnDroppedItem(DropItemType type, int amount, double centerX, double centerY) {
        if (type == null || type == DropItemType.UNKNOWN || amount <= 0) {
            return;
        }
        if (type == DropItemType.BOMB_TRAP) {
            droppedItems.add(new BombDropItem(amount, centerX, centerY));
            return;
        }
        double[] size = resolveDroppedItemSize(type);
        double x = centerX - size[0] / 2.0;
        double y = centerY - size[1] / 2.0;
        droppedItems.add(new AnimatedDropItem(type, amount, x, y, size[0], size[1]));
    }

    private double[] resolveDroppedItemSize(DropItemType type) {
        if (type == null || type == DropItemType.UNKNOWN) {
            return new double[]{GameBalance.DROPPED_ITEM_SIZE, GameBalance.DROPPED_ITEM_SIZE};
        }
        String itemId = type.getInventoryItemId();
        if (TORCH_ITEM_ID.equals(itemId)) {
            return new double[]{GameBalance.DROPPED_TORCH_WIDTH, GameBalance.DROPPED_TORCH_HEIGHT};
        }
        if (ARCHER_TOWER_ITEM_ID.equals(itemId)) {
            return new double[]{GameBalance.DROPPED_ARCHER_TOWER_WIDTH, GameBalance.DROPPED_ARCHER_TOWER_HEIGHT};
        }
        if (type == DropItemType.BOMB_TRAP) {
            return new double[]{GameBalance.DROPPED_BOMB_TRAP_SIZE, GameBalance.DROPPED_BOMB_TRAP_SIZE};
        }
        if (type == DropItemType.ROCK) {
            return new double[]{GameBalance.DROPPED_STONE_WIDTH, GameBalance.DROPPED_STONE_HEIGHT};
        }
        return new double[]{type.getRenderWidth(), type.getRenderHeight()};
    }

    private double distance(double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double edgeDistanceBetween(Entity entity, BuildObject object) {
        if (entity == null || object == null) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = axisGap(
                entity.getCollisionX(),
                entity.getCollisionX() + entity.getCollisionWidth(),
                object.getCollisionX(),
                object.getCollisionX() + object.getCollisionWidth()
        );
        double dy = axisGap(
                entity.getCollisionY(),
                entity.getCollisionY() + entity.getCollisionHeight(),
                object.getCollisionY(),
                object.getCollisionY() + object.getCollisionHeight()
        );
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double normalizeAngleRadians(double angle) {
        double twoPi = Math.PI * 2.0;
        double normalized = angle % twoPi;
        if (normalized > Math.PI) {
            normalized -= twoPi;
        } else if (normalized < -Math.PI) {
            normalized += twoPi;
        }
        return normalized;
    }

    private double axisGap(double minA, double maxA, double minB, double maxB) {
        if (maxA < minB) {
            return minB - maxA;
        }
        if (maxB < minA) {
            return minA - maxB;
        }
        return 0.0;
    }

    // seedStartingBuildItems:
    // - World moi chi nhan coin de mua build item trong shop.
    private void seedStartingBuildItems() {
        if (inventory.getAmount(COIN_ITEM_ID) <= 0) {
            inventory.addItem(COIN_ITEM_ID, GameBalance.STARTING_COIN_AMOUNT);
        }
        if (inventory.getAmount(NIKU_ITEM_ID) <= 0) {
            inventory.addItem(NIKU_ITEM_ID, 2);
        }
    }

    private void loadMapWoodFences() {
        if (mapData == null) {
            return;
        }
        Set<String> fenceTiles = new LinkedHashSet<>();

        FencePerimeter perimeter = buildInitialFencePerimeterAroundBaseCamp();
        int gateSize = INITIAL_FENCE_GATE_SIZE_TILES;
        int gateStartX = perimeter.centerX() - gateSize / 2;
        int gateEndX = gateStartX + gateSize - 1;
        int gateStartY = perimeter.centerY() - gateSize / 2;
        int gateEndY = gateStartY + gateSize - 1;

        for (int x = perimeter.left(); x <= perimeter.right(); x++) {
            if (x < gateStartX || x > gateEndX) {
                fenceTiles.add(x + ":" + perimeter.top());
                fenceTiles.add(x + ":" + perimeter.bottom());
            }
        }

        for (int y = perimeter.top(); y <= perimeter.bottom(); y++) {
            if (y < gateStartY || y > gateEndY) {
                fenceTiles.add(perimeter.left() + ":" + y);
                fenceTiles.add(perimeter.right() + ":" + y);
            }
        }

        for (String key : fenceTiles) {
            String[] parts = key.split(":");
            if (parts.length != 2) {
                continue;
            }
            addMapWoodFence(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    private FencePerimeter buildInitialFencePerimeterAroundBaseCamp() {
        int tileWidth = Math.max(1, mapData.getTileWidth());
        int tileHeight = Math.max(1, mapData.getTileHeight());

        // Rao khoi tao dat theo tam cua base camp nhu logic cu, nhung chi rong hon trai mot it
        // de van nhin thay ro va khong om sat hitbox cua tent.
        double campX = baseCamp.getX();
        double campY = baseCamp.getY();
        double campWidth = baseCamp.getWidth();
        double campHeight = baseCamp.getHeight();

        int campLeft = (int) Math.floor(campX / tileWidth);
        int campTop = (int) Math.floor(campY / tileHeight);
        int campRight = (int) Math.floor((campX + campWidth - 1.0) / tileWidth);
        int campBottom = (int) Math.floor((campY + campHeight - 1.0) / tileHeight);

        int left = campLeft - INITIAL_FENCE_PADDING_X_TILES;
        int top = campTop - INITIAL_FENCE_PADDING_Y_TILES;
        int right = campRight + INITIAL_FENCE_PADDING_X_TILES;
        int bottom = campBottom + INITIAL_FENCE_PADDING_Y_TILES;
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;

        return new FencePerimeter(left, top, right, bottom, centerX, centerY);
    }

    private void addMapWoodFence(int gridX, int gridY) {
        if (mapData == null) {
            return;
        }
        if (gridX < 0 || gridY < 0 || gridX >= mapData.getWidthInTiles() || gridY >= mapData.getHeightInTiles()) {
            return;
        }
        if (intersectsPlayerTile(gridX, gridY)) {
            return;
        }
        if (buildManager.getPlacedObjectAt(gridX, gridY) != null) {
            return;
        }
        FenceEntity fence = new FenceEntity(
                gridX,
                gridY,
                mapData.getTileWidth(),
                mapData.getTileHeight()
        );
        fence.setHealth(GameBalance.WOOD_FENCE_MAX_HP);
        fence.setSaveEnabled(false);
        buildManager.addPlacedObject(fence);
    }

    private boolean intersectsPlayerTile(int gridX, int gridY) {
        int tileWidth = mapData == null ? 16 : mapData.getTileWidth();
        int tileHeight = mapData == null ? 16 : mapData.getTileHeight();
        double x = gridX * tileWidth;
        double y = gridY * tileHeight;
        return CollisionSystem.intersects(
                player.getX() + player.getWidth() * 0.22,
                player.getY() + player.getHeight() * 0.30,
                player.getWidth() * 0.56,
                player.getHeight() * 0.62,
                x,
                y,
                tileWidth,
                tileHeight
        );
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
        // - Hien tai slot nao co item build thi BuildManager se tu doc tu toolbar model.
        // - Chon slot nay se thong bao cho BuildManager bat BUILD_WALL_MODE.
        // - Cac slot khac de null de game thoat khoi che do xay.
        selectedHotbarIndex = slotIndex;
        syncSelectedHotbarMode();
    }

    private void updateDroppedItemPickup() {
        if (droppedItems.isEmpty()) {
            return;
        }
        boolean pickedInventoryItem = false;
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
                pickedInventoryItem |= applyDroppedItemPickup(droppedItem);
                picked.add(droppedItem);
            }
        }
        if (!picked.isEmpty()) {
            droppedItems.removeAll(picked);
        }
        if (pickedInventoryItem) {
            refreshBuildInventoryUi();
        }
    }

    private boolean applyDroppedItemPickup(DroppedItem droppedItem) {
        if (droppedItem == null || droppedItem.getAmount() <= 0) {
            return false;
        }
        DropItemType type = droppedItem.getType();
        if (type == null || type == DropItemType.UNKNOWN) {
            return false;
        }
        if (type.isExperienceDrop()) {
            player.addExperience(droppedItem.getAmount());
            return false;
        }
        String inventoryItemId = type.getInventoryItemId();
        if (inventoryItemId == null || inventoryItemId.isBlank()) {
            return false;
        }
        inventory.addItem(inventoryItemId, droppedItem.getAmount());
        return true;
    }

    private void refreshBuildInventoryUi() {
        String previouslySelectedItemId = getSelectedHotbarItemId();
        buildManager.syncToolbar(inventory.snapshot());
        rebuildHotbarItems();
        if (previouslySelectedItemId != null && !previouslySelectedItemId.isBlank()) {
            int matchedIndex = findHotbarIndexByItemId(previouslySelectedItemId);
            if (matchedIndex >= 0) {
                selectedHotbarIndex = matchedIndex;
            }
        }
        if (selectedHotbarIndex >= hotbarItems.size()) {
            selectedHotbarIndex = Math.max(0, hotbarItems.size() - 1);
        }
        if (selectedHotbarIndex < 0) {
            selectedHotbarIndex = 0;
        }
        syncSelectedHotbarMode();
    }

    private void rebuildHotbarItems() {
        hotbarItems.clear();
        Map<String, Integer> snapshot = inventory.snapshot();
        for (String itemId : HOTBAR_PRIORITY) {
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            int amount = snapshot.getOrDefault(itemId, 0);
            if (amount <= 0) {
                continue;
            }
            ItemUiMeta meta = renderer.getItemMeta(itemId);
            hotbarItems.add(new HotbarItemStack(itemId, amount, meta, isBuildItemId(itemId)));
            if (hotbarItems.size() >= 9) {
                return;
            }
        }
    }

    private void syncSelectedHotbarMode() {
        String selectedItemId = getSelectedHotbarItemId();
        if (selectedItemId == null || selectedItemId.isBlank()) {
            buildManager.cancelBuildMode();
            player.setEquipmentMode(Player.EquipmentMode.HAND_MODE);
            return;
        }
        player.setEquipmentMode(AXE_ITEM_ID.equals(selectedItemId)
                ? Player.EquipmentMode.AXE_MODE
                : Player.EquipmentMode.HAND_MODE);
        if (isBuildItemId(selectedItemId)) {
            buildController.onSelectBuildItem(selectedItemId);
            return;
        }
        buildManager.cancelBuildMode();
    }

    private String getSelectedHotbarItemId() {
        if (selectedHotbarIndex < 0 || selectedHotbarIndex >= hotbarItems.size()) {
            return "";
        }
        HotbarItemStack stack = hotbarItems.get(selectedHotbarIndex);
        return stack == null ? "" : stack.getItemId();
    }

    private boolean isBuildItemId(String itemId) {
        if (BOMB_TRAP_ITEM_ID.equals(itemId) || FIRE_BOMB_ITEM_ID.equals(itemId)) {
            return false;
        }
        return buildManager.getRegistry().findByItemId(itemId) != null;
    }

    private int findHotbarIndexByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < hotbarItems.size(); i++) {
            HotbarItemStack stack = hotbarItems.get(i);
            if (stack != null && itemId.equals(stack.getItemId())) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> resolveShopPurchaseCosts(String resolvedItemId) {
        return switch (resolvedItemId) {
            case WOOD_FENCE_ITEM_ID -> Map.of(COIN_ITEM_ID, WOOD_FENCE_PRICE);
            case WOOD_WALL_ITEM_ID -> Map.of(COIN_ITEM_ID, WOOD_WALL_PRICE);
            case POTION_ITEM_ID -> Map.of(COIN_ITEM_ID, 12);
            case TORCH_ITEM_ID -> Map.of(COIN_ITEM_ID, TORCH_PRICE);
            case AXE_ITEM_ID -> Map.of("wood", AXE_WOOD_COST, "stone", AXE_STONE_COST);
            case ARCHER_TOWER_ITEM_ID -> Map.of(COIN_ITEM_ID, ARCHER_TOWER_PRICE);
            case FRIENDLY_ARCHER_ITEM_ID -> Map.of(COIN_ITEM_ID, FRIENDLY_ARCHER_PRICE);
            case CHEST_ITEM_ID -> Map.of(COIN_ITEM_ID, CHEST_PRICE);
            case BOMB_TRAP_ITEM_ID -> Map.of(COIN_ITEM_ID, BOMB_TRAP_PRICE);
            case FIRE_BOMB_ITEM_ID -> Map.of(COIN_ITEM_ID, FIRE_BOMB_PRICE);
            case BASIC_SWORD_ITEM_ID -> Map.of(COIN_ITEM_ID, 18);
            case PICKAXE_ITEM_ID -> Map.of(COIN_ITEM_ID, 14);
            case CARROT_ITEM_ID -> Map.of(COIN_ITEM_ID, 3);
            default -> Map.of();
        };
    }

    private boolean hasEnoughResources(Map<String, Integer> purchaseCosts) {
        if (purchaseCosts == null || purchaseCosts.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int required = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (inventory.getAmount(entry.getKey()) < required) {
                return false;
            }
        }
        return true;
    }

    private boolean consumePurchaseCosts(Map<String, Integer> purchaseCosts) {
        if (!hasEnoughResources(purchaseCosts)) {
            return false;
        }
        List<Map.Entry<String, Integer>> consumed = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (amount <= 0) {
                continue;
            }
            if (!inventory.consumeItem(entry.getKey(), amount)) {
                for (Map.Entry<String, Integer> rollback : consumed) {
                    inventory.addItem(rollback.getKey(), rollback.getValue());
                }
                return false;
            }
            consumed.add(Map.entry(entry.getKey(), amount));
        }
        return true;
    }

    private void refundPurchaseCosts(Map<String, Integer> purchaseCosts) {
        if (purchaseCosts == null || purchaseCosts.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : purchaseCosts.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (amount > 0) {
                inventory.addItem(entry.getKey(), amount);
            }
        }
    }

    private void onResourceDestroyed(system.resource.ResourceNode resource) {
        if (resource == null) {
            return;
        }
        if (DEBUG_DROP_LOGS) {
            System.out.println("Resource destroyed at: " + resource.getCenterX() + ", " + resource.getCenterY());
        }
        List<DropSpec> dropTable = switch (resource.getResourceType()) {
            case TREE -> TREE_DROP_TABLE;
            case ROCK -> ROCK_DROP_TABLE;
            default -> List.of();
        };
        spawnDropTable(resource.getCenterX(), resource.getCenterY(), dropTable);
    }

    private void spawnDropTable(double x, double y, List<DropSpec> dropTable) {
        if (dropTable == null || dropTable.isEmpty()) {
            return;
        }
        List<Point2D> usedPositions = new ArrayList<>();

        for (DropSpec spec : dropTable) {
            if (spec == null || spec.getType() == DropItemType.UNKNOWN || spec.getQuantity() <= 0) {
                continue;
            }
            for (int index = 0; index < spec.getQuantity(); index++) {
                Point2D pos = getNonOverlappingDropPosition(x, y, spec.getType(), usedPositions);
                spawnDroppedItem(
                        spec.getType(),
                        1,
                        pos.getX() + spec.getType().getRenderWidth() * 0.5,
                        pos.getY() + spec.getType().getRenderHeight() * 0.5
                );
                usedPositions.add(pos);
            }
        }
    }

    private Point2D getNonOverlappingDropPosition(double centerX, double centerY, DropItemType type, List<Point2D> usedPositions) {
        double width = type == null ? GameBalance.DROPPED_ITEM_SIZE : type.getRenderWidth();
        double height = type == null ? GameBalance.DROPPED_ITEM_SIZE : type.getRenderHeight();
        Point2D fallback = new Point2D(
                centerX - width * 0.5,
                centerY - height * 0.5
        );
        if (usedPositions == null) {
            return fallback;
        }

        for (int attempt = 0; attempt < DROP_POSITION_MAX_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = DROP_MIN_RADIUS + random.nextDouble() * (DROP_MAX_RADIUS - DROP_MIN_RADIUS);
            double px = centerX + Math.cos(angle) * radius - width * 0.5;
            double py = centerY + Math.sin(angle) * radius - height * 0.5;
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

    private void handleEnemyDeathDrops(Enemy enemy) {
        if (enemy == null || enemy.hasSpawnedDeathDrop() || enemy.isAlive()) {
            return;
        }
        List<DropSpec> dropTable = enemy.isHostile() ? ENEMY_DROP_TABLE : ANIMAL_DROP_TABLE;
        enemy.markDeathDropSpawned();
        spawnDropTable(enemy.getCenterX(), enemy.getCenterY(), dropTable);
    }

    private String normalizeShopItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return switch (itemId.trim().toLowerCase()) {
            case "axe", "riu", "rìu" -> AXE_ITEM_ID;
            case WALL_ITEM_ALIAS -> WOOD_FENCE_ITEM_ID;
            case "wood_fence", "wood fence" -> WOOD_FENCE_ITEM_ID;
            case "cung", "archer_tower" -> ARCHER_TOWER_ITEM_ID;
            case "archer", "friendly_archer", "friendly archer" -> FRIENDLY_ARCHER_ITEM_ID;
            case "chest", "ruong", "ruong_do" -> CHEST_ITEM_ID;
            case "bomb", "bomb_trap", "bomb trap", "bom" -> BOMB_TRAP_ITEM_ID;
            case "fire_bomb", "fire bomb", "firebomb", "bom_lua", "bomb_fire" -> FIRE_BOMB_ITEM_ID;
            default -> itemId.trim().toLowerCase();
        };
    }

    private void logShopPurchase(String requestedItemId,
                                 String resolvedItemId,
                                 Map<String, Integer> purchaseCosts,
                                 boolean inventoryAddResult,
                                 String reasonFailed) {
        System.out.println("[ShopDebug] itemId=" + requestedItemId
                + " resolvedItemId=" + resolvedItemId
                + " costs=" + purchaseCosts
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
        } else if (object.getType() == buildsystem.core.BuildType.BOMB_TRAP) {
            System.out.println("Placed BOMB_TRAP at " + x + "," + y);
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

