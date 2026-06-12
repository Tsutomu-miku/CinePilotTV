package tv.cinepilot.plugin.spi;

/**
 * Fired by the host player as playback progresses. All position values are
 * in wall-clock milliseconds since playback began; durations are in
 * milliseconds of total run time.
 *
 * <p>Hooks are fired on a background thread. Plugins should debounce their
 * upstream scrobble calls to avoid hammering their server; the host does
 * NOT throttle playback progress callbacks -- they can fire as frequently
 * as every few hundred milliseconds.
 */
public interface PlaybackSyncPlugin extends CinePilotPlugin {
    /** Fired once on playback start (after first frame decoded). */
    void onPlaybackStarted(PluginSettingsStore store, MediaItemSnapshot item, long durationMillis);

    /** Fired periodically during playback with the current playback position. */
    void onPlaybackProgress(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            long positionMillis,
            long durationMillis
    );

    /** Fired when playback ends (pause, stop, app backgrounded, completion). */
    void onPlaybackStopped(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            long finalPositionMillis,
            long durationMillis
    );
}
