package tv.cinepilot.tv.player

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import tv.cinepilot.core.protocol.PlaybackSessionController

class Media3PlaybackBridge(
    private val controller: PlaybackSessionController,
    private val onPlaybackError: (PlaybackException) -> Unit = {},
    private val clock: () -> Long = { System.currentTimeMillis() },
) : Player.Listener {
    private var started = false
    private var stopped = false
    private var lastPositionBeforeSeek = 0L
    private var playbackRate = 1f

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && !started) {
            started = true
            controller.start(clock(), lastPositionBeforeSeek)
        }
        if (playbackState == Player.STATE_ENDED) {
            stop(lastPositionBeforeSeek)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!started || stopped) {
            return
        }
        if (isPlaying) {
            controller.unpause(clock(), lastPositionBeforeSeek)
        } else {
            controller.pause(clock(), lastPositionBeforeSeek)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        lastPositionBeforeSeek = newPosition.positionMs
        if (started && !stopped && reason == Player.DISCONTINUITY_REASON_SEEK) {
            controller.seek(clock(), newPosition.positionMs)
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        val nextPlaybackRate = playbackParameters.speed
        if (nextPlaybackRate == playbackRate) {
            return
        }
        playbackRate = nextPlaybackRate
        if (started && !stopped) {
            controller.playbackRateChanged(clock(), lastPositionBeforeSeek, nextPlaybackRate)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        onPlaybackError(error)
    }

    fun tick(positionMillis: Long) {
        lastPositionBeforeSeek = positionMillis
        if (started && !stopped) {
            controller.progressIfDue(clock(), positionMillis)
        }
    }

    fun stop(positionMillis: Long) {
        lastPositionBeforeSeek = positionMillis
        if (started && !stopped) {
            stopped = true
            controller.stop(positionMillis)
        }
    }
}
