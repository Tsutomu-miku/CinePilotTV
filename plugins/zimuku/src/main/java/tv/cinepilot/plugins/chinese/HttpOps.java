package tv.cinepilot.plugins.chinese;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

/**
 * Shared HTTP helpers for the Chinese subtitle providers.
 *
 * <p>Mirrors what ChineseSubFinder's HttpClient: randomized desktop browser
 * user-agent, ~60s combined timeout, no keep-alive, redirects followed.
 * Callers are expected to run on background threads.
 */
final class HttpOps {

    static final int CONNECT_MS = 12_000;
    static final int READ_MS = 25_000;
    static final int MAX_BODY = 64 * 1024 * 1024;   // 64 MiB cap (subtitles are tiny; this covers RAR/SRT bundles)
    static final int BUF = 8192;

    private static final String[] UAS = new String[] {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
    };
    private static final Random RND = new Random();

    static String randomUA() {
        return UAS[RND.nextInt(UAS.length)];
    }

    /** GET a URL into a byte array. Returns raw bytes regardless of charset. */
    static byte[] httpGet(String url, Map<String, String> extraHeaders) throws IOException {
        return httpRequest(url, "GET", null, null, extraHeaders);
    }

    /** POST form-encoded POST. {@code form} entries are urlencoded by this helper. */
    static byte[] httpPostForm(String url, Map<String, String> form, Map<String, String> extraHeaders)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        return httpRequest(url, "POST", body,
                "application/x-www-form-urlencoded; charset=UTF-8", extraHeaders);
    }

    private static byte[] httpRequest(
            String url,
            String method,
            byte[] reqBody,
            String contentType,
            Map<String, String> extraHeaders) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(CONNECT_MS);
            conn.setReadTimeout(READ_MS);
            conn.setRequestMethod(method);
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", randomUA());
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7");
            conn.setRequestProperty("Accept", "*/*");
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            if (extraHeaders != null) {
                for (Map.Entry<String, String> h : extraHeaders.entrySet()) {
                    conn.setRequestProperty(h.getKey(), h.getValue());
                }
            }
            if (reqBody != null && reqBody.length > 0) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(reqBody);
                }
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String snippet = "";
                try (InputStream es = conn.getErrorStream()) {
                    if (es != null) {
                        byte[] b = readN(es, 1024);
                        snippet = new String(b, StandardCharsets.ISO_8859_1);
                    }
                }
                throw new IOException("HTTP " + code + " " + method + " " + url + " " + snippet);
            }
            try (InputStream in = conn.getInputStream()) {
                return readN(in, MAX_BODY);
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Parse a byte response as text, auto-detect charset from Content-Type or HTML meta tag. */
    static String decodeText(byte[] body, String contentType) {
        Charset cs = charsetFromContentType(contentType);
        if (cs != null) return new String(body, cs);
        cs = detectHtmlCharset(body, StandardCharsets.UTF_8);
        return new String(body, cs);
    }

    private static Charset charsetFromContentType(String ct) {
        if (ct == null) return null;
        int i = ct.toLowerCase().indexOf("charset=");
        if (i < 0) return null;
        String name = ct.substring(i + 8).trim();
        int semi = name.indexOf(';');
        if (semi > 0) name = name.substring(0, semi);
        name = name.replace("\"", "").trim();
        try {
            return Charset.forName(name);
        } catch (RuntimeException ignored) { return null; }
    }

    static Charset detectHtmlCharset(byte[] body, Charset fallback) {
        int probeLen = Math.min(body.length, 4096);
        String head = new String(body, 0, probeLen, StandardCharsets.ISO_8859_1);
        int idx = head.toLowerCase().indexOf("charset=");
        if (idx < 0) return fallback;
        int end = idx + 8;
        while (end < head.length()
                && (head.charAt(end) == ' '
                    || head.charAt(end) == '='
                    || head.charAt(end) == '"'
                    || head.charAt(end) == '\'')) end++;
        int start = end;
        while (end < head.length()) {
            char c = head.charAt(end);
            if (c == '"' || c == '\'' || c == ' ' || c == ';' || c == '/' || c == '>') break;
            end++;
        }
        String name = head.substring(start, end).trim().toLowerCase();
        if (name.startsWith("gb")) return Charset.forName("GB18030");
        if (name.startsWith("utf-8") || name.startsWith("utf8")) return StandardCharsets.UTF_8;
        if (name.isBlank()) return fallback;
        try {
            return Charset.forName(name);
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static byte[] readN(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[BUF];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > max) throw new IOException("response exceeded " + max + " bytes");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private HttpOps() {}
}
