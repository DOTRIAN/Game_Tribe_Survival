package buildsystem.core;

import buildsystem.object.BuildObject;
import buildsystem.placement.PlacementContext;
import buildsystem.preview.GhostPreviewRenderer;
import buildsystem.preview.PreviewMaterial;
import buildsystem.sprite.BuildSpriteResolver;
import buildsystem.sprite.OrthogonalAutoTileResolver;
import buildsystem.sprite.SpriteSelection;
import buildsystem.ui.BuildHotbarSlot;
import buildsystem.ui.BuildToolbar;
import entity.Player;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BuildManager:
 * - He thong build dung chung cho game chinh va demo, khong phai "wall-only system".
 * - Build flow:
 *   1) Toolbar slot hoac item id -> chon BuildDefinition.
 *   2) Mouse move -> update preview ghost dung chung.
 *   3) Q -> doi rotation neu object co RotationComponent.
 *   4) Click -> validate placement -> tru inventory -> tao BuildObject -> add vao world.
 *   5) Sau khi dat xong -> refresh sprite object vua dat va neighbors qua AutoTileResolver.
 *   6) Save/load -> serialize/restore danh sach BuildObject bang id/type/tile/rotation/health.
 */
public class BuildManager {
    private final BuildAssetResolver assetResolver;
    private final BuildWorldQuery worldQuery;
    private final BuildRegistry registry;
    private final BuildFactory factory;
    private final PlacementValidator placementValidator;
    private final RotationManager rotationManager;
    private final BuildSpriteResolver spriteResolver;
    private final GhostPreviewRenderer previewRenderer;
    private final BuildPreview preview;
    private final BuildToolbar toolbar;
    private final Map<String, BuildObject> objectsById;
    private final Map<String, BuildObject> objectsByTile;

    private BuildDefinition selectedDefinition;
    private BuildMode buildMode;
    private BuildObject lastPlacedObject;

    public BuildManager(BuildAssetResolver assetResolver, BuildWorldQuery worldQuery) {
        this.assetResolver = assetResolver;
        this.worldQuery = worldQuery;
        this.registry = new BuildRegistry();
        this.factory = new BuildFactory();
        this.placementValidator = new PlacementValidator(worldQuery);
        this.rotationManager = new RotationManager();
        this.spriteResolver = new BuildSpriteResolver(new OrthogonalAutoTileResolver());
        this.previewRenderer = new GhostPreviewRenderer(new PreviewMaterial(0.5, Color.color(1, 0.2, 0.2, 0.20)));
        this.preview = new BuildPreview();
        this.toolbar = new BuildToolbar();
        this.objectsById = new LinkedHashMap<>();
        this.objectsByTile = new LinkedHashMap<>();
        this.selectedDefinition = null;
        this.buildMode = BuildMode.NONE;
        this.lastPlacedObject = null;
        syncToolbar(Collections.emptyMap());
    }

    public void syncToolbar(Map<String, Integer> inventorySnapshot) {
        List<BuildHotbarSlot> slots = new ArrayList<>();
        for (BuildDefinition definition : registry.all()) {
            if (!definition.isToolbarVisible()) {
                continue;
            }
            int count = inventorySnapshot == null ? 0 : inventorySnapshot.getOrDefault(definition.getItemId(), 0);
            if (count <= 0) {
                continue;
            }
            slots.add(new BuildHotbarSlot(definition, count, definition.getBuildCost()));
        }
        toolbar.setSlots(slots);
    }

    public void selectToolbarSlot(int slotIndex, BuildInventory inventory) {
        syncToolbar(inventory == null ? Collections.emptyMap() : inventory.snapshot());
        if (slotIndex < 0 || slotIndex >= toolbar.getSlots().size()) {
            cancelBuildMode();
            return;
        }
        toolbar.setSelectedIndex(slotIndex);
        BuildHotbarSlot slot = toolbar.getSelectedSlot();
        if (slot == null) {
            cancelBuildMode();
            return;
        }
        int available = inventory == null ? 0 : inventory.getAmount(slot.getDefinition().getItemId());
        if (available < slot.getDefinition().getBuildCost()) {
            buildMode = BuildMode.NONE;
            selectedDefinition = null;
            preview.setVisible(false);
            preview.setValid(false);
            return;
        }
        selectItem(slot.getDefinition().getItemId());
    }

    public void selectItem(String itemId) {
        selectedDefinition = registry.findByItemId(itemId);
        if (selectedDefinition == null) {
            cancelBuildMode();
            return;
        }
        buildMode = BuildMode.BUILDING;
        rotationManager.reset();
        preview.setVisible(true);
    }

    public void rotateSelected() {
        if (selectedDefinition == null || !selectedDefinition.isRotatable()) {
            return;
        }
        rotationManager.rotateClockwise();
    }

    public void cancelBuildMode() {
        selectedDefinition = null;
        buildMode = BuildMode.NONE;
        rotationManager.reset();
        preview.setVisible(false);
        preview.setValid(false);
        preview.setValidationMessage("cancelled");
    }

    public void updatePreview(double mouseScreenX,
                              double mouseScreenY,
                              double cameraX,
                              double cameraY,
                              double cameraZoom,
                              boolean mouseOverUi,
                              Player player,
                              BuildInventory inventory) {
        if (buildMode != BuildMode.BUILDING || selectedDefinition == null) {
            preview.setVisible(false);
            preview.setValid(false);
            return;
        }
        int availableCount = inventory == null ? 0 : inventory.getAmount(selectedDefinition.getItemId());
        if (availableCount <= 0) {
            preview.setVisible(false);
            preview.setValid(false);
            preview.setValidationMessage("out-of-item");
            return;
        }

        double worldX = cameraX + mouseScreenX / cameraZoom;
        double worldY = cameraY + mouseScreenY / cameraZoom;
        int tileWidth = worldQuery.getTileWidth();
        int tileHeight = worldQuery.getTileHeight();
        int tileX = selectedDefinition.getPlacementStrategy().snapX(worldX, tileWidth);
        int tileY = selectedDefinition.getPlacementStrategy().snapY(worldY, tileHeight);

        PlacementContext context = new PlacementContext(
                worldX,
                worldY,
                tileX,
                tileY,
                rotationManager.getCurrentRotationDegrees(),
                player,
                mouseOverUi
        );
        PlacementResult placementResult = placementValidator.validate(selectedDefinition, context, objectsByTile.values());
        SpriteSelection selection = spriteResolver.resolve(
                selectedDefinition,
                tileX,
                tileY,
                objectsByTile.values(),
                rotationManager.getCurrentRotationDegrees(),
                selectedDefinition.getDefaultSpriteKey()
        );

        preview.setVisible(true);
        preview.setType(selectedDefinition.getType());
        preview.setTileX(tileX);
        preview.setTileY(tileY);
        preview.setRotationDegrees(selection.getRotationDegrees());
        preview.setRotationLabel(rotationLabelFromDegrees(selection.getRotationDegrees()));
        preview.setSpriteKey(selection.getSpriteKey());
        preview.setNeighborMask(selection.getNeighborMask());
        preview.setImage(assetResolver.getSprite(selection.getSpriteKey()));
        preview.setWidth(selectedDefinition.getFootprintWidthTiles() * tileWidth);
        preview.setHeight(selectedDefinition.getFootprintHeightTiles() * tileHeight);
        preview.setRenderX(tileX * tileWidth);
        preview.setRenderY(tileY * tileHeight);
        preview.setValidationMessage(placementResult.getReason());
        previewRenderer.applyMaterial(preview, placementResult.isValid());
    }

    public boolean tryPlaceSelected(Player player, BuildInventory inventory) {
        lastPlacedObject = null;
        if (selectedDefinition == null || inventory == null) {
            return false;
        }
        if (!preview.isVisible() || !preview.isValid()) {
            return false;
        }
        if (inventory.getAmount(selectedDefinition.getItemId()) < selectedDefinition.getBuildCost()) {
            preview.setValid(false);
            preview.setValidationMessage("out-of-item");
            return false;
        }

        PlacementContext context = new PlacementContext(
                preview.getRenderX(),
                preview.getRenderY(),
                preview.getTileX(),
                preview.getTileY(),
                preview.getRotationDegrees(),
                player,
                false
        );
        PlacementResult validNow = placementValidator.validate(selectedDefinition, context, objectsByTile.values());
        if (!validNow.isValid()) {
            preview.setValid(false);
            preview.setValidationMessage(validNow.getReason());
            return false;
        }
        if (!inventory.consumeItem(selectedDefinition.getItemId(), selectedDefinition.getBuildCost())) {
            preview.setValid(false);
            preview.setValidationMessage("consume-failed");
            return false;
        }

        BuildObject object = factory.create(
                selectedDefinition,
                UUID.randomUUID().toString(),
                preview.getTileX(),
                preview.getTileY(),
                worldQuery.getTileWidth(),
                worldQuery.getTileHeight(),
                preview.getSpriteKey(),
                preview.getRotationDegrees(),
                selectedDefinition.getHealth()
        );
        addPlacedObject(object);
        lastPlacedObject = object;
        syncToolbar(inventory.snapshot());
        if (inventory.getAmount(selectedDefinition.getItemId()) < selectedDefinition.getBuildCost()) {
            buildMode = BuildMode.NONE;
            selectedDefinition = null;
            preview.setVisible(false);
            preview.setValid(false);
        }
        return true;
    }

    public void addPlacedObject(BuildObject object) {
        if (object == null) {
            return;
        }
        objectsById.put(object.getId(), object);
        objectsByTile.put(tileKey(object.getTileX(), object.getTileY()), object);
        refreshObjectAndNeighbors(object.getTileX(), object.getTileY());
    }

    public BuildDamageResult hitFirstDamageableIntersecting(double x,
                                                            double y,
                                                            double width,
                                                            double height,
                                                            int damage,
                                                            long nowNs) {
        if (damage <= 0) {
            return null;
        }
        for (BuildObject object : new ArrayList<>(objectsById.values())) {
            if (object == null || !object.isAlive()) {
                continue;
            }
            if (!intersectsRect(
                    x,
                    y,
                    width,
                    height,
                    object.getRenderX(),
                    object.getRenderY(),
                    object.getRenderWidth(),
                    object.getRenderHeight())) {
                continue;
            }

            int applied = Math.min(object.getHealth(), Math.max(0, damage));
            if (applied <= 0) {
                continue;
            }

            object.setHealth(object.getHealth() - applied);
            object.triggerHitFlash(nowNs, 130_000_000L, Color.rgb(255, 196, 92));

            boolean destroyed = !object.isAlive();
            String dropItemId = "";
            int dropAmount = 0;
            if (destroyed) {
                BuildDefinition definition = registry.findByType(object.getType());
                if (definition != null) {
                    dropItemId = definition.getItemId();
                    dropAmount = 1;
                }
                removePlacedObject(object);
            }
            return new BuildDamageResult(object, applied, destroyed, dropItemId, dropAmount);
        }
        return null;
    }

    public void restoreFromSaveData(Object savedData) {
        clearObjects();
        if (!(savedData instanceof Collection<?> entries)) {
            return;
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Object typeValue = rawMap.get("type");
            if (!(typeValue instanceof String typeName)) {
                continue;
            }
            BuildType type;
            try {
                type = BuildType.valueOf(typeName);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            BuildDefinition definition = registry.findByType(type);
            if (definition == null) {
                continue;
            }
            String objectId = rawMap.get("id") instanceof String id ? id : UUID.randomUUID().toString();
            int tileX = readInt(rawMap.get("tileX"), 0);
            int tileY = readInt(rawMap.get("tileY"), 0);
            double rotation = readDouble(rawMap.get("rotation"), 0.0);
            int health = readInt(rawMap.get("health"), definition.getHealth());
            String spriteKey = rawMap.get("spriteKey") instanceof String sprite
                    ? sprite
                    : definition.getDefaultSpriteKey();

            BuildObject object = factory.create(
                    definition,
                    objectId,
                    tileX,
                    tileY,
                    worldQuery.getTileWidth(),
                    worldQuery.getTileHeight(),
                    spriteKey,
                    rotation,
                    health
            );
            objectsById.put(object.getId(), object);
            objectsByTile.put(tileKey(tileX, tileY), object);
        }

        for (BuildObject object : objectsById.values()) {
            refresh(object.getTileX(), object.getTileY());
        }
    }

    public List<Map<String, Object>> exportSaveData() {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (BuildObject object : objectsById.values()) {
            snapshot.add(object.toSaveMap());
        }
        return snapshot;
    }

    public Collection<BuildObject> getPlacedObjects() {
        return Collections.unmodifiableCollection(objectsById.values());
    }

    public BuildPreview getPreview() {
        return preview;
    }

    public BuildMode getBuildMode() {
        return buildMode;
    }

    public BuildToolbar getToolbar() {
        return toolbar;
    }

    public BuildRegistry getRegistry() {
        return registry;
    }

    public BuildDefinition getSelectedDefinition() {
        return selectedDefinition;
    }

    public BuildObject getLastPlacedObject() {
        return lastPlacedObject;
    }

    public boolean isPreviewVisible() {
        return preview.isVisible();
    }

    public void clearObjects() {
        objectsById.clear();
        objectsByTile.clear();
        lastPlacedObject = null;
        preview.setVisible(false);
        preview.setValid(false);
    }

    private void refreshObjectAndNeighbors(int tileX, int tileY) {
        refresh(tileX, tileY);
        refresh(tileX + 1, tileY);
        refresh(tileX - 1, tileY);
        refresh(tileX, tileY + 1);
        refresh(tileX, tileY - 1);
    }

    private void removePlacedObject(BuildObject object) {
        if (object == null) {
            return;
        }
        objectsById.remove(object.getId());
        objectsByTile.remove(tileKey(object.getTileX(), object.getTileY()));
        refreshObjectAndNeighbors(object.getTileX(), object.getTileY());
    }

    private void refresh(int tileX, int tileY) {
        BuildObject object = objectsByTile.get(tileKey(tileX, tileY));
        if (object == null) {
            return;
        }
        BuildDefinition definition = registry.findByType(object.getType());
        if (definition == null) {
            return;
        }
        SpriteSelection selection = spriteResolver.resolve(
                definition,
                tileX,
                tileY,
                objectsById.values(),
                object.getRotationDegrees(),
                object.getSpriteKey()
        );
        object.setRotationDegrees(selection.getRotationDegrees());
        object.setSpriteKey(selection.getSpriteKey());
    }

    private String tileKey(int tileX, int tileY) {
        return tileX + ":" + tileY;
    }

    private boolean intersectsRect(double ax, double ay, double aw, double ah,
                                   double bx, double by, double bw, double bh) {
        return ax < bx + bw
                && ax + aw > bx
                && ay < by + bh
                && ay + ah > by;
    }

    private String rotationLabelFromDegrees(double degrees) {
        int normalized = (int) ((degrees % 360 + 360) % 360);
        return switch (normalized) {
            case 90 -> "E";
            case 180 -> "S";
            case 270 -> "W";
            default -> "N";
        };
    }

    private int readInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private double readDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}
