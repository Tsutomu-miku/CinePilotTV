package tv.cinepilot.tv.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.infusePanelScreen(
    title: String,
    content: LinearLayout.() -> Unit,
): FrameLayout {
    return infuseStage(title, infuseBackdrop()) {
        addView(infuseTopChrome(title, emptyList()), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(InfuseLayoutTokens.TopRailHeight),
        ).apply {
            bottomMargin = dp(24)
        })
        addView(glassPanel {
            setPadding(
                dp(InfuseLayoutTokens.GlassPadding),
                dp(InfuseLayoutTokens.GlassPadding),
                dp(InfuseLayoutTokens.GlassPadding),
                dp(InfuseLayoutTokens.GlassPadding),
            )
            addView(LinearLayout(this@infusePanelScreen).apply {
                orientation = LinearLayout.VERTICAL
                content()
            })
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }
}

fun ComponentActivity.infusePanelTitle(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = InfuseTypeTokens.ShelfTitle
        setTextColor(TvColors.AccentStrong)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(10))
    }
}

fun ComponentActivity.infusePanelNote(text: String): TextView {
    return bodyText(text).apply {
        textSize = InfuseTypeTokens.Metadata
        setPadding(0, dp(8), 0, dp(4))
    }
}

fun View.compactPanelSpacing(bottomDp: Int = 12): View {
    val params = layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )
    params.bottomMargin = resources.displayMetrics.run { (bottomDp * density + 0.5f).toInt() }
    layoutParams = params
    return this
}
