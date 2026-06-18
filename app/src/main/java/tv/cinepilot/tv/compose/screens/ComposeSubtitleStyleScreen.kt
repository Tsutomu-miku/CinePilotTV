package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.components.TvOptionRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.playback.LabeledSubtitleOption
import tv.cinepilot.tv.playback.SubtitleBackground
import tv.cinepilot.tv.playback.SubtitleBottomMargin
import tv.cinepilot.tv.playback.SubtitleEdgeStyle
import tv.cinepilot.tv.playback.SubtitleFontFamily
import tv.cinepilot.tv.playback.SubtitleStyleFocusGroup
import tv.cinepilot.tv.playback.SubtitleStylePreferences
import tv.cinepilot.tv.playback.SubtitleTextColor
import tv.cinepilot.tv.playback.SubtitleTextOpacity
import tv.cinepilot.tv.playback.SubtitleTextSize

@Composable
fun ComposeSubtitleStyleScreen(
    palette: CinePilotPalette,
    current: SubtitleStylePreferences,
    focusGroup: SubtitleStyleFocusGroup,
    onSize: (SubtitleTextSize) -> Unit,
    onColor: (SubtitleTextColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onFont: (SubtitleFontFamily) -> Unit,
    onEdgeStyle: (SubtitleEdgeStyle) -> Unit,
    onMargin: (SubtitleBottomMargin) -> Unit,
    onOpacity: (SubtitleTextOpacity) -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleOptionGrid(
            palette = palette,
            title = "字号",
            options = SubtitleTextSize.entries,
            current = current.size,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.SIZE,
            onSelected = onSize,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "颜色",
            options = SubtitleTextColor.entries,
            current = current.color,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.COLOR,
            onSelected = onColor,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "背景 / 投影",
            options = SubtitleBackground.entries,
            current = current.background,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.BACKGROUND,
            onSelected = onBackground,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "字体",
            options = SubtitleFontFamily.entries,
            current = current.fontFamily,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.FONT,
            onSelected = onFont,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "描边样式",
            options = SubtitleEdgeStyle.entries,
            current = current.edgeStyle,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.EDGE,
            onSelected = onEdgeStyle,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "底部位置",
            options = SubtitleBottomMargin.entries,
            current = current.bottomMargin,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.MARGIN,
            onSelected = onMargin,
        )
        SubtitleOptionGrid(
            palette = palette,
            title = "文字不透明度",
            options = SubtitleTextOpacity.entries,
            current = current.textOpacity,
            requestFocus = focusGroup == SubtitleStyleFocusGroup.OPACITY,
            onSelected = onOpacity,
        )
        SettingsGrid(
            palette = palette,
            title = "重置",
            rows = listOf(
                {
                    TvActionButton(
                        palette = palette,
                        label = "恢复默认",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onReset,
                    )
                },
            ),
        )
    }
}

@Composable
private fun <T> SubtitleOptionGrid(
    palette: CinePilotPalette,
    title: String,
    options: Iterable<T>,
    current: T,
    requestFocus: Boolean,
    onSelected: (T) -> Unit,
) where T : Enum<T>, T : LabeledSubtitleOption {
    SettingsGrid(
        palette = palette,
        title = title,
        rows = options.map { option ->
            {
                val selected = option == current
                TvOptionRow(
                    palette = palette,
                    label = option.label,
                    value = if (selected) "当前" else "选择",
                    selected = selected,
                    requestInitialFocus = selected && requestFocus,
                    onClick = { onSelected(option) },
                )
            }
        },
    )
}
