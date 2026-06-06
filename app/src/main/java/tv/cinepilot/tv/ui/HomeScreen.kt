package tv.cinepilot.tv.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
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
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaTicks
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
    var fallbackFocusAssigned = false
    var restoredFocusAssigned = false
    val backdrop = cinematicBackdrop()
    val summaryTitle = homeSummaryTitle()
    val summaryMeta = homeSummaryMeta()

    val stage = homeStage(if (isSearchResults) "搜索结果" else "媒体库", backdrop) {
        addView(homeTopRail(
            title = if (isSearchResults) "搜索结果" else "媒体库",
            actions = listOf(
                compactIconAction("搜索", TvIcon.SEARCH, navigation.onSearch),
                compactIconAction("刷新", TvIcon.REFRESH, navigation.onRefresh),
                compactIconAction("账号", TvIcon.ACCOUNT, navigation.onSwitchAccount),
                compactIconAction("设置", TvIcon.SETTINGS, navigation.onSettings),
                compactIconAction("退出", TvIcon.LOGOUT, navigation.onLogout),
            ),
        ))
        addView(homeFocusSummary(summaryTitle, summaryMeta))
        addView(homeRowsContainer {
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
                                updateHomeArtwork(backdrop, summaryTitle, summaryMeta, item, loadBackdrop)
                                onFocusedCard(card)
                            } else if (!restoredFocusAssigned && !fallbackFocusAssigned) {
                                fallbackFocusAssigned = true
                                updateHomeArtwork(backdrop, summaryTitle, summaryMeta, item, loadBackdrop)
                                onFocusedCard(card)
                            }
                        },
                        onFocus = { focusedRow, item ->
                            updateHomeArtwork(backdrop, summaryTitle, summaryMeta, item, loadBackdrop)
                            onFocusItem(focusedRow, item)
                        },
                        onOpen = onOpen,
                        loadImage = loadImage,
                    ))
                }
            }
            val browseActions = mutableListOf<View>()
            if (navigation.canPageBackward) {
                browseActions.add(iconAction("上一页", TvIcon.BACK, navigation.onPreviousPage))
            }
            if (navigation.canPageForward) {
                browseActions.add(iconAction("下一页", TvIcon.FORWARD, navigation.onNextPage))
            }
            if (browseActions.isNotEmpty()) {
                addView(section("浏览"))
                addView(actionStrip(browseActions))
            }
        })
    }
    return stage
}

private fun ComponentActivity.homeStage(
    title: String,
    backdrop: ImageView,
    content: LinearLayout.() -> Unit,
): FrameLayout {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(TvSpacing.ScreenX), dp(22), dp(TvSpacing.ScreenX), dp(TvSpacing.ScreenBottom))
        content()
    }
    val scroll = ScrollView(this).apply {
        isFillViewport = true
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        contentDescription = title
        addView(container, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }
    return FrameLayout(this).apply {
        setBackgroundColor(TvColors.Background)
        addView(backdrop, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(View(this@homeStage).apply {
            background = rounded(Color.argb(174, 0, 0, 0), 0)
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        addView(scroll)
    }
}

private fun ComponentActivity.cinematicBackdrop(): ImageView {
    return ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.72f
        setBackgroundColor(TvColors.Background)
        applyBackdropBlur()
    }
}

private fun ComponentActivity.homeTopRail(title: String, actions: List<View>): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(this@homeTopRail).apply {
            text = "CinePilot TV · $title"
            textSize = 13f
            letterSpacing = 0.06f
            setTextColor(TvColors.AccentStrong)
            includeFontPadding = false
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(actionStrip(actions))
    }
}

private fun ComponentActivity.homeFocusSummary(title: TextView, metadata: TextView): View {
    return glassPanel {
        setPadding(dp(20), dp(18), dp(20), dp(18))
        addView(LinearLayout(this@homeFocusSummary).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(metadata)
        })
    }.apply {
        layoutParams = LinearLayout.LayoutParams(dp(540), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(44)
            bottomMargin = dp(24)
        }
    }
}

private fun ComponentActivity.homeRowsContainer(content: LinearLayout.() -> Unit): View {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        content()
    }
}

private fun ComponentActivity.homeSummaryTitle(): TextView {
    return TextView(this).apply {
        text = "选择媒体"
        textSize = 31f
        typeface = Typeface.DEFAULT
        setTextColor(TvColors.TextPrimary)
        includeFontPadding = false
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setLineSpacing(2f, 1.02f)
    }
}

private fun ComponentActivity.homeSummaryMeta(): TextView {
    return TextView(this).apply {
        text = "移动焦点浏览媒体库"
        textSize = TvType.Body
        setTextColor(TvColors.TextSecondary)
        includeFontPadding = false
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(10), 0, 0)
    }
}

private fun updateHomeArtwork(
    backdrop: ImageView,
    title: TextView,
    metadata: TextView,
    item: MediaItemSummary,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    title.text = item.name().ifBlank { item.id() }
    metadata.text = homeFocusMetadata(item)
    loadBackdrop(backdrop, item, 1280, 720)
}

private fun homeFocusMetadata(item: MediaItemSummary): String {
    val values = mutableListOf<String>()
    mediaTypeLabel(item.type()).takeIf { it.isNotBlank() }?.let(values::add)
    item.productionYear()?.let { values.add(it.toString()) }
    item.runTimeTicks()?.let { values.add(durationLabel(it)) }
    values.addAll(item.genres().take(2))
    if (item.hasResumePosition()) {
        values.add("可继续播放")
    }
    return values.ifEmpty { listOf("媒体库项目") }.joinToString(" · ")
}

private fun mediaTypeLabel(type: MediaItemType): String = when (type) {
    MediaItemType.MOVIE -> "电影"
    MediaItemType.SERIES -> "剧集"
    MediaItemType.SEASON -> "季"
    MediaItemType.EPISODE -> "单集"
    MediaItemType.VIDEO -> "视频"
    MediaItemType.COLLECTION_FOLDER,
    MediaItemType.FOLDER -> "目录"
    MediaItemType.UNKNOWN -> ""
}

private fun durationLabel(ticks: Long): String {
    val totalMinutes = (MediaTicks.toMilliseconds(ticks) / 60_000).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分钟"
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
    val onSettings: () -> Unit,
    val onLogout: () -> Unit,
    val onBackInBrowse: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)
