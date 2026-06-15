package tv.cinepilot.tv.compose.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget

@Composable
fun ComposeHomeScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    state: TvAppState,
    navigation: ComposeHomeNavigation,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    onLibraryOverview: (viewId: String, title: String, isSeries: Boolean) -> Unit,
) {
    val rows = state.homeRows()
    val displayRows = remember(rows) { rows.filter { row -> row.items().isNotEmpty() && row.id() != "views" } }
    val initialAddress = remember(state, displayRows) { initialFocusAddress(state, displayRows) }
    val focusedRowIndexState = remember(displayRows) { mutableIntStateOf(initialAddress.rowIndex) }
    val focusedItemIndexState = remember(displayRows) { mutableIntStateOf(initialAddress.itemIndex) }
    var focusedRowIndex by focusedRowIndexState
    var focusedItemIndex by focusedItemIndexState
    val columnState = rememberLazyListState()
    val initialFocus = displayRows
        .getOrNull(initialAddress.rowIndex)
        ?.items()
        ?.getOrNull(initialAddress.itemIndex)
    var backdropItem by remember(state, displayRows) { mutableStateOf(initialFocus) }

    // One FocusRequester per rail. Recreated when the row count changes.
    val railFocusRequesters = remember(displayRows.size) {
        Array(displayRows.size.coerceAtLeast(1)) { FocusRequester() }
    }

    // Request initial focus on the appropriate rail when rows become available.
    LaunchedEffect(displayRows.size) {
        if (displayRows.isNotEmpty()) {
            delay(50)
            val targetRow = initialAddress.rowIndex.coerceIn(0, displayRows.lastIndex)
            runCatching { railFocusRequesters[targetRow].requestFocus() }
        }
    }

    LaunchedEffect(focusedRowIndex, displayRows.size) {
        if (focusedRowIndex >= 0 && displayRows.isNotEmpty()) {
            val rowListIndex = focusedRowIndex + 1
            val visibleIndexes = columnState.layoutInfo.visibleItemsInfo.map { item -> item.index }
            if (rowListIndex !in visibleIndexes) {
                val targetListIndex = if (focusedRowIndex <= 1) 0 else rowListIndex - 1
                columnState.scrollToItem(targetListIndex.coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(focusedRowIndex, focusedItemIndex, displayRows) {
        delay(150)
        backdropItem = displayRows
            .getOrNull(focusedRowIndex)
            ?.items()
            ?.getOrNull(focusedItemIndex)
    }

    /**
     * Called when a rail gains real focus. Updates the logical focused row,
     * clamps the item index to the new row's range, and notifies the callback.
     */
    fun onRowFocused(rowIndex: Int) {
        if (rowIndex == focusedRowIndex) return
        val row = displayRows.getOrNull(rowIndex) ?: return
        val items = row.items()
        if (items.isEmpty()) return
        val clampedIndex = focusedItemIndex.coerceIn(0, items.lastIndex)
        focusedRowIndex = rowIndex
        focusedItemIndex = clampedIndex
        onFocusItem(row, items[clampedIndex])
    }

    /**
     * Called when the focused item index changes within the currently focused rail.
     */
    fun onItemIndexChanged(rowIndex: Int, itemIndex: Int) {
        if (rowIndex != focusedRowIndex) return
        val row = displayRows.getOrNull(rowIndex) ?: return
        val items = row.items()
        if (itemIndex < 0 || itemIndex > items.lastIndex) return
        focusedItemIndex = itemIndex
        onFocusItem(row, items[itemIndex])
    }

    fun openItem(rowIndex: Int, itemIndex: Int) {
        val row = displayRows.getOrNull(rowIndex) ?: return
        val item = row.items().getOrNull(itemIndex) ?: return
        if (row.id() == "views") {
            onLibraryOverview(item.id(), libraryOverviewTitle(item), item.isSeriesLibrary())
        } else {
            onOpen(row, item)
        }
    }

    /**
     * Move focus up one row. Returns true if the move was handled.
     * If at row 0, returns false so focus can move to the top bar naturally.
     */
    fun moveUp(): Boolean {
        if (focusedRowIndex > 0) {
            val nextRow = focusedRowIndex - 1
            val row = displayRows[nextRow]
            val items = row.items()
            if (items.isEmpty()) return false
            val clampedIndex = focusedItemIndex.coerceIn(0, items.lastIndex)
            focusedRowIndex = nextRow
            focusedItemIndex = clampedIndex
            onFocusItem(row, items[clampedIndex])
            runCatching { railFocusRequesters[nextRow].requestFocus() }
            return true
        }
        return false
    }

    /**
     * Move focus down one row. Returns true if the move was handled.
     */
    fun moveDown(): Boolean {
        if (focusedRowIndex < displayRows.lastIndex) {
            val nextRow = focusedRowIndex + 1
            val row = displayRows[nextRow]
            val items = row.items()
            if (items.isEmpty()) return false
            val clampedIndex = focusedItemIndex.coerceIn(0, items.lastIndex)
            focusedRowIndex = nextRow
            focusedItemIndex = clampedIndex
            onFocusItem(row, items[clampedIndex])
            runCatching { railFocusRequesters[nextRow].requestFocus() }
            return true
        }
        return false
    }

    val backdrop = rememberArtworkRequest(
        factory = artworkFactory,
        authenticated = state.authenticated(),
        item = backdropItem,
        target = ArtworkTarget.BACKDROP,
        width = 1280,
        height = 720,
    )
    HomeWallStage(
        palette = palette,
        backdrop = backdrop,
    ) {
        LazyColumn(
            state = columnState,
            verticalArrangement = Arrangement.spacedBy(TvDp.RowGap),
            contentPadding = PaddingValues(bottom = TvDp.ScreenBottom),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item(contentType = "home-top-bar") {
                HomeTopBar(palette = palette, title = homeTitle(state), navigation = navigation)
            }
            if (displayRows.isEmpty()) {
                item(contentType = "home-empty") {
                    EmptyHome(
                        palette = palette,
                        searchResults = rows.any { it.id().startsWith("search:") },
                        navigation = navigation,
                    )
                }
            } else {
                itemsIndexed(
                    items = displayRows,
                    key = { _, row -> row.id() },
                    contentType = { _, _ -> "home-media-row" },
                ) { rowIndex, row ->
                    HomeMediaRow(
                        palette = palette,
                        artworkFactory = artworkFactory,
                        state = state,
                        row = row,
                        rowIndex = rowIndex,
                        focusedRowIndexState = focusedRowIndexState,
                        focusedItemIndexState = focusedItemIndexState,
                        focusRequester = railFocusRequesters[rowIndex],
                        onRowFocused = { onRowFocused(rowIndex) },
                        onItemIndexChanged = { itemIndex -> onItemIndexChanged(rowIndex, itemIndex) },
                        onMoveUp = ::moveUp,
                        onMoveDown = ::moveDown,
                        onOpen = { itemIndex -> openItem(rowIndex, itemIndex) },
                    )
                }
            }
        }
    }
}

data class ComposeHomeNavigation(
    val canGoBack: Boolean,
    val canPageBackward: Boolean,
    val canPageForward: Boolean,
    val onSearch: () -> Unit,
    val onRefresh: () -> Unit,
    val onSwitchAccount: () -> Unit,
    val onSettings: () -> Unit,
    val onLogout: () -> Unit,
    val onBackInBrowse: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)

@Composable
private fun HomeWallStage(
    palette: CinePilotPalette,
    backdrop: ArtworkRequestSpec?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
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
        // 整体暗化遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = 0.78f)),
        )
        // 左侧渐变：更宽、过渡更柔和
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to palette.background.copy(alpha = 0.96f),
                        0.35f to palette.background.copy(alpha = 0.82f),
                        0.6f to palette.background.copy(alpha = 0.5f),
                        0.85f to palette.background.copy(alpha = 0.18f),
                        1.0f to palette.background.copy(alpha = 0.05f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, top = 24.dp, end = 40.dp, bottom = 0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun EmptyHome(
    palette: CinePilotPalette,
    searchResults: Boolean,
    navigation: ComposeHomeNavigation,
) {
    InfoPanel(
        palette = palette,
        title = if (searchResults) "没有找到匹配的媒体" else "没有可显示的媒体",
        body = if (searchResults) "可以重新搜索，或返回首页浏览媒体库。" else "可以刷新首页，或切换账号后重试。",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TvActionButton(
            palette = palette,
            label = if (searchResults) "重新搜索" else "刷新",
            selected = true,
            requestInitialFocus = true,
            modifier = Modifier.weight(1f),
            onClick = if (searchResults) navigation.onSearch else navigation.onRefresh,
        )
        if (searchResults && navigation.canGoBack) {
            TvActionButton(
                palette = palette,
                label = "返回首页",
                modifier = Modifier.weight(1f),
                onClick = navigation.onBackInBrowse,
            )
        }
        TvActionButton(
            palette = palette,
            label = "切换账号",
            modifier = Modifier.weight(1f),
            onClick = navigation.onSwitchAccount,
        )
    }
}
