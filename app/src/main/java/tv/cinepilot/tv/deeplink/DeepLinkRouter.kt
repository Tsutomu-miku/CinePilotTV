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
    fun handleIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != SCHEME) return
        when (uri.host) {
            HOST_PLAY -> handlePlay(uri.getQueryParameter(PARAM_ITEM))
            HOST_HOME -> { /* No-op: default launch goes to home anyway. */ }
        }
    }

    private fun handlePlay(itemId: String?) {
        if (itemId.isNullOrBlank()) return
        // Only handle the deep link if we already have an authenticated session.
        // Otherwise the normal login / home flow will run and the user can
        // navigate to the item manually.
        if (workflowController.state().authenticated() == null) return
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
