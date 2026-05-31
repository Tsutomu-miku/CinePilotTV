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
        background = rounded(Color.rgb(18, 26, 38), dp(10))
        setOnClickListener { onOpen(row, item) }
        setOnFocusChangeListener { focusedView, hasFocus ->
            focusedView.scaleX = if (hasFocus) 1.06f else 1f
            focusedView.scaleY = if (hasFocus) 1.06f else 1f
            (focusedView as FrameLayout).foreground = rounded(
                Color.TRANSPARENT,
                dp(10),
                if (hasFocus) dp(3) else 0,
                Color.rgb(94, 234, 212),
            )
        }
    }
    val poster = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Color.rgb(24, 34, 49))
    }
    card.addView(
        poster,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
    card.addView(
        TextView(this).apply {
            text = item.name().ifBlank { item.id() }
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundColor(Color.argb(210, 8, 13, 24))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        },
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ),
    )
    loadImage(poster, item, 320, 480)
    card.layoutParams = LinearLayout.LayoutParams(dp(190), dp(285)).apply {
        rightMargin = dp(18)
        bottomMargin = dp(18)
    }
    return card
}
