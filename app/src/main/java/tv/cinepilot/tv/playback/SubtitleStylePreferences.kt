package tv.cinepilot.tv.playback

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

data class SubtitleStylePreferences(
    val size: SubtitleTextSize,
    val color: SubtitleTextColor,
    val background: SubtitleBackground,
) {
    companion object {
        fun defaults(): SubtitleStylePreferences {
            return SubtitleStylePreferences(
                size = SubtitleTextSize.STANDARD,
                color = SubtitleTextColor.WHITE,
                background = SubtitleBackground.SHADOW,
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
}

enum class SubtitleBackground(
    override val label: String,
    val backgroundColor: Int,
    val edgeColor: Int,
) : LabeledSubtitleOption {
    SHADOW("阴影", Color.TRANSPARENT, Color.BLACK),
    TRANSLUCENT("半透明黑底", Color.argb(180, 0, 0, 0), Color.BLACK),
    NONE("无背景", Color.TRANSPARENT, Color.TRANSPARENT),
}

class SubtitleStyleStore(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        "subtitle_style",
        Context.MODE_PRIVATE,
    )

    fun current(): SubtitleStylePreferences {
        return SubtitleStylePreferences(
            size = enumValue("size", SubtitleTextSize.STANDARD),
            color = enumValue("color", SubtitleTextColor.WHITE),
            background = enumValue("background", SubtitleBackground.SHADOW),
        )
    }

    fun save(value: SubtitleStylePreferences) {
        preferences.edit()
            .putString("size", value.size.name)
            .putString("color", value.color.name)
            .putString("background", value.background.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T {
        val rawValue = preferences.getString(key, default.name) ?: default.name
        return enumValues<T>().firstOrNull { it.name == rawValue } ?: default
    }
}
