package tv.cinepilot.tv.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

private const val PREFERENCES_NAME = "cinepilot_home"

private val Context.homeSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinepilot_home_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, PREFERENCES_NAME))
    },
)

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
    private val dataStore = context.applicationContext.homeSettingsDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun load(): HomeSettings {
        val defaults = HomeSettings.defaults()
        return read { preferences ->
            HomeSettings(
                showSmartCollections = preferences[KEY_SMART_COLLECTIONS]
                    ?: defaults.showSmartCollections,
                showContinueWatchingInLauncher = preferences[KEY_LAUNCHER_CONTINUE_WATCHING]
                    ?: defaults.showContinueWatchingInLauncher,
                showNextUpInLauncher = preferences[KEY_LAUNCHER_NEXT_UP]
                    ?: defaults.showNextUpInLauncher,
            )
        }
    }

    fun save(settings: HomeSettings) {
        write { preferences ->
            preferences[KEY_SMART_COLLECTIONS] = settings.showSmartCollections
            preferences[KEY_LAUNCHER_CONTINUE_WATCHING] = settings.showContinueWatchingInLauncher
            preferences[KEY_LAUNCHER_NEXT_UP] = settings.showNextUpInLauncher
        }
    }

    fun save(block: (HomeSettings) -> HomeSettings) {
        save(block(load()))
    }

    val flow: Flow<HomeSettings> = dataStore.data.map { prefs ->
        val defaults = HomeSettings.defaults()
        HomeSettings(
            showSmartCollections = prefs[KEY_SMART_COLLECTIONS]
                ?: defaults.showSmartCollections,
            showContinueWatchingInLauncher = prefs[KEY_LAUNCHER_CONTINUE_WATCHING]
                ?: defaults.showContinueWatchingInLauncher,
            showNextUpInLauncher = prefs[KEY_LAUNCHER_NEXT_UP]
                ?: defaults.showNextUpInLauncher,
        )
    }

    val stateFlow: StateFlow<HomeSettings> by lazy {
        flow.stateIn(storeScope, SharingStarted.Eagerly, load())
    }

    private fun <T> read(block: (Preferences) -> T): T = runBlocking(Dispatchers.IO) {
        dataStore.data.map(block).first()
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { preferences -> block(preferences) }
        }
    }

    private companion object {
        val KEY_SMART_COLLECTIONS = booleanPreferencesKey("smartCollections")
        val KEY_LAUNCHER_CONTINUE_WATCHING = booleanPreferencesKey("launcherContinueWatching")
        val KEY_LAUNCHER_NEXT_UP = booleanPreferencesKey("launcherNextUp")
    }
}
