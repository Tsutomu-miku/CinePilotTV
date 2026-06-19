package tv.cinepilot.tv.playback

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

private const val SUBTITLE_STYLE_PREFERENCES_NAME = "subtitle_style"

private val Context.subtitleStyleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "subtitle_style_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, SUBTITLE_STYLE_PREFERENCES_NAME))
    },
)

data class SubtitleStylePreferences(
    val size: SubtitleTextSize,
    val color: SubtitleTextColor,
    val background: SubtitleBackground,
    val fontFamily: SubtitleFontFamily,
    val edgeStyle: SubtitleEdgeStyle,
    val bottomMargin: SubtitleBottomMargin,
    val textOpacity: SubtitleTextOpacity,
    val letterSpacingDp: Float,
) {
    companion object {
        fun defaults(): SubtitleStylePreferences {
            return SubtitleStylePreferences(
                size = SubtitleTextSize.STANDARD,
                color = SubtitleTextColor.WHITE,
                background = SubtitleBackground.SHADOW,
                fontFamily = SubtitleFontFamily.DEFAULT_BOLD,
                edgeStyle = SubtitleEdgeStyle.AUTO,
                bottomMargin = SubtitleBottomMargin.STANDARD,
                textOpacity = SubtitleTextOpacity.OPAQUE,
                letterSpacingDp = 0f,
            )
        }
    }
}

interface LabeledSubtitleOption {
    val label: String
}

enum class SubtitleTextSize(override val label: String, val fraction: Float) : LabeledSubtitleOption {
    SMALL("小号", 0.042f),
    STANDARD("标准", 0.052f),
    LARGE("大号", 0.064f),
    EXTRA_LARGE("特大", 0.076f),
}

enum class SubtitleTextColor(override val label: String, val color: Int) : LabeledSubtitleOption {
    WHITE("白色", Color.WHITE),
    WARM("暖白", Color.rgb(255, 244, 214)),
    YELLOW("黄色", Color.rgb(255, 230, 109)),
    CYAN("青色", Color.rgb(90, 240, 255)),
}

enum class SubtitleBackground(
    override val label: String,
    val backgroundColor: Int,
    val edgeColor: Int,
) : LabeledSubtitleOption {
    SHADOW("阴影", Color.TRANSPARENT, Color.BLACK),
    OUTLINE("粗描边", Color.TRANSPARENT, Color.BLACK),
    RAISED("浮雕", Color.TRANSPARENT, Color.argb(120, 0, 0, 0)),
    TRANSLUCENT("半透明黑底", Color.argb(180, 0, 0, 0), Color.BLACK),
    NONE("无背景", Color.TRANSPARENT, Color.TRANSPARENT),
}

enum class SubtitleFontFamily(
    override val label: String,
    val typeface: Typeface,
) : LabeledSubtitleOption {
    DEFAULT_BOLD("默认粗体", Typeface.DEFAULT_BOLD),
    DEFAULT("默认常规", Typeface.DEFAULT),
    SANS_SERIF("无衬线", Typeface.SANS_SERIF),
    SERIF("衬线", Typeface.SERIF),
    MONOSPACE("等宽", Typeface.MONOSPACE),
}

enum class SubtitleEdgeStyle(override val label: String) : LabeledSubtitleOption {
    AUTO("跟随背景"),
    NONE("无"),
    OUTLINE("描边"),
    DROP_SHADOW("阴影"),
    RAISED("浮雕"),
    DEPRESSED("凹陷"),
}

enum class SubtitleBottomMargin(override val label: String, val fraction: Float) : LabeledSubtitleOption {
    TIGHT("贴近底部", 0.035f),
    STANDARD("标准", 0.08f),
    HIGH("高于控制条", 0.14f),
    VERY_HIGH("画面上 1/3", 0.25f),
}

enum class SubtitleTextOpacity(override val label: String, val alpha: Int) : LabeledSubtitleOption {
    OPAQUE("不透明", 255),
    HIGH("90%", 230),
    MEDIUM("75%", 191),
    LOW("60%", 153),
}

class SubtitleStyleStore(context: Context) {
    private val dataStore = context.applicationContext.subtitleStyleDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun current(): SubtitleStylePreferences = stateFlow.value

    fun save(value: SubtitleStylePreferences) {
        storeScope.launch { saveAsync(value) }
    }

    /** Non-blocking save; completes when the DataStore edit has been applied. */
    suspend fun saveAsync(value: SubtitleStylePreferences) {
        dataStore.edit { preferences ->
            preferences[KEY_SIZE] = value.size.name
            preferences[KEY_COLOR] = value.color.name
            preferences[KEY_BACKGROUND] = value.background.name
            preferences[KEY_FONT_FAMILY] = value.fontFamily.name
            preferences[KEY_EDGE_STYLE] = value.edgeStyle.name
            preferences[KEY_BOTTOM_MARGIN] = value.bottomMargin.name
            preferences[KEY_TEXT_OPACITY] = value.textOpacity.name
            preferences[KEY_LETTER_SPACING_DP] = value.letterSpacingDp
        }
    }

    val flow: Flow<SubtitleStylePreferences> = dataStore.data.map { prefs ->
        SubtitleStylePreferences(
            size = enumValue(prefs[KEY_SIZE], SubtitleTextSize.STANDARD),
            color = enumValue(prefs[KEY_COLOR], SubtitleTextColor.WHITE),
            background = enumValue(prefs[KEY_BACKGROUND], SubtitleBackground.SHADOW),
            fontFamily = enumValue(prefs[KEY_FONT_FAMILY], SubtitleFontFamily.DEFAULT_BOLD),
            edgeStyle = enumValue(prefs[KEY_EDGE_STYLE], SubtitleEdgeStyle.AUTO),
            bottomMargin = enumValue(prefs[KEY_BOTTOM_MARGIN], SubtitleBottomMargin.STANDARD),
            textOpacity = enumValue(prefs[KEY_TEXT_OPACITY], SubtitleTextOpacity.OPAQUE),
            letterSpacingDp = prefs[KEY_LETTER_SPACING_DP] ?: 0f,
        )
    }

    val stateFlow: StateFlow<SubtitleStylePreferences> = flow.stateIn(
        scope = storeScope,
        started = SharingStarted.Eagerly,
        initialValue = SubtitleStylePreferences.defaults(),
    )

    private inline fun <reified T : Enum<T>> enumValue(rawValue: String?, default: T): T {
        val raw = rawValue ?: default.name
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }

    private companion object {
        val KEY_SIZE = stringPreferencesKey("size")
        val KEY_COLOR = stringPreferencesKey("color")
        val KEY_BACKGROUND = stringPreferencesKey("background")
        val KEY_FONT_FAMILY = stringPreferencesKey("fontFamily")
        val KEY_EDGE_STYLE = stringPreferencesKey("edgeStyle")
        val KEY_BOTTOM_MARGIN = stringPreferencesKey("bottomMargin")
        val KEY_TEXT_OPACITY = stringPreferencesKey("textOpacity")
        val KEY_LETTER_SPACING_DP = floatPreferencesKey("letterSpacingDp")
    }
}
