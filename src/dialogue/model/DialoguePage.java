package dialogue.model;

import java.util.ArrayList;
import java.util.List;

public final class DialoguePage {
    private final String sectionLabel;
    private final String title;
    private final List<String> lines;

    public DialoguePage(String sectionLabel, String title, List<String> lines) {
        this.sectionLabel = sectionLabel == null ? "" : sectionLabel.trim();
        this.title = title == null ? "" : title.trim();
        this.lines = List.copyOf(new ArrayList<>(lines == null ? List.of() : lines));
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLines() {
        return lines;
    }
}
