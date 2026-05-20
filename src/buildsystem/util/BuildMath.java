package buildsystem.util;

/** Utility nho cho buildsystem. */
public final class BuildMath {
    private BuildMath() {}

    public static boolean intersects(double ax, double ay, double aw, double ah,
                                     double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
