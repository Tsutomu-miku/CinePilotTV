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

fun ComponentActivity.homeShelfSection(
    row: HomeRow,
    onCard: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(InfuseLayoutTokens.ShelfTop), 0, 0)
        addView(homeShelfTitle(row.title()))
        addView(homeShelf(row, onCard, onFocus, onOpen, loadImage))
    }
}

private fun ComponentActivity.homeShelfTitle(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = InfuseTypeTokens.ShelfTitle
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(8))
    }
}

private fun ComponentActivity.homeShelf(
    row: HomeRow,
    onCard: (View, MediaItemSummary) -> Unit,
    onFocus: (HomeRow, MediaItemSummary) -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
): HorizontalScrollView {
    val shelf = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        row.items().forEach { item ->
            val card = infusePosterCard(row, item, onFocus, onOpen, loadImage)
            onCard(card, item)
            addView(card)
        }
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        clipChildren = false
        clipToPadding = false
        addView(shelf)
    }
}
