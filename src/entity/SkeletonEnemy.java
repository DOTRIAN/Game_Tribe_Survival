package entity;

/**
 * SkeletonEnemy:
 * - Quai loai 2 (xuat hien chu yeu ban dem).
 * - Stat hien tai:
 *   hp=5, damage=2, speed=0.95 (nhanh hon orc mot chut).
 */
public class SkeletonEnemy extends Enemy {
    public SkeletonEnemy(double x, double y) {
        super(
                x,
                y,
                64,
                64,
                0.95,
                5,
                2,
                650_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Skeleton Crew/Skeleton - Base/Run/Run-Sheet.png",
                6,
                1,
                95_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Skeleton Crew/Skeleton - Base/Idle/Idle-Sheet.png",
                4,
                1,
                180_000_000L
        );
    }

    @Override
    public String getEnemyType() {
        return "SKELETON";
    }
}

