package tv.cinepilot.core.tv;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;
import tv.cinepilot.core.protocol.MediaItemSummary;

public record ShowStructure(
        MediaItemSummary series,
        List<MediaItemSummary> seasons,
        MediaItemSummary selectedSeason,
        List<MediaItemSummary> episodes,
        MediaItemSummary nextUp,
        MediaItemSummary resumeEpisode
) {
    public ShowStructure {
        if (series == null) {
            throw new IllegalArgumentException("series is required");
        }
        seasons = AndroidCollections.listCopy(seasons);
        episodes = AndroidCollections.listCopy(episodes);
    }
}
