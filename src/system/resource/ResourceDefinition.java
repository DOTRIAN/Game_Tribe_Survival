package system.resource;

/**
 * ResourceDefinition:
 * - Dinh nghia "template" cho 1 loai tai nguyen.
 * - Day la du lieu tinh: ten loai, hp mac dinh, item roi ra, so luong roi ra.
 * - Runtime state (hp hien tai, da bi pha hay chua) KHONG nam o day.
 */
public class ResourceDefinition {
    private final String kindKey;
    private final ResourceType resourceType;
    private final int defaultMaxHp;
    private final String defaultDropItem;
    private final int defaultDropMin;
    private final int defaultDropMax;
    private final int defaultRespawnSeconds;

    public ResourceDefinition(String kindKey,
                              ResourceType resourceType,
                              int defaultMaxHp,
                              String defaultDropItem,
                              int defaultDropMin,
                              int defaultDropMax,
                              int defaultRespawnSeconds) {
        this.kindKey = kindKey;
        this.resourceType = resourceType;
        this.defaultMaxHp = Math.max(1, defaultMaxHp);
        this.defaultDropItem = defaultDropItem == null ? "" : defaultDropItem;
        this.defaultDropMin = Math.max(0, defaultDropMin);
        this.defaultDropMax = Math.max(this.defaultDropMin, defaultDropMax);
        this.defaultRespawnSeconds = defaultRespawnSeconds;
    }

    public String getKindKey() {
        return kindKey;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public int getDefaultMaxHp() {
        return defaultMaxHp;
    }

    public String getDefaultDropItem() {
        return defaultDropItem;
    }

    public int getDefaultDropMin() {
        return defaultDropMin;
    }

    public int getDefaultDropMax() {
        return defaultDropMax;
    }

    public int getDefaultRespawnSeconds() {
        return defaultRespawnSeconds;
    }
}

