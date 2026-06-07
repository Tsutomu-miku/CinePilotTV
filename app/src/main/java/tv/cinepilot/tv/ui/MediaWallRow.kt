package tv.cinepilot.tv.ui

import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow

fun ComponentActivity.mediaWallRow(
    row: HomeRow,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp(MediaWallTokens.RowGap))
        addView(mediaWallRowTitle(row.title()))
        addView(mediaWallStrip(row, onCell, onFocus, onOpen, loadImage))
    }
}

private fun ComponentActivity.mediaWallRowTitle(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = MediaWallType.RowTitle
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(MediaWallTokens.RowTitleBottom))
    }
}

private fun ComponentActivity.mediaWallStrip(
    row: HomeRow,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): HorizontalScrollView {
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        row.items().forEach { item ->
            val cell = artworkCell(row, item, onFocus, onOpen, loadImage)
            onCell(cell, item)
            addView(cell)
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        addView(strip)
    }
}
