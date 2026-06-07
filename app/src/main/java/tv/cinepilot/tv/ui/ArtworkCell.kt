package tv.cinepilot.tv.ui

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

fun ComponentActivity.artworkCell(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): FrameLayout {
    val root = FrameLayout(this).apply {
        isFocusable = true
        isClickable = true
        clipToOutline = true
        contentDescription = "${row.title()} ${item.name()}"
        setBackgroundColor(TvColors.PosterFallback)
        setOnClickListener { onOpen(row, item) }
    }
    val poster = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(TvColors.PosterFallback)
    }
    val title = artworkFocusTitle(item)
    root.addView(poster, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    root.addView(title, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM,
    ))
    root.setOnFocusChangeListener { view, focused ->
        if (focused) {
            onFocus(row, item)
        }
        title.visibility = if (focused) View.VISIBLE else View.GONE
        view.applyFocusOutline(focused)
    }
    loadImage(poster, item, 224, 336)
    root.layoutParams = LinearLayout.LayoutParams(
        dp(MediaWallTokens.CellWidth),
        dp(MediaWallTokens.CellHeight),
    ).apply {
        rightMargin = dp(MediaWallTokens.CellGap)
        bottomMargin = dp(MediaWallTokens.CellGap)
    }
    return root
}

private fun ComponentActivity.artworkFocusTitle(item: MediaItemSummary): TextView {
    return TextView(this).apply {
        text = item.name().ifBlank { item.id() }
        textSize = MediaWallType.CellTitle
        setTextColor(TvColors.TextPrimary)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        visibility = View.GONE
        background = rounded(Color.argb(205, 0, 0, 0), 0)
        setPadding(dp(8), dp(6), dp(8), dp(7))
    }
}
