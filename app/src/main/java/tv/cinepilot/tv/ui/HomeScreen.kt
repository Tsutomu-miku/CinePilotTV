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
    return screen("首页") {
        addView(actionStrip(listOf(
            compactIconAction("搜索媒体", TvIcon.SEARCH, navigation.onSearch),
            compactIconAction("刷新", TvIcon.REFRESH, navigation.onRefresh),
            compactIconAction("退出", TvIcon.LOGOUT, navigation.onLogout),
        )))

        if (state.homeRows().isEmpty() || state.homeRows().all { it.items().isEmpty() }) {
            addView(emptyState("没有可显示的媒体"))
        }

        state.homeRows().forEach { row ->
            if (row.items().isNotEmpty()) {
                addView(section(row.title()))
                addView(mediaShelf(
                    row,
                    onCard = { card, item ->
                        if (state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()) {
                            onFocusedCard(card)
                        }
                    },
                    onOpen = onOpen,
                    loadImage = loadImage,
                ))
            }
        }

        val browseActions = mutableListOf<View>()
        if (navigation.canGoBack) {
            browseActions.add(iconAction("返回上级", TvIcon.BACK, navigation.onBackInBrowse))
        }
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

data class HomeNavigation(
    val canGoBack: Boolean,
    val canPageBackward: Boolean,
    val canPageForward: Boolean,
    val onSearch: () -> Unit,
    val onRefresh: () -> Unit,
    val onLogout: () -> Unit,
    val onBackInBrowse: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)
