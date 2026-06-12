package tv.cinepilot.tv.runtime

import android.app.Activity
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.tv.FileHomeRowsCache
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.home.channel.HomeChannelSyncWorker
import tv.cinepilot.tv.offline.OfflineHomeRow

/**
 * Orchestrates the cache-aware home-entry flow used by every authentication
 * path (restore-session on launch, continue-account from server entry,
 * password login, Quick Connect approval, QA login).
 *
 * Sequence for a successful entry:
 *   1. Execute [preload] on the background executor (server handshake,
 *      session restore, credential validation, …). Must throw on failure.
 *   2. If a cached [tv.cinepilot.core.tv.HomeRow] list exists for the
 *      current (serverId, userId) scope, restore it to the workflow and
 *      immediately paint the home UI on the main thread. The TV is now
 *      interactive *before* any network round-trip.
 *   3. Run a full [TvWorkflowController.loadHome] network refresh on the
 *      background thread; persist the result back to disk so the next
 *      cold-start benefits from the newest rows.
 *   4. Invoke [remember] on the main thread (used to persist recently-used
 *      account lists after a credentialed login), then repaint home.
 *
 * If [showLoadingMessage] is non-null a loading overlay is displayed for
 * the duration. When null and a cache hit paints the wall in step (2), no
 * loading overlay is shown at all — the network refresh runs silently in
 * the background. If there is no cache hit and [showLoadingMessage] is
 * null, a generic loading overlay is substituted so users never stare at
 * a blank screen.
 */
class HomeEntryFlow(
    private val activity: Activity,
    private val executor: Executor,
    private val workflowController: TvWorkflowController,
    private val homeRowsCache: FileHomeRowsCache,
    private val homeSettingsStore: HomeSettingsStore,
    private val mediaBrowserClient: tv.cinepilot.core.protocol.MediaBrowserClient,
    private val offlineRepository: OfflineRepository,
    private val showLoading: (String) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showError: (Throwable) -> Unit,
) {
    fun run(
        showLoadingMessage: String?,
        preload: () -> Unit,
        remember: () -> Unit = {},
        fallback: (Throwable) -> Unit = showError,
    ) {
        if (showLoadingMessage != null) showLoading(showLoadingMessage)
        executor.execute {
            try {
                preload()
                val authenticated: AuthenticatedServer = workflowController.state().authenticated()
                    ?: error("Authenticated server is missing")
                val serverId = authenticated.server().serverId()
                val userId = authenticated.session().userId()
                val cached = homeRowsCache.load(serverId, userId).orElse(null)
                val cacheHit = if (cached != null) {
                    workflowController.restoreHomeFromCache(withOfflineRow(authenticated, cached))
                } else {
                    false
                }
                if (cacheHit) {
                    activity.runOnUiThread {
                        // Dismiss any loading overlay because the cached wall
                        // is already interactive; the network refresh keeps
                        // running in the background.
                        showHome(workflowController.state())
                    }
                } else if (showLoadingMessage == null) {
                    // No cache hit and the caller wanted a silent restore:
                    // fall back to the loading overlay so the user knows work
                    // is still happening.
                    activity.runOnUiThread { showLoading("正在加载首页...") }
                }
                // Refresh from the network. When there's a cache hit, refresh
                // failures are best-effort — we keep the cached UI visible
                // instead of yanking the user to an error screen during the
                // exact offline / slow-server scenario the cache is for.
                //
                // Capture the current route before refreshing so we can tell
                // whether the user has navigated away from home (e.g. opened
                // details or playback) while the cached wall was displayed.
                // loadHome() internally resets the route to HOME via
                // TvWorkflow.homeLoaded, so a post-refresh route check would
                // always pass — we must snapshot the route beforehand.
                val routeBeforeRefresh = workflowController.state().route()
                val refreshIsBackground = routeBeforeRefresh != TvRoute.HOME
                val includeSmartCollections = homeSettingsStore.load().showSmartCollections
                val refreshSuccess = runCatching {
                    workflowController.loadHome(includeSmartCollections, refreshIsBackground)
                    val finalRows = withOfflineRow(
                        authenticated,
                        workflowController.state().homeRows(),
                    )
                    val finalState = workflowController.setHomeRows(finalRows, refreshIsBackground)
                    homeRowsCache.save(serverId, userId, finalState.homeRows())
                    // Best-effort sync to Android TV preview channels.
                    runCatching {
                        HomeChannelSyncWorker.syncNow(
                            activity.applicationContext,
                            mediaBrowserClient,
                            authenticated,
                            homeSettingsStore,
                        )
                        HomeChannelSyncWorker.scheduleImmediate(activity.applicationContext)
                    }
                    finalState
                }
                if (refreshSuccess.isSuccess) {
                    activity.runOnUiThread {
                        remember()
                        // Only repaint the home UI if the user was still on
                        // the home wall when the refresh started; otherwise
                        // leave them wherever they navigated to.
                        if (!refreshIsBackground) {
                            showHome(refreshSuccess.getOrThrow())
                        }
                    }
                } else if (!cacheHit) {
                    // No cache to fall back on — propagate the error.
                    throw refreshSuccess.exceptionOrNull()!!
                }
            } catch (error: Throwable) {
                activity.runOnUiThread { fallback(error) }
            }
        }
    }

    /** Save the currently-loaded home rows to disk. Safe on any thread. */
    fun persistCurrentHome() {
        executor.execute {
            val authenticated: AuthenticatedServer = workflowController.state().authenticated()
                ?: return@execute
            val rows = workflowController.state().homeRows()
            if (rows == null || rows.isEmpty()) return@execute
            homeRowsCache.save(
                authenticated.server().serverId(),
                authenticated.session().userId(),
                rows,
            )
        }
    }

    /** Remove cached rows for the currently-authenticated scope. */
    fun clearCurrentHomeCache() {
        executor.execute {
            val authenticated: AuthenticatedServer = workflowController.state().authenticated()
                ?: return@execute
            homeRowsCache.clear(
                authenticated.server().serverId(),
                authenticated.session().userId(),
            )
        }
    }

    private fun withOfflineRow(
        authenticated: AuthenticatedServer,
        rows: List<HomeRow>,
    ): List<HomeRow> {
        val offlineRow = OfflineHomeRow.build(
            authenticated,
            mediaBrowserClient,
            offlineRepository,
        ) ?: return rows
        if (rows.any { it.id() == offlineRow.id() }) return rows
        return buildList {
            add(offlineRow)
            addAll(rows)
        }
    }
}
