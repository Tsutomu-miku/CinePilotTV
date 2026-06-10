package tv.cinepilot.tv.runtime

import android.app.Activity
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.tv.FileHomeRowsCache
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController

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
                    workflowController.restoreHomeFromCache(cached)
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
                workflowController.loadHome()
                homeRowsCache.save(
                    serverId,
                    userId,
                    workflowController.state().homeRows(),
                )
                activity.runOnUiThread {
                    remember()
                    showHome(workflowController.state())
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
}
