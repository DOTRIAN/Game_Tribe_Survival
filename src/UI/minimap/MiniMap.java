package ui.minimap;

import entity.Enemy;
import entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * MiniMap:
 * - Vung ban do thu nho de nguoi choi xac dinh vi tri tong quat trong world.
 * - Class nay chi render UI overlay, KHONG can thiep gameplay logic.
 *
 * Nguyen tac:
 * 1) Nhan du lieu world-space (worldW/worldH, player, enemies, camera)
 * 2) Quy doi sang minimap-space theo scaleX/scaleY
 * 3) Ve panel + marker theo thu tu de de doc
 */
public class MiniMap {
    // Kich thuoc panel minimap.
    private static final double PANEL_WIDTH = 190;
    private static final double PANEL_HEIGHT = 138;
    // Dat minimap o goc phai duoi de tranh de len HUD tai nguyen goc phai tren.
    private static final double PANEL_MARGIN_RIGHT = 16;
    private static final double PANEL_MARGIN_BOTTOM = 16;
    // Le trong de markers khong sat vien.
    private static final double INNER_PADDING = 8;
    // Bo goc panel.
    private static final double PANEL_ARC = 10;

    // Mau co ban.
    private static final Color PANEL_BG = Color.color(0, 0, 0, 0.42);
    private static final Color PANEL_BORDER = Color.color(1, 1, 1, 0.30);
    private static final Color WORLD_BG = Color.color(0.12, 0.18, 0.12, 0.90);
    private static final Color WORLD_BORDER = Color.color(1, 1, 1, 0.25);
    private static final Color CAMERA_RECT = Color.color(1, 1, 1, 0.45);
    private static final Color PLAYER_DOT = Color.web("#4dd2ff");
    private static final Color ENEMY_DOT = Color.web("#ff7866");

    /**
     * render:
     * - Ve minimap tren screen-space (khong scale theo camera zoom gameplay).
     * - worldW/worldH la kich thuoc world thuc te dang dung.
     */
    public void render(GraphicsContext gc,
                       double worldW,
                       double worldH,
                       double cameraX,
                       double cameraY,
                       double cameraZoom,
                       double viewportWidth,
                       double viewportHeight,
                       Player player,
                       List<Enemy> enemies) {
        if (gc == null || player == null) {
            return;
        }
        if (worldW <= 0 || worldH <= 0) {
            return;
        }

        // Toa do panel tren man hinh.
        double panelX = viewportWidth - PANEL_MARGIN_RIGHT - PANEL_WIDTH;
        double panelY = viewportHeight - PANEL_MARGIN_BOTTOM - PANEL_HEIGHT;

        // Vung world hien thi ben trong panel.
        double mapX = panelX + INNER_PADDING;
        double mapY = panelY + INNER_PADDING;
        double mapW = PANEL_WIDTH - INNER_PADDING * 2;
        double mapH = PANEL_HEIGHT - INNER_PADDING * 2;

        // Scale world -> minimap.
        double scaleX = mapW / worldW;
        double scaleY = mapH / worldH;

        // 1) Panel bao ngoai.
        gc.setFill(PANEL_BG);
        gc.fillRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_ARC, PANEL_ARC);
        gc.setStroke(PANEL_BORDER);
        gc.strokeRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_ARC, PANEL_ARC);

        // 2) Nen world.
        gc.setFill(WORLD_BG);
        gc.fillRect(mapX, mapY, mapW, mapH);
        gc.setStroke(WORLD_BORDER);
        gc.strokeRect(mapX, mapY, mapW, mapH);

        // 3) Camera viewport rectangle.
        // Viewport world = screen / zoom.
        // Camera rect bat buoc dung viewport runtime, neu khong minimap se sai khi fullscreen.
        double viewWorldW = viewportWidth / cameraZoom;
        double viewWorldH = viewportHeight / cameraZoom;
        double camRectX = mapX + cameraX * scaleX;
        double camRectY = mapY + cameraY * scaleY;
        double camRectW = viewWorldW * scaleX;
        double camRectH = viewWorldH * scaleY;
        gc.setStroke(CAMERA_RECT);
        gc.strokeRect(camRectX, camRectY, camRectW, camRectH);

        // 4) Marker player (lay tam sprite cho nhin chinh xac hon).
        double playerCenterX = player.getX() + player.getWidth() / 2.0;
        double playerCenterY = player.getY() + player.getHeight() / 2.0;
        drawDot(gc, mapX + playerCenterX * scaleX, mapY + playerCenterY * scaleY, 4.2, PLAYER_DOT);

        // 5) Marker enemy.
        // Ve toi da 24 cham de minimap gon va de doc.
        if (enemies != null) {
            int rendered = 0;
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) {
                    continue;
                }
                double enemyCenterX = enemy.getX() + enemy.getWidth() / 2.0;
                double enemyCenterY = enemy.getY() + enemy.getHeight() / 2.0;
                drawDot(gc, mapX + enemyCenterX * scaleX, mapY + enemyCenterY * scaleY, 3.2, ENEMY_DOT);
                rendered++;
                if (rendered >= 24) {
                    break;
                }
            }
        }
    }

    // Ve cham marker nho, co vien den mong de de doc tren nen toi/sang.
    private void drawDot(GraphicsContext gc, double x, double y, double radius, Color fill) {
        double size = radius * 2;
        gc.setFill(fill);
        gc.fillOval(x - radius, y - radius, size, size);
        gc.setStroke(Color.color(0, 0, 0, 0.75));
        gc.strokeOval(x - radius, y - radius, size, size);
    }
}
