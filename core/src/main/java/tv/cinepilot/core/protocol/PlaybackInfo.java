package tv.cinepilot.core.protocol;

import java.util.List;
import tv.cinepilot.core.AndroidCollections;

public record PlaybackInfo(
        String itemId,
        String playSessionId,
        List<MediaSourceInfo> mediaSources
) {
    public PlaybackInfo {
        require(itemId, "itemId");
        require(playSessionId, "playSessionId");
        mediaSources = AndroidCollections.listCopy(mediaSources);
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
