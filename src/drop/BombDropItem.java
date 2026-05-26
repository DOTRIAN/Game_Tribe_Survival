package drop;

import core.GameBalance;
import javafx.scene.image.Image;

public class BombDropItem extends DroppedItem {
    private final DropAnimation animation;

    public BombDropItem(int amount, double centerX, double centerY) {
        super(DropItemType.BOMB_TRAP,
                amount,
                centerX - GameBalance.DROPPED_BOMB_TRAP_SIZE * 0.5,
                centerY - GameBalance.DROPPED_BOMB_TRAP_SIZE * 0.5,
                GameBalance.DROPPED_BOMB_TRAP_SIZE,
                GameBalance.DROPPED_BOMB_TRAP_SIZE);
        this.animation = DropItemType.BOMB_TRAP.getDropAnimation();
    }

    @Override
    public Image getCurrentImage(long nowNs) {
        if (animation == null || animation.isEmpty()) {
            return null;
        }
        return animation.getFrame(nowNs, 0L);
    }
}

