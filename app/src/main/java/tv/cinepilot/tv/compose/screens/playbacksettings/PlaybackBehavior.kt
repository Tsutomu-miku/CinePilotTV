package tv.cinepilot.tv.compose.screens.playbacksettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.playback.PlaybackSettings
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup

@Composable
internal fun AutoPlaySection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "连播与预览")
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_play_next_row,
            label = "自动连播",
            description = "剧集末段显示下一集",
            checked = current.autoPlayNext,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AUTO_PLAY,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AUTO_PLAY) {
                    it.copy(autoPlayNext = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "下一集预览",
            value = "30秒",
            onClick = {},
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "章节列表",
            description = "底部显示章节 chips",
            checked = current.showChapterStrip,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.CHAPTERS,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.CHAPTERS) {
                    it.copy(showChapterStrip = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_thumbnail,
            label = "缩略图预览",
            description = "保留设置，预览后续接入",
            checked = current.showTrickplayPreview,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.TRICKPLAY,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.TRICKPLAY) {
                    it.copy(showTrickplayPreview = checked)
                }
            },
        )
    }
}

@Composable
internal fun IntroCreditsSection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "片头 / 片尾")
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "跳过片头",
            checked = current.showIntroSkipButton,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.INTRO_SKIP,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(showIntroSkipButton = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "自动跳片头",
            checked = current.autoSkipIntro,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(autoSkipIntro = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "跳过片尾",
            checked = current.showCreditsSkipButton,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(showCreditsSkipButton = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_skip_forward_row,
            label = "自动跳片尾",
            checked = current.autoSkipCredits,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.INTRO_SKIP) {
                    it.copy(autoSkipCredits = checked)
                }
            },
        )
    }
}
