package tv.cinepilot.tv.playback

import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.plugin.toSnapshot

internal class DetailsCoordinator(
    private val workflowController: TvWorkflowController,
    private val pluginHost: PluginHost,
    private val mainExecutor: Executor,
) {
    fun loadPluginSyncStates(
        item: MediaItemSummary,
        authenticated: AuthenticatedServer?,
        onLoaded: (List<PluginHost.PluginItemSyncState>) -> Unit,
    ) {
        val snapshot = authenticated?.let { item.toSnapshot(it) } ?: return
        pluginHost.itemSyncStatusesAsync(snapshot, mainExecutor) { states ->
            val current = workflowController.state()
            if (current.route() == TvRoute.DETAILS && current.selectedItem()?.id() == item.id()) {
                onLoaded(states)
            }
        }
    }
}
