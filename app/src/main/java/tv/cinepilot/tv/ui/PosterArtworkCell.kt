package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    cell.addView(cellTitleOverlay(item, maxLines = 2), FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        dp(cellTitleOverlayHeight(maxLines = 2)),
        Gravity.BOTTOM,
    ))
    episodeWatchedBadge(item)?.let(cell::addView)
    poster.deferArtworkLoad {
        loadArtwork(poster, item, ArtworkTarget.POSTER, 208, 312)
    }
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
        background = rounded(
            TvColors.PosterFallback,
            dp(MediaWallTokens.CellRadius),
            dp(1),
            homeHairlineColor(22),
        )
        setOnClickListener { onOpen(row, item) }
        setOnFocusChangeListener { view, focused ->
            if (focused) onFocus(row, item)
            view.applyFocusOutline(focused, MediaWallTokens.CellRadius)
        }
    }
}

fun ComponentActivity.cellTitleOverlay(item: MediaItemSummary, maxLines: Int): FrameLayout {
    return FrameLayout(this).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.TRANSPARENT, Color.argb(MediaWallTokens.TitleScrimAlpha, 0, 0, 0)),
        )
        addView(TextView(this@cellTitleOverlay).apply {
            text = item.name().ifBlank { item.id() }
            textSize = MediaWallType.CellTitle
            setTextColor(TvColors.TextPrimary)
            this.maxLines = maxLines
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
            setPadding(dp(7), 0, dp(7), dp(6))
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ))
    }
}

fun cellTitleOverlayHeight(maxLines: Int): Int = if (maxLines > 1) 46 else 34

fun homeHairlineColor(alpha: Int = MediaWallTokens.HairlineAlpha): Int {
    return Color.argb(alpha, 244, 247, 255)
}
