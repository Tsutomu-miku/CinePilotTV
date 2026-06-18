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

    /**
     * Poll the plugin for the current sync status of a single item. Used
     * by the host to render the small sync chip on the details screen
     * (e.g. "Bangumi 已同步 / 同步失败 / 未匹配").
     *
     * <p>Implementations MUST NOT do network I/O here -- answer only from
     * the plugin's settings-store cache.
     *
     * @return {@link ItemSyncStatus#UNSUPPORTED} when the plugin does not
     *     handle this item kind (most common); otherwise one of the other
     *     statuses reflecting stored state.
     */
    default ItemSyncStatus itemSyncStatus(PluginSettingsStore store, MediaItemSnapshot item) {
        return ItemSyncStatus.UNSUPPORTED;
    }

    /**
     * Re-sync a specific item on user request. The host calls this when
     * the user clicks the "重试" action on a FAILED sync chip.
     *
     * <p>Implementations should redo whatever the original dispatch did
     * (e.g. re-push watched, favorite, rating). They may throw; the host
     * wraps the call in try/catch and surfaces the updated status through
     * {@link #itemSyncStatus} after a screen re-render.
     */
    default void retrySyncItem(PluginSettingsStore store, MediaItemSnapshot item) {
        // no-op default
    }
}
