package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.settingsGridSection
import tv.cinepilot.tv.ui.settingsOptionRow
import tv.cinepilot.tv.ui.settingsPage
import tv.cinepilot.tv.ui.settingsToggleRow

fun ComponentActivity.settingsRouteScreen(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    homeSettings: HomeSettings,
    onHomeSettings: (HomeSettings) -> Unit,
): View {
    return settingsPage(
        title = "设置",
        subtitle = "外观 / 首页 / Android TV 主屏",
    ) {
        addView(settingsGridSection(
            title = "外观",
            rows = listOf(
                settingsOptionRow(
                    label = "主题",
                    description = theme.description,
                    selectedLabel = theme.label,
                    options = AppTheme.values().map { option ->
                        TvOptionSelectItem(option.label, option == theme) {
                            onTheme(option)
                        }
                    },
                    requestFocus = true,
                ),
            ),
        ))
        addView(settingsGridSection(
            title = "首页",
            rows = listOf(
                settingsToggleRow(
                    label = "智能合集行",
                    description = "首页展开精选合集内容",
                    checked = homeSettings.showSmartCollections,
                ) { next ->
                    onHomeSettings(homeSettings.copy(showSmartCollections = next))
                },
            ),
        ))
        addView(settingsGridSection(
            title = "Android TV 主屏",
            rows = listOf(
                settingsToggleRow(
                    label = "继续观看通道",
                    description = "同步最近播放项",
                    checked = homeSettings.showContinueWatchingInLauncher,
                ) { next ->
                    onHomeSettings(homeSettings.copy(showContinueWatchingInLauncher = next))
                },
                settingsToggleRow(
                    label = "下一集通道",
                    description = "同步待看剧集",
                    checked = homeSettings.showNextUpInLauncher,
                ) { next ->
                    onHomeSettings(homeSettings.copy(showNextUpInLauncher = next))
                },
            ),
        ))
    }
}
