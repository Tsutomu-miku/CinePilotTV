package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity

object GlassTokens {
    const val BackdropBlur = 28f
    const val PanelRadius = 18
    const val ControlRadius = 14
    const val BorderAlpha = 118
    const val HighlightAlpha = 72
    const val ShadowElevation = 12
}

class GlassDrawable(
    private val tintColor: Int,
    private val radius: Float,
    private val borderColor: Int,
) : LayerDrawable(arrayOf(
    GradientDrawable().apply {
        orientation = GradientDrawable.Orientation.TOP_BOTTOM
        colors = intArrayOf(
            Color.argb(GlassTokens.HighlightAlpha, 255, 255, 255),
            tintColor,
            tintColor,
        )
        cornerRadius = radius
    },
    GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = radius
        setStroke(1, borderColor)
    },
)) {
    init {
        setLayerInset(1, 0, 0, 0, 0)
    }
}

fun ComponentActivity.glassPanel(
    radiusDp: Int = GlassTokens.PanelRadius,
    content: FrameLayout.() -> Unit = {},
): FrameLayout {
    return FrameLayout(this).apply {
        background = glassDrawable(radiusDp)
        elevation = dp(GlassTokens.ShadowElevation).toFloat()
        content()
    }
}

fun ComponentActivity.glassDrawable(radiusDp: Int = GlassTokens.PanelRadius): GlassDrawable {
    return GlassDrawable(
        tintColor = TvColors.GlassTint,
        radius = dp(radiusDp).toFloat(),
        borderColor = TvColors.GlassBorder,
    )
}

fun View.applyGlassFocus(hasFocus: Boolean) {
    animate()
        .translationZ(if (hasFocus) viewDp(10).toFloat() else 0f)
        .alpha(if (hasFocus) 1f else 0.96f)
        .setDuration(160L)
        .start()
    background = if (hasFocus) {
        GlassDrawable(TvColors.GlassFocusTint, viewDp(GlassTokens.ControlRadius).toFloat(), TvColors.FocusRing)
    } else {
        GlassDrawable(TvColors.GlassTint, viewDp(GlassTokens.ControlRadius).toFloat(), TvColors.GlassBorder)
    }
}

fun ImageView.applyBackdropBlur() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(RenderEffect.createBlurEffect(
            GlassTokens.BackdropBlur,
            GlassTokens.BackdropBlur,
            Shader.TileMode.CLAMP,
        ))
    }
}

private fun View.viewDp(value: Int): Int {
    return (value * resources.displayMetrics.density + 0.5f).toInt()
}
