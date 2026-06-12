package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.HomeRow

/**
 * Show "全部剧集" / "全部电影" chips only when the server exposes matching user views
 * (typically 电影 / 电视节目 / Movies / TV Shows). Chip press opens an overview page with
 * dedicated rows: continue watching, next-up, unplayed, favorites, all.
 */
fun ComponentActivity.libraryOverviewChips(
    rows: List<HomeRow>,
    onLibraryOverview: (viewId: String, title: String, isSeries: Boolean) -> Unit,
): View? {
    val viewsRow = rows.firstOrNull { it.id() == "views" } ?: return null
    val candidates = viewsRow.items().mapNotNull { view ->
        val name = view.name()
        when {
            name.contains("电影") || name.contains("Movies", ignoreCase = true) ->
                Triple(view.id(), if (name.isBlank()) "全部电影" else "$name · 总览", false)
            name.contains("电视") || name.contains("Series", ignoreCase = true) ||
                    name.contains("Shows", ignoreCase = true) ->
                Triple(view.id(), if (name.isBlank()) "全部剧集" else "$name · 总览", true)
            else -> null
        }
    }
    if (candidates.isEmpty()) return null
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(MediaWallTokens.ScreenX), 0, dp(MediaWallTokens.ScreenX), dp(10))
    }
    candidates.forEach { (viewId, title, isSeries) ->
        strip.addView(TextView(this).apply {
            text = title
            textSize = InfuseTypeTokens.Secondary
            setTextColor(TvColors.TextSecondary)
            includeFontPadding = false
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GlassDrawable(
                TvColors.GlassTint,
                MediaWallTokens.FilterChipRadius.toFloat(),
                TvColors.GlassBorder,
            )
            setOnClickListener { onLibraryOverview(viewId, title, isSeries) }
            setOnFocusChangeListener { v, hasFocus ->
                v.background = GlassDrawable(
                    if (hasFocus) TvColors.GlassFocusTint else TvColors.GlassTint,
                    MediaWallTokens.FilterChipRadius.toFloat(),
                    if (hasFocus) TvColors.FocusRing else TvColors.GlassBorder,
                )
                (v as TextView).setTextColor(
                    if (hasFocus) TvColors.TextPrimary else TvColors.TextSecondary
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(MediaWallTokens.FilterChipHeight),
            ).apply { rightMargin = dp(MediaWallTokens.FilterChipGap) }
        })
    }
    return strip
}
