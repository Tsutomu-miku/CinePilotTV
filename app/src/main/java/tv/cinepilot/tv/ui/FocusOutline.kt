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
        val removePending = shadows.containsKey(view)
        if (!focused) {
            scheduleRemove(view)
            return
        }
        view.post {
            // Always remove any previous shadow from this view inside the posted
            // block so the rootView used for removal matches the one used for add.
            removeNow(view)
            if (!view.isAttachedToWindow || !view.isFocused) {
                return@post
            }
            val root = view.rootView as? ViewGroup ?: return@post
            val shadow = FocusShadowDrawable(
                radius = view.viewDp(radiusDp).toFloat(),
                spread = view.viewDp(MediaWallTokens.FocusShadowInnerSpread).toFloat(),
                enterMillis = 150L,
                exitMillis = 120L,
            )
            shadow.bounds = shadowBounds(root, view, view.viewDp(MediaWallTokens.FocusShadowSpread))
            root.overlay.add(shadow)
            shadows[view] = shadow
            shadow.enter()
            // Avoid leaking a stale posted remove if view already requested exit.
            if (removePending) {
                // No-op: shadow just entered; if caller toggled again while the
                // post was in flight, scheduleRemove would have updated this view.
            }
        }
    }

    /** Schedule a fade-out then remove. Safe to call from any thread state. */
    private fun scheduleRemove(view: View) {
        val existing = shadows[view] ?: return
        val root = view.rootView as? ViewGroup
        if (root == null) {
            shadows.remove(view)
            return
        }
        existing.exit {
            root.overlay.remove(existing)
            // Only clear the map entry if it still points to the same drawable
            // (focus might have been re-granted during the fade-out).
            if (shadows[view] === existing) {
                shadows.remove(view)
            }
        }
    }

    /** Immediate removal without animation; always called from within a post. */
    private fun removeNow(view: View) {
        val root = view.rootView as? ViewGroup
        shadows.remove(view)?.let { drawable ->
            root?.overlay?.remove(drawable)
        }
    }

    @Deprecated("Use scheduleRemove / removeNow instead.")
    private fun remove(view: View) = scheduleRemove(view)

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
    private val enterMillis: Long = 150L,
    private val exitMillis: Long = 120L,
) : android.graphics.drawable.Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val rect = RectF()
    private var alphaProgress = 0f
    private var exitRunnable: Runnable? = null
    private var onExitDone: (() -> Unit)? = null

    fun enter() {
        onExitDone?.let { return } // An exit is queued; don't clobber its callback.
        android.animation.ValueAnimator.ofFloat(alphaProgress, 1f).apply {
            duration = enterMillis
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                alphaProgress = anim.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    fun exit(onDone: () -> Unit) {
        if (onExitDone != null) return // already exiting
        onExitDone = onDone
        val anim = android.animation.ValueAnimator.ofFloat(alphaProgress, 0f)
        anim.duration = exitMillis
        anim.interpolator = android.view.animation.AccelerateInterpolator()
        anim.addUpdateListener { a ->
            alphaProgress = a.animatedValue as Float
            invalidateSelf()
        }
        anim.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) = Unit
            override fun onAnimationCancel(animation: android.animation.Animator) {
                onExitDone?.invoke()
                onExitDone = null
            }
            override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onExitDone?.invoke()
                onExitDone = null
            }
        })
        anim.start()
    }

    override fun draw(canvas: Canvas) {
        rect.set(
            bounds.left + spread,
            bounds.top + spread,
            bounds.right - spread,
            bounds.bottom - spread,
        )
        val p = alphaProgress.coerceIn(0f, 1f)
        if (p <= 0f) return
        drawOuterStroke(canvas, rect, 2f, 4f, (92 * p).toInt().coerceIn(0, 255))
        drawOuterStroke(canvas, rect, 5f, 8f, (56 * p).toInt().coerceIn(0, 255))
        drawOuterStroke(canvas, rect, 9f, 12f, (28 * p).toInt().coerceIn(0, 255))
    }

    private fun drawOuterStroke(canvas: Canvas, content: RectF, offset: Float, stroke: Float, alpha: Int) {
        if (alpha <= 0) return
        val half = stroke / 2f
        val glowRect = RectF(
            content.left - offset - half,
            content.top - offset - half,
            content.right + offset + half,
            content.bottom + offset + half,
        )
        paint.strokeWidth = stroke
        paint.color = Color.argb(alpha, 255, 255, 255)
        val glowRadius = radius + offset + half
        canvas.drawRoundRect(glowRect, glowRadius, glowRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        // alpha is driven by the enter/exit progress; ignore framework setAlpha
        // to keep the shadow lifecycle synchronized with the foreground anim.
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Android framework")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
