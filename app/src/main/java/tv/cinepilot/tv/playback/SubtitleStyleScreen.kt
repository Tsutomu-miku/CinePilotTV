package tv.cinepilot.tv.playback

import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.tv.compose.screens.ComposeSubtitleStyleScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.radioChoice
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.settingChoiceRow

fun ComponentActivity.showSubtitleStyleScreen(
    store: SubtitleStyleStore,
    focusGroup: SubtitleStyleFocusGroup = SubtitleStyleFocusGroup.SIZE,
    renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
) {
    fun refresh(
        nextFocusGroup: SubtitleStyleFocusGroup,
        update: (SubtitleStylePreferences) -> SubtitleStylePreferences,
    ) {
        store.save(update(store.current()))
        showSubtitleStyleScreen(store, nextFocusGroup, renderCompose)
    }
    renderCompose("字幕样式") { palette ->
        ComposeSubtitleStyleScreen(
            palette = palette,
            current = store.current(),
            focusGroup = focusGroup,
            onSize = { size -> refresh(SubtitleStyleFocusGroup.SIZE) { it.copy(size = size) } },
            onColor = { color -> refresh(SubtitleStyleFocusGroup.COLOR) { it.copy(color = color) } },
            onBackground = { background ->
                refresh(SubtitleStyleFocusGroup.BACKGROUND) { it.copy(background = background) }
            },
            onFont = { font ->
                refresh(SubtitleStyleFocusGroup.FONT) { it.copy(fontFamily = font) }
            },
            onEdgeStyle = { edge ->
                refresh(SubtitleStyleFocusGroup.EDGE) { it.copy(edgeStyle = edge) }
            },
            onMargin = { margin ->
                refresh(SubtitleStyleFocusGroup.MARGIN) { it.copy(bottomMargin = margin) }
            },
            onOpacity = { opacity ->
                refresh(SubtitleStyleFocusGroup.OPACITY) { it.copy(textOpacity = opacity) }
            },
            onReset = {
                store.save(SubtitleStylePreferences.defaults())
                showSubtitleStyleScreen(store, renderCompose = renderCompose)
            },
        )
    }
}

fun ComponentActivity.subtitleStyleScreen(
    current: SubtitleStylePreferences,
    focusGroup: SubtitleStyleFocusGroup,
    onSize: (SubtitleTextSize) -> Unit,
    onColor: (SubtitleTextColor) -> Unit,
    onBackground: (SubtitleBackground) -> Unit,
    onFont: (SubtitleFontFamily) -> Unit,
    onEdgeStyle: (SubtitleEdgeStyle) -> Unit,
    onMargin: (SubtitleBottomMargin) -> Unit,
    onOpacity: (SubtitleTextOpacity) -> Unit,
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
        addView(section("背景 / 投影"))
        addView(subtitleChoiceRow(
            options = SubtitleBackground.entries,
            current = current.background,
            onSelected = onBackground,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.BACKGROUND,
        ))
        addView(section("字体"))
        addView(subtitleChoiceRow(
            options = SubtitleFontFamily.entries,
            current = current.fontFamily,
            onSelected = onFont,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.FONT,
        ))
        addView(section("描边样式"))
        addView(subtitleChoiceRow(
            options = SubtitleEdgeStyle.entries,
            current = current.edgeStyle,
            onSelected = onEdgeStyle,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.EDGE,
        ))
        addView(section("底部位置"))
        addView(subtitleChoiceRow(
            options = SubtitleBottomMargin.entries,
            current = current.bottomMargin,
            onSelected = onMargin,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.MARGIN,
        ))
        addView(section("文字不透明度"))
        addView(subtitleChoiceRow(
            options = SubtitleTextOpacity.entries,
            current = current.textOpacity,
            onSelected = onOpacity,
            focusSelected = focusGroup == SubtitleStyleFocusGroup.OPACITY,
        ))
        addView(iconAction("恢复默认", TvIcon.REFRESH, onReset))
    }
}

enum class SubtitleStyleFocusGroup {
    SIZE,
    COLOR,
    BACKGROUND,
    FONT,
    EDGE,
    MARGIN,
    OPACITY,
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
