package tv.cinepilot.tv.ui

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

fun ComponentActivity.edgeChrome(actions: List<InfuseAction>): View {
    val rail = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        actions.forEach { action -> addView(edgeChromeButton(action)) }
    }
    return FrameLayout(this).apply {
        addView(rail, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            dp(MediaWallTokens.EdgeChromeSize),
            Gravity.END or Gravity.TOP,
        ))
    }
}

private fun ComponentActivity.edgeChromeButton(action: InfuseAction): ImageButton {
    return ImageButton(this).apply {
        contentDescription = action.label
        setImageResource(action.icon.drawableRes)
        setColorFilter(TvColors.TextSecondary)
        background = rounded(
            Color.argb(42, 0, 0, 0),
            dp(MediaWallTokens.EdgeChromeRadius),
            dp(1),
            homeHairlineColor(38),
        )
        scaleType = ImageView.ScaleType.CENTER
        isFocusable = true
        isClickable = true
        setPadding(dp(7), dp(7), dp(7), dp(7))
        setOnClickListener { action.onClick() }
        setOnFocusChangeListener { view, focused ->
            view.applyFocusOutline(focused, MediaWallTokens.EdgeChromeRadius)
            if (view is ImageButton) {
                view.setColorFilter(if (focused) TvColors.AccentStrong else TvColors.TextSecondary)
            }
        }
        layoutParams = LinearLayout.LayoutParams(
            dp(MediaWallTokens.EdgeChromeSize),
            dp(MediaWallTokens.EdgeChromeSize),
        ).apply {
            leftMargin = dp(MediaWallTokens.EdgeChromeGap)
        }
    }
}
