package tv.cinepilot.tv.compose.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText

/**
 * A small semi-transparent badge chip used on media cards.
 *
 * Typically used to show technical info like resolution (4K, 1080p) or HDR formats.
 */
@Composable
fun CardBadgeChip(
    text: String,
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .border(0.5.dp, palette.glassBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = palette.textPrimary,
                fontSize = TvText.Label,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * A pill-shaped chip with glass/accent styling.
 *
 * Used for genre tags, provider badges, and other label-style chips.
 */
@Composable
fun PillChip(
    text: String,
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val bgColor = if (accent) palette.accent.copy(alpha = 0.28f) else palette.glass
    val borderColor = if (accent) palette.accentStrong else palette.glassBorder
    val textColor = if (accent) palette.accentStrong else palette.textSecondary

    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = textColor,
                fontSize = TvText.Label,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
