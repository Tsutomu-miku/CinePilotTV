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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

private const val PREFERENCES_NAME = "cinepilot_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinepilot_settings_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, PREFERENCES_NAME))
    },
)

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun theme(): AppTheme {
        return read { preferences ->
            AppTheme.fromId(preferences[KEY_THEME])
        }
    }

    fun saveTheme(theme: AppTheme) {
        write { preferences ->
            preferences[KEY_THEME] = theme.id
        }
    }

    val flow: Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.fromId(prefs[KEY_THEME])
    }

    val stateFlow: StateFlow<AppTheme> by lazy {
        flow.stateIn(storeScope, SharingStarted.Eagerly, theme())
    }

    private fun <T> read(block: (Preferences) -> T): T = runBlocking(Dispatchers.IO) {
        dataStore.data.map(block).first()
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { preferences -> block(preferences) }
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
    }
}
