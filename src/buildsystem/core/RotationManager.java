package buildsystem.core;

/**
 * RotationManager:
 * - Quan ly huong xoay chung cho build flow.
 * - Bam Q se goi rotateClockwise().
 * - Moi object co RotationComponent deu tai su dung gia tri nay luc preview/place.
 */
public class RotationManager {
    private static final double[] ROTATIONS = {0.0, 90.0, 180.0, 270.0};
    private int index;

    public RotationManager() {
        this.index = 0;
    }

    public void rotateClockwise() {
        index = (index + 1) % ROTATIONS.length;
    }

    public void reset() {
        index = 0;
    }

    public void setRotationDegrees(double rotationDegrees) {
        double normalized = rotationDegrees % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        for (int i = 0; i < ROTATIONS.length; i++) {
            if (ROTATIONS[i] == normalized) {
                index = i;
                return;
            }
        }
        index = 0;
    }

    public double getCurrentRotationDegrees() {
        return ROTATIONS[index];
    }
}
