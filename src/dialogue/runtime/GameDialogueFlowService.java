package dialogue.runtime;

import dialogue.script.GameDialogueFactory;
import ui.Renderer;

import java.nio.file.Path;

public class GameDialogueFlowService {
    private final Renderer renderer;

    public GameDialogueFlowService(Renderer renderer) {
        this.renderer = renderer;
    }

    public DialogueRunner startOpeningIntro(String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createOpeningIntro(playerName));
    }

    public DialogueRunner startDayOneDialogue(String scriptPath, String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createDayOneDialogue(Path.of(scriptPath), playerName));
    }

    public DialogueRunner startSealGemRewardDialogue(int requiredSealGems) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createSealGemRewardDialogue(requiredSealGems));
    }

    public DialogueRunner startBossDialogue(String scriptPath, String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createBossDialogue(Path.of(scriptPath), playerName));
    }

    public DialogueRunner startBossLostDialogue(String scriptPath, String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createBossLostDialogue(Path.of(scriptPath), playerName));
    }

    public DialogueRunner startBossNinjaAwakeningDialogue(String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createBossNinjaAwakeningDialogue(playerName));
    }

    public DialogueRunner startEndingOpeningDialogue(String scriptPath, String playerName) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createEndingOpeningDialogue(Path.of(scriptPath), playerName));
    }

    public DialogueRunner startEndingEpilogueDialogue(String scriptPath) {
        prepareDialogueUi();
        return new DialogueRunner(GameDialogueFactory.createEndingEpilogueDialogue(Path.of(scriptPath)));
    }

    private void prepareDialogueUi() {
        renderer.hideToast();
        renderer.setInventoryVisible(false);
        renderer.setShopVisible(false);
        renderer.setChestVisible(false);
    }
}
