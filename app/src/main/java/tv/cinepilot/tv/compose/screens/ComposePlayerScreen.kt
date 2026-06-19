package tv.cinepilot.tv.compose.screens

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.tv.compose.screens.player.ComposeNextUpInfo as PlayerComposeNextUpInfo
import tv.cinepilot.tv.compose.screens.player.ComposePlayerScreen as PlayerComposePlayerScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

/**
 * Information about the next-up item for automatic playback.
 *
 * This is a type alias to the implementation in the `player` sub-package.
 *
 * @see tv.cinepilot.tv.compose.screens.player.ComposeNextUpInfo
 */
typealias ComposeNextUpInfo = PlayerComposeNextUpInfo

/**
 * Top-level player screen composable.
 *
 * This is a thin facade that delegates to the implementation in the `player` sub-package.
 * The actual implementation lives in `screens/player/PlayerScreen.kt`.
 *
 * @see tv.cinepilot.tv.compose.screens.player.ComposePlayerScreen
 */
@Composable
fun ComposePlayerScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    playerView: View,
    mediaTitle: String,
    mediaSubtitle: String,
    positionTicks: Long,
    durationTicks: Long,
    debugInfo: String,
    chapters: List<Pair<Long, String>>,
    introSegmentTicks: LongRange?,
    creditsSegmentTicks: LongRange?,
    nextUp: ComposeNextUpInfo?,
    onChapterClick: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSkipIntro: () -> Unit,
    onSkipCredits: () -> Unit,
    onPlayNext: () -> Unit,
    onCancelNextUp: () -> Unit,
    onOpenPlaybackSettings: () -> Unit,
    subtitleShortcutLabel: String,
    audioShortcutLabel: String,
    qualityLabel: String,
    speedLabel: String,
    onSubtitlesShortcut: () -> Unit,
    onAudioShortcut: () -> Unit,
    onQualityShortcut: () -> Unit,
    onSpeedShortcut: () -> Unit,
    osdVisible: Boolean,
    onUserInteraction: () -> Unit,
    onBackPressed: () -> Unit,
) {
    PlayerComposePlayerScreen(
        palette = palette,
        owner = owner,
        artworkFactory = artworkFactory,
        authenticated = authenticated,
        playerView = playerView,
        mediaTitle = mediaTitle,
        mediaSubtitle = mediaSubtitle,
        positionTicks = positionTicks,
        durationTicks = durationTicks,
        debugInfo = debugInfo,
        chapters = chapters,
        introSegmentTicks = introSegmentTicks,
        creditsSegmentTicks = creditsSegmentTicks,
        nextUp = nextUp,
        onChapterClick = onChapterClick,
        onTogglePlayPause = onTogglePlayPause,
        onSeekBack = onSeekBack,
        onSeekForward = onSeekForward,
        onSkipIntro = onSkipIntro,
        onSkipCredits = onSkipCredits,
        onPlayNext = onPlayNext,
        onCancelNextUp = onCancelNextUp,
        onOpenPlaybackSettings = onOpenPlaybackSettings,
        subtitleShortcutLabel = subtitleShortcutLabel,
        audioShortcutLabel = audioShortcutLabel,
        qualityLabel = qualityLabel,
        speedLabel = speedLabel,
        onSubtitlesShortcut = onSubtitlesShortcut,
        onAudioShortcut = onAudioShortcut,
        onQualityShortcut = onQualityShortcut,
        onSpeedShortcut = onSpeedShortcut,
        osdVisible = osdVisible,
        onUserInteraction = onUserInteraction,
        onBackPressed = onBackPressed,
    )
}
