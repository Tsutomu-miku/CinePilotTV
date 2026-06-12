package tv.cinepilot.tv.home

import android.content.Context

data class HomeSettings(
    val showSmartCollections: Boolean,
    val showContinueWatchingInLauncher: Boolean,
    val showNextUpInLauncher: Boolean,
) {
    companion object {
        fun defaults() = HomeSettings(
            showSmartCollections = true,
            showContinueWatchingInLauncher = true,
            showNextUpInLauncher = true,
        )
    }
}

/**
 * Persists user toggles for home-screen presentation. Settings here are kept
 * intentionally light-weight and UI-only -- they never affect protocol or
 * session behaviour. The store lives in the home package so it can be shared
 * by the home route controller and the Android TV launcher channel sync.
 */
class HomeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("cinepilot_home", Context.MODE_PRIVATE)

    fun load(): HomeSettings {
        return HomeSettings(
            showSmartCollections = bool("smartCollections",
                HomeSettings.defaults().showSmartCollections),
            showContinueWatchingInLauncher = bool("launcherContinueWatching",
                HomeSettings.defaults().showContinueWatchingInLauncher),
            showNextUpInLauncher = bool("launcherNextUp",
                HomeSettings.defaults().showNextUpInLauncher),
        )
    }

    fun save(settings: HomeSettings) {
        prefs.edit()
            .putBoolean("smartCollections", settings.showSmartCollections)
            .putBoolean("launcherContinueWatching", settings.showContinueWatchingInLauncher)
            .putBoolean("launcherNextUp", settings.showNextUpInLauncher)
            .apply()
    }

    fun save(block: (HomeSettings) -> HomeSettings) {
        save(block(load()))
    }

    private fun bool(key: String, default: Boolean) = prefs.getBoolean(key, default)
}
