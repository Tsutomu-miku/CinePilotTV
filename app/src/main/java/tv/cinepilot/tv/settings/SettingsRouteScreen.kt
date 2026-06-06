package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.compactPanelSpacing
import tv.cinepilot.tv.ui.infusePanelNote
import tv.cinepilot.tv.ui.infusePanelScreen
import tv.cinepilot.tv.ui.infusePanelTitle
import tv.cinepilot.tv.ui.optionSelect

fun ComponentActivity.settingsRouteScreen(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
): View {
    return infusePanelScreen("设置") {
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
        addView(infusePanelTitle("说明"))
        addView(infusePanelNote("主题会保存到本机，重启应用后继续生效。"))
    }
}
