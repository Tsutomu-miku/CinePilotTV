package tv.cinepilot.tv.compose.components.media

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.foundation.FocusSurface
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

/**
 * Internal shared artwork composable used by both poster and landscape cards.
 *
 * Handles the image, gradient overlay, title text, and focus border.
 */
@Composable
private fun MediaArtwork(
    palette: CinePilotPalette,
    artwork: ArtworkRequestSpec?,
    title: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TvDp.CardRadius))
            .background(palette.posterFallback)
            .border(0.5.dp, palette.glassBorder, RoundedCornerShape(TvDp.CardRadius)),
    ) {
        if (artwork != null) {
            CinePilotAsyncImage(
                request = artwork,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.0f),
                            palette.background.copy(alpha = 0.92f),
                        ),
                    ),
                )
                .padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            BasicText(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textPrimary, fontSize = TvText.CardTitle),
            )
        }
        if (focused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(TvDp.FocusRing, palette.focusRing, RoundedCornerShape(TvDp.CardRadius)),
            )
        }
    }
}

/**
 * A portrait poster card with artwork and title overlay.
 *
 * Standard dimensions from TvDp.PosterWidth / PosterHeight (2:3 aspect ratio).
 */
@Composable
fun PosterCard(
    palette: CinePilotPalette,
    item: tv.cinepilot.core.protocol.MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    modifier: Modifier = Modifier,
    isFocused: Boolean? = null,
    requestInitialFocus: Boolean = false,
    enabled: Boolean = true,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        modifier = modifier
            .width(TvDp.PosterWidth)
            .height(TvDp.PosterHeight),
        radius = TvDp.CardRadius,
        padding = PaddingValues(0.dp),
        isFocused = isFocused,
        requestInitialFocus = requestInitialFocus,
        focusedScale = 1f,
        enabled = enabled,
        onFocusChanged = { if (it) onFocus() },
        onClick = onClick,
    ) { focused ->
        MediaArtwork(
            palette = palette,
            artwork = artwork,
            title = item.name().ifBlank { item.id() },
            focused = focused,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * A landscape thumbnail card with artwork and title overlay.
 *
 * Standard dimensions from TvDp.LandscapeWidth / LandscapeHeight (16:9 aspect ratio).
 */
@Composable
fun LandscapeCard(
    palette: CinePilotPalette,
    item: tv.cinepilot.core.protocol.MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    modifier: Modifier = Modifier,
    isFocused: Boolean? = null,
    requestInitialFocus: Boolean = false,
    enabled: Boolean = true,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    FocusSurface(
        palette = palette,
        modifier = modifier
            .width(TvDp.LandscapeWidth)
            .height(TvDp.LandscapeHeight),
        radius = TvDp.CardRadius,
        padding = PaddingValues(0.dp),
        isFocused = isFocused,
        requestInitialFocus = requestInitialFocus,
        focusedScale = 1f,
        enabled = enabled,
        onFocusChanged = { if (it) onFocus() },
        onClick = onClick,
    ) { focused ->
        MediaArtwork(
            palette = palette,
            artwork = artwork,
            title = item.name().ifBlank { item.id() },
            focused = focused,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * A horizontal rail of media cards with a section title.
 *
 * Uses LazyRow for efficient scrolling. Cards are evenly sized and spaced.
 *
 * @param T the type of items in the rail
 */
@Composable
fun <T> MediaRail(
    palette: CinePilotPalette,
    title: String,
    items: List<T>,
    key: (T) -> String,
    modifier: Modifier = Modifier,
    card: @Composable (T) -> Unit,
) {
    MediaRailIndexed(
        palette = palette,
        title = title,
        items = items,
        key = key,
        modifier = modifier,
    ) { _, item ->
        card(item)
    }
}

/**
 * A horizontal rail with indexed access — for when focus state needs index tracking.
 *
 * @param T the type of items in the rail
 * @see MediaRail for the simpler version
 */
@Composable
fun <T> MediaRailIndexed(
    palette: CinePilotPalette,
    title: String,
    items: List<T>,
    key: (T) -> String,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    card: @Composable (Int, T) -> Unit,
) {
    val railState = listState ?: rememberLazyListState()
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Section),
        )
        Spacer(Modifier.height(5.dp))
        LazyRow(
            state = railState,
            horizontalArrangement = Arrangement.spacedBy(TvDp.CellGap),
            contentPadding = PaddingValues(start = 3.dp, top = 3.dp, end = 21.dp, bottom = 4.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> key(item) },
                contentType = { _, _ -> "media-card" },
            ) { index, item ->
                card(index, item)
            }
        }
    }
}
