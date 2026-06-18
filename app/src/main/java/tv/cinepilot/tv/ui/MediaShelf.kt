package tv.cinepilot.tv.ui

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import java.util.WeakHashMap

fun ComponentActivity.mediaShelf(
    row: HomeRow,
    onCard: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): HorizontalScrollView {
    val shelf = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(dp(TvSpacing.FocusInset), dp(2), dp(12), dp(8))
    }
    row.items().forEach { item ->
        val card = mediaCard(row, item, onFocus, onOpen, loadImage)
        onCard(card, item)
        shelf.addView(card)
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        addView(shelf)
    }
}

private fun ComponentActivity.mediaCard(
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
    card.addView(
        poster,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
    val title = TextView(this).apply {
        text = item.name().ifBlank { item.id() }
        textSize = TvType.CardTitle
        setTextColor(TvColors.TextPrimary)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        visibility = View.GONE
        background = glassDrawable(TvRadius.Card)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }
    card.addView(
        title,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ),
    )
    card.setOnFocusChangeListener { focusedView, hasFocus ->
        if (hasFocus) {
            onFocus(row, item)
        }
        title.visibility = if (hasFocus) View.VISIBLE else View.GONE
        focusedView.animate()
            .alpha(if (hasFocus) 1f else 0.94f)
            .translationZ(if (hasFocus) dp(10).toFloat() else 0f)
            .setDuration(MEDIA_CARD_FOCUS_ANIMATION_MS)
            .start()
        title.setTextColor(if (hasFocus) TvColors.FocusText else TvColors.TextPrimary)
        animateMediaCardFocus(focusedView as FrameLayout, title, hasFocus)
    }
    poster.deferArtworkLoad {
        loadImage(poster, item, 200, 300)
    }
    card.layoutParams = LinearLayout.LayoutParams(dp(TvSize.PosterWidth), dp(TvSize.PosterHeight)).apply {
        rightMargin = dp(TvSpacing.CardGap)
        bottomMargin = dp(TvSpacing.CardGap)
    }
    return card
}

private fun ComponentActivity.animateMediaCardFocus(card: FrameLayout, title: TextView, hasFocus: Boolean) {
    title.background = GlassDrawable(
        TvColors.GlassTint,
        dp(TvRadius.Card).toFloat(),
        if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
    )

    mediaRingAnimators[card]?.cancel()
    mediaRingAnimators[card] = ValueAnimator.ofFloat(if (hasFocus) 0f else 1f, if (hasFocus) 1f else 0f).apply {
        duration = MEDIA_CARD_FOCUS_ANIMATION_MS
        addUpdateListener { animator ->
            val progress = animator.animatedValue as Float
            val strokeWidth = (dp(3) * progress).toInt()
            card.foreground = rounded(
                Color.TRANSPARENT,
                dp(TvRadius.Card),
                strokeWidth,
                focusRingWithAlpha(progress),
            )
        }
        start()
    }
}

private fun focusRingWithAlpha(progress: Float): Int {
    val alpha = (255 * progress).toInt().coerceIn(0, 255)
    return Color.argb(
        alpha,
        Color.red(TvColors.FocusRing),
        Color.green(TvColors.FocusRing),
        Color.blue(TvColors.FocusRing),
    )
}

private val mediaRingAnimators = WeakHashMap<FrameLayout, ValueAnimator>()
private const val MEDIA_CARD_FOCUS_ANIMATION_MS = 160L
