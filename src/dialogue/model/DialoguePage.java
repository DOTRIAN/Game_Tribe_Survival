package dialogue.model;

import java.util.ArrayList;
import java.util.List;

public final class DialoguePage {
    private final String speakerName;
    private final String portraitPath;
    private final List<String> lines;

    public DialoguePage(String speakerName, String portraitPath, List<String> lines) {
        this.speakerName = speakerName == null ? "" : speakerName.trim();
        this.portraitPath = portraitPath == null ? "" : portraitPath.trim();
        this.lines = List.copyOf(new ArrayList<>(lines == null ? List.of() : lines));
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public String getPortraitPath() {
        return portraitPath;
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean hasSpeaker() {
        return !speakerName.isBlank();
    }

    public boolean hasPortrait() {
        return !portraitPath.isBlank();
    }
}
