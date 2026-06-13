package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.screens.ComposeSettingsScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.ui.TvColors

class SettingsRouteController(
    private val activity: ComponentActivity,
    private val settingsStore: SettingsStore,
    private val homeSettingsStore: HomeSettingsStore,
    private val showHome: (TvAppState) -> Unit,
    private val renderView: (View) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
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
        val homeSettings = homeSettingsStore.load()
        TvColors.applyTheme(theme.id)
        renderCompose("设置") { palette ->
            ComposeSettingsScreen(
                palette = palette,
                theme = theme,
                homeSettings = homeSettings,
                onTheme = { selected ->
                    settingsStore.saveTheme(selected)
                    TvColors.applyTheme(selected.id)
                    render()
                },
                onHomeSettings = { next ->
                    homeSettingsStore.save(next)
                    render()
                },
            )
        }
    }
}
