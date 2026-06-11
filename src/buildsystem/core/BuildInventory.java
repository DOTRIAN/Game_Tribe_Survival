package buildsystem.core;

import java.util.Map;

/**
 * BuildInventory:
 * - Abstraction nho de BuildManager co the doc/tru vat pham xay dung.
 * - Khong troi BuildSystem vao 1 class inventory cu the cua game.
 */
public interface BuildInventory {
    int getAmount(String itemId);

    boolean consumeItem(String itemId, int amount);

    Map<String, Integer> snapshot();
}
