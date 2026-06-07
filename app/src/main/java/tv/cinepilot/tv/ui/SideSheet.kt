package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

fun ComponentActivity.sideSheet(content: LinearLayout.() -> Unit): View {
    return glassPanel {
        setPadding(dp(16), dp(14), dp(16), dp(16))
        addView(LinearLayout(this@sideSheet).apply {
            orientation = LinearLayout.VERTICAL
            content()
        })
    }.apply {
        layoutParams = FrameLayout.LayoutParams(
            dp(MediaWallTokens.SheetWidth),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.CENTER_VERTICAL,
        ).apply {
            rightMargin = dp(MediaWallTokens.ScreenX)
        }
    }
}
