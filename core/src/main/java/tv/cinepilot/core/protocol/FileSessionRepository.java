package tv.cinepilot.core.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class FileSessionRepository implements SessionRepository {
    private final Path file;
    private final Properties properties = new Properties();

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
        persist();
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

