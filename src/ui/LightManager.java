package ui;

import buildsystem.component.LightComponent;
import buildsystem.core.BuildManager;
import buildsystem.object.BuildObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;

/**
 * LightManager:
 * - Mot noi duy nhat dieu phoi overlay dem + moon ambient + light mask.
 * - Khong tao canvas/timer rieng cho tung duoc.
 * - Dung density clamp de nhieu nguon sang gan nhau khong bi chay trang.
 */
public class LightManager {
    private static final Color NIGHT_OVERLAY = Color.web("#05070D");
    private static final Color DEEP_SHADOW = Color.web("#0A0F18");
    private static final Color NIGHT_SHADE = Color.web("#101722");

    private static final Color TORCH_CORE = Color.web("#FFD76A");
    private static final Color TORCH_INNER = Color.web("#FFB347");
    private static final Color TORCH_OUTER = Color.web("#D97A1A");
    private static final Color TORCH_EDGE = Color.web("#6B2E12");

    private static final double MAX_BRIGHTNESS = 1.0;
    private static final double MAX_LIGHT_ALPHA = 0.8;
    private static final double LIGHT_QUERY_MARGIN = 260.0;

    private record RuntimeLight(
            double screenX,
            double screenY,
            double coreRadius,
            double innerRadius,
            double outerRadius,
            double fadeRadius,
            double baseAlpha
    ) {
    }

    public void render(GraphicsContext graphicsContext,
                       BuildManager buildManager,
                       double cameraX,
                       double cameraY,
                       double cameraZoom,
                       double viewportWidth,
                       double viewportHeight,
                       double darknessAlpha,
                       long nowNs) {
        if (graphicsContext == null || darknessAlpha <= 0.001) {
            return;
        }

        renderNightBase(graphicsContext, viewportWidth, viewportHeight, darknessAlpha);

        List<RuntimeLight> lights = collectVisibleLights(buildManager, cameraX, cameraY, cameraZoom, viewportWidth, viewportHeight, nowNs);
        if (!lights.isEmpty()) {
            graphicsContext.save();
            graphicsContext.setGlobalBlendMode(BlendMode.SCREEN);
            for (int i = 0; i < lights.size(); i++) {
                RuntimeLight light = lights.get(i);
                double clampedAlpha = computeClampedAlpha(light, lights, i);
                renderTorchGradient(graphicsContext, light, clampedAlpha);
            }
            graphicsContext.restore();
        }
    }

    private void renderNightBase(GraphicsContext graphicsContext,
                                 double viewportWidth,
                                 double viewportHeight,
                                 double darknessAlpha) {
        graphicsContext.save();
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphicsContext.setFill(withAlpha(NIGHT_OVERLAY, darknessAlpha));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
        graphicsContext.setFill(withAlpha(DEEP_SHADOW, Math.min(0.32, darknessAlpha * 0.34)));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
        graphicsContext.setFill(withAlpha(NIGHT_SHADE, Math.min(0.22, darknessAlpha * 0.24)));
        graphicsContext.fillRect(0, 0, viewportWidth, viewportHeight);
        graphicsContext.restore();
    }

    private List<RuntimeLight> collectVisibleLights(BuildManager buildManager,
                                                    double cameraX,
                                                    double cameraY,
                                                    double cameraZoom,
                                                    double viewportWidth,
                                                    double viewportHeight,
                                                    long nowNs) {
        List<RuntimeLight> lights = new ArrayList<>();
        if (buildManager == null) {
            return lights;
        }

        for (BuildObject object : buildManager.getPlacedObjectsInWorldRect(
                cameraX - LIGHT_QUERY_MARGIN,
                cameraY - LIGHT_QUERY_MARGIN,
                viewportWidth / cameraZoom + LIGHT_QUERY_MARGIN * 2.0,
                viewportHeight / cameraZoom + LIGHT_QUERY_MARGIN * 2.0
        )) {
            if (object == null) {
                continue;
            }
            LightComponent lightComponent = object.getComponent(LightComponent.class);
            if (lightComponent == null) {
                continue;
            }

            double seed = stableSeed(object.getId());
            double wave = oscillate(nowNs, lightComponent.getFlickerSpeed(), seed);
            double radiusScale = lightComponent.isFlickerEnabled()
                    ? 1.0 + wave * lightComponent.getFlickerRadiusPercent()
                    : 1.0;
            double alphaScale = lightComponent.isFlickerEnabled()
                    ? 1.0 + wave * lightComponent.getFlickerAlphaPercent()
                    : 1.0;
            double intensity = Math.max(0.0, Math.min(MAX_BRIGHTNESS, lightComponent.getIntensity() * alphaScale));

            lights.add(new RuntimeLight(
                    (object.getCenterX() - cameraX) * cameraZoom,
                    (object.getCenterY() - cameraY) * cameraZoom,
                    lightComponent.getCoreRadius() * cameraZoom * radiusScale,
                    lightComponent.getInnerRadius() * cameraZoom * radiusScale,
                    lightComponent.getOuterRadius() * cameraZoom * radiusScale,
                    lightComponent.getFadeRadius() * cameraZoom * radiusScale,
                    intensity
            ));
        }
        return lights;
    }

    private double computeClampedAlpha(RuntimeLight light, List<RuntimeLight> lights, int lightIndex) {
        double density = 1.0;
        for (int index = 0; index < lights.size(); index++) {
            if (index == lightIndex) {
                continue;
            }
            RuntimeLight other = lights.get(index);
            double dx = other.screenX - light.screenX;
            double dy = other.screenY - light.screenY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double influenceRadius = Math.max(light.fadeRadius, other.fadeRadius) * 0.92;
            if (distance >= influenceRadius) {
                continue;
            }
            density += 1.0 - (distance / influenceRadius);
        }

        double attenuation = 1.0 / Math.sqrt(Math.max(1.0, density * 1.15));
        double alpha = light.baseAlpha * attenuation;
        return Math.max(0.0, Math.min(MAX_LIGHT_ALPHA, alpha));
    }

    private void renderTorchGradient(GraphicsContext graphicsContext, RuntimeLight light, double alpha) {
        if (alpha <= 0.001 || light.fadeRadius <= 1.0) {
            return;
        }

        graphicsContext.setFill(new RadialGradient(
                0,
                0,
                light.screenX,
                light.screenY,
                light.fadeRadius,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.00, withAlpha(TORCH_CORE, alpha * 0.64)),
                new Stop(0.25, withAlpha(TORCH_INNER, alpha * 0.54)),
                new Stop(0.60, withAlpha(TORCH_OUTER, alpha * 0.34)),
                new Stop(0.85, withAlpha(TORCH_EDGE, alpha * 0.14)),
                new Stop(1.00, Color.TRANSPARENT)
        ));
        graphicsContext.fillOval(
                light.screenX - light.fadeRadius,
                light.screenY - light.fadeRadius,
                light.fadeRadius * 2.0,
                light.fadeRadius * 2.0
        );

        graphicsContext.setFill(new RadialGradient(
                0,
                0,
                light.screenX,
                light.screenY,
                Math.max(light.coreRadius, 1.0),
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, withAlpha(TORCH_CORE, alpha * 0.10)),
                new Stop(1.0, Color.TRANSPARENT)
        ));
        graphicsContext.fillOval(
                light.screenX - light.coreRadius,
                light.screenY - light.coreRadius,
                light.coreRadius * 2.0,
                light.coreRadius * 2.0
        );
    }

    private double oscillate(long nowNs, double speed, double seed) {
        double time = nowNs / 1_000_000_000.0;
        double a = Math.sin(time * speed + seed);
        double b = Math.sin(time * speed * 0.61 + seed * 1.73);
        return a * 0.65 + b * 0.35;
    }

    private double stableSeed(String id) {
        if (id == null || id.isBlank()) {
            return 0.0;
        }
        return (id.hashCode() & 0xFFFF) / 137.0;
    }

    private Color withAlpha(Color color, double alpha) {
        if (color == null) {
            return Color.TRANSPARENT;
        }
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0.0, Math.min(1.0, alpha)));
    }
}
