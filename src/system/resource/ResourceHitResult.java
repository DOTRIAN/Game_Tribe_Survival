package system.resource;

/**
 * ResourceHitResult:
 * - Ket qua moi lan danh vao resource (khong can cho den luc bi pha).
 * - Chua damage thuc te, trang thai destroyed, va drop neu co.
 */
public class ResourceHitResult {
    private final ResourceNode resourceNode;
    private final int damageApplied;
    private final boolean destroyed;
    private final DropResult dropResult;

    public ResourceHitResult(ResourceNode resourceNode, int damageApplied, boolean destroyed, DropResult dropResult) {
        this.resourceNode = resourceNode;
        this.damageApplied = Math.max(0, damageApplied);
        this.destroyed = destroyed;
        this.dropResult = dropResult;
    }

    public ResourceNode getResourceNode() {
        return resourceNode;
    }

    public int getDamageApplied() {
        return damageApplied;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public DropResult getDropResult() {
        return dropResult;
    }
}
