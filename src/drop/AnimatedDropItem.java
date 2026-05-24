package drop;

import javafx.scene.image.Image;

public class AnimatedDropItem extends DroppedItem {
    private final DropAnimation animation;
    private final long animationStartedAtNs;

    public AnimatedDropItem(DropItemType type, int amount, double x, double y, double width, double height) {
        super(type, amount, x, y, width, height);
        this.animation = type == null ? null : type.getDropAnimation();
        this.animationStartedAtNs = resolveAnimationStartNs(type, x, y, this.animation);
    }

    @Override
    public Image getCurrentImage(long nowNs) {
        if (animation == null || animation.isEmpty()) {
            return null;
        }
        return animation.getFrame(nowNs, animationStartedAtNs);
    }

    private static long resolveAnimationStartNs(DropItemType type, double x, double y, DropAnimation animation) {
        long nowNs = System.nanoTime();
        if (type == null || animation == null || animation.isEmpty()) {
            return nowNs;
        }
        long cycleNs = animation.getFrameDurationNs() * Math.max(1, animation.getFrameCount());
        long seed = 17L;
        seed = seed * 31L + Double.doubleToLongBits(x);
        seed = seed * 31L + Double.doubleToLongBits(y);
        seed = seed * 31L + type.getId().hashCode();
        long offsetNs = Math.floorMod(seed, Math.max(1L, cycleNs));
        return nowNs - offsetNs;
    }
}
