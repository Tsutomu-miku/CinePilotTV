package tv.cinepilot.plugins.chinese;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny zero-dependency JSON parser, sufficient for the small and
 * predictable payloads returned by Shooter, Xunlei, and the search page
 * metadata used by Zimuku.
 *
 * <p>Rules:
 * <ul>
 *   <li>Objects → {@code LinkedHashMap<String, Object>}</li>
 *   <li>Arrays  → {@code ArrayList<Object>}</li>
 *   <li>Strings → {@code String} (unicode escapes handled)</li>
 *   <li>Numbers → {@code Long} when integral, {@code Double} otherwise</li>
 *   <li>true/false/null → {@code Boolean} / {@code null}</li>
 * </ul>
 *
 * <p>Not a general-purpose parser. Unquoted keys are NOT accepted (all
 * Chinese subtitle APIs produce well-formed JSON with quoted keys).
 */
final class MinJson {

    /**
     * Parse a JSON document, returning Map, List, String, Number, Boolean,
     * or null. Throws on malformed input.
     */
    static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("null json");
        Cursor c = new Cursor(text);
        skipWs(c);
        Object v = readValue(c);
        skipWs(c);
        if (c.pos != c.src.length()) {
            throw new IllegalArgumentException(
                    "trailing garbage at position " + c.pos);
        }
        return v;
    }

    // --- internals ---------------------------------------------------------

    private static final class Cursor {
        final String src;
        int pos;
        Cursor(String s) { this.src = s; }
        char peek() { return src.charAt(pos); }
        boolean eof() { return pos >= src.length(); }
        char bump() { return src.charAt(pos++); }
        boolean matches(String s) {
            return src.regionMatches(false, pos, s, 0, s.length());
        }
    }

    private static void skipWs(Cursor c) {
        while (!c.eof()) {
            char ch = c.peek();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') c.pos++;
            else break;
        }
    }

    private static Object readValue(Cursor c) {
        skipWs(c);
        if (c.eof()) throw err(c, "unexpected EOF");
        char ch = c.peek();
        switch (ch) {
            case '"': return readString(c);
            case '{': return readObject(c);
            case '[': return readArray(c);
            case 't': case 'f': return readBool(c);
            case 'n': return readNull(c);
            case '-':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return readNumber(c);
            default:
                throw err(c, "unexpected character '" + ch + "'");
        }
    }

    private static Map<String, Object> readObject(Cursor c) {
        expect(c, '{');
        Map<String, Object> out = new LinkedHashMap<>();
        skipWs(c);
        if (tryConsume(c, '}')) return out;
        while (true) {
            skipWs(c);
            expect(c, '"');
            String key = readStringBody(c);
            skipWs(c);
            expect(c, ':');
            Object v = readValue(c);
            out.put(key, v);
            skipWs(c);
            if (tryConsume(c, ',')) continue;
            expect(c, '}');
            return out;
        }
    }

    private static List<Object> readArray(Cursor c) {
        expect(c, '[');
        List<Object> out = new ArrayList<>();
        skipWs(c);
        if (tryConsume(c, ']')) return out;
        while (true) {
            Object v = readValue(c);
            out.add(v);
            skipWs(c);
            if (tryConsume(c, ',')) continue;
            expect(c, ']');
            return out;
        }
    }

    private static String readString(Cursor c) {
        expect(c, '"');
        return readStringBody(c);
    }

    private static String readStringBody(Cursor c) {
        StringBuilder sb = new StringBuilder();
        while (!c.eof()) {
            char ch = c.bump();
            if (ch == '"') return sb.toString();
            if (ch == '\\') {
                if (c.eof()) throw err(c, "bad escape");
                char esc = c.bump();
                switch (esc) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case 'r':  sb.append('\r'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'u': {
                        if (c.pos + 4 > c.src.length()) throw err(c, "short \\u escape");
                        int cp = 0;
                        for (int i = 0; i < 4; i++) {
                            char hex = c.bump();
                            cp <<= 4;
                            if (hex >= '0' && hex <= '9') cp |= hex - '0';
                            else if (hex >= 'a' && hex <= 'f') cp |= 10 + hex - 'a';
                            else if (hex >= 'A' && hex <= 'F') cp |= 10 + hex - 'A';
                            else throw err(c, "bad hex in \\u escape");
                        }
                        if (Character.isHighSurrogate((char) cp)) {
                            // Low surrogate pair — handled via StringBuilder
                            // which will combine when the next unicode
                            // escape arrives.
                            sb.append((char) cp);
                        } else {
                            sb.append(Character.toChars(cp));
                        }
                        break;
                    }
                    default:
                        throw err(c, "unknown escape '\\" + esc + "'");
                }
            } else {
                sb.append(ch);
            }
        }
        throw err(c, "unterminated string");
    }

    private static Number readNumber(Cursor c) {
        int start = c.pos;
        if (c.peek() == '-') c.pos++;
        while (!c.eof()) {
            char ch = c.peek();
            if ((ch >= '0' && ch <= '9') || ch == '+'
                    || ch == '-' || ch == '.'
                    || ch == 'e' || ch == 'E') c.pos++;
            else break;
        }
        String tok = c.src.substring(start, c.pos);
        if (tok.indexOf('.') < 0 && tok.indexOf('e') < 0 && tok.indexOf('E') < 0) {
            try { return Long.parseLong(tok); }
            catch (NumberFormatException e) { /* fall through to Double */ }
        }
        try { return Double.parseDouble(tok); }
        catch (NumberFormatException e) { throw err(c, "bad number: " + tok); }
    }

    private static Boolean readBool(Cursor c) {
        if (c.matches("true")) { c.pos += 4; return Boolean.TRUE; }
        if (c.matches("false")) { c.pos += 5; return Boolean.FALSE; }
        throw err(c, "expected true/false");
    }

    private static Object readNull(Cursor c) {
        if (c.matches("null")) { c.pos += 4; return null; }
        throw err(c, "expected null");
    }

    private static boolean tryConsume(Cursor c, char ch) {
        if (!c.eof() && c.peek() == ch) { c.pos++; return true; }
        return false;
    }

    private static void expect(Cursor c, char ch) {
        if (c.eof() || c.peek() != ch) {
            throw err(c, "expected '" + ch + "'");
        }
        c.pos++;
    }

    private static RuntimeException err(Cursor c, String msg) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < c.pos && i < c.src.length(); i++) {
            if (c.src.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        return new IllegalArgumentException(
                "JSON parse error: " + msg + " at line " + line + " col " + col);
    }

    private MinJson() {}
}
