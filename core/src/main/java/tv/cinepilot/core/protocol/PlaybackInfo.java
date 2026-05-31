package tv.cinepilot.core.protocol;

import java.util.List;

public record PlaybackInfo(
        String itemId,
        String playSessionId,
        List<MediaSourceInfo> mediaSources
) {
    public PlaybackInfo {
        require(itemId, "itemId");
        require(playSessionId, "playSessionId");
        mediaSources = List.copyOf(mediaSources == null ? List.of() : mediaSources);
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

