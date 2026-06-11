package buildsystem.core;

import buildsystem.object.BuildObject;

/**
 * BuildDamageResult:
 * - Goi gon ket qua khi danh vao 1 build object.
 * - Cho phep Game xu ly floating text, drop va pickup ma khong can biet noi bo BuildManager.
 */
public class BuildDamageResult {
    private final BuildObject object;
    private final int damageApplied;
    private final boolean destroyed;
    private final String dropItemId;
    private final int dropAmount;

    public BuildDamageResult(BuildObject object,
                             int damageApplied,
                             boolean destroyed,
                             String dropItemId,
                             int dropAmount) {
        this.object = object;
        this.damageApplied = Math.max(0, damageApplied);
        this.destroyed = destroyed;
        this.dropItemId = dropItemId == null ? "" : dropItemId;
        this.dropAmount = Math.max(0, dropAmount);
    }

    public BuildObject getObject() {
        return object;
    }

    public int getDamageApplied() {
        return damageApplied;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public String getDropItemId() {
        return dropItemId;
    }

    public int getDropAmount() {
        return dropAmount;
    }
}
