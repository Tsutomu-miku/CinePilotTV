package tv.cinepilot.tv.compose.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.ArtworkRequestSpec

// Re-exports — keep the old package working while the codebase migrates.
// New code should import directly from the sub-packages.

// ── Layout ──────────────────────────────────────────────────────────────
@Composable
fun ImmersiveStage(
    palette: CinePilotPalette,
    title: String,
    modifier: Modifier = Modifier,
    backdrop: ArtworkRequestSpec? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    tv.cinepilot.tv.compose.components.layout.ImmersiveStage(
        palette = palette,
        title = title,
        modifier = modifier,
        backdrop = backdrop,
        content = content,
    )
}

@Composable
fun SettingsGrid(
    palette: CinePilotPalette,
    title: String,
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    tv.cinepilot.tv.compose.components.layout.SettingsGrid(
        palette = palette,
        title = title,
        rows = rows,
        modifier = modifier,
        columns = columns,
    )
}

@Composable
fun InfoPanel(
    palette: CinePilotPalette,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    tv.cinepilot.tv.compose.components.layout.InfoPanel(
        palette = palette,
        title = title,
        body = body,
        modifier = modifier,
    )
}

@Composable
fun TvLazyPage(
    content: LazyListScope.() -> Unit,
) {
    tv.cinepilot.tv.compose.components.layout.TvLazyPage(
        content = content,
    )
}

// ── Media cards & rails ─────────────────────────────────────────────────
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
    tv.cinepilot.tv.compose.components.media.PosterCard(
        palette = palette,
        item = item,
        artwork = artwork,
        modifier = modifier,
        isFocused = isFocused,
        requestInitialFocus = requestInitialFocus,
        enabled = enabled,
        onFocus = onFocus,
        onClick = onClick,
    )
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
    tv.cinepilot.tv.compose.components.media.LandscapeCard(
        palette = palette,
        item = item,
        artwork = artwork,
        modifier = modifier,
        isFocused = isFocused,
        requestInitialFocus = requestInitialFocus,
        enabled = enabled,
        onFocus = onFocus,
        onClick = onClick,
    )
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
    tv.cinepilot.tv.compose.components.media.MediaRail(
        palette = palette,
        title = title,
        items = items,
        key = key,
        modifier = modifier,
        card = card,
    )
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
    tv.cinepilot.tv.compose.components.media.MediaRailIndexed(
        palette = palette,
        title = title,
        items = items,
        key = key,
        modifier = modifier,
        listState = listState,
        card = card,
    )
}
