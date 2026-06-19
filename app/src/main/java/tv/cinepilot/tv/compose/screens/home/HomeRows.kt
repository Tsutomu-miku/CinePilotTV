package tv.cinepilot.tv.compose.screens.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.artwork.rememberArtworkRequest
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp
import tv.cinepilot.tv.compose.theme.TvText
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.RowVisualStyle
import tv.cinepilot.tv.ui.toHomeRowPresentation

@Composable
internal fun HomeMediaRow(
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
    val presentation = remember(row) { row.toHomeRowPresentation() }
    val style = presentation.visualStyle
    val railState = rememberLazyListState()
    val itemCount by remember { derivedStateOf { row.items().size } }
    val authenticated = state.authenticated()
    var rowHasFocus by remember(row.id()) { mutableStateOf(false) }
    val rowIsActive by remember(rowIndex, focusedRowIndex) {
        derivedStateOf { rowHasFocus || focusedRowIndex == rowIndex }
    }

    LaunchedEffect(rowIsActive, focusedItemIndex, itemCount) {
        if (rowIsActive && focusedItemIndex >= 0 && itemCount > 0) {
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

    fun handleKey(key: Key): Boolean {
        if (!rowIsActive) return false
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
            .padding(horizontal = 8.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focusState ->
                rowHasFocus = focusState.hasFocus
                if (focusState.hasFocus) onRowFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                handleKey(event.key)
            },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val rowTitle = presentation.title.ifBlank { row.title() }
            if (rowTitle.isNotBlank()) {
                BasicText(
                    text = rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = TvText.RowTitle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(12.dp))
            }
            LazyRow(
                state = railState,
                horizontalArrangement = Arrangement.spacedBy(TvDp.HomeRailCardGap),
                // 外层 8dp + 这里 16dp + 卡片 8dp Bleed → 卡片内容距屏幕边缘 32dp，与 ScreenX 对齐。
                // 右侧多留一点下一张卡片可见，提示可横向滚动。
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 24.dp, bottom = 20.dp),
            ) {
                itemsIndexed(
                    items = row.items(),
                    key = { _, item -> item.id() },
                    contentType = { _, _ -> "media-card" },
                ) { itemIndex, item ->
                    val isFocusedCard = rowIsActive && focusedItemIndex == itemIndex
                    // 统一成两类：9:16 海报 / 16:9 横版。COLLECTION 也走横版。
                    val isPoster = style == RowVisualStyle.POSTER_RAIL
                    val artworkTarget = if (isPoster) ArtworkTarget.POSTER else ArtworkTarget.LANDSCAPE
                    val (reqWidth, reqHeight) = if (isPoster) {
                        // 9:16 (TvDp.PosterWidth 108 × PosterHeight 192) × 4
                        432 to 768
                    } else if (row.id() == "resume") {
                        // 继续观看 16:9 大卡 216×122 × 4
                        864 to 488
                    } else {
                        // 横版标准（合集 / 最近添加 / 下一集 / 收藏夹等）16:9 176×99 × 4
                        704 to 396
                    }
                    val artwork = rememberArtworkRequest(
                        factory = artworkFactory,
                        authenticated = authenticated,
                        item = item,
                        target = artworkTarget,
                        width = reqWidth,
                        height = reqHeight,
                    )
                    val onCardClick = {
                        if (focusedRowIndex != rowIndex || focusedItemIndex != itemIndex) {
                            onRowFocused()
                            onItemIndexChanged(itemIndex)
                        }
                        onOpen(itemIndex)
                    }
                    when {
                        isPoster -> HomePosterCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            onClick = onCardClick,
                        )
                        style == RowVisualStyle.COLLECTION_RAIL -> HomeCollectionCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            onClick = onCardClick,
                        )
                        row.id() == "resume" -> HomeLandscapeCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            width = TvDp.ContinueWidth,
                            height = TvDp.ContinueHeight,
                            onClick = onCardClick,
                        )
                        else -> HomeLandscapeCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            width = TvDp.LandscapeWidth,
                            height = TvDp.LandscapeHeight,
                            onClick = onCardClick,
                        )
                    }
                }
            }
        }
    }
}
