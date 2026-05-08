package input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.util.HashSet;
import java.util.Set;

public class InputHandler {

    private final Set<KeyCode> pressedKeys;
    private final Set<KeyCode> justPressedKeys;
    // Buffer ky tu text vua duoc go (dung cho man nhap ten).
    private final StringBuilder typedCharacters;

    // Du lieu chuot cho menu UI screen-space.
    private double mouseX;
    private double mouseY;
    private boolean mouseLeftJustClicked;

    public InputHandler() {
        this.pressedKeys = new HashSet<>();
        this.justPressedKeys = new HashSet<>();
        this.typedCharacters = new StringBuilder();
        this.mouseX = 0;
        this.mouseY = 0;
        this.mouseLeftJustClicked = false;
    }

    public void attach(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (!pressedKeys.contains(code)) {
                justPressedKeys.add(code);
            }
            pressedKeys.add(code);
        });

        scene.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            pressedKeys.remove(code);
            justPressedKeys.remove(code);
        });

        // onKeyTyped cho input text on dinh hon keycode (chu, so, space...).
        scene.setOnKeyTyped(event -> {
            String ch = event.getCharacter();
            if (ch != null && !ch.isEmpty()) {
                typedCharacters.append(ch);
            }
        });

        // Cap nhat vi tri chuot cho hover menu.
        scene.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        scene.setOnMouseDragged(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });

        // Click trai 1-lan/frame de xu ly chon menu.
        scene.setOnMouseClicked(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            if (event.getButton() == MouseButton.PRIMARY) {
                mouseLeftJustClicked = true;
            }
        });
    }

    public void update() {
        justPressedKeys.clear();
        mouseLeftJustClicked = false;
    }

    public boolean isPressed(KeyCode keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public void clearPressedKeys() {
        pressedKeys.clear();
    }

    public boolean isJustPressed(KeyCode keyCode) {
        return justPressedKeys.contains(keyCode);
    }

    public String consumeTypedCharacters() {
        String value = typedCharacters.toString();
        typedCharacters.setLength(0);
        return value;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public boolean isMouseLeftJustClicked() {
        return mouseLeftJustClicked;
    }
}
