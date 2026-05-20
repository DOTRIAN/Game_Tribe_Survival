package buildsystem.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BuildToolbar:
 * - Model UI dung chung cho moi build item.
 * - BuildManager rebuild toolbar tu registry + inventory snapshot.
 * - Renderer/UI layer chi doc model nay, khong tu suy luan build item bang hardcode nua.
 */
public class BuildToolbar {
    private final List<BuildHotbarSlot> slots;
    private int selectedIndex;

    public BuildToolbar() {
        this.slots = new ArrayList<>();
        this.selectedIndex = 0;
    }

    public void setSlots(List<BuildHotbarSlot> slots) {
        this.slots.clear();
        if (slots != null) {
            this.slots.addAll(slots);
        }
        if (selectedIndex >= this.slots.size()) {
            selectedIndex = Math.max(0, this.slots.size() - 1);
        }
    }

    public List<BuildHotbarSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < slots.size()) {
            this.selectedIndex = selectedIndex;
        }
    }

    public BuildHotbarSlot getSelectedSlot() {
        if (selectedIndex < 0 || selectedIndex >= slots.size()) {
            return null;
        }
        return slots.get(selectedIndex);
    }
}
