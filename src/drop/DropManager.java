package drop;

public final class DropManager {
    private DropManager() {
    }

    public static void preloadAll() {
        DropItemType.preloadAll();
        CollectibleDrop.preloadAssets();
    }
}
