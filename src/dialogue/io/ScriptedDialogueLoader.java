package dialogue.io;

import dialogue.model.DialoguePage;
import dialogue.model.DialogueScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public final class ScriptedDialogueLoader {
    private static final String DAY_ONE_SCRIPT_ID = "day1_dialogue";
    private static final String BOSS_SCRIPT_ID = "boss_intro";

    private ScriptedDialogueLoader() {
    }

    public static DialogueScript loadDayOneScript(Path path, String playerName) {
        return loadScript(path, playerName, DAY_ONE_SCRIPT_ID, false);
    }

    public static DialogueScript loadBossScript(Path path, String playerName) {
        return loadScript(path, playerName, BOSS_SCRIPT_ID, true);
    }

    public static DialogueScript loadCustomScript(Path path,
                                                  String playerName,
                                                  String scriptId,
                                                  boolean ignoreBracketDirectives) {
        return loadScript(path, playerName, scriptId, ignoreBracketDirectives);
    }

    private static DialogueScript loadScript(Path path,
                                             String playerName,
                                             String scriptId,
                                             boolean ignoreBracketDirectives) {
        if (path == null) {
            throw new IllegalArgumentException("Script path must not be null.");
        }
        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read dialogue script: " + path, exception);
        }

        List<DialoguePage> pages = new ArrayList<>();
        List<String> currentLines = new ArrayList<>();
        String currentSpeaker = "";
        String currentPortrait = "";
        String pendingPortrait = "";

        for (String rawLine : rawLines) {
            String line = normalizeLine(rawLine);
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                if (ignoreBracketDirectives) {
                    continue;
                }
                String normalizedBracket = canonicalToken(line);
                if (normalizedBracket.contains("HIEN ANH")) {
                    pendingPortrait = resolvePortraitPath(extractImageSpeakerToken(line));
                }
                continue;
            }

            if (line.endsWith(":")) {
                flushPage(pages, currentSpeaker, currentPortrait, currentLines);
                currentLines = new ArrayList<>();

                String token = line.substring(0, line.length() - 1).trim();
                currentSpeaker = resolveSpeakerName(token, playerName);
                currentPortrait = ignoreBracketDirectives ? "" : resolveSpeakerPortrait(token, pendingPortrait);
                pendingPortrait = "";
                continue;
            }

            String cleaned = stripQuotes(line);
            if (!cleaned.isBlank()) {
                currentLines.add(cleaned);
            }
        }

        flushPage(pages, currentSpeaker, currentPortrait, currentLines);
        if (pages.isEmpty()) {
            throw new IllegalStateException("Dialogue script contains no pages: " + path);
        }
        return new DialogueScript(scriptId, pages);
    }

    private static void flushPage(List<DialoguePage> pages, String speaker, String portrait, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        pages.add(new DialoguePage(speaker, portrait, lines));
    }

    private static String normalizeLine(String line) {
        return line == null ? "" : line.trim();
    }

    private static String extractImageSpeakerToken(String line) {
        String content = line.substring(1, line.length() - 1).trim();
        String[] parts = content.split("\\s+", 3);
        if (parts.length >= 3) {
            return parts[2].trim();
        }
        return content;
    }

    private static String resolveSpeakerName(String token, String playerName) {
        return switch (canonicalToken(token)) {
            case "PLAYER" -> normalizePlayerName(playerName);
            case "PHU THUY", "THO DIA" -> "Thổ Địa";
            default -> token == null ? "" : token.trim();
        };
    }

    private static String resolvePortraitPath(String token) {
        return switch (canonicalToken(token)) {
            case "PHU THUY", "THO DIA" -> "assets/phuthuy.png";
            default -> "";
        };
    }

    private static String resolveSpeakerPortrait(String token, String pendingPortrait) {
        String speakerKey = canonicalToken(token);
        if ("PLAYER".equals(speakerKey)) {
            return "";
        }
        if (pendingPortrait != null && !pendingPortrait.isBlank()) {
            return pendingPortrait;
        }
        return resolvePortraitPath(token);
    }

    private static String stripQuotes(String line) {
        String value = line == null ? "" : line.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\u201C' && last == '\u201D')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static String canonicalToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
        normalized = normalized.replaceAll("[\\[\\]:\"\u201C\u201D]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.toUpperCase();
    }

    private static String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Player";
        }
        return playerName.trim();
    }
}
