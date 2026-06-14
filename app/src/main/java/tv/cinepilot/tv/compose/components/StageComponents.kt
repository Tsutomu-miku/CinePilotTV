package tv.cinepilot.tv.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
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
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

@Composable
fun ImmersiveStage(
    palette: CinePilotPalette,
    title: String,
    modifier: Modifier = Modifier,
    backdrop: ArtworkRequestSpec? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (backdrop != null) {
            CinePilotAsyncImage(
                request = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(920.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.96f),
                            palette.background.copy(alpha = 0.76f),
                            palette.background.copy(alpha = 0.0f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = TvDp.ScreenX,
                    top = TvDp.ScreenTop,
                    end = TvDp.ScreenX,
                    bottom = TvDp.ScreenBottom,
                ),
        ) {
            BasicText(
                text = "CinePilot TV",
                maxLines = 1,
                style = TextStyle(
                    color = palette.accentStrong,
                    fontSize = TvText.Brand,
                ),
            )
            Spacer(Modifier.height(5.dp))
            BasicText(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.PageTitle,
                ),
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
fun SettingsGrid(
    palette: CinePilotPalette,
    title: String,
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = title,
            maxLines = 1,
            style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
        )
        Spacer(Modifier.height(6.dp))
        rows.chunked(safeColumns).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp),
            ) {
                pair.forEach { row ->
                    Box(Modifier.weight(1f)) { row() }
                }
                repeat(safeColumns - pair.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun InfoPanel(
    palette: CinePilotPalette,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TvDp.PanelRadius))
            .background(palette.glass)
            .border(0.5.dp, palette.glassBorder, RoundedCornerShape(TvDp.PanelRadius))
            .padding(10.dp),
    ) {
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = palette.accentStrong, fontSize = TvText.Section),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = body,
            style = TextStyle(color = palette.textSecondary, fontSize = TvText.Body),
        )
    }
}

@Composable
fun PosterCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
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

@Composable
fun LandscapeCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
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
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(29.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.0f),
                            palette.background.copy(alpha = 0.86f),
                        ),
                    ),
                )
                .padding(horizontal = 5.dp, vertical = 4.dp),
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

@Composable
fun TvLazyPage(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(TvDp.RowGap),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}
