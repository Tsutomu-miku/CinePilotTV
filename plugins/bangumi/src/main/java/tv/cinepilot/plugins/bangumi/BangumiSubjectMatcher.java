package tv.cinepilot.plugins.bangumi;

import java.util.Locale;
import tv.cinepilot.plugin.spi.MediaItemSnapshot;

/**
 * Translate Jellyfin / Emby {@link MediaItemSnapshot}s into Bangumi
 * subject ids + episode ids. The matcher prefers explicit provider ids
 * (Bangumi subject ID) and falls back to a name+year fuzzy match that
 * the caller is expected to verify against the upstream response.
 *
 * <p>Result is always a fuzzy match: the caller should send the returned
 * search query to the Bangumi API and then pick the highest-confidence
 * result from the response. Caching the matched ids in
 * {@link tv.cinepilot.plugin.spi.PluginSettingsStore} avoids re-searching
 * every time the same Jellyfin id shows up.
 */
final class BangumiSubjectMatcher {

    /**
     * @return the bangumi subject id if the item already carries one via
     *     a well-known provider id key ({@code "BgmTv"}), otherwise 0.
     */
    static long explicitSubjectId(MediaItemSnapshot item) {
        String providerId = item.providerId("BgmTv");
        if (providerId.isBlank()) providerId = item.providerId("BANGUMI_TV");
        if (providerId.isBlank()) providerId = item.providerId("Bangumi");
        if (providerId.isBlank()) return 0L;
        try {
            return Long.parseLong(providerId);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * @return the keyword to use for a search request. Weights the
     *     original-title / preferred-title mix that Jellyfin commonly
     *     stores in the item name.
     */
    static String searchKeyword(MediaItemSnapshot item) {
        String raw = item.name().trim();
        if (raw.isBlank()) return "";
        // Strip trailing year suffix: "Show Name (2024)" -> "Show Name"
        String stripped = raw.replaceAll("\\s*\\(\\d{4}\\)\\s*$", "").trim();
        if (stripped.isBlank()) stripped = raw;
        if (item.productionYear() > 0 && !stripped.contains(Integer.toString(item.productionYear()))) {
            return stripped + " " + item.productionYear();
        }
        return stripped;
    }

    /**
     * @return the bangumi subject type to filter searches with. We
     *     default to {@link BangumiApi.SubjectType#ANIME} because the
     *     bangumi audience skew is almost entirely anime viewers; users
     *     tracking real content via a Bangumi account are rare enough
     *     that fuzzy-matching against everything else usually produces
     *     worse matches than just narrowing to anime.
     */
    static int subjectType(MediaItemSnapshot item) {
        return switch (item.kind()) {
            case MOVIE, VIDEO, OTHER -> BangumiApi.SubjectType.ANIME;
            default -> BangumiApi.SubjectType.ANIME;
        };
    }

    /**
     * @return the 1-indexed bangumi episode number we should mark watched
     *     for an episode item. Returns 0 if the snapshot does not carry a
     *     usable season/episode numbering.
     */
    static int episodeNumber(MediaItemSnapshot episode) {
        if (episode.kind() != MediaItemSnapshot.Kind.EPISODE) return 0;
        if (episode.indexNumber() <= 0) return 0;
        return episode.indexNumber();
    }

    /**
     * Human-readable string used for caching and logging. Composed of the
     * (stable) Jellyfin server id + item id pair so two different servers
     * never alias their bangumi lookups.
     */
    static String cacheKey(MediaItemSnapshot item) {
        return String.format(Locale.ROOT, "%s:%s", item.serverId(), item.itemId());
    }

    static String cacheEpisodeKey(MediaItemSnapshot item) {
        return String.format(Locale.ROOT, "%s:%s:ep", item.serverId(), item.itemId());
    }

    private BangumiSubjectMatcher() { }
}
