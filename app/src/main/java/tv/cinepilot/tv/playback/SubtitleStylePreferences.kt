package tv.cinepilot.tv.playback

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface

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
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        "subtitle_style",
        Context.MODE_PRIVATE,
    )

    fun current(): SubtitleStylePreferences {
        return SubtitleStylePreferences(
            size = enumValue("size", SubtitleTextSize.STANDARD),
            color = enumValue("color", SubtitleTextColor.WHITE),
            background = enumValue("background", SubtitleBackground.SHADOW),
            fontFamily = enumValue("fontFamily", SubtitleFontFamily.DEFAULT_BOLD),
            edgeStyle = enumValue("edgeStyle", SubtitleEdgeStyle.AUTO),
            bottomMargin = enumValue("bottomMargin", SubtitleBottomMargin.STANDARD),
            textOpacity = enumValue("textOpacity", SubtitleTextOpacity.OPAQUE),
            letterSpacingDp = preferences.getFloat("letterSpacingDp", 0f),
        )
    }

    fun save(value: SubtitleStylePreferences) {
        preferences.edit()
            .putString("size", value.size.name)
            .putString("color", value.color.name)
            .putString("background", value.background.name)
            .putString("fontFamily", value.fontFamily.name)
            .putString("edgeStyle", value.edgeStyle.name)
            .putString("bottomMargin", value.bottomMargin.name)
            .putString("textOpacity", value.textOpacity.name)
            .putFloat("letterSpacingDp", value.letterSpacingDp)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T {
        val rawValue = preferences.getString(key, default.name) ?: default.name
        return enumValues<T>().firstOrNull { it.name == rawValue } ?: default
    }
}
