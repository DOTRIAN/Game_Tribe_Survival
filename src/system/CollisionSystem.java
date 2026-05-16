package system;

import entity.Entity;

/**
 * CollisionSystem:
 * - Gom logic va cham AABB ve 1 noi thay vi moi class tu check thu cong.
 * - Ho tro:
 *   1) Entity vs Entity
 *   2) Entity vs Rect
 *   3) Rect vs Rect
 */
public final class CollisionSystem {
    private CollisionSystem() {
    }

    public static boolean intersects(Entity left, Entity right) {
        if (left == null || right == null) {
            return false;
        }
        return intersects(
                left.getX(), left.getY(), left.getWidth(), left.getHeight(),
                right.getX(), right.getY(), right.getWidth(), right.getHeight()
        );
    }

    public static boolean intersects(Entity entity, double x, double y, double width, double height) {
        if (entity == null) {
            return false;
        }
        return intersects(entity.getX(), entity.getY(), entity.getWidth(), entity.getHeight(), x, y, width, height);
    }

    public static boolean intersects(double leftX, double leftY, double leftW, double leftH,
                                     double rightX, double rightY, double rightW, double rightH) {
        return leftX < rightX + rightW
                && leftX + leftW > rightX
                && leftY < rightY + rightH
                && leftY + leftH > rightY;
    }
}
