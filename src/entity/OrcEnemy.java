package entity;

/**
 * OrcEnemy:
 * - Quai loai 1 (xuat hien ban ngay + ban dem).
 * - Stat hien tai:
 *   hp=5, damage=1 (đã giảm từ 2), speed=0.3 (đã giảm từ 0.85 -> 0.45)
 */
public class OrcEnemy extends Enemy {
    public OrcEnemy(double x, double y) {
        // Sử dụng constructor của lớp cha Enemy để khởi tạo các thông số cơ bản.
        // Tốc độ được điều chỉnh xuống 0.3 để quái đi chậm hơn nữa theo yêu cầu.
        super(
                x,
                y,
                64,
                64,
                0.3,  // Tốc độ di chuyển (speed) - tiếp tục giảm
                5,    // Máu tối đa (maxHp)
                1,    // Sát thương (damage) - giảm để dễ thở hơn
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

