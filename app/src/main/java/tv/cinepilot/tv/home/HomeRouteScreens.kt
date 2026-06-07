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
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.ui.HomeNavigation
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.TvOptionSelectItem
import tv.cinepilot.tv.ui.compactPanelSpacing
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.homeScreen
import tv.cinepilot.tv.ui.cinematicStage
import tv.cinepilot.tv.ui.infuseActions
import tv.cinepilot.tv.ui.infusePanelTitle
import tv.cinepilot.tv.ui.input
import tv.cinepilot.tv.ui.optionSelect
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.sideSheet

fun ComponentActivity.homeRouteScreen(
    state: TvAppState,
    canGoBack: Boolean,
    canPageBackward: Boolean,
    canPageForward: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onBackInBrowse: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpen: (HomeRow, MediaItemSummary) -> Unit,
    onFocusItem: (HomeRow, MediaItemSummary) -> Unit,
    loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    onFocusedCard: (View) -> Unit,
) = homeScreen(
    state = state,
    navigation = HomeNavigation(
        canGoBack = canGoBack,
        canPageBackward = canPageBackward,
        canPageForward = canPageForward,
        onSearch = onSearch,
        onRefresh = onRefresh,
        onSwitchAccount = onSwitchAccount,
        onSettings = onSettings,
        onLogout = onLogout,
        onBackInBrowse = onBackInBrowse,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
    ),
    onOpen = onOpen,
    onFocusItem = onFocusItem,
    loadArtwork = loadArtwork,
    loadBackdrop = loadBackdrop,
    onFocusedCard = onFocusedCard,
)

fun ComponentActivity.searchScreen(
    initialTerm: String,
    selectedFilter: SearchFilter,
    onFilter: (SearchFilter, String) -> Unit,
    onVoiceInput: (String) -> Unit,
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
    val root = cinematicStage(scrollable = false) {
        addView(sideSheet {
            addView(infusePanelTitle("搜索"))
            addView(searchInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52),
            ).apply {
                bottomMargin = dp(10)
            })
            addView(optionSelect(
                title = "范围",
                selectedLabel = selectedFilter.label(),
                options = SearchFilter.values().map { filter ->
                    TvOptionSelectItem(filter.label(), filter == selectedFilter) {
                        onFilter(filter, searchInput.text.toString())
                    }
                },
            ).compactPanelSpacing(10))
            addView(infuseActions(listOf(
                InfuseAction("搜索", TvIcon.SEARCH, InfuseActionEmphasis.PRIMARY, ::submitSearch),
                InfuseAction("语音", TvIcon.MIC, InfuseActionEmphasis.QUIET) {
                    onVoiceInput(searchInput.text.toString())
                },
            )))
        })
    }
    searchInput.requestInitialFocus()
    return SearchViews(root, searchInput)
}

data class SearchViews(
    val root: View,
    val input: EditText,
)
