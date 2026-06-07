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
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.mediaWallRow(
    presentation: HomeRowPresentation,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    if (presentation.visualStyle == RowVisualStyle.COLLECTION_RAIL) {
        return collectionRail(presentation.row, onCell, onFocus, onOpen, loadArtwork)
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp(MediaWallTokens.RowGap))
        if (presentation.showTitle) {
            addView(mediaWallRowTitle(presentation.title))
        }
        addView(mediaWallStrip(presentation, onCell, onFocus, onOpen, loadArtwork))
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
    presentation: HomeRowPresentation,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): HorizontalScrollView {
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        presentation.row.items().forEach { item ->
            val cell = if (presentation.visualStyle == RowVisualStyle.LANDSCAPE_RAIL) {
                landscapeArtworkCell(presentation.row, item, onFocus, onOpen, loadArtwork)
            } else {
                posterArtworkCell(presentation.row, item, onFocus, onOpen, loadArtwork)
            }
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
