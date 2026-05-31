package tv.cinepilot.core.protocol;

public record PlaybackSelectionPreferences(
        long startTimeTicks,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        Integer maxAudioChannels,
        int maxWidth,
        int maxHeight,
        int maxBitRate
) {
    public PlaybackSelectionPreferences {
        if (startTimeTicks < 0) {
            throw new IllegalArgumentException("startTimeTicks must be zero or greater");
        }
    }

    public static PlaybackSelectionPreferences defaults() {
        return new PlaybackSelectionPreferences(0L, null, null, null, 0, 0, 0);
    }
}

