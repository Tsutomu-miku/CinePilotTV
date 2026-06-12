package tv.cinepilot.plugins.bangumi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimalist JSON extractor for the small subset of Bangumi v0 response
 * shapes CinePilot reads. Avoids pulling in a full JSON binding library
 * so the bangumi plugin jar stays under ~30KB.
 *
 * <p>Only supports: objects with string/number fields, arrays of objects,
 * numeric ids, string names. Designed for the very specific shapes returned
 * by {@code /v0/search/subjects}, {@code /v0/episodes} and {@code /v0/me}.
 * Throws {@link IllegalArgumentException} if fed anything structurally
 * unexpected so callers can log and fall back.
 */
final class BangumiJson {

    /** Result row from a subject search. */
    static final class SubjectMatch {
        final long id;
        final String name;
        final String nameCn;
        final int year;

        SubjectMatch(long id, String name, String nameCn, int year) {
            this.id = id;
            this.name = name;
            this.nameCn = nameCn;
            this.year = year;
        }
    }

    /** Episode row returned by {@code /v0/episodes}. */
    static final class EpisodeMatch {
        final long id;
        final float sort;   // Bangumi sort field (1-based float; usually integer)
        final String name;

        EpisodeMatch(long id, float sort, String name) {
            this.id = id;
            this.sort = sort;
            this.name = name;
        }
    }

    /**
     * @return up to {@code limit} subject matches from a search-subjects response.
     */
    static List<SubjectMatch> parseSearch(String json) {
        if (json == null || json.isBlank()) return List.of();
        String data = extractArray(json, "data");
        if (data.isEmpty()) return List.of();
        List<SubjectMatch> out = new ArrayList<>();
        for (String obj : splitObjects(data)) {
            long id = extractLong(obj, "id");
            if (id <= 0) continue;
            String name = extractString(obj, "name");
            String nameCn = extractString(obj, "name_cn");
            int year = extractInt(obj, "year");
            out.add(new SubjectMatch(id, name, nameCn, year));
        }
        return out;
    }

    /**
     * @return every regular (non-SP) episode sorted by the Bangumi {@code sort}
     *     field, 1-indexed.
     */
    static List<EpisodeMatch> parseEpisodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        String data = extractArray(json, "data");
        if (data.isEmpty()) return List.of();
        List<EpisodeMatch> out = new ArrayList<>();
        for (String obj : splitObjects(data)) {
            long id = extractLong(obj, "id");
            if (id <= 0) continue;
            float sort = extractFloat(obj, "sort");
            if (sort <= 0) continue;
            String type = extractString(obj, "type");
            // type==0 is "本篇" (regular episodes); 1=SP, 2=OP/ED, etc.
            if (!"0".equals(type)) continue;
            String name = extractString(obj, "name");
            out.add(new EpisodeMatch(id, sort, name));
        }
        out.sort((a, b) -> Float.compare(a.sort, b.sort));
        return out;
    }

    static String parseUserNickname(String meJson) {
        return extractString(meJson, "nickname");
    }

    // --- primitive field extractors ------------------------------------

    static String extractString(String json, String key) {
        int idx = findKey(json, key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) return "";
        StringBuilder out = new StringBuilder();
        int i = quote + 1;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') break;
            if (c == '\\' && i < json.length()) {
                char esc = json.charAt(i++);
                switch (esc) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    default -> out.append(esc);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static long extractLong(String json, String key) {
        String raw = extractNumberRaw(json, key);
        if (raw.isEmpty()) return 0L;
        try { return Long.parseLong(raw); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    static int extractInt(String json, String key) {
        String raw = extractNumberRaw(json, key);
        if (raw.isEmpty()) return 0;
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException ignored) { return 0; }
    }

    static float extractFloat(String json, String key) {
        String raw = extractNumberRaw(json, key);
        if (raw.isEmpty()) return 0f;
        try { return Float.parseFloat(raw); }
        catch (NumberFormatException ignored) { return 0f; }
    }

    private static String extractNumberRaw(String json, String key) {
        int idx = findKey(json, key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        int start = i;
        while (i < json.length()) {
            char c = json.charAt(i);
            boolean isDigit = Character.isDigit(c) || c == '-' || c == '+' || c == '.'
                    || c == 'e' || c == 'E';
            if (!isDigit) break;
            i++;
        }
        return i == start ? "" : json.substring(start, i);
    }

    // --- structural helpers -------------------------------------------

    private static String extractArray(String json, String key) {
        int idx = findKey(json, key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int start = json.indexOf('[', colon + 1);
        if (start < 0) return "";
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start + 1, i);
                }
            }
        }
        return "";
    }

    /**
     * Split a comma-separated top-level array body into individual object
     * strings. Handles nested objects/arrays so the simple top-level split
     * doesn't cut mid-field.
     */
    private static List<String> splitObjects(String arrayBody) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{' || c == '[') {
                if (depth == 0) start = i;
                depth++;
                continue;
            }
            if (c == '}' || c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(arrayBody.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out;
    }

    private static int findKey(String json, String key) {
        String needle = String.format(Locale.ROOT, "\"%s\"", key);
        int from = 0;
        while (true) {
            int hit = json.indexOf(needle, from);
            if (hit < 0) return -1;
            // Make sure we're not inside a string value. Heuristic: walk
            // backwards from hit and count unescaped quotes.
            int quotes = 0;
            for (int j = hit - 1; j >= 0; j--) {
                char c = json.charAt(j);
                if (c == '"' && (j == 0 || json.charAt(j - 1) != '\\')) {
                    quotes++;
                }
            }
            if (quotes % 2 == 0) return hit;
            from = hit + 1;
        }
    }

    private BangumiJson() { }
}
