package tv.cinepilot.core.protocol;

import java.time.Duration;
import java.util.Optional;

public final class PlaybackSessionController {
    private final MediaBrowserClient client;
    private final AuthenticatedServer authenticated;
    private final PlayableMedia playableMedia;
    private final PlaybackCheckInScheduler scheduler;
    private final boolean canSeek;
    private boolean paused;
    private Integer audioStreamIndex;
    private Integer subtitleStreamIndex;
    private Float playbackRate;

    public PlaybackSessionController(
            MediaBrowserClient client,
            AuthenticatedServer authenticated,
            PlayableMedia playableMedia
    ) {
        this(client, authenticated, playableMedia, PlaybackCheckInScheduler.DEFAULT_PROGRESS_INTERVAL, true);
    }

    public PlaybackSessionController(
            MediaBrowserClient client,
            AuthenticatedServer authenticated,
            PlayableMedia playableMedia,
            Duration progressInterval,
            boolean canSeek
    ) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        if (authenticated == null) {
            throw new IllegalArgumentException("authenticated is required");
        }
        if (playableMedia == null) {
            throw new IllegalArgumentException("playableMedia is required");
        }
        this.client = client;
        this.authenticated = authenticated;
        this.playableMedia = playableMedia;
        this.scheduler = new PlaybackCheckInScheduler(progressInterval);
        this.canSeek = canSeek;
        this.audioStreamIndex = playableMedia.audioStreamIndex();
        this.subtitleStreamIndex = playableMedia.subtitleStreamIndex();
    }

    public void start(long nowMillis, long positionMillis) {
        client.sendPlaybackCheckIn(authenticated, scheduler.start(nowMillis, report(positionMillis)));
    }

    public boolean progressIfDue(long nowMillis, long positionMillis) {
        Optional<PlaybackCheckIn> checkIn = scheduler.progressIfDue(nowMillis, report(positionMillis));
        checkIn.ifPresent(value -> client.sendPlaybackCheckIn(authenticated, value));
        return checkIn.isPresent();
    }

    public void pause(long nowMillis, long positionMillis) {
        paused = true;
        immediate(nowMillis, PlaybackEvent.PAUSE, positionMillis);
    }

    public void unpause(long nowMillis, long positionMillis) {
        paused = false;
        immediate(nowMillis, PlaybackEvent.UNPAUSE, positionMillis);
    }

    public void seek(long nowMillis, long positionMillis) {
        immediate(nowMillis, PlaybackEvent.SEEK, positionMillis);
    }

    public void audioTrackChanged(long nowMillis, long positionMillis, Integer streamIndex) {
        audioStreamIndex = streamIndex;
        immediate(nowMillis, PlaybackEvent.AUDIO_TRACK_CHANGE, positionMillis);
    }

    public void subtitleTrackChanged(long nowMillis, long positionMillis, Integer streamIndex) {
        subtitleStreamIndex = streamIndex;
        immediate(nowMillis, PlaybackEvent.SUBTITLE_TRACK_CHANGE, positionMillis);
    }

    public void playbackRateChanged(long nowMillis, long positionMillis, Float rate) {
        playbackRate = rate;
        immediate(nowMillis, PlaybackEvent.PLAYBACK_RATE_CHANGE, positionMillis);
    }

    public void stop(long positionMillis) {
        client.sendPlaybackCheckIn(authenticated, scheduler.stop(report(positionMillis)));
    }

    public boolean paused() {
        return paused;
    }

    public PlaybackReport report(long positionMillis) {
        return new PlaybackReport(
                playableMedia.itemId(),
                playableMedia.mediaSourceId(),
                playableMedia.playSessionId(),
                playableMedia.playMethod(),
                canSeek,
                paused,
                MediaTicks.fromMilliseconds(positionMillis),
                audioStreamIndex,
                subtitleStreamIndex,
                playbackRate
        );
    }

    private void immediate(long nowMillis, PlaybackEvent event, long positionMillis) {
        client.sendPlaybackCheckIn(authenticated, scheduler.immediate(nowMillis, event, report(positionMillis)));
    }
}

