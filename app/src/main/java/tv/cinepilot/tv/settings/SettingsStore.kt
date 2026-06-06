package tv.cinepilot.tv.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("cinepilot_settings", Context.MODE_PRIVATE)

    fun theme(): AppTheme {
        return AppTheme.fromId(preferences.getString(KEY_THEME, null))
    }

    fun saveTheme(theme: AppTheme) {
        preferences.edit().putString(KEY_THEME, theme.id).apply()
    }

    companion object {
        private const val KEY_THEME = "theme"
    }
}
