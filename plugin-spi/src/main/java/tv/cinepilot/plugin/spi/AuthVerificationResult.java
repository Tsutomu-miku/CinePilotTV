package tv.cinepilot.plugin.spi;

import java.util.Objects;

/**
 * Result type returned by
 * {@link CinePilotPlugin#verifyAuth(PluginSettingsStore, String)} so the
 * settings UI can report verification success/failure consistently across
 * plugins.
 *
 * <p>On success the caller typically saves the credential through
 * {@link PluginSettingsStore}; the plugin is expected to persist any extra
 * runtime state (user id, scopes, refresh token, ...) that it needs for
 * subsequent dispatches inside the same store.
 *
 * <p>On failure {@link #message} should be a short human-readable sentence
 * (preferably localized) that the host renders inline on the settings form,
 * e.g. "令牌无效或已过期" / "网络连接超时".
 */
public final class AuthVerificationResult {
    private final boolean ok;
    private final String message;
    private final String displayName;

    private AuthVerificationResult(boolean ok, String message, String displayName) {
        this.ok = ok;
        this.message = Objects.requireNonNullElse(message, "");
        this.displayName = Objects.requireNonNullElse(displayName, "");
    }

    public static AuthVerificationResult success(String displayName) {
        return new AuthVerificationResult(true, "", displayName == null ? "" : displayName);
    }

    public static AuthVerificationResult failure(String message) {
        return new AuthVerificationResult(false, message == null ? "" : message, "");
    }

    /** @return {@code true} if the provided credential passed verification. */
    public boolean isOk() { return ok; }

    /** @return short error message on failure, or an empty string on success. */
    public String message() { return message; }

    /** @return verified user nickname / display name on success, or empty string. */
    public String displayName() { return displayName; }
}
