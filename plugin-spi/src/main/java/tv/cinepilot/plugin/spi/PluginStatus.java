package tv.cinepilot.plugin.spi;

/**
 * Rough status indicator surfaced to users in the plugin settings screen.
 * Plugins are responsible for reporting the correct state when the host
 * polls {@link CinePilotPlugin#status(PluginSettingsStore)}.
 */
public enum PluginStatus {
    /** Plugin is enabled, credentials are present, sync hooks should fire. */
    READY,
    /** Plugin requires user auth before it can do useful work. */
    AUTH_REQUIRED,
    /** Plugin was disabled from the settings UI. */
    DISABLED,
    /** Last sync attempt failed; retrying is safe. */
    TEMPORARILY_UNAVAILABLE,
}
