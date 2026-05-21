package core;

/**
 * GameBalance:
 * - Gom cac thong so item/build/light de tranh hardcode rai rac.
 * - Chi chua gia tri gameplay co kha nang canh chinh nhanh.
 */
public final class GameBalance {
    public static final int STARTING_COIN_AMOUNT = 120;

    public static final int STONE_WALL_PRICE = 6;
    public static final int WOOD_WALL_PRICE = 4;
    public static final int TORCH_PRICE = 20;
    public static final int TORCH_HITS_TO_BREAK = 2;
    public static final int ROCK_HITS_TO_BREAK = 4;

    public static final double TORCH_LIGHT_RADIUS = 150.0;
    public static final double TORCH_LIGHT_INTENSITY = 0.90;
    public static final long TORCH_ANIMATION_FRAME_NS = 90_000_000L;
    public static final double TORCH_WORLD_WIDTH = 24.0;
    public static final double TORCH_WORLD_HEIGHT = 36.0;

    public static final double DROPPED_ITEM_SIZE = 14.0;
    public static final double DROPPED_TORCH_WIDTH = TORCH_WORLD_WIDTH / 3.0;
    public static final double DROPPED_TORCH_HEIGHT = TORCH_WORLD_HEIGHT / 3.0;
    public static final double DROPPED_STONE_WIDTH = 12.0;
    public static final double DROPPED_STONE_HEIGHT = 12.0;

    private GameBalance() {
    }
}
