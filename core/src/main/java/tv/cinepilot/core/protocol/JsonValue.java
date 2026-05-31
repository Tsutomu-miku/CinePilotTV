package tv.cinepilot.core.protocol;

import java.util.List;
import java.util.Map;

final class JsonValue {
    private JsonValue() {
    }

    static Map<String, Object> object(String json) {
        Object value = new Parser(json).parse();
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        throw new IllegalArgumentException("JSON root must be an object");
    }

    static List<Object> array(String json) {
        Object value = new Parser(json).parse();
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("JSON root must be an array");
    }

    static String string(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof String string ? string : null;
    }

    static boolean bool(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof Boolean bool && bool;
    }

    static Map<String, Object> childObject(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return Map.of();
    }

    static List<Object> array(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            if (json == null || json.isBlank()) {
                throw new IllegalArgumentException("json is required");
            }
            this.json = json;
        }

        Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw error("Unexpected end of JSON");
            }
            char current = json.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character: " + current);
                }
            };
        }

        private Map<String, Object> parseObject() {
            java.util.LinkedHashMap<String, Object> object = new java.util.LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            java.util.ArrayList<Object> values = new java.util.ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char current = json.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    builder.append(parseEscape());
                } else {
                    builder.append(current);
                }
            }
            throw error("Unterminated string");
        }

        private char parseEscape() {
            if (index >= json.length()) {
                throw error("Unterminated escape");
            }
            char escaped = json.charAt(index++);
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> parseUnicode();
                default -> throw error("Unsupported escape: " + escaped);
            };
        }

        private char parseUnicode() {
            if (index + 4 > json.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = json.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            String value = json.substring(start, index);
            return decimal ? Double.parseDouble(value) : Long.parseLong(value);
        }

        private Object literal(String expected, Object value) {
            if (!json.startsWith(expected, index)) {
                throw error("Expected " + expected);
            }
            index += expected.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw error("Expected " + expected);
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + index);
        }
    }
}
