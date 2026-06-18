package tv.cinepilot.tv.compose.screens.playbacksettings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.playback.PlaybackSettings
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup
import tv.cinepilot.tv.playback.SubtitleEncoding
import tv.cinepilot.tv.ui.streamLabel

@Composable
internal fun StreamSettingsSection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    audioStreams: List<MediaStreamInfo>?,
    currentAudioStreamIndex: Int?,
    subtitleStreams: List<MediaStreamInfo>?,
    currentSubtitleStreamIndex: Int?,
    onEncoding: (SubtitleEncoding) -> Unit,
    onBurnGraphicSubtitle: (Boolean) -> Unit,
    onAudioStreamChanged: (Int?) -> Unit,
    onSubtitleStreamChanged: (Int?) -> Unit,
) {
    Column {
        SectionTitle(palette, "音轨与字幕")
        // Audio streams
        audioStreams.orEmpty().forEach { stream ->
            val selected = stream.index() == currentAudioStreamIndex
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_audio,
                label = "音轨：${streamLabel(stream)}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUDIO_TRACK && selected,
                onClick = { onAudioStreamChanged(stream.index()) },
            )
            Spacer(Modifier.height(6.dp))
        }
        // Subtitle off option
        val subtitlesOff = currentSubtitleStreamIndex == -1
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_subtitle_row,
            label = "字幕：关闭",
            value = if (subtitlesOff) "当前" else "切换",
            selected = subtitlesOff,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && subtitlesOff,
            onClick = { onSubtitleStreamChanged(-1) },
        )
        Spacer(Modifier.height(6.dp))
        // Split subtitle streams into external (online) and server-provided groups.
        val all = subtitleStreams.orEmpty()
        val external = all.filter { it.external() }
        val server = all.filter { !it.external() }

        @Composable
        fun renderStream(stream: MediaStreamInfo) {
            val selected = stream.index() == currentSubtitleStreamIndex
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_subtitle_row,
                label = "字幕：${streamLabel(stream)}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_TRACK && selected,
                onClick = { onSubtitleStreamChanged(stream.index()) },
            )
            Spacer(Modifier.height(6.dp))
        }

        if (external.isNotEmpty()) {
            SubtitleGroupHeader(palette, "在线字幕")
            external.forEach { renderStream(it) }
        }
        if (server.isNotEmpty()) {
            SubtitleGroupHeader(
                palette,
                if (external.isNotEmpty()) "服务器字幕" else "字幕",
            )
            server.forEach { renderStream(it) }
        }
        // Subtitle encoding
        SubtitleEncoding.values().forEach { encoding ->
            val selected = encoding == current.subtitleEncoding
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_subtitle_row,
                label = "字幕编码：${encoding.label}",
                value = if (selected) "当前" else "切换",
                selected = selected,
                requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.SUBTITLE_ENCODING && selected,
                onClick = { onEncoding(encoding) },
            )
            Spacer(Modifier.height(6.dp))
        }
        // Burn graphic subtitle
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_subtitle_row,
            label = "图形字幕烧录",
            description = "转码时由服务器烧录",
            checked = current.burnGraphicSubtitleWhenTranscoding,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.BURN_GRAPHIC_SUBTITLE,
            onCheckedChange = onBurnGraphicSubtitle,
        )
    }
}

/** Small, subtly-coloured label used to separate online / server subtitle groups. */
@Composable
private fun SubtitleGroupHeader(
    palette: CinePilotPalette,
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = palette.accentStrong,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(0.5.dp)
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.accentStrong.copy(alpha = 0.25f),
                            palette.glassBorder.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
    Spacer(Modifier.height(8.dp))
}
