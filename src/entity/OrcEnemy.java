package entity;

/**
 * OrcEnemy:
 * - Quai loai 1 (xuat hien ban ngay + ban dem).
 * - Stat hien tai:
 *   hp=5, damage=2, speed=0.85
 */
public class OrcEnemy extends Enemy {
    public OrcEnemy(double x, double y) {
        super(
                x,
                y,
                64,
                64,
                0.85,
                5,
                2,
                650_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Orc Crew/Orc/Run/Run-Sheet.png",
                6,
                1,
                95_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Orc Crew/Orc/Idle/Idle-Sheet.png",
                4,
                1,
                180_000_000L
        );
    }

    @Override
    public String getEnemyType() {
        return "ORC";
    }
}

