package drop;

import core.GameBalance;
import javafx.scene.image.Image;
import java.util.List;

public enum DropItemType {
    WOOD("wood", "wood", "assets/Chip/wood", "assets/Chip/wood", null, 0, 0, 4, GameBalance.DROPPED_WOOD_WIDTH, GameBalance.DROPPED_WOOD_HEIGHT, false),
    ROCK("rock", "rock", "assets/Chip/rock", "assets/Chip/rock", null, 0, 0, 4, GameBalance.DROPPED_STONE_WIDTH, GameBalance.DROPPED_STONE_HEIGHT, false),
    GOLD("gold", "coin", null, null, "assets/coin&xp/coin.png", 16, 16, 5, GameBalance.DROPPED_GOLD_WIDTH, GameBalance.DROPPED_GOLD_HEIGHT, false),
    NIKU("niku", "niku", "assets/Chip/niku", "assets/Chip/niku", null, 0, 0, 3, GameBalance.DROPPED_NIKU_WIDTH, GameBalance.DROPPED_NIKU_HEIGHT, false),
    XP("xp", null, null, null, "assets/coin&xp/xp.png", 16, 16, 4, GameBalance.DROPPED_XP_WIDTH, GameBalance.DROPPED_XP_HEIGHT, true),
    BOMB_TRAP("bomb_trap", "bomb_trap", null, null, "assets/bom/png/bom.png", 0, 0, 1, GameBalance.DROPPED_BOMB_TRAP_SIZE, GameBalance.DROPPED_BOMB_TRAP_SIZE, false),
    UNKNOWN("unknown", null, null, null, null, 0, 0, 0, GameBalance.DROPPED_ITEM_SIZE, GameBalance.DROPPED_ITEM_SIZE, false);

    private final String id;
    private final String inventoryItemId;
    private final String inventoryIconDirectory;
    private final String dropFrameDirectory;
    private final String dropSpritePath;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final double renderWidth;
    private final double renderHeight;
    private final boolean experienceDrop;
    private DropAnimation dropAnimation;
    private Image inventoryIcon;

    DropItemType(String id,
                 String inventoryItemId,
                 String inventoryIconDirectory,
                 String dropFrameDirectory,
                 String dropSpritePath,
                 int frameWidth,
                 int frameHeight,
                 int frameCount,
                 double renderWidth,
                 double renderHeight,
                 boolean experienceDrop) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.inventoryIconDirectory = inventoryIconDirectory;
        this.dropFrameDirectory = dropFrameDirectory;
        this.dropSpritePath = dropSpritePath;
        this.frameWidth = Math.max(0, frameWidth);
        this.frameHeight = Math.max(0, frameHeight);
        this.frameCount = Math.max(0, frameCount);
        this.renderWidth = Math.max(4.0, renderWidth);
        this.renderHeight = Math.max(4.0, renderHeight);
        this.experienceDrop = experienceDrop;
    }

    public String getId() {
        return id;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public double getRenderWidth() {
        return renderWidth;
    }

    public double getRenderHeight() {
        return renderHeight;
    }

    public boolean isExperienceDrop() {
        return experienceDrop;
    }

    public synchronized DropAnimation getDropAnimation() {
        if (dropAnimation != null) {
            return dropAnimation;
        }
        if (dropFrameDirectory != null && !dropFrameDirectory.isBlank()) {
            dropAnimation = DropSpriteLoader.loadFrameSequence(dropFrameDirectory, frameCount, GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS);
            return dropAnimation;
        }
        if (dropSpritePath != null && !dropSpritePath.isBlank()) {
            if (frameWidth > 0 && frameHeight > 0 && frameCount > 0) {
                dropAnimation = DropSpriteLoader.loadSpriteSheet(dropSpritePath, frameWidth, frameHeight, frameCount, GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS);
            } else {
                Image staticFrame = DropSpriteLoader.loadPreparedImage(dropSpritePath);
                dropAnimation = new DropAnimation(staticFrame == null ? List.of() : List.of(staticFrame), GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS);
            }
            return dropAnimation;
        }
        dropAnimation = new DropAnimation(List.of(), GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS);
        return dropAnimation;
    }

    public synchronized Image getInventoryIcon() {
        if (inventoryIcon != null) {
            return inventoryIcon;
        }
        if (inventoryIconDirectory == null || inventoryIconDirectory.isBlank()) {
            return null;
        }
        inventoryIcon = InventoryIconLoader.loadMainIcon(inventoryIconDirectory);
        return inventoryIcon;
    }

    public static void preloadAll() {
        for (DropItemType type : values()) {
            type.getDropAnimation();
        }
    }

    public static DropItemType fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "wood" -> WOOD;
            case "rock", "stone" -> ROCK;
            case "gold", "coin" -> GOLD;
            case "niku", "meat" -> NIKU;
            case "xp", "experience" -> XP;
            case "bomb_trap" -> BOMB_TRAP;
            default -> UNKNOWN;
        };
    }
}
