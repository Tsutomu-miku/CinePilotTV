package tv.cinepilot.tv.ui

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap

fun View.applyFocusOutline(focused: Boolean, radiusDp: Int = 8) {
    FocusShadowOverlay.update(this, focused, radiusDp)
    animate()
        .scaleX(if (focused) 1.018f else 1f)
        .scaleY(if (focused) 1.018f else 1f)
        .translationZ(if (focused) viewDp(MediaWallTokens.FocusElevation).toFloat() else 0f)
        .alpha(if (focused) 1f else 0.98f)
        .setDuration(150L)
        .start()
    ValueAnimator.ofFloat(if (focused) 0f else 1f, if (focused) 1f else 0f).apply {
        duration = 150L
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
    val ring = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius.toFloat()
        setStroke(viewStroke(progress, 2), focusWhite(progress, 152))
    }
    val hairline = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius.toFloat()
        setStroke(viewStroke(progress, 1), focusWhite(progress, 108))
    }
    return LayerDrawable(arrayOf(ring, hairline))
}

private fun focusWhite(progress: Float, maxAlpha: Int): Int {
    val alpha = (maxAlpha * progress).toInt().coerceIn(0, maxAlpha)
    return Color.argb(alpha, 255, 255, 255)
}

private fun View.viewStroke(progress: Float, maxDp: Int): Int {
    return (viewDp(maxDp) * progress).toInt().coerceAtLeast(if (progress > 0f) 1 else 0)
}

private fun View.viewDp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}

private object FocusShadowOverlay {
    private val shadows = WeakHashMap<View, FocusShadowDrawable>()

    fun update(view: View, focused: Boolean, radiusDp: Int) {
        remove(view)
        if (!focused) {
            return
        }
        view.post {
            if (!view.isFocused) {
                return@post
            }
            val root = view.rootView as? ViewGroup ?: return@post
            val shadow = FocusShadowDrawable(
                radius = view.viewDp(radiusDp).toFloat(),
                spread = view.viewDp(12).toFloat(),
            )
            shadow.bounds = shadowBounds(root, view, view.viewDp(14))
            root.overlay.add(shadow)
            shadows[view] = shadow
        }
    }

    private fun remove(view: View) {
        val root = view.rootView as? ViewGroup
        shadows.remove(view)?.let { drawable ->
            root?.overlay?.remove(drawable)
        }
    }

    private fun shadowBounds(root: View, target: View, spread: Int): Rect {
        val rootLocation = IntArray(2)
        val targetLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        target.getLocationOnScreen(targetLocation)
        val left = targetLocation[0] - rootLocation[0]
        val top = targetLocation[1] - rootLocation[1]
        return Rect(
            left - spread,
            top - spread,
            left + target.width + spread,
            top + target.height + spread,
        )
    }
}

private class FocusShadowDrawable(
    private val radius: Float,
    private val spread: Float,
) : android.graphics.drawable.Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        rect.set(
            bounds.left + spread,
            bounds.top + spread,
            bounds.right - spread,
            bounds.bottom - spread,
        )
        drawOuterStroke(canvas, rect, 2f, 4f, 92)
        drawOuterStroke(canvas, rect, 5f, 8f, 56)
        drawOuterStroke(canvas, rect, 9f, 12f, 28)
    }

    private fun drawOuterStroke(canvas: Canvas, content: RectF, offset: Float, stroke: Float, alpha: Int) {
        val half = stroke / 2f
        val glowRect = RectF(
            content.left - offset - half,
            content.top - offset - half,
            content.right + offset + half,
            content.bottom + offset + half,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = Color.argb(alpha, 255, 255, 255)
        val glowRadius = radius + offset + half
        canvas.drawRoundRect(glowRect, glowRadius, glowRadius, paint)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}

    @Deprecated("Deprecated in Android framework")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
