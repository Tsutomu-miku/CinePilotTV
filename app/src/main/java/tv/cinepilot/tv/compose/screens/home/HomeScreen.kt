package tv.cinepilot.tv.compose.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    val initialAddress = remember(state, displayRows) { initialFocusAddress(state, displayRows) }
    // rowIndex: -1 = TopBar, 0..lastIndex = media rows
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

    // TopBar 作为虚拟 row -1。把 FocusRequester 贴到第一个 HeaderIconButton
    // （通过 HomeTopBar 的参数传进去），这样 moveFocusToTopBar 就等于聚焦
    // 第一个可点的图标按钮，后续 LEFT/RIGHT 交给 Compose 自然 focus。
    val topBarFirstButtonFocusRequester = remember { FocusRequester() }

    fun moveFocusToTopBar() {
        focusedRowIndex = -1
        runCatching { topBarFirstButtonFocusRequester.requestFocus() }
    }
    fun moveFocusToFirstRow() {
        if (displayRows.isEmpty()) return
        focusedRowIndex = 0
        val items = displayRows.first().items()
        if (items.isNotEmpty()) {
            val clampedIndex = focusedItemIndex.coerceIn(0, items.lastIndex)
            focusedItemIndex = clampedIndex
        }
        runCatching { railFocusRequesters[0].requestFocus() }
    }

    // 首次 rows 从空变为非空时，只做一次初始聚焦，避免每一次重排都重新触发 focus 调度。
    var initialFocusBootstrapped by remember(displayRows) { mutableStateOf(false) }
    LaunchedEffect(displayRows.size) {
        val requestInitialFocus = !initialFocusBootstrapped
        if (requestInitialFocus) {
            delay(50)
            if (displayRows.isNotEmpty()) {
                val targetRow = initialAddress.rowIndex.coerceIn(0, displayRows.lastIndex)
                runCatching { railFocusRequesters[targetRow].requestFocus() }
            } else {
                runCatching { topBarFirstButtonFocusRequester.requestFocus() }
            }
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

    // 焦点停稳后再同步背景和 workflow focus，避免 D-pad 连按时在主线程频繁做大图请求和 row/item 校验。
    val latestOnFocusItem = rememberUpdatedState(onFocusItem)
    LaunchedEffect(focusedRowIndex, focusedItemIndex, displayRows) {
        val row = displayRows.getOrNull(focusedRowIndex) ?: return@LaunchedEffect
        val item = row.items().getOrNull(focusedItemIndex) ?: return@LaunchedEffect
        delay(280)
        backdropItem = item
        latestOnFocusItem.value(row, item)
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
     * At row 0 moves to the top bar (virtual row -1).
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
            runCatching { railFocusRequesters[nextRow].requestFocus() }
            return true
        }
        if (focusedRowIndex == 0 && displayRows.isNotEmpty()) {
            moveFocusToTopBar()
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
    val isSearchResults = remember(rows) { rows.any { it.id().startsWith("search:") } }
    HomeWallStage(
        palette = palette,
        backdrop = backdrop,
    ) {
        LazyColumn(
            state = columnState,
            verticalArrangement = Arrangement.spacedBy(TvDp.HomeRowVerticalGap),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = TvDp.ScreenTop,
                end = 0.dp,
                bottom = TvDp.ScreenBottom,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item(contentType = "home-top-bar") {
                // TopBar 外层：不自己占焦点，只负责 1) 检测任一子按钮是否有焦点
                // 以更新 focusedRowIndex=-1，2) 截获 DOWN 键切到 row 0。
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) focusedRowIndex = -1
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (event.key == Key.DirectionDown && displayRows.isNotEmpty()) {
                                moveFocusToFirstRow()
                                true
                            } else false
                        },
                ) {
                    HomeTopBar(
                        palette = palette,
                        title = homeTitle(state),
                        navigation = navigation,
                        firstButtonFocusRequester = topBarFirstButtonFocusRequester,
                        onMoveDown = {
                            moveFocusToFirstRow()
                            true
                        },
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
                        focusedRowIndex = focusedRowIndex,
                        focusedItemIndex = focusedItemIndex,
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
