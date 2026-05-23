package buildsystem.core;

import buildsystem.fence.FenceEntity;
import buildsystem.object.Campfire;
import buildsystem.object.Chest;
import buildsystem.object.ArcherTower;
import buildsystem.object.BombTrap;
import buildsystem.object.Torch;
import buildsystem.object.Trap;
import buildsystem.object.Turret;
import buildsystem.object.Wall;
import core.GameBalance;
import buildsystem.placement.FreePlacementStrategy;
import buildsystem.placement.GridPlacementStrategy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BuildRegistry:
 * - Dang ky toan bo build definition dung trong game chinh va demo.
 * - Them object moi theo dung flow:
 *   1) Tao BuildObject subclass neu can.
 *   2) Dang ky 1 BuildDefinition tai day.
 *   3) UI/preview/place/save-load tu dong tai su dung definition do.
 */
public class BuildRegistry {
    private final Map<BuildType, BuildDefinition> byType;
    private final Map<String, BuildDefinition> byItemId;

    public BuildRegistry() {
        this.byType = new LinkedHashMap<>();
        this.byItemId = new LinkedHashMap<>();
        registerDefaults();
    }

    public void register(BuildDefinition definition) {
        if (definition == null) {
            return;
        }
        byType.put(definition.getType(), definition);
        byItemId.put(definition.getItemId(), definition);
    }

    public BuildDefinition findByItemId(String itemId) {
        return byItemId.get(itemId);
    }

    public BuildDefinition findByType(BuildType type) {
        return byType.get(type);
    }

    public Collection<BuildDefinition> all() {
        return byType.values();
    }

    private void registerDefaults() {
        register(BuildDefinition.builder(BuildType.STONE_WALL, "wood_fence", "Wood Fence")
                .defaultSpriteKey(buildsystem.fence.FenceRenderer.SINGLE_SPRITE_KEY)
                .iconSpriteKey(buildsystem.fence.FenceRenderer.ICON_SPRITE_KEY)
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(false)
                .collisionEnabled(true)
                .health(GameBalance.WOOD_FENCE_MAX_HP)
                .buildCost(1)
                .footprint(1, 1)
                .autoTileGroup("wood_fence")
                .objectBuilder(seed -> new FenceEntity(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.WOOD_WALL, "wood_wall", "Wood Wall")
                .defaultSpriteKey("wall_straight_base")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(true)
                .collisionEnabled(true)
                .health(90)
                .buildCost(1)
                .autoTileGroup("wall")
                .objectBuilder(seed -> new Wall(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.SPIKE_TRAP, "spike_trap", "Spike Trap")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(true)
                .collisionEnabled(true)
                .waterRestricted(true)
                .minDistance(BuildType.SPIKE_TRAP, 1)
                .health(80)
                .buildCost(1)
                .objectBuilder(seed -> new Trap(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.BEAR_TRAP, "bear_trap", "Bear Trap")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(true)
                .collisionEnabled(true)
                .waterRestricted(true)
                .minDistance(BuildType.BEAR_TRAP, 1)
                .health(100)
                .buildCost(1)
                .objectBuilder(seed -> new Trap(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.TORCH, "torch", "Torch")
                .defaultSpriteKey("torch_frame_0")
                .iconSpriteKey("torch_icon")
                .placementStrategy(new FreePlacementStrategy())
                .rotatable(false)
                .collisionEnabled(false)
                .waterRestricted(true)
                .health(GameBalance.TORCH_HITS_TO_BREAK)
                .buildCost(1)
                .objectBuilder(seed -> new Torch(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.ARCHER_TOWER, "archer_tower", "Archer Tower")
                .defaultSpriteKey("archer_tower_idle_0")
                .iconSpriteKey("archer_tower_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(false)
                .collisionEnabled(true)
                .waterRestricted(true)
                .health(GameBalance.ARCHER_TOWER_HITS_TO_BREAK)
                .buildCost(1)
                .objectBuilder(seed -> new ArcherTower(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.BOMB_TRAP, "bomb_trap", "Bomb Trap")
                .defaultSpriteKey("bomb_trap_0")
                .iconSpriteKey("bomb_trap_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(false)
                .collisionEnabled(false)
                .waterRestricted(true)
                .health(GameBalance.BOMB_TRAP_HITS_TO_BREAK)
                .buildCost(1)
                .footprint(1, 1)
                .objectBuilder(seed -> new BombTrap(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.CHEST, "chest", "Chest")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(false)
                .collisionEnabled(true)
                .waterRestricted(true)
                .health(150)
                .buildCost(1)
                .objectBuilder(seed -> new Chest(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.WORKBENCH, "workbench", "Workbench")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(false)
                .collisionEnabled(true)
                .requiresFlatTerrain(true)
                .health(180)
                .buildCost(1)
                .objectBuilder(seed -> new Chest(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.CAMPFIRE, "campfire", "Campfire")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new FreePlacementStrategy())
                .rotatable(false)
                .collisionEnabled(false)
                .waterRestricted(true)
                .health(100)
                .buildCost(1)
                .objectBuilder(seed -> new Campfire(seed.getDefinition(), seed))
                .build());

        register(BuildDefinition.builder(BuildType.TURRET, "turret", "Turret")
                .defaultSpriteKey("wall_single")
                .iconSpriteKey("wall_icon")
                .placementStrategy(new GridPlacementStrategy())
                .rotatable(true)
                .collisionEnabled(true)
                .requiresFlatTerrain(true)
                .health(220)
                .buildCost(1)
                .objectBuilder(seed -> new Turret(seed.getDefinition(), seed))
                .build());
    }
}
