package tv.cinepilot.tv.ui

import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

class HomeFocusHeader(
    val root: View,
    val title: TextView,
    val metadata: TextView,
)

fun ComponentActivity.homeFocusHeader(): HomeFocusHeader {
    val title = TextView(this).apply {
        textSize = MediaWallType.SummaryTitle
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val metadata = TextView(this).apply {
        textSize = MediaWallType.SummaryMeta
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(4), 0, 0)
    }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.INVISIBLE
        addView(title)
        addView(metadata)
    }
    return HomeFocusHeader(root, title, metadata)
}

fun updateHomeFocusHeader(header: HomeFocusHeader, item: MediaItemSummary) {
    val presentation = item.toMediaPresentation()
    header.root.visibility = View.VISIBLE
    header.title.text = presentation.title
    header.metadata.text = listOf(presentation.contextLine, presentation.watchState)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}
