package wallbuilder.world;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import wallbuilder.collision.CollisionManager;
import wallbuilder.util.GridUtils;

/**
 * Player:
 * - Nhan vat test don gian de kiem tra collision.
 * - Dung rectangle de giam code khong can thiet, tap trung vao build system.
 */
public class Player {
    private static final double MOVE_SPEED = 3.2;

    private final Rectangle body;
    private double x;
    private double y;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
        this.body = new Rectangle(30, 30, Color.web("#23364a"));
        this.body.setArcWidth(8);
        this.body.setArcHeight(8);
        this.body.setStroke(Color.WHITE);
        syncView();
    }

    /**
     * move:
     * - Input: huong x/y va collision manager.
     * - Output: khong tra ve, cap nhat vi tri neu hop le.
     * - Tac dong gameplay: player khong di xuyen qua wall va vat can.
     */
    public void move(double deltaX, double deltaY, CollisionManager collisionManager) {
        double nextX = x + deltaX * MOVE_SPEED;
        double nextY = y + deltaY * MOVE_SPEED;

        if (!collisionManager.collides(nextX, y, body.getWidth(), body.getHeight())) {
            x = nextX;
        }
        if (!collisionManager.collides(x, nextY, body.getWidth(), body.getHeight())) {
            y = nextY;
        }
        syncView();
    }

    public Rectangle getBody() {
        return body;
    }

    public int getOccupiedTileColumn() {
        return GridUtils.snapToTile(x + body.getWidth() * 0.5);
    }

    public int getOccupiedTileRow() {
        return GridUtils.snapToTile(y + body.getHeight() * 0.5);
    }

    private void syncView() {
        body.setLayoutX(x);
        body.setLayoutY(y);
    }
}
