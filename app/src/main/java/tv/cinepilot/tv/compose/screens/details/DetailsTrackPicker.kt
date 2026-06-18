package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.tv.compose.components.buttons.TvActionButton
import tv.cinepilot.tv.compose.foundation.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.ui.streamLabel

// ── Focus keys shared with the selector row ─────────────────────────────

private const val FOCUS_SOURCE = "source"
private const val FOCUS_AUDIO = "audio"
private const val FOCUS_SUBTITLE = "subtitle"
private const val SUBTITLES_OFF_INDEX = -1

/** Kind of track being edited. Drives the sheet title + row content. */
internal enum class TrackPickerKind { SOURCE, AUDIO, SUBTITLE }

// ── Side sheet: right-aligned glass panel with a close button ───────────

@Composable
internal fun TrackOptionPickerSheet(
    palette: CinePilotPalette,
    kind: TrackPickerKind,
    sources: List<MediaSourceInfo>,
    activeSource: MediaSourceInfo,
    selection: DetailTrackSelection,
    onClose: () -> Unit,
    onPick: (DetailTrackSelection) -> Unit,
) {
    val dimInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = dimInteraction,
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .width(PANEL_WIDTH_DP.dp)
                .fillMaxSize()
                .padding(vertical = TvDp.ScreenTop - 2.dp)
                .padding(end = TvDp.ScreenX),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(TvDp.PanelRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.glass.copy(alpha = 0.96f),
                                palette.glass.copy(alpha = 0.92f),
                            ),
                        ),
                    )
                    .border(0.8.dp, palette.glassBorder, RoundedCornerShape(TvDp.PanelRadius)),
            ) {
                PickerHeader(palette, kind)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(pickerItems(kind, sources, activeSource, selection)) { row ->
                        PickerOptionRow(
                            palette = palette,
                            row = row,
                            onPick = { onPick(row.nextSelection) },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    TvActionButton(
                        palette = palette,
                        label = "关闭",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClose,
                    )
                }
            }
        }
    }
}

private const val PANEL_WIDTH_DP = 420

// ── Header + row composables ────────────────────────────────────────────

@Composable
private fun PickerHeader(palette: CinePilotPalette, kind: TrackPickerKind) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        BasicText(
            text = pickerTitle(kind),
            maxLines = 1,
            style = TextStyle(
                color = palette.textPrimary,
                fontSize = TvText.PageTitle,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

internal data class PickerRow(
    val label: String,
    val selected: Boolean,
    val requestFocus: Boolean,
    val nextSelection: DetailTrackSelection,
)

@Composable
private fun PickerOptionRow(
    palette: CinePilotPalette,
    row: PickerRow,
    onPick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        selected = row.selected,
        requestInitialFocus = row.requestFocus,
        radius = TvDp.ControlRadius,
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = onPick,
    ) { _ ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = row.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.Body,
                    fontWeight = if (row.selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            if (row.selected) {
                Image(
                    painter = painterResource(tv.cinepilot.tv.R.drawable.ic_check),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(palette.accentStrong),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Title + item builders ───────────────────────────────────────────────

private fun pickerTitle(kind: TrackPickerKind): String = when (kind) {
    TrackPickerKind.SOURCE -> "选择媒体源"
    TrackPickerKind.AUDIO -> "选择音轨"
    TrackPickerKind.SUBTITLE -> "选择字幕"
}

internal fun pickerItems(
    kind: TrackPickerKind,
    sources: List<MediaSourceInfo>,
    activeSource: MediaSourceInfo,
    selection: DetailTrackSelection,
): List<PickerRow> {
    return when (kind) {
        TrackPickerKind.SOURCE -> sources.map { source ->
            val selected = source.id() == activeSource.id()
            PickerRow(
                label = selectorSourceValue(source),
                selected = selected,
                requestFocus = selected,
                nextSelection = DetailTrackSelection(mediaSourceId = source.id(), focusKey = FOCUS_SOURCE),
            )
        }
        TrackPickerKind.AUDIO -> buildAudioRows(activeSource, selection)
        TrackPickerKind.SUBTITLE -> buildSubtitleRows(activeSource, selection)
    }
}

private fun buildAudioRows(
    source: MediaSourceInfo,
    selection: DetailTrackSelection,
): List<PickerRow> {
    val streams = source.streamsOfType(MediaStreamType.AUDIO)
    val defaultLabel = selectorDefaultLabel(streams)
    val rows = mutableListOf<PickerRow>()
    rows += PickerRow(
        label = defaultLabel,
        selected = !selection.audioSelected,
        requestFocus = !selection.audioSelected,
        nextSelection = selection.forSource(source.id()).copy(
            audioSelected = false,
            audioStreamIndex = null,
            focusKey = FOCUS_AUDIO,
        ),
    )
    streams.forEach { stream ->
        val selected = selection.audioSelected && selection.audioStreamIndex == stream.index()
        rows += PickerRow(
            label = streamLabel(stream),
            selected = selected,
            requestFocus = selected,
            nextSelection = selection.forSource(source.id()).copy(
                audioSelected = true,
                audioStreamIndex = stream.index(),
                focusKey = FOCUS_AUDIO,
            ),
        )
    }
    return rows
}

private fun buildSubtitleRows(
    source: MediaSourceInfo,
    selection: DetailTrackSelection,
): List<PickerRow> {
    val streams = source.streamsOfType(MediaStreamType.SUBTITLE)
    val defaultLabel = selectorDefaultLabel(streams)
    val rows = mutableListOf<PickerRow>()
    rows += PickerRow(
        label = defaultLabel,
        selected = !selection.subtitleSelected,
        requestFocus = !selection.subtitleSelected,
        nextSelection = selection.forSource(source.id()).copy(
            subtitleSelected = false,
            subtitleStreamIndex = null,
            burnSubtitleWhenTranscoding = false,
            focusKey = FOCUS_SUBTITLE,
        ),
    )
    rows += PickerRow(
        label = "关闭字幕",
        selected = selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX,
        requestFocus = selection.subtitleSelected && selection.subtitleStreamIndex == SUBTITLES_OFF_INDEX,
        nextSelection = selection.forSource(source.id()).copy(
            subtitleSelected = true,
            subtitleStreamIndex = SUBTITLES_OFF_INDEX,
            burnSubtitleWhenTranscoding = false,
            focusKey = FOCUS_SUBTITLE,
        ),
    )
    streams.forEach { stream ->
        val selected = selection.subtitleSelected && selection.subtitleStreamIndex == stream.index()
        rows += PickerRow(
            label = streamLabel(stream),
            selected = selected,
            requestFocus = selected,
            nextSelection = selection.forSource(source.id()).copy(
                subtitleSelected = true,
                subtitleStreamIndex = stream.index(),
                burnSubtitleWhenTranscoding = stream.requiresBurnInWhenTranscoding(),
                focusKey = FOCUS_SUBTITLE,
            ),
        )
    }
    return rows
}

private fun DetailTrackSelection.forSource(sourceId: String): DetailTrackSelection {
    return if (mediaSourceId == sourceId) this else DetailTrackSelection(mediaSourceId = sourceId)
}
