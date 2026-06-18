package tv.cinepilot.tv.compose.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

/** Shared backdrop + scrim stage used by both Details and the Episode row variant. */
@Composable
internal fun DetailsStage(
    backdrop: ArtworkRequestSpec?,
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop image at 84% opacity — matches original
        if (backdrop != null) {
            CinePilotAsyncImage(
                request = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.84f),
            )
        }
        // Dark base overlay — matches original Color.argb(42, 0, 0, 0) (16.5% black)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 42f / 255f)),
        )
        // Left-side readable scrim — wide gradient, softer transition
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(900.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 250f / 255f),
                            Color.Black.copy(alpha = 220f / 255f),
                            Color.Black.copy(alpha = 150f / 255f),
                            Color.Black.copy(alpha = 60f / 255f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // Scrollable content
        content()
    }
}

/** Section header used above rails and pill rows on the details page. */
@Composable
internal fun DetailSection(
    title: String,
    palette: CinePilotPalette,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                color = palette.textSecondary,
                fontSize = TvText.Section,
                fontWeight = FontWeight.Medium,
            ),
        )
        content()
    }
}
