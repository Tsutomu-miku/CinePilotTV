package tv.cinepilot.tv.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.PlaybackUrlAuthorizer
import tv.cinepilot.core.protocol.PlaybackSessionController
import tv.cinepilot.core.tv.TvAppState

class Media3PlayerHost(
    private val context: Context,
    private val mediaBrowserClient: MediaBrowserClient,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var bridge: Media3PlaybackBridge? = null
    private var progressTicker: Runnable? = null
    private val checkInExecutor = Executors.newSingleThreadExecutor()

    fun createPlayerView(state: TvAppState, onPlaybackError: (PlaybackException) -> Unit = {}): View {
        val playable = state.playableMedia()
            ?: throw IllegalStateException("playable media is required")
        val authenticated = state.authenticated()
            ?: throw IllegalStateException("authenticated server is required")
        val playbackUrl = playable.url()
            ?: playable.request()?.url(authenticated.server().address())
            ?: throw IllegalStateException("playback url is required")
        val authorizedPlaybackUrl = PlaybackUrlAuthorizer.withAccessToken(
            playbackUrl,
            authenticated.session(),
        )

        release()
        val playbackBridge = Media3PlaybackBridge(
            PlaybackSessionController(mediaBrowserClient, authenticated, playable),
            checkInExecutor,
            onPlaybackError,
        )
        val nextPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(playbackBridge)
            setMediaItem(MediaItem.fromUri(Uri.parse(authorizedPlaybackUrl)))
            prepare()
            playWhenReady = true
        }
        bridge = playbackBridge
        player = nextPlayer
        startProgressTicks(nextPlayer, playbackBridge)
        return PlayerView(context).apply {
            this.player = nextPlayer
            useController = true
            keepScreenOn = true
            setOnKeyListener { _, keyCode, event -> handleRemoteKey(keyCode, event) }
        }
    }

    fun seekBack() {
        seekBy(-10_000L)
    }

    fun seekForward() {
        seekBy(30_000L)
    }

    fun togglePlayPause() {
        player?.let { currentPlayer ->
            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
            } else {
                currentPlayer.play()
            }
        }
    }

    fun release() {
        progressTicker?.let(handler::removeCallbacks)
        progressTicker = null
        player?.let { currentPlayer ->
            runCatching { bridge?.stop(currentPlayer.currentPosition) }
        }
        player?.release()
        player = null
        bridge = null
    }

    fun shutdown() {
        release()
        checkInExecutor.shutdownNow()
    }

    private fun handleRemoteKey(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBack()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekForward()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                player?.play()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                player?.pause()
                true
            }
            else -> false
        }
    }

    private fun seekBy(deltaMillis: Long) {
        player?.let { currentPlayer ->
            val duration = currentPlayer.duration
            val unclampedPosition = currentPlayer.currentPosition + deltaMillis
            val targetPosition = if (duration != C.TIME_UNSET && duration > 0) {
                unclampedPosition.coerceIn(0L, duration)
            } else {
                unclampedPosition.coerceAtLeast(0L)
            }
            currentPlayer.seekTo(targetPosition)
        }
    }

    private fun startProgressTicks(nextPlayer: ExoPlayer, playbackBridge: Media3PlaybackBridge) {
        val ticker = object : Runnable {
            override fun run() {
                if (player === nextPlayer && bridge === playbackBridge) {
                    playbackBridge.tick(nextPlayer.currentPosition)
                    handler.postDelayed(this, 1_000L)
                }
            }
        }
        progressTicker = ticker
        handler.post(ticker)
    }
}
