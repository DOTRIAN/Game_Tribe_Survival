package buildsystem.fence;

import buildsystem.component.CollisionComponent;

public final class FenceCollision {
    private FenceCollision() {
    }

    public static CollisionComponent create(int tileWidth, int tileHeight) {
        return new CollisionComponent(Math.max(1, tileWidth), Math.max(1, tileHeight));
    }
}
