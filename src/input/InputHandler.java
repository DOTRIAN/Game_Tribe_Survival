package input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.HashSet; // 1 class cụ thể để cài đặt set
import java.util.Set; // 1 kiểu tập hợp nơi luu nhiều phần tử không trùng lặp



public class InputHandler {

    private final Set<KeyCode> pressedKeys;
    // Sett<Keycode>: 1 tapj hop cac phan tu kieu Keycode
    //pressedKeys: ten bien, cac phim hien dang duoc nhan
    //Neu chi de la Keycode thi chi nhan duoc 1 phim tai 1 thoi diem
    private final Set<KeyCode> justPressedKeys;

    public InputHandler() {
        this.pressedKeys = new HashSet<>();
        this.justPressedKeys = new HashSet<>();
    }

    public void attach(Scene scene) {   // gan InpuHandler vao Scene
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (!pressedKeys.contains(code)) {
                justPressedKeys.add(code);   // chỉ thêm khi vừa nhấn xuống lần đầu
            }
            pressedKeys.add(code);
        });

        scene.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            pressedKeys.remove(code);
            justPressedKeys.remove(code);
        });
        // scene.setOnKeyPressed(...) : Dky 1 ham xu ly khi phim dc nhan xuong(hoac khi userr nhan phim thi hay xu ly doan code nay)
        //event la doi tuong su kien ban phim
        // chua tthong tin ve lan bam phim do
        // event.geCode() lấy ra mã phím vừa nhấn
        //pressedKeys.add(..) : thêm phims đó vào tập hợp pressedKeys
        //scene.setOnKeyReleased(...) đky hàm xư lý khi phím được thả ra
        //event.getcode() lấy mã phím vừa thả
        //pressedKeys.remove: xóa phím đó khỏi tập hợp
    }

    public void update() {
        // Skeleton: buoc sau doc pressedKeys de dieu khien player.
        justPressedKeys.clear(); // mỗi frame reset "vừa nhấn"
    }

    public boolean isPressed(KeyCode keyCode) {
        return pressedKeys.contains(keyCode);  // ktra trong tập hợp pressedKeys có phần tử đó ko
    }  // hàm check xem phím  đó có đang được nhấn ko

    public void clearPressedKeys() {
        pressedKeys.clear();
    }
    public boolean isJustPressed(KeyCode keyCode) {
        return justPressedKeys.contains(keyCode);
    }
}
