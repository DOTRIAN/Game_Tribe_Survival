package system.level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser/writer de tranh phu thuoc thu vien ngoai.
 * Ho tro: object, array, string, number, boolean, null.
 *
 * Ly do ton tai:
 * - Repo hien tai khong co dependency JSON ro rang trong workspace
 * - Level system/save system chi can parser nho, de doc va debug
 * - Giu game build don gian hon trong giai doan do an/MVP
 *
 * Lien he voi hit/combat:
 * - Duoc dung gian tiep khi luu ket qua level sau combat/resource gathering
 *   (thong qua LevelManager.saveProgress()).
 */
public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String json) {
        return new Parser(stripBom(json)).parseValue();
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }

        if (value instanceof String stringValue) {
            builder.append('"').append(escape(stringValue)).append('"');
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }

        if (value instanceof Map<?, ?> mapValue) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, item);
            }
            builder.append(']');
            return;
        }

        builder.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static String escape(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String stripBom(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        if (raw.charAt(0) == '\uFEFF') {
            return raw.substring(1);
        }
        return raw;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
            this.index = 0;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                return null;
            }

            char ch = text.charAt(index);
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == 't' || ch == 'f') {
                return parseBoolean();
            }
            if (ch == 'n') {
                return parseNull();
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            index++;
            skipWhitespace();

            if (peek('}')) {
                index++;
                return object;
            }

            while (index < text.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }

            return object;
        }

        private List<Object> parseArray() {
            List<Object> array = new ArrayList<>();
            index++;
            skipWhitespace();

            if (peek(']')) {
                index++;
                return array;
            }

            while (index < text.length()) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
            }

            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();

            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    break;
                }
                if (ch == '\\' && index < text.length()) {
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"':
                            builder.append('"');
                            break;
                        case '\\':
                            builder.append('\\');
                            break;
                        case '/':
                            builder.append('/');
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            if (index + 4 <= text.length()) {
                                String hex = text.substring(index, index + 4);
                                builder.append((char) Integer.parseInt(hex, 16));
                                index += 4;
                            }
                            break;
                        default:
                            builder.append(escaped);
                            break;
                    }
                    continue;
                }
                builder.append(ch);
            }

            return builder.toString();
        }

        private Boolean parseBoolean() {
            if (text.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at index " + index);
        }

        private Object parseNull() {
            if (text.startsWith("null", index)) {
                index += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null at index " + index);
        }

        private Number parseNumber() {
            int start = index;
            while (index < text.length()) {
                char ch = text.charAt(index);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                    index++;
                    continue;
                }
                break;
            }

            String raw = text.substring(start, index);
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                    continue;
                }
                break;
            }
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }
    }
}
