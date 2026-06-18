package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.compose.components.layout.FlowRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

// ── Badge / pill constants ──────────────────────────────────────────────

private val PillChipHeight = 26.dp
private val PillChipRadius = 8.dp

private val RatingBadgeHeight = 34.dp
private val RatingBadgeIconWidth = 58.dp

private val DoubanGreen = Color(0xFF2ECC71)
private val ImdbGold = Color(0xFFF5C518)

// ── Metadata pills ───────────────────────────────────────────────────────

@Composable
internal fun MetadataPills(badges: List<String>, palette: CinePilotPalette) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        badges.take(8).forEach { badge ->
            PillChip(
                text = badge,
                palette = palette,
                primary = false,
            )
        }
    }
}

@Composable
internal fun ProviderBadgeFlow(
    badges: List<Pair<String, String>>,
    palette: CinePilotPalette,
    onClick: (String) -> Unit,
) {
    FlowRow(
        horizontalGap = 6.dp,
        verticalGap = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        badges.take(5).forEach { (label, url) ->
            var focused by remember { mutableStateOf(false) }
            PillChip(
                text = label,
                palette = palette,
                primary = false,
                clickable = true,
                focused = focused,
                onFocusChange = { focused = it },
                onClick = { onClick(url) },
            )
        }
    }
}

@Composable
private fun PillChip(
    text: String,
    palette: CinePilotPalette,
    primary: Boolean = false,
    clickable: Boolean = false,
    focused: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val bgColor = when {
        primary -> palette.accentStrong
        focused -> palette.glassFocus
        else -> palette.glass
    }
    val textColor = when {
        primary -> palette.focusText
        focused -> palette.textPrimary
        else -> palette.textSecondary
    }
    val borderColor = when {
        primary -> palette.accentStrong
        focused -> palette.focusRing
        else -> palette.glassBorder.copy(alpha = 0.4f)
    }
    val interaction = remember { MutableInteractionSource() }
    val baseModifier = Modifier
        .height(PillChipHeight)
        .clip(RoundedCornerShape(PillChipRadius))
        .background(bgColor)
        .border(if (focused) TvDp.FocusRing else 0.5.dp, borderColor, RoundedCornerShape(PillChipRadius))
        .padding(horizontal = 10.dp)

    val finalModifier = if (clickable) {
        baseModifier
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
    } else {
        baseModifier
    }

    Box(finalModifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ── Rating badges (with colored icon backgrounds) ───────────────────────

@Composable
internal fun RatingBadgeRow(
    palette: CinePilotPalette,
    communityRating: Double,
    hasTmdb: Boolean,
    hasImdb: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // TMDB / 社区评分 — teal accent style
        IconRatingBadge(
            palette = palette,
            iconLabel = if (hasTmdb) "TMDb" else "评分",
            value = "%.0f%%".format(communityRating * 10.0),
            iconBgColor = palette.accent,
            iconTextColor = palette.focusText,
        )
        // 豆瓣 — green style
        IconRatingBadge(
            palette = palette,
            iconLabel = "豆瓣",
            value = "%.1f".format(communityRating),
            iconBgColor = DoubanGreen,
            iconTextColor = Color.White,
        )
        // IMDb — gold style
        if (hasImdb) {
            IconRatingBadge(
                palette = palette,
                iconLabel = "IMDb",
                value = "—",
                iconBgColor = ImdbGold,
                iconTextColor = Color.Black,
            )
        }
    }
}

@Composable
private fun IconRatingBadge(
    palette: CinePilotPalette,
    iconLabel: String,
    value: String,
    iconBgColor: Color,
    iconTextColor: Color,
) {
    val height = RatingBadgeHeight
    val iconWidth = RatingBadgeIconWidth

    Box(
        modifier = Modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon area — colored background
            Box(
                modifier = Modifier
                    .width(iconWidth)
                    .fillMaxHeight()
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = iconLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = iconTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            // Value area
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = value,
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
