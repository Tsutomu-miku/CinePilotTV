package tv.cinepilot.tv.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.PlaybackSessionController

class Media3PlaybackBridge(
    private val controller: PlaybackSessionController,
    private val checkInExecutor: Executor,
    private val onPlaybackError: (PlaybackException) -> Unit = {},
    private val onPlaybackEnded: () -> Unit = {},
    private val trackResolver: Media3StreamIndexResolver = Media3StreamIndexResolver(emptyList()),
    initialAudioStreamIndex: Int? = null,
    initialSubtitleStreamIndex: Int? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : Player.Listener {
    private var started = false
    private var stopped = false
    private var lastPositionBeforeSeek = 0L
    private var playbackRate = 1f
    private var audioStreamIndex = initialAudioStreamIndex
    private var subtitleStreamIndex = initialSubtitleStreamIndex

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && !started) {
            started = true
            checkIn { controller.start(clock(), lastPositionBeforeSeek) }
        }
        if (playbackState == Player.STATE_ENDED) {
            stop(lastPositionBeforeSeek)
            onPlaybackEnded()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!started || stopped) {
            return
        }
        if (isPlaying) {
            checkIn { controller.unpause(clock(), lastPositionBeforeSeek) }
        } else {
            checkIn { controller.pause(clock(), lastPositionBeforeSeek) }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        lastPositionBeforeSeek = newPosition.positionMs
        if (started && !stopped && reason == Player.DISCONTINUITY_REASON_SEEK) {
            checkIn { controller.seek(clock(), newPosition.positionMs) }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        if (!started || stopped) {
            return
        }
        val nextAudio = selectedTrackIndex(tracks, C.TRACK_TYPE_AUDIO, trackResolver::audioStreamIndex)
        if (nextAudio != null && nextAudio != audioStreamIndex) {
            audioStreamIndex = nextAudio
            checkIn { controller.audioTrackChanged(clock(), lastPositionBeforeSeek, nextAudio) }
        }

        val nextSubtitle = selectedTrackIndex(tracks, C.TRACK_TYPE_TEXT, trackResolver::subtitleStreamIndex)
        val reportedSubtitle = nextSubtitle ?: if (
            !hasSelectedTrack(tracks, C.TRACK_TYPE_TEXT) &&
            subtitleStreamIndex != null &&
            subtitleStreamIndex != -1
        ) {
            -1
        } else {
            null
        }
        if (reportedSubtitle != null && reportedSubtitle != subtitleStreamIndex) {
            subtitleStreamIndex = reportedSubtitle
            checkIn { controller.subtitleTrackChanged(clock(), lastPositionBeforeSeek, reportedSubtitle) }
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        val nextPlaybackRate = playbackParameters.speed
        if (nextPlaybackRate == playbackRate) {
            return
        }
        playbackRate = nextPlaybackRate
        if (started && !stopped) {
            checkIn { controller.playbackRateChanged(clock(), lastPositionBeforeSeek, nextPlaybackRate) }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        onPlaybackError(error)
    }

    fun tick(positionMillis: Long) {
        lastPositionBeforeSeek = positionMillis
        if (started && !stopped) {
            checkIn { controller.progressIfDue(clock(), positionMillis) }
        }
    }

    fun stop(positionMillis: Long) {
        lastPositionBeforeSeek = positionMillis
        if (started && !stopped) {
            stopped = true
            checkIn { controller.stop(positionMillis) }
        }
    }

    private fun checkIn(action: () -> Unit) {
        checkInExecutor.execute {
            runCatching(action)
        }
    }

    private fun selectedTrackIndex(
        tracks: Tracks,
        trackType: Int,
        resolve: (Format) -> Int?,
    ): Int? {
        tracks.groups.forEach { group ->
            if (group.type == trackType && group.isSelected) {
                for (index in 0 until group.length) {
                    if (group.isTrackSelected(index)) {
                        return resolve(group.getTrackFormat(index))
                    }
                }
            }
        }
        return null
    }

    private fun hasSelectedTrack(tracks: Tracks, trackType: Int): Boolean {
        tracks.groups.forEach { group ->
            if (group.type == trackType && group.isSelected) {
                for (index in 0 until group.length) {
                    if (group.isTrackSelected(index)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
