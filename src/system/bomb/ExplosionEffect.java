package system.bomb;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExplosionEffect {
    private static final long DURATION_NS = 520_000_000L;
    private final double worldX;
    private final double worldY;
    private final double radius;
    private final long startedAtNs;
    private final List<Particle> particles;

    public ExplosionEffect(double worldX, double worldY, double radius, long startedAtNs) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.radius = radius;
        this.startedAtNs = startedAtNs;
        this.particles = new ArrayList<>();
        Random random = new Random(Double.doubleToLongBits(worldX * 31.0 + worldY * 17.0 + startedAtNs));
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 24.0 + random.nextDouble() * 40.0;
            double life = 0.35 + random.nextDouble() * 0.55;
            particles.add(new Particle(Math.cos(angle) * speed, Math.sin(angle) * speed, life));
        }
    }

    public boolean isAlive(long nowNs) {
        return nowNs - startedAtNs <= DURATION_NS;
    }

    public void render(GraphicsContext graphics, double cameraX, double cameraY, double zoom, long nowNs) {
        long elapsed = Math.max(0L, nowNs - startedAtNs);
        double progress = Math.min(1.0, elapsed / (double) DURATION_NS);
        double centerX = (worldX - cameraX) * zoom;
        double centerY = (worldY - cameraY) * zoom;
        double burstRadius = radius * zoom * (0.35 + progress * 0.85);

        graphics.save();
        graphics.setGlobalAlpha(Math.max(0.0, 0.75 - progress * 0.75));
        graphics.setFill(Color.color(1.0, 0.92, 0.65, 0.85));
        graphics.fillOval(centerX - burstRadius * 0.5, centerY - burstRadius * 0.5, burstRadius, burstRadius);

        graphics.setGlobalAlpha(Math.max(0.0, 0.55 - progress * 0.55));
        graphics.setFill(Color.color(0.9, 0.42, 0.15, 0.85));
        graphics.fillOval(centerX - burstRadius * 0.38, centerY - burstRadius * 0.38, burstRadius * 0.76, burstRadius * 0.76);

        for (Particle particle : particles) {
            double particleProgress = Math.min(1.0, progress / particle.lifeFactor);
            double px = centerX + particle.vx * particleProgress * zoom;
            double py = centerY + particle.vy * particleProgress * zoom;
            double size = Math.max(1.5, (3.5 - particleProgress * 2.2) * zoom * 0.35);

            graphics.setGlobalAlpha(Math.max(0.0, 0.85 - particleProgress * 0.85));
            graphics.setFill(Color.color(1.0, 0.55, 0.22, 0.95));
            graphics.fillOval(px - size, py - size, size * 2, size * 2);

            graphics.setGlobalAlpha(Math.max(0.0, 0.65 - particleProgress * 0.65));
            graphics.setFill(Color.color(0.30, 0.30, 0.30, 0.80));
            graphics.fillOval(px - size * 0.8, py - size * 0.8, size * 1.6, size * 1.6);
        }
        graphics.restore();
    }

    private static final class Particle {
        private final double vx;
        private final double vy;
        private final double lifeFactor;

        private Particle(double vx, double vy, double lifeFactor) {
            this.vx = vx;
            this.vy = vy;
            this.lifeFactor = lifeFactor;
        }
    }
}

