package tv.cinepilot.tv.ui

import android.animation.ValueAnimator
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow

fun ComponentActivity.infusePosterCard(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): FrameLayout {
    val card = FrameLayout(this).apply {
        isFocusable = true
        isClickable = true
        clipToOutline = true
        contentDescription = "${row.title()} ${item.name()}"
        background = rounded(TvColors.PosterFallback, dp(TvRadius.Card), dp(1), TvColors.PosterBorder)
        setOnClickListener { onOpen(row, item) }
    }
    val poster = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(TvColors.PosterFallback)
    }
    val title = infusePosterTitle(item)
    card.addView(poster, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    card.addView(title, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM,
    ))
    card.setOnFocusChangeListener { focusedView, hasFocus ->
        if (hasFocus) {
            onFocus(row, item)
        }
        title.visibility = if (hasFocus) View.VISIBLE else View.GONE
        focusedView.animate()
            .alpha(if (hasFocus) 1f else 0.96f)
            .translationZ(if (hasFocus) dp(InfuseFocusTokens.ElevationDp).toFloat() else 0f)
            .setDuration(InfuseFocusTokens.DurationMs)
            .start()
        animatePosterRing(card, hasFocus)
    }
    loadImage(poster, item, 220, 330)
    card.layoutParams = LinearLayout.LayoutParams(
        dp(InfuseLayoutTokens.PosterWidth),
        dp(InfuseLayoutTokens.PosterHeight),
    ).apply {
        rightMargin = dp(InfuseLayoutTokens.PosterGap)
        bottomMargin = dp(InfuseLayoutTokens.PosterGap)
    }
    return card
}

private fun ComponentActivity.infusePosterTitle(item: MediaItemSummary): TextView {
    return TextView(this).apply {
        text = item.name().ifBlank { item.id() }
        textSize = InfuseTypeTokens.CardTitle
        setTextColor(TvColors.TextPrimary)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        visibility = View.GONE
        background = glassDrawable(TvRadius.Card)
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }
}

private fun ComponentActivity.animatePosterRing(card: FrameLayout, hasFocus: Boolean) {
    ValueAnimator.ofFloat(if (hasFocus) 0f else 1f, if (hasFocus) 1f else 0f).apply {
        duration = InfuseFocusTokens.DurationMs
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            card.foreground = rounded(
                Color.TRANSPARENT,
                dp(TvRadius.Card),
                (dp(InfuseFocusTokens.BorderDp) * progress).toInt(),
                focusRingAlpha(progress),
            )
        }
        start()
    }
}

private fun focusRingAlpha(progress: Float): Int {
    val alpha = (220 * progress).toInt().coerceIn(0, 220)
    return Color.argb(
        alpha,
        Color.red(TvColors.FocusRing),
        Color.green(TvColors.FocusRing),
        Color.blue(TvColors.FocusRing),
    )
}
