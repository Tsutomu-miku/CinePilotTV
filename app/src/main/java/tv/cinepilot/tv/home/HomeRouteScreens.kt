package tv.cinepilot.tv.home

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.SearchFilter
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.HomeNavigation
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.actionStrip
import tv.cinepilot.tv.ui.choiceAction
import tv.cinepilot.tv.ui.compactIconAction
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.homeScreen
import tv.cinepilot.tv.ui.input
import tv.cinepilot.tv.ui.screen

fun ComponentActivity.homeRouteScreen(
    state: TvAppState,
    canGoBack: Boolean,
    canPageBackward: Boolean,
    canPageForward: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onBackInBrowse: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    loadImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
) = homeScreen(
    state = state,
    navigation = HomeNavigation(
        canGoBack = canGoBack,
        canPageBackward = canPageBackward,
        canPageForward = canPageForward,
        onSearch = onSearch,
        onRefresh = onRefresh,
        onLogout = onLogout,
        onBackInBrowse = onBackInBrowse,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
    ),
    onOpen = onOpen,
    loadImage = loadImage,
    onFocusedCard = onFocusedCard,
)

fun ComponentActivity.searchScreen(
    initialTerm: String,
    selectedFilter: SearchFilter,
    onFilter: (SearchFilter, String) -> Unit,
    onSubmit: (String, SearchFilter, EditText) -> Unit,
): SearchViews {
    val searchInput = input("搜索媒体", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
    searchInput.setText(initialTerm)
    searchInput.setSelection(searchInput.text.length)
    fun submitSearch() {
        onSubmit(searchInput.text.toString().trim(), selectedFilter, searchInput)
    }
    searchInput.imeOptions = EditorInfo.IME_ACTION_SEARCH
    searchInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
            submitSearch()
            true
        } else {
            false
        }
    }
    val root = screen("搜索媒体") {
        addView(searchInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56),
        ).apply {
            bottomMargin = dp(16)
        })
        addView(actionStrip(SearchFilter.values().map { filter ->
            choiceAction(filter.label(), filter == selectedFilter) {
                onFilter(filter, searchInput.text.toString())
            }
        }))
        addView(actionStrip(listOf(
            compactIconAction("搜索", TvIcon.SEARCH, ::submitSearch),
        )))
    }
    return SearchViews(root, searchInput)
}

data class SearchViews(
    val root: View,
    val input: EditText,
)
