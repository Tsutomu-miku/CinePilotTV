package tv.cinepilot.tv.ui

import android.graphics.Color
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
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.collectionRail(
    row: HomeRow,
    onCell: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View {
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        row.items().forEach { item ->
            val cell = collectionCell(row, item, onFocus, onOpen, loadArtwork)
            onCell(cell, item)
            addView(cell)
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        setPadding(0, 0, 0, dp(8))
        addView(strip)
    }
}

private fun ComponentActivity.collectionCell(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): FrameLayout {
    val cell = mediaCell(row, item, onFocus, onOpen).apply {
        background = rounded(
            Color.argb(MediaWallTokens.CollectionTintAlpha, 0, 0, 0),
            dp(MediaWallTokens.CollectionRadius),
            dp(1),
            homeHairlineColor(),
        )
    }
    val image = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.34f
    }
    cell.addView(image, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    cell.addView(TextView(this).apply {
        text = item.name().ifBlank { item.id() }
        textSize = MediaWallType.RowTitle
        setTextColor(TvColors.TextSecondary)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        gravity = Gravity.CENTER
        setPadding(dp(10), 0, dp(10), 0)
    }, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    loadArtwork(image, item, ArtworkTarget.COLLECTION, 300, 104)
    cell.layoutParams = LinearLayout.LayoutParams(
        dp(MediaWallTokens.CollectionCellWidth),
        dp(MediaWallTokens.CollectionCellHeight),
    ).apply {
        rightMargin = dp(MediaWallTokens.CellGap)
        bottomMargin = dp(8)
    }
    return cell
}
