package tv.cinepilot.tv.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

fun ComponentActivity.detailsInfoSections(
    trackControls: View?,
    technicalInfo: List<String>,
    overview: String,
): List<View> {
    val sections = mutableListOf<View>()
    trackControls?.let { controls ->
        sections.add(detailsInfoSection { addView(controls) })
    }
    if (technicalInfo.isNotEmpty()) {
        sections.add(detailsInfoSection {
            addView(infuseSectionTitle("媒体信息"))
            addView(metadataPills(technicalInfo.take(12)))
        })
    }
    if (overview.isNotBlank()) {
        sections.add(detailsInfoSection {
            addView(infuseSectionTitle("剧情简介"))
            addView(bodyText(overview))
        })
    }
    return sections
}

private fun ComponentActivity.detailsInfoSection(content: LinearLayout.() -> Unit): FrameLayout {
    return glassPanel {
        setPadding(
            dp(InfuseLayoutTokens.GlassPadding),
            dp(InfuseLayoutTokens.GlassPadding),
            dp(InfuseLayoutTokens.GlassPadding),
            dp(InfuseLayoutTokens.GlassPadding),
        )
        addView(LinearLayout(this@detailsInfoSection).apply {
            orientation = LinearLayout.VERTICAL
            content()
        })
    }.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(12)
        }
    }
}

private fun ComponentActivity.infuseSectionTitle(text: String): TextView {
    return TextView(this).apply {
        this.text = text
        textSize = InfuseTypeTokens.ShelfTitle
        setTextColor(TvColors.AccentStrong)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(8))
    }
}
