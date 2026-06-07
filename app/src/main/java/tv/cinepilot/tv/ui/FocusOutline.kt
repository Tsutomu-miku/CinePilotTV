package tv.cinepilot.tv.ui

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View

fun View.applyFocusOutline(focused: Boolean, radiusDp: Int = 8) {
    animate()
        .scaleX(if (focused) 1.035f else 1f)
        .scaleY(if (focused) 1.035f else 1f)
        .translationZ(if (focused) viewDp(MediaWallTokens.FocusElevation).toFloat() else 0f)
        .alpha(if (focused) 1f else 0.98f)
        .setDuration(170L)
        .start()
    ValueAnimator.ofFloat(if (focused) 0f else 1f, if (focused) 1f else 0f).apply {
        duration = 170L
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            foreground = this@applyFocusOutline.focusDrawable(
                radius = viewDp(radiusDp),
                progress = progress,
            )
        }
        start()
    }
}

private fun View.focusDrawable(radius: Int, progress: Float): LayerDrawable {
    val glow = GradientDrawable().apply {
        setColor(focusFill(progress))
        cornerRadius = radius.toFloat()
        setStroke(viewStroke(progress, 6), focusGlow(progress, 58))
    }
    val ring = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius.toFloat()
        setStroke(viewStroke(progress, 2), focusGlow(progress, 228))
    }
    val inner = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius.toFloat()
        setStroke(viewStroke(progress, 1), focusGlow(progress, 132))
    }
    return LayerDrawable(arrayOf(glow, ring, inner))
}

private fun focusFill(progress: Float): Int {
    val alpha = (24 * progress).toInt().coerceIn(0, 24)
    return Color.argb(alpha, 255, 255, 255)
}

private fun focusGlow(progress: Float, maxAlpha: Int): Int {
    val alpha = (maxAlpha * progress).toInt().coerceIn(0, maxAlpha)
    return Color.argb(alpha, 255, 255, 255)
}

private fun View.viewStroke(progress: Float, maxDp: Int): Int {
    return (viewDp(maxDp) * progress).toInt().coerceAtLeast(if (progress > 0f) 1 else 0)
}

private fun View.viewDp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}
