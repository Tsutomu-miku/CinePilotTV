package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvOptionRow
import tv.cinepilot.tv.compose.components.TvToggleRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.settings.AppTheme

@Composable
fun ComposeSettingsScreen(
    palette: CinePilotPalette,
    theme: AppTheme,
    homeSettings: HomeSettings,
    onTheme: (AppTheme) -> Unit,
    onHomeSettings: (HomeSettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsGrid(
            palette = palette,
            title = "外观",
            rows = AppTheme.values().map { option ->
                {
                    TvOptionRow(
                        palette = palette,
                        label = option.label,
                        value = if (option == theme) "当前" else "切换",
                        description = option.description,
                        selected = option == theme,
                        onClick = { onTheme(option) },
                    )
                }
            },
        )
        SettingsGrid(
            palette = palette,
            title = "首页",
            rows = listOf(
                {
                    TvToggleRow(
                        palette = palette,
                        label = "智能合集行",
                        description = "首页展开精选合集内容",
                        checked = homeSettings.showSmartCollections,
                        onCheckedChange = { checked ->
                            onHomeSettings(homeSettings.copy(showSmartCollections = checked))
                        },
                    )
                },
            ),
        )
        SettingsGrid(
            palette = palette,
            title = "Android TV 主屏",
            rows = listOf(
                {
                    TvToggleRow(
                        palette = palette,
                        label = "继续观看通道",
                        description = "同步最近播放项",
                        checked = homeSettings.showContinueWatchingInLauncher,
                        onCheckedChange = { checked ->
                            onHomeSettings(homeSettings.copy(showContinueWatchingInLauncher = checked))
                        },
                    )
                },
                {
                    TvToggleRow(
                        palette = palette,
                        label = "下一集通道",
                        description = "同步待看剧集",
                        checked = homeSettings.showNextUpInLauncher,
                        onCheckedChange = { checked ->
                            onHomeSettings(homeSettings.copy(showNextUpInLauncher = checked))
                        },
                    )
                },
            ),
        )
    }
}
