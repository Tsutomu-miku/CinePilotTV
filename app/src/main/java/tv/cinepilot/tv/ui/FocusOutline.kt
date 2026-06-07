package tv.cinepilot.tv.ui

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

fun View.applyFocusOutline(focused: Boolean, radiusDp: Int = 8) {
    animate()
        .translationZ(if (focused) viewDp(MediaWallTokens.FocusElevation).toFloat() else 0f)
        .alpha(if (focused) 1f else 0.96f)
        .setDuration(150L)
        .start()
    ValueAnimator.ofFloat(if (focused) 0f else 1f, if (focused) 1f else 0f).apply {
        duration = 150L
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            foreground = outlineDrawable(
                radius = viewDp(radiusDp),
                width = (viewDp(MediaWallTokens.FocusBorder) * progress).toInt(),
                color = focusColor(progress),
            )
        }
        start()
    }
}

private fun outlineDrawable(radius: Int, width: Int, color: Int): GradientDrawable {
    return GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius.toFloat()
        if (width > 0) {
            setStroke(width, color)
        }
    }
}

private fun focusColor(progress: Float): Int {
    val alpha = (230 * progress).toInt().coerceIn(0, 230)
    return Color.argb(
        alpha,
        Color.red(TvColors.FocusRing),
        Color.green(TvColors.FocusRing),
        Color.blue(TvColors.FocusRing),
    )
}

private fun View.viewDp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}
