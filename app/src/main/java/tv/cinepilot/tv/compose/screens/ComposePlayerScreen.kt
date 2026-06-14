package tv.cinepilot.tv.compose.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.R
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
            update = { host -> host.attachPlayerView(playerView) },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.92f),
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

data class ComposeNextUpInfo(
    val item: MediaItemSummary,
    val episodeLabel: String,
    val countdownSeconds: Int,
    val autoPlay: Boolean,
)

@Composable
private fun SegmentPill(
    palette: CinePilotPalette,
    label: String,
    range: LongRange,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
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
        BasicText(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = if (focused) palette.accentStrong else palette.textSecondary,
                fontSize = TvText.Body,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun PlayerBottomOsd(
    palette: CinePilotPalette,
    mediaTitle: String,
    mediaSubtitle: String,
    positionTicks: Long,
    durationTicks: Long,
    chapters: List<Pair<Long, String>>,
    modifier: Modifier = Modifier,
    onChapterClick: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onInfo: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 30.dp, bottom = 22.dp),
    ) {
        BasicText(
            text = listOf(mediaTitle, mediaSubtitle).filter { it.isNotBlank() }.joinToString("  ·  "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            ),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Seek30Button(
                palette = palette,
                forward = false,
                onClick = onSeekBack,
            )
            Spacer(Modifier.width(20.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_skip_previous,
                size = 44.dp,
                iconSize = 18.dp,
                onClick = onSeekBack,
            )
            Spacer(Modifier.width(18.dp))
            PlayPauseButton(
                palette = palette,
                isPlaying = false,
                onClick = onTogglePlayPause,
            )
            Spacer(Modifier.width(18.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_skip_next,
                size = 44.dp,
                iconSize = 18.dp,
                onClick = onSeekForward,
            )
            Spacer(Modifier.width(20.dp))
            Seek30Button(
                palette = palette,
                forward = true,
                onClick = onSeekForward,
            )
            Spacer(Modifier.width(72.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_subtitles,
                size = 36.dp,
                iconSize = 16.dp,
                onClick = {},
            )
            Spacer(Modifier.width(12.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_audio,
                size = 36.dp,
                iconSize = 16.dp,
                onClick = {},
            )
            Spacer(Modifier.width(12.dp))
            TextOsdButton(
                palette = palette,
                text = "4K",
                onClick = {},
            )
            Spacer(Modifier.width(12.dp))
            TextOsdButton(
                palette = palette,
                text = "1.0x",
                onClick = {},
            )
            Spacer(Modifier.width(12.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_info,
                size = 36.dp,
                iconSize = 16.dp,
                onClick = onInfo,
            )
            Spacer(Modifier.width(12.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_settings,
                size = 36.dp,
                iconSize = 16.dp,
                onClick = onSettings,
            )
        }
        Spacer(Modifier.height(16.dp))
        ProgressBarRow(
            palette = palette,
            positionTicks = positionTicks,
            durationTicks = durationTicks,
        )
    }
}

@Composable
private fun ProgressBarRow(
    palette: CinePilotPalette,
    positionTicks: Long,
    durationTicks: Long,
) {
    val fraction = if (durationTicks > 0L) {
        (positionTicks.toFloat() / durationTicks.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = formatPlayerClock(positionTicks),
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = TvText.PlayerTime,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp),
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
            // Played track
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(palette.accentStrong),
            )
            // Thumb with glow
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction)
                        .height(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(10.dp)
                            .shadow(
                                elevation = 8.dp,
                                spotColor = palette.focusGlow,
                                ambientColor = palette.focusGlow,
                                shape = RoundedCornerShape(5.dp),
                            )
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        BasicText(
            text = formatPlayerClock(durationTicks),
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = TvText.PlayerTime,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private fun formatPlayerClock(ticks: Long): String {
    val totalSeconds = MediaTicks.toSeconds(ticks.coerceAtLeast(0L))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
private fun PlayPauseButton(
    palette: CinePilotPalette,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val size = 58.dp
    val shape = RoundedCornerShape(size / 2)
    val glowColor = palette.focusGlow
    val iconColor = Color.White

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 18.dp else 10.dp,
                shape = shape,
                spotColor = glowColor,
                ambientColor = glowColor,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass.copy(alpha = 0.9f),
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 1.dp,
                color = if (focused) palette.focusRing else palette.focusRing.copy(alpha = 0.6f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun Seek30Button(
    palette: CinePilotPalette,
    forward: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val size = 44.dp
    val shape = RoundedCornerShape(size / 2)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 10.dp else 4.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(if (forward) R.drawable.ic_forward_30 else R.drawable.ic_rewind_30),
                contentDescription = if (forward) "Forward 30s" else "Rewind 30s",
                colorFilter = ColorFilter.tint(if (focused) palette.accentStrong else palette.textPrimary),
                modifier = Modifier.size(size - 6.dp),
            )
            BasicText(
                text = "30",
                style = TextStyle(
                    color = if (focused) palette.accentStrong else palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun IconOsdButton(
    palette: CinePilotPalette,
    iconRes: Int,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(size / 2)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (focused) 10.dp else 3.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (focused) palette.accentStrong else palette.textSecondary),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun TextOsdButton(
    palette: CinePilotPalette,
    text: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val height = 30.dp
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(height)
            .shadow(
                elevation = if (focused) 10.dp else 3.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Transparent,
                ambientColor = if (focused) palette.focusGlow else Color.Transparent,
            )
            .clip(shape)
            .background(
                if (focused) palette.glassFocus else palette.glass,
                shape,
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = if (focused) palette.accentStrong else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun NextUpMiniCard(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    authenticated: AuthenticatedServer?,
    info: ComposeNextUpInfo,
    modifier: Modifier = Modifier,
    onPlayNext: () -> Unit,
) {
    val artwork = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = authenticated,
        item = info.item,
        target = ArtworkTarget.LANDSCAPE,
        width = 400,
        height = 225,
    )
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .width(200.dp)
            .height(112.dp)
            .clip(shape)
            .shadow(
                elevation = if (focused) 16.dp else 6.dp,
                shape = shape,
                spotColor = if (focused) palette.focusGlow else Color.Black.copy(alpha = 0.4f),
                ambientColor = if (focused) palette.focusGlow else Color.Black.copy(alpha = 0.3f),
            )
            .border(
                width = if (focused) TvDp.FocusRing else 0.8.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPlayNext,
            ),
    ) {
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = info.item.name(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Dark gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.65f),
                        ),
                    ),
                ),
        )
        // Play icon circle overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(12.dp),
            )
        }
        // Bottom text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = "下一集",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.Body,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            BasicText(
                text = info.episodeLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = TvText.Label,
                ),
            )
        }
    }
}

@Composable
private fun ChapterStrip(
    palette: CinePilotPalette,
    chapters: List<Pair<Long, String>>,
    modifier: Modifier = Modifier,
    onChapterClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = modifier,
    ) {
        items(items = chapters, key = { chapter -> chapter.first }) { (ticks, title) ->
            TvActionButton(
                palette = palette,
                label = title.ifBlank { MediaTicks.formatShort(ticks) },
                modifier = Modifier.width(95.dp),
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
        width = 600,
        height = 338,
    )
    val shape = RoundedCornerShape(TvDp.CardRadius)
    Box(
        modifier = modifier
            .width(TvDp.NextUpWidth)
            .clip(shape)
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, shape)
            .padding(12.dp),
    ) {
        Column {
            BasicText(
                text = "接下来播放",
                maxLines = 1,
                style = TextStyle(
                    color = palette.textSecondary,
                    fontSize = TvText.Metadata,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(width = TvDp.LandscapeWidth, height = TvDp.LandscapeHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.posterFallback)
                        .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
                ) {
                    CinePilotAsyncImage(
                        request = artwork,
                        contentDescription = info.item.name(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = info.episodeLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textMuted,
                            fontSize = TvText.Metadata,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = info.item.name().ifBlank { "下一集" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = TvText.Body,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        text = info.item.overview().ifBlank {
                            if (info.autoPlay) "${info.countdownSeconds}s 后自动播放" else "下一集"
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = palette.textMuted, fontSize = TvText.Label),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TvActionButton(
                    palette = palette,
                    label = "立即播放",
                    selected = true,
                    requestInitialFocus = true,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayNext,
                )
                TvActionButton(
                    palette = palette,
                    label = "取消",
                    modifier = Modifier.weight(1f),
                    onClick = onCancelNextUp,
                )
            }
        }
    }
}

private fun FrameLayout.attachPlayerView(playerView: View) {
    if (playerView.parent === this && childCount == 1 && getChildAt(0) === playerView) {
        return
    }
    (playerView.parent as? ViewGroup)?.removeView(playerView)
    removeAllViews()
    addView(
        playerView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
}
