package tv.cinepilot.tv.compose.screens.playbacksettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
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
        // Subtitle streams
        subtitleStreams.orEmpty().forEach { stream ->
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
