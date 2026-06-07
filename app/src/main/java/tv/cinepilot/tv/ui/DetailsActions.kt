package tv.cinepilot.tv.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.detailsActions(actions: List<InfuseAction>): View {
    val views = actions.map(::detailAction)
    if (actions.firstOrNull()?.emphasis == InfuseActionEmphasis.PRIMARY) {
        views.firstOrNull()?.requestInitialFocus()
    }
    return TvFlowLayout(this).apply {
        isFocusable = false
        setPadding(0, dp(4), 0, dp(2))
        views.forEach(::addView)
    }
}

private fun ComponentActivity.detailAction(action: InfuseAction): TextView {
    val primary = action.emphasis == InfuseActionEmphasis.PRIMARY
    return TextView(this).apply {
        text = action.label
        textSize = MediaWallType.Action
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        isFocusable = true
        isClickable = true
        setCompoundDrawablesRelativeWithIntrinsicBounds(action.icon.drawableRes, 0, 0, 0)
        compoundDrawablePadding = dp(if (primary) 7 else 5)
        updateDetailActionTint(primary, focused = false)
        background = rounded(
            if (primary) TvColors.AccentStrong else Color.argb(42, 0, 0, 0),
            dp(16),
            dp(1),
            if (primary) TvColors.AccentStrong else homeHairlineColor(44),
        )
        setPadding(dp(if (primary) 18 else 10), 0, dp(if (primary) 18 else 10), 0)
        setOnClickListener { action.onClick() }
        setOnFocusChangeListener { view, focused ->
            view.applyFocusOutline(focused, 16)
            if (view is TextView) {
                view.updateDetailActionTint(primary, focused)
            }
        }
        layoutParams = ViewGroup.MarginLayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(if (primary) 38 else 34),
        ).apply {
            rightMargin = dp(8)
            bottomMargin = dp(8)
        }
        minWidth = dp(if (primary) 126 else 72)
    }
}

private fun TextView.updateDetailActionTint(primary: Boolean, focused: Boolean) {
    val color = when {
        primary -> TvColors.FocusText
        focused -> TvColors.TextPrimary
        else -> TvColors.TextSecondary
    }
    setTextColor(color)
    compoundDrawableTintList = ColorStateList.valueOf(color)
}
