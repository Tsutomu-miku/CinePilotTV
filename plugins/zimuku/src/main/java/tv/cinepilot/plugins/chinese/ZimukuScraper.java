package tv.cinepilot.plugins.chinese;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * HTML scraping client for Zimuku (字幕库, zimuku.org).
 *
 * <p>Zimuku does not expose a public API — its search page is regular HTML
 * and downloads are exposed via signed CDN links. ChineseSubFinder used to
 * maintain a zimuku scraper that relied on headless Chrome, but in recent
 * years Zimuku has softened the anti-bot wall for non-browser traffic with
 * a plausible User-Agent. We therefore use a plain HTTP client with
 * randomized desktop browser UAs and referrer spoofing, and fall back to
 * "TEMPORARILY_UNAVAILABLE" via {@code lastError} on the plugin store if
 * we repeatedly detect blocking (see {@link ChineseSubPlugin}).
 *
 * <p>HTML parsing is deliberately naive (indexOf / substring, no
 * dependencies). Zimuku's layout has been remarkably stable for the past
 * 7 years, so this approach is robust enough.
 */
final class ZimukuScraper {

    private static final String BASE = "https://www.zimuku.org";
    private static final int MAX_RESULTS = 20;
    private static final String ROW_OPEN = "class=\"persub\"";
    // Zimuku ends one persub row with a pair of close tags; pick a stable marker.
    private static final String ROW_CLOSE = "</div>\n<div class=\"subtitlenav\">";

    static final class Hit {
        final String detailId;     // numeric id part of /detail/<id>.html
        final String name;         // display name
        final String language;     // e.g. "简繁英双语"
        final int downloads;       // parsed from <span class="dl-num">
        final String rating;       // ★ count string
        final String author;       // uploader name

        Hit(String detailId, String name, String language, int downloads,
            String rating, String author) {
            this.detailId = detailId;
            this.name = name;
            this.language = language;
            this.downloads = downloads;
            this.rating = rating;
            this.author = author;
        }
    }

    /** Run a keyword search. */
    List<Hit> search(String keyword) throws IOException {
        String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        String url = BASE + "/search?q=" + encoded;
        Map<String, String> headers = Map.of(
                "Referer", BASE + "/",
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        byte[] body = HttpOps.httpGet(url, headers);
        String html = HttpOps.decodeText(body, "text/html");
        return parseSearch(html);
    }

    /**
     * Returns a direct CDN download URL for the given detail id. Zimuku's
     * /down/<id> endpoint is usually a 302 redirect to a CDN, which
     * HttpURLConnection's automatic following resolves into the final body.
     * Here we want the *bytes* of the actual download (zip/rar/srt etc.)
     * so we just expose a convenience method that fetches them.
     */
    byte[] download(String detailId) throws IOException {
        if (detailId == null || detailId.isBlank()) {
            throw new IOException("invalid zimuku detail id");
        }
        String url = BASE + "/down/" + detailId;
        Map<String, String> headers = Map.of(
                "Referer", BASE + "/detail/" + detailId + ".html");
        return HttpOps.httpGet(url, headers);
    }

    // --- HTML parsing (deliberately dependency-free) -----------------------

    static List<Hit> parseSearch(String html) {
        List<Hit> out = new ArrayList<>();
        if (html == null || html.isEmpty()) return out;
        // Zimuku lists result rows in a single <div class="subs box"> with
        // repeated <div class="persub" id="sub_12345"> items.
        int pos = 0;
        while (pos < html.length()) {
            int start = html.indexOf(ROW_OPEN, pos);
            if (start < 0) break;
            // Walk backwards to find the <div that opens this row (we want
            // the id attribute too).
            int openDiv = html.lastIndexOf("<div", start);
            if (openDiv < 0) openDiv = start;
            int end = html.indexOf(ROW_CLOSE, start);
            if (end < 0) {
                // Fallback: find the next persub div or end of doc.
                int next = html.indexOf(ROW_OPEN, start + 10);
                end = next > 0 ? next : html.length();
            }
            String row = html.substring(openDiv, end);
            Hit hit = parseRow(row);
            if (hit != null) out.add(hit);
            pos = end;
            if (out.size() >= MAX_RESULTS * 2) break; // sort then trim
        }
        out.sort(Comparator.comparingInt((Hit h) -> h.downloads).reversed());
        if (out.size() > MAX_RESULTS) out = new ArrayList<>(out.subList(0, MAX_RESULTS));
        return out;
    }

    private static Hit parseRow(String row) {
        // id="sub_<number>" or href="/detail/<id>.html"
        String id = extractId(row);
        if (id == null) return null;
        // Name is inside the first <a> ... </a> of the .tt div.
        String ttInner = firstDivClassInner(row, "tt");
        String anchor = firstAnchor(ttInner);
        String name = stripTags(anchor).trim();
        if (name.isEmpty()) {
            // fallback: .subtitle_box has the movie title, also acceptable
            name = stripTags(firstDivClassInner(row, "sub_con")).trim();
        }
        if (name.isEmpty()) return null;

        String subInfo = firstDivClassInner(row, "subInfo");
        String language = stripTags(firstSpanClass(subInfo, "lang")).trim();
        String rate = stripTags(firstSpanClass(subInfo, "rate")).trim();
        String dlText = stripTags(firstSpanClass(subInfo, "dl-num")).trim();
        String author = stripTags(firstSpanClass(subInfo, "ur")).trim();
        int downloads = parseDownloads(dlText);
        return new Hit(id, name, language, downloads, rate, author);
    }

    private static String extractId(String row) {
        // Prefer href="/detail/NNNN.html" (guaranteed unique per row)
        int idx = row.indexOf("/detail/");
        if (idx < 0) return null;
        int s = idx + "/detail/".length();
        int e = row.indexOf(".html", s);
        if (e < 0) return null;
        String id = row.substring(s, e);
        return id.isBlank() ? null : id;
    }

    private static int parseDownloads(String text) {
        if (text.isEmpty()) return 0;
        // Cases: "12,345" / "1.2万" / "123" / "123次下载"
        String t = text.replace(",", "").replace("，", "")
                .replace("次下载", "").replace("下载", "").trim();
        try {
            if (t.contains("万")) {
                String n = t.replace("万", "").trim();
                double v = Double.parseDouble(n);
                return (int) (v * 10_000);
            }
            String digits = t.replaceAll("[^0-9.]", "");
            if (digits.isEmpty()) return 0;
            double v = Double.parseDouble(digits);
            return (int) v;
        } catch (NumberFormatException ignored) { return 0; }
    }

    // --- HTML substring helpers --------------------------------------------

    private static String firstDivClassInner(String html, String cls) {
        String marker = "class=\"" + cls + "\"";
        int i = html.indexOf(marker);
        if (i < 0) return "";
        int open = findTagEnd(html, i);
        if (open < 0) return "";
        // Match the corresponding </div> by nesting depth.
        int depth = 1;
        int pos = open;
        while (pos < html.length() && depth > 0) {
            int nextClose = html.indexOf("</div", pos);
            int nextOpen  = html.indexOf("<div", pos);
            if (nextClose < 0 && nextOpen < 0) break;
            boolean advanceClose = nextOpen < 0 || (nextClose >= 0 && nextClose < nextOpen);
            if (advanceClose) {
                depth--;
                if (depth == 0) return html.substring(open, nextClose);
                pos = nextClose + 6;
            } else {
                depth++;
                pos = nextOpen + 4;
            }
        }
        return html.substring(open);
    }

    private static String firstSpanClass(String html, String cls) {
        if (html.isEmpty()) return "";
        String marker = "class=\"" + cls + "\"";
        int i = html.indexOf(marker);
        if (i < 0) return "";
        int open = findTagEnd(html, i);
        if (open < 0) return "";
        int close = html.indexOf("</span>", open);
        if (close < 0) close = html.length();
        return html.substring(open, close);
    }

    private static String firstAnchor(String html) {
        if (html.isEmpty()) return "";
        int i = html.indexOf("<a ");
        if (i < 0) i = html.indexOf("<a>");
        if (i < 0) return html;
        int open = findTagEnd(html, i);
        if (open < 0) return html;
        int close = html.indexOf("</a>", open);
        if (close < 0) close = html.length();
        return html.substring(open, close);
    }

    private static int findTagEnd(String html, int tagStart) {
        // Find the matching '>' of the tag that opens at or after tagStart,
        // ignoring '>' inside quoted attribute values.
        int i = tagStart;
        while (i < html.length()) {
            char c = html.charAt(i);
            if (c == '>') return i + 1;
            if (c == '"' || c == '\'') {
                char q = c;
                i++;
                while (i < html.length() && html.charAt(i) != q) {
                    if (html.charAt(i) == '\\' && i + 1 < html.length()) i += 2;
                    else i++;
                }
            }
            i++;
        }
        return -1;
    }

    private static String stripTags(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean inTag = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') inTag = true;
            else if (c == '>') inTag = false;
            else if (!inTag) sb.append(c);
        }
        return sb.toString()
                .replace("&nbsp;", " ")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    ZimukuScraper() {}
}
