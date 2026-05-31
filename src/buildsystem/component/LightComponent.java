package buildsystem.component;

/**
 * LightComponent:
 * - Dung cho object phat sang nhu torch/campfire.
 */
public class LightComponent implements BuildComponent {
    private final double coreRadius;
    private final double innerRadius;
    private final double outerRadius;
    private final double fadeRadius;
    private final double intensity;
    private final boolean flickerEnabled;
    private final double flickerRadiusPercent;
    private final double flickerAlphaPercent;
    private final double flickerSpeed;

    public LightComponent(double coreRadius,
                          double innerRadius,
                          double outerRadius,
                          double fadeRadius,
                          double intensity,
                          boolean flickerEnabled,
                          double flickerRadiusPercent,
                          double flickerAlphaPercent,
                          double flickerSpeed) {
        this.coreRadius = coreRadius;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.fadeRadius = fadeRadius;
        this.intensity = intensity;
        this.flickerEnabled = flickerEnabled;
        this.flickerRadiusPercent = flickerRadiusPercent;
        this.flickerAlphaPercent = flickerAlphaPercent;
        this.flickerSpeed = flickerSpeed;
    }

    public double getCoreRadius() {
        return coreRadius;
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public double getFadeRadius() {
        return fadeRadius;
    }

    public double getIntensity() {
        return intensity;
    }

    public boolean isFlickerEnabled() {
        return flickerEnabled;
    }

    public double getFlickerRadiusPercent() {
        return flickerRadiusPercent;
    }

    public double getFlickerAlphaPercent() {
        return flickerAlphaPercent;
    }

    public double getFlickerSpeed() {
        return flickerSpeed;
    }
}
