package tv.cinepilot.plugins.chinese;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tv.cinepilot.plugin.spi.SubtitleSearchResult;

/**
 * Client for the Shooter (射手网) public subtitle search API.
 *
 * <p>Endpoint: {@code POST https://www.shooter.cn/api/subapi.php}.
 *
 * <p>Shooter's API is designed for file-based lookups using a 4-sample
 * MD5 hash. Since CinePilot does not have access to the raw video bytes
 * (it streams from the media server), we fall back to keyword-based
 * search using the video filename / title. This yields lower hit rates
 * but still works for popular Chinese releases.
 *
 * <p>Download links returned are direct CDN URLs, no auth required.
 */
final class ShooterApi {

    private static final String ENDPOINT = "https://www.shooter.cn/api/subapi.php";
    private static final int MAX_RESULTS = 20;

    /** One parsed search result from shooter's API. */
    static final class Match {
        final String id;           // md5(downloadLink)
        final String name;         // display name
        final String language;     // inferred
        final int downloads;       // synthetic score (first results rank higher)
        final String author;
        final String rating;
        final String downloadLink;

        Match(String id, String name, String language, int downloads,
              String author, String rating, String downloadLink) {
            this.id = id;
            this.name = name;
            this.language = language;
            this.downloads = downloads;
            this.author = author;
            this.rating = rating;
            this.downloadLink = downloadLink;
        }
    }

    List<Match> searchByKeyword(String keyword) throws IOException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("pathinfo", keyword);
        form.put("format", "json");
        form.put("lang", "Chn");
        form.put("filehash", zeroHashes());
        byte[] body = HttpOps.httpPostForm(ENDPOINT, form, headers());
        return parseResponse(body);
    }

    private static Map<String, String> headers() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Referer", "https://www.shooter.cn/");
        h.put("Origin", "https://www.shooter.cn");
        return h;
    }

    private static String zeroHashes() {
        return "00000000000000000000000000000000"
                + ";00000000000000000000000000000000"
                + ";00000000000000000000000000000000"
                + ";00000000000000000000000000000000";
    }

    // ------------------------------------------------------------------

    /**
     * Parse the JSON array returned by shooter. Schema:
     * <pre>
     * [ { "Desc": "...", "Delay": 0,
     *     "Files": [ { "Ext": "srt", "Link": "https://..." }, ... ] } ]
     * </pre>
     */
    private static List<Match> parseResponse(byte[] body) {
        List<Match> out = new ArrayList<>();
        if (body == null || body.length == 0) return out;
        String text = new String(body, StandardCharsets.UTF_8).trim();
        if (text.isEmpty() || text.equals("0") || text.equalsIgnoreCase("null")
                || text.equals("-1") || text.charAt(0) != '[') return out;
        Object parsed = MinJson.parse(text);
        if (!(parsed instanceof List)) return out;
        @SuppressWarnings("unchecked")
        List<Object> arr = (List<Object>) parsed;
        int position = 0;
        for (Object row : arr) {
            if (!(row instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) row;
            String desc = stringOr(obj.get("Desc"), "");
            Object filesObj = obj.get("Files");
            if (!(filesObj instanceof List)) continue;
            @SuppressWarnings("unchecked")
            List<Object> files = (List<Object>) filesObj;
            for (Object f : files) {
                if (!(f instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) f;
                String ext = stringOr(entry.get("Ext"), "");
                String link = stringOr(entry.get("Link"), "");
                if (link.isBlank()) continue;
                String id = md5Hex(link);
                String language = LanguageGuess.from(desc, filenameFromUrl(link));
                String name = buildDisplayName(desc, filenameFromUrl(link), ext);
                // Shooter does not expose real download counts; report 0 so the UI can
                // suppress the "N 次下载" line instead of showing misleading numbers.
                // A separate position-based rank is still used by the plugin for sort order.
                int syntheticRank = Math.max(0, (arr.size() - position) * 100);
                out.add(new Match(id, name, language, syntheticRank, "shooter.cn", "", link));
                if (out.size() >= MAX_RESULTS) return out;
            }
            position++;
        }
        return out;
    }

    private static String stringOr(Object o, String def) {
        return o instanceof String ? (String) o : def;
    }

    private static String buildDisplayName(String desc, String fileName, String ext) {
        String base = (fileName == null || fileName.isBlank()) ? desc : fileName;
        if (base.isBlank()) base = "射手字幕";
        if (!ext.isBlank()) {
            String lower = base.toLowerCase();
            if (lower.endsWith("." + ext.toLowerCase())) {
                base = base.substring(0, base.length() - ext.length() - 1);
            }
        }
        if (!desc.isBlank() && !desc.equals(base)) {
            return base + " — " + desc;
        }
        return base;
    }

    private static String filenameFromUrl(String url) {
        if (url == null) return "";
        int slash = url.lastIndexOf('/');
        if (slash < 0) return url;
        int q = url.indexOf('?', slash);
        return q < 0 ? url.substring(slash + 1) : url.substring(slash + 1, q);
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new RuntimeException(impossible);
        }
    }

    // Reference SubtitleSearchResult.Format (imported above) so the symbol
    // stays resolved if callers add format detection here.
    @SuppressWarnings("unused")
    private static SubtitleSearchResult.Format fmtRef() { return null; }

    ShooterApi() {}
}
