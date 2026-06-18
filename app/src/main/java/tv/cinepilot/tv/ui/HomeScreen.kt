package tv.cinepilot.tv.ui

import android.os.Handler
import android.os.Looper
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
    onLibraryOverview: (viewId: String, title: String, isSeries: Boolean) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
): View {
    val homeRows = state.homeRows()
    val hasNoMedia = homeRows.isEmpty() || homeRows.all { it.items().isEmpty() }
    val isSearchResults = homeRows.any { it.id().startsWith("search:") }
    val isOverview = homeRows.any { it.id().startsWith("overview:") }
    val isEmptySearch = hasNoMedia && isSearchResults
    val backdrop = cinematicBackdrop(blurred = false).apply { alpha = 0.42f }
    val header = homeFocusHeader()
    val focusUpdater = HomeFocusUpdater(backdrop, header, loadBackdrop)
    var rowsAppender: HomeRowsAppender? = null
    var fallbackFocusAssigned = false
    var restoredFocusAssigned = false

    val root = cinematicStage(backdrop, scrollable = false) {
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
            if (!isSearchResults && !isOverview) {
                libraryOverviewChips(homeRows, onLibraryOverview)?.let(::addView)
            }
            val displayRows = homeRows.filter { it.items().isNotEmpty() }
            rowsAppender = HomeRowsAppender(
                rows = displayRows,
                addRow = { row ->
                    val presentation = row.toHomeRowPresentation()
                    addView(mediaWallRow(
                        presentation = presentation,
                        onCell = { cell, item ->
                            val restored = state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()
                            if (restored) {
                                restoredFocusAssigned = true
                                focusUpdater.update(item, immediate = true)
                                onFocusedCard(cell)
                            } else if (!restoredFocusAssigned && !fallbackFocusAssigned) {
                                fallbackFocusAssigned = true
                                focusUpdater.update(item, immediate = true)
                                onFocusedCard(cell)
                            }
                        },
                        onFocus = { focusedRow, item ->
                            focusUpdater.update(item, immediate = false)
                            onFocusItem(focusedRow, item)
                        },
                        onOpen = onOpen,
                        loadArtwork = loadArtwork,
                    ))
                },
                onFinished = {
                    addView(homeBrowseActions(navigation))
                },
            ).also { it.start() }
        })
        if (hasNoMedia) {
            addView(emptyOverlay(if (isEmptySearch) "没有找到匹配的媒体" else "没有可显示的媒体",
                if (isEmptySearch) searchEmptyActions(navigation) else homeEmptyActions(navigation)))
        }
    }
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit

        override fun onViewDetachedFromWindow(v: View) {
            focusUpdater.clear()
            rowsAppender?.clear()
        }
    })
    return root
}

private class HomeRowsAppender(
    private val rows: List<HomeRow>,
    private val addRow: (HomeRow) -> Unit,
    private val onFinished: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var nextIndex = 0
    private var finished = false
    private val appendNextRow = Runnable {
        appendRows(1)
        scheduleNext()
    }

    fun start() {
        appendRows(3)
        scheduleNext()
    }

    fun clear() {
        handler.removeCallbacks(appendNextRow)
        finished = true
    }

    private fun appendRows(count: Int) {
        repeat(count) {
            if (nextIndex >= rows.size) {
                return
            }
            addRow(rows[nextIndex])
            nextIndex += 1
        }
    }

    private fun scheduleNext() {
        if (finished) {
            return
        }
        if (nextIndex >= rows.size) {
            finished = true
            onFinished()
            return
        }
        handler.postDelayed(appendNextRow, 48L)
    }
}

private class HomeFocusUpdater(
    private val backdrop: ImageView,
    private val header: HomeFocusHeader,
    private val loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingItem: MediaItemSummary? = null
    private var pendingBackdropId: String? = null
    private var loadedBackdropId: String? = null
    private val loadPendingBackdrop = Runnable {
        val item = pendingItem ?: return@Runnable
        loadBackdropNow(item)
    }

    fun update(item: MediaItemSummary, immediate: Boolean) {
        updateHomeFocusHeader(header, item)
        val itemId = item.id()
        if (itemId == loadedBackdropId || itemId == pendingBackdropId) {
            return
        }
        pendingItem = item
        pendingBackdropId = itemId
        handler.removeCallbacks(loadPendingBackdrop)
        if (immediate) {
            loadBackdropNow(item)
        } else {
            handler.postDelayed(loadPendingBackdrop, 150L)
        }
    }

    fun clear() {
        handler.removeCallbacks(loadPendingBackdrop)
        pendingItem = null
        pendingBackdropId = null
    }

    private fun loadBackdropNow(item: MediaItemSummary) {
        val itemId = item.id()
        pendingBackdropId = null
        if (itemId == loadedBackdropId) {
            return
        }
        loadedBackdropId = itemId
        loadBackdrop(backdrop, item, 960, 540)
    }
}

private fun ComponentActivity.mediaWallContent(content: LinearLayout.() -> Unit): View {
    val wall = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
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
        clipChildren = false
        clipToPadding = false
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
