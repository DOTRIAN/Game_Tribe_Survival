package core;

import entity.Player;
import entity.Tree;
import entity.Wolf;
import input.InputHandler;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
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
            if (inputHandler.isJustPressed(KeyCode.UP) || inputHandler.isJustPressed(KeyCode.W)) {
                menuIndex = (menuIndex - 1 + MENU_COUNT) % MENU_COUNT;
            }
            if (inputHandler.isJustPressed(KeyCode.DOWN) || inputHandler.isJustPressed(KeyCode.S)) {
                menuIndex = (menuIndex + 1) % MENU_COUNT;
            }
            if (inputHandler.isJustPressed(KeyCode.H)) {
                gameState = GameState.GUIDE;
            } else if (inputHandler.isJustPressed(KeyCode.ENTER)) {
                if (menuIndex == MENU_PLAY) {
                    restartGame();
                    gameState = GameState.PLAYING;
                } else if (menuIndex == MENU_GUIDE) {
                    gameState = GameState.GUIDE;
                } else if (menuIndex == MENU_EXIT) {
                    Platform.exit();
                }
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

        if (!skipGameplay && gameState == GameState.PLAYING) {
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

            if (isPlayerCollidingWithAnyTree()) {
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
        renderer.render(gameState, player, wolf, now, wolfMoving, trees, cameraX, cameraY, menuIndex);
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
}
