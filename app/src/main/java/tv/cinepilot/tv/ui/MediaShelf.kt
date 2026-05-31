package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
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
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): HorizontalScrollView {
    val shelf = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, dp(12), dp(6))
    }
    row.items().forEach { item ->
        val card = mediaCard(row, item, onOpen, loadImage)
        onCard(card, item)
        shelf.addView(card)
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(shelf)
    }
}

private fun ComponentActivity.mediaCard(
    row: HomeRow,
    item: MediaItemSummary,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): FrameLayout {
    val card = FrameLayout(this).apply {
        isFocusable = true
        isClickable = true
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
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TvColors.TextPrimary)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setBackgroundColor(Color.argb(210, 8, 13, 24))
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
        focusedView.scaleX = if (hasFocus) 1.08f else 1f
        focusedView.scaleY = if (hasFocus) 1.08f else 1f
        focusedView.elevation = if (hasFocus) dp(10).toFloat() else 0f
        title.setTextColor(if (hasFocus) TvColors.FocusText else TvColors.TextPrimary)
        title.setBackgroundColor(if (hasFocus) TvColors.AccentStrong else Color.argb(210, 8, 13, 24))
        (focusedView as FrameLayout).foreground = rounded(
            Color.TRANSPARENT,
            dp(TvRadius.Card),
            if (hasFocus) dp(5) else 0,
            TvColors.FocusRing,
        )
    }
    loadImage(poster, item, 240, 360)
    card.layoutParams = LinearLayout.LayoutParams(dp(TvSize.PosterWidth), dp(TvSize.PosterHeight)).apply {
        rightMargin = dp(TvSpacing.CardGap)
        bottomMargin = dp(TvSpacing.CardGap)
    }
    return card
}
