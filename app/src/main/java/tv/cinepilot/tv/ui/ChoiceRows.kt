package tv.cinepilot.tv.ui

import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

fun ComponentActivity.settingChoiceRow(choices: List<View>): HorizontalScrollView {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        choices.forEach(::addView)
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        clipToPadding = false
        background = rounded(TvColors.Surface, dp(TvRadius.Card), dp(1), TvColors.PosterBorder)
        setPadding(dp(10), dp(8), dp(2), dp(0))
        addView(row)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(6)
        }
    }
}

fun ComponentActivity.settingChoiceGroup(choices: List<View>): TvFlowLayout {
    return TvFlowLayout(this).apply {
        choices.forEach(::addView)
        background = rounded(TvColors.Surface, dp(TvRadius.Card), dp(1), TvColors.PosterBorder)
        setPadding(dp(10), dp(8), dp(2), dp(0))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(6)
        }
    }
}
