package tv.cinepilot.tv.compose.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

/**
 * Horizontal pill strip of chapters, rendered above the OSD control row.
 * The currently-active chapter is highlighted with an accent stroke + fill;
 * focused pills receive the focus ring, matching the rest of the OSD controls.
 */
@Composable
internal fun PlayerChapterStrip(
    palette: CinePilotPalette,
    chapters: List<Pair<Long, String>>,
    positionTicks: Long,
    onChapterClick: (Long) -> Unit,
) {
    val currentChapterIndex = chapterIndexForPosition(chapters, positionTicks)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(chapters, key = { it.first }) { (startTicks, rawName) ->
            val idx = chapters.indexOfFirst { it.first == startTicks }
            val active = idx == currentChapterIndex
            var focused by remember { mutableStateOf(false) }
            val label = chapterButtonLabel(startTicks, rawName)
            val fill = when {
                focused -> palette.glassFocus
                active -> palette.accentStrong.copy(alpha = 0.18f)
                else -> palette.glass
            }
            val border = when {
                focused -> palette.focusRing
                active -> palette.accentStrong
                else -> palette.glassBorder
            }
            val textColor = when {
                focused -> palette.textPrimary
                active -> palette.accentStrong
                else -> palette.textSecondary
            }
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fill)
                    .border(
                        width = if (focused) TvDp.FocusRing else 0.6.dp,
                        color = border,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .focusable(interactionSource = remember { MutableInteractionSource() })
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onChapterClick(startTicks) },
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

/**
 * Format a single chapter button label: "HH:MM:SS  Chapter Name" (truncated).
 * Falls back to a plain timestamp when the chapter has no server-side name.
 */
private fun chapterButtonLabel(startTicks: Long, rawName: String): String = buildString {
    append(MediaTicks.formatShort(startTicks))
    val name = rawName.trim()
    if (name.isNotBlank()) {
        append("  ")
        append(name)
    }
}

/**
 * Locate the chapter that contains [positionTicks], defined as the last
 * chapter whose start time is not greater than the playback position.
 * Returns 0 when the chapters list is empty or the position precedes every
 * chapter marker.
 */
private fun chapterIndexForPosition(
    chapters: List<Pair<Long, String>>,
    positionTicks: Long,
): Int {
    if (chapters.isEmpty()) return 0
    var idx = 0
    while (idx < chapters.lastIndex && chapters[idx + 1].first <= positionTicks) idx++
    return idx
}
