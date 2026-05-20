package ui;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * ResponsiveLayoutManager:
 * - Gom cac ham anchor co dinh de UI duoc canh nhat quan khi resize/fullscreen.
 * - Input: node va khoang cach canh.
 * - Output: node duoc gan anchor vao AnchorPane.
 */
public final class ResponsiveLayoutManager {
    private ResponsiveLayoutManager() {
    }

    public static void anchorTopLeft(Node node, double top, double left) {
        AnchorPane.setTopAnchor(node, top);
        AnchorPane.setLeftAnchor(node, left);
    }

    public static void anchorTopRight(Node node, double top, double right) {
        AnchorPane.setTopAnchor(node, top);
        AnchorPane.setRightAnchor(node, right);
    }

    public static void anchorBottomRight(Node node, double bottom, double right) {
        AnchorPane.setBottomAnchor(node, bottom);
        AnchorPane.setRightAnchor(node, right);
    }

    public static void anchorBottomCenter(Node node, double bottom) {
        AnchorPane.setBottomAnchor(node, bottom);
    }
}
