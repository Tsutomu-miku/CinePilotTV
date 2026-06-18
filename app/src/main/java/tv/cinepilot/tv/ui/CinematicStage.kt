package tv.cinepilot.tv.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity

fun ComponentActivity.cinematicBackdrop(blurred: Boolean = true): ImageView {
    return ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.5f
        setBackgroundColor(Color.BLACK)
        if (blurred) {
            applyBackdropBlur()
        }
    }
}

fun ComponentActivity.cinematicStage(
    backdrop: ImageView = cinematicBackdrop(),
    scrollable: Boolean = true,
    content: FrameLayout.() -> Unit,
): FrameLayout {
    return FrameLayout(this).apply {
        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.BLACK)
        addView(backdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(cinematicScrim(), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        if (scrollable) {
            addView(cinematicScrollContainer(content))
        } else {
            content()
        }
    }
}

private fun ComponentActivity.cinematicScrollContainer(content: FrameLayout.() -> Unit): ScrollView {
    val frame = FrameLayout(this).apply {
        clipChildren = false
        clipToPadding = false
        setPadding(
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.ScreenTop),
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.ScreenBottom),
        )
        content()
    }
    return ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        addView(frame, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }
}

private fun ComponentActivity.cinematicScrim(): View {
    return View(this).apply {
        background = rounded(Color.argb(202, 0, 0, 0), 0)
    }
}
