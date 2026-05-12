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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import map.MapData;
import map.MapRenderer;
import system.resource.ResourceNode;
import ui.minimap.MiniMap;

import java.util.List;
import java.util.Map;

public class Renderer {
    // Zoom camera cho gameplay:
    // - > 1.0: nhin gan hon (entity/map to hon tren man hinh)
    // - = 1.0: giu nguyen
    // - < 1.0: nhin xa hon
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
                       double darknessAlpha, boolean isNight, String dayNightPhase,
                       double worldWidth, double worldHeight) {
        if (gameState == GameState.WELCOME) {
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

            return;
        }

        if (gameState == GameState.GUIDE) {
            if (welcomeBackgroundImage.isError()) {
                graphicsContext.setFill(Color.web("#2a3a2a"));
                graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            } else {
                graphicsContext.drawImage(welcomeBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            }

            graphicsContext.setFill(Color.color(0, 0, 0, 0.55));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

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
            graphicsContext.fillText("F : Use skill", 285, 255);
            graphicsContext.fillText("K : Heal", 285, 295);

            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
            graphicsContext.fillText("ENTER : Play", 285, 355);
            graphicsContext.fillText("ESC : Back to Menu", 470, 355);

            return;
        }

        if (gameState == GameState.NAME_INPUT) {
            // Man hinh nhap ten:
            // - su dung nen menu de giu tinh dong bo voi flow WELCOME
            // - cho nguoi choi xem ten dang go (draft)
            // - ENTER xac nhan, ESC quay lai menu
            if (welcomeBackgroundImage.isError()) {
                graphicsContext.setFill(Color.web("#2a3a2a"));
                graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            } else {
                graphicsContext.drawImage(welcomeBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            }

            graphicsContext.setFill(Color.color(0, 0, 0, 0.55));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

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
            // Tang tieu de de de doc hon.
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
            graphicsContext.fillText("ENTER NAME", 360, 192);

            // Input box.
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
            // Tang size text nhap ten de nguoi choi nhin ro ky tu dang go.
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
            graphicsContext.fillText(shownName, boxX + 14, boxY + 34);

            graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 20));
            graphicsContext.fillText("Length: " + shownName.length() + "/" + maxNameLength, boxX + 14, boxY + 74);
            graphicsContext.fillText("ENTER: Confirm", 285, 338);
            graphicsContext.fillText("ESC: Back", 520, 338);
            return;
        }

        // Gameplay: clear full canvas de tranh bong frame cu.
        graphicsContext.setFill(Color.web("#1b1b1b"));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        // Bat dau world-space rendering.
        // save() giu lai trang thai transform hien tai.
        graphicsContext.save();
        // scale() phong to toan bo world (map + player + wolf).
        graphicsContext.scale(CAMERA_ZOOM, CAMERA_ZOOM);

        // Luu y quan trong:
        // cameraX/cameraY la world offset goc (chua zoom), vi vay KHONG duoc chia cho zoom.
        // Cong thuc dung la: screen = zoom * (world - camera).
        // Neu chia camera cho zoom se gay lech tam nhin va co the lam mat entity tren man hinh.
        double renderCameraX = cameraX;
        double renderCameraY = cameraY;

        if (mapRenderer != null) {
            // Dong bo danh sach resource de renderer co the an tile object/foreground cua node da bi pha.
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

        // Entity duoc ve sau layer duoi, truoc layer tren.
        player.draw(graphicsContext, renderCameraX, renderCameraY);
        // Hieu ung level-up popup ngay tren nhan vat (world-space).
        renderLevelUpEffect(player, renderCameraX, renderCameraY, now);
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                enemy.draw(graphicsContext, renderCameraX, renderCameraY);
            }
        }

        if (mapRenderer != null) {
            mapRenderer.renderAboveEntities(graphicsContext, renderCameraX, renderCameraY, now);
        }

        // Ket thuc world-space rendering, tra lai he toa do man hinh.
        graphicsContext.restore();

        // ===== Day/Night overlay + local lights =====
        // - Ve sau world de toi toan canh.
        // - Ve truoc HUD de HUD van de doc.
        renderNightOverlayAndLights(player, cameraX, cameraY, darknessAlpha, isNight);

        // Debug text nho: giup test nhanh chu ky day/night khi can.
        graphicsContext.setFill(Color.color(1, 1, 1, 0.85));
        graphicsContext.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        graphicsContext.fillText("Light: " + dayNightPhase + " alpha=" + String.format("%.2f", darknessAlpha), 14, 532);

        // Da an toan bo text debug/hint cu de HUD gon hon.
        // Neu can bat lai, chi can bo comment cac dong duoi:
        // graphicsContext.setFill(Color.DARKGREEN);
        // graphicsContext.fillText("State: " + gameState, 20, 30);
        // graphicsContext.fillText("WASD: move", 20, 55);
        // graphicsContext.fillText("J: take damage | K: heal", 20, 80);

        hud.render(graphicsContext, player, collectedResources);
        // Minimap la UI overlay doc lap.
        // Dat sau world rendering de khong bi anh huong boi camera zoom cua gameplay.
        miniMap.render(
                graphicsContext,
                worldWidth,
                worldHeight,
                cameraX,
                cameraY,
                CAMERA_ZOOM,
                player,
                enemies
        );

        if (gameState == GameState.GAME_OVER) {
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 50));
            graphicsContext.fillText("GAME OVER", 375, 250);

            graphicsContext.setFill(Color.BLACK);
            graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 26));
            graphicsContext.fillText("Player is dead.", 375, 280);
            graphicsContext.fillText("Press R to restart.", 375, 310);
        }

        if (gameState == GameState.PAUSED) {
            graphicsContext.setFill(Color.color(0, 0, 0, 0.45));
            graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

            graphicsContext.setFill(Color.WHITE);
            graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 42));
            graphicsContext.fillText("PAUSED", 390, 230);

            graphicsContext.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
            graphicsContext.fillText("Press P to Resume", 360, 280);
            graphicsContext.fillText("Press ESC to Menu", 355, 315);
        }
    }

    /**
     * renderNightOverlayAndLights:
     * - Mo phong "troi toi dan" bang lop den alpha.
     * - Tao "vung sang" bang hieu ung radial light (kieu duc lo, nhung than thien Canvas JavaFX).
     * - Vung sang hien tai:
     *   1) Quanh player (de gameplay khong bi mu)
     *   2) 1 diem lua tinh tren map (tam thoi, co the doi sang object light sau)
     */
    private void renderNightOverlayAndLights(Player player, double cameraX, double cameraY, double darknessAlpha, boolean isNight) {
        if (darknessAlpha <= 0.001) {
            return;
        }

        // 1) Phu lop toi toan man hinh.
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphicsContext.setFill(Color.color(0, 0, 0, darknessAlpha));
        graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);

        // 2) Ve vung sang bo sung bang blend SCREEN de "day lui" bong toi.
        graphicsContext.save();
        graphicsContext.setGlobalBlendMode(BlendMode.SCREEN);

        // Vung sang quanh player:
        // - Ban dem radius nho hon de tao cam giac gioi han tam nhin.
        // - Luc dusk/dawn van co sang nhe cho de chiu.
        double playerWorldX = player.getX() + player.getWidth() / 2.0;
        double playerWorldY = player.getY() + player.getHeight() / 2.0;
        double playerScreenX = (playerWorldX - cameraX) * CAMERA_ZOOM;
        double playerScreenY = (playerWorldY - cameraY) * CAMERA_ZOOM;
        double playerLightRadius = isNight ? 120 : 170;
        drawRadialLight(playerScreenX, playerScreenY, playerLightRadius, Color.color(1.0, 0.96, 0.86, 0.58));

        // Bonfire light tam thoi:
        // - Dung toa do world hard-code theo map hien tai de co "dom lua sang".
        // - Sau nay nen doi sang object light trong map de map artist tu quan ly.
        double bonfireWorldX = 1060;
        double bonfireWorldY = 520;
        double bonfireScreenX = (bonfireWorldX - cameraX) * CAMERA_ZOOM;
        double bonfireScreenY = (bonfireWorldY - cameraY) * CAMERA_ZOOM;
        drawRadialLight(bonfireScreenX, bonfireScreenY, 180, Color.color(1.0, 0.80, 0.40, 0.62));

        graphicsContext.restore();
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    // Ve 1 radial light tam x,y ban kinh radius.
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

    // Ve popup level-up de nguoi choi thay feedback "manh hon" ngay lap tuc.
    private void renderLevelUpEffect(Player player, double cameraX, double cameraY, long now) {
        if (player == null || !player.isLevelUpEffectActive(now)) {
            return;
        }

        double progress = player.getLevelUpEffectProgress(now); // 0 -> 1
        double screenX = player.getX() - cameraX + player.getWidth() / 2.0;
        double screenY = player.getY() - cameraY - 12 - progress * 16; // bay len nhe

        double alpha = 1.0 - progress;
        if (alpha < 0) {
            alpha = 0;
        }

        String text = "↑ LEVEL UP " + player.getLastLeveledUpTo();
        graphicsContext.save();
        graphicsContext.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
        double textWidth = measureTextWidth(graphicsContext, text);
        double textX = screenX - textWidth / 2.0;

        // Vien den mong de doc ro tren nen bat ky.
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.75 * alpha));
        graphicsContext.strokeText(text, textX, screenY);
        graphicsContext.setFill(Color.color(1.0, 0.93, 0.46, 0.96 * alpha));
        graphicsContext.fillText(text, textX, screenY);
        graphicsContext.restore();
    }

    // Ham helper do text width theo font hien tai cua graphics context.
    // Hien tai dang du phong cho canh chinh text dong trong cac UI tiep theo.
    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }
}
