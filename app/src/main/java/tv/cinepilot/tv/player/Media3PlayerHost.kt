package tv.cinepilot.tv.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import tv.cinepilot.tv.playback.SubtitleEdgeStyle
import tv.cinepilot.tv.playback.SubtitleEncoding
import tv.cinepilot.tv.playback.SubtitleStyleStore
import tv.cinepilot.tv.subtitles.CinePilotSubtitleDecoderFactory
import tv.cinepilot.tv.subtitles.PgsSubtitleOverlay
import tv.cinepilot.tv.subtitles.SubtitleSideChannel

class Media3PlayerHost(
    private val context: Context,
    private val mediaBrowserClient: MediaBrowserClient,
    private val subtitleStyleStore: SubtitleStyleStore,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var bridge: Media3PlaybackBridge? = null
    private var progressTicker: Runnable? = null
    private var currentPlayerView: PlayerView? = null
    private var currentOverlay: PgsSubtitleOverlay? = null
    private var positionTickCallback: ((Long, Long) -> Unit)? = null
    private val checkInExecutor = Executors.newSingleThreadExecutor()

    fun createPlayerView(
        state: TvAppState,
        subtitleEncoding: SubtitleEncoding = SubtitleEncoding.AUTO,
        onPlaybackError: (PlaybackException) -> Unit = {},
        onPlaybackEnded: () -> Unit = {},
        onPositionTick: ((positionTicks: Long, durationTicks: Long) -> Unit)? = null,
    ): View {
        this.positionTickCallback = onPositionTick
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
        val playerBuilder = ExoPlayer.Builder(context)
        // Install the CinePilot subtitle factory on the builder BEFORE build() so
        // ExoPlayer constructs its TextRenderer with our ASS / PGS / SubRip decoders
        // instead of the default ones (P1-1 / P1-2 / P1-3).
        CinePilotSubtitleDecoderFactory.installOn(
            builder = playerBuilder,
            context = context,
            userPreferredEncoding = subtitleEncoding,
        )
        val nextPlayer = playerBuilder.build().apply {
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
        val playerView = RemotePlayerView(
            context = context,
            onSeekBack = ::seekBack,
            onSeekForward = ::seekForward,
            onTogglePlayPause = ::togglePlayPause,
            onPlay = { player?.play() },
            onPause = { player?.pause() },
            normalControllerTimeoutMs = PLAYER_CONTROLLER_TIMEOUT_MS,
        ).apply {
            this.player = nextPlayer
            useController = true
            keepScreenOn = true
            setShowSubtitleButton(true)
            setShowPreviousButton(false)
            setShowNextButton(false)
            setShowRewindButton(false)
            setShowFastForwardButton(false)
            setShowPlayButtonIfPlaybackIsSuppressed(false)
            setControllerAutoShow(false)
            setControllerShowTimeoutMs(PLAYER_CONTROLLER_TIMEOUT_MS)
            applySubtitleStyle(this)
            hideInlineTransportButtons()
        }
        // Wrap the PlayerView in a FrameLayout so the PGS / ASS bitmap overlay can
        // sit above the video surface without relying on RemotePlayerView internals.
        val surfaceHost = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(playerView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
        }
        val overlay = PgsSubtitleOverlay.injectInto(surfaceHost)
        overlay.attachToPlayer(nextPlayer)
        currentOverlay = overlay
        currentPlayerView = playerView
        return surfaceHost
    }

    fun playerView(): PlayerView? = currentPlayerView

    fun currentPositionTicks(): Long {
        val positionMs = player?.currentPosition ?: return 0L
        return if (positionMs <= 0) 0L else MediaTicks.fromMilliseconds(positionMs)
    }

    fun durationTicks(): Long {
        val ms = player?.duration ?: return 0L
        if (ms == C.TIME_UNSET || ms <= 0L) return 0L
        return MediaTicks.fromMilliseconds(ms)
    }

    fun seekToTicks(targetTicks: Long) {
        player?.seekTo(MediaTicks.toMilliseconds(targetTicks.coerceAtLeast(0L)))
    }

    fun referenceFrameRate(): Float? {
        val videoFormat = player?.videoFormat ?: return null
        val frameRate = videoFormat.frameRate
        return if (frameRate > 0f) frameRate else null
    }

    fun seekBack() {
        seekBy(-REMOTE_SEEK_STEP_MS)
    }

    fun seekForward() {
        seekBy(REMOTE_SEEK_STEP_MS)
    }

    fun togglePlayPause(): Boolean {
        return player?.let { currentPlayer ->
            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
                false
            } else {
                currentPlayer.play()
                true
            }
        } ?: false
    }

    fun release() {
        currentOverlay?.attachToPlayer(null)
        currentOverlay = null
        SubtitleSideChannel.clear()
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
            "pgs", "sup" -> "application/pgs"
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    private fun applySubtitleStyle(playerView: PlayerView) {
        val style = subtitleStyleStore.current()
        val edgeType = when (style.edgeStyle) {
            SubtitleEdgeStyle.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
            SubtitleEdgeStyle.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            SubtitleEdgeStyle.DROP_SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            SubtitleEdgeStyle.RAISED -> CaptionStyleCompat.EDGE_TYPE_RAISED
            SubtitleEdgeStyle.DEPRESSED -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
            SubtitleEdgeStyle.AUTO -> when (style.background) {
                SubtitleBackground.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                SubtitleBackground.RAISED -> CaptionStyleCompat.EDGE_TYPE_RAISED
                SubtitleBackground.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
                SubtitleBackground.SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                SubtitleBackground.TRANSLUCENT -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            }
        }
        val foregroundColor = applyAlpha(style.color.color, style.textOpacity.alpha)
        playerView.subtitleView?.apply {
            // ASS/SSA embedded styles are respected when the renderer supports them;
            // Media3 otherwise falls back to this CaptionStyleCompat. Author-provided
            // fonts/colors/positions are preserved by the LibASS native path; user
            // style here only governs size, margin, and edge fallbacks.
            setApplyEmbeddedStyles(true)
            setApplyEmbeddedFontSizes(true)
            setFractionalTextSize(style.size.fraction)
            setBottomPaddingFraction(style.bottomMargin.fraction)
            setStyle(CaptionStyleCompat(
                foregroundColor,
                style.background.backgroundColor,
                android.graphics.Color.TRANSPARENT,
                edgeType,
                style.background.edgeColor,
                style.fontFamily.typeface,
            ))
        }
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        val clamped = alpha.coerceIn(0, 255)
        return (clamped shl 24) or (color and 0x00FFFFFF)
    }

    private fun startProgressTicks(nextPlayer: ExoPlayer, playbackBridge: Media3PlaybackBridge) {
        val ticker = object : Runnable {
            override fun run() {
                if (player === nextPlayer && bridge === playbackBridge) {
                    playbackBridge.tick(nextPlayer.currentPosition)
                    val callback = positionTickCallback
                    if (callback != null) {
                        val duration = nextPlayer.duration.let {
                            if (it == C.TIME_UNSET || it < 0L) 0L else it
                        }
                        callback(
                            if (nextPlayer.currentPosition < 0L) 0L else MediaTicks.fromMilliseconds(nextPlayer.currentPosition),
                            if (duration == 0L) 0L else MediaTicks.fromMilliseconds(duration),
                        )
                    }
                    handler.postDelayed(this, PROGRESS_TICK_INTERVAL_MS)
                }
            }
        }
        progressTicker = ticker
        handler.post(ticker)
    }

    private companion object {
        private const val PLAYER_CONTROLLER_TIMEOUT_MS = 5_000
        private const val REMOTE_SEEK_STEP_MS = 30_000L
        private const val PROGRESS_TICK_INTERVAL_MS = 500L
    }
}
