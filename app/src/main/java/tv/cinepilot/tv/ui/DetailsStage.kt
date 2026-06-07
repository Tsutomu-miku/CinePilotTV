package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.detailsStage(
    item: MediaItemSummary,
    loadBackdrop: (ImageView, MediaItemSummary) -> Unit,
    content: LinearLayout.() -> Unit,
): FrameLayout {
    val backdrop = cinematicBackdrop()
    backdrop.alpha = 0.84f
    loadBackdrop(backdrop, item)
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.DetailTop + 8),
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.ScreenBottom),
        )
        content()
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        contentDescription = "详情 ${item.name()}"
        addView(container, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }.bindVerticalDpadScrollFallback()
    return FrameLayout(this).apply {
        setBackgroundColor(Color.BLACK)
        addView(backdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(View(this@detailsStage).apply {
            background = rounded(Color.argb(42, 0, 0, 0), 0)
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(View(this@detailsStage).apply {
            background = readableDetailsScrim()
        }, FrameLayout.LayoutParams(
            dp(1180),
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(scroll)
    }
}

private fun readableDetailsScrim(): GradientDrawable {
    return GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(
            Color.argb(236, 0, 0, 0),
            Color.argb(184, 0, 0, 0),
            Color.argb(72, 0, 0, 0),
            Color.TRANSPARENT,
        ),
    )
}
