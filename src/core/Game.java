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
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceManager;
import system.resource.TileResourceAdapter;
import ui.Renderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {
    // Phai dong bo voi CAMERA_ZOOM trong Renderer de camera center dung khi co zoom.
    // Neu doi zoom ben Renderer, nho doi gia tri nay theo.
    private static final double CAMERA_ZOOM = 1.5;

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final Player player;
    // Danh sach quai runtime (quan ly theo da hinh Enemy).
    private final List<Enemy> enemies;
    private final List<MapObjectData> mapCollisions;
    private final ResourceManager resourceManager;
    private final TileCollisionResolver tileCollisionResolver;
    private final DayNightCycle dayNightCycle;
    // Tai nguyen nguoi choi da thu thap (hien thi HUD text tam thoi).
    private final Map<String, Integer> collectedResources;
    private GameState gameState;

    // ===== Enemy Spawn Config =====
    // Ban ngay: chi spawn orc it (1-2 con).
    private static final int DAY_ORC_TARGET_MIN = 1;
    private static final int DAY_ORC_TARGET_MAX = 2;
    private static final int DAY_SKELETON_TARGET = 0;
    // Ban dem: ca 2 loai deu xuat hien nhieu.
    private static final int NIGHT_ORC_TARGET_MIN = 7;
    private static final int NIGHT_ORC_TARGET_MAX = 8;
    private static final int NIGHT_SKELETON_TARGET_MIN = 7;
    private static final int NIGHT_SKELETON_TARGET_MAX = 8;
    // Moi lan spawn theo nhip de thay quai vao dan tu ranh map.
    private static final long ENEMY_SPAWN_INTERVAL_NS = 650_000_000L;

    private static final int MENU_PLAY = 0;
    private static final int MENU_GUIDE = 1;
    private static final int MENU_EXIT = 2;
    private static final int MENU_COUNT = 3;

    // ===== Mouse Hitbox cho menu WELCOME =====
    // Cac gia tri nay map 1-1 voi toa do ve button trong Renderer.
    private static final double MENU_BUTTON_X = 350;
    private static final double MENU_BUTTON_Y_START = 246; // itemY - 24 voi itemY ban dau = 270
    private static final double MENU_BUTTON_WIDTH = 260;
    private static final double MENU_BUTTON_HEIGHT = 42;
    private static final double MENU_BUTTON_GAP = 58;

    private long lastEnemySpawnAtNs;
    private double cameraX;
    private double cameraY;
    // Kich thuoc world thuc te dang dung cho movement/camera.
    // Neu co map tiled thi lay theo pixel size cua map, neu khong thi fallback GameConfig.
    private double worldWidth;
    private double worldHeight;
    // Vi tri spawn hien tai cua player (tinh theo center cua world thuc te).
    private double playerStartX;
    private double playerStartY;
    private int menuIndex;
    private MapData mapData;

    private long welcomeFlashUntilNs;
    private static final long WELCOME_FLASH_DURATION_NS = 120_000_000L; // flash nhe khi enter o Welcome
    private static final long WELCOME_TRANSITION_DELAY_NS = 220_000_000L; // tre de thay duoc hieu ung chuyen man
    private int pendingWelcomeAction; // -1: khong co action dang cho

    // ===== Name Input Config =====
    // Gioi han do dai ten de tranh ten qua dai de vo UI khi ve tren dau player.
    private static final int MAX_PLAYER_NAME_LENGTH = 14;
    // Buffer tam trong man hinh nhap ten.
    private final StringBuilder playerNameBuffer;
    private final Random random;
    // Moc thoi gian frame truoc de tinh delta time (phuc vu energy drain/regen).
    private long lastUpdateNowNs;

    // ===== Energy / Progression Config =====
    // Tieu hao nang luong khi nhan vat THUC SU di chuyen (don vi: energy/second).
    private static final double MOVE_ENERGY_DRAIN_PER_SECOND = 2.2;
    // Hoi nang luong nhe khi dung im.
    private static final double IDLE_ENERGY_REGEN_PER_SECOND = 0.9;
    // Danh thuong khong ton nang luong; skill F ton nang luong o muc nhe.
    private static final double SKILL_F_ENERGY_COST = 3.0;
    // Thu thap "food" se hoi nang luong manh hon.
    private static final double FOOD_ENERGY_BONUS = 15.0;

    public Game(Stage stage) {
        this.inputHandler = new InputHandler();
        // Spawn tam thoi; sau khi load map se reset lai vao dung center world thuc te.
        this.player = new Player(100, 100, 58, 58, 1, 100);
        this.renderer = new Renderer(stage, inputHandler);
        this.gameLoop = new GameLoop(this);
        this.gameState = GameState.WELCOME;
        this.enemies = new ArrayList<>();
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
        this.playerNameBuffer = new StringBuilder("Player");
        this.random = new Random();
        this.lastUpdateNowNs = -1L;
        this.resourceManager = new ResourceManager();
        this.dayNightCycle = new DayNightCycle();
        this.collectedResources = new LinkedHashMap<>();

        MapData loadedMap = null;
        try {
            // Uu tien map moi tu team ve map.
            loadedMap = new TiledMapLoader().load("assets/Map_game/map.tmx");
        } catch (Exception exception) {
            System.out.println("Cannot load assets/Map_game/map.tmx: " + exception.getMessage());
            try {
                // Fallback map cu de tranh chan pipeline gameplay.
                loadedMap = new TiledMapLoader().load("assets/maps/mapdemo.tmx");
            } catch (Exception fallbackException) {
                // Neu map loi thi game van chay theo luong cu.
                System.out.println("Cannot load fallback tiled map: " + fallbackException.getMessage());
            }
        }

        this.mapData = loadedMap;
        List<MapObjectData> runtimeCollisionObjects = new ArrayList<>();
        if (loadedMap != null) {
            runtimeCollisionObjects.addAll(loadedMap.getCollisionObjects());
            // Adapter: map Tile Properties -> resource objects runtime.
            // Chi them vao runtime list, khong sua file map goc.
            runtimeCollisionObjects.addAll(new TileResourceAdapter().buildResourceObjects(loadedMap));
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

        // Neu load duoc map tiled: dung kich thuoc map pixel lam world boundary thuc te.
        if (loadedMap != null) {
            this.worldWidth = loadedMap.getPixelWidth();
            this.worldHeight = loadedMap.getPixelHeight();
        }

        // Spawn player o GIUA world thuc te (uu tien map tiled 120x80 neu co).
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
        // Tach update() thanh nhieu ham state-specific de:
        // 1) de doc/de review tung man hinh
        // 2) de debug regression nhanh (loi nam o state nao ro rang)
        // 3) de sau nay co the dua tung khoi sang subsystem rieng
        //    (UI flow, input flow, combat flow) ma khong pha logic con lai.
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
            case PAUSED:
                handlePausedState();
                break;
            case PLAYING:
                // Neu ham nay tra true: frame da early-return (vi pause),
                // khong chay inputHandler.update() lan nua.
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
            restartGame();
        }
    }

    private void handleWelcomeState(long now) {
        // Ho tro hover + click chuot trong WELCOME.
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
            String normalizedName = normalizePlayerName(playerNameBuffer.toString());
            player.setPlayerName(normalizedName);
            restartGame();
            gameState = GameState.PLAYING;
        }
    }

    private void handleGuideState() {
        if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
        } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
            restartGame();
            gameState = GameState.PLAYING;
        }
    }

    private void handlePausedState() {
        if (inputHandler.isJustPressed(KeyCode.P)) {
            gameState = GameState.PLAYING;
        } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
            gameState = GameState.WELCOME;
            menuIndex = 0;
        }
    }

    // Tra ve true neu frame dung tai day (vi vua bam ESC de pause).
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
            player.takeDamage(1);
        }
        if (inputHandler.isPressed(KeyCode.K)) {
            player.heal(1);
        }

        // Input tan cong:
        // - Click chuot trai -> HIT (4 frame Hit_Base) theo yeu cau moi.
        // - Phim F -> SLICE (giu lai de test/legacy, sau nay map voi vu khi).
        if (inputHandler.isMouseLeftJustClicked()) {
            performPlayerAttack(now, Player.AttackAnimationType.HIT);
        } else if (inputHandler.isJustPressed(KeyCode.F)) {
            // Skill F yeu cau du nang luong moi duoc kich hoat.
            if (player.consumeEnergy(SKILL_F_ENERGY_COST)) {
                performPlayerAttack(now, Player.AttackAnimationType.SLICE);
            }
        }

        resourceManager.update(now);
        updateEnemySpawning(now);
        updateEnemies(now);
        player.clampPosition(0, 0, worldWidth, worldHeight);

        if (isPlayerCollidingWithMapCollision()) {
            player.setPosition(oldPlayerX, oldPlayerY);
        }

        boolean playerMoving = oldPlayerX != player.getX() || oldPlayerY != player.getY();
        player.updateAnimation(now, playerMoving, moveUp, moveDown, moveLeft, moveRight);
        updateEnergyByMovement(now, playerMoving);
        if (!player.isAlive()) {
            gameState = GameState.GAME_OVER;
        }

        updateCamera();
        return false;
    }

    private void updateCamera() {
        // Khi zoom > 1, viewport world thuc te nho hon man hinh.
        // Vi vay tam camera phai dua tren "viewport world sau zoom":
        // viewWorldWidth  = screenWidth  / zoom
        // viewWorldHeight = screenHeight / zoom
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

        // Camera phai clamp theo world thuc te (map tiled neu co),
        // neu khong se sinh ra "ban do ao" va gioi han di chuyen sai.
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
                worldWidth,
                worldHeight
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
        // Moi lan vao game/reset, spawn dung giua world hien tai.
        recalculatePlayerStartAtWorldCenter();
        player.reset(playerStartX, playerStartY);
        enemies.clear();
        gameState = GameState.PLAYING;
        menuIndex = 0;
        pendingWelcomeAction = -1; // dam bao khong con action cho tu menu
        lastEnemySpawnAtNs = 0L;
        lastUpdateNowNs = -1L;
        // Update camera ngay luc reset de frame dau tien vao game da focus dung vao player.
        updateCamera();
        dayNightCycle.reset(System.nanoTime());
    }

    private void recalculatePlayerStartAtWorldCenter() {
        // Center cua map/world tinh theo pixel:
        // centerX = worldWidth / 2, centerY = worldHeight / 2
        // Vi player co kich thuoc rieng, can tru di nua width/height de dat tam player vao giua.
        playerStartX = worldWidth / 2 - player.getWidth() / 2;
        playerStartY = worldHeight / 2 - player.getHeight() / 2;
    }

    // Khong con tree/wolf test class: enemy duoc quan ly boi List<Enemy>.

    private boolean isPlayerCollidingWithMapCollision() {
        double px = getPlayerCollisionX();
        double py = getPlayerCollisionY();
        double pw = getPlayerCollisionWidth();
        double ph = getPlayerCollisionHeight();

        // 1) Collision theo Object Layer / object runtime adapter.
        for (MapObjectData object : mapCollisions) {
            // Chi check object dung vai tro collision.
            if (!"Collision".equalsIgnoreCase(object.getType())) {
                continue;
            }

            // Neu object nay la resource va da bi pha thi bo qua collision,
            // giup player di xuyen qua sau khi "chat/dao" xong.
            if (isResourceObject(object) && !isResourceAlive(object.getId())) {
                continue;
            }
            if (object.intersects(px, py, pw, ph)) {
                return true;
            }
        }

        // 2) Collision theo Tile Properties (fallback/chinh cho map moi khong co Object Layer).
        if (tileCollisionResolver != null && tileCollisionResolver.isBlocked(px, py, pw, ph)) {
            return true;
        }

        return false;
    }

    private double getPlayerCollisionX() {
        // Thu nho hitbox ngang de sprite 48x48 khong bi block qua som.
        return player.getX() + player.getWidth() * 0.22;
    }

    private double getPlayerCollisionY() {
        // Day hitbox xuong duoi de uu tien phan "chan" va cham world object.
        return player.getY() + player.getHeight() * 0.30;
    }

    private double getPlayerCollisionWidth() {
        return player.getWidth() * 0.56;
    }

    private double getPlayerCollisionHeight() {
        return player.getHeight() * 0.62;
    }

    // Duyet A..Z va append ky tu vua bam.
    // Cac phim duoc luu theo enum KeyCode.A ... KeyCode.Z.
    private void appendLetterFromKeyboard() {
        if (playerNameBuffer.length() >= MAX_PLAYER_NAME_LENGTH) {
            return;
        }

        for (char c = 'A'; c <= 'Z'; c++) {
            KeyCode code = KeyCode.getKeyCode(String.valueOf(c));
            if (code != null && inputHandler.isJustPressed(code)) {
                playerNameBuffer.append(c);
                return; // Moi frame chi them 1 ky tu de input on dinh.
            }
        }
    }

    // Duyet 0..9 va append so vao ten.
    private void appendDigitFromKeyboard() {
        if (playerNameBuffer.length() >= MAX_PLAYER_NAME_LENGTH) {
            return;
        }

        // Ho tro day du 2 cum phim so:
        // 1) Hang so tren cung ban phim: DIGIT0..DIGIT9
        // 2) Cum numpad: NUMPAD0..NUMPAD9
        // Ly do bo sung: tuy layout may, nguoi dung co the bam numpad
        // va neu chi bat DIGIT thi se co cam giac "khong nhap duoc so".
        for (int d = 0; d <= 9; d++) {
            KeyCode digitCode = KeyCode.getKeyCode("DIGIT" + d);
            KeyCode numpadCode = KeyCode.getKeyCode("NUMPAD" + d);

            boolean digitPressed = digitCode != null && inputHandler.isJustPressed(digitCode);
            boolean numpadPressed = numpadCode != null && inputHandler.isJustPressed(numpadCode);

            if (digitPressed || numpadPressed) {
                playerNameBuffer.append(d);
                return;
            }
        }
    }

    // Chuan hoa ten truoc khi save vao player:
    // - trim bo khoang trang dau/cuoi
    // - neu rong thi fallback "Player"
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

    // Xu ly ky tu text da duoc nhap trong frame hien tai.
    // Chi cho phep: chu cai, so va dau cach.
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

            // Bo qua ky tu dieu khien (Enter, Backspace, ...)
            if (Character.isISOControl(ch)) {
                continue;
            }

            // Cho phep chu cai va so.
            if (Character.isLetterOrDigit(ch)) {
                playerNameBuffer.append(Character.toUpperCase(ch));
                continue;
            }

            // Cho phep 1 dau cach, nhung khong dat o dau ten va khong lap lai lien tiep.
            if (ch == ' '
                    && playerNameBuffer.length() > 0
                    && playerNameBuffer.charAt(playerNameBuffer.length() - 1) != ' ') {
                playerNameBuffer.append(' ');
            }
        }
    }

    // Cap nhat item menu dang duoc hover boi chuot.
    private void updateWelcomeMenuHoverByMouse() {
        int hoveredIndex = findWelcomeMenuIndexAt(inputHandler.getMouseX(), inputHandler.getMouseY());
        if (hoveredIndex != -1 && pendingWelcomeAction == -1) {
            menuIndex = hoveredIndex;
        }
    }

    // Tra ve index menu neu diem (mx,my) nam trong 1 button WELCOME.
    // -1 nghia la chuot dang ngoai vung button.
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

    // Xu ly 1 lan tan cong cua player:
    // 1) Bat state slash animation
    // 2) Lay hitbox tan cong theo huong nhan vat
    // 3) Apply damage len resource dau tien giao hitbox
    // 4) Neu resource vo -> nhan drop vao kho thu thap tam thoi
    private void performPlayerAttack(long now, Player.AttackAnimationType attackType) {
        if (!player.startAttack(now, attackType)) {
            return;
        }

        // Damage theo loai don danh:
        // - HIT (danh thuong) = 1
        // - SLICE (skill F)   = 2
        int attackDamage = attackType == Player.AttackAnimationType.SLICE ? 2 : 1;

        double[] attackBox = player.buildAttackHitbox();
        boolean hitEnemy = applyAttackToFirstEnemy(attackBox[0], attackBox[1], attackBox[2], attackBox[3], attackDamage);
        if (hitEnemy) {
            System.out.println("[Combat] Hit enemy for " + attackDamage + " damage.");
            return;
        }

        DropResult dropResult = resourceManager.hitFirstResourceIntersecting(
                attackBox[0],
                attackBox[1],
                attackBox[2],
                attackBox[3],
                attackDamage,
                now
        );

        if (dropResult == null) {
            System.out.println("[Resource] Attack landed, no resource destroyed.");
            return;
        }

        addCollectedItem(dropResult.getItemId(), dropResult.getAmount());
        System.out.println("[Resource] Destroyed -> drop " + dropResult.getItemId() + " x" + dropResult.getAmount());
    }

    // Apply damage len enemy dau tien giao hitbox.
    private boolean applyAttackToFirstEnemy(double x, double y, double w, double h, int damage) {
        for (Enemy enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            if (!enemy.intersects(x, y, w, h)) {
                continue;
            }
            enemy.takeDamage(damage);
            return true;
        }
        return false;
    }

    // Spawn manager:
    // - Ban ngay: it orc (1-2), khong skeleton.
    // - Ban dem: ca orc + skeleton deu nhieu (7-8 moi loai).
    // - Spawn tu ria map (4 phia) roi duoi vao player.
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

    // Update AI va attack cua moi enemy, dong thoi don enemy chet de list gon.
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

    // Spawn tu 4 ria map:
    // 0=top, 1=right, 2=bottom, 3=left.
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
        // Moi lan thu thap tai nguyen: +1 EXP theo yeu cau MVP.
        player.addExperience(1);
        // Food cho hoi nang luong manh hon dung im.
        if (isFoodItem(itemId)) {
            player.recoverEnergy(FOOD_ENERGY_BONUS);
        }
    }

    private void updateEnergyByMovement(long now, boolean playerMoving) {
        // Delta second theo frame de drain/regen on dinh, khong phu thuoc FPS.
        double deltaSeconds = (now - lastUpdateNowNs) / 1_000_000_000.0;
        if (deltaSeconds < 0) {
            deltaSeconds = 0;
        }
        if (deltaSeconds > 0.25) {
            // Clamp anti-spike: tranh frame hut qua nhieu energy khi co pause lag.
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

    // Detect object co schema resource theo contract/property map.
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
