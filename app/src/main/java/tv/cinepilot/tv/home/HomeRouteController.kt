package tv.cinepilot.tv.home

import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.GenreInfo
import tv.cinepilot.core.protocol.MediaBrowseFilters
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
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
    private val executor: Executor,
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
    private var cachedGenreNames: List<String> = emptyList()

    /**
     * Reloads the genre list for the current view on a background thread.
     * `genresForCurrentView()` hits the network and must not be called on the UI thread.
     * Re-renders the home screen when the list changes, so chips don't stay stale
     * after a cold load or when switching to a different view scope.
     */
    fun refreshGenres() {
        executor.execute {
            val genres: List<GenreInfo> = runCatching { workflowController.genresForCurrentView() }
                .getOrDefault(emptyList())
            val names = genres.map { it.displayName() }
            activity.runOnUiThread {
                if (cachedGenreNames == names) return@runOnUiThread
                cachedGenreNames = names
                if (workflowController.state().route() == TvRoute.HOME) {
                    render(workflowController.state())
                }
            }
        }
    }

    fun render(state: TvAppState) {
        val filters = workflowController.browseFilters()
        var focusedCard: View? = null
        activity.setContentView(activity.homeRouteScreen(
            state = state,
            canGoBack = workflowController.canGoBackInBrowse(),
            canPageBackward = workflowController.canPageBackwardInBrowse(),
            canPageForward = workflowController.canPageForwardInBrowse(),
            filters = filters,
            availableGenreNames = cachedGenreNames,
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
            refreshGenres()
            showHome(state)
            state.authenticated()?.let { authenticated ->
                // Channel sync hits the network and writes to the TV provider
                // synchronously — run it on the background executor so it
                // doesn't freeze the home UI after refresh completes.
                executor.execute {
                    tv.cinepilot.tv.home.channel.HomeChannelSyncWorker.syncNow(
                        activity.applicationContext,
                        mediaBrowserClient,
                        authenticated,
                        homeSettingsStore,
                    )
                }
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
            workflowController.setBrowseFilters(
                nextFilters,
                homeSettingsStore.load().showSmartCollections,
            )
        }) {
            refreshGenres()
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
        refreshGenres()
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
