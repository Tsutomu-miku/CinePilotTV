package tv.cinepilot.tv.ui

import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.choiceAction(text: String, selected: Boolean, onClick: () -> Unit): Button {
    return action(if (selected) "已选 $text" else text, onClick).apply {
        val normalColor = if (selected) TvColors.AccentStrong else TvColors.SurfaceControl
        setTextColor(if (selected) TvColors.FocusText else TvColors.TextPrimary)
        background = rounded(normalColor, dp(TvRadius.Control), dp(1), TvColors.FocusRing)
        setOnFocusChangeListener { focusedView, hasFocus ->
            applyFocusState(focusedView, hasFocus)
            if (focusedView is TextView) {
                focusedView.setTextColor(if (hasFocus || selected) TvColors.FocusText else TvColors.TextPrimary)
            }
            focusedView.background = rounded(
                if (hasFocus) TvColors.Focus else normalColor,
                dp(TvRadius.Control),
                if (hasFocus) dp(3) else dp(1),
                TvColors.FocusRing,
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(TvSize.ControlHeight),
        ).apply {
            rightMargin = dp(TvSpacing.ControlGap)
            bottomMargin = dp(TvSpacing.ControlGap)
        }
    }
}

fun <T : View> T.requestInitialFocus(): T {
    post { requestFocus() }
    return this
}

fun <T : View> T.keepFocusOnVerticalDpad(): T {
    setOnKeyListener { _, keyCode, event ->
        event.action == KeyEvent.ACTION_DOWN &&
            (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
    }
    return this
}

fun ComponentActivity.setFocusableColors(
    view: TextView,
    focusedColor: Int,
    normalColor: Int,
    focusedTextColor: Int? = null,
    normalTextColor: Int? = null,
) {
    view.background = rounded(normalColor, dp(TvRadius.Control))
    view.setOnFocusChangeListener { focusedView, hasFocus ->
        applyFocusState(focusedView, hasFocus)
        if (focusedView is TextView && focusedTextColor != null && normalTextColor != null) {
            focusedView.setTextColor(if (hasFocus) focusedTextColor else normalTextColor)
        }
        focusedView.background = rounded(
            if (hasFocus) focusedColor else normalColor,
            dp(TvRadius.Control),
            if (hasFocus) dp(3) else dp(1),
            TvColors.FocusRing,
        )
    }
}

private fun ComponentActivity.applyFocusState(view: View, hasFocus: Boolean) {
    view.animate()
        .alpha(if (hasFocus) 1f else 0.96f)
        .translationZ(if (hasFocus) dp(6).toFloat() else 0f)
        .setDuration(FOCUS_ANIMATION_MS)
        .start()
}

private const val FOCUS_ANIMATION_MS = 120L
