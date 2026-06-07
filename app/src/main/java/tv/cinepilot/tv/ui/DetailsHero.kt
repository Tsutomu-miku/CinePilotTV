package tv.cinepilot.tv.ui

import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

fun ComponentActivity.detailsHero(
    item: MediaItemSummary,
    presentation: MediaPresentation,
    actions: List<InfuseAction>,
    folderAction: InfuseAction,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.TOP
        clipChildren = false
        clipToPadding = false
        addView(detailsPoster(item, loadPoster))
        addView(LinearLayout(this@detailsHero).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, 0)
            addView(detailsTitle(presentation.title))
            if (presentation.subtitle.isNotBlank()) {
                addView(detailsSubtitle(presentation.subtitle))
            }
            addView(metadataPills(presentation.microMetadata))
            if (presentation.deliveryBadges.isNotEmpty()) {
                addView(metadataPills(presentation.deliveryBadges))
            }
            if (presentation.progressLabel.isNotBlank()) {
                addView(detailsProgress(presentation.progressLabel))
            }
            addView(detailsActions(if (item.playable()) actions else listOf(folderAction)))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}

private fun ComponentActivity.detailsPoster(
    item: MediaItemSummary,
    loadPoster: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): ImageView {
    return ImageView(this).apply {
        contentDescription = "${item.name()} 海报"
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackground(rounded(TvColors.PosterFallback, dp(TvRadius.Card), dp(1), TvColors.PosterBorder))
        clipToOutline = true
        elevation = dp(InfuseFocusTokens.ElevationDp).toFloat()
        loadPoster(this, item, 320, 480)
        layoutParams = LinearLayout.LayoutParams(
            dp(MediaWallTokens.DetailPosterWidth),
            dp(MediaWallTokens.DetailPosterHeight),
        ).apply {
            rightMargin = dp(MediaWallTokens.DetailGap)
        }
    }
}

private fun ComponentActivity.detailsTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = MediaWallType.DetailTitle
        typeface = Typeface.DEFAULT
        setTextColor(TvColors.TextPrimary)
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setLineSpacing(2f, 1.02f)
        setPadding(0, 0, 0, dp(8))
    }
}

private fun ComponentActivity.detailsSubtitle(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = MediaWallType.DetailMeta
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(8))
    }
}

private fun ComponentActivity.detailsProgress(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = MediaWallType.DetailMeta
        setTextColor(TvColors.AccentStrong)
        includeFontPadding = false
        setPadding(0, dp(6), 0, dp(8))
    }
}
