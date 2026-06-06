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
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
): View {
    val homeRows = state.homeRows()
    val hasNoMedia = homeRows.isEmpty() || homeRows.all { it.items().isEmpty() }
    val isSearchResults = homeRows.any { it.id().startsWith("search:") }
    val isEmptySearch = hasNoMedia && isSearchResults
    val backdrop = infuseBackdrop()
    val hero = homeHero()
    var fallbackFocusAssigned = false
    var restoredFocusAssigned = false

    return infuseStage(if (isSearchResults) "搜索结果" else "媒体库", backdrop) {
        addView(homeTopChrome(if (isSearchResults) "搜索结果" else "媒体库", navigation))
        addView(hero.root)
        if (hasNoMedia) {
            addView(emptyState(if (isEmptySearch) "没有找到匹配的媒体" else "没有可显示的媒体"))
            addView(if (isEmptySearch) searchEmptyActions(navigation) else homeEmptyActions(navigation))
        }
        homeRows.forEach { row ->
            if (row.items().isNotEmpty()) {
                addView(homeShelfSection(
                    row = row,
                    onCard = { card, item ->
                        val restored = state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()
                        if (restored) {
                            restoredFocusAssigned = true
                            updateHomeFocus(backdrop, hero, item, loadBackdrop)
                            onFocusedCard(card)
                        } else if (!restoredFocusAssigned && !fallbackFocusAssigned) {
                            fallbackFocusAssigned = true
                            updateHomeFocus(backdrop, hero, item, loadBackdrop)
                            onFocusedCard(card)
                        }
                    },
                    onFocus = { focusedRow, item ->
                        updateHomeFocus(backdrop, hero, item, loadBackdrop)
                        onFocusItem(focusedRow, item)
                    },
                    onOpen = onOpen,
                    loadImage = loadImage,
                ))
            }
        }
        addView(homeBrowseActions(navigation))
    }
}

private fun updateHomeFocus(
    backdrop: ImageView,
    hero: HomeHeroBinding,
    item: MediaItemSummary,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    updateHomeHero(hero, item)
    loadBackdrop(backdrop, item, 1280, 720)
}

private fun ComponentActivity.homeBrowseActions(navigation: HomeNavigation): View {
    val actions = mutableListOf<InfuseAction>()
    if (navigation.canPageBackward) {
        actions.add(InfuseAction("上一页", TvIcon.BACK, InfuseActionEmphasis.QUIET, navigation.onPreviousPage))
    }
    if (navigation.canPageForward) {
        actions.add(InfuseAction("下一页", TvIcon.FORWARD, InfuseActionEmphasis.QUIET, navigation.onNextPage))
    }
    return if (actions.isEmpty()) {
        View(this)
    } else {
        infuseActions(actions)
    }
}

private fun ComponentActivity.searchEmptyActions(navigation: HomeNavigation): View {
    val actions = mutableListOf(
        InfuseAction("重新搜索", TvIcon.SEARCH, InfuseActionEmphasis.PRIMARY, navigation.onSearch),
    )
    if (navigation.canGoBack) {
        actions.add(InfuseAction("返回首页", TvIcon.BACK, InfuseActionEmphasis.SECONDARY, navigation.onBackInBrowse))
    }
    actions.add(InfuseAction("切换账号", TvIcon.ACCOUNT, InfuseActionEmphasis.SECONDARY, navigation.onSwitchAccount))
    return infuseActions(actions, requestFirstFocus = true)
}

private fun ComponentActivity.homeEmptyActions(navigation: HomeNavigation): View {
    return infuseActions(listOf(
        InfuseAction("刷新", TvIcon.REFRESH, InfuseActionEmphasis.PRIMARY, navigation.onRefresh),
        InfuseAction("搜索媒体", TvIcon.SEARCH, InfuseActionEmphasis.SECONDARY, navigation.onSearch),
        InfuseAction("切换账号", TvIcon.ACCOUNT, InfuseActionEmphasis.SECONDARY, navigation.onSwitchAccount),
    ), requestFirstFocus = true)
}

data class HomeNavigation(
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
