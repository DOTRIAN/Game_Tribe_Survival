package wallbuilder.controller;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import wallbuilder.service.BuildBoard;
import wallbuilder.ui.BuildBoardView;
import wallbuilder.ui.HotbarView;

/**
 * BuildController:
 * - Noi input voi BuildBoard va cac view.
 * - Class nay gom xu ly click dat block, xoa block va doi o hotbar.
 */
public class BuildController {
    private final BuildBoard buildBoard;
    private final BuildBoardView buildBoardView;
    private final HotbarView hotbarView;

    public BuildController(BuildBoard buildBoard, BuildBoardView buildBoardView, HotbarView hotbarView) {
        this.buildBoard = buildBoard;
        this.buildBoardView = buildBoardView;
        this.hotbarView = hotbarView;
    }

    public void handleMouseMoved(MouseEvent mouseEvent) {
        int row = buildBoardView.toBoardRow(mouseEvent.getY());
        int column = buildBoardView.toBoardColumn(mouseEvent.getX());
        if (row < 0 || column < 0) {
            buildBoardView.clearHoverCell();
            return;
        }
        buildBoardView.setHoverCell(row, column);
    }

    public void handleMouseExited() {
        buildBoardView.clearHoverCell();
    }

    public void handleMousePressed(MouseEvent mouseEvent) {
        int row = buildBoardView.toBoardRow(mouseEvent.getY());
        int column = buildBoardView.toBoardColumn(mouseEvent.getX());
        if (row < 0 || column < 0) {
            return;
        }

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            buildBoard.placeItem(row, column, hotbarView.getSelectedItem());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            buildBoard.removeTile(row, column);
        }

        buildBoardView.setHoverCell(row, column);
        buildBoardView.redraw();
    }

    public void handleKeyPressed(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code.isDigitKey()) {
            int slotIndex = parseSlotIndex(code);
            if (slotIndex >= 0 && slotIndex < 9) {
                hotbarView.updateSelection(slotIndex);
            }
        }
    }

    private int parseSlotIndex(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            case DIGIT5, NUMPAD5 -> 4;
            case DIGIT6, NUMPAD6 -> 5;
            case DIGIT7, NUMPAD7 -> 6;
            case DIGIT8, NUMPAD8 -> 7;
            case DIGIT9, NUMPAD9 -> 8;
            default -> -1;
        };
    }
}
