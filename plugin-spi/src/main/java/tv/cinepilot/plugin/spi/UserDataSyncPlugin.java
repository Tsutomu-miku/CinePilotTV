package tv.cinepilot.plugin.spi;

/**
 * Fired by the host whenever the user makes a user-data change against a
 * media item. Implementations should sync the change to their upstream
 * service asynchronously; the host never blocks on plugin callbacks and
 * any thrown {@link RuntimeException} is logged and swallowed.
 *
 * <p>Hooks are best-effort, at-least-once: if a plugin throws, the host
 * does not retry, but the next start of the same workflow (e.g. the user
 * rates the same item again) will trigger the hook again.
 */
public interface UserDataSyncPlugin extends CinePilotPlugin {
    /** Rating values are normalized to the 0-10 range; 0 means "cleared". */
    void onRatingChanged(PluginSettingsStore store, MediaItemSnapshot item, double ratingZeroToTen);

    void onFavoriteChanged(PluginSettingsStore store, MediaItemSnapshot item, boolean isFavorite);

    /** Fired when an item crosses the "completion" threshold in the host player. */
    void onWatchedChanged(PluginSettingsStore store, MediaItemSnapshot item, boolean isWatched);
}
