package dialogue.script;

import dialogue.io.ScriptedDialogueLoader;
import dialogue.model.DialoguePage;
import dialogue.model.DialogueScript;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public final class GameDialogueFactory {
    private static final String BOSS_NINJA_AWAKENING_SCRIPT_ID = "boss_ninja_awakening";

    private GameDialogueFactory() {
    }

    public static DialogueScript createOpeningIntro(String playerName) {
        return OpeningIntroFactory.create(playerName);
    }

    public static DialogueScript createDayOneDialogue(Path scriptPath, String playerName) {
        return ScriptedDialogueLoader.loadDayOneScript(scriptPath, playerName);
    }

    public static DialogueScript createBossDialogue(Path scriptPath, String playerName) {
        return ScriptedDialogueLoader.loadBossScript(scriptPath, playerName);
    }

    public static DialogueScript createBossLostDialogue(Path scriptPath, String playerName) {
        return ScriptedDialogueLoader.loadCustomScript(scriptPath, playerName, "boss_lost", false);
    }

    public static DialogueScript createSealGemRewardDialogue(int requiredSealGemsForBoss) {
        return new DialogueScript(
                "seal_gem_reward",
                List.of(
                        new DialoguePage(
                                "Phù Thủy",
                                "assets/phuthuy.png",
                                List.of(
                                        "Khá lắm, ngươi lại sống sót thêm một đêm nữa.",
                                        "Cầm lấy 1 viên Ngọc Phong Ấn.",
                                        "Thu thập đủ " + requiredSealGemsForBoss + " viên, rồi hãy đến cổng boss."
                                )
                        )
                )
        );
    }

    public static DialogueScript createBossNinjaAwakeningDialogue(String playerName) {
        return new DialogueScript(
                BOSS_NINJA_AWAKENING_SCRIPT_ID,
                List.of(
                        new DialoguePage(
                                "Thổ Địa",
                                "assets/phuthuy.png",
                                List.of(
                                        "Hai viên Ngọc Phong Ấn đã cộng hưởng.",
                                        "Cổng boss sẽ không chỉ mở lối, nó còn đánh thức chiến y Ninja bị phong ấn bên trong ngươi."
                                )
                        ),
                        new DialoguePage(
                                normalizePlayerName(playerName),
                                "",
                                List.of(
                                        "Vậy nên khi bước vào chiến địa ấy, ta sẽ mang một hình dạng khác..."
                                )
                        ),
                        new DialoguePage(
                                "Thổ Địa",
                                "assets/phuthuy.png",
                                List.of(
                                        "Đúng. Chỉ trong trận chiến cuối cùng, sức mạnh đó mới thực sự hiện hình.",
                                        "Hãy chuẩn bị đi. Khi chạm tới cổng phong ấn, ngươi sẽ hiểu vì sao mình được chọn."
                                )
                        )
                )
        );
    }

    public static DialogueScript createEndingOpeningDialogue(Path scriptPath, String playerName) {
        List<String> rawLines = readEndingScriptLines(scriptPath);
        List<DialoguePage> pages = new ArrayList<>();
        List<String> currentLines = new ArrayList<>();
        String currentSpeaker = "";
        String currentPortrait = "";
        for (String rawLine : rawLines) {
            String line = normalizeEndingLine(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (isEndingEpilogueMarker(line)) {
                break;
            }
            if (line.endsWith(":")) {
                flushEndingPage(pages, currentSpeaker, currentPortrait, currentLines);
                currentLines = new ArrayList<>();
                String speakerToken = line.substring(0, line.length() - 1).trim();
                currentSpeaker = resolveEndingSpeakerName(speakerToken, playerName);
                currentPortrait = resolveEndingPortrait(speakerToken);
                continue;
            }
            if (isQuotedDialogueLine(line) && !currentSpeaker.isBlank()) {
                currentLines.add(stripDialogueQuotes(line));
            }
        }
        flushEndingPage(pages, currentSpeaker, currentPortrait, currentLines);
        if (pages.isEmpty()) {
            throw new IllegalStateException("Ending opening dialogue contains no quoted pages.");
        }
        return new DialogueScript("ending_open", pages);
    }

    public static DialogueScript createEndingEpilogueDialogue(Path scriptPath) {
        List<String> rawLines = readEndingScriptLines(scriptPath);
        List<String> lines = new ArrayList<>();
        boolean collecting = false;
        for (String rawLine : rawLines) {
            String line = normalizeEndingLine(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (!collecting) {
                if (isEndingEpilogueMarker(line)) {
                    collecting = true;
                }
                continue;
            }
            if (isQuotedDialogueLine(line)) {
                lines.add(stripDialogueQuotes(line));
            }
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("Ending epilogue dialogue contains no quoted lines.");
        }
        return new DialogueScript(
                "ending_epilogue",
                List.of(new DialoguePage("Thổ Địa", "assets/phuthuy.png", lines))
        );
    }

    private static List<String> readEndingScriptLines(Path scriptPath) {
        try {
            return Files.readAllLines(scriptPath, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read ending dialogue script: " + scriptPath, exception);
        }
    }

    private static void flushEndingPage(List<DialoguePage> pages, String speaker, String portrait, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        pages.add(new DialoguePage(speaker, portrait, List.copyOf(lines)));
    }

    private static String normalizeEndingLine(String line) {
        return line == null ? "" : line.trim();
    }

    private static boolean isQuotedDialogueLine(String line) {
        String value = normalizeEndingLine(line);
        if (value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' && last == '"') || (first == '\u201C' && last == '\u201D');
    }

    private static String stripDialogueQuotes(String line) {
        String value = normalizeEndingLine(line);
        return isQuotedDialogueLine(value) ? value.substring(1, value.length() - 1).trim() : value;
    }

    private static boolean isEndingEpilogueMarker(String line) {
        String normalized = Normalizer.normalize(normalizeEndingLine(line), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toUpperCase();
        return normalized.contains("THO DIA") && normalized.contains("XUAT HIEN");
    }

    private static String resolveEndingSpeakerName(String token, String playerName) {
        String normalized = canonicalToken(token);
        if ("PLAYER".equals(normalized)) {
            return normalizePlayerName(playerName);
        }
        if ("THO DIA".equals(normalized)) {
            return "Thổ Địa";
        }
        return token == null ? "" : token.trim();
    }

    private static String resolveEndingPortrait(String token) {
        return "THO DIA".equals(canonicalToken(token)) ? "assets/phuthuy.png" : "";
    }

    private static String canonicalToken(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toUpperCase();
    }

    private static String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Player";
        }
        return playerName.trim();
    }
}
