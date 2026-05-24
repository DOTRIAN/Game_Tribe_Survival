package buildsystem.fence;

import buildsystem.component.CollisionComponent;

public final class FenceCollision {
    public static final double WIDTH_SCALE = 0.84;
    public static final double HEIGHT_SCALE = 0.82;

    private FenceCollision() {
    }

    public static CollisionComponent create(int tileWidth, int tileHeight) {
        return new CollisionComponent(
                Math.max(1.0, tileWidth * WIDTH_SCALE),
                Math.max(1.0, tileHeight * HEIGHT_SCALE)
        );
    }
}
