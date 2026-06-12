package tv.cinepilot.plugin.spi;

import java.util.List;

/**
 * Plugin interface for online subtitle search and download providers.
 *
 * <p>Plugins implement this interface to add subtitle search capabilities to
 * CinePilot TV. The host calls {@link #searchSubtitles} when the user opens
 * the subtitle search panel for a media item, and {@link #downloadSubtitle}
 * when the user selects a specific result.
 *
 * <p>All methods are called on a background executor -- plugins may perform
 * network I/O directly. Any thrown {@link RuntimeException} is logged and
 * swallowed by the host.
 *
 * <p>A plugin may return results in any order; the host sorts and groups by
 * language for display.
 */
public interface SubtitleSearchPlugin extends CinePilotPlugin {

    /**
     * Search for subtitles matching the given media item.
     *
     * <p>Implementations should use the best available identifiers from the
     * snapshot (provider ids like TMDB/IMDB, name + year, etc.) to find
     * matching subtitles.
     *
     * @param store   per-plugin settings store, for reading API keys etc.
     * @param item    snapshot of the media item to find subtitles for
     * @param language optional language filter (ISO 639-1 code or empty for all)
     * @return list of matching subtitle results (may be empty, never null)
     */
    List<SubtitleSearchResult> searchSubtitles(
            PluginSettingsStore store,
            MediaItemSnapshot item,
            String language
    );

    /**
     * Download the actual subtitle file for a search result.
     *
     * <p>Implementations should return the raw bytes of the subtitle file
     * (e.g. SRT text, ASS text, PGS bitmap data). The host handles caching
     * and format detection on its side.
     *
     * @param store  per-plugin settings store
     * @param result the search result to download (from this same plugin)
     * @return raw subtitle file bytes
     * @throws RuntimeException if download fails
     */
    byte[] downloadSubtitle(
            PluginSettingsStore store,
            SubtitleSearchResult result
    );
}
