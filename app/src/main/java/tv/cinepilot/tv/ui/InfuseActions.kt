package tv.cinepilot.tv.ui

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.infuseAction(action: InfuseAction): TextView {
    return TextView(this).apply {
        text = action.label
        textSize = InfuseTypeTokens.Action
        isFocusable = true
        isClickable = true
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        minHeight = dp(InfuseLayoutTokens.ActionHeight)
        setTextColor(if (action.emphasis == InfuseActionEmphasis.PRIMARY) TvColors.TextPrimary else TvColors.TextSecondary)
        setCompoundDrawablesRelativeWithIntrinsicBounds(action.icon.drawableRes, 0, 0, 0)
        compoundDrawablePadding = dp(8)
        background = glassDrawable(GlassTokens.ControlRadius)
        setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { action.onClick() }
        setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.applyGlassFocus(hasFocus)
            if (focusedView is TextView) {
                focusedView.setTextColor(if (hasFocus) TvColors.AccentStrong else TvColors.TextSecondary)
            }
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(InfuseLayoutTokens.ActionHeight),
        ).apply {
            rightMargin = dp(InfuseLayoutTokens.ActionGap)
        }
        minWidth = dp(if (action.emphasis == InfuseActionEmphasis.PRIMARY) 132 else InfuseLayoutTokens.ActionMinWidth)
    }
}

fun ComponentActivity.infuseActionRow(actions: List<View>): HorizontalScrollView {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        actions.forEach { addView(it) }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        addView(row)
    }
}

fun ComponentActivity.infuseActions(actions: List<InfuseAction>): View {
    return infuseActionRow(actions.map(::infuseAction))
}
