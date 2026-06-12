package tv.cinepilot.tv.details

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.HomeRowPresentation
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.MediaWallType
import tv.cinepilot.tv.ui.MediaWallTokens
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.TvFlowLayout
import tv.cinepilot.tv.ui.applyFocusOutline
import tv.cinepilot.tv.ui.detailsActions
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.homeHairlineColor
import tv.cinepilot.tv.ui.mediaWallRow
import tv.cinepilot.tv.ui.metadataPills
import tv.cinepilot.tv.ui.rounded

internal fun ComponentActivity.showHero(
    title: String,
    meta: String,
    badges: List<String>,
    overview: String,
    actions: List<InfuseAction>,
    providerBadges: List<tv.cinepilot.tv.ui.ProviderBadge> = emptyList(),
    onProviderBadgeClick: (String) -> Unit = {},
): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        clipChildren = false
        clipToPadding = false
        setPadding(0, dp(70), 0, dp(30))
        addView(LinearLayout(this@showHero).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            addView(showTitle(title))
            if (meta.isNotBlank()) {
                addView(showMeta(meta))
            }
            if (badges.isNotEmpty()) {
                addView(metadataPills(badges))
            }
            if (providerBadges.isNotEmpty()) {
                addView(showProviderBadges(providerBadges, onProviderBadgeClick))
            }
            if (overview.isNotBlank()) {
                addView(showOverview(overview))
            }
            addView(detailsActions(actions))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}

private fun ComponentActivity.showProviderBadges(
    badges: List<tv.cinepilot.tv.ui.ProviderBadge>,
    onClick: (String) -> Unit,
): View {
    return TvFlowLayout(this).apply {
        isFocusable = false
        setPadding(0, dp(2), 0, dp(2))
        badges.take(5).forEach { badge ->
            addView(TextView(this@showProviderBadges).apply {
                text = badge.label
                textSize = 11f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
                isFocusable = true
                isClickable = true
                setTextColor(TvColors.TextSecondary)
                background = rounded(
                    Color.argb(34, 255, 255, 255),
                    dp(8),
                    dp(1),
                    homeHairlineColor(64),
                )
                setPadding(dp(8), 0, dp(8), 0)
                setOnClickListener { onClick(badge.externalUrl) }
                setOnFocusChangeListener { view, focused ->
                    view.applyFocusOutline(focused, 8)
                    (view as? TextView)?.setTextColor(if (focused) TvColors.TextPrimary else TvColors.TextSecondary)
                }
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(26),
                ).apply {
                    rightMargin = dp(6)
                    bottomMargin = dp(4)
                }
            })
        }
    }
}

internal fun ComponentActivity.seasonRail(
    seasons: List<MediaItemSummary>,
    selectedSeason: MediaItemSummary?,
    onOpenSeason: (MediaItemSummary) -> Unit,
    onSelectSeason: ((MediaItemSummary) -> Unit)? = null,
    seasonsStartedStatus: Map<String, Boolean> = emptyMap(),
    onExpandFoldedSeasons: () -> Unit = {},
): View {
    val selectedId = selectedSeason?.id()
    val (normal, folded) = seasons.partition { season ->
        val id = season.id()
        id == selectedId ||
            !seasonsStartedStatus.getOrDefault(id, false) ||
            seasonsStartedStatus.isEmpty() // no preloaded status → render all (fallback)
    }
    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        normal.forEach { season ->
            addView(seasonChip(
                season = season,
                selected = selectedId == season.id(),
                onClick = {
                    if (selectedId == season.id()) {
                        onOpenSeason(season)
                    } else {
                        if (onSelectSeason != null) onSelectSeason(season) else onOpenSeason(season)
                    }
                },
            ))
        }
        if (folded.isNotEmpty()) {
            addView(seasonFoldedChip(folded.size, onExpandFoldedSeasons))
        }
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, dp(14))
        addView(showSectionTitle("季"))
        addView(HorizontalScrollView(this@seasonRail).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            setPadding(0, 0, 0, dp(2))
            addView(strip)
        })
    }
}

internal fun ComponentActivity.peopleStrip(
    title: String,
    people: List<MediaPerson>,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
    onClick: (MediaPerson) -> Unit = {},
): View? {
    if (people.isEmpty()) {
        return null
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(18))
        addView(showSectionTitle(title))
        addView(LinearLayout(this@peopleStrip).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
            people.take(10).forEach { person ->
                addView(personCard(person, loadPerson, onClick))
            }
        })
    }
}

internal fun ComponentActivity.sameCollectionRail(
    currentItem: MediaItemSummary,
    collection: List<MediaItemSummary>,
    onOpen: (MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
): View? {
    val items = collection.filterNot { it.id() == currentItem.id() }
    if (items.isEmpty()) return null
    return mediaWallRow(
        presentation = HomeRowPresentation(
            row = HomeRow("detail:collection:${currentItem.id()}", "同系列其他", items),
            title = "同系列其他",
            visualStyle = RowVisualStyle.POSTER_RAIL,
            wrapItems = false,
        ),
        onCell = { _, _ -> },
        onFocus = { _, _ -> },
        onOpen = { _, item -> onOpen(item) },
        loadArtwork = loadArtwork,
    )
}

internal fun ComponentActivity.showSection(title: String, content: TvFlowLayout.() -> Unit): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, dp(14))
        addView(showSectionTitle(title))
        addView(TvFlowLayout(this@showSection).apply(content))
    }
}

private fun ComponentActivity.showTitle(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = MediaWallType.DetailTitle
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        setLineSpacing(2f, 1.0f)
        setPadding(0, 0, 0, dp(10))
    }
}

private fun ComponentActivity.showMeta(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = MediaWallType.DetailMeta
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, dp(10))
    }
}

private fun ComponentActivity.showOverview(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
        setLineSpacing(2f, 1.05f)
        setPadding(0, dp(10), dp(160), dp(12))
    }
}

private fun ComponentActivity.showSectionTitle(value: String): TextView {
    return TextView(this).apply {
        text = value
        textSize = MediaWallType.RowTitle
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(8))
    }
}

private fun ComponentActivity.seasonChip(
    season: MediaItemSummary,
    selected: Boolean,
    onClick: () -> Unit,
): TextView {
    return TextView(this).apply {
        text = season.name().ifBlank { "第 ${season.indexNumber() ?: 1} 季" }
        textSize = 13f
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(if (selected) TvColors.TextPrimary else TvColors.TextSecondary)
        background = rounded(
            if (selected) Color.argb(58, 235, 242, 255) else Color.argb(28, 0, 0, 0),
            dp(MediaWallTokens.SeasonChipRadius),
            dp(1),
            if (selected) TvColors.FocusRing else homeHairlineColor(42),
        )
        setPadding(dp(16), 0, dp(16), 0)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { view, focused -> view.applyFocusOutline(focused, MediaWallTokens.SeasonChipRadius) }
        layoutParams = ViewGroup.MarginLayoutParams(
            dp(MediaWallTokens.SeasonChipWidth),
            dp(MediaWallTokens.SeasonChipHeight),
        ).apply {
            rightMargin = dp(MediaWallTokens.CellGap)
            bottomMargin = dp(MediaWallTokens.CellGap)
        }
    }
}

private fun ComponentActivity.seasonFoldedChip(
    foldedCount: Int,
    onClick: () -> Unit,
): TextView {
    val label = if (foldedCount <= 3) {
        "▶ S${foldedCount} · 尚未开始"
    } else {
        "▶ S${foldedCount} · 未开始的季"
    }
    return TextView(this).apply {
        text = label
        textSize = 13f
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(TvColors.TextSecondary)
        background = rounded(
            Color.argb(20, 0, 0, 0),
            dp(MediaWallTokens.SeasonChipRadius),
            dp(1),
            homeHairlineColor(38),
        )
        setPadding(dp(16), 0, dp(16), 0)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { view, focused ->
            view.applyFocusOutline(focused, MediaWallTokens.SeasonChipRadius)
            (view as? TextView)?.setTextColor(
                if (focused) TvColors.TextPrimary else TvColors.TextSecondary
            )
        }
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.MarginLayoutParams.WRAP_CONTENT,
            dp(MediaWallTokens.SeasonChipHeight),
        ).apply {
            rightMargin = dp(MediaWallTokens.CellGap)
            bottomMargin = dp(MediaWallTokens.CellGap)
        }
    }
}

private fun ComponentActivity.personCard(
    person: MediaPerson,
    loadPerson: (ImageView, MediaPerson, Int, Int) -> Unit,
    onClick: (MediaPerson) -> Unit,
): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        isFocusable = true
        isClickable = true
        clipChildren = false
        clipToPadding = false
        val image = ImageView(this@personCard).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(TvColors.PosterFallback, dp(9), dp(1), homeHairlineColor(38))
            clipToOutline = true
            loadPerson(this, person, 128, 128)
        }
        addView(image, LinearLayout.LayoutParams(dp(64), dp(64)))
        addView(personText(person.name(), TvColors.TextPrimary))
        addView(personText(person.role().ifBlank { person.type() }, TvColors.TextSecondary))
        setOnClickListener { onClick(person) }
        setOnFocusChangeListener { view, focused -> view.applyFocusOutline(focused, 10) }
        layoutParams = LinearLayout.LayoutParams(dp(108), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(12)
        }
    }
}

private fun ComponentActivity.personText(value: String, color: Int): TextView {
    return TextView(this).apply {
        text = value
        textSize = 11.5f
        setTextColor(color)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setPadding(0, dp(6), 0, 0)
    }
}
