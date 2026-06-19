package tv.cinepilot.tv.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val PREFERENCES_NAME = "cinepilot_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinepilot_settings_datastore",
    produceMigrations = { context ->
        // Migrates the old getSharedPreferences("cinepilot_settings", MODE_PRIVATE) store.
        listOf(SharedPreferencesMigration(context, PREFERENCES_NAME))
    },
)

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun theme(): AppTheme = stateFlow.value

    fun saveTheme(theme: AppTheme) {
        storeScope.launch { saveThemeAsync(theme) }
    }

    /** Non-blocking save; completes when the DataStore edit has been applied. */
    suspend fun saveThemeAsync(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme.id
        }
    }

    val flow: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.fromId(prefs[KEY_THEME])
    }

    val stateFlow: StateFlow<AppTheme> = flow.stateIn(
        scope = storeScope,
        started = SharingStarted.Eagerly,
        initialValue = AppTheme.defaults(),
    )

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
    }
}
