package tv.cinepilot.tv.playback

import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.plugin.toSnapshot
import tv.cinepilot.tv.subtitle.SubtitleCache
import tv.cinepilot.tv.ui.subtitleSearchSheet

/**
 * Coordinates the subtitle search & download flow: detail page entry point,
 * search side sheet, download, cache, and selection.
 *
 * Decoupled from the main playback controller so the search-result UI and
 * subtitle caching logic can evolve independently from player lifecycle.
 */
class SubtitleSearchController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val pluginHost: PluginHost,
    private val subtitleCache: SubtitleCache,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val renderView: (View) -> Unit,
    private val setAuxiliaryBackAction: (() -> Unit) -> Unit,
) {

    /** Subtitle result selected by the user, scoped by item id. */
    private var selectedResult: tv.cinepilot.plugin.spi.SubtitleSearchResult? = null
    private var selectedResultItemId: String? = null

    fun hasSubtitleSearch(): Boolean = pluginHost.hasSubtitleSearchPlugins()

    fun selectedSubtitleFor(item: MediaItemSummary): tv.cinepilot.plugin.spi.SubtitleSearchResult? =
        selectedResult?.takeIf { selectedResultItemId == item.id() }

    /** Show the subtitle search side sheet for the given item. */
    fun showSearchSheet(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        var results: List<tv.cinepilot.plugin.spi.SubtitleSearchResult> = emptyList()
        val snapshot = item.toSnapshot(auth)

        fun rerender(loading: Boolean) {
            val sheet = activity.subtitleSearchSheet(
                itemName = item.name(),
                results = results,
                isLoading = loading,
                selectedId = selectedResult?.takeIf { selectedResultItemId == item.id() }?.id(),
                onPick = { result ->
                    downloadAndSelect(item, result, playbackInfo, episodeContext, onClose)
                },
                onClose = onClose,
            )
            setAuxiliaryBackAction(onClose)
            renderView(sheet)
        }

        rerender(true)
        runTask("正在搜索字幕...", {
            results = pluginHost.searchSubtitles(snapshot)
        }) {
            rerender(false)
        }
    }

    /** Download a subtitle (using cache if available) and mark it selected. */
    private fun downloadAndSelect(
        item: MediaItemSummary,
        result: tv.cinepilot.plugin.spi.SubtitleSearchResult,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        val serverId = auth.server().serverId()
        runTask("正在下载字幕...", {
            val cached = subtitleCache.get(serverId, item.id(), result)
                ?: run {
                    val bytes = pluginHost.downloadSubtitle(result)
                        ?: throw IllegalStateException("字幕下载失败")
                    subtitleCache.put(serverId, item.id(), result, bytes)
                }
            if (!cached.file.exists() || cached.file.length() == 0L) {
                throw IllegalStateException("字幕文件无效")
            }
        }) {
            selectedResult = result
            selectedResultItemId = item.id()
            Toast.makeText(activity, "已选择字幕：${result.name()}", Toast.LENGTH_SHORT).show()
            showSearchSheet(item, playbackInfo, episodeContext, onClose)
        }
    }

    /** Build Media3 ExternalSubtitle list for the currently selected subtitle. */
    fun buildExternalSubtitles(item: MediaItemSummary): List<Media3PlayerHost.ExternalSubtitle> {
        val auth = workflowController.state().authenticated() ?: return emptyList()
        val result = selectedResult?.takeIf { selectedResultItemId == item.id() }
            ?: return emptyList()
        val cached = subtitleCache.get(auth.server().serverId(), item.id(), result)
            ?: return emptyList()
        val extSub = Media3PlayerHost.ExternalSubtitle(
            file = cached.file,
            mimeType = subtitleMimeTypeFor(result.format()),
            language = result.language(),
            label = result.name(),
        )
        return listOf(extSub)
    }

    private fun subtitleMimeTypeFor(format: tv.cinepilot.plugin.spi.SubtitleSearchResult.Format): String {
        return when (format) {
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.SRT ->
                androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.ASS,
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.SSA ->
                androidx.media3.common.MimeTypes.TEXT_SSA
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.VTT ->
                androidx.media3.common.MimeTypes.TEXT_VTT
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.PGS -> "application/pgs"
            tv.cinepilot.plugin.spi.SubtitleSearchResult.Format.UNKNOWN ->
                androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
        }
    }
}
