package tv.cinepilot.tv.playback

import android.content.Context
import android.content.SharedPreferences

data class PlaybackSettings(
    val autoFrameMatching: Boolean,
    val matchColorSpace: Boolean,
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
                autoFrameMatching = true,
                matchColorSpace = true,
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
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        "playback_settings",
        Context.MODE_PRIVATE,
    )

    fun current(): PlaybackSettings {
        return PlaybackSettings(
            autoFrameMatching = bool("afm", PlaybackSettings.defaults().autoFrameMatching),
            matchColorSpace = bool("matchColor", PlaybackSettings.defaults().matchColorSpace),
            autoPlayNext = bool("autoPlayNext", PlaybackSettings.defaults().autoPlayNext),
            autoSkipIntro = bool("autoSkipIntro", PlaybackSettings.defaults().autoSkipIntro),
            autoSkipCredits = bool("autoSkipCredits", PlaybackSettings.defaults().autoSkipCredits),
            showIntroSkipButton = bool("showIntroSkip",
                PlaybackSettings.defaults().showIntroSkipButton),
            showCreditsSkipButton = bool("showCreditsSkip",
                PlaybackSettings.defaults().showCreditsSkipButton),
            showTrickplayPreview = bool("trickplay",
                PlaybackSettings.defaults().showTrickplayPreview),
            showChapterStrip = bool("chapterStrip",
                PlaybackSettings.defaults().showChapterStrip),
            subtitleEncoding = enumValue("subtitleEncoding",
                PlaybackSettings.defaults().subtitleEncoding),
            burnGraphicSubtitleWhenTranscoding = bool("burnGraphicSubtitle",
                PlaybackSettings.defaults().burnGraphicSubtitleWhenTranscoding),
        )
    }

    fun save(value: PlaybackSettings) {
        preferences.edit()
            .putBoolean("afm", value.autoFrameMatching)
            .putBoolean("matchColor", value.matchColorSpace)
            .putBoolean("autoPlayNext", value.autoPlayNext)
            .putBoolean("autoSkipIntro", value.autoSkipIntro)
            .putBoolean("autoSkipCredits", value.autoSkipCredits)
            .putBoolean("showIntroSkip", value.showIntroSkipButton)
            .putBoolean("showCreditsSkip", value.showCreditsSkipButton)
            .putBoolean("trickplay", value.showTrickplayPreview)
            .putBoolean("chapterStrip", value.showChapterStrip)
            .putString("subtitleEncoding", value.subtitleEncoding.name)
            .putBoolean("burnGraphicSubtitle", value.burnGraphicSubtitleWhenTranscoding)
            .apply()
    }

    private fun bool(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T {
        val raw = preferences.getString(key, default.name) ?: default.name
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }
}
