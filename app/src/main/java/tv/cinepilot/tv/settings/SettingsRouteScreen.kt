package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.cinematicStage
import tv.cinepilot.tv.ui.compactPanelSpacing
import tv.cinepilot.tv.ui.infusePanelNote
import tv.cinepilot.tv.ui.infusePanelTitle
import tv.cinepilot.tv.ui.optionSelect
import tv.cinepilot.tv.ui.sideSheet
import tv.cinepilot.tv.ui.toggleChoice

fun ComponentActivity.settingsRouteScreen(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    homeSettings: HomeSettings,
    onHomeSettings: (HomeSettings) -> Unit,
): View {
    return cinematicStage(scrollable = false) {
        addView(sideSheet {
            addView(infusePanelTitle("外观"))
            addView(optionSelect(
                title = "主题",
                selectedLabel = theme.label,
                options = AppTheme.values().map { option ->
                    TvOptionSelectItem("${option.label} · ${option.description}", option == theme) {
                        onTheme(option)
                    }
                },
                requestFocus = true,
            ).compactPanelSpacing())
            addView(infusePanelNote("主题会保存到本机，重启应用后继续生效。"))
            addView(infusePanelTitle("首页 / 主屏"))
            addView(toggleChoice(
                label = "智能合集行",
                description = "为每个精选合集自动展开其内容，作为独立的首页行。",
                checked = homeSettings.showSmartCollections,
            ) { next ->
                onHomeSettings(homeSettings.copy(showSmartCollections = next))
            })
            addView(toggleChoice(
                label = "主屏通道：继续观看",
                description = "在 Android TV 主屏的继续观看通道中同步最近播放项。",
                checked = homeSettings.showContinueWatchingInLauncher,
            ) { next ->
                onHomeSettings(homeSettings.copy(showContinueWatchingInLauncher = next))
            })
            addView(toggleChoice(
                label = "主屏通道：下一集",
                description = "在 Android TV 主屏的下一集通道中同步待看剧集。",
                checked = homeSettings.showNextUpInLauncher,
            ) { next ->
                onHomeSettings(homeSettings.copy(showNextUpInLauncher = next))
            })
            addView(infusePanelNote("主屏通道更改会在下一次通道刷新时生效，或重启应用后立即生效。"))
        })
    }
}
