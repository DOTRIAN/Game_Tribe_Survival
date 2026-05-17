package entity;

/**
 * SkeletonEnemy:
 * - Quai loai 2 (xuat hien chu yeu ban dem).
 * - Stat hien tai:
 *   hp=5, damage=1 (đã giảm từ 2), speed=0.35 (đã giảm từ 0.95 -> 0.55)
 */
public class SkeletonEnemy extends Enemy {
    public SkeletonEnemy(double x, double y) {
        // Khởi tạo Skeleton thông qua lớp trừu tượng Enemy (tính kế thừa trong OOP).
        // Tốc độ 0.35 đã được giảm để người chơi có thể xoay sở tốt hơn ban đêm.
        super(
                x,
                y,
                64,
                64,
                0.35, // Tốc độ di chuyển (speed) - đã giảm
                5,    // Máu (hp)
                1,    // Sát thương (damage) - giảm sát thương
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

