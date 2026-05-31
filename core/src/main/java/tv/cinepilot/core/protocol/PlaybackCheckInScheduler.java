package tv.cinepilot.core.protocol;

import java.time.Duration;
import java.util.Optional;

public final class PlaybackCheckInScheduler {
    public static final Duration DEFAULT_PROGRESS_INTERVAL = Duration.ofSeconds(10);

    private final Duration progressInterval;
    private boolean started;
    private boolean stopped;
    private long lastProgressAtMillis = Long.MIN_VALUE;

    public PlaybackCheckInScheduler() {
        this(DEFAULT_PROGRESS_INTERVAL);
    }

    public PlaybackCheckInScheduler(Duration progressInterval) {
        if (progressInterval == null || progressInterval.isNegative() || progressInterval.isZero()) {
            throw new IllegalArgumentException("progressInterval must be positive");
        }
        this.progressInterval = progressInterval;
    }

    public PlaybackCheckIn start(long nowMillis, PlaybackReport report) {
        ensureNotStopped();
        if (started) {
            throw new IllegalStateException("playback has already started");
        }
        started = true;
        lastProgressAtMillis = nowMillis;
        return new PlaybackCheckIn(PlaybackEndpoint.STARTED, null, report);
    }

    public Optional<PlaybackCheckIn> progressIfDue(long nowMillis, PlaybackReport report) {
        ensureStarted();
        ensureNotStopped();
        if (nowMillis - lastProgressAtMillis < progressInterval.toMillis()) {
            return Optional.empty();
        }
        lastProgressAtMillis = nowMillis;
        return Optional.of(new PlaybackCheckIn(PlaybackEndpoint.PROGRESS, PlaybackEvent.TIME_UPDATE, report));
    }

    public PlaybackCheckIn immediate(long nowMillis, PlaybackEvent event, PlaybackReport report) {
        ensureStarted();
        ensureNotStopped();
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        lastProgressAtMillis = nowMillis;
        return new PlaybackCheckIn(PlaybackEndpoint.PROGRESS, event, report);
    }

    public PlaybackCheckIn stop(PlaybackReport report) {
        ensureStarted();
        ensureNotStopped();
        stopped = true;
        return new PlaybackCheckIn(PlaybackEndpoint.STOPPED, null, report);
    }

    public boolean started() {
        return started;
    }

    public boolean stopped() {
        return stopped;
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("playback has not started");
        }
    }

    private void ensureNotStopped() {
        if (stopped) {
            throw new IllegalStateException("playback has already stopped");
        }
    }
}

