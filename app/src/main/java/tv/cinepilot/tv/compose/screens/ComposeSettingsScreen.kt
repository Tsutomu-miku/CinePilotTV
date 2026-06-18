package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.components.layout.SettingsGrid
import tv.cinepilot.tv.compose.components.buttons.TvActionButton
import tv.cinepilot.tv.compose.components.settings.TvOptionRow
import tv.cinepilot.tv.compose.components.settings.TvToggleRow
import tv.cinepilot.tv.compose.screens.settings.BangumiPluginSettingsScreen
import tv.cinepilot.tv.compose.screens.settings.PluginSettingsSection
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.settings.AppTheme

@Composable
fun ComposeSettingsScreen(
    palette: CinePilotPalette,
    theme: AppTheme,
    homeSettings: HomeSettings,
    pluginHost: PluginHost,
    onTheme: (AppTheme) -> Unit,
    onHomeSettings: (HomeSettings) -> Unit,
    onOpenPlugin: (PluginHost.PluginInfo) -> Unit,
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
        Spacer(Modifier.height(2.dp))
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
        Spacer(Modifier.height(2.dp))
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
        Spacer(Modifier.height(2.dp))
        PluginSettingsSection(
            palette = palette,
            pluginHost = pluginHost,
            onOpenPlugin = onOpenPlugin,
        )
    }
}
