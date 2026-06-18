package tv.cinepilot.tv.compose.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tv.cinepilot.core.protocol.MediaBrowseFilters
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.BrowseFilterChipsRow
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget

@Composable
fun ComposeHomeScreen(
    palette: CinePilotPalette,
    owner: ComponentActivity,
    artworkFactory: ArtworkRequestFactory,
    state: TvAppState,
    browseFilters: MediaBrowseFilters,
    availableGenreNames: List<String>,
    navigation: ComposeHomeNavigation,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    onLibraryOverview: (viewId: String, title: String, isSeries: Boolean) -> Unit,
    onFiltersChanged: (MediaBrowseFilters) -> Unit,
) {
    val rows = state.homeRows()
    val displayRows = remember(rows) { rows.filter { row -> row.items().isNotEmpty() } }
    val isSearchResults = remember(rows) { rows.any { it.id().startsWith("search:") } }
    val isOverview = remember(rows) { rows.any { it.id().startsWith("overview:") } }
    val showFilters = isSearchResults || isOverview || (!isSearchResults && !isOverview)
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

    // 首次 rows 从空变为非空时，只做一次初始聚焦，避免每一次重排都重新触发 focus 调度。
    var initialFocusBootstrapped by remember(displayRows) { mutableStateOf(false) }
    // Request initial focus on the appropriate rail when rows become available.
    LaunchedEffect(displayRows.size) {
        if (!initialFocusBootstrapped && displayRows.isNotEmpty()) {
            delay(50)
            val targetRow = initialAddress.rowIndex.coerceIn(0, displayRows.lastIndex)
            runCatching { railFocusRequesters[targetRow].requestFocus() }
            initialFocusBootstrapped = true
        }
    }

    LaunchedEffect(focusedRowIndex, displayRows.size) {
        if (focusedRowIndex >= 0 && displayRows.isNotEmpty()) {
            val rowListIndex = focusedRowIndex + 1
            val visible = columnState.layoutInfo.visibleItemsInfo
            val firstVisible = visible.firstOrNull()?.index ?: 0
            val lastVisible = visible.lastOrNull()?.index ?: firstVisible
            // 只有目标行真正落到视窗之外时才 scroll，避免每次焦点变更都读 layoutInfo 再触发滚动
            if (rowListIndex < firstVisible || rowListIndex > lastVisible) {
                val targetListIndex = if (focusedRowIndex <= 1) 0 else rowListIndex - 1
                columnState.scrollToItem(targetListIndex.coerceAtLeast(0))
            }
        }
    }

    // backdrop 切换防抖：快速切焦点时不频繁加载图片，停顿 150ms 以上才更新
    val focusedAddressState = rememberUpdatedState(focusedRowIndex to focusedItemIndex)
    LaunchedEffect(Unit) {
        var lastRow = -1
        var lastItem = -1
        while (true) {
            val (r, i) = focusedAddressState.value
            if (r != lastRow || i != lastItem) {
                delay(150)
                val (rr, ii) = focusedAddressState.value
                if (rr != lastRow || ii != lastItem) {
                    lastRow = rr
                    lastItem = ii
                    backdropItem = displayRows
                        .getOrNull(rr)
                        ?.items()
                        ?.getOrNull(ii)
                }
            } else {
                delay(50)
            }
        }
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
            if (showFilters) {
                item(contentType = "home-filter-chips") {
                    BrowseFilterChipsRow(
                        palette = palette,
                        filters = browseFilters,
                        availableGenreNames = availableGenreNames,
                        onChanged = onFiltersChanged,
                    )
                }
            }
            if (displayRows.isEmpty()) {
                item(contentType = "home-empty") {
                    EmptyHome(
                        palette = palette,
                        searchResults = isSearchResults,
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

