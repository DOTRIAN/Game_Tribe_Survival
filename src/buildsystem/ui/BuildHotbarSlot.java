package buildsystem.ui;

import buildsystem.core.BuildDefinition;

/**
 * BuildHotbarSlot:
 * - Du lieu 1 slot tren BuildToolbar.
 * - Slot nay dung chung cho game chinh, demo va editor:
 *   1) definition de biet dang build gi.
 *   2) count de biet inventory con bao nhieu.
 *   3) cost de nhac gia dat.
 */
public class BuildHotbarSlot {
    private final BuildDefinition definition;
    private final int count;
    private final int cost;

    public BuildHotbarSlot(BuildDefinition definition, int count, int cost) {
        this.definition = definition;
        this.count = count;
        this.cost = cost;
    }

    public BuildDefinition getDefinition() {
        return definition;
    }

    public int getCount() {
        return count;
    }

    public int getCost() {
        return cost;
    }
}
