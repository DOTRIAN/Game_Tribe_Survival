package buildsystem.object;

import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.InventoryComponent;
import buildsystem.core.BuildDefinition;
import buildsystem.core.BuildObjectSeed;

/** Chest/workbench-like object co inventory rieng hoac station state rieng. */
public class Chest extends BuildObject {
    private static final long AJAR_DURATION_NS = 180_000_000L;
    private static final long OPEN_DURATION_NS = 9_000_000_000L;
    private long openedAtNs;

    public Chest(BuildDefinition definition, BuildObjectSeed seed) {
        super(seed.getObjectId(),
                definition.getType(),
                seed.getTileX(),
                seed.getTileY(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                definition.getAutoTileGroup(),
                seed.getTileWidth(),
                seed.getTileHeight(),
                0,
                0,
                seed.getSpriteKey(),
                seed.getRotationDegrees(),
                seed.getHealth());
        addComponent(new CollisionComponent(seed.getTileWidth(), seed.getTileHeight()));
        addComponent(new HealthComponent(seed.getHealth(), definition.getHealth()));
        addComponent(new InventoryComponent());
        this.openedAtNs = -1L;
    }

    public void openTemporarily(long nowNs) {
        this.openedAtNs = nowNs <= 0 ? System.nanoTime() : nowNs;
    }

    public void close() {
        this.openedAtNs = -1L;
    }

    public boolean isOpen(long nowNs) {
        if (openedAtNs < 0L) {
            return false;
        }
        return nowNs - openedAtNs < OPEN_DURATION_NS;
    }

    public boolean isAjar(long nowNs) {
        if (!isOpen(nowNs)) {
            return false;
        }
        return nowNs - openedAtNs < AJAR_DURATION_NS;
    }
}
