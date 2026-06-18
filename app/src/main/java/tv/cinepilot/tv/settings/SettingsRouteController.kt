package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.screens.ComposeSettingsScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettings
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

    private fun render(
        themeSnapshot: AppTheme? = null,
        homeSettingsSnapshot: HomeSettings? = null,
    ) {
        val theme = themeSnapshot ?: settingsStore.stateFlow.value
        val homeSettings = homeSettingsSnapshot ?: homeSettingsStore.stateFlow.value
        TvColors.applyTheme(theme.id)
        renderCompose("设置") { palette ->
            ComposeSettingsScreen(
                palette = palette,
                theme = theme,
                homeSettings = homeSettings,
                onTheme = { selected ->
                    activity.lifecycleScope.launch {
                        settingsStore.saveThemeAsync(selected)
                    }
                    TvColors.applyTheme(selected.id)
                    render(themeSnapshot = selected, homeSettingsSnapshot = homeSettings)
                },
                onHomeSettings = { next ->
                    activity.lifecycleScope.launch {
                        homeSettingsStore.saveAsync(next)
                    }
                    render(themeSnapshot = theme, homeSettingsSnapshot = next)
                },
            )
        }
    }
}
