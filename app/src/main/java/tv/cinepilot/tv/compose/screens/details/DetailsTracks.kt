package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.details.DetailTrackSelection

// ── Focus keys shared with the picker sheet ─────────────────────────────

private const val FOCUS_SOURCE = "source"
private const val FOCUS_AUDIO = "audio"
private const val FOCUS_SUBTITLE = "subtitle"

/**
 * Public entry point: render three clickable track selectors on the details
 * screen and, while any picker is open, overlay the side sheet with options.
 *
 * The selector row is intentionally small and kept in this file; the picker
 * overlay with its row builders lives in DetailsTrackPicker.kt so each file
 * stays under the 300 line soft cap.
 */
@Composable
internal fun DetailTrackOptions(
    palette: CinePilotPalette,
    playbackInfo: PlaybackInfo?,
    selection: DetailTrackSelection,
    onSelection: (DetailTrackSelection) -> Unit,
) {
    val sources = playbackInfo?.mediaSources().orEmpty()
    if (sources.isEmpty()) return

    var openPicker: TrackPickerKind? by remember { mutableStateOf(null) }
    val activeSource = sources.firstOrNull { it.id() == selection.mediaSourceId } ?: sources.first()

    SelectorRow(
        palette = palette,
        selection = selection,
        activeSource = activeSource,
        sources = sources,
        onOpenSource = { openPicker = TrackPickerKind.SOURCE },
        onOpenAudio = { openPicker = TrackPickerKind.AUDIO },
        onOpenSubtitle = { openPicker = TrackPickerKind.SUBTITLE },
    )

    openPicker?.let { kind ->
        TrackOptionPickerSheet(
            palette = palette,
            kind = kind,
            sources = sources,
            activeSource = activeSource,
            selection = selection,
            onClose = { openPicker = null },
            onPick = { next ->
                openPicker = null
                onSelection(next)
            },
        )
    }
}

// ── Three-column clickable selector row ─────────────────────────────────

@Composable
private fun SelectorRow(
    palette: CinePilotPalette,
    selection: DetailTrackSelection,
    activeSource: MediaSourceInfo,
    sources: List<MediaSourceInfo>,
    onOpenSource: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (sources.size > 1) {
                TrackOptionSelector(
                    palette = palette,
                    title = "媒体源",
                    value = selectorSourceValue(activeSource),
                    iconRes = tv.cinepilot.tv.R.drawable.ic_media_source,
                    modifier = Modifier.weight(1f),
                    requestInitialFocus = selection.focusKey == FOCUS_SOURCE,
                    onClick = onOpenSource,
                )
            }
            TrackOptionSelector(
                palette = palette,
                title = "音轨",
                value = selectorAudioValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_volume,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_AUDIO,
                onClick = onOpenAudio,
            )
            TrackOptionSelector(
                palette = palette,
                title = "字幕",
                value = selectorSubtitleValue(activeSource, selection),
                iconRes = tv.cinepilot.tv.R.drawable.ic_subtitles,
                modifier = Modifier.weight(1f),
                requestInitialFocus = selection.focusKey == FOCUS_SUBTITLE,
                onClick = onOpenSubtitle,
            )
        }
    }
}
