package buildsystem.core;

import entity.Player;

/**
 * BuildController:
 * - Facade cho input layer goi BuildManager theo action user.
 * - Giup Game/demo khong phai biet chi tiet noi bo cua preview/place/rotate/toolbar.
 */
public class BuildController {
    private final BuildManager buildManager;

    public BuildController(BuildManager buildManager) {
        this.buildManager = buildManager;
    }

    public void onToolbarSlotSelected(int slotIndex, BuildInventory inventory) {
        buildManager.selectToolbarSlot(slotIndex, inventory);
    }

    public void onSelectBuildItem(String itemId) {
        buildManager.selectItem(itemId);
    }

    public void onRotatePressed() {
        buildManager.rotateSelected();
    }

    public void onCursorMoved(double mouseScreenX,
                              double mouseScreenY,
                              double cameraX,
                              double cameraY,
                              double cameraZoom,
                              boolean mouseOverUi,
                              Player player,
                              BuildInventory inventory) {
        buildManager.updatePreview(mouseScreenX, mouseScreenY, cameraX, cameraY, cameraZoom, mouseOverUi, player, inventory);
    }

    public boolean onPrimaryClickPlace(Player player, BuildInventory inventory) {
        return buildManager.tryPlaceSelected(player, inventory);
    }
}
