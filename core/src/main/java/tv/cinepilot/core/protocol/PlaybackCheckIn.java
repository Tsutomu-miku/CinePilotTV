package tv.cinepilot.core.protocol;

public record PlaybackCheckIn(
        PlaybackEndpoint endpoint,
        PlaybackEvent event,
        PlaybackReport report
) {
    public PlaybackCheckIn {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (report == null) {
            throw new IllegalArgumentException("report is required");
        }
    }
}

