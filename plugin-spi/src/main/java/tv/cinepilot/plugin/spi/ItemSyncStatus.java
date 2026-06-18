package tv.cinepilot.plugin.spi;

/**
 * Rough per-item sync status returned by
 * {@link UserDataSyncPlugin#itemSyncStatus(PluginSettingsStore, MediaItemSnapshot)}.
 *
 * <p>Describes the state of a single media item from the plugin's perspective
 * so the host can render a small status chip next to the details poster.
 * {@code UNSUPPORTED} is returned for media kinds the plugin does not
 * handle (e.g. Bangumi ignores non-anime features and non-episode videos).
 */
public enum ItemSyncStatus {
    /** Plugin does not handle items of this kind / provider. */
    UNSUPPORTED,
    /** Plugin requires user auth before it can emit any sync for this item. */
    AUTH_REQUIRED,
    /** Item has never been matched against upstream (no subject id cache hit). */
    UNMATCHED,
    /** Item was matched but the last sync attempt failed. Retrying is safe. */
    FAILED,
    /** Item has been successfully synced upstream. */
    SYNCED,
}
