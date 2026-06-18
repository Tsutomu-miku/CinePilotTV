package tv.cinepilot.tv.compose.screens

import android.view.View
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.screens.playbacksettings.ComposePlaybackSettingsScreen as PlaybackSettingsScreenImpl
import tv.cinepilot.tv.playback.PlaybackSettings
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup

/**
 * Playback settings screen facade.
 *
 * The actual implementation lives in [tv.cinepilot.tv.compose.screens.playbacksettings].
 * This file exists to preserve the original public API surface for existing callers.
 */
@Composable
fun ComposePlaybackSettingsScreen(
    palette: CinePilotPalette,
    playerView: View,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    audioStreams: List<MediaStreamInfo>?,
    currentAudioStreamIndex: Int?,
    subtitleStreams: List<MediaStreamInfo>?,
    currentSubtitleStreamIndex: Int?,
    onChanged: (PlaybackSettings, PlaybackSettingsFocusGroup) -> Unit,
    onAudioStreamChanged: (Int?) -> Unit,
    onSubtitleStreamChanged: (Int?) -> Unit,
    onBack: () -> Unit,
) {
    PlaybackSettingsScreenImpl(
        palette = palette,
        playerView = playerView,
        current = current,
        focusGroup = focusGroup,
        audioStreams = audioStreams,
        currentAudioStreamIndex = currentAudioStreamIndex,
        subtitleStreams = subtitleStreams,
        currentSubtitleStreamIndex = currentSubtitleStreamIndex,
        onChanged = onChanged,
        onAudioStreamChanged = onAudioStreamChanged,
        onSubtitleStreamChanged = onSubtitleStreamChanged,
        onBack = onBack,
    )
}
