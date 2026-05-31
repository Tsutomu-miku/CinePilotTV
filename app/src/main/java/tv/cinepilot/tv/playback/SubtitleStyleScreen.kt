package tv.cinepilot.tv.playback

import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section

fun ComponentActivity.showSubtitleStyleScreen(
    store: SubtitleStyleStore,
    onBackDetails: () -> Unit,
) {
    fun refresh(update: (SubtitleStylePreferences) -> SubtitleStylePreferences) {
        store.save(update(store.current()))
        showSubtitleStyleScreen(store, onBackDetails)
    }
    setContentView(subtitleStyleScreen(
        current = store.current(),
        onSize = { size -> refresh { it.copy(size = size) } },
        onColor = { color -> refresh { it.copy(color = color) } },
        onBackground = { background -> refresh { it.copy(background = background) } },
        onReset = {
            store.save(SubtitleStylePreferences.defaults())
            showSubtitleStyleScreen(store, onBackDetails)
        },
        onBackDetails = onBackDetails,
    ))
}

fun ComponentActivity.subtitleStyleScreen(
    current: SubtitleStylePreferences,
    onSize: (SubtitleTextSize) -> Unit,
    onColor: (SubtitleTextColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onReset: () -> Unit,
    onBackDetails: () -> Unit,
): ScrollView {
    return screen("字幕样式") {
        addView(label("当前：${current.size.label} / ${current.color.label} / ${current.background.label}"))
        addView(section("字号"))
        SubtitleTextSize.entries.forEachIndexed { index, option ->
            val optionAction = action(option.optionLabel(current.size)) { onSize(option) }
            addView(if (index == 0) optionAction.requestInitialFocus() else optionAction)
        }
        addView(section("颜色"))
        SubtitleTextColor.entries.forEach { option ->
            addView(action(option.optionLabel(current.color)) { onColor(option) })
        }
        addView(section("背景"))
        SubtitleBackground.entries.forEach { option ->
            addView(action(option.optionLabel(current.background)) { onBackground(option) })
        }
        addView(action("恢复默认字幕样式", onReset))
        addView(iconAction("返回详情", TvIcon.BACK, onBackDetails))
    }
}

private fun <T> T.optionLabel(current: T): String where T : Enum<T>, T : LabeledSubtitleOption {
    return if (this == current) "已选：${label}" else label
}
