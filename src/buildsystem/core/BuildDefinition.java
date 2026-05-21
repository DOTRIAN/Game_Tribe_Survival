package buildsystem.core;

import buildsystem.object.BuildObject;
import buildsystem.placement.PlacementStrategy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BuildDefinition:
 * - Metadata trung tam cho moi loai buildable object.
 * - Day la "hop dong" de BuildManager/Facotry/Toolbar/Save-Load cung doc chung 1 nguon.
 *
 * Build flow:
 * 1. Registry dang ky BuildDefinition.
 * 2. Toolbar/selection doc definition de hien thi item build.
 * 3. Preview/placement doc rule trong definition.
 * 4. Factory doc definition de tao BuildObject that.
 * 5. Save/load doc type/id/health/sprite/rotation tu definition + snapshot.
 */
public class BuildDefinition {
    @FunctionalInterface
    public interface ObjectBuilder {
        BuildObject create(BuildObjectSeed seed);
    }

    private final BuildType type;
    private final String itemId;
    private final String displayName;
    private final String defaultSpriteKey;
    private final String iconSpriteKey;
    private final PlacementStrategy placementStrategy;
    private final boolean rotatable;
    private final boolean toolbarVisible;
    private final boolean collisionEnabled;
    private final boolean waterRestricted;
    private final boolean requiresFlatTerrain;
    private final int health;
    private final int buildCost;
    private final int footprintWidthTiles;
    private final int footprintHeightTiles;
    private final String autoTileGroup;
    private final Map<BuildType, Integer> minDistanceByType;
    private final ObjectBuilder objectBuilder;

    private BuildDefinition(Builder builder) {
        this.type = builder.type;
        this.itemId = builder.itemId;
        this.displayName = builder.displayName;
        this.defaultSpriteKey = builder.defaultSpriteKey;
        this.iconSpriteKey = builder.iconSpriteKey;
        this.placementStrategy = builder.placementStrategy;
        this.rotatable = builder.rotatable;
        this.toolbarVisible = builder.toolbarVisible;
        this.collisionEnabled = builder.collisionEnabled;
        this.waterRestricted = builder.waterRestricted;
        this.requiresFlatTerrain = builder.requiresFlatTerrain;
        this.health = builder.health;
        this.buildCost = builder.buildCost;
        this.footprintWidthTiles = builder.footprintWidthTiles;
        this.footprintHeightTiles = builder.footprintHeightTiles;
        this.autoTileGroup = builder.autoTileGroup;
        this.minDistanceByType = Collections.unmodifiableMap(new LinkedHashMap<>(builder.minDistanceByType));
        this.objectBuilder = builder.objectBuilder;
    }

    public static Builder builder(BuildType type, String itemId, String displayName) {
        return new Builder(type, itemId, displayName);
    }

    public BuildType getType() {
        return type;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultSpriteKey() {
        return defaultSpriteKey;
    }

    public String getIconSpriteKey() {
        return iconSpriteKey;
    }

    public PlacementStrategy getPlacementStrategy() {
        return placementStrategy;
    }

    public boolean isRotatable() {
        return rotatable;
    }

    public boolean isToolbarVisible() {
        return toolbarVisible;
    }

    public boolean isCollisionEnabled() {
        return collisionEnabled;
    }

    public boolean isWaterRestricted() {
        return waterRestricted;
    }

    public boolean isRequiresFlatTerrain() {
        return requiresFlatTerrain;
    }

    public int getHealth() {
        return health;
    }

    public int getBuildCost() {
        return buildCost;
    }

    public int getFootprintWidthTiles() {
        return footprintWidthTiles;
    }

    public int getFootprintHeightTiles() {
        return footprintHeightTiles;
    }

    public String getAutoTileGroup() {
        return autoTileGroup;
    }

    public Map<BuildType, Integer> getMinDistanceByType() {
        return minDistanceByType;
    }

    public ObjectBuilder getObjectBuilder() {
        return objectBuilder;
    }

    public static final class Builder {
        private final BuildType type;
        private final String itemId;
        private final String displayName;
        private String defaultSpriteKey = "wall_straight_base";
        private String iconSpriteKey = "wall_icon";
        private PlacementStrategy placementStrategy;
        private boolean rotatable;
        private boolean toolbarVisible = true;
        private boolean collisionEnabled = true;
        private boolean waterRestricted = true;
        private boolean requiresFlatTerrain;
        private int health = 100;
        private int buildCost = 1;
        private int footprintWidthTiles = 1;
        private int footprintHeightTiles = 1;
        private String autoTileGroup = "";
        private final Map<BuildType, Integer> minDistanceByType = new LinkedHashMap<>();
        private ObjectBuilder objectBuilder;

        private Builder(BuildType type, String itemId, String displayName) {
            this.type = type;
            this.itemId = itemId;
            this.displayName = displayName;
        }

        public Builder defaultSpriteKey(String defaultSpriteKey) {
            this.defaultSpriteKey = defaultSpriteKey;
            return this;
        }

        public Builder iconSpriteKey(String iconSpriteKey) {
            this.iconSpriteKey = iconSpriteKey;
            return this;
        }

        public Builder placementStrategy(PlacementStrategy placementStrategy) {
            this.placementStrategy = placementStrategy;
            return this;
        }

        public Builder rotatable(boolean rotatable) {
            this.rotatable = rotatable;
            return this;
        }

        public Builder toolbarVisible(boolean toolbarVisible) {
            this.toolbarVisible = toolbarVisible;
            return this;
        }

        public Builder collisionEnabled(boolean collisionEnabled) {
            this.collisionEnabled = collisionEnabled;
            return this;
        }

        public Builder waterRestricted(boolean waterRestricted) {
            this.waterRestricted = waterRestricted;
            return this;
        }

        public Builder requiresFlatTerrain(boolean requiresFlatTerrain) {
            this.requiresFlatTerrain = requiresFlatTerrain;
            return this;
        }

        public Builder health(int health) {
            this.health = Math.max(1, health);
            return this;
        }

        public Builder buildCost(int buildCost) {
            this.buildCost = Math.max(1, buildCost);
            return this;
        }

        public Builder footprint(int widthTiles, int heightTiles) {
            this.footprintWidthTiles = Math.max(1, widthTiles);
            this.footprintHeightTiles = Math.max(1, heightTiles);
            return this;
        }

        public Builder autoTileGroup(String autoTileGroup) {
            this.autoTileGroup = autoTileGroup == null ? "" : autoTileGroup;
            return this;
        }

        public Builder minDistance(BuildType type, int tiles) {
            if (type != null && tiles > 0) {
                minDistanceByType.put(type, tiles);
            }
            return this;
        }

        public Builder objectBuilder(ObjectBuilder objectBuilder) {
            this.objectBuilder = objectBuilder;
            return this;
        }

        public BuildDefinition build() {
            if (placementStrategy == null) {
                throw new IllegalStateException("placementStrategy is required for " + type);
            }
            if (objectBuilder == null) {
                throw new IllegalStateException("objectBuilder is required for " + type);
            }
            return new BuildDefinition(this);
        }
    }
}
