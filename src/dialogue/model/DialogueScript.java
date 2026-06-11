package dialogue.model;

import java.util.ArrayList;
import java.util.List;

public final class DialogueScript {
    private final String id;
    private final List<DialoguePage> pages;

    public DialogueScript(String id, List<DialoguePage> pages) {
        this.id = id == null ? "" : id.trim();
        this.pages = List.copyOf(new ArrayList<>(pages == null ? List.of() : pages));
        if (this.pages.isEmpty()) {
            throw new IllegalArgumentException("Dialogue script must contain at least one page.");
        }
    }

    public String getId() {
        return id;
    }

    public List<DialoguePage> getPages() {
        return pages;
    }
}
