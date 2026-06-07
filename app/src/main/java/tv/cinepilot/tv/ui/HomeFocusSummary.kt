package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

class HomeFocusSummary(
    val root: View,
    val title: TextView,
    val metadata: TextView,
    val state: TextView,
)

fun ComponentActivity.homeFocusSummary(): HomeFocusSummary {
    val title = TextView(this).apply {
        textSize = MediaWallType.SummaryTitle
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val metadata = TextView(this).apply {
        textSize = MediaWallType.SummaryMeta
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(5), 0, 0)
    }
    val state = TextView(this).apply {
        textSize = MediaWallType.SummaryMeta
        setTextColor(TvColors.AccentStrong)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(dp(16), 0, 0, 0)
    }
    val root = FrameLayout(this).apply {
        visibility = View.GONE
        background = rounded(Color.argb(186, 0, 0, 0), 0)
        setPadding(dp(MediaWallTokens.ScreenX), dp(10), dp(MediaWallTokens.ScreenX), dp(10))
        addView(LinearLayout(this@homeFocusSummary).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@homeFocusSummary).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(metadata)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(state)
        })
    }
    return HomeFocusSummary(root, title, metadata, state)
}

fun updateHomeFocusSummary(summary: HomeFocusSummary, item: MediaItemSummary) {
    val presentation = item.toMediaPresentation()
    summary.root.visibility = View.VISIBLE
    summary.title.text = presentation.title
    summary.metadata.text = presentation.contextLine
    summary.state.text = presentation.watchState
    summary.state.visibility = if (presentation.watchState.isBlank()) View.GONE else View.VISIBLE
}
