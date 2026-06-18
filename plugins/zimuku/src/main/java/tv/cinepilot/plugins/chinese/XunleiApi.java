package tv.cinepilot.plugins.chinese;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the Xunlei (迅雷) subtitle lookup service.
 *
 * <p>Endpoint: {@code GET http://sub.xmp.sandai.net:8000/subxl/<CID>.json}.
 *
 * <p>Xunlei's API was designed around content-id (SHA1 of 3x20KiB samples
 * from the video file). In CinePilot we don't have access to the video
 * bytes, so we use a degraded mode: we call the API with a synthetic CID
 * and use Xunlei's secondary keyword-matching response if any. In practice
 * this yields very few hits, but the call is cheap and the plugin is
 * additive (any match found is surfaced to the user alongside results
 * from shooter / zimuku), so it's worth trying.
 *
 * <p>Note: the Xunlei endpoint is HTTP-only (unencrypted). ChineseSubFinder
 * treats it the same way.
 */
final class XunleiApi {

    private static final String HOST = "http://sub.xmp.sandai.net:8000/subxl/";
    private static final int MAX_RESULTS = 10;

    static final class Match {
        final String scid;
        final String name;
        final String language;
        final int downloads;
        final String rating;
        final String author;
        final String url;

        Match(String scid, String name, String language, int downloads,
              String rating, String author, String url) {
            this.scid = scid;
            this.name = name;
            this.language = language;
            this.downloads = downloads;
            this.rating = rating;
            this.author = author;
            this.url = url;
        }
    }

    /**
     * Attempt a Xunlei lookup. Returns matches, if any. When the remote API
     * is unreachable (sandai DNS does not always resolve outside mainland
     * China) the method returns an empty list instead of throwing, so the
     * aggregator plugin can fall through silently.
     */
    List<Match> query(String syntheticCid) throws IOException {
        String url = HOST + syntheticCid + ".json";
        byte[] body;
        try {
            body = HttpOps.httpGet(url, null);
        } catch (IOException netErr) {
            // DNS failure, connect timeout, etc. — treat as "no matches"
            // instead of propagating. Users outside China will reliably
            // hit this branch.
            return List.of();
        }
        return parseResponse(body);
    }

    // ------------------------------------------------------------------

    /**
     * Xunlei JSON schema (observed from ChineseSubFinder):
     * <pre>
     * { "sublist": [ { "scid": "...", "sname": "...", "language": "简体&英语",
     *                 "rate": "...", "surl": "...", "svote": 123 }, ... ] }
     * </pre>
     *
     * <p>Outer response might also be a bare array or plain 404 text when
     * the CID has no match; we handle both gracefully.
     */
    private static List<Match> parseResponse(byte[] body) {
        List<Match> out = new ArrayList<>();
        if (body == null || body.length == 0) return out;
        String text = new String(body, StandardCharsets.UTF_8).trim();
        if (text.isEmpty() || text.charAt(0) != '{' && text.charAt(0) != '[') return out;
        Object parsed;
        try { parsed = MinJson.parse(text); }
        catch (RuntimeException ignored) { return out; }
        List<Object> sublist = null;
        if (parsed instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> arr = (List<Object>) parsed;
            sublist = arr;
        } else if (parsed instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) parsed;
            Object sl = root.get("sublist");
            if (sl instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> arr = (List<Object>) sl;
                sublist = arr;
            }
        }
        if (sublist == null) return out;
        for (Object row : sublist) {
            if (!(row instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) row;
            String scid = str(m, "scid");
            String sname = str(m, "sname");
            String language = str(m, "language");
            String surl = str(m, "surl");
            String rate = str(m, "rate");
            int svote = intVal(m.get("svote"));
            if (sname.isBlank() || surl.isBlank()) continue;
            if (!LanguageGuess.isChinese(language) && !LanguageGuess.isChinese(sname)) {
                // Skip obviously non-Chinese results — Chinese users prefer
                // their own language first; others are of marginal value.
                continue;
            }
            out.add(new Match(scid.isBlank() ? Integer.toHexString(surl.hashCode()) : scid,
                    sname, language, svote, rate, "xunlei", surl));
            if (out.size() >= MAX_RESULTS) break;
        }
        return out;
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : "";
    }

    private static int intVal(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt((String) o); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    XunleiApi() {}
}
