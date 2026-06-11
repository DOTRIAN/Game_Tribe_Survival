package buildsystem.fence;

import buildsystem.core.BuildDefinition;
import buildsystem.object.BuildObject;
import buildsystem.sprite.SpriteSelection;

import java.util.Collection;

public class FenceConnectionSystem {
    public SpriteSelection resolve(BuildDefinition definition,
                                   int tileX,
                                   int tileY,
                                   Collection<BuildObject> placedObjects) {
        return new SpriteSelection(FenceRenderer.SINGLE_SPRITE_KEY, 0.0, 0);
    }

    public int[][] tilesToRefresh(int tileX, int tileY) {
        return new int[][]{
                {tileX, tileY}
        };
    }
}
