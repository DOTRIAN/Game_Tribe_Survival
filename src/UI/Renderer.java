package ui;

import core.GameConfig;
import core.GameState;
import entity.Enemy;
import entity.Player;
import input.InputHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import map.MapData;
import map.MapRenderer;
import system.level.Level;
import system.level.LevelManager;
import system.level.LevelResult;
import system.resource.ResourceNode;
import ui.minimap.MiniMap;

import java.util.List;
import java.util.Map;

public class Renderer {
    private static final double CAMERA_ZOOM = 1.5;

    private final Canvas canvas;
    private final GraphicsContext graphicsContext;
    private final Hud hud;
    private final Image welcomeBackgroundImage;
    private final Image gameBackgroundImage;
    private final MiniMap miniMap;
    private MapRenderer mapRenderer;

    public Renderer(Stage stage, InputHandler inputHandler) {
        this.canvas = new Canvas(GameConfig.WIDTH, GameConfig.HEIGHT);
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.hud = new Hud();
        this.welcomeBackgroundImage = new Image("file:assets/backgrounds/menu_bg1.png");
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");
        this.miniMap = new MiniMap();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, GameConfig.WIDTH, GameConfig.HEIGHT);
        inputHandler.attach(scene);

        stage.setScene(scene);
        stage.show();
        canvas.setFocusTraversable(true);
        canvas.requestFocus();
        stage.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                canvas.requestFocus();
            }
        });
    }

    public void setMapData(MapData mapData) {
        this.mapRenderer = (mapData == null) ? null : new MapRenderer(mapData);
    }

    public void render(GameState gameState, Player player, List<Enemy> enemies, long now,
                       double cameraX, double cameraY, int menuIndex, boolean welcomeFlashing,
                       String playerNameDraft, int maxNameLength,
                       List<ResourceNode> allResources, Map<String, Integer> collectedResources,
                       double darknessAlpha, boolean isNight, String dayNightPhase, List<FloatingDamageText> floatingDamageTexts,
                       double worldWidth, double worldHeight, LevelManager levelManager,
                       Level currentLevel, int selectedLevelId, LevelResult lastLevelResult,
                       String objectiveText) {
        if (gameState == GameState.WELCOME) {
            renderWelcome(menuIndex, welcomeFlashing);
            return;
        }

        if (gameState == GameState.GUIDE) {
            renderGuide();
            return;
        }

        if (gameState == GameState.NAME_INPUT) {
            renderNameInput(playerNameDraft, maxNameLength);
            return;
        }

        if (gameState == GameState.LEVEL_SELECT) {
            renderLevelSelect(levelManager, selectedLevelId);
            return;
        }

        renderGameplayWorld(player, enemies, now, cameraX, cameraY, allResources, darknessAlpha, isNight,
                dayNightPhase, collectedResources, floatingDamageTexts, worldWidth, worldHeight);
        renderLevelHeader(levelManager, currentLevel, objectiveText);

        if (gameState == GameState.GAME_OVER) {
            renderCenteredOverlay("GAME OVER", "R: Retry level", "ESC: Back to level select");
            return;
        }

        if (gameState == GameState.PAUSED) {
            renderCenteredOverlay("PAUSED", "P: Resume", "ESC: Level select");
            return;
        }

        if (gameState == GameState.LEVEL_COMPLETE) {
            renderLevelComplete(lastLevelResult, levelManager);
        }
    }

    private void renderWelcome(int menuIndex, boolean welcomeFlashing) {
        if (welcomeBackgroundImage.isError()) {
            graphicsContext.setFill(Color.web("#2a3a2a"));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        } else {
            graphicsContext.drawImage(welcomeBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        }

        graphicsContext.setFill(Color.color(0, 0, 0, 0.5));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        if (welcomeFlashing) {
            graphicsContext.setFill(Color.color(1, 1, 1, 0.18));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        }

        double panelX = 280;
        double panelY = 120;
        double panelW = 400;
        double panelH = 300;
        graphicsContext.setFill(Color.color(0.95, 0.92, 0.78, 0.9));
        graphicsContext.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        graphicsContext.setStroke(Color.web("#5b4a2e"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        graphicsContext.fillText("TRIBE SURVIVAL", 334, 186);

        String[] menuItems = {"PLAY", "GUIDE", "EXIT"};
        double itemY = 270;
        for (int i = 0; i < menuItems.length; i++) {
            boolean selected = i == menuIndex;
            graphicsContext.setFill(selected ? Color.web("#7b5b2e") : Color.web("#d6c29b"));
            graphicsContext.fillRoundRect(350, itemY - 24, 260, 42, 12, 12);
            graphicsContext.setStroke(Color.web("#4a3a20"));
            graphicsContext.strokeRoundRect(350, itemY - 24, 260, 42, 12, 12);

            graphicsContext.setFill(selected ? Color.web("#fff4d8") : Color.web("#3a2f1d"));
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
            if ("PLAY".equals(menuItems[i])) {
                graphicsContext.fillText(menuItems[i], 456, itemY + 2);
            } else if ("GUIDE".equals(menuItems[i])) {
                graphicsContext.fillText(menuItems[i], 448, itemY + 2);
            } else {
                graphicsContext.fillText(menuItems[i], 460, itemY + 2);
            }
            if (selected) {
                graphicsContext.setFill(Color.web("#fff4d8"));
                graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
                graphicsContext.fillText(">", 365, itemY + 3);
            }
            itemY += 58;
        }
    }

    private void renderGuide() {
        drawMenuBackground();

        double panelX = 210;
        double panelY = 95;
        double panelW = 540;
        double panelH = 350;
        graphicsContext.setFill(Color.color(0.95, 0.92, 0.78, 0.92));
        graphicsContext.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        graphicsContext.setStroke(Color.web("#5b4a2e"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        graphicsContext.fillText("GUIDE", 430, 155);
        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        graphicsContext.fillText("W A S D : Move", 285, 215);
        graphicsContext.fillText("Mouse Left : Basic Attack", 285, 255);
        graphicsContext.fillText("F : Skill Attack", 285, 295);
        graphicsContext.fillText("K : Heal", 285, 335);

        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        graphicsContext.fillText("ENTER : Level Select", 265, 395);
        graphicsContext.fillText("ESC : Back to Menu", 510, 395);
    }

    private void renderNameInput(String playerNameDraft, int maxNameLength) {
        drawMenuBackground();

        double panelX = 220;
        double panelY = 140;
        double panelW = 520;
        double panelH = 240;
        graphicsContext.setFill(Color.color(0.95, 0.92, 0.78, 0.93));
        graphicsContext.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        graphicsContext.setStroke(Color.web("#5b4a2e"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        graphicsContext.fillText("ENTER NAME", 360, 192);

        double boxX = 280;
        double boxY = 220;
        double boxW = 400;
        double boxH = 52;
        graphicsContext.setFill(Color.web("#fff8e8"));
        graphicsContext.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        graphicsContext.setStroke(Color.web("#6f5a36"));
        graphicsContext.setLineWidth(2);
        graphicsContext.strokeRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        String shownName = playerNameDraft == null ? "" : playerNameDraft;
        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
        graphicsContext.fillText(shownName, boxX + 14, boxY + 34);

        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 20));
        graphicsContext.fillText("Length: " + shownName.length() + "/" + maxNameLength, boxX + 14, boxY + 74);
        graphicsContext.fillText("ENTER: Confirm", 285, 338);
        graphicsContext.fillText("ESC: Back", 520, 338);
    }

    private void renderLevelSelect(LevelManager levelManager, int selectedLevelId) {
        drawMenuBackground();

        graphicsContext.setFill(Color.web("#f4ead1"));
        graphicsContext.fillRoundRect(90, 72, 780, 430, 22, 22);
        graphicsContext.setStroke(Color.web("#5b4a2e"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeRoundRect(90, 72, 780, 430, 22, 22);

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        graphicsContext.fillText("LEVEL SELECT", 355, 120);
        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        graphicsContext.fillText("Completed: " + levelManager.getTotalCompletedLevels() + "   Stars: " + levelManager.getTotalStars(), 318, 146);

        List<Level> levels = levelManager.getAllLevels();
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            int col = i % 2;
            int row = i / 2;
            double x = 145 + col * 342;
            double y = 165 + row * 136;
            boolean selected = level.getId() == selectedLevelId;
            boolean unlocked = levelManager.getPlayerProgress().isLevelUnlocked(level.getId());
            int stars = levelManager.getPlayerProgress().getLevelStars(level.getId());

            graphicsContext.setFill(unlocked
                    ? (selected ? Color.web("#8a6936") : Color.web("#d9c59a"))
                    : Color.web("#9f9a8d"));
            graphicsContext.fillRoundRect(x, y, 300, 110, 16, 16);
            graphicsContext.setStroke(selected ? Color.web("#fff0c7") : Color.web("#5a4727"));
            graphicsContext.setLineWidth(selected ? 4 : 2);
            graphicsContext.strokeRoundRect(x, y, 300, 110, 16, 16);

            graphicsContext.setFill(unlocked ? Color.web("#2f2618") : Color.web("#5c564a"));
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
            graphicsContext.fillText("Level " + level.getId() + " - " + level.getName(), x + 16, y + 30);
            graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 16));
            graphicsContext.fillText("Difficulty: " + level.getDifficulty(), x + 16, y + 56);
            graphicsContext.fillText(level.getPrimaryObjectiveLabel(), x + 16, y + 80);
            graphicsContext.fillText(unlocked ? "Stars: " + stars : "Locked", x + 16, y + 100);
        }

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        graphicsContext.fillText("Arrow keys / WASD: move  |  ENTER: play  |  ESC: back", 235, 470);
    }

    private void renderGameplayWorld(Player player, List<Enemy> enemies, long now,
                                     double cameraX, double cameraY, List<ResourceNode> allResources,
                                     double darknessAlpha, boolean isNight, String dayNightPhase,
                                     Map<String, Integer> collectedResources,
                                     List<FloatingDamageText> floatingDamageTexts,
                                     double worldWidth, double worldHeight) {
        graphicsContext.setFill(Color.web("#1b1b1b"));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        graphicsContext.save();
        graphicsContext.scale(CAMERA_ZOOM, CAMERA_ZOOM);
        double renderCameraX = cameraX;
        double renderCameraY = cameraY;

        if (mapRenderer != null) {
            mapRenderer.setResources(allResources);
            mapRenderer.renderBelowEntities(graphicsContext, renderCameraX, renderCameraY, now);
        } else {
            if (gameBackgroundImage.isError()) {
                graphicsContext.setFill(Color.BEIGE);
                graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            } else {
                graphicsContext.drawImage(gameBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            }
        }

        player.draw(graphicsContext, renderCameraX, renderCameraY);
        renderLevelUpEffect(player, renderCameraX, renderCameraY, now);
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                enemy.draw(graphicsContext, renderCameraX, renderCameraY, now);
            }
        }

        if (mapRenderer != null) {
            mapRenderer.renderAboveEntities(graphicsContext, renderCameraX, renderCameraY, now);
        }

        graphicsContext.restore();
        renderFloatingDamageTexts(floatingDamageTexts, now, cameraX, cameraY);
        renderNightOverlayAndLights(player, cameraX, cameraY, darknessAlpha, isNight);

        graphicsContext.setFill(Color.color(1, 1, 1, 0.85));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText("Light: " + dayNightPhase + " alpha=" + String.format("%.2f", darknessAlpha), 14, 532);

        hud.render(graphicsContext, player, collectedResources);
        miniMap.render(graphicsContext, worldWidth, worldHeight, cameraX, cameraY, CAMERA_ZOOM, player, enemies);
    }

    private void renderLevelHeader(LevelManager levelManager, Level currentLevel, String objectiveText) {
        if (currentLevel == null) {
            return;
        }

        double panelX = 250;
        double panelY = 14;
        double panelW = 430;
        double panelH = 56;
        graphicsContext.setFill(Color.color(0, 0, 0, 0.34));
        graphicsContext.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
        graphicsContext.setStroke(Color.color(1, 1, 1, 0.24));
        graphicsContext.strokeRoundRect(panelX, panelY, panelW, panelH, 12, 12);

        graphicsContext.setFill(Color.web("#f5f0de"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 19));
        graphicsContext.fillText("Level " + currentLevel.getId() + ": " + currentLevel.getName(), panelX + 16, panelY + 22);

        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 13));
        graphicsContext.fillText("Objective: " + objectiveText, panelX + 16, panelY + 42);
        graphicsContext.fillText("Completed " + levelManager.getTotalCompletedLevels() + "/" + levelManager.getAllLevels().size(),
                panelX + 330, panelY + 22);
    }

    private void renderLevelComplete(LevelResult lastLevelResult, LevelManager levelManager) {
        String secondLine = lastLevelResult == null
                ? "ENTER: Next level"
                : "Score " + lastLevelResult.getScore() + "   Stars " + lastLevelResult.getStars();
        String thirdLine = "ENTER: Next  |  R: Retry  |  ESC: Level select";

        graphicsContext.setFill(Color.color(0, 0, 0, 0.52));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        graphicsContext.setFill(Color.web("#f4ead1"));
        graphicsContext.fillRoundRect(255, 155, 450, 190, 18, 18);
        graphicsContext.setStroke(Color.web("#5b4a2e"));
        graphicsContext.setLineWidth(3);
        graphicsContext.strokeRoundRect(255, 155, 450, 190, 18, 18);

        graphicsContext.setFill(Color.web("#2f2618"));
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        graphicsContext.fillText("LEVEL CLEAR", 365, 205);
        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        graphicsContext.fillText(secondLine, 322, 246);
        graphicsContext.fillText("Total Stars: " + levelManager.getTotalStars(), 374, 280);
        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        graphicsContext.fillText(thirdLine, 292, 317);
    }

    private void renderCenteredOverlay(String title, String lineOne, String lineTwo) {
        graphicsContext.setFill(Color.color(0, 0, 0, 0.45));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 42));
        graphicsContext.fillText(title, 365, 230);

        graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        graphicsContext.fillText(lineOne, 350, 280);
        graphicsContext.fillText(lineTwo, 320, 315);
    }

    private void drawMenuBackground() {
        if (welcomeBackgroundImage.isError()) {
            graphicsContext.setFill(Color.web("#2a3a2a"));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        } else {
            graphicsContext.drawImage(welcomeBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
        }
        graphicsContext.setFill(Color.color(0, 0, 0, 0.55));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
    }

    private void renderNightOverlayAndLights(Player player, double cameraX, double cameraY, double darknessAlpha, boolean isNight) {
        if (darknessAlpha <= 0.001) {
            return;
        }

        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphicsContext.setFill(Color.color(0, 0, 0, darknessAlpha));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        graphicsContext.save();
        graphicsContext.setGlobalBlendMode(BlendMode.SCREEN);

        double playerWorldX = player.getX() + player.getWidth() / 2.0;
        double playerWorldY = player.getY() + player.getHeight() / 2.0;
        double playerScreenX = (playerWorldX - cameraX) * CAMERA_ZOOM;
        double playerScreenY = (playerWorldY - cameraY) * CAMERA_ZOOM;
        double playerLightRadius = isNight ? 120 : 170;
        drawRadialLight(playerScreenX, playerScreenY, playerLightRadius, Color.color(1.0, 0.96, 0.86, 0.58));

        double bonfireWorldX = 1060;
        double bonfireWorldY = 520;
        double bonfireScreenX = (bonfireWorldX - cameraX) * CAMERA_ZOOM;
        double bonfireScreenY = (bonfireWorldY - cameraY) * CAMERA_ZOOM;
        drawRadialLight(bonfireScreenX, bonfireScreenY, 180, Color.color(1.0, 0.80, 0.40, 0.62));

        graphicsContext.restore();
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    private void drawRadialLight(double x, double y, double radius, Color centerColor) {
        RadialGradient gradient = new RadialGradient(
                0,
                0,
                x,
                y,
                radius,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, centerColor),
                new Stop(0.55, Color.color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), centerColor.getOpacity() * 0.36)),
                new Stop(1.0, Color.color(0, 0, 0, 0.0))
        );
        graphicsContext.setFill(gradient);
        graphicsContext.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private void renderLevelUpEffect(Player player, double cameraX, double cameraY, long now) {
        if (player == null || !player.isLevelUpEffectActive(now)) {
            return;
        }

        double progress = player.getLevelUpEffectProgress(now);
        double screenX = player.getX() - cameraX + player.getWidth() / 2.0;
        double screenY = player.getY() - cameraY - 12 - progress * 16;

        double alpha = 1.0 - progress;
        if (alpha < 0) {
            alpha = 0;
        }

        String text = "↑ LEVEL UP " + player.getLastLeveledUpTo();
        graphicsContext.save();
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
        double textWidth = measureTextWidth(graphicsContext, text);
        double textX = screenX - textWidth / 2.0;
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.75 * alpha));
        graphicsContext.strokeText(text, textX, screenY);
        graphicsContext.setFill(Color.color(1.0, 0.93, 0.46, 0.96 * alpha));
        graphicsContext.fillText(text, textX, screenY);
        graphicsContext.restore();
    }

    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }

    private void renderFloatingDamageTexts(List<FloatingDamageText> floatingDamageTexts, long now, double cameraX, double cameraY) {
        if (floatingDamageTexts == null || floatingDamageTexts.isEmpty()) {
            return;
        }
        graphicsContext.save();
        for (FloatingDamageText text : floatingDamageTexts) {
            if (text == null || text.isExpired(now)) {
                continue;
            }
            double progress = text.getProgress(now);
            double alpha = 1.0 - progress;
            double lift = 22.0 * progress;
            double sx = (text.getWorldX() - cameraX) * CAMERA_ZOOM;
            double sy = (text.getWorldY() - cameraY) * CAMERA_ZOOM - lift;
            double fontSize = text.isCritical() ? 21 : 15;
            Color fillColor = text.isCritical()
                    ? Color.color(1.0, 0.86, 0.22, 0.98 * alpha)
                    : Color.color(1.0, 1.0, 1.0, 0.96 * alpha);

            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, fontSize));
            double textWidth = measureTextWidth(graphicsContext, text.getText());
            double tx = sx - textWidth / 2.0;
            graphicsContext.setStroke(Color.color(0, 0, 0, 0.76 * alpha));
            graphicsContext.strokeText(text.getText(), tx, sy);
            graphicsContext.setFill(fillColor);
            graphicsContext.fillText(text.getText(), tx, sy);
        }
        graphicsContext.restore();
    }
}
