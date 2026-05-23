package core;

/**
 * GameBalance:
 * - Gom cac thong so item/build/light de tranh hardcode rai rac.
 * - Chi chua gia tri gameplay co kha nang canh chinh nhanh.
 */
public final class GameBalance {
    public static final int STARTING_COIN_AMOUNT = 120;

    public static final int STONE_WALL_PRICE = 6;
    public static final int WOOD_FENCE_PRICE = STONE_WALL_PRICE;
    public static final int WOOD_WALL_PRICE = 4;
    public static final int TORCH_PRICE = 20;
    public static final int ARCHER_TOWER_PRICE = 35;
    public static final int BOMB_TRAP_PRICE = 28;
    public static final int FRIENDLY_ARCHER_PRICE = 1;
    public static final int FIRE_BOMB_PRICE = 50;
    public static final int TORCH_HITS_TO_BREAK = 2;
    public static final int WOOD_FENCE_MAX_HP = 10;
    public static final int ARCHER_TOWER_HITS_TO_BREAK = 8;
    public static final int BOMB_TRAP_HITS_TO_BREAK = 2;
    public static final int ARCHER_TOWER_DAMAGE = 2;
    public static final int BOMB_TRAP_DAMAGE = 50;
    public static final int FIRE_BOMB_IMPACT_DAMAGE = 12;
    public static final int FIRE_BOMB_BURN_DAMAGE = 5;
    public static final double FIRE_BOMB_RADIUS = 32.0;
    public static final long FIRE_BOMB_BURN_DURATION_NS = 3_600_000_000L;
    public static final long FIRE_BOMB_FADE_DURATION_NS = 2_000_000_000L;
    public static final long FIRE_BOMB_DAMAGE_TICK_NS = 450_000_000L;
    public static final int ROCK_HITS_TO_BREAK = 4;
    public static final double ARCHER_TOWER_RANGE = 220.0;
    public static final long ARCHER_TOWER_ATTACK_COOLDOWN_NS = 800_000_000L;
    public static final long ARCHER_TOWER_ANIMATION_FRAME_NS = 100_000_000L;
    public static final double ARCHER_ARROW_SPEED = 6.2;
    public static final long ARCHER_ARROW_LIFETIME_NS = 1_800_000_000L;
    public static final int FRIENDLY_ARCHER_MAX_HP = 30;
    public static final int FRIENDLY_ARCHER_RANGED_DAMAGE = 4;
    public static final int FRIENDLY_ARCHER_MELEE_DAMAGE = 2;
    public static final double FRIENDLY_ARCHER_ARROW_SPEED = 6.0;
    public static final double FRIENDLY_ARCHER_VISION_RANGE = 12 * 16.0;
    public static final double FRIENDLY_ARCHER_SHOOT_RANGE = 8 * 16.0;
    public static final double FRIENDLY_ARCHER_MELEE_RANGE = 1.1 * 16.0;
    public static final int FRIENDLY_ARCHER_WANDER_RADIUS_TILES = 4;
    public static final long FRIENDLY_ARCHER_SHOT_COOLDOWN_NS = 800_000_000L;
    public static final long FRIENDLY_ARCHER_MELEE_COOLDOWN_NS = 550_000_000L;
    public static final long FRIENDLY_ARCHER_ARROW_LIFETIME_NS = 1_800_000_000L;
    public static final double BOMB_TRAP_TRIGGER_RANGE_TILES = 2.0;
    public static final double BOMB_TRAP_EXPLOSION_RADIUS_TILES = 2.0;
    public static final long BOMB_TRAP_FUSE_NS = 1_600_000_000L;
    public static final double BOMB_TRAP_WORLD_WIDTH = 18.0;
    public static final double BOMB_TRAP_WORLD_HEIGHT = 24.0;

    public static final double TORCH_LIGHT_RADIUS = 150.0;
    public static final double TORCH_LIGHT_INTENSITY = 0.90;
    public static final long TORCH_ANIMATION_FRAME_NS = 90_000_000L;
    public static final double TORCH_WORLD_WIDTH = 24.0;
    public static final double TORCH_WORLD_HEIGHT = 36.0;
    public static final double ARCHER_TOWER_WORLD_WIDTH = 96.0;
    public static final double ARCHER_TOWER_WORLD_HEIGHT = 144.0;
    public static final double FRIENDLY_ARCHER_RENDER_WIDTH = 46.0 * 58.0 / 52.0;
    public static final double FRIENDLY_ARCHER_RENDER_HEIGHT = 58.0;

    public static final double DROPPED_ITEM_SIZE = 14.0;
    public static final double DROPPED_TORCH_WIDTH = TORCH_WORLD_WIDTH / 3.0;
    public static final double DROPPED_TORCH_HEIGHT = TORCH_WORLD_HEIGHT / 3.0;
    public static final double DROPPED_ARCHER_TOWER_WIDTH = ARCHER_TOWER_WORLD_WIDTH / 2.6;
    public static final double DROPPED_ARCHER_TOWER_HEIGHT = ARCHER_TOWER_WORLD_HEIGHT / 2.6;
    public static final double DROPPED_BOMB_TRAP_SIZE = 11.0;
    public static final double DROPPED_STONE_WIDTH = 12.0;
    public static final double DROPPED_STONE_HEIGHT = 12.0;

    private GameBalance() {
    }
}
