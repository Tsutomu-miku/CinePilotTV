package tv.cinepilot.tv.compose.screens

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.FocusSurface
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget

@Composable
fun ComposePlayerScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    playerView: View,
    debugInfo: String,
    chapters: List<Pair<Long, String>>,
    introSegmentTicks: LongRange?,
    creditsSegmentTicks: LongRange?,
    nextUp: ComposeNextUpInfo?,
    onChapterClick: (Long) -> Unit,
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
            .background(androidx.compose.ui.graphics.Color.Black),
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
            update = { host ->
                (playerView.parent as? ViewGroup)?.removeView(playerView)
                host.removeAllViews()
                host.addView(playerView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ))
            },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 86.dp),
        ) {
            TvActionButton(
                palette = palette,
                label = if (infoVisible) "隐藏信息" else "视频信息",
                modifier = Modifier.width(112.dp),
                onClick = { infoVisible = !infoVisible },
            )
            TvActionButton(
                palette = palette,
                label = "播放设置",
                modifier = Modifier.width(112.dp),
                onClick = onOpenPlaybackSettings,
            )
        }
        if (infoVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TvDp.ScreenX)
                    .width(460.dp),
            ) {
                InfoPanel(
                    palette = palette,
                    title = "视频信息",
                    body = debugInfo.lineSequence().filter { it.isNotBlank() }.take(14).joinToString("\n"),
                )
            }
        }
        introSegmentTicks?.let { range ->
            SkipButton(
                palette = palette,
                label = "跳过片头",
                range = range,
                onClick = onSkipIntro,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 40.dp, bottom = 140.dp),
            )
        }
        creditsSegmentTicks?.let { range ->
            SkipButton(
                palette = palette,
                label = "跳过片尾",
                range = range,
                onClick = onSkipCredits,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 140.dp),
            )
        }
        if (chapters.isNotEmpty()) {
            ChapterStrip(
                palette = palette,
                chapters = chapters,
                onChapterClick = onChapterClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 40.dp, end = 40.dp, bottom = 210.dp)
                    .fillMaxWidth(),
            )
        }
        nextUp?.let { info ->
            NextUpCard(
                palette = palette,
                owner = owner,
                artworkFactory = artworkFactory,
                authenticated = authenticated,
                info = info,
                onPlayNext = onPlayNext,
                onCancelNextUp = onCancelNextUp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 140.dp),
            )
        }
    }
}

data class ComposeNextUpInfo(
    val item: MediaItemSummary,
    val episodeLabel: String,
    val countdownSeconds: Int,
    val autoPlay: Boolean,
)

@Composable
private fun SkipButton(
    palette: CinePilotPalette,
    label: String,
    range: LongRange,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val seconds = ((range.last - range.first).coerceAtLeast(0L) / MediaTicks.TICKS_PER_SECOND)
        .toInt()
        .coerceAtLeast(1)
    TvActionButton(
        palette = palette,
        label = "$label ${seconds}s",
        selected = true,
        modifier = modifier.width(150.dp),
        onClick = onClick,
    )
}

@Composable
private fun ChapterStrip(
    palette: CinePilotPalette,
    chapters: List<Pair<Long, String>>,
    modifier: Modifier = Modifier,
    onChapterClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
        modifier = modifier,
    ) {
        items(items = chapters, key = { chapter -> chapter.first }) { (ticks, title) ->
            TvActionButton(
                palette = palette,
                label = title.ifBlank { MediaTicks.formatShort(ticks) },
                modifier = Modifier.width(150.dp),
                onClick = { onChapterClick(ticks) },
            )
        }
    }
}

@Composable
private fun NextUpCard(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    info: ComposeNextUpInfo,
    modifier: Modifier = Modifier,
    onPlayNext: () -> Unit,
    onCancelNextUp: () -> Unit,
) {
    val artwork = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = info.item,
        target = ArtworkTarget.LANDSCAPE,
        width = 392,
        height = 220,
    )
    FocusSurface(
        palette = palette,
        enabled = false,
        modifier = modifier.width(620.dp),
        padding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(width = 196.dp, height = 110.dp)
                    .clip(RoundedCornerShape(TvDp.CardRadius))
                    .background(palette.posterFallback)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(TvDp.CardRadius)),
            ) {
                CinePilotAsyncImage(
                    request = artwork,
                    contentDescription = info.item.name(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = info.episodeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textSecondary, fontSize = TvText.Metadata),
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = info.item.name().ifBlank { "下一集" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textPrimary, fontSize = TvText.Body),
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = info.item.overview().ifBlank {
                        if (info.autoPlay) "${info.countdownSeconds}s 后自动播放" else "下一集"
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TvActionButton(
                    palette = palette,
                    label = "立即播放",
                    selected = true,
                    requestInitialFocus = true,
                    modifier = Modifier.width(112.dp),
                    onClick = onPlayNext,
                )
                TvActionButton(
                    palette = palette,
                    label = "取消",
                    modifier = Modifier.width(112.dp),
                    onClick = onCancelNextUp,
                )
            }
        }
    }
}
