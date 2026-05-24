package drop;

import entity.Entity;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.List;

public class CollectibleDrop extends Entity {
    private static final DropAnimation COIN_ANIMATION = DropSpriteLoader.loadSpriteSheet(
            "assets/coin&xp/coin.png",
            16,
            16,
            5,
            core.GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS
    );
    private static final DropAnimation XP_ANIMATION = DropSpriteLoader.loadSpriteSheet(
            "assets/coin&xp/xp.png",
            16,
            16,
            4,
            core.GameBalance.DROPPED_ITEM_ANIMATION_FRAME_NS
    );

    private final DropType type;
    private final int value;
    private final ImageView imageView;
    private final DropAnimation animation;
    private final Rectangle2D hitbox;

    public CollectibleDrop(DropType type, double x, double y, int value, double size) {
        super(x, y, Math.max(4.0, size), Math.max(4.0, size), 0, 1);
        this.type = type == null ? DropType.COIN : type;
        this.value = Math.max(0, value);
        this.imageView = new ImageView();
        this.imageView.setFitWidth(Math.max(4.0, size));
        this.imageView.setFitHeight(Math.max(4.0, size));
        this.imageView.setPreserveRatio(true);
        this.imageView.setLayoutX(x);
        this.imageView.setLayoutY(y);

        this.animation = this.type == DropType.COIN ? COIN_ANIMATION : XP_ANIMATION;
        if (animation != null && !animation.isEmpty()) {
            this.imageView.setImage(animation.getFrames().get(0));
        }
        this.hitbox = new Rectangle2D(x, y, getWidth(), getHeight());
    }

    public DropType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public List<Image> getFrames() {
        return animation == null ? List.of() : animation.getFrames();
    }

    public int getCurrentFrame() {
        if (animation == null || animation.isEmpty()) {
            return 0;
        }
        Image current = imageView.getImage();
        int index = animation.getFrames().indexOf(current);
        return Math.max(0, index);
    }

    public Rectangle2D getHitbox() {
        return hitbox;
    }

    public static void preloadAssets() {
        int loadedFrameCount = COIN_ANIMATION.getFrameCount() + XP_ANIMATION.getFrameCount();
        if (loadedFrameCount <= 0) {
            System.out.println("Cannot load drop sprite: no coin/xp frames loaded");
        }
    }

    public void updateAnimation(long nowNs) {
        if (animation == null || animation.isEmpty()) {
            return;
        }
        Image nextFrame = animation.getFrame(nowNs, 0L);
        if (nextFrame == null || nextFrame == imageView.getImage()) {
            return;
        }
        imageView.setImage(nextFrame);
    }

    @Override
    public void triggerHitFlash(long nowNs, long durationNs, Color color) {
        // Drop khong nhan damage.
    }
}
