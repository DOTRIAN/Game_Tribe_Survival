package inventory;

import ui.HotbarItemStack;
import ui.ItemUiMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class HotbarService {
    private final List<String> priorityItemIds;
    private final List<HotbarItemStack> items;
    private int selectedIndex;

    public HotbarService(List<String> priorityItemIds) {
        this.priorityItemIds = priorityItemIds == null ? List.of() : List.copyOf(priorityItemIds);
        this.items = new ArrayList<>();
        this.selectedIndex = 0;
    }

    public void setSelectedIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 9) {
            return;
        }
        selectedIndex = slotIndex;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItemId() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            return "";
        }
        HotbarItemStack stack = items.get(selectedIndex);
        return stack == null ? "" : stack.getItemId();
    }

    public List<HotbarItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void refresh(Map<String, Integer> inventorySnapshot,
                        Function<String, ItemUiMeta> itemMetaResolver,
                        Predicate<String> buildItemPredicate) {
        int previousSelectedIndex = selectedIndex;
        String previouslySelectedItemId = getSelectedItemId();

        items.clear();
        Map<String, Integer> snapshot = inventorySnapshot == null ? Map.of() : inventorySnapshot;
        for (String itemId : priorityItemIds) {
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            int amount = snapshot.getOrDefault(itemId, 0);
            if (amount <= 0) {
                continue;
            }
            ItemUiMeta meta = itemMetaResolver == null ? null : itemMetaResolver.apply(itemId);
            boolean buildItem = buildItemPredicate != null && buildItemPredicate.test(itemId);
            items.add(new HotbarItemStack(itemId, amount, meta, buildItem));
            if (items.size() >= 9) {
                break;
            }
        }

        if (previouslySelectedItemId != null && !previouslySelectedItemId.isBlank()) {
            int matchedIndex = findSlotIndexByItemId(previouslySelectedItemId);
            if (matchedIndex >= 0) {
                selectedIndex = matchedIndex;
            } else {
                selectedIndex = previousSelectedIndex;
            }
        } else {
            selectedIndex = previousSelectedIndex;
        }

        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        if (selectedIndex >= 9) {
            selectedIndex = 8;
        }
    }

    private int findSlotIndexByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            HotbarItemStack stack = items.get(i);
            if (stack != null && itemId.equals(stack.getItemId())) {
                return i;
            }
        }
        return -1;
    }
}
