package buildsystem.object;

import buildsystem.component.BuildComponent;
import buildsystem.component.CollisionComponent;
import buildsystem.component.HealthComponent;
import buildsystem.component.RotationComponent;
import buildsystem.core.BuildType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BuildObject:
 * - Base class chung cho moi object dat xuong world.
 * - Save/load ready:
 *   1) id duy nhat.
 *   2) type.
 *   3) tile position.
 *   4) rotation.
 *   5) health.
 *   6) spriteKey de load lai visual trung khop.
 *
 * Luong du lieu:
 * - BuildFactory tao object that tu BuildDefinition + BuildObjectSeed.
 * - BuildManager giu danh sach placed object, refresh sprite va save ra JSON.
 * - Runtime system co the doc component map de mo rong light/inventory/damage/collision sau nay.
 */
public abstract class BuildObject {
    private final String id;
    private final BuildType type;
    private final int tileX;
    private final int tileY;
    private final int tileWidth;
    private final int tileHeight;
    private final String autoTileGroup;
    private final double renderWidth;
    private final double renderHeight;
    private final double anchorX;
    private final double anchorY;
    private String spriteKey;
    private double rotationDegrees;
    private int health;
    private final Map<Class<? extends BuildComponent>, BuildComponent> components;

    protected BuildObject(String id,
                          BuildType type,
                          int tileX,
                          int tileY,
                          int tileWidth,
                          int tileHeight,
                          String autoTileGroup,
                          double renderWidth,
                          double renderHeight,
                          double anchorX,
                          double anchorY,
                          String spriteKey,
                          double rotationDegrees,
                          int health) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.type = type;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.autoTileGroup = autoTileGroup == null ? "" : autoTileGroup;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.spriteKey = spriteKey;
        this.rotationDegrees = normalize(rotationDegrees);
        this.health = Math.max(0, health);
        this.components = new LinkedHashMap<>();
    }

    public String getId() { return id; }
    public BuildType getType() { return type; }
    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public String getAutoTileGroup() { return autoTileGroup; }
    public double getRenderWidth() { return renderWidth; }
    public double getRenderHeight() { return renderHeight; }
    public String getSpriteKey() { return spriteKey; }
    public double getRotationDegrees() { return rotationDegrees; }
    public int getHealth() { return health; }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = normalize(rotationDegrees);
        RotationComponent rotationComponent = getComponent(RotationComponent.class);
        if (rotationComponent != null) {
            rotationComponent.setRotationDegrees(this.rotationDegrees);
        }
    }

    public void setSpriteKey(String spriteKey) {
        this.spriteKey = spriteKey;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, health);
        HealthComponent healthComponent = getComponent(HealthComponent.class);
        if (healthComponent != null) {
            healthComponent.setHp(this.health);
        }
    }

    public double getRenderX() {
        return tileX * tileWidth + (tileWidth - renderWidth) / 2.0 + anchorX;
    }

    public double getRenderY() {
        return tileY * tileHeight + (tileHeight - renderHeight) / 2.0 + anchorY;
    }

    public double getCollisionWidth() {
        CollisionComponent collision = getComponent(CollisionComponent.class);
        return collision == null ? renderWidth : collision.getWidth();
    }

    public double getCollisionHeight() {
        CollisionComponent collision = getComponent(CollisionComponent.class);
        return collision == null ? renderHeight : collision.getHeight();
    }

    public void addComponent(BuildComponent component) {
        if (component == null) {
            return;
        }
        components.put(component.getClass(), component);
    }

    @SuppressWarnings("unchecked")
    public <T extends BuildComponent> T getComponent(Class<T> clazz) {
        BuildComponent component = components.get(clazz);
        if (component == null) {
            return null;
        }
        return (T) component;
    }

    public Map<String, Object> toSaveMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("tileX", tileX);
        map.put("tileY", tileY);
        map.put("rotation", rotationDegrees);
        map.put("health", health);
        map.put("spriteKey", spriteKey);
        return map;
    }

    private double normalize(double value) {
        double out = value % 360.0;
        if (out < 0) {
            out += 360.0;
        }
        return out;
    }
}
