package tv.cinepilot.tv.offline

import tv.cinepilot.core.protocol.AuthenticatedServer
import tv.cinepilot.core.protocol.MediaBrowserClient
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.protocol.UserItemData
import tv.cinepilot.core.tv.HomeRow

/**
 * Builds the "离线下载" [HomeRow] shown at the top of the home screen when
 * any content on the active server has been fully downloaded. Items whose
 * server metadata cannot be loaded (e.g. offline session) fall back to a
 * minimal [MediaItemSummary] containing only the id and name already stored
 * in the download queue entry.
 */
internal object OfflineHomeRow {

    private const val OFFLINE_ROW_ID = "cinepilot:offline"
    const val OFFLINE_ROW_TITLE = "离线下载"

    fun build(
        authenticated: AuthenticatedServer,
        client: MediaBrowserClient,
        offline: OfflineRepository,
    ): HomeRow? {
        val serverId = authenticated.server().serverId()
        val ready = offline.readyForServer(serverId)
        if (ready.isEmpty()) return null
        val summaries = ready.mapNotNull { entry ->
            runCatching { client.item(authenticated, entry.itemId()) }
                .getOrNull()
                ?: synthesizedSummary(entry)
        }
        if (summaries.isEmpty()) return null
        return HomeRow(OFFLINE_ROW_ID, OFFLINE_ROW_TITLE, summaries)
    }

    /**
     * Low-fidelity fallback used when the client call fails (most likely
     * because we have no network). The renderer tolerates empty poster URLs
     * by drawing the item name on top of a solid-color placeholder card.
     */
    private fun synthesizedSummary(entry: OfflineRepository.Entry): MediaItemSummary? {
        if (entry.itemId().isBlank() || entry.itemName().isBlank()) return null
        return MediaItemSummary(
            entry.itemId(),
            "",
            entry.itemName(),
            MediaItemType.VIDEO,
            false,
            true,
            null,
            null,
            null,
            null,
            "",
            "",
            "",
            emptyList(),
            "",
            null,
            "",
            emptyList(),
            emptyList(),
            UserItemData.empty(),
            emptyMap(),
            emptyList(),
            emptyMap(),
            emptyList(),
        )
    }
}

