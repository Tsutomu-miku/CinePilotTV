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
internal fun DisplayModeSection(
    palette: CinePilotPalette,
    current: PlaybackSettings,
    focusGroup: PlaybackSettingsFocusGroup,
    onUpdate: (PlaybackSettingsFocusGroup, (PlaybackSettings) -> PlaybackSettings) -> Unit,
) {
    Column {
        SectionTitle(palette, "显示模式")
        // 画面比例
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_aspect_ratio,
            label = "画面比例",
            value = "原始比例 16:9",
            onClick = { /* TODO: aspect ratio selection */ },
        )
        Spacer(Modifier.height(6.dp))
        // 画面缩放
        IconSettingRow(
            palette = palette,
            iconRes = R.drawable.ic_zoom,
            label = "画面缩放",
            value = "默认",
            onClick = { /* TODO: zoom selection */ },
        )
        Spacer(Modifier.height(6.dp))
        // 自动匹配刷新率
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_refresh,
            label = "自动匹配刷新率",
            description = "按片源帧率切换显示模式",
            checked = current.autoFrameMatching,
            requestInitialFocus = focusGroup == PlaybackSettingsFocusGroup.AFM,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(autoFrameMatching = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        // 匹配色彩空间
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_eye,
            label = "匹配色彩空间",
            description = "HDR / SDR 自动匹配",
            checked = current.matchColorSpace,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(matchColorSpace = checked)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        // 切换前确认
        IconToggleRow(
            palette = palette,
            iconRes = R.drawable.ic_check,
            label = "切换前确认",
            description = "切换显示模式前确认",
            checked = current.confirmBeforeFrameSwitch,
            onCheckedChange = { checked ->
                onUpdate(PlaybackSettingsFocusGroup.AFM) {
                    it.copy(
                        confirmBeforeFrameSwitch = checked,
                        skipFrameSwitchConfirm = if (checked) false else it.skipFrameSwitchConfirm,
                    )
                }
            },
        )
        if (current.skipFrameSwitchConfirm) {
            Spacer(Modifier.height(6.dp))
            IconSettingRow(
                palette = palette,
                iconRes = R.drawable.ic_refresh,
                label = "恢复确认提示",
                description = "重新显示切换确认",
                value = "恢复",
                onClick = {
                    onUpdate(PlaybackSettingsFocusGroup.AFM) {
                        it.copy(skipFrameSwitchConfirm = false)
                    }
                },
            )
        }
    }
}
