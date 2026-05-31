package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.playerScreen(
    playerView: View,
    title: String,
    onStop: () -> Unit,
    onDiagnostics: () -> Unit,
): View {
    val root = FrameLayout(this).apply {
        setBackgroundColor(Color.BLACK)
    }
    root.addView(playerView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    root.addView(playerTopScrim(title), FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        dp(TvSize.PlayerTopOverlay),
        Gravity.TOP,
    ))
    root.addView(playerControls(onStop, onDiagnostics), FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        dp(TvSize.PlayerBottomOverlay),
        Gravity.BOTTOM,
    ))
    return root
}

private fun ComponentActivity.playerTopScrim(title: String): View {
    return TextView(this).apply {
        text = title.ifBlank { "正在播放" }
        textSize = TvType.PlayerTitle
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.TextPrimary)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(TvSpacing.PlayerOverlayX), 0, dp(TvSpacing.PlayerOverlayX), 0)
        setBackgroundColor(Color.argb(150, 0, 0, 0))
    }
}

private fun ComponentActivity.playerControls(
    onStop: () -> Unit,
    onDiagnostics: () -> Unit,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        setPadding(dp(TvSpacing.PlayerOverlayX), dp(16), dp(TvSpacing.PlayerOverlayX), dp(16))
        setBackgroundColor(Color.argb(170, 0, 0, 0))
        addView(iconAction("停止并返回详情", TvIcon.BACK, onStop), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(TvSize.ControlHeight),
        ).apply {
            rightMargin = dp(TvSpacing.ControlGap)
        })
        addView(action("诊断信息", onDiagnostics), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(TvSize.ControlHeight),
        ))
    }
}
