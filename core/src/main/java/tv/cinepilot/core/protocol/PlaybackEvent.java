package tv.cinepilot.core.protocol;

public enum PlaybackEvent {
    TIME_UPDATE("TimeUpdate"),
    PAUSE("Pause"),
    UNPAUSE("Unpause"),
    SEEK("TimeUpdate"),
    AUDIO_TRACK_CHANGE("AudioTrackChange"),
    SUBTITLE_TRACK_CHANGE("SubtitleTrackChange"),
    QUALITY_CHANGE("QualityChange"),
    SUBTITLE_OFFSET_CHANGE("SubtitleOffsetChange"),
    PLAYBACK_RATE_CHANGE("PlaybackRateChange");

    private final String wireName;

    PlaybackEvent(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

