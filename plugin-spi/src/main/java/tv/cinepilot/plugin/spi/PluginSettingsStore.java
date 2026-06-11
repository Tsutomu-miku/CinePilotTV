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

    /** Wipe every key the owning plugin has ever written. */
    void clear();
}
