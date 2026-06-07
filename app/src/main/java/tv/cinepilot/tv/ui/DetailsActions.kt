package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.detailsActions(actions: List<InfuseAction>): View {
    val views = actions.map(::detailAction)
    if (actions.firstOrNull()?.emphasis == InfuseActionEmphasis.PRIMARY) {
        views.firstOrNull()?.requestInitialFocus()
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        addView(LinearLayout(this@detailsActions).apply {
            orientation = LinearLayout.HORIZONTAL
            views.forEach(::addView)
        })
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
        setTextColor(if (primary) TvColors.FocusText else TvColors.TextSecondary)
        setCompoundDrawablesRelativeWithIntrinsicBounds(action.icon.drawableRes, 0, 0, 0)
        compoundDrawablePadding = dp(7)
        background = rounded(
            if (primary) TvColors.AccentStrong else Color.argb(92, 0, 0, 0),
            dp(18),
            dp(1),
            if (primary) TvColors.AccentStrong else TvColors.GlassBorder,
        )
        setPadding(dp(if (primary) 18 else 12), 0, dp(if (primary) 18 else 12), 0)
        setOnClickListener { action.onClick() }
        setOnFocusChangeListener { view, focused ->
            view.applyFocusOutline(focused, 18)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(38),
        ).apply {
            rightMargin = dp(8)
        }
        minWidth = dp(if (primary) 118 else 86)
    }
}
