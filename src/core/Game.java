package core;

import entity.Enemy;
import entity.OrcEnemy;
import entity.Player;
import entity.SkeletonEnemy;
import input.InputHandler;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import map.MapData;
import map.MapObjectData;
import map.TileCollisionResolver;
import map.TiledMapLoader;
import system.CollisionSystem;
import system.DamageSystem;
import system.level.Level;
import system.level.LevelManager;
import system.level.LevelResult;
import system.DamageResult;
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceManager;
import system.resource.ResourceHitResult;
import system.resource.TileResourceAdapter;
import ui.FloatingDamageText;
import ui.Renderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {
    // Zoom camera cho gameplay. Gia tri nay phai khop voi Renderer
    // de camera va viewport world tinh dung tam.
    private static final double CAMERA_ZOOM = 1.5;

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final Player player;
    private final List<Enemy> enemies;
    private final ResourceManager resourceManager;
    private final DayNightCycle dayNightCycle;
    private final Map<String, Integer> collectedResources;
    private final StringBuilder playerNameBuffer;
    private final List<FloatingDamageText> floatingDamageTexts;
    private final Random random;
    // Quan ly danh sach level + tien do da mo khoa/hoan thanh.
    private final LevelManager levelManager;

    private GameState gameState;
    private List<MapObjectData> mapCollisions;
    private TileCollisionResolver tileCollisionResolver;
    private MapData mapData;
    private long lastEnemySpawnAtNs;
    private double cameraX;
    private double cameraY;
    private double worldWidth;
    private double worldHeight;
    private double playerStartX;
    private double playerStartY;
    private int menuIndex;
    private long welcomeFlashUntilNs;
    private int pendingWelcomeAction;
    private long lastUpdateNowNs;
    // selectedLevelId:
    // - level dang duoc focus trong man chon level
    // - khong nhat thiet la level dang choi
    private int selectedLevelId;
    // Moc bat dau runtime cua level hien tai de tinh objective SURVIVE va score.
    private long levelStartedAtNs;
    // Luu ket qua vua clear level de ve overlay LEVEL_COMPLETE.
    private LevelResult lastLevelResult;

    private static final int DAY_ORC_TARGET_MIN = 1;
    private static final int DAY_ORC_TARGET_MAX = 2;
    private static final int DAY_SKELETON_TARGET = 0;
    private static final int NIGHT_ORC_TARGET_MIN = 7;
    private static final int NIGHT_ORC_TARGET_MAX = 8;
    private static final int NIGHT_SKELETON_TARGET_MIN = 7;
    private static final int NIGHT_SKELETON_TARGET_MAX = 8;
    private static final long ENEMY_SPAWN_INTERVAL_NS = 650_000_000L;

    private static final int MENU_PLAY = 0;
    private static final int MENU_GUIDE = 1;
    private static final int MENU_EXIT = 2;
    private static final int MENU_COUNT = 3;

    private static final double MENU_BUTTON_X = 350;
    private static final double MENU_BUTTON_Y_START = 246;
    private static final double MENU_BUTTON_WIDTH = 260;
    private static final double MENU_BUTTON_HEIGHT = 42;
    private static final double MENU_BUTTON_GAP = 58;

    private static final int LEVEL_SELECT_COLUMNS = 2;
    private static final double LEVEL_CARD_X = 145;
    private static final double LEVEL_CARD_Y = 165;
    private static final double LEVEL_CARD_WIDTH = 300;
    private static final double LEVEL_CARD_HEIGHT = 110;
    private static final double LEVEL_CARD_GAP_X = 42;
    private static final double LEVEL_CARD_GAP_Y = 26;

    private static final long WELCOME_FLASH_DURATION_NS = 120_000_000L;
    private static final long WELCOME_TRANSITION_DELAY_NS = 220_000_000L;
    private static final int MAX_PLAYER_NAME_LENGTH = 14;

    private static final double MOVE_ENERGY_DRAIN_PER_SECOND = 2.2;
    private static final double IDLE_ENERGY_REGEN_PER_SECOND = 0.9;
    private static final double SKILL_F_ENERGY_COST = 3.0;
    private static final double FOOD_ENERGY_BONUS = 15.0;

    public Game(Stage stage) {
        this.inputHandler = new InputHandler();
        this.player = new Player(100, 100, 58, 58, 1, 100);
        this.renderer = new Renderer(stage, inputHandler);
        this.gameLoop = new GameLoop(this);
        this.gameState = GameState.WELCOME;
        this.enemies = new ArrayList<>();
        this.resourceManager = new ResourceManager();
        this.dayNightCycle = new DayNightCycle();
        this.collectedResources = new LinkedHashMap<>();
        this.playerNameBuffer = new StringBuilder("Player");
        this.floatingDamageTexts = new ArrayList<>();
        this.random = new Random();
        this.levelManager = LevelManager.getInstance();
        this.mapCollisions = new ArrayList<>();
        this.tileCollisionResolver = null;
        this.mapData = null;
        this.lastEnemySpawnAtNs = 0L;
        this.cameraX = 0;
        this.cameraY = 0;
        this.worldWidth = GameConfig.WORLD_WIDTH;
        this.worldHeight = GameConfig.WORLD_HEIGHT;
        this.playerStartX = 100;
        this.playerStartY = 100;
        this.menuIndex = 0;
        this.welcomeFlashUntilNs = 0L;
        this.pendingWelcomeAction = -1;
        this.lastUpdateNowNs = -1L;
        this.selectedLevelId = 1;
        this.levelStartedAtNs = 0L;
        this.lastLevelResult = null;

        // 1) Load du lieu level tu JSON
        // 2) Load tien do da luu cua nguoi choi
        levelManager.loadLevels("resources/levels/level_data.json");
        levelManager.loadProgress();
        this.selectedLevelId = levelManager.getFirstUnlockedLevelId();

        // Khi vua mo game, world se dung map chinh trong folder Map_Game.
        loadMapIntoWorld(resolveDefaultMapPath());
        recalculatePlayerStartAtWorldCenter();
        player.reset(playerStartX, playerStartY);
        updateCamera();
        dayNightCycle.reset(System.nanoTime());
    }

    public void start() {
        gameLoop.start();
    }

    public void update(long now) {
        if (lastUpdateNowNs < 0) {
            lastUpdateNowNs = now;
        }

        switch (gameState) {
            case GAME_OVER:
                handleGameOverState();
                break;
            case WELCOME:
                handleWelcomeState(now);
                break;
            case NAME_INPUT:
                handleNameInputState();
                break;
            case GUIDE:
                handleGuideState();
                break;
            case LEVEL_SELECT:
                handleLevelSelectState();
                break;
            case LEVEL_COMPLETE:
                handleLevelCompleteState();
                break;
            case PAUSED:
                handlePausedState();
                break;
            case PLAYING:
                if (handlePlayingState(now)) {
                    return;
                }
                break;
            default:
                break;
        }
        inputHandler.update();
    }

    private void handleGameOverState() {
        if (inputHandler.isJustPressed(KeyCode.R)) {
            restartCurrentLevel();
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            openLevelSelect();
        }
    }

    private void handleWelcomeState(long now) {
        updateWelcomeMenuHoverByMouse();

        if (pendingWelcomeAction == -1) {
            if (inputHandler.isJustPressed(KeyCode.UP) || inputHandler.isJustPressed(KeyCode.W)) {
                menuIndex = (menuIndex - 1 + MENU_COUNT) % MENU_COUNT;
            }
            if (inputHandler.isJustPressed(KeyCode.DOWN) || inputHandler.isJustPressed(KeyCode.S)) {
                menuIndex = (menuIndex + 1) % MENU_COUNT;
            }
        }

        if (pendingWelcomeAction == -1 && inputHandler.isMouseLeftJustClicked()) {
            int clickedIndex = findWelcomeMenuIndexAt(inputHandler.getMouseX(), inputHandler.getMouseY());
            if (clickedIndex != -1) {
                menuIndex = clickedIndex;
                welcomeFlashUntilNs = now + WELCOME_FLASH_DURATION_NS;
                pendingWelcomeAction = menuIndex;
            }
        }

        if (inputHandler.isJustPressed(KeyCode.H)) {
            gameState = GameState.GUIDE;
        } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            welcomeFlashUntilNs = now + WELCOME_FLASH_DURATION_NS;
            pendingWelcomeAction = menuIndex;
        }

        long transitionAtNs = welcomeFlashUntilNs + WELCOME_TRANSITION_DELAY_NS;
        if (pendingWelcomeAction != -1 && now >= transitionAtNs) {
            if (pendingWelcomeAction == MENU_PLAY) {
                gameState = GameState.NAME_INPUT;
            } else if (pendingWelcomeAction == MENU_GUIDE) {
                gameState = GameState.GUIDE;
            } else if (pendingWelcomeAction == MENU_EXIT) {
                Platform.exit();
            }
            pendingWelcomeAction = -1;
        }
    }

    private void handleNameInputState() {
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
            pendingWelcomeAction = -1;
            return;
        }

        if (inputHandler.isJustPressed(KeyCode.BACK_SPACE) && playerNameBuffer.length() > 0) {
            playerNameBuffer.deleteCharAt(playerNameBuffer.length() - 1);
        }

        if (inputHandler.isJustPressed(KeyCode.SPACE)
                && playerNameBuffer.length() < MAX_PLAYER_NAME_LENGTH
                && playerNameBuffer.length() > 0
                && playerNameBuffer.charAt(playerNameBuffer.length() - 1) != ' ') {
            playerNameBuffer.append(' ');
        }

        appendTypedCharacters();

        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            player.setPlayerName(normalizePlayerName(playerNameBuffer.toString()));
            openLevelSelect();
        }
    }

    private void handleGuideState() {
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
        } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            openLevelSelect();
        }
    }

    private void handleLevelSelectState() {
        // Level Select chi la UI state:
        // - di chuyen con tro chon level
        // - neu ENTER/click vao level da unlock thi moi vao PLAYING
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
            return;
        }

        if (inputHandler.isJustPressed(KeyCode.LEFT) || inputHandler.isJustPressed(KeyCode.A)) {
            moveLevelSelection(-1, 0);
        }
        if (inputHandler.isJustPressed(KeyCode.RIGHT) || inputHandler.isJustPressed(KeyCode.D)) {
            moveLevelSelection(1, 0);
        }
        if (inputHandler.isJustPressed(KeyCode.UP) || inputHandler.isJustPressed(KeyCode.W)) {
            moveLevelSelection(0, -1);
        }
        if (inputHandler.isJustPressed(KeyCode.DOWN) || inputHandler.isJustPressed(KeyCode.S)) {
            moveLevelSelection(0, 1);
        }

        if (inputHandler.isMouseLeftJustClicked()) {
            int clickedLevelId = findLevelSelectAt(inputHandler.getMouseX(), inputHandler.getMouseY());
            if (clickedLevelId != -1) {
                selectedLevelId = clickedLevelId;
                if (levelManager.getPlayerProgress().isLevelUnlocked(clickedLevelId)) {
                    startSelectedLevel();
                }
            }
        }

        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            startSelectedLevel();
        }
    }

    private void handleLevelCompleteState() {
        if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            selectedLevelId = levelManager.getSuggestedNextLevelId();
            if (!startSelectedLevel()) {
                openLevelSelect();
            }
        } else if (inputHandler.isJustPressed(KeyCode.R)) {
            restartCurrentLevel();
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            openLevelSelect();
        }
    }

    private void handlePausedState() {
        if (inputHandler.isJustPressed(KeyCode.P)) {
            gameState = GameState.PLAYING;
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            openLevelSelect();
        }
    }

    private boolean handlePlayingState(long now) {
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.PAUSED;
            inputHandler.update();
            return true;
        }

        double oldPlayerX = player.getX();
        double oldPlayerY = player.getY();

        boolean moveLeft = !player.isAttacking() && inputHandler.isPressed(KeyCode.A);
        boolean moveRight = !player.isAttacking() && inputHandler.isPressed(KeyCode.D);
        boolean moveUp = !player.isAttacking() && inputHandler.isPressed(KeyCode.W);
        boolean moveDown = !player.isAttacking() && inputHandler.isPressed(KeyCode.S);

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

        if (inputHandler.isPressed(KeyCode.J)) {
            DamageSystem.applyDamage(null, player, 1);
        }
        if (inputHandler.isPressed(KeyCode.K)) {
            player.heal(1);
        }

        if (inputHandler.isMouseLeftJustClicked()) {
            performPlayerAttack(now, Player.AttackAnimationType.HIT);
        } else if (inputHandler.isJustPressed(KeyCode.F)) {
            if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
                performPlayerAttack(now, Player.AttackAnimationType.SLICE);
            }
        }

        resourceManager.update(now);
        updateEnemySpawning(now);
        updateEnemies(now);
        cleanupExpiredDamageTexts(now);
        player.clampPosition(0, 0, worldWidth, worldHeight);

        if (isPlayerCollidingWithMapCollision()) {
            player.setPosition(oldPlayerX, oldPlayerY);
        }

        boolean playerMoving = oldPlayerX != player.getX() || oldPlayerY != player.getY();
        player.updateAnimation(now, playerMoving, moveUp, moveDown, moveLeft, moveRight);
        updateEnergyByMovement(now, playerMoving);

        if (!player.isAlive()) {
            gameState = GameState.GAME_OVER;
        } else {
            checkCurrentLevelCompletion(now);
        }

        updateCamera();
        return false;
    }

    private void updateCamera() {
        double viewWorldWidth = GameConfig.WIDTH / CAMERA_ZOOM;
        double viewWorldHeight = GameConfig.HEIGHT / CAMERA_ZOOM;

        cameraX = player.getX() + player.getWidth() / 2 - viewWorldWidth / 2;
        cameraY = player.getY() + player.getHeight() / 2 - viewWorldHeight / 2;

        if (cameraX < 0) {
            cameraX = 0;
        }
        if (cameraY < 0) {
            cameraY = 0;
        }

        double maxCameraX = worldWidth - viewWorldWidth;
        double maxCameraY = worldHeight - viewWorldHeight;

        if (maxCameraX < 0) {
            maxCameraX = 0;
        }
        if (maxCameraY < 0) {
            maxCameraY = 0;
        }

        if (cameraX > maxCameraX) {
            cameraX = maxCameraX;
        }
        if (cameraY > maxCameraY) {
            cameraY = maxCameraY;
        }
    }

    public void render(long now) {
        boolean welcomeFlashing = now < welcomeFlashUntilNs;
        Level currentLevel = levelManager.getCurrentLevel();
        String objectiveText = currentLevel == null
                ? ""
                : currentLevel.buildObjectiveStatus(collectedResources, Math.max(0L, now - levelStartedAtNs));

        renderer.render(
                gameState,
                player,
                enemies,
                now,
                cameraX,
                cameraY,
                menuIndex,
                welcomeFlashing,
                playerNameBuffer.toString(),
                MAX_PLAYER_NAME_LENGTH,
                resourceManager.getAllResources(),
                collectedResources,
                dayNightCycle.getDarknessAlpha(now),
                dayNightCycle.isNight(now),
                dayNightCycle.getPhaseName(now),
                floatingDamageTexts,
                worldWidth,
                worldHeight,
                levelManager,
                currentLevel,
                selectedLevelId,
                lastLevelResult,
                objectiveText
        );
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public Player getPlayer() {
        return player;
    }

    private void restartGame() {
        recalculatePlayerStartAtWorldCenter();
        player.reset(playerStartX, playerStartY);
        enemies.clear();
        floatingDamageTexts.clear();
        collectedResources.clear();
        resourceManager.loadFromMapObjects(mapCollisions);
        menuIndex = 0;
        pendingWelcomeAction = -1;
        lastEnemySpawnAtNs = 0L;
        lastUpdateNowNs = -1L;
        levelStartedAtNs = System.nanoTime();
        updateCamera();
        dayNightCycle.reset(levelStartedAtNs);
    }

    private void restartCurrentLevel() {
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel == null) {
            openLevelSelect();
            return;
        }
        loadMapIntoWorld(currentLevel.getMapFile());
        restartGame();
        gameState = GameState.PLAYING;
    }

    private void openLevelSelect() {
        selectedLevelId = levelManager.getFirstUnlockedLevelId();
        gameState = GameState.LEVEL_SELECT;
        pendingWelcomeAction = -1;
        menuIndex = 0;
    }

    private boolean startSelectedLevel() {
        // Chon level trong LevelManager truoc.
        // Neu level bi lock thi reject ngay o day.
        if (!levelManager.selectLevel(selectedLevelId)) {
            return false;
        }
        lastLevelResult = null;
        loadCurrentLevelMap();
        restartGame();
        gameState = GameState.PLAYING;
        return true;
    }

    private void loadCurrentLevelMap() {
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel == null) {
            return;
        }
        // Moi level tu JSON se tro toi 1 map file cu the.
        loadMapIntoWorld(currentLevel.getMapFile());
    }

    private void loadMapIntoWorld(String mapPath) {
        // loadMapIntoWorld la diem noi giua:
        // - file TMX tren dia
        // - collision runtime
        // - resource runtime
        // - renderer/map renderer
        MapData loadedMap = null;
        try {
            loadedMap = new TiledMapLoader().load(mapPath);
        } catch (Exception exception) {
            System.out.println("Cannot load " + mapPath + ": " + exception.getMessage());
            if (!"assets/maps/mapdemo.tmx".equalsIgnoreCase(mapPath)) {
                try {
                    loadedMap = new TiledMapLoader().load("assets/maps/mapdemo.tmx");
                } catch (Exception fallbackException) {
                    System.out.println("Cannot load fallback tiled map: " + fallbackException.getMessage());
                }
            }
        }

        this.mapData = loadedMap;
        List<MapObjectData> runtimeCollisionObjects = new ArrayList<>();
        if (loadedMap != null) {
            runtimeCollisionObjects.addAll(loadedMap.getCollisionObjects());
            // Adapter nay quet tile properties trong map de tao resource runtime.
            // Nghia la map artist chi can lam viec trong Tiled, code tu suy ra resource node.
            runtimeCollisionObjects.addAll(new TileResourceAdapter().buildResourceObjects(loadedMap));
            this.worldWidth = loadedMap.getPixelWidth();
            this.worldHeight = loadedMap.getPixelHeight();
        } else {
            this.worldWidth = GameConfig.WORLD_WIDTH;
            this.worldHeight = GameConfig.WORLD_HEIGHT;
        }

        this.mapCollisions = runtimeCollisionObjects;
        this.resourceManager.loadFromMapObjects(this.mapCollisions);
        List<String> contractErrors = new ResourceContractValidator().validate(this.mapCollisions);
        if (!contractErrors.isEmpty()) {
            System.out.println("=== Resource contract warnings ===");
            for (String error : contractErrors) {
                System.out.println(error);
            }
        }
        this.renderer.setMapData(loadedMap);
        this.tileCollisionResolver = loadedMap == null ? null : new TileCollisionResolver(loadedMap, resourceManager);
        recalculatePlayerStartAtWorldCenter();
        updateCamera();
    }

    private String resolveDefaultMapPath() {
        // Uu tien map chinh ma team dang lam trong folder Map_Game.
        return "assets/Map_Game/map.tmx";
    }

    private void recalculatePlayerStartAtWorldCenter() {
        playerStartX = worldWidth / 2 - player.getWidth() / 2;
        playerStartY = worldHeight / 2 - player.getHeight() / 2;
    }

    private void checkCurrentLevelCompletion(long now) {
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel == null) {
            return;
        }

        // Objective duoc tinh ngay trong runtime bang data JSON cua level:
        // - COLLECT: dua theo collectedResources
        // - SURVIVE: dua theo thoi gian choi cua man hien tai
        long elapsedNs = Math.max(0L, now - levelStartedAtNs);
        if (!currentLevel.areObjectivesCompleted(collectedResources, elapsedNs)) {
            return;
        }

        int collectedCount = getCollectedTotalCount();
        LevelResult result = LevelResult.createForCompletion(
                currentLevel,
                elapsedNs / 1_000_000_000L,
                0,
                collectedCount
        );
        levelManager.completeLevel(result);
        lastLevelResult = result;
        selectedLevelId = levelManager.getSuggestedNextLevelId();
        gameState = GameState.LEVEL_COMPLETE;
    }

    private int getCollectedTotalCount() {
        int total = 0;
        for (Integer value : collectedResources.values()) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }

    private void moveLevelSelection(int dx, int dy) {
        List<Level> levels = levelManager.getAllLevels();
        if (levels.isEmpty()) {
            return;
        }

        int currentIndex = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).getId() == selectedLevelId) {
                currentIndex = i;
                break;
            }
        }

        int row = currentIndex / LEVEL_SELECT_COLUMNS;
        int col = currentIndex % LEVEL_SELECT_COLUMNS;
        int newRow = Math.max(0, row + dy);
        int newCol = Math.max(0, Math.min(LEVEL_SELECT_COLUMNS - 1, col + dx));
        int newIndex = newRow * LEVEL_SELECT_COLUMNS + newCol;
        if (newIndex >= levels.size()) {
            newIndex = levels.size() - 1;
        }
        selectedLevelId = levels.get(newIndex).getId();
    }

    private int findLevelSelectAt(double mx, double my) {
        List<Level> levels = levelManager.getAllLevels();
        for (int i = 0; i < levels.size(); i++) {
            int col = i % LEVEL_SELECT_COLUMNS;
            int row = i / LEVEL_SELECT_COLUMNS;
            double x = LEVEL_CARD_X + col * (LEVEL_CARD_WIDTH + LEVEL_CARD_GAP_X);
            double y = LEVEL_CARD_Y + row * (LEVEL_CARD_HEIGHT + LEVEL_CARD_GAP_Y);
            boolean insideX = mx >= x && mx <= x + LEVEL_CARD_WIDTH;
            boolean insideY = my >= y && my <= y + LEVEL_CARD_HEIGHT;
            if (insideX && insideY) {
                return levels.get(i).getId();
            }
        }
        return -1;
    }

    private boolean isPlayerCollidingWithMapCollision() {
        double px = getPlayerCollisionX();
        double py = getPlayerCollisionY();
        double pw = getPlayerCollisionWidth();
        double ph = getPlayerCollisionHeight();

        for (MapObjectData object : mapCollisions) {
            if (!"Collision".equalsIgnoreCase(object.getType())) {
                continue;
            }
            if (isResourceObject(object) && !isResourceAlive(object.getId())) {
                continue;
            }
            if (CollisionSystem.intersects(px, py, pw, ph, object.getX(), object.getY(), object.getWidth(), object.getHeight())) {
                return true;
            }
        }

        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(px, py, pw, ph)) {
            return true;
        }

        return false;
    }

    private double getPlayerCollisionX() {
        return player.getX() + player.getWidth() * 0.22;
    }

    private double getPlayerCollisionY() {
        return player.getY() + player.getHeight() * 0.30;
    }

    private double getPlayerCollisionWidth() {
        return player.getWidth() * 0.56;
    }

    private double getPlayerCollisionHeight() {
        return player.getHeight() * 0.62;
    }

    private String normalizePlayerName(String rawName) {
        if (rawName == null) {
            return "Player";
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return "Player";
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

    private void updateWelcomeMenuHoverByMouse() {
        int hoveredIndex = findWelcomeMenuIndexAt(inputHandler.getMouseX(), inputHandler.getMouseY());
        if (hoveredIndex != -1 && pendingWelcomeAction == -1) {
            menuIndex = hoveredIndex;
        }
    }

    private int findWelcomeMenuIndexAt(double mx, double my) {
        for (int i = 0; i < MENU_COUNT; i++) {
            double top = MENU_BUTTON_Y_START + i * MENU_BUTTON_GAP;
            boolean insideX = mx >= MENU_BUTTON_X && mx <= MENU_BUTTON_X + MENU_BUTTON_WIDTH;
            boolean insideY = my >= top && my <= top + MENU_BUTTON_HEIGHT;
            if (insideX && insideY) {
                return i;
            }
        }
        return -1;
    }

    private void performPlayerAttack(long now, Player.AttackAnimationType attackType) {
        if (!player.startAttack(now, attackType)) {
            return;
        }

        // Thu tu uu tien:
        // 1) Neu hit vao enemy thi dung o combat
        // 2) Neu khong trung enemy thi moi thu pha resource
        int attackDamage = attackType == Player.AttackAnimationType.SLICE ? 2 : 1;
        double[] attackBox = player.buildAttackHitbox();
        boolean hitEnemy = applyAttackToFirstEnemy(attackBox[0], attackBox[1], attackBox[2], attackBox[3], attackDamage, now);
        if (hitEnemy) {
            return;
        }

        ResourceHitResult resourceHitResult = resourceManager.hitFirstResourceIntersecting(
                attackBox[0],
                attackBox[1],
                attackBox[2],
                attackBox[3],
                attackDamage,
                now
        );

        if (resourceHitResult == null) {
            return;
        }
        if (resourceHitResult.getDamageApplied() > 0 && resourceHitResult.getResourceNode() != null) {
            spawnFloatingDamageText(
                    resourceHitResult.getResourceNode().getCenterX(),
                    resourceHitResult.getResourceNode().getY(),
                    resourceHitResult.getDamageApplied(),
                    false,
                    now
            );
        }
        DropResult dropResult = resourceHitResult.getDropResult();
        if (dropResult != null) {
            addCollectedItem(dropResult.getItemId(), dropResult.getAmount());
        }
    }

    private boolean applyAttackToFirstEnemy(double x, double y, double w, double h, int damage, long nowNs) {
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (!CollisionSystem.intersects(enemy, x, y, w, h)) {
                continue;
            }
            DamageResult damageResult = DamageSystem.applyDamage(player, enemy, damage, nowNs);
            if (damageResult.hasDamage()) {
                spawnFloatingDamageText(enemy.getCenterX(), enemy.getY(), damageResult.getFinalDamage(), damageResult.isCritical(), nowNs);
            }
            return true;
        }
        return false;
    }

    private void spawnFloatingDamageText(double worldX, double worldY, int damage, boolean critical, long nowNs) {
        if (damage <= 0) {
            return;
        }
        floatingDamageTexts.add(new FloatingDamageText("-" + damage, worldX, worldY, nowNs, 820_000_000L, critical));
    }

    private void cleanupExpiredDamageTexts(long nowNs) {
        floatingDamageTexts.removeIf(text -> text == null || text.isExpired(nowNs));
    }

    private void updateEnemySpawning(long now) {
        if (now - lastEnemySpawnAtNs < ENEMY_SPAWN_INTERVAL_NS) {
            return;
        }

        boolean night = dayNightCycle.isNight(now);
        int targetOrc = night ? randomBetween(NIGHT_ORC_TARGET_MIN, NIGHT_ORC_TARGET_MAX)
                : randomBetween(DAY_ORC_TARGET_MIN, DAY_ORC_TARGET_MAX);
        int targetSkeleton = night ? randomBetween(NIGHT_SKELETON_TARGET_MIN, NIGHT_SKELETON_TARGET_MAX)
                : DAY_SKELETON_TARGET;

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
            enemy.update(now, player, worldWidth, worldHeight);
            enemy.tryAttackPlayer(player, now);
        }
        if (!dead.isEmpty()) {
            enemies.removeAll(dead);
        }
    }

    private int countAliveEnemyByType(String type) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (type.equalsIgnoreCase(enemy.getEnemyType())) {
                count++;
            }
        }
        return count;
    }

    private double[] randomEdgeSpawnPoint() {
        int side = random.nextInt(4);
        double margin = 4;
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

    private int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private void addCollectedItem(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        int oldAmount = collectedResources.getOrDefault(itemId, 0);
        collectedResources.put(itemId, oldAmount + amount);
        // Moi lan nhat/pha duoc resource thi cong EXP ngay lap tuc.
        player.addExperience(1);
        if (isFoodItem(itemId)) {
            player.recoverEnergy(FOOD_ENERGY_BONUS);
        }
    }

    private void updateEnergyByMovement(long now, boolean playerMoving) {
        double deltaSeconds = (now - lastUpdateNowNs) / 1_000_000_000.0;
        if (deltaSeconds < 0) {
            deltaSeconds = 0;
        }
        if (deltaSeconds > 0.25) {
            deltaSeconds = 0.25;
        }
        lastUpdateNowNs = now;

        if (playerMoving) {
            player.consumeEnergy(MOVE_ENERGY_DRAIN_PER_SECOND * deltaSeconds);
            return;
        }
        player.recoverEnergy(IDLE_ENERGY_REGEN_PER_SECOND * deltaSeconds);
    }

    private boolean isFoodItem(String itemId) {
        String normalized = itemId.trim().toLowerCase();
        return normalized.contains("meat")
                || normalized.contains("carrot")
                || normalized.contains("vegetable")
                || normalized.contains("food");
    }

    private boolean isResourceObject(MapObjectData object) {
        if (object == null || object.getProperties() == null) {
            return false;
        }
        String kind = object.getProperties().get("kind");
        String dropItem = object.getProperties().get("dropItem");
        String maxHp = object.getProperties().get("maxHp");
        String hpLegacy = object.getProperties().get("hp");
        return hasText(kind) || hasText(dropItem) || hasText(maxHp) || hasText(hpLegacy);
    }

    private boolean isResourceAlive(int objectId) {
        if (resourceManager.getResourceById(objectId) == null) {
            return true;
        }
        return resourceManager.getResourceById(objectId).isAlive();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
