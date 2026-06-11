package core;

import boss.BossManager;
import boss.projectile.FireOrb;
import boss.entity.FinalBoss;
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
import dialogue.runtime.GameDialogueFlowService;
import dialogue.runtime.DialogueRunner;
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
import entity.EnemyAiDebug;
import entity.EnemyNavigationContext;
import entity.EnemyObstacleTarget;
import entity.Entity;
import entity.FlowFieldManager;
import entity.FriendlyArcher;
import entity.FriendlyArcherManager;
import entity.GolemEnemy;
import entity.PathfindingManager;
import entity.Player;
import entity.ThrownBomb;
import entity.WildlifeSpawnManager;
import entity.WallJumperEnemy;
import entity.WolfEnemy;
import entity.WolfSpawnManager;
import event.GameEvent;
import event.GameEventBus;
import event.GameEventType;
import input.InputHandler;
import inventory.HotbarService;
import inventory.Inventory;
import inventory.ShopService;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import map.GameMapDefinition;
import map.GameMapFactory;
import map.MapData;
import map.MapManager;
import map.MapObjectData;
import map.MapRuntimeApplier;
import map.MapRuntimeData;
import map.MapType;
import map.BaseCampLocator;
import map.TileLayerData;
import map.TileCollisionResolver;
import map.TilePropertyCatalog;
import map.TiledMapLoader;
import progression.SurvivalProgressionService;
import system.CollisionSystem;
import system.DamageResult;
import system.DamageSystem;
import system.MovementSlideSystem;
import system.level.LevelManager;
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceHitResult;
import system.resource.ResourceManager;
import system.resource.ResourceNode;
import system.resource.ResourceType;
import system.resource.TileResourceAdapter;
import system.bomb.BombSystem;
import system.bomb.ExplosionEffect;
import system.bomb.FireBombBurnZone;
import system.save.WorldSaveService;
import system.save.WorldSnapshotHelper;
import ui.FloatingDamageText;
import ui.ItemUiMeta;
import ui.Renderer;
import world.InfiniteWorldManager;
import world.WorldChunk;

import java.util.ArrayList;
import java.nio.file.Files;
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
    private static final long DAY_PASSIVE_HEAL_INTERVAL_NS = 12_000_000_000L;
    private static final long FLOW_FIELD_REBUILD_DEBOUNCE_NS = 500_000_000L;
    private static final long FLOW_FIELD_DESTROY_REBUILD_DEBOUNCE_NS = 300_000_000L;
    private static final int WALL_JUMPER_MAX_ALIVE = 5;
    private static final int GOLEM_MAX_ALIVE = 2;
    private static final int MAX_PATH_REQUESTS_PER_FRAME = 3;
    private static final int MAX_WAVE_SPAWNS_PER_FRAME = 2;
    private static final int MAX_FULL_NAV_ENEMIES_PER_FRAME = 3;
    private static final BuildType[] ATTACKABLE_WALL_TYPES = {
            BuildType.FENCE,
            BuildType.WOOD_WALL,
            BuildType.STONE_WALL,
            BuildType.ARCHER_TOWER,
            BuildType.DOOR
    };

    // World save file cho mode sinh ton.
    private static final String SURVIVAL_SAVE_FILE = "data/survival_world.json";
    private static final String DAY_ONE_DIALOGUE_SCRIPT_PATH = "tongquanproject/scriptday1.txt";
    private static final String BOSS_DIALOGUE_SCRIPT_PATH = "tongquanproject/boss.txt";
    private static final String BOSS_LOST_DIALOGUE_SCRIPT_PATH = "tongquanproject/boss_lost.txt";
    private static final String BOSS_NINJA_AWAKENING_SCRIPT_ID = "boss_ninja_awakening";
    private static final String ENDING_DIALOGUE_SCRIPT_PATH = "tongquanproject/ending.txt";
    private static final int DEFAULT_BASE_CAMP_HP = 100;
    private static final int INITIAL_FENCE_PADDING_X_TILES = 10;
    private static final int INITIAL_FENCE_PADDING_Y_TILES = 8;
    private static final int INITIAL_FENCE_GATE_SIZE_TILES = 3;
    private static final int INITIAL_CORNER_FENCE_ARM_TILES = 3;

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final GameDialogueFlowService dialogueFlowService;
    private final InputHandler inputHandler;
    private DialogueRunner introDialogueRunner;
    private boolean bossIntroActive;
    private boolean bossDefeatDialogueShown;
    private long bossFightBannerUntilNs;
    private boolean pendingBossReturnToMainMapEntrance;
    private boolean pendingEndingDialogueAfterBossReturn;
    private final AssetManager wallAssetManager;
    private final BuildManager buildManager;
    private final BuildController buildController;
    private final CollisionManager buildCollisionManager;
    private final BlackGrouseSpawnManager blackGrouseSpawnManager;
    private final WildlifeSpawnManager wildlifeSpawnManager;
    private final WolfSpawnManager wolfSpawnManager;
    private final EnemyNavigationContext enemyNavigationContext;
    private final EnemyAiDebug enemyAiDebug;
    private final PathfindingManager pathfindingManager;
    private final FlowFieldManager flowFieldManager;
    private final Player player;
    private final BaseCamp baseCamp;
    private final List<Enemy> enemies;
    private final FriendlyArcherManager friendlyArcherManager;

    private final List<MapObjectData> mapCollisions;
    private final ResourceManager resourceManager;
    private TileCollisionResolver tileCollisionResolver;
    private final DayNightManager dayNightManager;
    private final EnemyWaveManager enemyWaveManager;
    private final List<FloatingDamageText> floatingDamageTexts;
    private final List<DroppedItem> droppedItems;
    private final List<ArrowProjectile> arrowProjectiles;
    private final List<ThrownBomb> thrownBombs;
    private final List<ExplosionEffect> explosionEffects;
    private final List<FireBombBurnZone> fireBombBurnZones;
    private final BombSystem bombSystem;
    private final HotbarService hotbarService;
    private final ShopService shopService;
    private final SurvivalProgressionService progressionService;
    private final BossManager bossManager;
    private final BaseCampLocator baseCampLocator;

    // Inventory la state gameplay chinh cho he thu thap/craft.
    private final Inventory inventory;
    private final GameEventBus eventBus;
    private final WorldSaveService worldSaveService;
    private final InfiniteWorldManager infiniteWorldManager;
    private final MapManager mapManager;

    private GameState gameState;
    private MapData mapData;
    private long worldStartedAtNs;
    private long lastEnemySpawnAtNs;
    private long nextWallJumperSpawnAtNs;
    private long nextGolemSpawnAtNs;
    private int queuedNightWolfSpawns;
    private int queuedNormalGolemSpawns;
    private int queuedWallBreakerGolemSpawns;
    private int queuedWallJumperSpawns;
    private long lastAutoSaveAtNs;
    private long lastUpdateNowNs;
    private long eatingNikuUntilNs;
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
    private int lastBuildObjectCount;
    private long navigationBatchFrame;

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
    private static final double SKILL_F_ENERGY_COST = 3.0;
    private static final double FOOD_ENERGY_BONUS = 50.0;
    private static final long NIKU_EAT_DURATION_NS = 2_000_000_000L;
    private static final long DAMAGE_TEXT_LIFETIME_NS = 650_000_000L;
    // Hotbar hien tai de mo rong dan:
    // - Slot dau tien thuong la Wood Fence neu inventory dang co item nay.
    // - Cac slot khac de trong cho item build/craft sau nay.
    private static final String WOOD_FENCE_ITEM_ID = "wood_fence";
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
    private static final int AXE_SKILL_UNLOCK_LEVEL = 2;
    private static final int STORMBREAKER_UNLOCK_LEVEL = 3;
    private static final int DEATH_SPEAR_UNLOCK_LEVEL = 4;
    private static final String NIKU_ITEM_ID = "niku";
    private static final String SEAL_GEM_ITEM_ID = "seal_gem";
    private static final int REQUIRED_SEAL_GEMS_FOR_BOSS = 2;
    private static final boolean DEBUG_ALLOW_BOSS_ENTRY_WITHOUT_GEMS = true;
    private static final String[] HOTBAR_PRIORITY = {
            WOOD_FENCE_ITEM_ID,
            TORCH_ITEM_ID,
            ARCHER_TOWER_ITEM_ID,
            CHEST_ITEM_ID,
            BOMB_TRAP_ITEM_ID,
            FIRE_BOMB_ITEM_ID,
            BASIC_SWORD_ITEM_ID,
            PICKAXE_ITEM_ID,
            POTION_ITEM_ID,
            NIKU_ITEM_ID,
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
            new DropSpec(DropItemType.GOLD, 2)
    );
    private static final List<DropSpec> ROCK_DROP_TABLE = List.of(
            new DropSpec(DropItemType.ROCK, 2),
            new DropSpec(DropItemType.GOLD, 2)
    );
    private static final List<DropSpec> ENEMY_DROP_TABLE = List.of(
            new DropSpec(DropItemType.GOLD, 2),
            new DropSpec(DropItemType.GOLD, 2)
    );
    private static final List<DropSpec> ANIMAL_DROP_TABLE = List.of(
            new DropSpec(DropItemType.NIKU, 2),
            new DropSpec(DropItemType.GOLD, 2)
    );
    private static final int ARCHER_TOWER_PRICE = GameBalance.ARCHER_TOWER_PRICE;
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
    private static final String SAMURAI_THROW_ITEM_ID = "samurai_throw";
    private static final double SAMURAI_THROW_RANGE = 320.0;
    private static final double SAMURAI_THROW_SPEED = 12.0;
    private static final double SAMURAI_THROW_BLAST_RADIUS = 26.0;
    private static final int SAMURAI_THROW_DAMAGE = 8;
    private static final long SAMURAI_THROW_LIFETIME_NS = 1_000_000_000L;
    private static final double SAMURAI_THROW_RENDER_SIZE = 20.0;
    private static final double BOSS_JUMP_DISTANCE = 74.0;
    private static final long BOSS_JUMP_IFRAME_NS = 420_000_000L;
    private static final double BOSS_SKILL_ENERGY_COST = 3.0;
    private static final double BOSS_STRONG_ATTACK_ENERGY_COST = 8.0;
    private static final long BOSS_FIGHT_BANNER_DURATION_NS = 1_600_000_000L;
    private static final double BOSS_EXIT_TRIGGER_WIDTH = 168.0;
    private static final double BOSS_EXIT_TRIGGER_HEIGHT = 120.0;
    private static final double BOSS_EXIT_TRIGGER_Y = 48.0;
    private static final int DAY_PASSIVE_HEAL_AMOUNT = 1;

    // selectedHotbarIndex:
    // - Luu slot nguoi choi dang chon tren thanh hotbar.
    // - Tac dong gameplay: sau nay build mode va item use se dua vao slot nay.
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
    private long lastDayPassiveHealAtNs;
    private boolean dayTwoDaytimeFullHealGranted;
    private boolean finalEndingOverlayActive;
    private long finalEndingOverlayStartedAtNs;

    public Game(Stage stage) {
        // Constructor:
        // - Khoi tao tat ca subsystem runtime cho 1 session sinh ton.
        this.inputHandler = new InputHandler();
        this.wallAssetManager = new AssetManager();
        DropManager.preloadAll();
        WolfEnemy.preloadAssets();
        GolemEnemy.preloadAssets();
        WallJumperEnemy.preloadAssets();
        this.player = new Player(100, 100, 58, 58, 3.5, 100);
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
        this.hotbarService = new HotbarService(List.of(HOTBAR_PRIORITY));
        this.shopService = new ShopService();
        this.progressionService = new SurvivalProgressionService();
        this.bossManager = new BossManager();
        this.baseCampLocator = new BaseCampLocator();
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
        this.introDialogueRunner = null;
        this.bossIntroActive = false;
        this.bossDefeatDialogueShown = false;
        this.bossFightBannerUntilNs = -1L;
        this.pendingBossReturnToMainMapEntrance = false;
        this.pendingEndingDialogueAfterBossReturn = false;

        // World size tam thoi; neu load duoc map se bi ghi de bang kich thuoc map pixel that.
        this.worldWidth = 1_000_000;
        this.worldHeight = 1_000_000;
        this.worldStartedAtNs = System.nanoTime();
        this.lastEnemySpawnAtNs = 0L;
        this.nextWallJumperSpawnAtNs = 0L;
        this.nextGolemSpawnAtNs = 0L;
        this.queuedNightWolfSpawns = 0;
        this.queuedNormalGolemSpawns = 0;
        this.queuedWallBreakerGolemSpawns = 0;
        this.queuedWallJumperSpawns = 0;
        this.lastAutoSaveAtNs = 0L;
        this.lastUpdateNowNs = -1L;
        this.eatingNikuUntilNs = -1L;
        this.victoryAtNs = -1L;
        this.perfLastReportNs = 0L;
        this.perfFrames = 0L;
        this.perfPreviewNs = 0L;
        this.perfCollisionNs = 0L;
        this.perfAiNs = 0L;
        this.perfRenderNs = 0L;
        this.gameOverReason = GameOverReason.PLAYER_DIED;
        this.navigationBatchFrame = 0L;
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
        this.lastDayPassiveHealAtNs = 0L;
        this.dayTwoDaytimeFullHealGranted = false;
        this.finalEndingOverlayActive = false;
        this.finalEndingOverlayStartedAtNs = -1L;

        MapData loadedMap = GameMapFactory.tryLoadMainMap();
        this.mapData = loadedMap;
        if (loadedMap != null) {
            // Neu map load thanh cong, world boundary phai khop map de camera/player nam dung vi tri.
            this.worldWidth = loadedMap.getPixelWidth();
            this.worldHeight = loadedMap.getPixelHeight();
        }
        this.mapManager = GameMapFactory.createMapManager(
                loadedMap,
                player.getWidth(),
                player.getHeight(),
                BossManager.BOSS_MAP_IMAGE_PATH
        );
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
        this.enemyAiDebug = new EnemyAiDebug();
        this.pathfindingManager = new PathfindingManager(
                buildCollisionManager.getTileWidth(),
                buildCollisionManager.getTileHeight(),
                MAX_PATH_REQUESTS_PER_FRAME,
                4,
                800,
                enemyAiDebug
        );
        this.flowFieldManager = new FlowFieldManager(
                buildCollisionManager.getTileWidth(),
                buildCollisionManager.getTileHeight(),
                Math.max(1, (int) Math.ceil(worldWidth / buildCollisionManager.getTileWidth())),
                Math.max(1, (int) Math.ceil(worldHeight / buildCollisionManager.getTileHeight())),
                this::isFlowFieldBlockedTile,
                enemyAiDebug
        );
        this.enemyNavigationContext = new EnemyNavigationContext() {
            @Override
            public int getTileWidth() {
                return buildCollisionManager.getTileWidth();
            }

            @Override
            public int getTileHeight() {
                return buildCollisionManager.getTileHeight();
            }

            @Override
            public void requestPath(Enemy requester,
                                    double startX,
                                    double startY,
                                    double targetX,
                                    double targetY,
                                    PathfindingManager.WalkValidator validator,
                                    long nowNs) {
                pathfindingManager.requestPath(requester, startX, startY, targetX, targetY, requester.getWidth(), requester.getHeight(), validator, nowNs);
            }

            @Override
            public PathfindingManager.PathResult consumePathResult(Enemy requester) {
                return pathfindingManager.consumeResult(requester);
            }

            @Override
            public boolean canPathOccupy(Enemy requester, double x, double y, double width, double height) {
                return Game.this.canEnemyPathOccupy(requester, x, y, width, height);
            }

            @Override
            public Point2D getFlowFieldWaypoint(double worldX, double worldY) {
                return flowFieldManager.getWaypoint(worldX, worldY);
            }

            @Override
            public BuildObject findBlockingObstacle(Enemy enemy, double targetX, double targetY, int maxRayTiles, int maxNearbyRadiusTiles) {
                return Game.this.findBlockingObstacle(enemy, targetX, targetY, maxRayTiles, maxNearbyRadiusTiles);
            }

            @Override
            public EnemyObstacleTarget findEscapeObstacle(Enemy enemy, double desiredDirX, double desiredDirY, int searchRadiusTiles) {
                return Game.this.findEscapeObstacle(enemy, desiredDirX, desiredDirY, searchRadiusTiles);
            }

            @Override
            public boolean damageWall(Enemy enemy, BuildObject wall, long nowNs) {
                return Game.this.damageEnemyWall(enemy, wall, nowNs);
            }

            @Override
            public boolean damageObstacle(Enemy enemy, EnemyObstacleTarget obstacle, long nowNs) {
                return Game.this.damageEnemyObstacle(enemy, obstacle, nowNs);
            }

            @Override
            public boolean damageBase(Enemy enemy, BaseCamp targetBaseCamp, long nowNs) {
                if (enemy instanceof WolfEnemy wolfEnemy) {
                    return Game.this.damageWolfBase(wolfEnemy, targetBaseCamp, nowNs);
                }
                int beforeHp = targetBaseCamp.getHp();
                DamageSystem.applyDamage(enemy, targetBaseCamp, enemy.getDamage(), nowNs);
                int dealt = Math.max(0, beforeHp - targetBaseCamp.getHp());
                if (dealt <= 0) {
                    return false;
                }
                eventBus.publish(new GameEvent(GameEventType.BASE_CAMP_DAMAGED, Map.of("damage", dealt, "hp", targetBaseCamp.getHp())));
                return true;
            }

            @Override
            public void onObstacleDestroyed(BuildObject obstacle, long nowNs) {
                if (obstacle != null) {
                    flowFieldManager.markDirty(nowNs, FLOW_FIELD_DESTROY_REBUILD_DEBOUNCE_NS);
                }
            }

            @Override
            public void recordFenceAttack(String enemyType) {
                enemyAiDebug.recordFenceAttack(enemyType);
            }

            @Override
            public void recordStuck(String enemyType) {
                enemyAiDebug.recordStuck();
            }
        };
        this.blackGrouseSpawnManager = new BlackGrouseSpawnManager(
                loadedMap,
                buildCollisionManager,
                buildManager,
                player,
                random
        );
        this.wildlifeSpawnManager = new WildlifeSpawnManager(
                loadedMap,
                buildCollisionManager,
                buildManager,
                player,
                random
        );
        this.wolfSpawnManager = new WolfSpawnManager(
                buildCollisionManager,
                this::canWolfOccupy,
                enemyNavigationContext,
                random
        );
        this.buildController = new BuildController(buildManager);
        this.renderer = new Renderer(stage, inputHandler, wallAssetManager);
        this.dialogueFlowService = new GameDialogueFlowService(this.renderer);
        this.renderer.setMapData(loadedMap);
        this.renderer.setWorldBackgroundImage(null);
        this.renderer.setShowBaseCamp(true);
        wireUiCallbacks();

        validateResourceContracts();
        resetWorldPosition();
        seedStartingBuildItems();
        this.hasLoadedSaveSnapshot = hasSavedWorldSnapshot();
        if (mapManager.getCurrentMapType() == MapType.MAIN_MAP) {
            loadMapWoodFences();
        }
        this.lastBuildObjectCount = buildManager.getPlacedObjects().size();
        this.flowFieldManager.markDirty(System.nanoTime(), 0L);
        if (mapManager.getCurrentMapType() == MapType.MAIN_MAP) {
            spawnAmbientWildlife();
        }
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
            case INTRO:
                handleIntroState();
                break;
            case DIALOGUE:
                handleDialogueState();
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
        renderer.setIntroDialogueRunner(introDialogueRunner);
        renderer.setGemRewardAnimation(
                progressionService.isGemRewardAnimationActive(),
                progressionService.getGemRewardAnimationProgress(now)
        );
        SurvivalProgressionService.SkillUnlockInfo activeCelebration = progressionService.getActiveSkillCelebration(now);
        SurvivalProgressionService.FormUnlockInfo activeFormCelebration = progressionService.getActiveFormCelebration(now);
        SurvivalProgressionService.TutorialHintInfo activeTutorialHint = progressionService.getActiveTutorialHint(now);
        ItemUiMeta tutorialMeta = activeTutorialHint == null ? null : renderer.getItemMeta(activeTutorialHint.itemId());
        renderer.setSkillUnlockCelebration(
                activeFormCelebration != null
                        ? activeFormCelebration.title()
                        : activeCelebration == null ? (activeTutorialHint == null ? null : activeTutorialHint.title()) : "LEVEL " + activeCelebration.level() + " UNLOCK",
                activeFormCelebration != null
                        ? activeFormCelebration.description()
                        : activeCelebration == null ? (activeTutorialHint == null ? null : activeTutorialHint.description()) : "Ch\u00fac m\u1eebng! M\u1edf kh\u00f3a k\u1ef9 n\u0103ng m\u1edbi \"" + activeCelebration.name() + "\"",
                activeFormCelebration != null
                        ? activeFormCelebration.previewType()
                        : activeCelebration == null ? null : activeCelebration.attackType(),
                activeFormCelebration != null
                        ? activeFormCelebration.keyHint()
                        : activeCelebration == null ? (activeTutorialHint == null ? null : activeTutorialHint.keyHint()) : null,
                activeFormCelebration == null && activeCelebration == null ? (tutorialMeta == null ? null : tutorialMeta.getImageIcon()) : null,
                activeFormCelebration != null && activeFormCelebration.epic()
        );
        boolean bossMode = mapManager.getCurrentMapType() == MapType.BOSS_MAP;
        String objectiveStatus = progressionService.buildObjectiveStatus(
                bossMode,
                bossManager.buildObjectiveStatus(player),
                player.getLevel(),
                inventory.getAmount("wood"),
                inventory.getAmount("rock"),
                getCurrentFoodAmount()
        );
        double darknessAlpha = bossMode ? 0.0 : dayNightManager.getDarknessAlpha(now);
        boolean isNight = !bossMode && dayNightManager.isNight(now);
        String dayNightPhase = bossMode ? "Boss Chamber" : dayNightManager.getScheduleDebugText(now);
        String timeIcon = bossMode ? "BOSS" : dayNightManager.getTimeIcon(now);
        String timeTitle = bossMode ? "Final Boss" : dayNightManager.getTimeTitle(now);
        String timeClock = bossMode ? "" : dayNightManager.getClockText(now);
        String timeAnnouncement = bossMode ? null : dayNightManager.getAnnouncement(now);
        renderer.setActionCountdown(
                eatingNikuUntilNs > now ? "Eating Niku" : null,
                eatingNikuUntilNs > now ? Math.max(0.0, (eatingNikuUntilNs - now) / 1_000_000_000.0) : 0.0
        );
        renderer.setFightBanner(now < bossFightBannerUntilNs, getBossFightBannerProgress(now));
        renderer.setFinalEndingOverlay(
                finalEndingOverlayActive,
                finalEndingOverlayActive && finalEndingOverlayStartedAtNs > 0L
                        ? Math.max(0.0, Math.min(1.0, (double) (now - finalEndingOverlayStartedAtNs) / 2_400_000_000.0))
                        : 0.0
        );

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
                getCurrentMapDefinition() == null ? null : getCurrentMapDefinition().transitionTrigger(),
                resourceManager.getAllResources(),
                inventory.snapshot(),
                hotbarService.getSelectedIndex(),
                inventory.getAmount(WOOD_FENCE_ITEM_ID),
                bossMode,
                buildManager,
                hotbarService.getItems(),
                arrowProjectiles,
                bossManager.getActiveFireOrbs(),
                bossManager.getFireOrbManager().getFrames(),
                thrownBombs,
                droppedItems,
                explosionEffects,
                fireBombBurnZones,
                floatingDamageTexts,
                screenShakeX,
                screenShakeY,
                screenFlashUntilNs > now ? Math.min(1.0, (screenFlashUntilNs - now) / 180_000_000.0) : 0.0,
                mapManager.getFadeAlpha(),
                darknessAlpha,
                isNight,
                dayNightPhase,
                timeIcon,
                timeTitle,
                timeClock,
                timeAnnouncement,
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

    private GameMapDefinition getCurrentMapDefinition() {
        return mapManager.getCurrentDefinition();
    }

    private boolean isBaseCampActive() {
        GameMapDefinition definition = getCurrentMapDefinition();
        return definition == null || definition.showBaseCamp();
    }

    private boolean isEnemySpawningEnabled() {
        GameMapDefinition definition = getCurrentMapDefinition();
        return definition == null || definition.allowEnemySpawns();
    }

    private boolean isChunkResourceEnabled() {
        GameMapDefinition definition = getCurrentMapDefinition();
        return definition == null || definition.allowChunkResources();
    }

    private boolean isBuildingEnabled() {
        GameMapDefinition definition = getCurrentMapDefinition();
        return definition == null || definition.allowBuilding();
    }

    private void updateMapTransition(long now) {
        mapManager.update(now, definition -> applyMapDefinition(definition, false));
    }

    private void checkMapTransitionTrigger() {
        GameMapDefinition definition = getCurrentMapDefinition();
        if (definition == null) {
            return;
        }
        MapType currentType = mapManager.getCurrentMapType();
        if (currentType == MapType.BOSS_MAP) {
            tryReturnFromBossGate();
            return;
        }
        if (definition.transitionTrigger() == null || definition.triggerTarget() == null) {
            return;
        }
        Rectangle2D trigger = definition.transitionTrigger();
        if (trigger.getWidth() <= 0 || trigger.getHeight() <= 0) {
            return;
        }
        if (!CollisionSystem.intersects(
                player.getCollisionX(),
                player.getCollisionY(),
                player.getCollisionWidth(),
                player.getCollisionHeight(),
                trigger.getMinX(),
                trigger.getMinY(),
                trigger.getWidth(),
                trigger.getHeight())) {
            return;
        }
        if (currentType == MapType.MAIN_MAP) {
            if (!DEBUG_ALLOW_BOSS_ENTRY_WITHOUT_GEMS
                    && inventory.getAmount(SEAL_GEM_ITEM_ID) < REQUIRED_SEAL_GEMS_FOR_BOSS) {
                renderer.showToast("C\u1ea7n \u0111\u1ee7 " + REQUIRED_SEAL_GEMS_FOR_BOSS + " vi\u00ean Ng\u1ecdc Phong \u1ea4n \u0111\u1ec3 v\u00e0o c\u1eeda boss.");
                return;
            }
            mapManager.changeMap(MapType.BOSS_MAP);
        }
    }

    private void tryReturnFromBossGate() {
        if (!bossManager.isVictory()) {
            return;
        }
        if (!bossDefeatDialogueShown || introDialogueRunner != null) {
            return;
        }
        Rectangle2D bossExitTrigger = buildBossExitTrigger();
        if (bossExitTrigger == null) {
            return;
        }
        if (!CollisionSystem.intersects(
                player.getCollisionX(),
                player.getCollisionY(),
                player.getCollisionWidth(),
                player.getCollisionHeight(),
                bossExitTrigger.getMinX(),
                bossExitTrigger.getMinY(),
                bossExitTrigger.getWidth(),
                bossExitTrigger.getHeight())) {
            return;
        }
        pendingBossReturnToMainMapEntrance = true;
        pendingEndingDialogueAfterBossReturn = true;
        mapManager.changeMap(MapType.MAIN_MAP);
    }

    private Rectangle2D buildBossExitTrigger() {
        if (worldWidth <= 0.0 || worldHeight <= 0.0) {
            return null;
        }
        double triggerWidth = Math.min(BOSS_EXIT_TRIGGER_WIDTH, Math.max(96.0, worldWidth * 0.28));
        double triggerHeight = Math.min(BOSS_EXIT_TRIGGER_HEIGHT, Math.max(72.0, worldHeight * 0.22));
        double triggerX = (worldWidth - triggerWidth) * 0.5;
        double triggerY = Math.min(BOSS_EXIT_TRIGGER_Y, Math.max(24.0, worldHeight * 0.08));
        return new Rectangle2D(triggerX, triggerY, triggerWidth, triggerHeight);
    }

    private void spawnPlayerAtBossEntranceReturnPoint() {
        GameMapDefinition mainDefinition = mapManager.getDefinition(MapType.MAIN_MAP);
        Rectangle2D entranceTrigger = mainDefinition == null ? null : mainDefinition.transitionTrigger();
        if (entranceTrigger == null) {
            resetWorldPosition();
            return;
        }
        double gap = Math.max(24.0, Math.max(buildCollisionManager.getTileWidth(), buildCollisionManager.getTileHeight()) * 1.5);
        double preferredX = entranceTrigger.getMinX() + (entranceTrigger.getWidth() - player.getWidth()) * 0.5;
        double preferredY = entranceTrigger.getMaxY() + gap;
        double[] safeSpawn = findNearestSafeSpawn(preferredX, preferredY);
        player.reset(safeSpawn[0], safeSpawn[1]);
        updateCamera();
    }

    private void applyMapDefinition(GameMapDefinition definition, boolean resetMainProgress) {
        if (definition == null) {
            return;
        }

        MapRuntimeData runtimeData = MapRuntimeApplier.prepare(definition, resourceManager);
        MapData loadedMap = runtimeData.mapData();
        mapData = loadedMap;
        mapCollisions.clear();
        mapCollisions.addAll(runtimeData.runtimeObjects());
        worldWidth = runtimeData.worldWidth();
        worldHeight = runtimeData.worldHeight();

        resourceManager.loadFromMapObjects(mapCollisions);
        tileCollisionResolver = runtimeData.tileCollisionResolver();
        buildCollisionManager.reconfigure(loadedMap, mapCollisions, tileCollisionResolver, resourceManager, worldWidth, worldHeight);
        buildManager.clearObjects();
        enemies.clear();
        wolfSpawnManager.clear();
        friendlyArcherManager.clear();
        floatingDamageTexts.clear();
        droppedItems.clear();
        arrowProjectiles.clear();
        thrownBombs.clear();
        explosionEffects.clear();
        fireBombBurnZones.clear();
        openedChest = null;
        renderer.setChestVisible(false);
        renderer.setMapData(loadedMap);
        renderer.setWorldBackgroundImage(definition.backgroundImagePath());
        renderer.setShowBaseCamp(definition.showBaseCamp());
        buildManager.cancelBuildMode();
        player.setBossCombatMode(definition.type() == MapType.BOSS_MAP);
        bossIntroActive = false;
        bossFightBannerUntilNs = -1L;

        if (definition.useBaseCampSpawn()) {
            if (resetMainProgress) {
                dayNightManager.reset(System.nanoTime());
                enemyWaveManager.reset();
            }
            if (definition.type() == MapType.MAIN_MAP && pendingBossReturnToMainMapEntrance) {
                spawnPlayerAtBossEntranceReturnPoint();
                pendingBossReturnToMainMapEntrance = false;
            } else {
                resetWorldPosition();
            }
        } else {
            baseCamp.configure(-10_000, -10_000, 1, 1, DEFAULT_BASE_CAMP_HP);
            baseCamp.setHpForLoad(baseCamp.getMaxHp());
            double[] safeSpawn = findNearestSafeSpawn(definition.spawnX(), definition.spawnY());
            player.reset(safeSpawn[0], safeSpawn[1]);
            updateCamera();
        }
        bossManager.onMapChanged(definition.type(), worldWidth, worldHeight, enemies);
        String bossToast = bossManager.consumePendingToast();
        if (bossToast != null && !bossToast.isBlank()) {
            renderer.showToast(bossToast);
        }
        if (definition.type() == MapType.BOSS_MAP) {
            startBossDialogue();
        } else if (definition.type() == MapType.MAIN_MAP && pendingEndingDialogueAfterBossReturn) {
            pendingEndingDialogueAfterBossReturn = false;
            startEndingDialogue();
        }
    }

    private void resetWorldPosition() {
        MapObjectData baseCampMarker = baseCampLocator.resolveBaseCampMarker(mapData, mapCollisions, DEFAULT_BASE_CAMP_HP);
        if (baseCampMarker != null) {
            configureBaseCampFromMarker(baseCampMarker);
            double[] safeSpawn = baseCampLocator.findPlayerSpawnNearBaseCamp(
                    baseCampMarker,
                    player.getWidth(),
                    player.getHeight(),
                    buildCollisionManager.getTileWidth(),
                    buildCollisionManager.getTileHeight(),
                    this::findNearestSafeSpawn
            );
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

    private void handleIntroState() {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.SPACE)) {
            if (!renderer.isIntroPageFullyRevealed(System.nanoTime())) {
                renderer.revealIntroPageImmediately();
                return;
            }
            advanceOpeningIntro();
        }
    }

    private void handleDialogueState() {
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.SPACE)) {
            if (!renderer.isIntroPageFullyRevealed(System.nanoTime())) {
                renderer.revealIntroPageImmediately();
                return;
            }
            advanceOpeningIntro();
        }
    }

    private boolean handlePlayingState(long now) {
        updateMapTransition(now);
        if (mapManager.isTransitioning()) {
            updateCamera();
            return false;
        }
        if (inputHandler.isJustPressed(KeyCode.F11)) {
            renderer.toggleFullscreen();
        }
        if (inputHandler.isJustPressed(KeyCode.M)) {
            renderer.toggleMinimap();
        }
        if (inputHandler.isJustPressed(KeyCode.B)) {
            renderer.toggleShop();
        }
        if (inputHandler.isJustPressed(KeyCode.I) && mapManager.getCurrentMapType() != MapType.BOSS_MAP) {
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
        // - MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¾ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ lÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â²ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â·ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â®ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â´ bÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â²ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â·ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â®ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³ Q se doi huong N -> E -> S -> W.
        // - Preview se cap nhat ngay sau do trong cung frame.
        if (!isBuildingEnabled()) {
            buildManager.cancelBuildMode();
        }
        boolean qJustPressed = inputHandler.isJustPressed(KeyCode.Q);
        boolean qPressed = inputHandler.isPressed(KeyCode.Q);
        String selectedHotbarItemId = getSelectedHotbarItemId();
        boolean mouseOverUi = renderer.isMouseOverUi(inputHandler.getMouseX(), inputHandler.getMouseY());
        boolean blockingOverlayVisible = renderer.isBlockingOverlayVisible();
        if (!blockingOverlayVisible && NIKU_ITEM_ID.equals(selectedHotbarItemId) && qJustPressed) {
            startEatingNiku(now);
        } else if (qJustPressed && !blockingOverlayVisible) {
            if (FIRE_BOMB_ITEM_ID.equals(selectedHotbarItemId) || BOMB_TRAP_ITEM_ID.equals(selectedHotbarItemId)) {
                throwSelectedThrowableBomb(selectedHotbarItemId, now);
            } else if (isBuildingEnabled() && isBuildItemId(selectedHotbarItemId)) {
                buildController.onRotatePressed();
            }
        }
        if (renderer.isChestVisible() && findNearestUsableChest(player.getCenterX(), player.getCenterY(), CHEST_INTERACT_RANGE) == null) {
            closeChestOverlay();
        }
        // Build preview:
        // - Chuyen mouse screen-space sang world-space qua camera/zoom.
        // - Snap ve grid de preview va wall that nam dung tren tile map.
        // - Chuot de len UI thi an preview de khong dat nham vao hotbar/minimap/HUD.
        long previewStartNs = System.nanoTime();
        if (isBuildingEnabled()) {
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
        }
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

        boolean bossMode = mapManager.getCurrentMapType() == MapType.BOSS_MAP;
        boolean bossCombatLocked = isBossCombatLocked(now);
        boolean moveLeft = !bossCombatLocked && !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.A);
        boolean moveRight = !bossCombatLocked && !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.D);
        boolean moveUp = !bossCombatLocked && !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.W);
        boolean moveDown = !bossCombatLocked && !blockingOverlayVisible && !player.isAttacking() && inputHandler.isPressed(KeyCode.S);
        boolean movingByInput = moveLeft || moveRight || moveUp || moveDown;
        updateNikuEating(now, qPressed, selectedHotbarItemId, blockingOverlayVisible);
        // SPACE + WASD => Running, con WASD thuong => Walking.
        boolean sprinting = !bossMode
                && movingByInput
                && inputHandler.isPressed(KeyCode.SPACE)
                && player.getEnergy() > 0.01;
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

        if (bossMode && !bossCombatLocked) {
            handleBossMapCombatInput(now, blockingOverlayVisible, hotbarClickConsumed, mouseOverUi, skipWorldPrimaryClickThisFrame);
        } else if (!bossMode
                && !suppressWorldPrimaryUntilMouseRelease
                && !skipWorldPrimaryClickThisFrame
                && !blockingOverlayVisible
                && !hotbarClickConsumed
                && inputHandler.isMouseLeftJustClicked()
                && !mouseOverUi) {
            String selectedItemId = getSelectedHotbarItemId();
            if (isBuildingEnabled() && buildManager.getBuildMode() == BuildMode.BUILDING) {
                // Dat wall:
                // - Chi dat khi click tren world, khong de len UI.
                // - BuildManager se validate occupied tile/collision truoc khi tao wall.
                // - Dat thanh cong moi tru 1 Wood Fence trong inventory.
                if (buildController.onPrimaryClickPlace(player, inventory)) {
                    refreshBuildInventoryUi();
                    logPlacedBuild(buildManager.getLastPlacedObject());
                }
            } else {
                performPlayerAttack(now, player.getAttackAnimationTypeForCurrentMode());
            }
        } else if (!bossMode && !blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.F)) {
            tryUseUnlockedSkill(now, Player.AttackAnimationType.SLICE, "Ti\u1ec1u phu ch\u00e9m c\u1ee7i", AXE_SKILL_UNLOCK_LEVEL, "F");
        } else if (!bossMode && !blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.J)) {
            performPlayerAttack(now, player.getAttackAnimationTypeForCurrentMode());
        } else if (!bossMode && !blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.L)) {
            tryUseUnlockedSkill(now, Player.AttackAnimationType.CRUSH, "StormBreaker", STORMBREAKER_UNLOCK_LEVEL, "L");
        } else if (!bossMode && !blockingOverlayVisible && inputHandler.isJustPressed(KeyCode.K)) {
            tryUseUnlockedSkill(now, Player.AttackAnimationType.PIERCE, "Ng\u1ecdn gi\u00e1o t\u1eed th\u1ea7n", DEATH_SPEAR_UNLOCK_LEVEL, "K");
        }

        boolean moving = oldX != player.getX() || oldY != player.getY();
        player.updateAnimation(now, moving, moveUp, moveDown, moveLeft, moveRight);
        updateEnergyByMovement(now, moving, sprinting);

        // Chunk map update:
        // - Moi chunk moi vao tam nhin se sinh them resource de world "vo han".
        if (isChunkResourceEnabled()) {
            for (WorldChunk chunk : infiniteWorldManager.updateAndGetNewChunks(player.getX(), player.getY())) {
                injectChunkResources(chunk);
            }
        }

        resourceManager.update(now);
        updateDroppedItemPickup();
        updateDaytimeHealthRecovery(now, bossMode);
        updateObjectiveProgressionV2(now);
        updateShopTutorialHints(now);
        updateGemRewardAnimation(now);
        updateBossNinjaAwakeningFlow(now);
        cleanupExpiredDamageTexts(now);
        cleanupExpiredExplosionEffects(now);
        updateScreenImpulse(now);
        long aiStartNs = System.nanoTime();
        if (bossMode && !bossCombatLocked) {
            bossManager.update(now, player, enemies, worldWidth, worldHeight, this::blocksFireOrb);
        }
        if (bossMode && bossManager.isVictory() && !bossDefeatDialogueShown && introDialogueRunner == null) {
            bossDefeatDialogueShown = true;
            startBossLostDialogue();
            updateCamera();
            return false;
        }
        if (isEnemySpawningEnabled()) {
            updateEnemySpawning(now);
            updateEnemies(now);
            updateFriendlyArchers(now);
            updateArcherTowers(now);
            updateBombTraps(now);
        }
        recordPerfAi(System.nanoTime() - aiStartNs);
        updateThrownBombs(now);
        updateFireBombBurnZones(now);
        if (isBuildingEnabled()) {
            updateArrowProjectiles(now);
        }

        checkMapTransitionTrigger();
        updateMapTransition(now);
        if (mapManager.isTransitioning()) {
            updateCamera();
            return false;
        }

        if (!player.isAlive() || (isBaseCampActive() && !baseCamp.isAlive())) {
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
        if (finalEndingOverlayActive) {
            return;
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
        if (!applyLoadedSaveIfAny()) {
            hasLoadedSaveSnapshot = false;
            renderer.setContinueAvailable(false);
            renderer.showToast("Saved world is unavailable");
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
            startOpeningIntro();
            return;
        }
        gameState = GameState.PLAYING;
    }

    private void startOpeningIntro() {
        introDialogueRunner = dialogueFlowService.startOpeningIntro(player.getPlayerName());
        gameState = GameState.INTRO;
    }

    private void advanceOpeningIntro() {
        if (introDialogueRunner == null) {
            gameState = GameState.PLAYING;
            return;
        }
        String scriptId = introDialogueRunner.getScript().getId();
        if (!introDialogueRunner.advance()) {
            if ("opening_intro".equals(scriptId)) {
                startDayOneDialogue();
                return;
            }
            if ("boss_intro".equals(scriptId)) {
                introDialogueRunner = null;
                bossIntroActive = false;
                bossFightBannerUntilNs = System.nanoTime() + BOSS_FIGHT_BANNER_DURATION_NS;
                gameState = GameState.PLAYING;
                return;
            }
            if ("boss_lost".equals(scriptId)) {
                introDialogueRunner = null;
                gameState = GameState.PLAYING;
                return;
            }
            if (BOSS_NINJA_AWAKENING_SCRIPT_ID.equals(scriptId)) {
                introDialogueRunner = null;
                triggerBossNinjaAwakeningCelebration(System.nanoTime());
                gameState = GameState.PLAYING;
                return;
            }
            if ("ending_open".equals(scriptId)) {
                triggerEndingRumble(System.nanoTime());
                startEndingEpilogueDialogue();
                return;
            }
            if ("ending_epilogue".equals(scriptId)) {
                introDialogueRunner = null;
                startFinalEndingOverlay(System.nanoTime());
                return;
            }
            if ("seal_gem_reward".equals(scriptId)) {
                introDialogueRunner = null;
                activateGemRewardAnimation(System.nanoTime());
                gameState = GameState.PLAYING;
                return;
            }
            introDialogueRunner = null;
            progressionService.unlockDayOneObjectiveChain();
            gameState = GameState.PLAYING;
        }
    }

    private void startDayOneDialogue() {
        introDialogueRunner = dialogueFlowService.startDayOneDialogue(DAY_ONE_DIALOGUE_SCRIPT_PATH, player.getPlayerName());
        gameState = GameState.DIALOGUE;
    }

    private void startSealGemRewardDialogue() {
        introDialogueRunner = dialogueFlowService.startSealGemRewardDialogue(REQUIRED_SEAL_GEMS_FOR_BOSS);
        gameState = GameState.DIALOGUE;
    }

    private void startBossDialogue() {
        introDialogueRunner = dialogueFlowService.startBossDialogue(BOSS_DIALOGUE_SCRIPT_PATH, player.getPlayerName());
        bossIntroActive = true;
        bossFightBannerUntilNs = -1L;
        gameState = GameState.DIALOGUE;
    }

    private void startBossLostDialogue() {
        introDialogueRunner = dialogueFlowService.startBossLostDialogue(BOSS_LOST_DIALOGUE_SCRIPT_PATH, player.getPlayerName());
        gameState = GameState.DIALOGUE;
    }

    private void startBossNinjaAwakeningDialogue() {
        introDialogueRunner = dialogueFlowService.startBossNinjaAwakeningDialogue(player.getPlayerName());
        gameState = GameState.DIALOGUE;
    }

    private void startEndingDialogue() {
        introDialogueRunner = dialogueFlowService.startEndingOpeningDialogue(ENDING_DIALOGUE_SCRIPT_PATH, player.getPlayerName());
        gameState = GameState.DIALOGUE;
    }

    private void startEndingEpilogueDialogue() {
        introDialogueRunner = dialogueFlowService.startEndingEpilogueDialogue(ENDING_DIALOGUE_SCRIPT_PATH);
        gameState = GameState.DIALOGUE;
    }

    private void startFinalEndingOverlay(long now) {
        introDialogueRunner = null;
        finalEndingOverlayActive = true;
        finalEndingOverlayStartedAtNs = now;
        victoryAtNs = -1L;
        renderer.hideToast();
        renderer.setInventoryVisible(false);
        renderer.setShopVisible(false);
        renderer.setChestVisible(false);
        gameState = GameState.LEVEL_COMPLETE;
    }

    private void triggerEndingRumble(long now) {
        cameraShakeUntilNs = Math.max(cameraShakeUntilNs, now + 1_200_000_000L);
        screenFlashUntilNs = Math.max(screenFlashUntilNs, now + 220_000_000L);
    }

    private void triggerBossNinjaAwakeningCelebration(long now) {
        progressionService.triggerBossNinjaAwakeningCelebration(now);
        cameraShakeUntilNs = Math.max(cameraShakeUntilNs, now + 750_000_000L);
        screenFlashUntilNs = Math.max(screenFlashUntilNs, now + 280_000_000L);
        renderer.showToast("M\u1edf kh\u00f3a Ninja chi\u1ebfn tr\u1eadn.");
    }

    private boolean isBossCombatLocked(long now) {
        return mapManager.getCurrentMapType() == MapType.BOSS_MAP
                && (bossIntroActive || now < bossFightBannerUntilNs);
    }

    private double getBossFightBannerProgress(long now) {
        if (now >= bossFightBannerUntilNs) {
            return 0.0;
        }
        long remainingNs = Math.max(0L, bossFightBannerUntilNs - now);
        return 1.0 - Math.max(0.0, Math.min(1.0, (double) remainingNs / BOSS_FIGHT_BANNER_DURATION_NS));
    }

    private void activateGemRewardAnimation(long now) {
        progressionService.activateGemRewardAnimation(now);
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
        String resolvedItemId = shopService.normalizeShopItemId(requestedItemId);
        Map<String, Integer> purchaseCosts = shopService.resolvePurchaseCosts(resolvedItemId);
        if (purchaseCosts.isEmpty()) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "unknown-item");
            renderer.showToast("Unknown item");
            return;
        }

        if (!shopService.hasEnoughResources(inventory, purchaseCosts)) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "not-enough-resources");
            renderer.showToast("Not enough resources");
            return;
        }

        if (!shopService.consumePurchaseCosts(inventory, purchaseCosts)) {
            logShopPurchase(requestedItemId, resolvedItemId, purchaseCosts, false, "consume-failed");
            renderer.showToast("Resource sync failed");
            return;
        }

        if (FRIENDLY_ARCHER_ITEM_ID.equals(resolvedItemId)) {
            boolean spawned = spawnFriendlyArcherNearPlayer();
            if (!spawned) {
                shopService.refundPurchaseCosts(inventory, purchaseCosts);
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
                shopService.refundPurchaseCosts(inventory, purchaseCosts);
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
            shopService.refundPurchaseCosts(inventory, purchaseCosts);
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
        introDialogueRunner = null;
        bossIntroActive = false;
        bossDefeatDialogueShown = false;
        bossFightBannerUntilNs = -1L;
        pendingBossReturnToMainMapEntrance = false;
        pendingEndingDialogueAfterBossReturn = false;
        progressionService.reset();
        lastDayPassiveHealAtNs = 0L;
        dayTwoDaytimeFullHealGranted = false;
        finalEndingOverlayActive = false;
        finalEndingOverlayStartedAtNs = -1L;
        clearPersistentProgressForFreshStart();
        mapManager.setCurrentMap(MapType.MAIN_MAP);
        applyMapDefinition(mapManager.getDefinition(MapType.MAIN_MAP), true);

        // Reset world moi: clear enemy/resource procedural va inventory.
        enemies.clear();
        wolfSpawnManager.clear();
        friendlyArcherManager.clear();
        buildManager.clearObjects();
        inventory.restore(Map.of());
        seedStartingBuildItems();
        hotbarService.setSelectedIndex(0);
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
        // Hoi mau day cho player va nha chinh de thoat khoi vong lap GAME_OVER.
        player.setHpForLoad(player.getMaxHp());
        player.setEnergyForLoad(player.getMaxEnergy());
        baseCamp.setHpForLoad(baseCamp.getMaxHp());
        worldStartedAtNs = System.nanoTime();
        lastEnemySpawnAtNs = 0L;
        nextWallJumperSpawnAtNs = 0L;
        nextGolemSpawnAtNs = 0L;
        lastUpdateNowNs = -1L;
        eatingNikuUntilNs = -1L;
        setSelectedHotbarIndex(0);
        hasLoadedSaveSnapshot = false;
        renderer.setContinueAvailable(false);
        loadMapWoodFences();
        spawnAmbientWildlife();
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
                Game.this.queueNightWolves(count);
            }

            @Override
            public void spawnGolems(int count, GolemEnemy.GolemMode mode) {
                Game.this.queueGolems(count, mode);
            }

            @Override
            public void spawnWallJumpers(int count) {
                Game.this.queueWallJumpers(count);
            }

            @Override
            public void showWarning() {
                renderer.showToast("MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â n ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¹Ãƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âªm sÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯p xuÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¹Ãƒâ€¦Ã¢â‚¬Å“ng. HÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£y dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â±ng rÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â o vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  chuÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©n bÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â© khÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­.");
            }

            @Override
            public void showDawn() {
            }

            @Override
            public void startDawnRetreat() {
                // Spawn da dung o phase DAWN. AI hien tai tiep tuc chay ra/bi despawn khi sang ngay moi.
            }

            @Override
            public void finishDawn() {
                Game.this.despawnHostileEnemiesForMorning();
            }
        });
    }

    private void updateEnemies(long now) {
        int buildObjectCount = buildManager.getPlacedObjects().size();
        if (buildObjectCount != lastBuildObjectCount) {
            flowFieldManager.markDirty(now, FLOW_FIELD_REBUILD_DEBOUNCE_NS);
            lastBuildObjectCount = buildObjectCount;
        }
        processQueuedWaveSpawns();
        flowFieldManager.update(now, baseCamp);
        pathfindingManager.update(now);
        if (debugCollisionOverlayEnabled) {
            enemyAiDebug.flush(now, enemies.size());
        }
        List<Enemy> dead = new ArrayList<>();
        int hostileIndex = 0;
        int hostileCount = countHostileEnemiesForNavigation();
        int navigationStride = Math.max(1, (int) Math.ceil(hostileCount / (double) MAX_FULL_NAV_ENEMIES_PER_FRAME));
        for (Enemy enemy : enemies) {
            if (enemy == null) {
                continue;
            }
            if (enemy.shouldRemoveFromWorld()) {
                handleEnemyDeathDrops(enemy);
                dead.add(enemy);
                continue;
            }
            boolean allowExpensiveNavigation = !enemy.isHostile()
                    || shouldRunFullNavigationThisFrame(hostileIndex, navigationStride);
            if (enemy.isHostile()) {
                hostileIndex++;
            }
            if (enemy instanceof GolemEnemy golemEnemy) {
                golemEnemy.setDebugEnabled(debugCollisionOverlayEnabled);
                golemEnemy.updateAi(now, baseCamp, player, friendlyArcherManager.getArchers(), buildManager.getPlacedObjectsByType(BuildType.ARCHER_TOWER), worldWidth, worldHeight, allowExpensiveNavigation);
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
                WolfEnemy wolfEnemy = (WolfEnemy) enemy;
                wolfEnemy.setDebugEnabled(debugCollisionOverlayEnabled);
                wolfEnemy.updateBehavior(now, dayNightManager.isNight(now), player, baseCamp, friendlyArcherManager.getArchers(), worldWidth, worldHeight, allowExpensiveNavigation);
                if (enemy.shouldRemoveFromWorld()) {
                    handleEnemyDeathDrops(enemy);
                    dead.add(enemy);
                }
                continue;
            }
            if (enemy instanceof FinalBoss finalBoss) {
                finalBoss.setDebugEnabled(debugCollisionOverlayEnabled);
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
        navigationBatchFrame++;
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
            if (debugCollisionOverlayEnabled) {
                System.out.println("ArcherTower attack enemy id " + System.identityHashCode(target));
            }
        }
    }

    private int countHostileEnemiesForNavigation() {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isHostile() && enemy.isAlive() && !enemy.shouldRemoveFromWorld()) {
                count++;
            }
        }
        return count;
    }

    private boolean shouldRunFullNavigationThisFrame(int hostileIndex, int navigationStride) {
        if (navigationStride <= 1) {
            return true;
        }
        long offset = Math.floorMod(navigationBatchFrame, navigationStride);
        return hostileIndex % navigationStride == offset;
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

    private boolean blocksFireOrb(double centerX, double centerY, double radius) {
        double diameter = radius * 2.0;
        double collisionX = centerX - radius;
        double collisionY = centerY - radius;
        return buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, diameter, diameter)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, diameter, diameter)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, diameter, diameter)
                || intersectsPlacedBuildObjectFast(collisionX, collisionY, diameter, diameter);
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
            if (SAMURAI_THROW_ITEM_ID.equals(bomb.getBombItemId())) {
                triggerSamuraiThrowImpact(bomb, now);
            } else if (BOMB_TRAP_ITEM_ID.equals(bomb.getBombItemId())) {
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

    private void triggerSamuraiThrowImpact(ThrownBomb bomb, long now) {
        if (bomb == null) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (distance(bomb.getX(), bomb.getY(), enemy.getCenterX(), enemy.getCenterY()) > bomb.getBlastRadius()) {
                continue;
            }
            DamageResult damageResult = DamageSystem.applyDamage(player, enemy, bomb.getDamage(), now);
            spawnEnemyDamageText(enemy, damageResult, now);
            break;
        }
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

    private BuildObject findBlockingObstacle(Enemy enemy, double targetX, double targetY, int maxRayTiles, int maxNearbyRadiusTiles) {
        if (enemy == null) {
            return null;
        }
        return raycastBlockingObstacle(enemy, targetX, targetY, maxRayTiles);
    }

    private BuildObject raycastBlockingObstacle(Enemy enemy, double targetX, double targetY, int maxRayTiles) {
        int tileWidth = buildCollisionManager.getTileWidth();
        int tileHeight = buildCollisionManager.getTileHeight();
        double startX = enemy.getCenterX();
        double startY = enemy.getCenterY();
        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.001) {
            return null;
        }
        double step = Math.max(6.0, Math.min(Math.max(tileWidth, tileHeight), Math.max(enemy.getCollisionWidth(), enemy.getCollisionHeight()) * 0.5));
        int maxSteps = Math.min(Math.max(1, maxRayTiles), Math.max(1, (int) Math.ceil(distance / step)));
        for (int i = 1; i <= maxSteps; i++) {
            double t = i / (double) maxSteps;
            double centerX = startX + dx * t;
            double centerY = startY + dy * t;
            double collisionX = centerX - enemy.getCollisionWidth() * 0.5;
            double collisionY = centerY - enemy.getCollisionHeight() * 0.5;
            BuildObject object = findIntersectingAttackableWall(collisionX, collisionY, enemy.getCollisionWidth(), enemy.getCollisionHeight());
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    private BuildObject findIntersectingAttackableWall(double x, double y, double width, double height) {
        BuildObject nearest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        double centerX = x + width * 0.5;
        double centerY = y + height * 0.5;
        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(x, y, width, height)) {
            if (object == null || !object.isAlive() || !isAttackableWallType(object.getType())) {
                continue;
            }
            if (!CollisionSystem.intersects(
                    x,
                    y,
                    width,
                    height,
                    object.getCollisionX(),
                    object.getCollisionY(),
                    object.getCollisionWidth(),
                    object.getCollisionHeight()
            )) {
                continue;
            }
            double distance = distance(centerX, centerY, object.getCenterX(), object.getCenterY());
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = object;
            }
        }
        return nearest;
    }

    private BuildObject findNearbyBlockingObstacle(Enemy enemy, int maxNearbyRadiusTiles) {
        int tileWidth = buildCollisionManager.getTileWidth();
        int tileHeight = buildCollisionManager.getTileHeight();
        double radius = Math.max(1, maxNearbyRadiusTiles) * Math.max(tileWidth, tileHeight);
        BuildObject nearest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                enemy.getCenterX() - radius,
                enemy.getCenterY() - radius,
                radius * 2.0,
                radius * 2.0
        )) {
            if (object == null || !object.isAlive() || !isAttackableWallType(object.getType())) {
                continue;
            }
            double distance = edgeDistanceBetween(enemy, object);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = object;
            }
        }
        return nearest;
    }

    private EnemyObstacleTarget findEscapeObstacle(Enemy enemy, double desiredDirX, double desiredDirY, int searchRadiusTiles) {
        if (enemy == null) {
            return null;
        }
        int tileWidth = buildCollisionManager.getTileWidth();
        int tileHeight = buildCollisionManager.getTileHeight();
        double radius = Math.max(1, searchRadiusTiles) * Math.max(tileWidth, tileHeight);
        double enemyCenterX = enemy.getCenterX();
        double enemyCenterY = enemy.getCenterY();
        double desiredLength = Math.sqrt(desiredDirX * desiredDirX + desiredDirY * desiredDirY);
        double dirX = desiredLength <= 0.001 ? 0.0 : desiredDirX / desiredLength;
        double dirY = desiredLength <= 0.001 ? 0.0 : desiredDirY / desiredLength;

        EnemyObstacleTarget best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                enemyCenterX - radius,
                enemyCenterY - radius,
                radius * 2.0,
                radius * 2.0
        )) {
            if (object == null || !object.isAlive() || !isBreakableEscapeBuildType(object.getType())) {
                continue;
            }
            double score = scoreEscapeObstacle(enemy, dirX, dirY, object.getCenterX(), object.getCenterY(), object.getCollisionX(), object.getCollisionY(), object.getCollisionWidth(), object.getCollisionHeight(), object.getHealth(), 0.0);
            if (score < bestScore) {
                bestScore = score;
                best = EnemyObstacleTarget.forBuild(object);
            }
        }

        for (ResourceNode resource : resourceManager.getAliveResourcesInWorldRect(
                enemyCenterX - radius,
                enemyCenterY - radius,
                radius * 2.0,
                radius * 2.0
        )) {
            if (resource == null || !isBreakableEscapeResource(enemy, resource)) {
                continue;
            }
            double typePenalty = resource.getResourceType() == ResourceType.ROCK && enemy instanceof WolfEnemy ? 28.0 : 0.0;
            double score = scoreEscapeObstacle(enemy, dirX, dirY, resource.getCenterX(), resource.getCenterY(), resource.getCollisionX(), resource.getCollisionY(), resource.getCollisionWidth(), resource.getCollisionHeight(), resource.getCurrentHp(), typePenalty);
            if (score < bestScore) {
                bestScore = score;
                best = EnemyObstacleTarget.forResource(resource);
            }
        }
        return best;
    }

    private boolean damageEnemyWall(Enemy enemy, BuildObject wall, long now) {
        if (enemy == null || wall == null || !wall.isAlive()) {
            return false;
        }
        BuildDamageResult hitResult = buildManager.damageObject(wall, enemy.getDamage(), now);
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
            flowFieldManager.markDirty(now, FLOW_FIELD_DESTROY_REBUILD_DEBOUNCE_NS);
        }
        return true;
    }

    private boolean damageEnemyObstacle(Enemy enemy, EnemyObstacleTarget obstacle, long nowNs) {
        if (enemy == null || obstacle == null || !obstacle.isAlive()) {
            return false;
        }
        if (obstacle.isBuildObject()) {
            return damageEnemyWall(enemy, obstacle.buildObject(), nowNs);
        }
        ResourceNode resource = obstacle.resourceNode();
        if (resource == null || !resource.isAlive() || !isBreakableEscapeResource(enemy, resource)) {
            return false;
        }
        if (!isEnemyWithinObstacleAttackRange(enemy, resource)) {
            return false;
        }
        int obstacleDamage = resolveEnemyResourceDamage(enemy, resource);
        if (obstacleDamage <= 0) {
            return false;
        }
        ResourceHitResult hitResult = resourceManager.hitResource(resource.getObjectId(), obstacleDamage, nowNs);
        if (hitResult == null) {
            return false;
        }
        spawnResourceDamageText(hitResult, nowNs);
        if (hitResult.isDestroyed()) {
            if (hitResult.getResourceNode() != null) {
                onResourceDestroyed(hitResult.getResourceNode(), false);
            }
            flowFieldManager.markDirty(nowNs, FLOW_FIELD_DESTROY_REBUILD_DEBOUNCE_NS);
        }
        return true;
    }

    private boolean damageWolfBase(WolfEnemy enemy, BaseCamp targetBaseCamp, long now) {
        return false;
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

    private boolean isBreakableEscapeBuildType(BuildType type) {
        return isAttackableWallType(type) || type == BuildType.DOOR;
    }

    private boolean isBreakableEscapeResource(Enemy enemy, ResourceNode resource) {
        if (resource == null || !resource.isAlive()) {
            return false;
        }
        if (enemy instanceof WolfEnemy) {
            return false;
        }
        return switch (resource.getResourceType()) {
            case TREE -> true;
            case ROCK -> enemy instanceof GolemEnemy;
            case GRASS -> resource.getKind().toLowerCase().contains("bush");
            case UNKNOWN -> resource.getKind().toLowerCase().contains("bush");
            default -> false;
        };
    }

    private double scoreEscapeObstacle(Enemy enemy,
                                       double dirX,
                                       double dirY,
                                       double centerX,
                                       double centerY,
                                       double collisionX,
                                       double collisionY,
                                       double collisionWidth,
                                       double collisionHeight,
                                       int currentHp,
                                       double typePenalty) {
        double offsetX = centerX - enemy.getCenterX();
        double offsetY = centerY - enemy.getCenterY();
        double distance = Math.max(0.0, Math.sqrt(offsetX * offsetX + offsetY * offsetY));
        double dot = dirX == 0.0 && dirY == 0.0 ? 1.0 : ((offsetX * dirX) + (offsetY * dirY)) / Math.max(0.001, Math.sqrt(offsetX * offsetX + offsetY * offsetY));
        double anglePenalty = dirX == 0.0 && dirY == 0.0 ? 0.0 : (1.0 - Math.max(-1.0, dot)) * 26.0;
        double hpPenalty = Math.max(0, currentHp) * 2.0;
        double directBlockBonus = intersectsRect(
                enemy.getCenterX() + dirX * 10.0,
                enemy.getCenterY() + dirY * 10.0,
                Math.max(8.0, enemy.getCollisionWidth()),
                Math.max(8.0, enemy.getCollisionHeight()),
                collisionX,
                collisionY,
                collisionWidth,
                collisionHeight
        ) ? -18.0 : 0.0;
        return distance + anglePenalty + hpPenalty + typePenalty + directBlockBonus;
    }

    private boolean isEnemyWithinObstacleAttackRange(Enemy enemy, ResourceNode resource) {
        double padding = enemy instanceof GolemEnemy ? 44.0 : 48.0;
        return intersectsRect(
                enemy.getCollisionX() - padding,
                enemy.getCollisionY() - padding,
                enemy.getCollisionWidth() + padding * 2.0,
                enemy.getCollisionHeight() + padding * 2.0,
                resource.getCollisionX(),
                resource.getCollisionY(),
                resource.getCollisionWidth(),
                resource.getCollisionHeight()
        );
    }

    private int resolveEnemyResourceDamage(Enemy enemy, ResourceNode resource) {
        if (enemy instanceof GolemEnemy) {
            return resource.getResourceType() == ResourceType.ROCK ? 3 : 4;
        }
        if (enemy instanceof WolfEnemy) {
            return resource.getResourceType() == ResourceType.ROCK ? 1 : 2;
        }
        return Math.max(1, enemy.getDamage());
    }

    private boolean intersectsRect(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
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

    private void handleBossMapCombatInput(long now,
                                          boolean blockingOverlayVisible,
                                          boolean hotbarClickConsumed,
                                          boolean mouseOverUi,
                                          boolean skipWorldPrimaryClickThisFrame) {
        if (blockingOverlayVisible) {
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.J)) {
            performPlayerAttack(now, Player.AttackAnimationType.ATTACK_TWO);
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.SPACE)) {
            if (!consumeBossSkillEnergy(BOSS_SKILL_ENERGY_COST, "Jump")) {
                return;
            }
            performBossJump(now);
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.I)) {
            if (!consumeBossSkillEnergy(BOSS_SKILL_ENERGY_COST, "Dash Attack")) {
                return;
            }
            performBossDashAttack(now);
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.K)) {
            if (!consumeBossSkillEnergy(BOSS_SKILL_ENERGY_COST, "Defend")) {
                return;
            }
            performPlayerAttack(now, Player.AttackAnimationType.DEFEND);
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.U)) {
            if (!consumeBossSkillEnergy(BOSS_STRONG_ATTACK_ENERGY_COST, "Strong Attack")) {
                return;
            }
            performPlayerAttack(now, Player.AttackAnimationType.STRONG_ATTACK);
            return;
        }
        if (inputHandler.isJustPressed(KeyCode.L)) {
            if (!consumeBossSkillEnergy(BOSS_SKILL_ENERGY_COST, "Throw")) {
                return;
            }
            performBossThrow(now);
        }
    }

    private boolean consumeBossSkillEnergy(double amount, String skillName) {
        if (player.consumeEnergy(amount)) {
            return true;
        }
        if (skillName != null && !skillName.isBlank()) {
            renderer.showToast(skillName + " needs more energy");
        }
        return false;
    }

    private void performBossDashAttack(long now) {
        if (player.isAttacking()) {
            return;
        }
        double dashDistance = player.getWidth() * 0.95;
        movePlayerWithSliding(player.isFacingRight() ? dashDistance : -dashDistance, 0.0);
        performPlayerAttack(now, Player.AttackAnimationType.DASH_ATTACK);
    }

    private void performBossJump(long now) {
        if (player.isAttacking()) {
            return;
        }
        player.grantInvulnerability(now + BOSS_JUMP_IFRAME_NS);
        movePlayerWithSliding(player.isFacingRight() ? BOSS_JUMP_DISTANCE : -BOSS_JUMP_DISTANCE, 0.0);
        performPlayerAttack(now, Player.AttackAnimationType.JUMP);
    }

    private void performBossThrow(long now) {
        if (player.isAttacking()) {
            return;
        }
        double sourceX = player.getCenterX();
        double sourceY = player.getCenterY() - player.getHeight() * 0.12;
        double direction = player.isFacingRight() ? 1.0 : -1.0;
        double targetX = sourceX + direction * SAMURAI_THROW_RANGE;
        double targetY = sourceY;
        thrownBombs.add(new ThrownBomb(
                SAMURAI_THROW_ITEM_ID,
                sourceX,
                sourceY,
                targetX,
                targetY,
                SAMURAI_THROW_SPEED,
                SAMURAI_THROW_BLAST_RADIUS,
                SAMURAI_THROW_DAMAGE,
                now,
                SAMURAI_THROW_LIFETIME_NS,
                SAMURAI_THROW_RENDER_SIZE
        ));
        performPlayerAttack(now, Player.AttackAnimationType.THROW);
    }

    private void performPlayerAttack(long now, Player.AttackAnimationType attackType) {
        if (!player.startAttack(now, attackType)) {
            return;
        }

        int attackDamage = switch (attackType) {
            case SLICE -> 2;
            case CRUSH -> 3;
            case PIERCE -> 4;
            case HIT -> 1;
            case ATTACK_TWO -> 5;
            case DASH_ATTACK -> 7;
            case THROW -> 6;
            case STRONG_ATTACK -> 11;
            case DEFEND, JUMP -> 0;
        };
        if (attackDamage <= 0) {
            return;
        }
        double[] attackBox = player.buildAttackHitbox();

        boolean hitEnemy = applyAttackToFirstEnemy(attackBox[0], attackBox[1], attackBox[2], attackBox[3], attackDamage, now);
        if (hitEnemy) {
            return;
        }

        if (player.isBossCombatMode()) {
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

    private boolean startEatingNiku(long now) {
        if (eatingNikuUntilNs > now) {
            return false;
        }
        if (inventory.getAmount(NIKU_ITEM_ID) <= 0) {
            renderer.showToast("No niku");
            return false;
        }
        eatingNikuUntilNs = now + NIKU_EAT_DURATION_NS;
        renderer.showToast("Eating Niku...");
        return true;
    }

    private void updateNikuEating(long now, boolean qPressed, String selectedHotbarItemId, boolean blockingOverlayVisible) {
        if (eatingNikuUntilNs <= 0L) {
            return;
        }
        if (!qPressed || blockingOverlayVisible || !NIKU_ITEM_ID.equals(selectedHotbarItemId)) {
            eatingNikuUntilNs = -1L;
            renderer.showToast("Eating canceled");
            return;
        }
        if (now < eatingNikuUntilNs) {
            return;
        }
        eatingNikuUntilNs = -1L;
        if (!inventory.consumeItem(NIKU_ITEM_ID, 1)) {
            renderer.showToast("Cannot eat niku");
            return;
        }
        player.recoverEnergy(FOOD_ENERGY_BONUS);
        refreshBuildInventoryUi();
        renderer.showToast("Ate Niku +" + (int) FOOD_ENERGY_BONUS + " EN");
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

    private void queueNightWolves(int count) {
        queuedNightWolfSpawns += Math.max(0, count);
    }

    private void queueGolems(int count, GolemEnemy.GolemMode mode) {
        if (mode == GolemEnemy.GolemMode.WALL_BREAKER) {
            queuedWallBreakerGolemSpawns += Math.max(0, count);
            return;
        }
        queuedNormalGolemSpawns += Math.max(0, count);
    }

    private void queueWallJumpers(int count) {
        queuedWallJumperSpawns += Math.max(0, count);
    }

    private void processQueuedWaveSpawns() {
        int spawnedThisFrame = 0;
        while (spawnedThisFrame < MAX_WAVE_SPAWNS_PER_FRAME && queuedNightWolfSpawns > 0) {
            spawnWaveWolves(1);
            queuedNightWolfSpawns--;
            spawnedThisFrame++;
        }
        while (spawnedThisFrame < MAX_WAVE_SPAWNS_PER_FRAME && queuedNormalGolemSpawns > 0) {
            GolemEnemy spawned = spawnGolemEnemy(GolemEnemy.GolemMode.NORMAL);
            queuedNormalGolemSpawns--;
            spawnedThisFrame++;
            if (spawned != null) {
                enemies.add(spawned);
            }
        }
        while (spawnedThisFrame < MAX_WAVE_SPAWNS_PER_FRAME && queuedWallBreakerGolemSpawns > 0) {
            GolemEnemy spawned = spawnGolemEnemy(GolemEnemy.GolemMode.WALL_BREAKER);
            queuedWallBreakerGolemSpawns--;
            spawnedThisFrame++;
            if (spawned != null) {
                enemies.add(spawned);
            }
        }
        while (spawnedThisFrame < MAX_WAVE_SPAWNS_PER_FRAME && queuedWallJumperSpawns > 0) {
            WallJumperEnemy spawned = spawnWallJumperEnemy();
            queuedWallJumperSpawns--;
            spawnedThisFrame++;
            if (spawned != null) {
                enemies.add(spawned);
            }
        }
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
        queuedNightWolfSpawns = 0;
        queuedNormalGolemSpawns = 0;
        queuedWallBreakerGolemSpawns = 0;
        queuedWallJumperSpawns = 0;
    }

    private GolemEnemy spawnGolemEnemy() {
        return spawnGolemEnemy(GolemEnemy.GolemMode.NORMAL);
    }

    private GolemEnemy spawnGolemEnemy(GolemEnemy.GolemMode mode) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double[] spawn = randomEdgeSpawnPoint(GolemEnemy.RENDER_WIDTH, GolemEnemy.RENDER_HEIGHT, 144.0);
            GolemEnemy enemy = new GolemEnemy(
                    spawn[0],
                    spawn[1],
                    this::canGolemOccupy,
                    enemyNavigationContext,
                    mode
            );
            enemy.setInitialPathDelayNs((long) (random.nextDouble() * 1_000_000_000L));
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
                    WallJumperEnemy.defaultRenderHeight(tileSize),
                    132.0
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

        MapType savedMapType = MapType.MAIN_MAP;
        Object rawMapType = save.get("mapType");
        if (rawMapType instanceof String mapTypeName) {
            try {
                savedMapType = MapType.valueOf(mapTypeName);
            } catch (IllegalArgumentException ignored) {
                savedMapType = MapType.MAIN_MAP;
            }
        }
        GameMapDefinition definition = mapManager.getDefinition(savedMapType);
        if (definition != null && savedMapType != mapManager.getCurrentMapType()) {
            mapManager.setCurrentMap(savedMapType);
            applyMapDefinition(definition, false);
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
        if (mapManager.getCurrentMapType() == MapType.MAIN_MAP) {
            // Initial map fences are not serialized, so restore them after loading saved player-built objects.
            loadMapWoodFences();
        }
        droppedItems.clear();
        droppedItems.addAll(WorldSnapshotHelper.restoreDroppedItems(save.get("droppedItems")));
        refreshBuildInventoryUi();
        lastBuildObjectCount = buildManager.getPlacedObjects().size();

        long elapsedNs = WorldSaveService.toLong(save.get("elapsedNs"), 0L);
        worldStartedAtNs = System.nanoTime() - Math.max(0L, elapsedNs);

        eventBus.publish(new GameEvent(GameEventType.WORLD_LOADED, Map.of("file", SURVIVAL_SAVE_FILE)));
        return true;
    }

    private boolean hasSavedWorldSnapshot() {
        return worldSaveService.load() != null;
    }

    private void saveWorldSnapshot() {
        Map<String, Object> snapshot = WorldSnapshotHelper.createSnapshot(
                mapManager.getCurrentMapType(),
                player.getX(),
                player.getY(),
                player.getHp(),
                player.getEnergy(),
                baseCamp.getHp(),
                System.nanoTime() - worldStartedAtNs,
                inventory,
                new ArrayList<>(buildManager.exportSaveData()),
                droppedItems
        );

        if (worldSaveService.save(snapshot)) {
            hasLoadedSaveSnapshot = true;
            renderer.setContinueAvailable(true);
            eventBus.publish(new GameEvent(GameEventType.WORLD_SAVED, Map.of("file", SURVIVAL_SAVE_FILE)));
        }
    }

    private void updateCamera() {
        double cameraZoom = renderer.getGameplayZoom(worldWidth, worldHeight);
        double viewWidth = renderer.getViewportWidth() / cameraZoom;
        double viewHeight = renderer.getViewportHeight() / cameraZoom;

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
        return true;
    }

    private boolean intersectsBaseCampCollision(double x, double y, double width, double height) {
        return isBaseCampActive()
                && baseCamp != null
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
        return true;
    }

    private boolean canEnemyPathOccupy(Enemy enemy, double x, double y, double width, double height) {
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
        return !buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                && !buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                && !buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)
                && !intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight);
    }

    private boolean isFlowFieldBlockedTile(int tileX, int tileY) {
        int tileWidth = buildCollisionManager.getTileWidth();
        int tileHeight = buildCollisionManager.getTileHeight();
        double x = tileX * tileWidth;
        double y = tileY * tileHeight;
        if (buildCollisionManager.isBlockedByStaticObjects(x, y, tileWidth, tileHeight)
                || buildCollisionManager.isBlockedByTerrain(x, y, tileWidth, tileHeight)
                || buildCollisionManager.isBlockedByWater(x, y, tileWidth, tileHeight)) {
            return true;
        }
        BuildObject object = buildManager.getPlacedObjectAt(tileX, tileY);
        return object != null && object.isAlive();
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
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)
                || intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
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
        if (buildCollisionManager.isBlockedByStaticObjects(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByTerrain(collisionX, collisionY, collisionWidth, collisionHeight)
                || buildCollisionManager.isBlockedByWater(collisionX, collisionY, collisionWidth, collisionHeight)
                || intersectsPlacedBuildObjectFast(collisionX, collisionY, collisionWidth, collisionHeight)) {
            return false;
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

    private void spawnAmbientWildlife() {
        blackGrouseSpawnManager.spawnInitialFlock(enemies);
        wildlifeSpawnManager.spawnInitialWildlife(enemies);
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
        return randomEdgeSpawnPoint(objectWidth, objectHeight, 12.0);
    }

    private double[] randomEdgeSpawnPoint(double objectWidth, double objectHeight, double edgeInset) {
        int side = random.nextInt(4);
        double margin = Math.max(0.0, edgeInset);
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
    private void updateGemRewardAnimation(long now) {
        if (progressionService.shouldStartSealGemRewardDialogue(
                dayNightManager.getDay(now),
                dayNightManager.getPhase(now) == DayNightManager.Phase.DAY,
                inventory.getAmount(SEAL_GEM_ITEM_ID),
                REQUIRED_SEAL_GEMS_FOR_BOSS
        )) {
            startSealGemRewardDialogue();
            return;
        }
        if (progressionService.updateGemRewardAnimation(now)) {
            inventory.addItem(SEAL_GEM_ITEM_ID, 1);
            refreshBuildInventoryUi();
            renderer.showToast("Nh\u1eadn \u0111\u01b0\u1ee3c 1 vi\u00ean Ng\u1ecdc Phong \u1ea4n.");
        }
    }

    private void updateBossNinjaAwakeningFlow(long now) {
        if (progressionService.shouldTriggerBossNinjaAwakening(
                mapManager.getCurrentMapType() == MapType.MAIN_MAP,
                inventory.getAmount(SEAL_GEM_ITEM_ID),
                REQUIRED_SEAL_GEMS_FOR_BOSS,
                introDialogueRunner != null
        )) {
            startBossNinjaAwakeningDialogue();
        }
    }

    private void updateDaytimeHealthRecovery(long now, boolean bossMode) {
        if (bossMode || !isBaseCampActive() || !baseCamp.isAlive()) {
            return;
        }
        int currentDay = dayNightManager.getDay(now);
        DayNightManager.Phase phase = dayNightManager.getPhase(now);

        if (phase == DayNightManager.Phase.DAY) {
            if (lastDayPassiveHealAtNs <= 0L) {
                lastDayPassiveHealAtNs = now;
            } else if (now - lastDayPassiveHealAtNs >= DAY_PASSIVE_HEAL_INTERVAL_NS) {
                baseCamp.heal(DAY_PASSIVE_HEAL_AMOUNT);
                lastDayPassiveHealAtNs = now;
            }
            return;
        }

        lastDayPassiveHealAtNs = 0L;
        if (!dayTwoDaytimeFullHealGranted
                && currentDay == 2
                && phase == DayNightManager.Phase.WARNING) {
            baseCamp.setHpForLoad(baseCamp.getMaxHp());
            dayTwoDaytimeFullHealGranted = true;
            renderer.showToast("L\u1ec1u ch\u00ednh \u0111\u00e3 \u0111\u01b0\u1ee3c gia c\u1ed1, HP h\u1ed3i \u0111\u1ea7y.");
        }
    }
    private void updateObjectiveProgressionV2(long now) {
        SurvivalProgressionService.ObjectiveCompletion completion = progressionService.advanceObjectiveIfReady(
                now,
                inventory.getAmount("wood"),
                inventory.getAmount("rock"),
                getCurrentFoodAmount()
        );
        if (completion != null) {
            inventory.addItem(COIN_ITEM_ID, progressionService.getObjectiveRewardGold());
            player.addExperience(player.getExperienceToNextLevel());
            refreshBuildInventoryUi();
            renderer.showToast(progressionService.finalizeObjectiveReward(completion.stepTitle(), now, player.getLastLeveledUpTo()));
        }
    }

    private int getCurrentFoodAmount() {
        return Math.max(0, inventory.getAmount(NIKU_ITEM_ID)) + Math.max(0, inventory.getAmount(CARROT_ITEM_ID));
    }

    private void updateShopTutorialHints(long now) {
        progressionService.updateShopTutorialHints(now);
    }

    private void tryUseUnlockedSkill(long now,
                                     Player.AttackAnimationType attackType,
                                     String skillName,
                                     int unlockLevel,
                                     String keyLabel) {
        if (!progressionService.isSkillUnlocked(player.getLevel(), unlockLevel)) {
            renderer.showToast("M\u1edf kh\u00f3a " + skillName + " \u1edf level " + unlockLevel + " [" + keyLabel + "]");
            return;
        }
        if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
            performPlayerAttack(now, attackType);
        }
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
        addCornerFenceMarkers(fenceTiles, perimeter.left(), perimeter.top(), 1, 1);
        addCornerFenceMarkers(fenceTiles, perimeter.right(), perimeter.top(), -1, 1);
        addCornerFenceMarkers(fenceTiles, perimeter.left(), perimeter.bottom(), 1, -1);
        addCornerFenceMarkers(fenceTiles, perimeter.right(), perimeter.bottom(), -1, -1);

        for (String key : fenceTiles) {
            String[] parts = key.split(":");
            if (parts.length != 2) {
                continue;
            }
            addMapWoodFence(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    private void addCornerFenceMarkers(Set<String> fenceTiles, int cornerX, int cornerY, int stepX, int stepY) {
        if (fenceTiles == null) {
            return;
        }
        int armLength = Math.max(1, INITIAL_CORNER_FENCE_ARM_TILES);
        for (int index = 0; index < armLength; index++) {
            fenceTiles.add((cornerX + stepX * index) + ":" + cornerY);
            fenceTiles.add(cornerX + ":" + (cornerY + stepY * index));
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
        hotbarService.setSelectedIndex(slotIndex);
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
        buildManager.syncToolbar(inventory.snapshot());
        hotbarService.refresh(inventory.snapshot(), renderer::getItemMeta, this::isBuildItemId);
        syncSelectedHotbarMode();
    }

    private void syncSelectedHotbarMode() {
        String selectedItemId = getSelectedHotbarItemId();
        if (selectedItemId == null || selectedItemId.isBlank()) {
            buildManager.cancelBuildMode();
            player.setEquipmentMode(Player.EquipmentMode.HAND_MODE);
            return;
        }
        player.setEquipmentMode(AXE_ITEM_ID.equals(selectedItemId)
                && progressionService.isSkillUnlocked(player.getLevel(), AXE_SKILL_UNLOCK_LEVEL)
                ? Player.EquipmentMode.AXE_MODE
                : Player.EquipmentMode.HAND_MODE);
        if (isBuildItemId(selectedItemId)) {
            buildController.onSelectBuildItem(selectedItemId);
            return;
        }
        buildManager.cancelBuildMode();
    }

    private String getSelectedHotbarItemId() {
        return hotbarService.getSelectedItemId();
    }

    private boolean isBuildItemId(String itemId) {
        if (BOMB_TRAP_ITEM_ID.equals(itemId) || FIRE_BOMB_ITEM_ID.equals(itemId)) {
            return false;
        }
        return buildManager.getRegistry().findByItemId(itemId) != null;
    }

    private void onResourceDestroyed(system.resource.ResourceNode resource) {
        onResourceDestroyed(resource, true);
    }

    private void onResourceDestroyed(system.resource.ResourceNode resource, boolean dropRewards) {
        if (resource == null) {
            return;
        }
        if (DEBUG_DROP_LOGS) {
            System.out.println("Resource destroyed at: " + resource.getCenterX() + ", " + resource.getCenterY());
        }
        if (!dropRewards) {
            return;
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
                if (enemy.isHostile()) {
                    progressionService.recordEnemyKill();
                }
        List<DropSpec> dropTable = enemy.isHostile() ? ENEMY_DROP_TABLE : ANIMAL_DROP_TABLE;
        enemy.markDeathDropSpawned();
        spawnDropTable(enemy.getCenterX(), enemy.getCenterY(), dropTable);
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

}





