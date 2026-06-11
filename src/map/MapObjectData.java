package map;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapObjectData {
    // id cua object trong Tiled (huu ich khi debug).
    private final int id;
    // name/type la metadata chinh de phan loai object gameplay.
    private final String name;
    private final String type;
    // toa do + kich thuoc theo world space cua map.
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    // custom properties trong object layer (hp, dropItem, ...).
    private final Map<String, String> properties;

    public MapObjectData(int id, String name, String type, double x, double y, double width, double height,
                         Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.properties = new HashMap<>(properties);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    // Ho tro logic collision nhanh.
    public boolean intersects(double otherX, double otherY, double otherW, double otherH) {
        return x < otherX + otherW
                && x + width > otherX
                && y < otherY + otherH
                && y + height > otherY;
    }
}
