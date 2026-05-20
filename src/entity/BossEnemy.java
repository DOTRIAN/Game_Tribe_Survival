package entity;

/**
 * BossEnemy:
 * - Boss cuoi cua world sinh ton vo han.
 * - Dieu kien thang: giet duoc boss.
 */
public class BossEnemy extends Enemy {
    // Constructor:
    // - Input: vi tri spawn boss.
    // - Tac dong: tao boss voi hp/sat thuong cao hon quai thuong.
    public BossEnemy(double x, double y) {
        super(
                x,
                y,
                96,
                96,
                0.42,
                120,
                9,
                520_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Orc Crew/Orc/Run/Run-Sheet.png",
                6,
                1,
                85_000_000L,
                "file:assets/tilesets/Pixel Crawler - Free Pack/Entities/Mobs/Orc Crew/Orc/Idle/Idle-Sheet.png",
                4,
                1,
                145_000_000L
        );
    }

    @Override
    public String getEnemyType() {
        return "BOSS";
    }
}
