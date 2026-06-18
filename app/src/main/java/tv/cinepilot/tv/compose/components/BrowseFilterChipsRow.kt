package tv.cinepilot.tv.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaBrowseFilters
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

// ── Chip sizing constants ───────────────────────────────────────────────

private val ChipHeight = 30.dp
private val ChipHorizontalPadding = 14.dp
private val ChipGap = 8.dp
private val ChipBottomPadding = 10.dp
private val ChipFontSize = 13.5.sp

// ── Public entry point ──────────────────────────────────────────────────

/**
 * Compose filter-chips strip used on the home screen, search results, and
 * library overviews. Mirrors `ComponentActivity.filterChipsRow()`:
 *  1. 清除筛选 (when any filter is active)
 *  2. genre chips (single-select)
 *  3. decade 年代 chips
 *  4. 社区评分 chips (★6+ / ★7+ / ★8+ / ★9+)
 *  5. 观看状态 flags (未观看 / 已看过 / 已收藏)
 *  6. 质量 flags (4K / HDR)
 */
@Composable
fun BrowseFilterChipsRow(
    palette: CinePilotPalette,
    filters: MediaBrowseFilters,
    availableGenreNames: List<String>,
    onChanged: (MediaBrowseFilters) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        contentPadding = PaddingValues(
            start = TvDp.ScreenX,
            end = TvDp.ScreenX,
            bottom = ChipBottomPadding,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!filters.isEmpty) {
            item {
                FilterChip(
                    palette = palette,
                    label = "清除筛选",
                    active = false,
                    onClick = { onChanged(MediaBrowseFilters.EMPTY) },
                )
            }
        }
        items(availableGenreNames) { genre ->
            val active = filters.genres() == genre
            FilterChip(
                palette = palette,
                label = genre,
                active = active,
                onClick = {
                    onChanged(
                        if (active) filters.withGenre("") else filters.withGenre(genre)
                    )
                },
            )
        }
        items(MediaBrowseFilters.decadeBuckets().toList()) { decadeStart ->
            val decadeYears = (decadeStart until decadeStart + 10).joinToString(",")
            val active = filters.years() == decadeYears
            FilterChip(
                palette = palette,
                label = "${decadeStart}s",
                active = active,
                onClick = {
                    onChanged(
                        if (active) filters.withYearsCsv("") else filters.withYearsCsv(decadeYears)
                    )
                },
            )
        }
        items(RATING_PRESETS) { (rating, label) ->
            val active = filters.minCommunityRating() == rating
            FilterChip(
                palette = palette,
                label = label,
                active = active,
                onClick = {
                    onChanged(
                        if (active) filters.withMinRating(0f) else filters.withMinRating(rating)
                    )
                },
            )
        }
        items(FLAG_PRESETS) { (flag, label) ->
            val active = filters.filterFlags().contains(flag)
            FilterChip(
                palette = palette,
                label = label,
                active = active,
                onClick = { onChanged(filters.withFilterFlag(flag, !active)) },
            )
        }
        items(QUALITY_PRESETS) { (key, label) ->
            val active = when (key) {
                "only4K" -> filters.only4K()
                "onlyHdr" -> filters.onlyHdr()
                else -> false
            }
            FilterChip(
                palette = palette,
                label = label,
                active = active,
                onClick = {
                    onChanged(
                        when (key) {
                            "only4K" -> filters.withOnly4K(!active)
                            "onlyHdr" -> filters.withOnlyHdr(!active)
                            else -> filters
                        }
                    )
                },
            )
        }
    }
}

private val RATING_PRESETS = listOf(
    6f to "★6+",
    7f to "★7+",
    8f to "★8+",
    9f to "★9+",
)

private val FLAG_PRESETS = listOf(
    MediaBrowseFilters.FLAG_IS_UNPLAYED to "未观看",
    MediaBrowseFilters.FLAG_IS_PLAYED to "已看过",
    MediaBrowseFilters.FLAG_IS_FAVORITE to "已收藏",
)

private val QUALITY_PRESETS = listOf("only4K" to "4K", "onlyHdr" to "HDR")

// ── Single filter chip ──────────────────────────────────────────────────

@Composable
private fun FilterChip(
    palette: CinePilotPalette,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fill = when {
        focused -> palette.glassFocus
        active -> palette.accentStrong.copy(alpha = 0.18f)
        else -> palette.glass
    }
    val text = when {
        focused -> palette.textPrimary
        active -> palette.accentStrong
        else -> palette.textPrimary.copy(alpha = 0.85f)
    }
    val borderWidth = when {
        focused -> TvDp.FocusRing
        active -> 1.2.dp
        else -> 0.6.dp
    }
    val borderColor = when {
        focused -> palette.focusRing
        active -> palette.accentStrong.copy(alpha = 0.75f)
        else -> palette.glassBorder.copy(alpha = 0.55f)
    }
    Box(
        modifier = Modifier
            .height(ChipHeight)
            .clip(RoundedCornerShape(TvDp.ControlRadius))
            .background(fill)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(TvDp.ControlRadius),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = ChipHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = text,
                fontSize = ChipFontSize,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
