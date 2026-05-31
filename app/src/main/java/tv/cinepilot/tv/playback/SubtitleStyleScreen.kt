package tv.cinepilot.tv.playback

import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.choiceAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section

fun ComponentActivity.showSubtitleStyleScreen(
    store: SubtitleStyleStore,
) {
    fun refresh(update: (SubtitleStylePreferences) -> SubtitleStylePreferences) {
        store.save(update(store.current()))
        showSubtitleStyleScreen(store)
    }
    setContentView(subtitleStyleScreen(
        current = store.current(),
        onSize = { size -> refresh { it.copy(size = size) } },
        onColor = { color -> refresh { it.copy(color = color) } },
        onBackground = { background -> refresh { it.copy(background = background) } },
        onReset = {
            store.save(SubtitleStylePreferences.defaults())
            showSubtitleStyleScreen(store)
        },
    ))
}

fun ComponentActivity.subtitleStyleScreen(
    current: SubtitleStylePreferences,
    onSize: (SubtitleTextSize) -> Unit,
    onColor: (SubtitleTextColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onReset: () -> Unit,
): ScrollView {
    return screen("字幕样式") {
        addView(section("字号"))
        addView(subtitleChoiceRow(SubtitleTextSize.entries, current.size, onSize, focusSelected = true))
        addView(section("颜色"))
        addView(subtitleChoiceRow(SubtitleTextColor.entries, current.color, onColor))
        addView(section("背景"))
        addView(subtitleChoiceRow(SubtitleBackground.entries, current.background, onBackground))
        addView(label("当前：${current.size.label} / ${current.color.label} / ${current.background.label}"))
        addView(action("恢复默认字幕样式", onReset))
    }
}

private fun <T> ComponentActivity.subtitleChoiceRow(
    options: Iterable<T>,
    current: T,
    onSelected: (T) -> Unit,
    focusSelected: Boolean = false,
): HorizontalScrollView where T : Enum<T>, T : LabeledSubtitleOption {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        options.forEach { option ->
            val optionAction = choiceAction(option.label, option == current) { onSelected(option) }
            addView(if (focusSelected && option == current) optionAction.requestInitialFocus() else optionAction)
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
    }
}
