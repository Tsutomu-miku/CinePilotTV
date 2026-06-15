package tv.cinepilot.tv.compose.screens.details

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.ui.sourceLabel
import tv.cinepilot.tv.ui.streamLabel

// ── Track selector constants ────────────────────────────────────────────

private val TrackSelectorHeight = 48.dp
private val TrackSelectorIconSize = 20.dp

private const val FOCUS_SOURCE = "source"
private const val FOCUS_AUDIO = "audio"
private const val FOCUS_SUBTITLE = "subtitle"
private const val SUBTITLES_OFF_INDEX = -1

// ── Detail track options (with icons) ──────────────────────────────────

@Composable
internal fun DetailTrackOptions(
    palette: CinePilotPalette,
    playbackInfo: PlaybackInfo?,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
) {
    val sources = playbackInfo?.mediaSources().orEmpty()
    if (sources.isEmpty()) return

    val activeSource = sources.firstOrNull { it.id() == selection.mediaSourceId } ?: sources.first()

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (sources.size > 1) {
                TrackOptionSelector(
                    palette = palette,
                    title = "媒体源",
                    value = sourceValue(activeSource),
                    iconRes = tv.cinepilot.tv.R.drawable.ic_media_source,
                    modifier = Modifier.weight(1f),
                    requestInitialFocus = selection.focusKey == FOCUS_SOURCE,
                    onClick = { /* popover TBD */ },
                )
            }
            TrackOptionSelector(
                palette = palette,
                title = "音轨",
                value = audioValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_volume,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_AUDIO,
                onClick = { /* popover TBD */ },
            )
            TrackOptionSelector(
                palette = palette,
                title = "字幕",
                value = subtitleValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_subtitles,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_SUBTITLE,
                onClick = { /* popover TBD */ },
            )
        }
    }
}

@Composable
private fun TrackOptionSelector(
    palette: CinePilotPalette,
    title: String,
    value: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    val bgColor = if (focused) palette.glassFocus else palette.glass
    val borderColor = if (focused) palette.focusRing else palette.glassBorder
    val valueColor = if (focused) palette.textPrimary else palette.textSecondary
    val iconTint = if (focused) palette.accentStrong else palette.textSecondary

    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(TvDp.ControlRadius),
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .height(TrackSelectorHeight)
            .then(glowModifier)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(bgColor)
            .border(
                if (focused) TvDp.FocusRing else 0.5.dp,
                borderColor,
                RoundedCornerShape(TvDp.ControlRadius),
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier
                    .size(TrackSelectorIconSize),
            )
            Spacer(Modifier.width(10.dp))
            BasicText(
                text = title,
                style = TextStyle(
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = valueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(tv.cinepilot.tv.R.drawable.ic_chevron_down),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.textMuted),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ── Track helper functions ──────────────────────────────────────────────

private fun sourceValue(source: MediaSourceInfo): String {
    return sourceLabel(source).removePrefix("媒体源：")
}

private fun audioValue(source: MediaSourceInfo, selection: DetailTrackSelection): String {
    val streams = source.streamsOf(MediaStreamType.AUDIO)
    val selectedStream = streams.firstOrNull {
        selection.audioSelected && selection.audioStreamIndex == it.index()
    }
    return selectedStream?.let { streamLabel(it) } ?: serverDefaultLabel(streams)
}

private fun subtitleValue(source: MediaSourceInfo, selection: DetailTrackSelection): String {
    val streams = source.streamsOf(MediaStreamType.SUBTITLE)
    return when {
        selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX ->
            "关闭字幕"
        else -> {
            val selectedStream = streams.firstOrNull {
                selection.subtitleSelected && selection.subtitleStreamIndex == it.index()
            }
            selectedStream?.let { streamLabel(it) } ?: serverDefaultLabel(streams)
        }
    }
}

private fun DetailTrackSelection.forSource(sourceId: String): DetailTrackSelection {
    return if (mediaSourceId == sourceId) this else DetailTrackSelection(mediaSourceId = sourceId)
}

private fun MediaSourceInfo.streamsOf(type: MediaStreamType): List<MediaStreamInfo> {
    return mediaStreams().filter { stream -> stream.type() == type }
}

private fun serverDefaultLabel(streams: List<MediaStreamInfo>): String {
    val defaultStream = streams.firstOrNull { it.defaultStream() } ?: return "服务器默认"
    return "服务器默认 · ${streamLabel(defaultStream)}"
}

private fun MediaStreamInfo.requiresBurnInWhenTranscoding(): Boolean {
    val value = listOf(codec(), displayTitle()).joinToString(" ").lowercase()
    return value.contains("pgs") ||
        value.contains("dvdsub") ||
        value.contains("dvd_subtitle") ||
        value.contains("vobsub")
}
