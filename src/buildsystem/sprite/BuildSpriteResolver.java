package buildsystem.sprite;

import buildsystem.core.BuildDefinition;
import buildsystem.object.BuildObject;

import java.util.Collection;

/**
 * BuildSpriteResolver:
 * - Chon sprite theo definition thay vi hardcode wall-only.
 * - AutoTileResolver duoc dung khi definition co autoTileGroup.
 */
public class BuildSpriteResolver {
    private final AutoTileResolver autoTileResolver;

    public BuildSpriteResolver(AutoTileResolver autoTileResolver) {
        this.autoTileResolver = autoTileResolver;
    }

    public SpriteSelection resolve(BuildDefinition definition,
                                   int tileX,
                                   int tileY,
                                   Collection<BuildObject> placedObjects,
                                   double preferredRotationDegrees,
                                   String fallbackSpriteKey) {
        if (definition == null) {
            return new SpriteSelection(fallbackSpriteKey, preferredRotationDegrees, 0);
        }
        if (definition.getAutoTileGroup() != null && !definition.getAutoTileGroup().isBlank()) {
            return autoTileResolver.resolve(definition, tileX, tileY, placedObjects, preferredRotationDegrees);
        }
        return new SpriteSelection(
                fallbackSpriteKey == null ? definition.getDefaultSpriteKey() : fallbackSpriteKey,
                preferredRotationDegrees,
                0
        );
    }
}
