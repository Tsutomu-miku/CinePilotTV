package tv.cinepilot.tv.player

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.PlaybackSessionController
import tv.cinepilot.core.tv.TvAppState

class Media3PlayerHost(
    private val context: Context,
    private val mediaBrowserClient: MediaBrowserClient,
) {
    private var player: ExoPlayer? = null
    private var bridge: Media3PlaybackBridge? = null

    fun createPlayerView(state: TvAppState): View {
        val playable = state.playableMedia()
            ?: throw IllegalStateException("playable media is required")
        val authenticated = state.authenticated()
            ?: throw IllegalStateException("authenticated server is required")
        val playbackUrl = playable.url()
            ?: playable.request()?.url(authenticated.server().address())
            ?: throw IllegalStateException("playback url is required")

        release()
        val playbackBridge = Media3PlaybackBridge(
            PlaybackSessionController(mediaBrowserClient, authenticated, playable),
        )
        val nextPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(playbackBridge)
            setMediaItem(MediaItem.fromUri(Uri.parse(playbackUrl)))
            prepare()
            playWhenReady = true
        }
        bridge = playbackBridge
        player = nextPlayer
        return PlayerView(context).apply {
            this.player = nextPlayer
            useController = true
            keepScreenOn = true
        }
    }

    fun release() {
        player?.let { bridge?.stop(it.currentPosition) }
        player?.release()
        player = null
        bridge = null
    }
}
