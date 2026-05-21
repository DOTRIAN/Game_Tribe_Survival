package buildsystem.component;

/**
 * LightComponent:
 * - Dung cho object phat sang nhu torch/campfire.
 */
public class LightComponent implements BuildComponent {
    private final double radius;
    private final double intensity;

    public LightComponent(double radius, double intensity) {
        this.radius = radius;
        this.intensity = intensity;
    }

    public double getRadius() {
        return radius;
    }

    public double getIntensity() {
        return intensity;
    }
}
