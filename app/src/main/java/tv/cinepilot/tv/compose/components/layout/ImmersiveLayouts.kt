package tv.cinepilot.tv.compose.components.layout

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

/**
 * Full-screen immersive stage with backdrop image, scrim, and left-side gradient.
 *
 * Used as the root container for detail and settings screens.
 */
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

/**
 * A 2-column grid of setting rows with a section title.
 *
 * Used in settings screens to organize options into pairs.
 */
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

/**
 * A glass-morphism info panel with title and body text.
 *
 * Used for side panels and info overlays.
 */
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
            .border(
                BorderStroke(0.5.dp, palette.glassBorder),
                RoundedCornerShape(TvDp.PanelRadius),
            )
            .padding(12.dp),
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

/**
 * A lazy page with vertical scrolling.
 *
 * @param content the LazyListScope content lambda
 */
@Composable
fun TvLazyPage(
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(TvDp.RowGap),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

/**
 * A lazy page with a title, palette, and scroll state callback — richer variant.
 *
 * Supports scroll state callbacks for integration with focus management.
 */
@Composable
fun TvLazyPage(
    title: String,
    palette: CinePilotPalette,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    onScrollIndex: (Int) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect { onScrollIndex(it) }
    }
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            BasicText(
                text = title,
                maxLines = 1,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.PageTitle,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(16.dp))
        }
        content()
    }
}
