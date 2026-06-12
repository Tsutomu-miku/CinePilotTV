package tv.cinepilot.core.tv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import tv.cinepilot.core.protocol.HomeRowsSerializer;

/**
 * Persists the latest successful home rows payload to disk so the TV UI can
 * render instantly on cold start before the network round-trip completes.
 *
 * Files are scoped per (server id, user id) so switching accounts or servers
 * never reuses stale rows. The file is a single JSON array produced by
 * {@link HomeRowsSerializer#serialize(List)}.
 *
 * Failures (missing parent dir, full disk, corrupt JSON, etc.) are surfaced as
 * empty optionals so the caller can fall back to a normal loadHome() network
 * call without any user-visible error surface.
 */
public final class FileHomeRowsCache {

    private static final String FILE_SUFFIX = "-home-rows.json";

    private final Path directory;

    public FileHomeRowsCache(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("directory is required");
        }
        this.directory = directory;
    }

    public synchronized Optional<List<HomeRow>> load(String serverId, String userId) {
        if (isBlank(serverId) || isBlank(userId)) return Optional.empty();
        Path file = directory.resolve(fileName(serverId, userId));
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            byte[] bytes = readAllBytesCompat(file);
            if (bytes.length == 0) return Optional.empty();
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<HomeRow> rows = HomeRowsSerializer.deserialize(json);
            if (rows == null || rows.isEmpty()) return Optional.empty();
            return Optional.of(rows);
        } catch (IOException | RuntimeException ignored) {
            // Corrupt file, missing permissions, etc. — just drop it and let
            // the caller refresh from the network.
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored2) { /* nothing more we can do */ }
            return Optional.empty();
        }
    }

    public synchronized boolean save(String serverId, String userId, List<HomeRow> rows) {
        if (isBlank(serverId) || isBlank(userId) || rows == null || rows.isEmpty()) {
            return false;
        }
        try {
            if (!Files.isDirectory(directory)) {
                Files.createDirectories(directory);
            }
            Path file = directory.resolve(fileName(serverId, userId));
            String json = HomeRowsSerializer.serialize(rows);
            // Write atomically via temp + rename so a partial write never
            // leaves a corrupt file on disk (prevents a race between a crash
            // and next launch's restore path).
            Path tmp = directory.resolve(fileName(serverId, userId) + ".tmp");
            Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public synchronized boolean clear(String serverId, String userId) {
        if (isBlank(serverId) || isBlank(userId)) return false;
        try {
            return Files.deleteIfExists(directory.resolve(fileName(serverId, userId)));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String fileName(String serverId, String userId) {
        // Use fixed, URL-safe hex hashes so filesystem encoding quirks on
        // budget TV devices (fat32 USB-adopted storage, non-UTF8 locales)
        // never create unreadable files.
        return hexHash(serverId) + "-" + hexHash(userId) + FILE_SUFFIX;
    }

    private static String hexHash(String s) {
        // FNV-1a 64-bit variant, rendered as 16 lowercase hex chars.
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        String hex = Long.toHexString(hash);
        if (hex.length() < 16) {
            StringBuilder sb = new StringBuilder(16);
            for (int i = hex.length(); i < 16; i++) sb.append('0');
            sb.append(hex);
            return sb.toString();
        }
        return hex;
    }

    // Android-compat: avoid Files.readAllBytes because some Xiaomi TV runtimes
    // strip Java 9+ java.nio.* helpers even when minSdk 26 nominally ships them.
    private static byte[] readAllBytesCompat(Path path) throws IOException {
        long size = Files.size(path);
        if (size > 64L * 1024 * 1024) {
            // Home rows payload is at most a few hundred KB; anything larger
            // is clearly corrupt and should not be loaded.
            throw new IOException("home rows cache too large: " + size);
        }
        try (InputStreamCompat in = new InputStreamCompat(Files.newInputStream(path))) {
            return in.readFullyCompat((int) size);
        }
    }

    // Nested type keeps java.io.InputStream references isolated from the
    // main call path, which makes it easier to validate the Xiaomi-TV guard
    // with a single grep in scripts/check.sh (no Java-9+ bulk-read anywhere).
    private static final class InputStreamCompat implements AutoCloseable {
        private final java.io.InputStream delegate;

        InputStreamCompat(java.io.InputStream delegate) {
            this.delegate = delegate;
        }

        byte[] readFullyCompat(int sizeHint) throws IOException {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(
                    Math.max(0, sizeHint));
            byte[] tmp = new byte[8192];
            int read;
            while ((read = delegate.read(tmp)) != -1) {
                buffer.write(tmp, 0, read);
            }
            return buffer.toByteArray();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty() || s.trim().isEmpty();
    }
}
