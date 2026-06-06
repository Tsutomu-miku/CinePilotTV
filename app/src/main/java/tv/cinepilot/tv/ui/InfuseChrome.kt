package tv.cinepilot.tv.ui

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.infuseBackdrop(): ImageView {
    return ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.58f
        setBackgroundColor(TvColors.Background)
        applyBackdropBlur()
    }
}

fun ComponentActivity.infuseStage(
    title: String,
    backdrop: ImageView,
    content: LinearLayout.() -> Unit,
): FrameLayout {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(InfuseLayoutTokens.ScreenX),
            dp(InfuseLayoutTokens.ScreenTop),
            dp(InfuseLayoutTokens.ScreenX),
            dp(InfuseLayoutTokens.ScreenBottom),
        )
        content()
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        contentDescription = title
        addView(container, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }
    return FrameLayout(this).apply {
        setBackgroundColor(TvColors.Background)
        addView(backdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(infuseScrim(), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(scroll)
    }
}

fun ComponentActivity.infuseTopChrome(title: String, actions: List<View>): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(this@infuseTopChrome).apply {
            text = "CinePilot TV"
            textSize = InfuseTypeTokens.Brand
            letterSpacing = 0.06f
            setTextColor(TvColors.TextMuted)
            includeFontPadding = false
        })
        addView(TextView(this@infuseTopChrome).apply {
            text = title
            textSize = InfuseTypeTokens.Metadata
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(infuseActionRow(actions))
    }
}

private fun ComponentActivity.infuseScrim(): View {
    return View(this).apply {
        background = rounded(Color.argb(194, 0, 0, 0), 0)
    }
}
