package entity;

import core.GameBalance;

public class BombDropItem extends DroppedItem {
    public BombDropItem(int amount, double centerX, double centerY) {
        super("bomb_trap",
                "bomb_trap_icon",
                amount,
                centerX - GameBalance.DROPPED_BOMB_TRAP_SIZE * 0.5,
                centerY - GameBalance.DROPPED_BOMB_TRAP_SIZE * 0.5,
                GameBalance.DROPPED_BOMB_TRAP_SIZE,
                GameBalance.DROPPED_BOMB_TRAP_SIZE);
    }
}

