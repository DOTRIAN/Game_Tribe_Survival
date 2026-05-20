package buildsystem.component;

/**
 * RotationComponent:
 * - Luu huong xoay cua object.
 * - RotationManager se cap nhat gia tri nay cho moi object co kha nang xoay.
 */
public class RotationComponent implements BuildComponent {
    private double rotationDegrees;

    public RotationComponent(double rotationDegrees) {
        this.rotationDegrees = normalize(rotationDegrees);
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = normalize(rotationDegrees);
    }

    private double normalize(double value) {
        double out = value % 360.0;
        if (out < 0) {
            out += 360.0;
        }
        return out;
    }
}
