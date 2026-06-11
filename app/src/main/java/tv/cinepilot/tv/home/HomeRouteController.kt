package tv.cinepilot.tv.home

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.GenreInfo
import tv.cinepilot.core.protocol.MediaBrowseFilters
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.playback.PlaybackRouteController
import tv.cinepilot.tv.runtime.ArtworkTarget

/**
 * Owns the home-screen render path (filters, library overview chips, browse paging, refresh).
 * Extracted from MainActivity so that file stays focused on top-level routing and lifecycle.
 */
class HomeRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val playbackRoutes: PlaybackRouteController,
    private val homeSettingsStore: HomeSettingsStore,
    private val mediaBrowserClient: tv.cinepilot.core.protocol.MediaBrowserClient,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showAccountSwitcher: () -> Unit,
    private val showSettings: (TvAppState) -> Unit,
    private val showSearch: (String) -> Unit,
    private val logoutFromHome: () -> Unit,
    private val loadArtwork: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    private val loadBackdrop: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    private var lastFocusedCard: View? = null

    fun render(state: TvAppState) {
        val filters = workflowController.browseFilters()
        val genres: List<GenreInfo> = workflowController.genresForCurrentView()
        val availableGenreNames = genres.map { it.displayName() }
        var focusedCard: View? = null
        activity.setContentView(activity.homeRouteScreen(
            state = state,
            canGoBack = workflowController.canGoBackInBrowse(),
            canPageBackward = workflowController.canPageBackwardInBrowse(),
            canPageForward = workflowController.canPageForwardInBrowse(),
            filters = filters,
            availableGenreNames = availableGenreNames,
            onSearch = { showSearch(state.activeSearchTerm()) },
            onRefresh = ::refreshHome,
            onSwitchAccount = showAccountSwitcher,
            onSettings = { showSettings(state) },
            onLogout = logoutFromHome,
            onBackInBrowse = ::goBackInBrowse,
            onPreviousPage = ::previousBrowsePage,
            onNextPage = ::nextBrowsePage,
            onOpen = playbackRoutes::openMediaItem,
            onFocusItem = ::handleFocusItem,
            onFiltersChanged = ::applyFilters,
            onLibraryOverview = ::openLibraryOverview,
            loadArtwork = loadArtwork,
            loadBackdrop = loadBackdrop,
            onFocusedCard = { focusedCard = it },
        ))
        lastFocusedCard = focusedCard
        focusedCard?.post { focusedCard?.requestFocus() }
    }

    private fun refreshHome() {
        runTask("正在重新加载首页...", {
            workflowController.loadHome(homeSettingsStore.load().showSmartCollections)
        }) {
            val state = workflowController.state()
            showHome(state)
            state.authenticated()?.let { authenticated ->
                tv.cinepilot.tv.home.channel.HomeChannelSyncWorker.syncNow(
                    activity.applicationContext,
                    mediaBrowserClient,
                    authenticated,
                    homeSettingsStore,
                )
            }
            tv.cinepilot.tv.home.channel.HomeChannelSyncWorker
                .scheduleImmediate(activity.applicationContext)
        }
    }

    private fun goBackInBrowse() {
        showHome(workflowController.back())
    }

    private fun previousBrowsePage() {
        runTask("正在加载上一页...", {
            workflowController.previousBrowsePage()
        }) {
            showHome(workflowController.state())
        }
    }

    private fun nextBrowsePage() {
        runTask("正在加载下一页...", {
            workflowController.nextBrowsePage()
        }) {
            showHome(workflowController.state())
        }
    }

    private fun applyFilters(nextFilters: MediaBrowseFilters) {
        runTask("正在应用筛选...", {
            workflowController.setBrowseFilters(nextFilters)
        }) {
            showHome(workflowController.state())
        }
    }

    private fun openLibraryOverview(viewId: String, title: String, isSeries: Boolean) {
        runTask(title, {
            workflowController.openLibraryOverview(viewId, title, isSeries)
        }) {
            showHome(workflowController.state())
        }
    }

    /** Pin view id when focus lands on a scoped row so genre chips are scoped to that view. */
    private fun handleFocusItem(row: HomeRow, item: MediaItemSummary) {
        pinViewIdFromRow(row)
        workflowController.focusItem(row.id(), item.id())
    }

    private fun pinViewIdFromRow(row: HomeRow) {
        val rowId = row.id()
        val viewId = when {
            rowId.startsWith("latest:") -> rowId.substringAfter("latest:", "")
            rowId.startsWith("filtered:") -> rowId.substringAfter("filtered:", "")
            else -> ""
        }
        workflowController.pinCurrentViewId(viewId)
    }
}
