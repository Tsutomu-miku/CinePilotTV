package tv.cinepilot.tv.ui

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
        setPadding(dp(TvSpacing.FocusInset), dp(TvSpacing.FocusInset), dp(12), dp(6))
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
        background = rounded(TvColors.SurfaceRaised, dp(TvRadius.Card))
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
        background = rounded(Color.argb(210, 8, 13, 24), dp(TvRadius.Card))
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
        focusedView.animate()
            .alpha(if (hasFocus) 1f else 0.96f)
            .translationZ(if (hasFocus) dp(8).toFloat() else 0f)
            .setDuration(120L)
            .start()
        title.setTextColor(if (hasFocus) TvColors.FocusText else TvColors.TextPrimary)
        title.background = rounded(
            if (hasFocus) TvColors.AccentStrong else Color.argb(210, 8, 13, 24),
            dp(TvRadius.Card),
        )
        (focusedView as FrameLayout).foreground = rounded(
            Color.TRANSPARENT,
            dp(TvRadius.Card),
            if (hasFocus) dp(4) else 0,
            TvColors.FocusRing,
        )
    }
    loadImage(poster, item, 200, 300)
    card.layoutParams = LinearLayout.LayoutParams(dp(TvSize.PosterWidth), dp(TvSize.PosterHeight)).apply {
        rightMargin = dp(TvSpacing.CardGap)
        bottomMargin = dp(TvSpacing.CardGap)
    }
    return card
}
