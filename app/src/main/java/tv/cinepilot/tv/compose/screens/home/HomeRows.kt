package tv.cinepilot.tv.compose.screens.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    focusedRowIndexState: State<Int>,
    focusedItemIndexState: State<Int>,
    focusRequester: FocusRequester,
    onRowFocused: () -> Unit,
    onItemIndexChanged: (Int) -> Unit,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    onOpen: (itemIndex: Int) -> Unit,
) {
    val isFocusedRow by remember { derivedStateOf { focusedRowIndexState.value == rowIndex } }
    val focusedItemIndex by remember { derivedStateOf { focusedItemIndexState.value } }
    val presentation = remember(row) { row.toHomeRowPresentation() }
    val style = presentation.visualStyle
    val railState = rememberLazyListState()
    val itemCount by remember { derivedStateOf { row.items().size } }
    val authenticated = state.authenticated()

    LaunchedEffect(focusedRowIndexState.value, focusedItemIndexState.value, itemCount) {
        if (isFocusedRow && focusedItemIndex >= 0 && itemCount > 0) {
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
        Column(modifier = Modifier.fillMaxWidth()) {
            val rowTitle = presentation.title.ifBlank { row.title() }
            if (rowTitle.isNotBlank()) {
                BasicText(
                    text = rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(9.dp))
            }
            LazyRow(
                state = railState,
                horizontalArrangement = Arrangement.spacedBy(TvDp.CellGap),
                contentPadding = PaddingValues(start = 3.dp, top = 3.dp, end = 21.dp, bottom = 4.dp),
            ) {
                itemsIndexed(
                    items = row.items(),
                    key = { _, item -> item.id() },
                    contentType = { _, _ -> "media-card" },
                ) { itemIndex, item ->
                    val isFocusedCard by remember(itemIndex) {
                        derivedStateOf { isFocusedRow && focusedItemIndexState.value == itemIndex }
                    }
                    val artworkTarget = when (style) {
                        RowVisualStyle.POSTER_RAIL -> ArtworkTarget.POSTER
                        RowVisualStyle.COLLECTION_RAIL -> ArtworkTarget.COLLECTION
                        else -> ArtworkTarget.LANDSCAPE
                    }
                    val artwork = rememberArtworkRequest(
                        factory = artworkFactory,
                        authenticated = authenticated,
                        item = item,
                        target = artworkTarget,
                        width = when (artworkTarget) {
                            ArtworkTarget.POSTER -> 300
                            ArtworkTarget.COLLECTION -> 300
                            else -> 600
                        },
                        height = when (artworkTarget) {
                            ArtworkTarget.POSTER -> 450
                            ArtworkTarget.COLLECTION -> 104
                            else -> 338
                        },
                    )
                    val onCardClick = {
                        if (focusedRowIndexState.value != rowIndex || focusedItemIndexState.value != itemIndex) {
                            onRowFocused()
                            onItemIndexChanged(itemIndex)
                        }
                        onOpen(itemIndex)
                    }
                    if (style == RowVisualStyle.COLLECTION_RAIL) {
                        HomeCollectionCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            onClick = onCardClick,
                        )
                    } else if (row.id() == "resume") {
                        HomeLandscapeCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            width = TvDp.ContinueWidth,
                            height = TvDp.ContinueHeight,
                            onClick = onCardClick,
                        )
                    } else if (style == RowVisualStyle.POSTER_RAIL) {
                        HomePosterCard(
                            palette = palette,
                            item = item,
                            artwork = artwork,
                            focused = isFocusedCard,
                            onClick = onCardClick,
                        )
                    } else {
                        HomeLandscapeCard(
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
