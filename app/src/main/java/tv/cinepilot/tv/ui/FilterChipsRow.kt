package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaBrowseFilters

fun ComponentActivity.filterChipsRow(
    filters: MediaBrowseFilters,
    availableGenreNames: List<String>,
    onChanged: (MediaBrowseFilters) -> Unit,
): View {
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(MediaWallTokens.ScreenX), 0, dp(MediaWallTokens.ScreenX), dp(MediaWallTokens.FilterRowBottom))
    }
    if (!filters.isEmpty()) {
        strip.addView(filterChip("清除筛选", active = false) { onChanged(MediaBrowseFilters.EMPTY) })
    }
    availableGenreNames.forEach { genre ->
        val active = filters.genres() == genre
        strip.addView(filterChip(genre, active) {
            onChanged(if (active) MediaBrowseFilters.EMPTY.withGenre("") else filters.withGenre(genre))
        })
    }
    MediaBrowseFilters.decadeBuckets().forEach { decadeStart ->
        val decadeYears = (decadeStart until decadeStart + 10).joinToString(",")
        val active = filters.years() == decadeYears
        strip.addView(filterChip("${decadeStart}s", active) {
            onChanged(if (active) filters.withYearsCsv("") else filters.withYearsCsv(decadeYears))
        })
    }
    listOf(6f to "★6+", 7f to "★7+", 8f to "★8+", 9f to "★9+").forEach { (rating, label) ->
        val active = filters.minCommunityRating() == rating
        strip.addView(filterChip(label, active) {
            onChanged(if (active) filters.withMinRating(0f) else filters.withMinRating(rating))
        })
    }
    listOf(
        MediaBrowseFilters.FLAG_IS_UNPLAYED to "未观看",
        MediaBrowseFilters.FLAG_IS_PLAYED to "已看过",
        MediaBrowseFilters.FLAG_IS_FAVORITE to "已收藏",
    ).forEach { (flag, label) ->
        val active = filters.filterFlags().contains(flag)
        strip.addView(filterChip(label, active) {
            onChanged(filters.withFilterFlag(flag, !active))
        })
    }
    listOf(
        "only4K" to "4K",
        "onlyHdr" to "HDR",
    ).forEach { (key, label) ->
        val active = when (key) {
            "only4K" -> filters.only4K()
            "onlyHdr" -> filters.onlyHdr()
            else -> false
        }
        strip.addView(filterChip(label, active) {
            onChanged(
                when (key) {
                    "only4K" -> filters.withOnly4K(!active)
                    "onlyHdr" -> filters.withOnlyHdr(!active)
                    else -> filters
                }
            )
        })
    }
    return HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        addView(strip)
    }
}

private fun ComponentActivity.filterChip(label: String, active: Boolean, onClick: () -> Unit): View {
    return TextView(this).apply {
        text = label
        textSize = InfuseTypeTokens.Secondary
        setTextColor(if (active) TvColors.FocusText else TvColors.TextSecondary)
        includeFontPadding = false
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = GlassDrawable(
            if (active) TvColors.Accent else TvColors.GlassTint,
            MediaWallTokens.FilterChipRadius.toFloat(),
            if (active) TvColors.Accent else TvColors.GlassBorder,
        )
        setOnClickListener { onClick() }
        setOnFocusChangeListener { v, hasFocus ->
            v.background = GlassDrawable(
                when {
                    hasFocus -> TvColors.GlassFocusTint
                    active -> TvColors.Accent
                    else -> TvColors.GlassTint
                },
                MediaWallTokens.FilterChipRadius.toFloat(),
                when {
                    hasFocus -> TvColors.FocusRing
                    active -> TvColors.Accent
                    else -> TvColors.GlassBorder
                },
            )
            (v as TextView).setTextColor(
                when {
                    hasFocus -> TvColors.TextPrimary
                    active -> TvColors.FocusText
                    else -> TvColors.TextSecondary
                }
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(MediaWallTokens.FilterChipHeight),
        ).apply { rightMargin = dp(MediaWallTokens.FilterChipGap) }
    }
}
