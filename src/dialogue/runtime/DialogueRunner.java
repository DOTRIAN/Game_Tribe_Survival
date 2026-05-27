package dialogue.runtime;

import dialogue.model.DialoguePage;
import dialogue.model.DialogueScript;

public final class DialogueRunner {
    private final DialogueScript script;
    private int currentPageIndex;
    private boolean complete;

    public DialogueRunner(DialogueScript script) {
        if (script == null) {
            throw new IllegalArgumentException("Dialogue script must not be null.");
        }
        this.script = script;
        this.currentPageIndex = 0;
        this.complete = false;
    }

    public DialogueScript getScript() {
        return script;
    }

    public DialoguePage getCurrentPage() {
        if (complete) {
            return null;
        }
        return script.getPages().get(currentPageIndex);
    }

    public int getCurrentPageNumber() {
        return Math.min(currentPageIndex + 1, getPageCount());
    }

    public int getPageCount() {
        return script.getPages().size();
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean advance() {
        if (complete) {
            return false;
        }
        if (currentPageIndex >= script.getPages().size() - 1) {
            complete = true;
            return false;
        }
        currentPageIndex++;
        return true;
    }
}
