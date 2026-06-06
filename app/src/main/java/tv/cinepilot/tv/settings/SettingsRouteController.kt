package tv.cinepilot.tv.settings

import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.TvColors

class SettingsRouteController(
    private val activity: ComponentActivity,
    private val settingsStore: SettingsStore,
    private val showHome: (TvAppState) -> Unit,
) {
    private var returnState: TvAppState? = null
    private var visible = false

    fun show(state: TvAppState) {
        returnState = state
        visible = true
        render()
    }

    fun hide() {
        visible = false
    }

    fun closeIfVisible(): Boolean {
        if (!visible) {
            return false
        }
        visible = false
        returnState?.let(showHome)
        return true
    }

    private fun render() {
        val theme = settingsStore.theme()
        TvColors.applyTheme(theme.id)
        activity.setContentView(activity.settingsRouteScreen(
            theme = theme,
            onTheme = { selected ->
                settingsStore.saveTheme(selected)
                TvColors.applyTheme(selected.id)
                render()
            },
        ))
    }
}
