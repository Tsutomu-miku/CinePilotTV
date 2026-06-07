package tv.cinepilot.tv.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.runtime.ArtworkTarget

fun ComponentActivity.homeScreen(
    state: TvAppState,
    navigation: HomeNavigation,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
): View {
    val homeRows = state.homeRows()
    val hasNoMedia = homeRows.isEmpty() || homeRows.all { it.items().isEmpty() }
    val isSearchResults = homeRows.any { it.id().startsWith("search:") }
    val isEmptySearch = hasNoMedia && isSearchResults
    val backdrop = cinematicBackdrop()
    val header = homeFocusHeader()
    var fallbackFocusAssigned = false
    var restoredFocusAssigned = false

    return cinematicStage(backdrop, scrollable = false) {
        addView(edgeChrome(homeChromeActions(navigation)), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            dp(MediaWallTokens.EdgeChromeSize),
            Gravity.TOP or Gravity.END,
        ).apply {
            topMargin = dp(MediaWallTokens.ScreenTop)
            rightMargin = dp(MediaWallTokens.ScreenX)
        })
        addView(header.root, FrameLayout.LayoutParams(
            dp(720),
            dp(MediaWallTokens.HeaderHeight),
            Gravity.TOP or Gravity.START,
        ).apply {
            topMargin = dp(MediaWallTokens.ScreenTop)
            leftMargin = dp(MediaWallTokens.ScreenX)
        })
        addView(mediaWallContent {
            if (isSearchResults) {
                addView(searchResultHint())
            }
            homeRows.forEach { row ->
                if (row.items().isNotEmpty()) {
                    val presentation = row.toHomeRowPresentation()
                    addView(mediaWallRow(
                        presentation = presentation,
                        onCell = { cell, item ->
                            val restored = state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()
                            if (restored) {
                                restoredFocusAssigned = true
                                updateHomeFocus(backdrop, header, item, loadBackdrop)
                                onFocusedCard(cell)
                            } else if (!restoredFocusAssigned && !fallbackFocusAssigned) {
                                fallbackFocusAssigned = true
                                updateHomeFocus(backdrop, header, item, loadBackdrop)
                                onFocusedCard(cell)
                            }
                        },
                        onFocus = { focusedRow, item ->
                            updateHomeFocus(backdrop, header, item, loadBackdrop)
                            onFocusItem(focusedRow, item)
                        },
                        onOpen = onOpen,
                        loadArtwork = loadArtwork,
                    ))
                }
            }
            addView(homeBrowseActions(navigation))
        })
        if (hasNoMedia) {
            addView(emptyOverlay(if (isEmptySearch) "没有找到匹配的媒体" else "没有可显示的媒体",
                if (isEmptySearch) searchEmptyActions(navigation) else homeEmptyActions(navigation)))
        }
    }
}

private fun updateHomeFocus(
    backdrop: ImageView,
    header: HomeFocusHeader,
    item: MediaItemSummary,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    updateHomeFocusHeader(header, item)
    loadBackdrop(backdrop, item, 1280, 720)
}

private fun ComponentActivity.mediaWallContent(content: LinearLayout.() -> Unit): View {
    val wall = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.ScreenTop + MediaWallTokens.HeaderHeight + 10),
            dp(MediaWallTokens.ScreenX),
            dp(MediaWallTokens.ScreenBottom),
        )
        content()
    }
    return ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        addView(wall, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }
}

private fun ComponentActivity.searchResultHint(): View {
    return TextView(this).apply {
        text = "搜索结果"
        textSize = MediaWallType.RowTitle
        setTextColor(TvColors.TextMuted)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(10))
    }
}

private fun ComponentActivity.emptyOverlay(text: String, actions: View): View {
    return sideSheet {
        addView(infusePanelTitle(text))
        addView(actions)
    }
}

private fun homeChromeActions(navigation: HomeNavigation): List<InfuseAction> {
    return listOf(
        InfuseAction("搜索", TvIcon.SEARCH, InfuseActionEmphasis.QUIET, navigation.onSearch),
        InfuseAction("刷新", TvIcon.REFRESH, InfuseActionEmphasis.QUIET, navigation.onRefresh),
        InfuseAction("账号", TvIcon.ACCOUNT, InfuseActionEmphasis.QUIET, navigation.onSwitchAccount),
        InfuseAction("设置", TvIcon.SETTINGS, InfuseActionEmphasis.QUIET, navigation.onSettings),
        InfuseAction("退出", TvIcon.LOGOUT, InfuseActionEmphasis.QUIET, navigation.onLogout),
    )
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
