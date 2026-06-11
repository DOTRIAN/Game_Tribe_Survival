package system.bomb;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExplosionEffect {
    private static final long SPREAD_DURATION_NS = 850_000_000L;
    private static final long EMBER_DURATION_NS = 700_000_000L;
    private static final long TOTAL_DURATION_NS = SPREAD_DURATION_NS + EMBER_DURATION_NS;
    private final double worldX;
    private final double worldY;
    private final double radius;
    private final long startedAtNs;
    private final List<Particle> particles;
    private final Image[] blastSprites;
    private final Image[] emberSprites;

    public ExplosionEffect(double worldX, double worldY, double radius, long startedAtNs) {
        this(worldX, worldY, radius, startedAtNs, new Image[0], new Image[0]);
    }

    public ExplosionEffect(double worldX, double worldY, double radius, long startedAtNs, Image blastSprite, Image emberSprite) {
        this(worldX, worldY, radius, startedAtNs,
                blastSprite == null ? new Image[0] : new Image[]{blastSprite},
                emberSprite == null ? new Image[0] : new Image[]{emberSprite});
    }

    public ExplosionEffect(double worldX, double worldY, double radius, long startedAtNs, Image[] blastSprites, Image[] emberSprites) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.radius = radius;
        this.startedAtNs = startedAtNs;
        this.particles = new ArrayList<>();
        this.blastSprites = blastSprites == null ? new Image[0] : blastSprites.clone();
        this.emberSprites = emberSprites == null ? new Image[0] : emberSprites.clone();
        Random random = new Random(Double.doubleToLongBits(worldX * 31.0 + worldY * 17.0 + startedAtNs));
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 24.0 + random.nextDouble() * 40.0;
            double life = 0.35 + random.nextDouble() * 0.55;
            particles.add(new Particle(Math.cos(angle) * speed, Math.sin(angle) * speed, life));
        }
    }

    public boolean isAlive(long nowNs) {
        return nowNs - startedAtNs <= TOTAL_DURATION_NS;
    }

    public void render(GraphicsContext graphics, double cameraX, double cameraY, double zoom, long nowNs) {
        long elapsed = Math.max(0L, nowNs - startedAtNs);
        double spreadProgress = Math.min(1.0, elapsed / (double) SPREAD_DURATION_NS);
        double emberProgress = elapsed <= SPREAD_DURATION_NS
                ? 0.0
                : Math.min(1.0, (elapsed - SPREAD_DURATION_NS) / (double) EMBER_DURATION_NS);
        double centerX = (worldX - cameraX) * zoom;
        double centerY = (worldY - cameraY) * zoom;
        double burstRadius = radius * zoom * (0.45 + spreadProgress * 1.15);

        graphics.save();
        graphics.setGlobalAlpha(Math.max(0.0, 0.55 - spreadProgress * 0.55));
        graphics.setFill(Color.color(1.0, 0.92, 0.65, 0.85));
        graphics.fillOval(centerX - burstRadius * 0.60, centerY - burstRadius * 0.24, burstRadius * 1.20, burstRadius * 0.48);

        graphics.setGlobalAlpha(Math.max(0.0, 0.45 - spreadProgress * 0.45));
        graphics.setFill(Color.color(0.9, 0.42, 0.15, 0.85));
        graphics.fillOval(centerX - burstRadius * 0.52, centerY - burstRadius * 0.20, burstRadius * 1.04, burstRadius * 0.40);
        renderSpriteLayers(graphics, centerX, centerY, burstRadius, spreadProgress, emberProgress);

        for (Particle particle : particles) {
            double particleProgress = Math.min(1.0, spreadProgress / particle.lifeFactor);
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

    private void renderSpriteLayers(GraphicsContext graphics,
                                    double centerX,
                                    double centerY,
                                    double burstRadius,
                                    double spreadProgress,
                                    double emberProgress) {
        Image blastSprite = resolveFrame(blastSprites, spreadProgress);
        if (blastSprite != null && !blastSprite.isError()) {
            double blastWidth = burstRadius * (0.85 + spreadProgress * 2.35);
            double blastHeight = blastWidth * (blastSprite.getHeight() / Math.max(1.0, blastSprite.getWidth()));
            graphics.setGlobalAlpha(Math.max(0.0, 0.92 - spreadProgress * 0.52));
            graphics.drawImage(blastSprite, centerX - blastWidth * 0.5, centerY - blastHeight * 0.5, blastWidth, blastHeight);
        }
        Image emberSprite = resolveFrame(emberSprites, emberProgress);
        if (emberSprite != null && !emberSprite.isError()) {
            double emberWidth = burstRadius * 2.15;
            double emberHeight = emberWidth * (emberSprite.getHeight() / Math.max(1.0, emberSprite.getWidth()));
            graphics.setGlobalAlpha(Math.max(0.0, 0.82 - emberProgress * 0.82));
            graphics.drawImage(emberSprite, centerX - emberWidth * 0.5, centerY - emberHeight * 0.5, emberWidth, emberHeight);
        }
    }

    private Image resolveFrame(Image[] frames, double progress) {
        if (frames == null || frames.length == 0) {
            return null;
        }
        int index = (int) Math.floor(Math.max(0.0, Math.min(0.999, progress)) * frames.length);
        index = Math.max(0, Math.min(frames.length - 1, index));
        return frames[index];
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

