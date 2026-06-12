package tv.cinepilot.plugins.bangumi;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Thin v0 Bangumi.tv client with only the endpoints CinePilot needs:
 * subject search, subject detail, episode list, collection state updates.
 *
 * <p>All traffic uses {@link HttpURLConnection} so the plugin does not
 * pull in a separate HTTP client dependency.
 */
final class BangumiApi {
    static final String API_BASE = "https://api.bangumi.tv";
    static final String AUTH_HEADER = "Authorization";
    static final String AUTH_SCHEME = "Bearer ";
    static final String USER_AGENT = "CinePilotTV/1.0 (bangumi-plugin)";

    /**
     * Bangumi subject types. We mostly care about {@code ANIME}; the rest
     * are documented for completeness so the matcher can filter correctly.
     */
    static final class SubjectType {
        static final int BOOK = 1;
        static final int ANIME = 2;
        static final int MUSIC = 3;
        static final int GAME = 4;
        static final int REAL = 6;
    }

    /**
     * Bangumi episode collection states. We typically set {@code WATCHED}
     * after a player completes a single episode.
     */
    static final class EpisodeCollectionType {
        static final int UNSET = 0;
        static final int WISH = 1;
        static final int WATCHED = 2;
        static final int DROPPED = 3;
    }

    /**
     * Bangumi subject collection states. Used when the user toggles
     * "继续观看 / 看过" at the series level.
     */
    static final class SubjectCollectionType {
        static final int WISH = 1;
        static final int WATCHING = 3;
        static final int DONE = 2;
        static final int ON_HOLD = 4;
        static final int DROPPED = 5;
    }

    private final String token;

    BangumiApi(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("bangumi access token is required");
        }
        this.token = token;
    }

    String searchSubject(String keyword, int type, int limit) throws IOException {
        String url = API_BASE + "/v0/search/subjects"
                + "?type=" + type
                + "&limit=" + Math.max(1, Math.min(25, limit))
                + "&keyword=" + encode(keyword);
        return get(url);
    }

    String getSubject(long subjectId) throws IOException {
        return get(API_BASE + "/v0/subjects/" + subjectId);
    }

    String listEpisodes(long subjectId, int limit) throws IOException {
        return get(API_BASE + "/v0/episodes"
                + "?subject_id=" + subjectId
                + "&limit=" + Math.max(1, Math.min(200, limit)));
    }

    void setEpisodeCollection(long episodeId, int collectionType) throws IOException {
        String url = API_BASE + "/v0/users/-/collections/-/episodes/" + episodeId
                + "?episode_type=" + collectionType;
        put(url);
    }

    void setSubjectCollection(long subjectId, int collectionType) throws IOException {
        String url = API_BASE + "/v0/users/-/collections/" + subjectId
                + "?type=" + collectionType;
        post(url);
    }

    void setSubjectRating(long subjectId, int ratingOneToTen) throws IOException {
        if (ratingOneToTen < 0 || ratingOneToTen > 10) {
            throw new IllegalArgumentException("rating must be in 0..10");
        }
        String url = API_BASE + "/v0/users/-/collections/" + subjectId
                + "?rating=" + ratingOneToTen;
        patch(url);
    }

    String me() throws IOException {
        return get(API_BASE + "/v0/me");
    }

    // --- transport -----------------------------------------------------

    private String get(String url) throws IOException {
        return read(execute(url, "GET"));
    }

    private void put(String url) throws IOException {
        HttpURLConnection connection = execute(url, "PUT");
        connection.connect();
        drain(connection);
    }

    private void post(String url) throws IOException {
        HttpURLConnection connection = execute(url, "POST");
        connection.connect();
        drain(connection);
    }

    private void patch(String url) throws IOException {
        HttpURLConnection connection = execute(url, "PATCH");
        connection.connect();
        drain(connection);
    }

    private HttpURLConnection execute(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty(AUTH_HEADER, AUTH_SCHEME + token);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(12_000);
        connection.setInstanceFollowRedirects(true);
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", "0");
        }
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            String body = readError(connection);
            throw new IOException(String.format(Locale.ROOT,
                    "Bangumi %s %s -> HTTP %d: %s", method, url, code, body));
        }
        return connection;
    }

    private static String read(HttpURLConnection connection) throws IOException {
        try (InputStream in = connection.getInputStream()) {
            return utf8(readAllBytesCompat(in));
        }
    }

    private static String readError(HttpURLConnection connection) {
        try (InputStream in = connection.getErrorStream()) {
            if (in == null) return "";
            return utf8(readAllBytesCompat(in));
        } catch (IOException ignored) {
            return "";
        }
    }

    private static void drain(HttpURLConnection connection) {
        try (InputStream in = connection.getInputStream()) {
            byte[] buf = new byte[4096];
            //noinspection StatementWithEmptyBody
            while (in.read(buf) > 0) { }
        } catch (IOException ignored) { }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Java 8-compatible bulk read helper. Mirrors the implementation in
     * the :core session repository to avoid Java 11 API use on older
     * Android TV runtimes that host the plugin class loader.
     */
    private static byte[] readAllBytesCompat(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk, 0, chunk.length)) > 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
