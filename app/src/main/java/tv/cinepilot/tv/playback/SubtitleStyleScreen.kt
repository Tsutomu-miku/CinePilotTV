package tv.cinepilot.tv.playback

import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.radioChoice
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.settingChoiceRow

fun ComponentActivity.showSubtitleStyleScreen(
    store: SubtitleStyleStore,
    focusGroup: SubtitleStyleFocusGroup = SubtitleStyleFocusGroup.SIZE,
) {
    fun refresh(
        nextFocusGroup: SubtitleStyleFocusGroup,
        update: (SubtitleStylePreferences) -> SubtitleStylePreferences,
    ) {
        store.save(update(store.current()))
        showSubtitleStyleScreen(store, nextFocusGroup)
    }
    setContentView(subtitleStyleScreen(
        current = store.current(),
        focusGroup = focusGroup,
        onSize = { size -> refresh(SubtitleStyleFocusGroup.SIZE) { it.copy(size = size) } },
        onColor = { color -> refresh(SubtitleStyleFocusGroup.COLOR) { it.copy(color = color) } },
        onBackground = { background -> refresh(SubtitleStyleFocusGroup.BACKGROUND) { it.copy(background = background) } },
        onReset = {
            store.save(SubtitleStylePreferences.defaults())
            showSubtitleStyleScreen(store)
        },
    ))
}

fun ComponentActivity.subtitleStyleScreen(
    current: SubtitleStylePreferences,
    focusGroup: SubtitleStyleFocusGroup,
    onSize: (SubtitleTextSize) -> Unit,
    onColor: (SubtitleTextColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onReset: () -> Unit,
): ScrollView {
    return screen("字幕样式") {
        addView(section("字号"))
        addView(subtitleChoiceRow(
            options = SubtitleTextSize.entries,
            current = current.size,
            onSelected = onSize,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.SIZE,
        ))
        addView(section("颜色"))
        addView(subtitleChoiceRow(
            options = SubtitleTextColor.entries,
            current = current.color,
            onSelected = onColor,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.COLOR,
        ))
        addView(section("背景"))
        addView(subtitleChoiceRow(
            options = SubtitleBackground.entries,
            current = current.background,
            onSelected = onBackground,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.BACKGROUND,
        ))
        addView(label("当前 ${current.size.label} / ${current.color.label} / ${current.background.label}"))
        addView(action("恢复默认字幕样式", onReset))
    }
}

enum class SubtitleStyleFocusGroup {
    SIZE,
    COLOR,
    BACKGROUND,
}

private fun <T> ComponentActivity.subtitleChoiceRow(
    options: Iterable<T>,
    current: T,
    onSelected: (T) -> Unit,
    focusSelected: Boolean = false,
): HorizontalScrollView where T : Enum<T>, T : LabeledSubtitleOption {
    return settingChoiceRow(options.map { option ->
        val optionAction = radioChoice(option.label, option == current) { onSelected(option) }
        if (focusSelected && option == current) optionAction.requestInitialFocus() else optionAction
    })
}
