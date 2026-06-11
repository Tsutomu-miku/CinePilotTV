package tv.cinepilot.core.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public final class FileSessionRepository implements SessionRepository {
    private static final String ACTIVE_KEY_PREFIX = "__active__";

    private final Path file;
    private final Properties properties = new Properties();
    /** Stable insertion-order mirror of {@link #properties} for the saved-session entries only. */
    private final Map<SessionScope, String> sessionOrder = new LinkedHashMap<>();

    public FileSessionRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file is required");
        }
        this.file = file;
        load();
    }

    @Override
    public synchronized void save(SavedSession session) {
        properties.setProperty(key(session.scope()), session.accessToken());
        sessionOrder.put(session.scope(), session.accessToken());
        persist();
    }

    @Override
    public synchronized Optional<SavedSession> find(SessionScope scope) {
        String token = properties.getProperty(key(scope));
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SavedSession(scope, token));
    }

    @Override
    public synchronized void revoke(SessionScope scope) {
        properties.remove(key(scope));
        sessionOrder.remove(scope);
        properties.remove(activeKey(scope.serverId(), scope.clientName(), scope.deviceId(), scope.appVersion()));
        persist();
    }

    @Override
    public synchronized List<SavedSession> listForServer(String serverId, ClientIdentity client) {
        List<SavedSession> out = new ArrayList<>();
        for (Map.Entry<SessionScope, String> entry : sessionOrder.entrySet()) {
            SessionScope scope = entry.getKey();
            if (!scope.serverId().equals(serverId)) continue;
            if (!scope.clientName().equals(client.clientName())) continue;
            if (!scope.deviceId().equals(client.deviceId())) continue;
            if (!scope.appVersion().equals(client.version())) continue;
            out.add(new SavedSession(scope, entry.getValue()));
        }
        return out;
    }

    @Override
    public synchronized void markActive(SessionScope scope) {
        properties.setProperty(
                activeKey(scope.serverId(), scope.clientName(), scope.deviceId(), scope.appVersion()),
                scope.userId() + "|" + encode(scope.serverUrl())
        );
        persist();
    }

    @Override
    public synchronized Optional<SessionScope> activeScope(String serverId, ClientIdentity client) {
        String raw = properties.getProperty(activeKey(
                serverId, client.clientName(), client.deviceId(), client.version()));
        if (raw == null || raw.isBlank()) return Optional.empty();
        int split = raw.indexOf('|');
        if (split <= 0) return Optional.empty();
        String userId = raw.substring(0, split);
        String serverUrl = decode(raw.substring(split + 1));
        SessionScope scope = new SessionScope(
                serverId, serverUrl, userId,
                client.clientName(), client.deviceId(), client.version());
        return sessionOrder.containsKey(scope) ? Optional.of(scope) : Optional.empty();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new MediaBrowserException("Failed to load saved sessions", exception);
        }
        // Rebuild the stable-order session map: Properties is a Hashtable and does not
        // preserve insertion order. We rebuild sessionOrder by iterating the raw file a
        // second time so key insertion order is retained for profile list rendering.
        try (InputStream ordered = Files.newInputStream(file)) {
            sessionOrder.clear();
            byte[] rawBytes = readAllBytesCompat(ordered);
            String content = new String(rawBytes);
            for (String line : content.split("\\R")) {
                if (line == null || line.isBlank()) continue;
                if (line.startsWith("#") || line.startsWith("!")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String rawKey = line.substring(0, eq).trim();
                if (rawKey.startsWith(ACTIVE_KEY_PREFIX)) continue;
                SessionScope scope = parseKey(rawKey);
                if (scope == null) continue;
                String token = properties.getProperty(rawKey);
                if (token == null || token.isBlank()) continue;
                sessionOrder.put(scope, token);
            }
        } catch (IOException exception) {
            throw new MediaBrowserException("Failed to rebuild saved sessions order", exception);
        }
    }

    private void persist() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "CinePilot TV sessions");
            }
        } catch (IOException exception) {
            throw new MediaBrowserException("Failed to persist saved sessions", exception);
        }
    }

    private static String key(SessionScope scope) {
        return encode(scope.serverId()) + "|" +
                encode(scope.serverUrl()) + "|" +
                encode(scope.userId()) + "|" +
                encode(scope.clientName()) + "|" +
                encode(scope.deviceId()) + "|" +
                encode(scope.appVersion());
    }

    private static SessionScope parseKey(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 6) return null;
        for (String part : parts) {
            if (part == null || part.isEmpty()) return null;
        }
        return new SessionScope(
                decode(parts[0]), decode(parts[1]), decode(parts[2]),
                decode(parts[3]), decode(parts[4]), decode(parts[5]));
    }

    private static String activeKey(String serverId, String client, String device, String version) {
        return ACTIVE_KEY_PREFIX +
                encode(serverId) + "|" + encode(client) + "|" +
                encode(device) + "|" + encode(version);
    }

    private static String encode(String value) {
        return UrlEncoding.encodeComponent(value);
    }

    private static String decode(String value) {
        return UrlEncoding.decodeComponent(value);
    }

    /**
     * Java 8 compatible InputStream-to-byte-array reader. We deliberately avoid
     * the Java 11 bulk InputStream read API because older Android TV runtimes
     * do not expose it.
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
