package tv.cinepilot.tv.compose.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette

@Composable
internal fun PlayerBottomOsd(
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
    subtitleShortcutLabel: String,
    audioShortcutLabel: String,
    qualityLabel: String,
    speedLabel: String,
    onSubtitlesShortcut: () -> Unit,
    onAudioShortcut: () -> Unit,
    onQualityShortcut: () -> Unit,
    onSpeedShortcut: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
    ) {
        // Title
        BasicText(
            text = listOf(mediaTitle, mediaSubtitle).filter { it.isNotBlank() }.joinToString("  ·  "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.textPrimary.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            ),
        )
        // Chapter strip — rendered between title and controls when chapters exist.
        if (chapters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            PlayerChapterStrip(
                palette = palette,
                chapters = chapters,
                positionTicks = positionTicks,
                onChapterClick = onChapterClick,
            )
        }
        Spacer(Modifier.height(12.dp))
        // Control row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Seek30Button(
                palette = palette,
                forward = false,
                onClick = onSeekBack,
            )
            Spacer(Modifier.width(18.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_skip_previous,
                size = 40.dp,
                iconSize = 16.dp,
                onClick = onSeekBack,
            )
            Spacer(Modifier.width(16.dp))
            PlayPauseButton(
                palette = palette,
                isPlaying = false,
                onClick = onTogglePlayPause,
            )
            Spacer(Modifier.width(16.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_skip_next,
                size = 40.dp,
                iconSize = 16.dp,
                onClick = onSeekForward,
            )
            Spacer(Modifier.width(18.dp))
            Seek30Button(
                palette = palette,
                forward = true,
                onClick = onSeekForward,
            )
            Spacer(Modifier.width(64.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_subtitles,
                size = 34.dp,
                iconSize = 15.dp,
                onClick = onSubtitlesShortcut,
                badgeLabel = subtitleShortcutLabel,
            )
            Spacer(Modifier.width(10.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_audio,
                size = 34.dp,
                iconSize = 15.dp,
                onClick = onAudioShortcut,
                badgeLabel = audioShortcutLabel,
            )
            Spacer(Modifier.width(10.dp))
            TextOsdButton(
                palette = palette,
                text = qualityLabel,
                onClick = onQualityShortcut,
            )
            Spacer(Modifier.width(10.dp))
            TextOsdButton(
                palette = palette,
                text = speedLabel,
                onClick = onSpeedShortcut,
            )
            Spacer(Modifier.width(10.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_info,
                size = 34.dp,
                iconSize = 15.dp,
                onClick = onInfo,
            )
            Spacer(Modifier.width(10.dp))
            IconOsdButton(
                palette = palette,
                iconRes = R.drawable.ic_settings,
                size = 34.dp,
                iconSize = 15.dp,
                onClick = onSettings,
            )
        }
        Spacer(Modifier.height(14.dp))
        ProgressBarRow(
            palette = palette,
            positionTicks = positionTicks,
            durationTicks = durationTicks,
        )
    }
}
