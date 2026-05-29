package system.resource;

import core.GameBalance;
import map.MapObjectData;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * ResourceManager:
 * - Lop trung tam quan ly toan bo resource runtime.
 * - Nhiem vu chinh:
 *   1) Parse object map -> tao ResourceNode
 *   2) Quan ly damage / destroy / drop
 *   3) Cung cap collision objects dang con song
 *   4) Ho tro respawn theo thoi gian
 *
 * Luu y:
 * - Map chi la "du lieu khoi tao"
 * - Trang thai runtime (currentHp, alive) nam trong manager + node
 */
public class ResourceManager {
    // Luu node theo objectId de truy cap nhanh.
    private final Map<Integer, ResourceNode> resourcesById;
    // Registry dinh nghia theo kind (tree_oak, rock_small, ...)
    private final Map<String, ResourceDefinition> definitionsByKind;
    // Random dung de roll drop amount.
    private final Random random;

    public ResourceManager() {
        this.resourcesById = new LinkedHashMap<>();
        this.definitionsByKind = new HashMap<>();
        this.random = new Random();
        registerDefaultDefinitions();
    }

    /**
     * registerDefaultDefinitions:
     * - Cac default config de fallback khi map thieu mot so field.
     * - Team co the doi so choi o 1 cho duy nhat nay.
     */
    private void registerDefaultDefinitions() {
        registerDefinition(new ResourceDefinition("tree_oak", ResourceType.TREE, 5, "wood", 2, 2, -1));
        registerDefinition(new ResourceDefinition("rock_small", ResourceType.ROCK, GameBalance.ROCK_HITS_TO_BREAK, "rock", 2, 2, -1));
        registerDefinition(new ResourceDefinition("grass", ResourceType.GRASS, 1, "fiber", 1, 2, 30));
        registerDefinition(new ResourceDefinition("vegetable_carrot", ResourceType.VEGETABLE, 1, "carrot", 1, 1, 60));
    }

    public void registerDefinition(ResourceDefinition definition) {
        if (definition == null || definition.getKindKey() == null || definition.getKindKey().isBlank()) {
            return;
        }
        definitionsByKind.put(definition.getKindKey().trim().toLowerCase(), definition);
    }

    /**
     * loadFromMapObjects:
     * - Parse object list -> build runtime resource nodes.
     * - CHI lay object nao co schema resource (co kind hoac dropItem hoac maxHp).
     */
    public void loadFromMapObjects(List<MapObjectData> objects) {
        resourcesById.clear();
        if (objects == null) {
            return;
        }

        for (MapObjectData object : objects) {
            if (object == null) {
                continue;
            }

            Map<String, String> props = object.getProperties();
            if (props == null || props.isEmpty()) {
                continue;
            }

            // Detect nhanh object co y do la resource.
            String kindRaw = props.getOrDefault("kind", "").trim();
            String dropItemRaw = props.getOrDefault("dropItem", "").trim();
            String maxHpRaw = props.getOrDefault("maxHp", "").trim();
            boolean isResource = !kindRaw.isEmpty() || !dropItemRaw.isEmpty() || !maxHpRaw.isEmpty();
            if (!isResource) {
                continue;
            }

            // Chuan hoa kind de tra registry.
            String kind = kindRaw.isEmpty() ? "unknown" : kindRaw.toLowerCase();
            ResourceDefinition definition = definitionsByKind.get(kind);
            if (definition == null) {
                definition = new ResourceDefinition(
                        kind,
                        guessTypeFromKind(kind),
                        1,
                        dropItemRaw.isEmpty() ? "unknown_item" : dropItemRaw,
                        1,
                        1,
                        -1
                );
            }

            // Resolve tung field:
            // - uu tien property tren map
            // - fallback sang definition default
            // Ho tro ca schema moi (maxHp) va schema cu (hp) de map hien tai van dung duoc.
            int maxHp = parseInt(props.get("maxHp"), parseInt(props.get("hp"), definition.getDefaultMaxHp()));
            String dropItem = valueOrDefault(props.get("dropItem"), definition.getDefaultDropItem());
            int dropMin = parseInt(props.get("dropMin"), definition.getDefaultDropMin());
            int dropMax = parseInt(props.get("dropMax"), definition.getDefaultDropMax());
            int respawnSec = parseInt(props.get("respawnSec"), definition.getDefaultRespawnSeconds());
            ResourceType type = definition.getResourceType();
            Set<Long> visualTiles = parseVisualTiles(props.get("visualTiles"));

            ResourceNode node = new ResourceNode(
                    object.getId(),
                    kind,
                    type,
                    object.getX(),
                    object.getY(),
                    object.getWidth(),
                    object.getHeight(),
                    dropItem,
                    dropMin,
                    dropMax,
                    respawnSec,
                    maxHp,
                    visualTiles
            );

            resourcesById.put(node.getObjectId(), node);
        }
    }

    /**
     * hitResource:
     * - Giam hp cua node theo objectId.
     * - Neu node bi pha se tra ve DropResult de he thong khac xu ly spawn item.
     */
    public ResourceHitResult hitResource(int objectId, int damage, long nowNs) {
        ResourceNode node = resourcesById.get(objectId);
        if (node == null || !node.isAlive()) {
            return null;
        }

        int realDamage = Math.max(0, damage);
        boolean destroyed = node.applyDamage(realDamage, nowNs);
        if (realDamage > 0) {
            // Tang thoi gian flash sat muc tren yeu cau (150ms) de de nhan thay hon.
            node.triggerHitFlash(nowNs, 150_000_000L, Color.rgb(255, 255, 255));
        }
        if (!destroyed) {
            return new ResourceHitResult(node, realDamage, false, null);
        }

        int amount = rollDropAmount(node.getDropMin(), node.getDropMax());
        return new ResourceHitResult(node, realDamage, true, new DropResult(node.getDropItem(), amount));
    }

    /**
     * hitFirstResourceIntersecting:
     * - Utility cho giai doan MVP:
     *   tim node dau tien giao voi hitbox attack roi ap damage.
     */
    public ResourceHitResult hitFirstResourceIntersecting(double x, double y, double w, double h, int damage, long nowNs) {
        for (ResourceNode node : resourcesById.values()) {
            if (!node.isAlive()) {
                continue;
            }
            if (node.intersects(x, y, w, h)) {
                return hitResource(node.getObjectId(), damage, nowNs);
            }
        }
        return null;
    }

    public List<ResourceHitResult> damageResourcesInRadius(double centerX, double centerY, double radius, int damage, long nowNs) {
        List<ResourceHitResult> results = new ArrayList<>();
        if (radius <= 0 || damage <= 0) {
            return results;
        }
        double radiusSquared = radius * radius;
        for (ResourceNode node : resourcesById.values()) {
            if (node == null || !node.isAlive()) {
                continue;
            }
            double dx = node.getCenterX() - centerX;
            double dy = node.getCenterY() - centerY;
            if (dx * dx + dy * dy > radiusSquared) {
                continue;
            }
            ResourceHitResult hitResult = hitResource(node.getObjectId(), damage, nowNs);
            if (hitResult != null) {
                results.add(hitResult);
            }
        }
        return results;
    }

    /**
     * update:
     * - Goi moi frame de xu ly respawn neu node dat dieu kien.
     */
    public void update(long nowNs) {
        for (ResourceNode node : resourcesById.values()) {
            if (node.shouldRespawn(nowNs)) {
                node.respawn();
            }
        }
    }

    /**
     * isBlockedByAliveResource:
     * - Ho tro collision movement:
     *   true khi hitbox nhan vat giao voi resource dang con song.
     */
    public boolean isBlockedByAliveResource(double x, double y, double w, double h) {
        for (ResourceNode node : resourcesById.values()) {
            if (!node.isAlive() || !node.blocksMovement()) {
                continue;
            }
            if (node.intersects(x, y, w, h)) {
                return true;
            }
        }
        return false;
    }

    public ResourceNode getResourceById(int objectId) {
        return resourcesById.get(objectId);
    }

    public List<ResourceNode> getAllResources() {
        return Collections.unmodifiableList(new ArrayList<>(resourcesById.values()));
    }

    public List<ResourceNode> getAliveResources() {
        List<ResourceNode> alive = new ArrayList<>();
        for (ResourceNode node : resourcesById.values()) {
            if (node.isAlive()) {
                alive.add(node);
            }
        }
        return Collections.unmodifiableList(alive);
    }

    /**
     * addGeneratedResource:
     * - Input: resource procedural sinh tu chunk map vo han.
     * - Output: ResourceNode vua duoc tao hoac node cu neu objectId da ton tai.
     * - Tac dong: bo sung tai nguyen runtime de nguoi choi co the khai thac o vung moi.
     */
    public ResourceNode addGeneratedResource(int objectId,
                                             String kind,
                                             ResourceType explicitType,
                                             double x,
                                             double y,
                                             double width,
                                             double height) {
        if (resourcesById.containsKey(objectId)) {
            return resourcesById.get(objectId);
        }

        String normalizedKind = kind == null ? "unknown" : kind.trim().toLowerCase();
        ResourceDefinition definition = definitionsByKind.get(normalizedKind);
        if (definition == null) {
            definition = new ResourceDefinition(
                    normalizedKind,
                    explicitType == null ? guessTypeFromKind(normalizedKind) : explicitType,
                    2,
                    "wood",
                    1,
                    2,
                    -1
            );
        }

        ResourceNode node = new ResourceNode(
                objectId,
                normalizedKind,
                explicitType == null ? definition.getResourceType() : explicitType,
                x,
                y,
                width,
                height,
                definition.getDefaultDropItem(),
                definition.getDefaultDropMin(),
                definition.getDefaultDropMax(),
                definition.getDefaultRespawnSeconds(),
                definition.getDefaultMaxHp(),
                Collections.emptySet()
        );
        resourcesById.put(objectId, node);
        return node;
    }

    private int rollDropAmount(int min, int max) {
        int a = Math.max(0, min);
        int b = Math.max(a, max);
        if (a == b) {
            return a;
        }
        return a + random.nextInt(b - a + 1);
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String valueOrDefault(String raw, String fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return raw.trim();
    }

    private ResourceType guessTypeFromKind(String kind) {
        if (kind == null) {
            return ResourceType.UNKNOWN;
        }
        String key = kind.toLowerCase();
        if (key.contains("tree")) {
            return ResourceType.TREE;
        }
        if (key.contains("rock") || key.contains("stone")) {
            return ResourceType.ROCK;
        }
        if (key.contains("grass")) {
            return ResourceType.GRASS;
        }
        if (key.contains("vegetable") || key.contains("carrot")) {
            return ResourceType.VEGETABLE;
        }
        return ResourceType.UNKNOWN;
    }

    private Set<Long> parseVisualTiles(String raw) {
        Set<Long> keys = new HashSet<>();
        if (raw == null || raw.isBlank()) {
            return keys;
        }
        String[] parts = raw.split(";");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String[] xy = part.trim().split(":");
            if (xy.length != 2) {
                continue;
            }
            try {
                int x = Integer.parseInt(xy[0].trim());
                int y = Integer.parseInt(xy[1].trim());
                keys.add(ResourceNode.packTileKey(x, y));
            } catch (Exception ignored) {
                // Bo qua tile loi format.
            }
        }
        return keys;
    }
}
