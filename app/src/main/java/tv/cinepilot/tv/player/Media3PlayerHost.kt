package tv.cinepilot.tv.player

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.AuthSession
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.core.protocol.PlayMethod
import tv.cinepilot.core.protocol.PlayableMedia
import tv.cinepilot.core.protocol.PlaybackUrlAuthorizer
import tv.cinepilot.core.protocol.PlaybackSessionController
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.playback.SubtitleBackground
import tv.cinepilot.tv.playback.SubtitleStyleStore

class Media3PlayerHost(
    private val context: Context,
    private val mediaBrowserClient: MediaBrowserClient,
    private val subtitleStyleStore: SubtitleStyleStore,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var bridge: Media3PlaybackBridge? = null
    private var progressTicker: Runnable? = null
    private val checkInExecutor = Executors.newSingleThreadExecutor()

    fun createPlayerView(
        state: TvAppState,
        onPlaybackError: (PlaybackException) -> Unit = {},
        onPlaybackEnded: () -> Unit = {},
    ): View {
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
            controller = PlaybackSessionController(mediaBrowserClient, authenticated, playable),
            checkInExecutor = checkInExecutor,
            onPlaybackError = onPlaybackError,
            onPlaybackEnded = onPlaybackEnded,
            trackResolver = Media3StreamIndexResolver(playable.mediaStreams()),
            initialAudioStreamIndex = playable.audioStreamIndex(),
            initialSubtitleStreamIndex = playable.subtitleStreamIndex(),
        )
        val nextPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(playbackBridge)
            applyTrackPreferences(playable)
            setMediaItem(
                mediaItem(playable, authorizedPlaybackUrl, authenticated.session()),
                initialPlayerPositionMillis(playable),
            )
            playable.playbackRate()?.takeIf { it > 0f && it != 1f }?.let(::setPlaybackSpeed)
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
            setShowSubtitleButton(true)
            setShowPreviousButton(false)
            setShowNextButton(false)
            setControllerAutoShow(true)
            setControllerShowTimeoutMs(PLAYER_CONTROLLER_TIMEOUT_MS)
            applySubtitleStyle(this)
            setOnKeyListener { _, keyCode, event -> handleRemoteKey(this, keyCode, event) }
        }
    }

    fun seekBack() {
        seekBy(-REMOTE_SEEK_STEP_MS)
    }

    fun seekForward() {
        seekBy(REMOTE_SEEK_STEP_MS)
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

    private fun handleRemoteKey(playerView: PlayerView, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_CAPTIONS -> {
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBack()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekForward()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekBack()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
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

    private fun mediaItem(
        playable: PlayableMedia,
        authorizedPlaybackUrl: String,
        session: AuthSession,
    ): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(authorizedPlaybackUrl))
        val subtitleDeliveryUrl = playable.subtitleDeliveryUrl()
        val subtitleStreamIndex = playable.subtitleStreamIndex()
        if (
            subtitleStreamIndex != null &&
            subtitleStreamIndex >= 0 &&
            !subtitleDeliveryUrl.isNullOrBlank()
        ) {
            val authorizedSubtitleUrl = PlaybackUrlAuthorizer.withAccessToken(subtitleDeliveryUrl, session)
            builder.setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(authorizedSubtitleUrl))
                    .setMimeType(subtitleMimeType(playable.subtitleCodec(), authorizedSubtitleUrl))
                    .setLanguage(playable.subtitleLanguage().ifBlank { null })
                    .setLabel(playable.subtitleDisplayTitle().ifBlank { null })
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            ))
        }
        return builder.build()
    }

    private fun ExoPlayer.applyTrackPreferences(playable: PlayableMedia) {
        val textIndex = playable.subtitleStreamIndex()
        val textDisabled = textIndex != null && textIndex < 0
        val textLanguage = playable.subtitleLanguage().ifBlank { null }
        val parameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, textDisabled)
            .setSelectUndeterminedTextLanguage(!textDisabled && textIndex != null)
            .setIgnoredTextSelectionFlags(if (textDisabled) C.SELECTION_FLAG_DEFAULT else 0)
            .apply {
                if (!textDisabled && textLanguage != null) {
                    setPreferredTextLanguage(textLanguage)
                }
            }
            .build()
        trackSelectionParameters = parameters
    }

    private fun initialPlayerPositionMillis(playable: PlayableMedia): Long {
        if (playable.playMethod() == PlayMethod.TRANSCODE && playable.request() != null) {
            return 0L
        }
        return MediaTicks.toMilliseconds(playable.startTimeTicks())
    }

    private fun subtitleMimeType(codec: String, url: String): String {
        val value = (codec.ifBlank { url.substringBefore('?').substringAfterLast('.', "") }).lowercase()
        return when (value) {
            "srt", "subrip" -> MimeTypes.APPLICATION_SUBRIP
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "vtt", "webvtt" -> MimeTypes.TEXT_VTT
            "ttml", "dfxp" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    private fun applySubtitleStyle(playerView: PlayerView) {
        val style = subtitleStyleStore.current()
        val edgeType = if (style.background == SubtitleBackground.NONE) {
            CaptionStyleCompat.EDGE_TYPE_NONE
        } else {
            CaptionStyleCompat.EDGE_TYPE_OUTLINE
        }
        playerView.subtitleView?.apply {
            setApplyEmbeddedStyles(false)
            setApplyEmbeddedFontSizes(false)
            setFractionalTextSize(style.size.fraction)
            setBottomPaddingFraction(0.08f)
            setStyle(CaptionStyleCompat(
                style.color.color,
                style.background.backgroundColor,
                android.graphics.Color.TRANSPARENT,
                edgeType,
                style.background.edgeColor,
                Typeface.DEFAULT_BOLD,
            ))
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

    private companion object {
        private const val PLAYER_CONTROLLER_TIMEOUT_MS = 5_000
        private const val REMOTE_SEEK_STEP_MS = 30_000L
    }
}
