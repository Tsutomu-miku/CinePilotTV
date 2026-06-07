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
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.posterArtworkCell(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): FrameLayout {
    val cell = mediaCell(row, item, onFocus, onOpen)
    val poster = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(TvColors.PosterFallback)
    }
    cell.addView(poster, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    cell.addView(cellTitle(item, maxLines = 2), FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM,
    ))
    loadArtwork(poster, item, ArtworkTarget.POSTER, 208, 312)
    cell.layoutParams = LinearLayout.LayoutParams(
        dp(MediaWallTokens.PosterCellWidth),
        dp(MediaWallTokens.PosterCellHeight),
    ).apply {
        rightMargin = dp(MediaWallTokens.CellGap)
        bottomMargin = dp(MediaWallTokens.CellGap)
    }
    return cell
}

fun ComponentActivity.mediaCell(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
): FrameLayout {
    return FrameLayout(this).apply {
        isFocusable = true
        isClickable = true
        clipToOutline = true
        contentDescription = "${row.title()} ${item.name()}"
        setBackgroundColor(TvColors.PosterFallback)
        setOnClickListener { onOpen(row, item) }
        setOnFocusChangeListener { view, focused ->
            if (focused) onFocus(row, item)
            view.applyFocusOutline(focused, 7)
        }
    }
}

fun ComponentActivity.cellTitle(item: MediaItemSummary, maxLines: Int): TextView {
    return TextView(this).apply {
        text = item.name().ifBlank { item.id() }
        textSize = MediaWallType.CellTitle
        setTextColor(TvColors.TextPrimary)
        this.maxLines = maxLines
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        background = rounded(Color.argb(188, 0, 0, 0), 0)
        setPadding(dp(7), dp(5), dp(7), dp(6))
    }
}
