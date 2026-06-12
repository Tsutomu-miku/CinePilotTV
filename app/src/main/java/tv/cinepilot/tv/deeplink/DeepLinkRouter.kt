package tv.cinepilot.tv.deeplink

import android.content.Intent
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController

/**
 * Handles launcher deep links (e.g. from Android TV preview programs).
 *
 * Supported URIs:
 *   - `cinepilot://play?item=<item-id>` — open an item's detail page
 *   - `cinepilot://home`                — return to home (no-op at launch)
 */
class DeepLinkRouter(
    private val workflowController: TvWorkflowController,
    private val runTask: (message: String, task: () -> Unit, onSuccess: () -> Unit) -> Unit,
    private val showDetails: (MediaItemSummary) -> Unit,
    private val showHome: (TvAppState) -> Unit,
) {
    private var pendingUri: android.net.Uri? = null

    fun handleIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != SCHEME) return
        if (workflowController.state().authenticated() == null) {
            // Session isn't ready yet (e.g. cold start from a launcher preview
            // program). Queue the URI and replay it once account restore finishes.
            pendingUri = uri
            return
        }
        dispatch(uri)
    }

    /** Replay a queued deep link if one exists and we are now authenticated. */
    fun replayPending() {
        val uri = pendingUri ?: return
        if (workflowController.state().authenticated() == null) return
        pendingUri = null
        dispatch(uri)
    }

    private fun dispatch(uri: android.net.Uri) {
        when (uri.host) {
            HOST_PLAY -> handlePlay(uri.getQueryParameter(PARAM_ITEM))
            HOST_HOME -> showHome(workflowController.state())
        }
    }

    private fun handlePlay(itemId: String?) {
        if (itemId.isNullOrBlank()) return
        runTask("正在打开...", {
            workflowController.openItem(itemId)
        }) {
            val item = workflowController.state().selectedItem()
            if (item != null) {
                showDetails(item)
            } else {
                showHome(workflowController.state())
            }
        }
    }

    private companion object {
        private const val SCHEME = "cinepilot"
        private const val HOST_PLAY = "play"
        private const val HOST_HOME = "home"
        private const val PARAM_ITEM = "item"
    }
}
