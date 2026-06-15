package tv.cinepilot.tv.compose.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText

@Composable
internal fun ProgressBarRow(
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
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp),
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            )
            // Played track
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.accentStrong),
            )
            // Thumb with glow
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction)
                        .height(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(14.dp)
                            .shadow(
                                elevation = 12.dp,
                                spotColor = palette.focusGlow,
                                ambientColor = palette.focusGlow,
                                shape = RoundedCornerShape(7.dp),
                            )
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
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

@Composable
internal fun ChapterStrip(
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

/**
 * Formats a ticks value into a HH:MM:SS clock string for the player OSD.
 */
internal fun formatPlayerClock(ticks: Long): String {
    val totalSeconds = MediaTicks.toSeconds(ticks.coerceAtLeast(0L))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
