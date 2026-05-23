package buildsystem.fence;

import buildsystem.core.BuildDefinition;

public final class FenceDropSystem {
    private FenceDropSystem() {
    }

    public static String resolveDropItemId(BuildDefinition definition) {
        return definition == null ? "" : definition.getItemId();
    }

    public static int resolveDropAmount(BuildDefinition definition) {
        return definition == null ? 0 : 1;
    }
}
