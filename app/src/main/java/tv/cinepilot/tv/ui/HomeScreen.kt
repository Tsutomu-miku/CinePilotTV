package tv.cinepilot.tv.ui

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState

fun ComponentActivity.homeScreen(
    state: TvAppState,
    navigation: HomeNavigation,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
): View {
    val homeRows = state.homeRows()
    val hasNoMedia = homeRows.isEmpty() || homeRows.all { it.items().isEmpty() }
    val isSearchResults = homeRows.any { it.id().startsWith("search:") }
    val isEmptySearch = hasNoMedia && isSearchResults
    var fallbackFocusAssigned = false
    var restoredFocusAssigned = false

    return screen(if (isSearchResults) "搜索结果" else "首页") {
        addView(actionStrip(listOf(
            compactIconAction("搜索媒体", TvIcon.SEARCH, navigation.onSearch),
            compactIconAction("刷新", TvIcon.REFRESH, navigation.onRefresh),
            compactIconAction("切换账号", TvIcon.ACCOUNT, navigation.onSwitchAccount),
            compactIconAction("退出", TvIcon.LOGOUT, navigation.onLogout),
        )))

        if (hasNoMedia) {
            addView(emptyState(if (isEmptySearch) "没有找到匹配的媒体" else "没有可显示的媒体"))
            addView(if (isEmptySearch) searchEmptyActions(navigation) else homeEmptyActions(navigation))
        }

        homeRows.forEach { row ->
            if (row.items().isNotEmpty()) {
                addView(section(row.title()))
                addView(mediaShelf(
                    row,
                    onCard = { card, item ->
                        val isRestoredFocus = state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()
                        if (isRestoredFocus) {
                            restoredFocusAssigned = true
                            onFocusedCard(card)
                        } else if (!restoredFocusAssigned && !fallbackFocusAssigned) {
                            fallbackFocusAssigned = true
                            onFocusedCard(card)
                        }
                    },
                    onOpen = onOpen,
                    loadImage = loadImage,
                ))
            }
        }

        val browseActions = mutableListOf<View>()
        if (navigation.canPageBackward) {
            browseActions.add(action("上一页", navigation.onPreviousPage))
        }
        if (navigation.canPageForward) {
            browseActions.add(action("下一页", navigation.onNextPage))
        }
        if (browseActions.isNotEmpty()) {
            addView(section("浏览"))
            addView(actionStrip(browseActions))
        }
    }
}

private fun ComponentActivity.searchEmptyActions(navigation: HomeNavigation): View {
    val actions = mutableListOf<View>(
        iconAction("重新搜索", TvIcon.SEARCH, navigation.onSearch).requestInitialFocus(),
    )
    if (navigation.canGoBack) {
        actions.add(iconAction("返回首页", TvIcon.BACK, navigation.onBackInBrowse))
    }
    actions.add(iconAction("切换账号", TvIcon.ACCOUNT, navigation.onSwitchAccount))
    return actionStrip(actions)
}

private fun ComponentActivity.homeEmptyActions(navigation: HomeNavigation): View {
    return actionStrip(listOf(
        iconAction("刷新", TvIcon.REFRESH, navigation.onRefresh).requestInitialFocus(),
        iconAction("搜索媒体", TvIcon.SEARCH, navigation.onSearch),
        iconAction("切换账号", TvIcon.ACCOUNT, navigation.onSwitchAccount),
    ))
}

data class HomeNavigation(
    val canGoBack: Boolean,
    val canPageBackward: Boolean,
    val canPageForward: Boolean,
    val onSearch: () -> Unit,
    val onRefresh: () -> Unit,
    val onSwitchAccount: () -> Unit,
    val onLogout: () -> Unit,
    val onBackInBrowse: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)
