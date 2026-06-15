package tv.cinepilot.tv.compose.screens.player

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

/**
 * Public entry point for the player screen composable.
 *
 * This is the top-level composable that hosts the video player view, bottom OSD
 * controls, segment skip pills, next-up card, and info panel.
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
) {
    var infoVisible by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { host -> host.attachPlayerView(playerView) },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.9f),
                        ),
                    ),
                ),
        )
        if (infoVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TvDp.ScreenX)
                    .width(TvDp.PanelWidth),
            ) {
                InfoPanel(
                    palette = palette,
                    title = "视频信息",
                    body = debugInfo.lineSequence().filter { it.isNotBlank() }.take(14).joinToString("\n"),
                )
            }
        }
        introSegmentTicks?.let { range ->
            SegmentPill(
                palette = palette,
                label = "跳过片头",
                range = range,
                onClick = onSkipIntro,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 76.dp),
            )
        }
        creditsSegmentTicks?.let { range ->
            SegmentPill(
                palette = palette,
                label = "跳过片尾",
                range = range,
                onClick = onSkipCredits,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 76.dp),
            )
        }
        nextUp?.let { info ->
            NextUpMiniCard(
                palette = palette,
                owner = owner,
                artworkFactory = artworkFactory,
                authenticated = authenticated,
                info = info,
                onPlayNext = onPlayNext,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 108.dp),
            )
        }
        PlayerBottomOsd(
            palette = palette,
            mediaTitle = mediaTitle,
            mediaSubtitle = mediaSubtitle,
            positionTicks = positionTicks,
            durationTicks = durationTicks,
            chapters = chapters,
            onChapterClick = onChapterClick,
            onTogglePlayPause = onTogglePlayPause,
            onSeekBack = onSeekBack,
            onSeekForward = onSeekForward,
            onInfo = { infoVisible = !infoVisible },
            onSettings = onOpenPlaybackSettings,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Information about the next-up item for automatic playback.
 *
 * @property item Summary of the next media item.
 * @property episodeLabel Human-readable episode label (e.g. "S1 E3").
 * @property countdownSeconds Countdown seconds until auto-play.
 * @property autoPlay Whether auto-play is enabled.
 */
data class ComposeNextUpInfo(
    val item: MediaItemSummary,
    val episodeLabel: String,
    val countdownSeconds: Int,
    val autoPlay: Boolean,
)

@Composable
internal fun SegmentPill(
    palette: CinePilotPalette,
    label: String,
    range: LongRange,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(
                Color.Black.copy(alpha = if (focused) 0.75f else 0.55f),
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 1.dp,
                color = if (focused) palette.focusRing else palette.glassBorder.copy(alpha = 0.6f),
                shape = shape,
            )
            .shadow(
                elevation = if (focused) 12.dp else 4.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_skip_forward_row),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.accentStrong),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = if (focused) palette.textPrimary else palette.textSecondary,
                    fontSize = TvText.Body,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
