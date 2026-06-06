package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView

fun ComponentActivity.playerScreen(
    playerView: View,
    debugInfo: String,
): View {
    val root = FrameLayout(this).apply {
        setBackgroundColor(Color.BLACK)
    }
    root.addView(playerView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    val infoPanel = playerInfoPanel(debugInfo)
    val infoButton = iconAction("视频信息", TvIcon.INFO) {
        infoPanel.visibility = if (infoPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    infoButton.visibility = View.GONE
    root.addView(infoButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        dp(TvSize.ControlHeight),
        Gravity.BOTTOM or Gravity.END,
    ).apply {
        rightMargin = dp(32)
        bottomMargin = dp(96)
    })
    root.addView(infoPanel, FrameLayout.LayoutParams(
        dp(560),
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM or Gravity.END,
    ).apply {
        rightMargin = dp(32)
        bottomMargin = dp(170)
    })
    if (playerView is PlayerView) {
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            infoButton.visibility = visibility
            if (visibility != View.VISIBLE) {
                infoPanel.visibility = View.GONE
            }
        })
    }
    return root
}

private fun ComponentActivity.playerInfoPanel(debugInfo: String): TextView {
    return TextView(this).apply {
        text = debugInfo
        textSize = TvType.Metadata
        setTextColor(TvColors.TextSecondary)
        setLineSpacing(2f, 1.05f)
        maxLines = 18
        ellipsize = TextUtils.TruncateAt.END
        background = rounded(Color.argb(230, 8, 13, 24), dp(TvRadius.Control), dp(1), TvColors.FocusRing)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        visibility = View.GONE
    }
}
