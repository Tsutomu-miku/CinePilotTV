package tv.cinepilot.tv.ui

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.detailsInfoSections(
    trackControls: View?,
    presentation: DetailPresentation,
): List<View> {
    val sections = mutableListOf<View>()
    trackControls?.let { controls ->
        sections.add(detailsInfoSection(top = 16) { addView(controls) })
    }
    if (presentation.overview.isNotBlank()) {
        sections.add(detailsInfoSection(top = 20) {
            addView(infuseSectionTitle("剧情简介"))
            addView(bodyText(presentation.overview))
        })
    }
    return sections
}

private fun ComponentActivity.detailsInfoSection(
    top: Int,
    content: LinearLayout.() -> Unit,
): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, 0)
        content()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(top)
        }
    }
}

private fun ComponentActivity.infuseSectionTitle(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = MediaWallType.RowTitle
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(8))
    }
}
