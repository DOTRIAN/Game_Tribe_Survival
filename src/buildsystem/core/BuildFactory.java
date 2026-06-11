package buildsystem.core;

import buildsystem.object.BuildObject;

/**
 * BuildFactory:
 * - Tao BuildObject theo BuildDefinition.
 * - Vi sao can:
 *   1) Gameplay code khong duoc new tung object o nhieu noi.
 *   2) Save/load chi can dua vao registry + snapshot de restore.
 */
public class BuildFactory {
    public BuildObject create(BuildDefinition definition,
                              String objectId,
                              int tileX,
                              int tileY,
                              int tileWidth,
                              int tileHeight,
                              String spriteKey,
                              double rotationDegrees,
                              int health) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        BuildObjectSeed seed = new BuildObjectSeed(
                objectId,
                definition,
                tileX,
                tileY,
                tileWidth,
                tileHeight,
                spriteKey,
                rotationDegrees,
                health
        );
        return definition.getObjectBuilder().create(seed);
    }
}
