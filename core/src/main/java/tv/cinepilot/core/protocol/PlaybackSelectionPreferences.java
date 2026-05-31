package tv.cinepilot.core.protocol;

public record PlaybackSelectionPreferences(
        long startTimeTicks,
        Integer audioStreamIndex,
        Integer subtitleStreamIndex,
        Integer maxAudioChannels,
        int maxWidth,
        int maxHeight,
        int maxBitRate,
        String mediaSourceId,
        Float playbackRate
) {
    public PlaybackSelectionPreferences {
        if (startTimeTicks < 0) {
            throw new IllegalArgumentException("startTimeTicks must be zero or greater");
        }
        if (mediaSourceId != null && mediaSourceId.isBlank()) {
            mediaSourceId = null;
        }
        if (playbackRate != null && playbackRate <= 0f) {
            playbackRate = null;
        }
    }

    public PlaybackSelectionPreferences(
            long startTimeTicks,
            Integer audioStreamIndex,
            Integer subtitleStreamIndex,
            Integer maxAudioChannels,
            int maxWidth,
            int maxHeight,
            int maxBitRate,
            String mediaSourceId
    ) {
        this(startTimeTicks, audioStreamIndex, subtitleStreamIndex, maxAudioChannels, maxWidth, maxHeight, maxBitRate, mediaSourceId, null);
    }

    public PlaybackSelectionPreferences(
            long startTimeTicks,
            Integer audioStreamIndex,
            Integer subtitleStreamIndex,
            Integer maxAudioChannels,
            int maxWidth,
            int maxHeight,
            int maxBitRate
    ) {
        this(startTimeTicks, audioStreamIndex, subtitleStreamIndex, maxAudioChannels, maxWidth, maxHeight, maxBitRate, null);
    }

    public static PlaybackSelectionPreferences defaults() {
        return new PlaybackSelectionPreferences(0L, null, null, null, 0, 0, 0, null, null);
    }

    public static PlaybackSelectionPreferences lowBitrate(long startTimeTicks) {
        return new PlaybackSelectionPreferences(startTimeTicks, null, null, 2, 1280, 720, 4_000_000, null, null);
    }

    public PlaybackSelectionPreferences withMediaSourceId(String value) {
        return new PlaybackSelectionPreferences(
                startTimeTicks,
                audioStreamIndex,
                subtitleStreamIndex,
                maxAudioChannels,
                maxWidth,
                maxHeight,
                maxBitRate,
                value,
                playbackRate
        );
    }

    public PlaybackSelectionPreferences withPlaybackRate(float value) {
        return new PlaybackSelectionPreferences(
                startTimeTicks,
                audioStreamIndex,
                subtitleStreamIndex,
                maxAudioChannels,
                maxWidth,
                maxHeight,
                maxBitRate,
                mediaSourceId,
                value
        );
    }
}
