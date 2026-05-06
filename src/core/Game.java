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
import ui.Renderer;

import java.util.ArrayList;
import java.util.List;

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
    private GameState gameState;

    private static final boolean ENABLE_WOLF = false;
    private static final double ENEMY_START_X = 700;
    private static final double ENEMY_START_Y = 300;
    private static final long DAMAGE_COOLDOWN_NS = 500_000_000L;

    private static final int MENU_PLAY = 0;
    private static final int MENU_GUIDE = 1;
    private static final int MENU_EXIT = 2;
    private static final int MENU_COUNT = 3;

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

    public Game(Stage stage) {
        this.inputHandler = new InputHandler();
        // Spawn tam thoi; sau khi load map se reset lai vao dung center world thuc te.
        this.player = new Player(100, 100, 48, 48, 1, 100);
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
            if (pendingWelcomeAction == -1) { // chi cho di chuyen menu khi khong dang transition
                if (inputHandler.isJustPressed(KeyCode.UP) || inputHandler.isJustPressed(KeyCode.W)) {
                    menuIndex = (menuIndex - 1 + MENU_COUNT) % MENU_COUNT;
                }
                if (inputHandler.isJustPressed(KeyCode.DOWN) || inputHandler.isJustPressed(KeyCode.S)) {
                    menuIndex = (menuIndex + 1) % MENU_COUNT;
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
                    restartGame();
                    gameState = GameState.PLAYING;
                } else if (pendingWelcomeAction == MENU_GUIDE) {
                    gameState = GameState.GUIDE;
                } else if (pendingWelcomeAction == MENU_EXIT) {
                    Platform.exit();
                }
                pendingWelcomeAction = -1; // reset action sau khi xu ly
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

            boolean moveLeft = inputHandler.isPressed(KeyCode.A);
            boolean moveRight = inputHandler.isPressed(KeyCode.D);
            boolean moveUp = inputHandler.isPressed(KeyCode.W);
            boolean moveDown = inputHandler.isPressed(KeyCode.S);

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
        renderer.render(gameState, player, wolf, now, wolfMoving, trees, cameraX, cameraY, menuIndex, welcomeFlashing);
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
}
