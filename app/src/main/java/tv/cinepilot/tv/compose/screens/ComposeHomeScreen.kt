package tv.cinepilot.tv.compose.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.R
import tv.cinepilot.tv.compose.artwork.CinePilotAsyncImage
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.MediaRailIndexed
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.home.activeSearchTerm
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkRequestSpec
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.cardBadgeLabels
import tv.cinepilot.tv.ui.toHomeRowPresentation

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
    var focusedRowIndex by remember(displayRows) { mutableIntStateOf(initialAddress.rowIndex) }
    var focusedItemIndex by remember(displayRows) { mutableIntStateOf(initialAddress.itemIndex) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = 0.86f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.94f),
                            palette.background.copy(alpha = 0.62f),
                            palette.background.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = TvDp.ScreenX, top = TvDp.ScreenTop, end = TvDp.ScreenX, bottom = 0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeTopBar(palette: CinePilotPalette, title: String, navigation: ComposeHomeNavigation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDp.TopBarHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            // Brand: CinePilot TV
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(
                    text = "CinePilot",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.Brand,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = " TV",
                    maxLines = 1,
                    style = TextStyle(
                        color = palette.accentStrong,
                        fontSize = TvText.Brand,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = "/",
                maxLines = 1,
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Body),
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textPrimary, fontSize = TvText.Body, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = "|",
                maxLines = 1,
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = "媒体库  Cinema Library",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textSecondary, fontSize = TvText.Metadata),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            homeActions(navigation).forEach { action ->
                HeaderIconButton(
                    palette = palette,
                    iconRes = action.iconRes,
                    contentDescription = action.label,
                    onClick = action.onClick,
                )
            }
        }
    }
}

private fun homeActions(navigation: ComposeHomeNavigation): List<HomeAction> {
    return buildList {
        if (navigation.canGoBack) add(HomeAction("返回", R.drawable.ic_back, navigation.onBackInBrowse))
        if (navigation.canPageBackward) add(HomeAction("上一页", R.drawable.ic_back, navigation.onPreviousPage))
        if (navigation.canPageForward) add(HomeAction("下一页", R.drawable.ic_forward, navigation.onNextPage))
        add(HomeAction("搜索", R.drawable.ic_search, navigation.onSearch))
        add(HomeAction("刷新", R.drawable.ic_refresh, navigation.onRefresh))
        add(HomeAction("账号", R.drawable.ic_account, navigation.onSwitchAccount))
        add(HomeAction("设置", R.drawable.ic_settings, navigation.onSettings))
        add(HomeAction("退出", R.drawable.ic_logout, navigation.onLogout))
    }
}

@Composable
private fun HomeMediaRow(
    palette: CinePilotPalette,
    artworkFactory: ArtworkRequestFactory,
    state: TvAppState,
    row: HomeRow,
    rowIndex: Int,
    focusedRowIndex: Int,
    focusedItemIndex: Int,
    focusRequester: FocusRequester,
    onRowFocused: () -> Unit,
    onItemIndexChanged: (Int) -> Unit,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    onOpen: (itemIndex: Int) -> Unit,
) {
    val isFocusedRow = rowIndex == focusedRowIndex
    val presentation = remember(row) { row.toHomeRowPresentation() }
    val style = presentation.visualStyle
    val railState = rememberLazyListState()

    LaunchedEffect(focusedRowIndex, focusedItemIndex, row.items().size) {
        if (isFocusedRow && focusedItemIndex >= 0 && row.items().isNotEmpty()) {
            val visible = railState.layoutInfo.visibleItemsInfo
            val firstVisible = visible.firstOrNull()?.index ?: 0
            val lastVisible = visible.lastOrNull()?.index ?: firstVisible
            val targetIndex = when {
                focusedItemIndex <= firstVisible -> (focusedItemIndex - 1).coerceAtLeast(0)
                focusedItemIndex >= lastVisible -> (focusedItemIndex - 2).coerceAtLeast(0)
                else -> null
            }
            if (targetIndex != null && targetIndex != railState.firstVisibleItemIndex) {
                railState.scrollToItem(targetIndex)
            }
        }
    }

    /**
     * Handles key events for this rail. Only called when the rail container
     * has actual focus. Manages horizontal logical focus and Enter action.
     * UP/DOWN are handled via parent callbacks for reliable vertical navigation
     * between rows and the top bar.
     */
    fun handleKey(key: Key): Boolean {
        if (!isFocusedRow) return false
        val items = row.items()
        if (items.isEmpty()) return false
        return when (key) {
            Key.DirectionLeft -> {
                if (focusedItemIndex > 0) {
                    onItemIndexChanged(focusedItemIndex - 1)
                    true
                } else false
            }
            Key.DirectionRight -> {
                if (focusedItemIndex < items.lastIndex) {
                    onItemIndexChanged(focusedItemIndex + 1)
                    true
                } else false
            }
            Key.DirectionUp -> onMoveUp()
            Key.DirectionDown -> onMoveDown()
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                val safeIndex = focusedItemIndex.coerceIn(0, items.lastIndex)
                onOpen(safeIndex)
                true
            }
            else -> false
        }
    }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { if (it.isFocused) onRowFocused() }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                handleKey(event.key)
            },
    ) {
        MediaRailIndexed(
            palette = palette,
            title = presentation.title.ifBlank { row.title() },
            items = row.items(),
            key = { item -> item.id() },
            listState = railState,
        ) { itemIndex, item ->
            val artworkTarget = if (style == RowVisualStyle.POSTER_RAIL) ArtworkTarget.POSTER else ArtworkTarget.LANDSCAPE
            val artwork = rememberArtworkRequest(
                factory = artworkFactory,
                authenticated = state.authenticated(),
                item = item,
                target = artworkTarget,
                width = if (artworkTarget == ArtworkTarget.POSTER) 300 else 600,
                height = if (artworkTarget == ArtworkTarget.POSTER) 450 else 338,
            )
            val isFocusedCard = isFocusedRow && itemIndex == focusedItemIndex
            if (row.id() == "resume") {
                HomeLandscapeCard(
                    palette = palette,
                    item = item,
                    artwork = artwork,
                    focused = isFocusedCard,
                    width = TvDp.ContinueWidth,
                    height = TvDp.ContinueHeight,
                )
            } else if (style == RowVisualStyle.POSTER_RAIL) {
                HomePosterCard(
                    palette = palette,
                    item = item,
                    artwork = artwork,
                    focused = isFocusedCard,
                )
            } else {
                HomeLandscapeCard(
                    palette = palette,
                    item = item,
                    artwork = artwork,
                    focused = isFocusedCard,
                    width = TvDp.LandscapeWidth,
                    height = TvDp.LandscapeHeight,
                )
            }
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

private data class HomeAction(val label: String, val iconRes: Int, val onClick: () -> Unit)

@Composable
private fun HeaderIconButton(
    palette: CinePilotPalette,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(TvDp.ControlRadius)
    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(TvDp.IconButtonSize)
            .then(glowModifier)
            .clip(shape)
            .background(if (focused) palette.glassFocus else palette.glass.copy(alpha = 0.0f), shape)
            .border(
                width = if (focused) TvDp.FocusRing else 0.dp,
                color = if (focused) palette.focusRing else palette.glassBorder.copy(alpha = 0f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (focused) palette.accentStrong else palette.textPrimary),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HomePosterCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
) {
    HomeArtworkCard(
        palette = palette,
        item = item,
        artwork = artwork,
        focused = focused,
        width = TvDp.PosterWidth,
        height = TvDp.PosterHeight,
        overlayHeight = 64.dp,
        showProgressPercent = focused || item.hasResumePosition(),
    )
}

@Composable
private fun HomeLandscapeCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    width: Dp,
    height: Dp,
) {
    HomeArtworkCard(
        palette = palette,
        item = item,
        artwork = artwork,
        focused = focused,
        width = width,
        height = height,
        overlayHeight = (height * 0.58f),
        showProgressPercent = item.hasResumePosition(),
    )
}

@Composable
private fun HomeArtworkCard(
    palette: CinePilotPalette,
    item: MediaItemSummary,
    artwork: ArtworkRequestSpec?,
    focused: Boolean,
    width: Dp,
    height: Dp,
    overlayHeight: Dp,
    showProgressPercent: Boolean,
) {
    val shape = RoundedCornerShape(TvDp.CardRadius)
    val progress = item.resumeFraction()
    val badges = remember(item) { item.cardBadgeLabels() }
    // Glow / halo effect around the card when focused
    val glowModifier = if (focused) {
        Modifier.shadow(
            elevation = 12.dp,
            shape = shape,
            spotColor = palette.focusGlow,
            ambientColor = palette.focusGlow,
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(glowModifier)
            .clip(shape)
            .background(palette.posterFallback, shape)
            .border(
                width = if (focused) TvDp.FocusRing else 0.5.dp,
                color = if (focused) palette.focusRing else palette.glassBorder,
                shape = shape,
            ),
    ) {
        CinePilotAsyncImage(
            request = artwork,
            contentDescription = item.name(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 技术徽章（左上角）
        if (badges.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                badges.take(2).forEach { badge ->
                    CardBadgeChip(text = badge, palette = palette)
                }
            }
        }
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(overlayHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.0f),
                            palette.background.copy(alpha = 0.70f),
                            palette.background.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        // 文字信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        ) {
            BasicText(
                text = item.name().ifBlank { item.id() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = TvText.CardTitle,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = item.cardMetaLine(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = palette.textMuted, fontSize = TvText.Metadata),
            )
            // 播放进度条
            if (progress > 0f) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(palette.textPrimary.copy(alpha = 0.18f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .background(palette.accent),
                        )
                    }
                    if (showProgressPercent) {
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = "${(progress * 100f).toInt()}%",
                            maxLines = 1,
                            style = TextStyle(
                                color = palette.textMuted,
                                fontSize = TvText.Label,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardBadgeChip(text: String, palette: CinePilotPalette) {
    Box(
        modifier = Modifier
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

private fun MediaItemSummary.cardMetaLine(): String {
    if (type() == MediaItemType.EPISODE) {
        val season = parentIndexNumber()?.let { "S$it" }.orEmpty()
        val episode = indexNumber()?.let { "E$it" }.orEmpty()
        return listOf(season, episode).filter { it.isNotBlank() }.joinToString(" · ").ifBlank {
            seriesName().ifBlank { productionYear()?.toString().orEmpty() }
        }
    }
    val year = productionYear()?.toString().orEmpty()
    val duration = runTimeTicks()?.let(::durationLabel).orEmpty()
    return listOf(year, duration).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun durationLabel(ticks: Long): String {
    val minutes = (MediaTicks.toSeconds(ticks) / 60L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val rest = minutes % 60L
    return when {
        hours > 0L && rest > 0L -> "${hours}小时 ${rest}分钟"
        hours > 0L -> "${hours}小时"
        rest > 0L -> "${rest}分钟"
        else -> ""
    }
}

private fun MediaItemSummary.resumeFraction(): Float {
    val duration = runTimeTicks() ?: return 0f
    if (duration <= 0L || !hasResumePosition()) return 0f
    return (userData().playbackPositionTicks().toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private data class HomeFocusAddress(val rowIndex: Int, val itemIndex: Int)

private fun initialFocusAddress(state: TvAppState, rows: List<HomeRow>): HomeFocusAddress {
    val focus = state.focus()
    if (focus != null) {
        rows.forEachIndexed { rowIndex, row ->
            if (row.id() == focus.rowId()) {
                val itemIndex = row.items().indexOfFirst { item -> item.id() == focus.itemId() }
                if (itemIndex >= 0) return HomeFocusAddress(rowIndex, itemIndex)
            }
        }
    }
    return HomeFocusAddress(0, 0)
}

private fun homeTitle(state: TvAppState): String {
    if (state.activeSearchTerm().isNotBlank()) return "搜索：${state.activeSearchTerm()}"
    val overviewTitle = state.homeRows().firstOrNull { row -> row.id().startsWith("overview:") }?.title().orEmpty()
    return overviewTitle.ifBlank { "首页" }
}

private fun libraryOverviewTitle(item: MediaItemSummary): String {
    val name = item.name().ifBlank { "媒体库" }
    return "$name · 总览"
}

private fun MediaItemSummary.isSeriesLibrary(): Boolean {
    val name = name()
    return name.contains("电视") || name.contains("Series", ignoreCase = true) ||
        name.contains("Shows", ignoreCase = true)
}
