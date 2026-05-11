package core;

import entity.Player;
import entity.Tree;
import entity.Wolf;
import input.InputHandler;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import map.MapData;
import map.MapObjectData;
import map.TiledMapLoader;
import system.resource.DropResult;
import system.resource.ResourceContractValidator;
import system.resource.ResourceManager;
import ui.Renderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Game {
    // Phai dong bo voi CAMERA_ZOOM trong Renderer de camera center dung khi co zoom.
    // Neu doi zoom ben Renderer, nho doi gia tri nay theo.
    private static final double CAMERA_ZOOM = 1.5;

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final Player player;
    private final Wolf wolf;
    private final List<Tree> trees;
    private final List<MapObjectData> mapCollisions;
    private final ResourceManager resourceManager;
    private final DayNightCycle dayNightCycle;
    // Tai nguyen nguoi choi da thu thap (hien thi HUD text tam thoi).
    private final Map<String, Integer> collectedResources;
    private GameState gameState;

    private static final boolean ENABLE_WOLF = false;
    private static final double ENEMY_START_X = 700;
    private static final double ENEMY_START_Y = 300;
    private static final long DAMAGE_COOLDOWN_NS = 500_000_000L;

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

    private long lastDamageTime;
    private boolean wolfMoving;
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

    public Game(Stage stage) {
        this.inputHandler = new InputHandler();
        // Spawn tam thoi; sau khi load map se reset lai vao dung center world thuc te.
        this.player = new Player(100, 100, 58, 58, 1, 100);
        this.renderer = new Renderer(stage, inputHandler);
        this.gameLoop = new GameLoop(this);
        this.gameState = GameState.WELCOME;
        this.wolf = ENABLE_WOLF ? new Wolf(1200, 500, 64, 64, 1) : null;
        this.lastDamageTime = 0;
        this.trees = new ArrayList<>();
        this.trees.add(new Tree(300, 220, 64, 64));
        this.trees.add(new Tree(450, 150, 64, 64));
        this.trees.add(new Tree(600, 320, 64, 64));
        this.trees.add(new Tree(300, 220, 64, 64));
        this.trees.add(new Tree(900, 300, 64, 64));
        this.trees.add(new Tree(1400, 700, 64, 64));
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
        this.resourceManager = new ResourceManager();
        this.dayNightCycle = new DayNightCycle();
        this.collectedResources = new LinkedHashMap<>();

        MapData loadedMap = null;
        try {
            // Load map TMX de thay cho background image tinh.
            loadedMap = new TiledMapLoader().load("assets/maps/mapdemo.tmx");
        } catch (Exception exception) {
            // Neu map loi thi game van chay theo luong cu.
            System.out.println("Cannot load tiled map: " + exception.getMessage());
        }

        this.mapData = loadedMap;
        this.mapCollisions = loadedMap != null ? loadedMap.getCollisionObjects() : new ArrayList<>();
        this.resourceManager.loadFromMapObjects(this.mapCollisions);
        List<String> contractErrors = new ResourceContractValidator().validate(this.mapCollisions);
        if (!contractErrors.isEmpty()) {
            System.out.println("=== Resource contract warnings ===");
            for (String error : contractErrors) {
                System.out.println(error);
            }
        }
        this.renderer.setMapData(loadedMap);

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
        boolean skipGameplay = false;

        if (gameState == GameState.GAME_OVER) {
            if (inputHandler.isJustPressed(KeyCode.R)) {
                restartGame();
            }
            skipGameplay = true;
        }

        if (!skipGameplay && gameState == GameState.WELCOME) {
            // Ho tro hover + click chuot trong WELCOME.
            // Hover se dong bo menuIndex de highlight dung item dang tro chuot.
            updateWelcomeMenuHoverByMouse();

            if (pendingWelcomeAction == -1) { // chi cho di chuyen menu khi khong dang transition
                if (inputHandler.isJustPressed(KeyCode.UP) || inputHandler.isJustPressed(KeyCode.W)) {
                    menuIndex = (menuIndex - 1 + MENU_COUNT) % MENU_COUNT;
                }
                if (inputHandler.isJustPressed(KeyCode.DOWN) || inputHandler.isJustPressed(KeyCode.S)) {
                    menuIndex = (menuIndex + 1) % MENU_COUNT;
                }
            }

            // Click trai: trigger hanh dong giong ENTER tren item dang hover/chon.
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
                welcomeFlashUntilNs = now + WELCOME_FLASH_DURATION_NS; // bat flash dung luc nhan ENTER
                pendingWelcomeAction = menuIndex; // luu action, chua switch ngay
            }

            long transitionAtNs = welcomeFlashUntilNs + WELCOME_TRANSITION_DELAY_NS; // moc thoi gian duoc phep switch
            if (pendingWelcomeAction != -1 && now >= transitionAtNs) {
                if (pendingWelcomeAction == MENU_PLAY) {
                    // Chuyen vao trang thai nhap ten thay vi vao game ngay.
                    // restartGame se duoc goi sau khi nguoi choi bam ENTER xac nhan ten.
                    gameState = GameState.NAME_INPUT;
                } else if (pendingWelcomeAction == MENU_GUIDE) {
                    gameState = GameState.GUIDE;
                } else if (pendingWelcomeAction == MENU_EXIT) {
                    Platform.exit();
                }
                pendingWelcomeAction = -1; // reset action sau khi xu ly
            }
            skipGameplay = true;
        }

        if (!skipGameplay && gameState == GameState.NAME_INPUT) {
            // ESC: quay lai menu chinh.
            if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
                gameState = GameState.WELCOME;
                pendingWelcomeAction = -1;
                skipGameplay = true;
            }

            // BACKSPACE: xoa ky tu cuoi cung trong buffer.
            if (!skipGameplay && inputHandler.isJustPressed(KeyCode.BACK_SPACE) && playerNameBuffer.length() > 0) {
                playerNameBuffer.deleteCharAt(playerNameBuffer.length() - 1);
            }

            // SPACE: cho phep chen 1 khoang trang (nhung khong de space o dau ten).
            if (!skipGameplay && inputHandler.isJustPressed(KeyCode.SPACE)
                    && playerNameBuffer.length() < MAX_PLAYER_NAME_LENGTH
                    && playerNameBuffer.length() > 0
                    && playerNameBuffer.charAt(playerNameBuffer.length() - 1) != ' ') {
                playerNameBuffer.append(' ');
            }

            // Nhan ky tu text tu InputHandler (onKeyTyped):
            // - ho tro on dinh cho ca chu va so
            // - khong phu thuoc layout ban phim (US/VN, numpad,...)
            if (!skipGameplay) {
                appendTypedCharacters();
            }

            // ENTER: xac nhan ten va bat dau game.
            if (!skipGameplay && inputHandler.isJustPressed(KeyCode.ENTER)) {
                String normalizedName = normalizePlayerName(playerNameBuffer.toString());
                player.setPlayerName(normalizedName);
                restartGame();
                gameState = GameState.PLAYING;
            }

            skipGameplay = true;
        }

        if (!skipGameplay && gameState == GameState.GUIDE) {
            if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
                gameState = GameState.WELCOME;
            } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
                restartGame();
                gameState = GameState.PLAYING;
            }
            skipGameplay = true;
        }

        if (!skipGameplay && gameState == GameState.PAUSED) {
            if (inputHandler.isJustPressed(KeyCode.P)) {
                gameState = GameState.PLAYING;
            } else if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
                gameState = GameState.WELCOME;
                menuIndex = 0;
            }
            skipGameplay = true;
        }

        if (!skipGameplay && gameState == GameState.PLAYING) {
            if (inputHandler.isJustPressed(KeyCode.ESCAPE)) {
                gameState = GameState.PAUSED;
                inputHandler.update();
                return;
            } // ddang choiw bam esc thi dung frame
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

            // Trigger attack bang click trai CHUOT hoac phim F (giu F de test nhanh khi can).
            boolean attackTriggered = inputHandler.isMouseLeftJustClicked() || inputHandler.isJustPressed(KeyCode.F);
            if (attackTriggered) {
                performPlayerAttack(now);
            }

            // Update resource runtime (respawn neu du dieu kien).
            resourceManager.update(now);

            player.clampPosition(0, 0, worldWidth, worldHeight);

            double oldWolfX = 0;
            double oldWolfY = 0;
            if (wolf != null) {
                oldWolfX = wolf.getX();
                oldWolfY = wolf.getY();
                wolf.moveToward(player);
                wolf.clampPosition(0, 0, worldWidth, worldHeight);
            }

            if (isPlayerCollidingWithAnyTree() || isPlayerCollidingWithMapCollision()) {
                player.setPosition(oldPlayerX, oldPlayerY);
            }

            boolean playerMoving = oldPlayerX != player.getX() || oldPlayerY != player.getY();
            player.updateAnimation(now, playerMoving, moveUp, moveDown, moveLeft, moveRight);

            wolfMoving = wolf != null && (oldWolfX != wolf.getX() || oldWolfY != wolf.getY());

            if (wolf != null && isColliding() && now - lastDamageTime >= DAMAGE_COOLDOWN_NS) {
                player.takeDamage(1);
                lastDamageTime = now;
            }

            if (!player.isAlive()) {
                gameState = GameState.GAME_OVER;
            }

            updateCamera();
        }

        inputHandler.update();
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
                wolf,
                now,
                wolfMoving,
                trees,
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
                dayNightCycle.getPhaseName(now)
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

    private boolean isColliding() {
        if (wolf == null) {
            return false;
        }
        return player.getX() < wolf.getX() + wolf.getWidth()
                && player.getX() + player.getWidth() > wolf.getX()
                && player.getY() < wolf.getY() + wolf.getHeight()
                && player.getY() + player.getHeight() > wolf.getY();
    }

    private void restartGame() {
        // Moi lan vao game/reset, spawn dung giua world hien tai.
        recalculatePlayerStartAtWorldCenter();
        player.reset(playerStartX, playerStartY);
        if (wolf != null) {
            wolf.reset(ENEMY_START_X, ENEMY_START_Y);
        }
        gameState = GameState.PLAYING;
        menuIndex = 0;
        pendingWelcomeAction = -1; // dam bao khong con action cho tu menu
        lastDamageTime = 0;
        wolfMoving = false;
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

    public boolean isWolfMoving() {
        return wolfMoving;
    }

    public List<Tree> getTrees() {
        return trees;
    }

    private boolean isPlayerCollidingWithAnyTree() {
        // Khi da co map collisions, tree hard-code cu khong can chen collision nua.
        if (mapData != null) {
            return false;
        }

        double px = getPlayerCollisionX();
        double py = getPlayerCollisionY();
        double pw = getPlayerCollisionWidth();
        double ph = getPlayerCollisionHeight();

        for (Tree tree : trees) {
            boolean colliding = px < tree.getX() + tree.getWidth()
                    && px + pw > tree.getX()
                    && py < tree.getY() + tree.getHeight()
                    && py + ph > tree.getY();

            if (colliding) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerCollidingWithMapCollision() {
        if (mapCollisions.isEmpty()) {
            return false;
        }

        double px = getPlayerCollisionX();
        double py = getPlayerCollisionY();
        double pw = getPlayerCollisionWidth();
        double ph = getPlayerCollisionHeight();

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
    private void performPlayerAttack(long now) {
        if (!player.startAttack(now)) {
            return;
        }

        double[] attackBox = player.buildAttackHitbox();
        DropResult dropResult = resourceManager.hitFirstResourceIntersecting(
                attackBox[0],
                attackBox[1],
                attackBox[2],
                attackBox[3],
                1,
                now
        );

        if (dropResult == null) {
            System.out.println("[Resource] Attack landed, no resource destroyed.");
            return;
        }

        addCollectedItem(dropResult.getItemId(), dropResult.getAmount());
        System.out.println("[Resource] Destroyed -> drop " + dropResult.getItemId() + " x" + dropResult.getAmount());
    }

    private void addCollectedItem(String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        int oldAmount = collectedResources.getOrDefault(itemId, 0);
        collectedResources.put(itemId, oldAmount + amount);
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
