package tv.cinepilot.tv.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

private const val PLAYBACK_PREFERENCES_NAME = "playback_settings"

private val Context.playbackSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_settings_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, PLAYBACK_PREFERENCES_NAME))
    },
)

data class PlaybackSettings(
    val autoFrameMatching: Boolean,
    val matchColorSpace: Boolean,
    val confirmBeforeFrameSwitch: Boolean,
    val skipFrameSwitchConfirm: Boolean,
    val autoPlayNext: Boolean,
    val autoSkipIntro: Boolean,
    val autoSkipCredits: Boolean,
    val showIntroSkipButton: Boolean,
    val showCreditsSkipButton: Boolean,
    val showTrickplayPreview: Boolean,
    val showChapterStrip: Boolean,
    val subtitleEncoding: SubtitleEncoding,
    val burnGraphicSubtitleWhenTranscoding: Boolean,
) {
    companion object {
        fun defaults(): PlaybackSettings {
            return PlaybackSettings(
                autoFrameMatching = false,
                matchColorSpace = true,
                confirmBeforeFrameSwitch = false,
                skipFrameSwitchConfirm = false,
                autoPlayNext = true,
                autoSkipIntro = false,
                autoSkipCredits = false,
                showIntroSkipButton = true,
                showCreditsSkipButton = true,
                showTrickplayPreview = true,
                showChapterStrip = true,
                subtitleEncoding = SubtitleEncoding.AUTO,
                burnGraphicSubtitleWhenTranscoding = true,
            )
        }
    }
}

enum class SubtitleEncoding(override val label: String) : LabeledSubtitleOption {
    AUTO("自动探测 (UTF-8 / GBK / BIG5)"),
    UTF8("强制 UTF-8"),
    GBK("强制 GBK (简中)"),
    BIG5("强制 BIG5 (繁中)"),
    SHIFT_JIS("强制 Shift_JIS (日文)"),
    EUC_KR("强制 EUC-KR (韩文)"),
}

class PlaybackSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.playbackSettingsDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun current(): PlaybackSettings {
        val defaults = PlaybackSettings.defaults()
        return read { preferences ->
            PlaybackSettings(
                autoFrameMatching = preferences[KEY_AFM] ?: defaults.autoFrameMatching,
                matchColorSpace = preferences[KEY_MATCH_COLOR] ?: defaults.matchColorSpace,
                confirmBeforeFrameSwitch = preferences[KEY_CONFIRM_BEFORE_FRAME_SWITCH]
                    ?: defaults.confirmBeforeFrameSwitch,
                skipFrameSwitchConfirm = preferences[KEY_SKIP_FRAME_SWITCH_CONFIRM]
                    ?: defaults.skipFrameSwitchConfirm,
                autoPlayNext = preferences[KEY_AUTO_PLAY_NEXT] ?: defaults.autoPlayNext,
                autoSkipIntro = preferences[KEY_AUTO_SKIP_INTRO] ?: defaults.autoSkipIntro,
                autoSkipCredits = preferences[KEY_AUTO_SKIP_CREDITS] ?: defaults.autoSkipCredits,
                showIntroSkipButton = preferences[KEY_SHOW_INTRO_SKIP] ?: defaults.showIntroSkipButton,
                showCreditsSkipButton = preferences[KEY_SHOW_CREDITS_SKIP] ?: defaults.showCreditsSkipButton,
                showTrickplayPreview = preferences[KEY_TRICKPLAY] ?: defaults.showTrickplayPreview,
                showChapterStrip = preferences[KEY_CHAPTER_STRIP] ?: defaults.showChapterStrip,
                subtitleEncoding = enumValue(preferences[KEY_SUBTITLE_ENCODING], defaults.subtitleEncoding),
                burnGraphicSubtitleWhenTranscoding = preferences[KEY_BURN_GRAPHIC_SUBTITLE]
                    ?: defaults.burnGraphicSubtitleWhenTranscoding,
            )
        }
    }

    fun save(value: PlaybackSettings) {
        write { preferences ->
            preferences[KEY_AFM] = value.autoFrameMatching
            preferences[KEY_MATCH_COLOR] = value.matchColorSpace
            preferences[KEY_CONFIRM_BEFORE_FRAME_SWITCH] = value.confirmBeforeFrameSwitch
            preferences[KEY_SKIP_FRAME_SWITCH_CONFIRM] = value.skipFrameSwitchConfirm
            preferences[KEY_AUTO_PLAY_NEXT] = value.autoPlayNext
            preferences[KEY_AUTO_SKIP_INTRO] = value.autoSkipIntro
            preferences[KEY_AUTO_SKIP_CREDITS] = value.autoSkipCredits
            preferences[KEY_SHOW_INTRO_SKIP] = value.showIntroSkipButton
            preferences[KEY_SHOW_CREDITS_SKIP] = value.showCreditsSkipButton
            preferences[KEY_TRICKPLAY] = value.showTrickplayPreview
            preferences[KEY_CHAPTER_STRIP] = value.showChapterStrip
            preferences[KEY_SUBTITLE_ENCODING] = value.subtitleEncoding.name
            preferences[KEY_BURN_GRAPHIC_SUBTITLE] = value.burnGraphicSubtitleWhenTranscoding
        }
    }

    /** Non-blocking save; completes when the DataStore edit has been applied. */
    suspend fun saveAsync(value: PlaybackSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_AFM] = value.autoFrameMatching
            preferences[KEY_MATCH_COLOR] = value.matchColorSpace
            preferences[KEY_CONFIRM_BEFORE_FRAME_SWITCH] = value.confirmBeforeFrameSwitch
            preferences[KEY_SKIP_FRAME_SWITCH_CONFIRM] = value.skipFrameSwitchConfirm
            preferences[KEY_AUTO_PLAY_NEXT] = value.autoPlayNext
            preferences[KEY_AUTO_SKIP_INTRO] = value.autoSkipIntro
            preferences[KEY_AUTO_SKIP_CREDITS] = value.autoSkipCredits
            preferences[KEY_SHOW_INTRO_SKIP] = value.showIntroSkipButton
            preferences[KEY_SHOW_CREDITS_SKIP] = value.showCreditsSkipButton
            preferences[KEY_TRICKPLAY] = value.showTrickplayPreview
            preferences[KEY_CHAPTER_STRIP] = value.showChapterStrip
            preferences[KEY_SUBTITLE_ENCODING] = value.subtitleEncoding.name
            preferences[KEY_BURN_GRAPHIC_SUBTITLE] = value.burnGraphicSubtitleWhenTranscoding
        }
    }

    val flow: Flow<PlaybackSettings> = dataStore.data.map { prefs ->
        val defaults = PlaybackSettings.defaults()
        PlaybackSettings(
            autoFrameMatching = prefs[KEY_AFM] ?: defaults.autoFrameMatching,
            matchColorSpace = prefs[KEY_MATCH_COLOR] ?: defaults.matchColorSpace,
            confirmBeforeFrameSwitch = prefs[KEY_CONFIRM_BEFORE_FRAME_SWITCH]
                ?: defaults.confirmBeforeFrameSwitch,
            skipFrameSwitchConfirm = prefs[KEY_SKIP_FRAME_SWITCH_CONFIRM]
                ?: defaults.skipFrameSwitchConfirm,
            autoPlayNext = prefs[KEY_AUTO_PLAY_NEXT] ?: defaults.autoPlayNext,
            autoSkipIntro = prefs[KEY_AUTO_SKIP_INTRO] ?: defaults.autoSkipIntro,
            autoSkipCredits = prefs[KEY_AUTO_SKIP_CREDITS] ?: defaults.autoSkipCredits,
            showIntroSkipButton = prefs[KEY_SHOW_INTRO_SKIP] ?: defaults.showIntroSkipButton,
            showCreditsSkipButton = prefs[KEY_SHOW_CREDITS_SKIP]
                ?: defaults.showCreditsSkipButton,
            showTrickplayPreview = prefs[KEY_TRICKPLAY] ?: defaults.showTrickplayPreview,
            showChapterStrip = prefs[KEY_CHAPTER_STRIP] ?: defaults.showChapterStrip,
            subtitleEncoding = enumValue(prefs[KEY_SUBTITLE_ENCODING], defaults.subtitleEncoding),
            burnGraphicSubtitleWhenTranscoding = prefs[KEY_BURN_GRAPHIC_SUBTITLE]
                ?: defaults.burnGraphicSubtitleWhenTranscoding,
        )
    }

    val stateFlow: StateFlow<PlaybackSettings> = flow.stateIn(
        scope = storeScope,
        started = SharingStarted.Eagerly,
        initialValue = PlaybackSettings.defaults(),
    )

    private fun <T> read(block: (Preferences) -> T): T = runBlocking(Dispatchers.IO) {
        dataStore.data.map(block).first()
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { preferences -> block(preferences) }
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(rawValue: String?, default: T): T {
        val raw = rawValue ?: default.name
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }

    private companion object {
        val KEY_AFM = booleanPreferencesKey("afm")
        val KEY_MATCH_COLOR = booleanPreferencesKey("matchColor")
        val KEY_CONFIRM_BEFORE_FRAME_SWITCH = booleanPreferencesKey("confirmBeforeFrameSwitch")
        val KEY_SKIP_FRAME_SWITCH_CONFIRM = booleanPreferencesKey("skipFrameSwitchConfirm")
        val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("autoPlayNext")
        val KEY_AUTO_SKIP_INTRO = booleanPreferencesKey("autoSkipIntro")
        val KEY_AUTO_SKIP_CREDITS = booleanPreferencesKey("autoSkipCredits")
        val KEY_SHOW_INTRO_SKIP = booleanPreferencesKey("showIntroSkip")
        val KEY_SHOW_CREDITS_SKIP = booleanPreferencesKey("showCreditsSkip")
        val KEY_TRICKPLAY = booleanPreferencesKey("trickplay")
        val KEY_CHAPTER_STRIP = booleanPreferencesKey("chapterStrip")
        val KEY_SUBTITLE_ENCODING = stringPreferencesKey("subtitleEncoding")
        val KEY_BURN_GRAPHIC_SUBTITLE = booleanPreferencesKey("burnGraphicSubtitle")
    }
}
