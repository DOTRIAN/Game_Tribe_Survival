package dialogue.ui;

import dialogue.model.DialoguePage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DialogueOverlay extends StackPane {
    private static final long PER_CHARACTER_NS = 28_000_000L;
    private static final long PAGE_FADE_NS = 220_000_000L;

    private final Label speakerLabel;
    private final VBox linesBox;
    private final Label footerLabel;
    private final ImageView portraitView;
    private final StackPane portraitFrame;
    private final HBox contentRow;
    private final VBox wrapper;
    private final VBox panel;
    private final Pane panelDecor;
    private final List<Label> lineLabels;
    private final Map<String, Image> portraitCache;
    private List<String> sourceLines;
    private DialoguePage currentPage;
    private int currentPageNumber;
    private int currentPageCount;
    private long pageShownAtNs;
    private boolean revealImmediately;

    public DialogueOverlay() {
        getStyleClass().add("screen-overlay");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(18, 28, 18, 28));

        this.wrapper = new VBox(6);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(1140);

        this.speakerLabel = new Label("");
        speakerLabel.getStyleClass().add("dialogue-nameplate");
        speakerLabel.setVisible(false);
        speakerLabel.setManaged(false);

        this.panel = new VBox(12);
        panel.getStyleClass().add("dialogue-story-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxWidth(1140);
        panel.setPrefWidth(1040);
        panel.setMinHeight(280);
        panel.setPadding(new Insets(28, 30, 24, 30));

        this.linesBox = new VBox(8);
        linesBox.setFillWidth(true);

        this.portraitView = new ImageView();
        portraitView.setPreserveRatio(true);
        portraitView.setFitWidth(78);
        portraitView.setFitHeight(78);
        portraitView.setSmooth(false);

        this.portraitFrame = new StackPane(portraitView);
        portraitFrame.getStyleClass().add("dialogue-portrait-frame");
        portraitFrame.setPrefSize(92, 92);
        portraitFrame.setMinSize(92, 92);
        portraitFrame.setMaxSize(92, 92);
        portraitFrame.setVisible(false);
        portraitFrame.setManaged(false);

        this.contentRow = new HBox(16, linesBox, portraitFrame);
        contentRow.setAlignment(Pos.CENTER_LEFT);

        this.footerLabel = new Label("SPACE de tiep tuc");
        footerLabel.getStyleClass().add("dialogue-footer");
        footerLabel.setMaxWidth(Double.MAX_VALUE);
        footerLabel.setTextAlignment(TextAlignment.RIGHT);
        footerLabel.setAlignment(Pos.CENTER_RIGHT);

        this.panelDecor = buildPanelDecor();

        this.lineLabels = new ArrayList<>();
        this.portraitCache = new LinkedHashMap<>();
        this.sourceLines = List.of();
        this.currentPage = null;
        this.currentPageNumber = 0;
        this.currentPageCount = 0;
        this.pageShownAtNs = 0L;
        this.revealImmediately = false;

        panel.getChildren().addAll(contentRow, footerLabel);
        StackPane panelStack = new StackPane(panelDecor, panel);
        wrapper.getChildren().addAll(speakerLabel, panelStack);
        getChildren().add(wrapper);
    }

    public void showPage(DialoguePage page, int pageNumber, int pageCount, long nowNs) {
        if (page == null) {
            linesBox.getChildren().clear();
            lineLabels.clear();
            sourceLines = List.of();
            currentPage = null;
            footerLabel.setText("");
            speakerLabel.setText("");
            speakerLabel.setVisible(false);
            speakerLabel.setManaged(false);
            portraitFrame.setVisible(false);
            portraitFrame.setManaged(false);
            portraitView.setImage(null);
            return;
        }

        if (page != currentPage || pageNumber != currentPageNumber || pageCount != currentPageCount) {
            currentPage = page;
            currentPageNumber = pageNumber;
            currentPageCount = pageCount;
            pageShownAtNs = nowNs;
            revealImmediately = false;
            rebuildLineLabels(page);
            updateSpeakerUi(page);
            updateLayoutMode(page);
        }

        applyReveal(nowNs);

        String hint = pageNumber >= pageCount ? "SPACE de bat dau" : "SPACE de tiep tuc";
        if (!isPageFullyRevealed(nowNs)) {
            hint = "SPACE de hien het";
        }
        footerLabel.setText(hint);

        double fadeProgress = revealImmediately ? 1.0 : clamp((double) (nowNs - pageShownAtNs) / PAGE_FADE_NS);
        linesBox.setOpacity(fadeProgress);
        speakerLabel.setOpacity(fadeProgress);
        footerLabel.setOpacity(0.72 + (0.28 * fadeProgress));
        portraitFrame.setOpacity(portraitFrame.isVisible() ? 1.0 : 0.0);
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
        speakerLabel.setOpacity(1.0);
        footerLabel.setOpacity(1.0);
        portraitFrame.setOpacity(portraitFrame.isVisible() ? 1.0 : 0.0);
    }

    private void rebuildLineLabels(DialoguePage page) {
        linesBox.getChildren().clear();
        lineLabels.clear();
        sourceLines = page.getLines();
        for (String ignored : sourceLines) {
            Label lineLabel = new Label("");
            lineLabel.getStyleClass().add("dialogue-line");
            lineLabel.setWrapText(true);
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabels.add(lineLabel);
            linesBox.getChildren().add(lineLabel);
        }
    }

    private void updateSpeakerUi(DialoguePage page) {
        boolean hasSpeaker = page.hasSpeaker();
        speakerLabel.setText(page.getSpeakerName());
        speakerLabel.setVisible(hasSpeaker);
        speakerLabel.setManaged(hasSpeaker);

        Image portrait = resolvePortrait(page.getPortraitPath());
        boolean hasPortrait = portrait != null;
        portraitView.setImage(portrait);
        portraitFrame.setVisible(hasPortrait);
        portraitFrame.setManaged(hasPortrait);
    }

    private void updateLayoutMode(DialoguePage page) {
        boolean storyMode = !page.hasSpeaker();
        if (storyMode) {
            setAlignment(Pos.CENTER);
            setPadding(new Insets(28, 36, 28, 36));
            wrapper.setAlignment(Pos.CENTER);
            wrapper.setMaxWidth(1140);
            panel.setMaxWidth(1140);
            panel.setPrefWidth(1040);
            panel.setMinHeight(280);
            panel.setPadding(new Insets(28, 30, 24, 30));
        } else {
            setAlignment(Pos.BOTTOM_CENTER);
            setPadding(new Insets(0, 28, 36, 28));
            wrapper.setAlignment(Pos.BOTTOM_LEFT);
            wrapper.setMaxWidth(980);
            panel.setMaxWidth(980);
            panel.setPrefWidth(900);
            panel.setMinHeight(170);
            panel.setPadding(new Insets(18, 20, 16, 20));
        }
    }

    private Image resolvePortrait(String portraitPath) {
        if (portraitPath == null || portraitPath.isBlank()) {
            return null;
        }
        Image cached = portraitCache.get(portraitPath);
        if (cached != null) {
            return cached;
        }
        Path path = Path.of(portraitPath);
        if (!Files.exists(path)) {
            return null;
        }
        Image image = new Image(path.toUri().toString(), false);
        if (image.isError()) {
            return null;
        }
        portraitCache.put(portraitPath, image);
        return image;
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

    private Pane buildPanelDecor() {
        AnchorPane decor = new AnchorPane();
        decor.setMouseTransparent(true);

        Pane topLeft = corner();
        Pane topRight = corner();
        Pane bottomLeft = corner();
        Pane bottomRight = corner();

        AnchorPane.setTopAnchor(topLeft, -2.0);
        AnchorPane.setLeftAnchor(topLeft, -2.0);
        AnchorPane.setTopAnchor(topRight, -2.0);
        AnchorPane.setRightAnchor(topRight, -2.0);
        AnchorPane.setBottomAnchor(bottomLeft, -2.0);
        AnchorPane.setLeftAnchor(bottomLeft, -2.0);
        AnchorPane.setBottomAnchor(bottomRight, -2.0);
        AnchorPane.setRightAnchor(bottomRight, -2.0);

        decor.getChildren().addAll(topLeft, topRight, bottomLeft, bottomRight);
        return decor;
    }

    private Pane corner() {
        Pane pane = new Pane();
        pane.getStyleClass().add("dialogue-corner");
        pane.setPrefSize(12, 12);
        pane.setMinSize(12, 12);
        pane.setMaxSize(12, 12);
        return pane;
    }
}
