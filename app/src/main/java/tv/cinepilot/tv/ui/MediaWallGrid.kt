package tv.cinepilot.tv.ui

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.mediaWallGrid(
    presentation: HomeRowPresentation,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    val metrics = gridMetrics(presentation.visualStyle)
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
        presentation.row.items().chunked(metrics.columns).forEach { rowItems ->
            addView(mediaWallGridLine(presentation, rowItems, metrics, onCell, onFocus, onOpen, loadArtwork))
        }
    }
}

private fun ComponentActivity.mediaWallGridLine(
    presentation: HomeRowPresentation,
    items: List<MediaItemSummary>,
    metrics: GridMetrics,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(dp(MediaWallTokens.FocusOverflow), 0, dp(MediaWallTokens.FocusOverflow), 0)
        items.forEachIndexed { index, item ->
            val cell = if (presentation.visualStyle == RowVisualStyle.LANDSCAPE_RAIL) {
                landscapeArtworkCell(presentation.row, item, onFocus, onOpen, loadArtwork)
            } else {
                posterArtworkCell(presentation.row, item, onFocus, onOpen, loadArtwork)
            }
            cell.layoutParams = LinearLayout.LayoutParams(dp(metrics.cellWidth), dp(metrics.cellHeight)).apply {
                rightMargin = if (index == items.lastIndex) 0 else dp(metrics.gap)
                bottomMargin = dp(MediaWallTokens.CellGap)
            }
            onCell(cell, item)
            addView(cell)
        }
    }
}

private fun ComponentActivity.gridMetrics(style: RowVisualStyle): GridMetrics {
    val cellWidth = if (style == RowVisualStyle.LANDSCAPE_RAIL) {
        MediaWallTokens.LandscapeCellWidth
    } else {
        MediaWallTokens.PosterCellWidth
    }
    val cellHeight = if (style == RowVisualStyle.LANDSCAPE_RAIL) {
        MediaWallTokens.LandscapeCellHeight
    } else {
        MediaWallTokens.PosterCellHeight
    }
    val available = screenWidthDp() - MediaWallTokens.ScreenX * 2
    val columns = ((available + MediaWallTokens.GridMinGap) / (cellWidth + MediaWallTokens.GridMinGap))
        .coerceAtLeast(1)
    val gap = if (columns > 1) {
        ((available - columns * cellWidth) / (columns - 1))
            .coerceIn(MediaWallTokens.GridMinGap, MediaWallTokens.CellGap)
    } else {
        0
    }
    return GridMetrics(cellWidth, cellHeight, columns, gap)
}

private fun ComponentActivity.screenWidthDp(): Int {
    return (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
}

private data class GridMetrics(
    val cellWidth: Int,
    val cellHeight: Int,
    val columns: Int,
    val gap: Int,
)
