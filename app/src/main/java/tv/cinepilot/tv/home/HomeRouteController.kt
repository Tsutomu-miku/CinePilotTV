package tv.cinepilot.tv.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.compose.screens.ComposeHomeNavigation
import tv.cinepilot.tv.compose.screens.ComposeHomeScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.playback.PlaybackRouteController
import tv.cinepilot.tv.runtime.ArtworkRequestFactory

/**
 * Owns the home-screen render path (library overview chips, browse paging, refresh).
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
    private val renderComposeFull: (@Composable (CinePilotPalette) -> Unit) -> Unit,
    private val showAccountSwitcher: () -> Unit,
    private val showSettings: (TvAppState) -> Unit,
    private val showSearch: (String) -> Unit,
    private val logoutFromHome: () -> Unit,
    private val artworkFactory: ArtworkRequestFactory,
) {
    fun render(state: TvAppState) {
        renderComposeFull { palette ->
            ComposeHomeScreen(
                palette = palette,
                owner = activity,
                artworkFactory = artworkFactory,
                state = state,
                navigation = ComposeHomeNavigation(
                    canGoBack = workflowController.canGoBackInBrowse(),
                    canPageBackward = workflowController.canPageBackwardInBrowse(),
                    canPageForward = workflowController.canPageForwardInBrowse(),
                    onSearch = { showSearch(state.activeSearchTerm()) },
                    onRefresh = ::refreshHome,
                    onSwitchAccount = showAccountSwitcher,
                    onSettings = { showSettings(state) },
                    onLogout = logoutFromHome,
                    onBackInBrowse = ::goBackInBrowse,
                    onPreviousPage = ::previousBrowsePage,
                    onNextPage = ::nextBrowsePage,
                ),
                onOpen = playbackRoutes::openMediaItem,
                onFocusItem = ::handleFocusItem,
                onLibraryOverview = ::openLibraryOverview,
            )
        }
    }

    private fun refreshHome() {
        runTask("正在重新加载首页...", {
            workflowController.loadHome(homeSettingsStore.load().showSmartCollections)
        }) {
            val state = workflowController.state()
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

    private fun openLibraryOverview(viewId: String, title: String, isSeries: Boolean) {
        runTask(title, {
            workflowController.openLibraryOverview(viewId, title, isSeries)
        }) {
            showHome(workflowController.state())
        }
    }

    private fun handleFocusItem(row: HomeRow, item: MediaItemSummary) {
        workflowController.focusItem(row.id(), item.id())
    }
}
