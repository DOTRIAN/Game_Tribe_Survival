package buildsystem.component;

/**
 * CollisionComponent:
 * - Luu hitbox runtime de PlacementValidator va collision runtime su dung chung.
 */
public class CollisionComponent implements BuildComponent {
    private final double width;
    private final double height;

    public CollisionComponent(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
