package ui;

import core.GameConfig;
import core.GameState;
import entity.Player;
import entity.Tree;
import entity.Wolf;
import input.InputHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import map.MapData;
import map.MapRenderer;

import java.util.List;

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
    private MapRenderer mapRenderer;

    public Renderer(Stage stage, InputHandler inputHandler) {
        this.canvas = new Canvas(GameConfig.WIDTH, GameConfig.HEIGHT);
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.hud = new Hud();
        this.welcomeBackgroundImage = new Image("file:assets/backgrounds/menu_bg1.png");
        this.gameBackgroundImage = new Image("file:assets/backgrounds/grass03.png");

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

    public void render(GameState gameState, Player player, Wolf wolf, long now, boolean wolfMoving,
                       List<Tree> trees, double cameraX, double cameraY, int menuIndex, boolean welcomeFlashing,
                       String playerNameDraft, int maxNameLength) {
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
            graphicsContext.fillText("J : Take Damage (Test)", 285, 255);
            graphicsContext.fillText("K : Heal (Test)", 285, 295);

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
            mapRenderer.renderBelowEntities(graphicsContext, renderCameraX, renderCameraY, now);
        } else {
            if (gameBackgroundImage.isError()) {
                graphicsContext.setFill(Color.BEIGE);
                graphicsContext.fillRect(0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            } else {
                graphicsContext.drawImage(gameBackgroundImage, 0, 0, GameConfig.WIDTH, GameConfig.HEIGHT);
            }
        }

        for (Tree tree : trees) {
            if (mapRenderer == null) {
                tree.draw(graphicsContext, renderCameraX, renderCameraY);
            }
        }

        // Entity duoc ve sau layer duoi, truoc layer tren.
        player.draw(graphicsContext, renderCameraX, renderCameraY);
        if (wolf != null) {
            wolf.draw(graphicsContext, renderCameraX, renderCameraY, now, wolfMoving, player);
        }

        if (mapRenderer != null) {
            mapRenderer.renderAboveEntities(graphicsContext, renderCameraX, renderCameraY, now);
        }

        // Ket thuc world-space rendering, tra lai he toa do man hinh.
        graphicsContext.restore();

        // Da an toan bo text debug/hint cu de HUD gon hon.
        // Neu can bat lai, chi can bo comment cac dong duoi:
        // graphicsContext.setFill(Color.DARKGREEN);
        // graphicsContext.fillText("State: " + gameState, 20, 30);
        // graphicsContext.fillText("WASD: move", 20, 55);
        // graphicsContext.fillText("J: take damage | K: heal", 20, 80);

        hud.render(graphicsContext, player);

        if (gameState == GameState.GAME_OVER) {
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillText("GAME OVER", 400, 250);

            graphicsContext.setFill(Color.BLACK);
            graphicsContext.fillText("The wolf caught you.", 380, 280);
            graphicsContext.fillText("Press R to restart.", 385, 310);
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

    // Ham helper do text width theo font hien tai cua graphics context.
    // Hien tai dang du phong cho canh chinh text dong trong cac UI tiep theo.
    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }
}
