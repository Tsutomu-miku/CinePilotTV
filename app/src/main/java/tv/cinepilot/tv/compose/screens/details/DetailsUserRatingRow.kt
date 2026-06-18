package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.compose.components.buttons.TvActionButton
import tv.cinepilot.tv.compose.foundation.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText

// ── Sizing / spacing constants for the user-rating row ──────────────────

private val StarButtonSize = 32.dp
private val StarButtonGap = 2.dp
private val StarTextSize = 20.sp
private val UserRatingRowHeight = 48.dp

// ── Public entry point ──────────────────────────────────────────────────

/**
 * Compose counterpart to `ComponentActivity.userRatingRow`.
 *
 * Five focusable ★ buttons mapped to 0–10 (each ★ = 2 points), with a readout
 * showing the user's exact score plus the community rating, and a clear
 * button shown whenever a rating is already set.
 *
 * Extracted to its own file so DetailsHero.kt / DetailsScreen.kt stay close
 * to the AGENTS.md 300-line soft cap.
 */
@Composable
internal fun UserRatingRow(
    palette: CinePilotPalette,
    currentRating: Double?,
    communityRating: Double?,
    onChange: (Double?) -> Unit,
) {
    val clampedZeroToTen = (currentRating ?: 0.0).coerceIn(0.0, 10.0)
    val starsToFill = (clampedZeroToTen / 2.0).coerceIn(0.0, 5.0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(UserRatingRowHeight)
            .padding(vertical = 10.dp),
    ) {
        (0 until 5).forEach { idx ->
            val filled = (idx + 1) <= starsToFill
            StarButton(
                palette = palette,
                filled = filled,
                index = idx,
                onChange = onChange,
            )
            Spacer(Modifier.width(StarButtonGap))
        }

        val exactLabel = currentRating?.let { "%.1f / 10".format(it) } ?: "未评分"
        val communityLabel = communityRating
            ?.takeIf { it > 0.0 }
            ?.let { " · 社区 %.1f".format(it) }
            ?: ""
        BasicText(
            text = exactLabel + communityLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )

        if (currentRating != null) {
            TvActionButton(
                palette = palette,
                label = "清除评分",
                onClick = { onChange(null) },
            )
        }
    }
}

// ── Single star button ──────────────────────────────────────────────────

@Composable
private fun StarButton(
    palette: CinePilotPalette,
    filled: Boolean,
    index: Int,
    onChange: (Double?) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val textColor = when {
        filled && focused -> palette.textPrimary
        filled -> palette.accentStrong
        focused -> palette.textPrimary
        else -> palette.textMuted
    }
    Box(
        modifier = Modifier
            .size(StarButtonSize)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onChange(((index + 1) * 2).toDouble()) },
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) palette.glassFocus else Color.Transparent,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (filled) "★" else "☆",
            style = TextStyle(
                color = textColor,
                fontSize = StarTextSize,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
