package tv.cinepilot.tv.ui

import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary

class HomeHeroBinding(
    val root: View,
    val title: TextView,
    val metadata: TextView,
    val progress: TextView,
)

fun ComponentActivity.homeHero(): HomeHeroBinding {
    val title = TextView(this).apply {
        text = "选择媒体"
        textSize = InfuseTypeTokens.HeroTitle
        typeface = Typeface.DEFAULT
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setLineSpacing(2f, 1.02f)
    }
    val metadata = TextView(this).apply {
        text = "移动焦点浏览媒体库"
        textSize = InfuseTypeTokens.Body
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(10), 0, 0)
    }
    val progress = TextView(this).apply {
        textSize = InfuseTypeTokens.Metadata
        setTextColor(TvColors.AccentStrong)
        includeFontPadding = false
        visibility = View.GONE
        setPadding(0, dp(10), 0, 0)
    }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(InfuseLayoutTokens.HeroTop), 0, dp(InfuseLayoutTokens.HeroGap))
        addView(title)
        addView(metadata)
        addView(progress)
        layoutParams = LinearLayout.LayoutParams(dp(InfuseLayoutTokens.HeroWidth), LinearLayout.LayoutParams.WRAP_CONTENT)
    }
    return HomeHeroBinding(root, title, metadata, progress)
}

fun updateHomeHero(binding: HomeHeroBinding, item: MediaItemSummary) {
    val presentation = item.toMediaPresentation()
    binding.title.text = presentation.title
    binding.metadata.text = presentation.metadata.text().ifBlank { presentation.subtitle }
    binding.progress.text = presentation.progressLabel
    binding.progress.visibility = if (presentation.progressLabel.isBlank()) View.GONE else View.VISIBLE
}
