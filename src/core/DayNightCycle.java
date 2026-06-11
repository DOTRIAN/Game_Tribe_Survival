package core;

/**
 * DayNightCycle:
 * - Quan ly chu ky ngay/đem theo thoi gian thuc.
 * - Muc tieu hien tai:
 *   1) Ban ngay sang ro (darkness = 0)
 *   2) Sau 2 phut (120s) bat dau toi dan (dusk)
 *   3) Den dem thi toi manh hon
 *   4) Co binh minh de quay ve ngay, lap chu ky
 *
 * Ghi chu:
 * - Class nay CHI quan ly "tham so anh sang", chua spawn quai.
 * - Sau nay he thong quai co the goi isNight(nowNs) de quyet dinh spawn.
 */
public class DayNightCycle {
    // 2 phut ban ngay truoc khi bat dau toi dan.
    private static final long DAY_DURATION_NS = 120L * 1_000_000_000L;
    // Thoi gian chuyen tiep sang dem (mau troi toi dan).
    private static final long DUSK_DURATION_NS = 20L * 1_000_000_000L;
    // Khoang dem on dinh.
    private static final long NIGHT_DURATION_NS = 80L * 1_000_000_000L;
    // Binh minh (sang dan tro lai).
    private static final long DAWN_DURATION_NS = 25L * 1_000_000_000L;

    // Do toi lon nhat cua lop phu den.
    private static final double MAX_DARKNESS_ALPHA = 0.82;
    // Ngay khi vao dusk se toi san mot muc nho de nguoi choi "nhin thay doi" ro rang hon.
    private static final double DUSK_START_ALPHA = 0.18;

    private long cycleStartedAtNs;

    public DayNightCycle() {
        this.cycleStartedAtNs = -1L;
    }

    /**
     * reset:
     * - Khoi dong lai chu ky ve "bat dau ban ngay".
     * - Thuong goi khi restart game.
     */
    public void reset(long nowNs) {
        cycleStartedAtNs = nowNs;
    }

    /**
     * getDarknessAlpha:
     * - Tra ve alpha [0..MAX_DARKNESS_ALPHA] de Renderer phu lop toi.
     */
    public double getDarknessAlpha(long nowNs) {
        ensureStarted(nowNs);

        long t = getTimeInCycle(nowNs);
        long dayEnd = DAY_DURATION_NS;
        long duskEnd = dayEnd + DUSK_DURATION_NS;
        long nightEnd = duskEnd + NIGHT_DURATION_NS;
        long dawnEnd = nightEnd + DAWN_DURATION_NS;

        // Ban ngay: khong phu toi.
        if (t < dayEnd) {
            return 0.0;
        }

        // Dusk: noi suy tu sang -> toi manh.
        if (t < duskEnd) {
            double progress = (double) (t - dayEnd) / DUSK_DURATION_NS;
            return DUSK_START_ALPHA + progress * (MAX_DARKNESS_ALPHA - DUSK_START_ALPHA);
        }

        // Dem on dinh: toi manh.
        if (t < nightEnd) {
            return MAX_DARKNESS_ALPHA;
        }

        // Dawn: noi suy tu toi -> sang.
        if (t < dawnEnd) {
            double progress = (double) (t - nightEnd) / DAWN_DURATION_NS;
            return (1.0 - progress) * MAX_DARKNESS_ALPHA;
        }

        return 0.0;
    }

    /**
     * isNight:
     * - True khi do toi da vuot nguong co y nghia gameplay dem.
     * - Nguong nay dung cho logic spawn quai sau nay.
     */
    public boolean isNight(long nowNs) {
        return getDarknessAlpha(nowNs) >= 0.55;
    }

    // Chuoi debug de render/HUD hien thi nhanh trong qua trinh test.
    public String getPhaseName(long nowNs) {
        ensureStarted(nowNs);
        long t = getTimeInCycle(nowNs);
        long dayEnd = DAY_DURATION_NS;
        long duskEnd = dayEnd + DUSK_DURATION_NS;
        long nightEnd = duskEnd + NIGHT_DURATION_NS;
        long dawnEnd = nightEnd + DAWN_DURATION_NS;

        if (t < dayEnd) {
            return "DAY";
        }
        if (t < duskEnd) {
            return "DUSK";
        }
        if (t < nightEnd) {
            return "NIGHT";
        }
        if (t < dawnEnd) {
            return "DAWN";
        }
        return "DAY";
    }

    private long getTimeInCycle(long nowNs) {
        long cycleDuration = DAY_DURATION_NS + DUSK_DURATION_NS + NIGHT_DURATION_NS + DAWN_DURATION_NS;
        long elapsed = Math.max(0L, nowNs - cycleStartedAtNs);
        return elapsed % cycleDuration;
    }

    private void ensureStarted(long nowNs) {
        if (cycleStartedAtNs < 0) {
            cycleStartedAtNs = nowNs;
        }
    }
}
