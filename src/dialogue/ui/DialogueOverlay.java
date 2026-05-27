package dialogue.ui;

import dialogue.model.DialoguePage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class DialogueOverlay extends StackPane {
    private static final long PER_CHARACTER_NS = 28_000_000L;
    private static final long PAGE_FADE_NS = 220_000_000L;

    private final VBox linesBox;
    private final Label footerLabel;
    private final List<Label> lineLabels;
    private List<String> sourceLines;
    private DialoguePage currentPage;
    private int currentPageNumber;
    private int currentPageCount;
    private long pageShownAtNs;
    private boolean revealImmediately;

    public DialogueOverlay() {
        getStyleClass().add("screen-overlay");

        VBox panel = new VBox(14);
        panel.getStyleClass().addAll("menu-panel", "dialogue-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(28, 32, 24, 32));
        panel.setMaxWidth(760);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: rgba(223, 183, 108, 0.18);");

        this.linesBox = new VBox(8);
        linesBox.setFillWidth(true);
        this.footerLabel = new Label("SPACE de tiep tuc");
        footerLabel.getStyleClass().add("dialogue-footer");
        footerLabel.setMaxWidth(Double.MAX_VALUE);
        footerLabel.setTextAlignment(TextAlignment.RIGHT);
        footerLabel.setAlignment(Pos.CENTER_RIGHT);

        this.lineLabels = new ArrayList<>();
        this.sourceLines = List.of();
        this.currentPage = null;
        this.currentPageNumber = 0;
        this.currentPageCount = 0;
        this.pageShownAtNs = 0L;
        this.revealImmediately = false;

        panel.getChildren().addAll(divider, linesBox, footerLabel);
        getChildren().add(panel);
    }

    public void showPage(DialoguePage page, int pageNumber, int pageCount, long nowNs) {
        if (page == null) {
            linesBox.getChildren().clear();
            lineLabels.clear();
            sourceLines = List.of();
            currentPage = null;
            footerLabel.setText("");
            return;
        }

        if (page != currentPage || pageNumber != currentPageNumber || pageCount != currentPageCount) {
            currentPage = page;
            currentPageNumber = pageNumber;
            currentPageCount = pageCount;
            pageShownAtNs = nowNs;
            revealImmediately = false;
            rebuildLineLabels(page);
        }

        applyReveal(nowNs);

        String hint = pageNumber >= pageCount ? "SPACE de bat dau" : "SPACE de tiep tuc";
        if (!isPageFullyRevealed(nowNs)) {
            hint = "SPACE de hien het";
        }
        footerLabel.setText(hint);

        double fadeProgress = revealImmediately ? 1.0 : clamp((double) (nowNs - pageShownAtNs) / PAGE_FADE_NS);
        linesBox.setOpacity(fadeProgress);
        footerLabel.setOpacity(0.72 + (0.28 * fadeProgress));
    }

    public boolean isPageFullyRevealed(long nowNs) {
        if (currentPage == null || revealImmediately) {
            return true;
        }
        return visibleCharacterBudget(nowNs) >= totalCharacterCount();
    }

    public void revealCurrentPageImmediately() {
        revealImmediately = true;
        applyReveal(Long.MAX_VALUE);
        linesBox.setOpacity(1.0);
        footerLabel.setOpacity(1.0);
    }

    private void rebuildLineLabels(DialoguePage page) {
        linesBox.getChildren().clear();
        lineLabels.clear();
        sourceLines = page.getLines();
        for (int i = 0; i < sourceLines.size(); i++) {
            Label lineLabel = new Label("");
            lineLabel.getStyleClass().add("dialogue-line");
            lineLabel.setWrapText(true);
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabels.add(lineLabel);
            linesBox.getChildren().add(lineLabel);
        }
    }

    private void applyReveal(long nowNs) {
        int visibleCharacters = revealImmediately ? totalCharacterCount() : visibleCharacterBudget(nowNs);
        for (int i = 0; i < lineLabels.size(); i++) {
            String line = sourceLines.get(i) == null ? "" : sourceLines.get(i);
            int revealCount = Math.min(line.length(), Math.max(0, visibleCharacters));
            lineLabels.get(i).setText(line.substring(0, revealCount));
            visibleCharacters -= line.length();
        }
    }

    private int visibleCharacterBudget(long nowNs) {
        long elapsedNs = Math.max(0L, nowNs - pageShownAtNs);
        long chars = elapsedNs / PER_CHARACTER_NS;
        return (int) Math.min(Integer.MAX_VALUE, chars);
    }

    private int totalCharacterCount() {
        int total = 0;
        for (String line : sourceLines) {
            if (line != null) {
                total += line.length();
            }
        }
        return total;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
