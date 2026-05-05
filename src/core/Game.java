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

    private final GameLoop gameLoop;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final Player player;
    private final Wolf wolf;
    private final List<Tree> trees;
    private final List<MapObjectData> mapCollisions;
    private GameState gameState;

    private static final double PLAYER_START_X = 100;
    private static final double PLAYER_START_Y = 100;
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
    private int menuIndex;
    private MapData mapData;

    private long welcomeFlashUntilNs;
    private static final long WELCOME_FLASH_DURATION_NS = 120_000_000L; // flash nhe khi enter o Welcome
    private static final long WELCOME_TRANSITION_DELAY_NS = 220_000_000L; // tre de thay duoc hieu ung chuyen man
    private int pendingWelcomeAction; // -1: khong co action dang cho

    public Game(Stage stage) {
        this.inputHandler = new InputHandler();
        this.player = new Player(PLAYER_START_X, PLAYER_START_Y, 32, 32, 4, 100);
        this.renderer = new Renderer(stage, inputHandler);
        this.gameLoop = new GameLoop(this);
        this.gameState = GameState.WELCOME;
        this.wolf = new Wolf(1200, 500, 64, 64, 1);
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

            if (inputHandler.isPressed(KeyCode.A)) {
                player.moveLeft();
            }
            if (inputHandler.isPressed(KeyCode.D)) {
                player.moveRight();
            }
            if (inputHandler.isPressed(KeyCode.W)) {
                player.moveUp();
            }
            if (inputHandler.isPressed(KeyCode.S)) {
                player.moveDown();
            }
            if (inputHandler.isPressed(KeyCode.J)) {
                player.takeDamage(1);
            }
            if (inputHandler.isPressed(KeyCode.K)) {
                player.heal(1);
            }

            player.clampPosition(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

            double oldWolfX = wolf.getX();
            double oldWolfY = wolf.getY();

            wolf.moveToward(player);
            wolf.clampPosition(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

            if (isPlayerCollidingWithAnyTree() || isPlayerCollidingWithMapCollision()) {
                player.setPosition(oldPlayerX, oldPlayerY);
            }

            wolfMoving = oldWolfX != wolf.getX() || oldWolfY != wolf.getY();

            if (isColliding() && now - lastDamageTime >= DAMAGE_COOLDOWN_NS) {
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
        cameraX = player.getX() + player.getWidth() / 2 - GameConfig.WIDTH / 2;
        cameraY = player.getY() + player.getHeight() / 2 - GameConfig.HEIGHT / 2;

        if (cameraX < 0) {
            cameraX = 0;
        }
        if (cameraY < 0) {
            cameraY = 0;
        }

        double maxCameraX = GameConfig.WORLD_WIDTH - GameConfig.WIDTH;
        double maxCameraY = GameConfig.WORLD_HEIGHT - GameConfig.HEIGHT;

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
        return player.getX() < wolf.getX() + wolf.getWidth()
                && player.getX() + player.getWidth() > wolf.getX()
                && player.getY() < wolf.getY() + wolf.getHeight()
                && player.getY() + player.getHeight() > wolf.getY();
    }

    private void restartGame() {
        player.reset(PLAYER_START_X, PLAYER_START_Y);
        wolf.reset(ENEMY_START_X, ENEMY_START_Y);
        gameState = GameState.PLAYING;
        menuIndex = 0;
        pendingWelcomeAction = -1; // dam bao khong con action cho tu menu
        lastDamageTime = 0;
        wolfMoving = false;
        cameraX = 0;
        cameraY = 0;
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

        for (Tree tree : trees) {
            boolean colliding = player.getX() < tree.getX() + tree.getWidth()
                    && player.getX() + player.getWidth() > tree.getX()
                    && player.getY() < tree.getY() + tree.getHeight()
                    && player.getY() + player.getHeight() > tree.getY();

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

        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();

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
}
