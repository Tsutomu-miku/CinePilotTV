package tv.cinepilot.tv.ui

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.episodeWatchedBadge(item: MediaItemSummary): View? {
    if (!item.isEpisode() || !item.userData().played()) {
        return null
    }
    return TextView(this).apply {
        text = "已看"
        textSize = 10.5f
        setTextColor(Color.rgb(12, 15, 22))
        gravity = Gravity.CENTER
        includeFontPadding = false
        background = rounded(Color.argb(230, 244, 247, 255), dp(8), dp(1), Color.WHITE)
        setPadding(dp(7), 0, dp(7), dp(1))
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            dp(22),
            Gravity.TOP or Gravity.RIGHT,
        ).apply {
            topMargin = dp(6)
            rightMargin = dp(6)
        }
    }
}
