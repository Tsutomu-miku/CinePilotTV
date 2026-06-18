package tv.cinepilot.tv.ui

import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.landscapeArtworkCell(
    row: HomeRow,
    item: MediaItemSummary,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): FrameLayout {
    val cell = mediaCell(row, item, onFocus, onOpen)
    val image = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(TvColors.PosterFallback)
    }
    cell.addView(image, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    ))
    cell.addView(cellTitleOverlay(item, maxLines = 1), FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        dp(cellTitleOverlayHeight(maxLines = 1)),
        Gravity.BOTTOM,
    ))
    if (item.resumeFraction() > 0.0) {
        cell.addView(progressBar(item), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(2),
            Gravity.BOTTOM,
        ))
    }
    episodeWatchedBadge(item)?.let(cell::addView)
    image.deferArtworkLoad {
        loadArtwork(image, item, ArtworkTarget.LANDSCAPE, 376, 212)
    }
    cell.layoutParams = LinearLayout.LayoutParams(
        dp(MediaWallTokens.LandscapeCellWidth),
        dp(MediaWallTokens.LandscapeCellHeight),
    ).apply {
        rightMargin = dp(MediaWallTokens.CellGap)
        bottomMargin = dp(MediaWallTokens.CellGap)
    }
    return cell
}

private fun ComponentActivity.progressBar(item: MediaItemSummary): View {
    val fraction = item.resumeFraction()
    return FrameLayout(this).apply {
        setBackgroundColor(Color.argb(MediaWallTokens.ProgressTrackAlpha, 255, 255, 255))
        addView(View(this@progressBar).apply {
            setBackgroundColor(
                Color.argb(
                    MediaWallTokens.ProgressFillAlpha,
                    Color.red(TvColors.AccentStrong),
                    Color.green(TvColors.AccentStrong),
                    Color.blue(TvColors.AccentStrong),
                ),
            )
        }, FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            width = dp((MediaWallTokens.LandscapeCellWidth * fraction).toInt())
        })
    }
}

private fun MediaItemSummary.resumeFraction(): Double {
    val duration = runTimeTicks() ?: return 0.0
    if (duration <= 0L || !hasResumePosition()) {
        return 0.0
    }
    return (userData().playbackPositionTicks().toDouble() / duration.toDouble()).coerceIn(0.0, 1.0)
}
