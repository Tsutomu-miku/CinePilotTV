package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.optionSelect
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section

fun ComponentActivity.settingsRouteScreen(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
): View {
    return screen("设置") {
        addView(section("外观"))
        addView(optionSelect(
            title = "主题",
            selectedLabel = theme.label,
            options = AppTheme.values().map { option ->
                TvOptionSelectItem("${option.label} · ${option.description}", option == theme) {
                    onTheme(option)
                }
            },
            requestFocus = true,
        ))
        addView(section("说明"))
        addView(label("主题会保存到本机，重启应用后继续生效。"))
    }
}
