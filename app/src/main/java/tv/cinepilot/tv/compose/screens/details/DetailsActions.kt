package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DataSaverOff
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.ui.browseChildrenLabel
import tv.cinepilot.tv.ui.isEpisode

// ── Action button constants ─────────────────────────────────────────────

private val ActionButtonWidth = 92.dp
private val ActionButtonHeight = 88.dp
private val ActionButtonIconSize = 26.dp
private val ActionButtonGap = 10.dp
private val ActionButtonProgressHeight = 3.dp

// ── Detail action data model ────────────────────────────────────────────

internal data class DetailAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

// ── Detail actions (icon + label, flow layout) ─────────────────────────

@Composable
internal fun DetailActionFlow(
    actions: List<DetailAction>,
    palette: CinePilotPalette,
    playbackProgress: Float = 0f,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ActionButtonGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
    ) {
        itemsIndexed(actions.take(9)) { index, action ->
            val isPrimary = index == 0
            val progress = if (isPrimary) playbackProgress else 0f
            DetailActionButton(
                label = action.label,
                icon = action.icon,
                palette = palette,
                primary = isPrimary,
                progress = progress,
                requestInitialFocus = isPrimary,
                onClick = action.onClick,
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: ImageVector,
    palette: CinePilotPalette,
    primary: Boolean,
    progress: Float = 0f,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val bgColor = when {
        primary -> palette.accentStrong
        focused -> palette.glassFocus
        else -> palette.glass
    }
    val textColor = when {
        primary -> Color.White
        focused -> palette.textPrimary
        else -> palette.textSecondary
    }
    val borderColor = when {
        primary -> palette.accentStrong
        focused -> palette.focusRing
        else -> palette.glassBorder
    }
    val iconColor = when {
        primary -> Color.White
        focused -> palette.accentStrong
        else -> palette.textSecondary
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(TvDp.ControlRadius),
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .width(ActionButtonWidth)
            .height(ActionButtonHeight)
            .then(glowModifier)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(bgColor)
            .border(
                if (focused) TvDp.FocusRing else 0.5.dp,
                borderColor,
                RoundedCornerShape(TvDp.ControlRadius),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
        ) {
            Image(
                imageVector = icon,
                contentDescription = label,
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier
                    .size(ActionButtonIconSize),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
        // Progress bar (only for primary button with progress > 0)
        if (primary && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(ActionButtonProgressHeight)
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            if (primary) Color.White else palette.accentStrong
                        ),
                )
            }
        }
    }
}

// ── Detail action builder ──────────────────────────────────────────────

@Composable
internal fun rememberDetailActions(
    item: MediaItemSummary,
    onPreparePlayback: (PlaybackSelectionPreferences?) -> Unit,
    onSubtitleStyle: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onSeriesNextUp: () -> Unit,
    onOpenEpisodePicker: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onOpenProviderIdsEditor: () -> Unit,
    onChooseDownloadQuality: (Int) -> Unit,
    onManageOffline: () -> Unit,
    offlineActionLabel: String?,
    offlineActionIsReady: Boolean,
    onAddToPlaylist: () -> Unit,
    onSearchSubtitles: () -> Unit,
    hasSubtitleSearch: Boolean,
): List<DetailAction> {
    if (!item.playable()) {
        return listOf(DetailAction(item.browseChildrenLabel().ifBlank { "打开子项目" }, Icons.Outlined.Folder, onOpenFolder))
    }
    return buildList {
        if (item.hasResumePosition()) {
            add(DetailAction("继续播放", Icons.Outlined.PlayArrow) { onPreparePlayback(null) })
            add(DetailAction("从头播放", Icons.Outlined.PlayArrow) { onPreparePlayback(PlaybackSelectionPreferences.defaults()) })
        } else {
            add(DetailAction("播放", Icons.Outlined.PlayArrow) { onPreparePlayback(null) })
        }
        val isFavorite = item.userData().favorite()
        add(DetailAction(if (isFavorite) "已收藏" else "收藏", if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, onToggleFavorite))
        add(DetailAction(if (item.userData().played()) "取消已看" else "标记已看", Icons.Outlined.Check, onToggleWatched))
        add(DetailAction("省流量", Icons.Outlined.DataSaverOff) { onPreparePlayback(lowBitratePreferences(item)) })
        add(DetailAction("字幕样式", Icons.Outlined.Title, onSubtitleStyle))
        if (hasSubtitleSearch) add(DetailAction("搜索字幕", Icons.Outlined.Search, onSearchSubtitles))
        add(DetailAction("速度", Icons.Outlined.Speed, onPlaybackSpeed))
        if (item.isEpisode() && item.parentId().isNotBlank()) add(DetailAction("选集", Icons.Outlined.ViewList, onOpenEpisodePicker))
        if (item.seriesId().isNotBlank()) add(DetailAction("剧集", Icons.Outlined.Tv, onOpenSeries))
        if (item.seriesId().isNotBlank()) add(DetailAction("下一集", Icons.Outlined.SkipNext, onSeriesNextUp))
        add(DetailAction("修正编号", Icons.Outlined.Edit, onOpenProviderIdsEditor))
        offlineActionLabel?.let { label ->
            add(DetailAction(label, Icons.Outlined.Download) {
                if (offlineActionIsReady) onManageOffline() else onChooseDownloadQuality(0)
            })
        }
        add(DetailAction("播放列表", Icons.Outlined.PlaylistAdd, onAddToPlaylist))
    }
}

private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
    val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
    return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
}
