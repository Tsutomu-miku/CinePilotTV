package tv.cinepilot.plugin.spi;

/**
 * Opaque key/value store the host exposes to each plugin so it can persist
 * credentials, last-watched timestamps per subject id, enable flags, and
 * any other small state the plugin needs to survive app restarts.
 *
 * <p>All values are namespaced per-plugin inside the host; a plugin can
 * never read keys written by another plugin. Implementations are expected
 * to be thread-safe but do not need to be crash-atomic.
 *
 * <p>Plugins should treat this store as <strong>untrusted</strong>: it may
 * wrap Android SharedPreferences or a similar surface the user can wipe
 * between launches. Never cache credential-equivalent values in-memory if
 * they can be re-read from here.
 */
public interface PluginSettingsStore {
    /** Read a previously-stored value. Returns {@code null} if missing. */
    String getString(String key, String defaultValue);

    /** Read a previously-stored long. Returns {@code defaultValue} if missing. */
    long getLong(String key, long defaultValue);

    /** Read a previously-stored boolean. Returns {@code defaultValue} if missing. */
    boolean getBoolean(String key, boolean defaultValue);

    /** Persist a string value. Safe to call from any thread. */
    void putString(String key, String value);

    /** Persist a long value. Safe to call from any thread. */
    void putLong(String key, long value);

    /** Persist a boolean value. Safe to call from any thread. */
    void putBoolean(String key, boolean value);

    /** Atomically remove a key. No-op if the key does not exist. */
    void remove(String key);

    /**
     * Remove every key whose string form starts with {@code prefix}.
     * The default implementation relies on {@link #keySet()} and is correct
     * but not atomic; host implementations can override with a native bulk
     * operation (e.g. SharedPreferences editor) when available.
     */
    default void removeByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return;
        for (String k : keySet()) {
            if (k != null && k.startsWith(prefix)) remove(k);
        }
    }

    /**
     * Return an immutable snapshot of all keys present in the store. Plugins
     * should generally prefer a typed helper over raw enumeration, but this
     * entry-point enables bulk cleanup (for example on account rotation) and
     * debugging utilities. Default returns an empty set so hosts that cannot
     * enumerate keys (e.g. opaque KV services) stay binary-compatible.
     */
    default java.util.Set<String> keySet() {
        return java.util.Collections.emptySet();
    }

    /** Wipe every key the owning plugin has ever written. */
    void clear();
}
