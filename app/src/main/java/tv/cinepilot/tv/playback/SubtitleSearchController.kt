package tv.cinepilot.tv.playback

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.plugin.spi.SubtitleSearchResult
import tv.cinepilot.tv.compose.screens.ComposeSubtitleSearchScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.plugin.toSnapshot
import tv.cinepilot.tv.subtitle.SubtitleCache

/**
 * Coordinates the subtitle search & download flow: detail page entry point,
 * search side sheet, download, cache, and selection.
 *
 * ### Sheet lifecycle
 *
 * Each call to [showSearchSheet] creates a [SheetState] that lives for the
 * duration of the sheet being open. Both the search callback and the
 * in-line download callbacks mutate that same state object, which means:
 *
 * 1. After a successful download we can re-render **without** re-running the
 *    network search (preserving list, scroll, focus).
 * 2. In-flight downloads are reflected per-row (loading / error) so the UI
 *    can show row-level feedback instead of a global busy overlay.
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
    private val runSilentTask: (() -> Unit, () -> Unit, (Throwable) -> Unit) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
    private val setAuxiliaryBackAction: (() -> Unit) -> Unit,
) {

    /** Subtitle result selected by the user, scoped by item id. */
    private var selectedResult: SubtitleSearchResult? = null
    private var selectedResultItemId: String? = null

    /** Currently-open sheet state; `null` when no sheet is displayed. */
    private var sheetState: SheetState? = null

    fun hasSubtitleSearch(): Boolean = pluginHost.hasSubtitleSearchPlugins()

    fun selectedSubtitleFor(item: MediaItemSummary): SubtitleSearchResult? =
        selectedResult?.takeIf { selectedResultItemId == item.id() }

    /** Holds per-sheet mutable state shared between search + download flows. */
    private class SheetState(
        val session: Long,
        val item: MediaItemSummary,
        val keyword: String,
        val episodeTag: String,
        @Volatile var results: List<SubtitleSearchResult> = emptyList(),
        @Volatile var searchError: Throwable? = null,
        /** Which result id is currently downloading, or null if none. */
        @Volatile var downloadingId: String? = null,
        /** Result id → last download error, or null if no recent failure for that row. */
        val rowErrors: MutableMap<String, Throwable> = mutableMapOf(),
    ) {
        /** Compound key used for `rowErrors` map. SPI id is unique enough here. */
        fun rowKey(result: SubtitleSearchResult): String = result.id()
    }

    /** Show the subtitle search side sheet for the given item. */
    fun showSearchSheet(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        onClose: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        val snapshot = item.toSnapshot(auth)
        val (keyword, episodeTag) = buildKeyword(item)
        val state = SheetState(
            session = System.nanoTime(),
            item = item,
            keyword = keyword,
            episodeTag = episodeTag,
        )
        sheetState = state

        fun rerender(loading: Boolean) {
            // If state has been superseded (user closed and reopened), drop.
            if (sheetState !== state) return
            setAuxiliaryBackAction(onClose)
            val sorted = sortResults(state.results)
            val selId = selectedResult
                ?.takeIf { selectedResultItemId == item.id() }
                ?.id()
            renderCompose("搜索在线字幕") { palette ->
                ComposeSubtitleSearchScreen(
                    palette = palette,
                    itemName = item.name(),
                    itemYear = if (item.productionYear() > 0) item.productionYear().toString() else "",
                    itemEpisodeTag = episodeTag,
                    searchKeyword = state.keyword,
                    results = sorted,
                    isLoading = loading,
                    error = state.searchError,
                    downloadingId = state.downloadingId,
                    rowErrorById = state.rowErrors,
                    selectedId = selId,
                    onPick = { result ->
                        downloadAndSelect(state, result, onClose)
                    },
                    onRetryRow = { result ->
                        state.rowErrors.remove(state.rowKey(result))
                        rerender(loading = false)
                        downloadAndSelect(state, result, onClose)
                    },
                    onRetry = {
                        // Retry from scratch (re-enters showSearchSheet).
                        showSearchSheet(item, playbackInfo, episodeContext, onClose)
                    },
                    onClose = {
                        if (sheetState === state) sheetState = null
                        onClose()
                    },
                )
            }
        }

        rerender(loading = true)
        runSilentTask(
            {
                state.results = pluginHost.searchSubtitles(snapshot, language = "")
            },
            { rerender(loading = false) },
            { err ->
                state.searchError = err
                rerender(loading = false)
            },
        )
    }

    /**
     * Download (or pull from cache) a subtitle and mark it selected.
     *
     * Uses [runSilentTask] rather than [runTask] so we drive UI feedback
     * through the row-level `downloadingId` / `rowErrors` state instead of
     * a global busy indicator — that keeps the search sheet visible and
     * avoids the global BusyOverlay fighting with the right-side sheet.
     */
    private fun downloadAndSelect(
        state: SheetState,
        result: SubtitleSearchResult,
        onClose: () -> Unit,
    ) {
        val rowKey = state.rowKey(result)
        state.rowErrors.remove(rowKey)
        state.downloadingId = rowKey
        fun rerender() {
            // Same session check; if user closed the sheet we're done.
            if (sheetState !== state) return
            // Trigger same-session rerender with current state, loading=false.
            // Reach into showSearchSheet's render path by re-running the
            // render closure we created there. Easiest: reuse showSearchSheet's
            // render by simulating same-session rerender via reflection-free
            // approach: manually rebuild the render via SheetState-aware
            // private render function defined below.
            renderSheet(state, onClose)
        }

        val auth = workflowController.state().authenticated()
        if (auth == null) {
            state.downloadingId = null
            state.rowErrors[rowKey] = IllegalStateException("未登录")
            rerender()
            return
        }
        val serverId = auth.server().serverId()

        // Cache hit → no task needed.
        val cached = subtitleCache.get(serverId, state.item.id(), result)
        if (cached != null) {
            state.downloadingId = null
            selectedResult = result
            selectedResultItemId = state.item.id()
            Toast.makeText(
                activity,
                "已选择字幕：${result.name()}",
                Toast.LENGTH_SHORT,
            ).show()
            rerender()
            return
        }

        runSilentTask(
            {
                val bytes = pluginHost.downloadSubtitle(result)
                    ?: throw IllegalStateException("字幕下载失败")
                val fresh = subtitleCache.put(serverId, state.item.id(), result, bytes)
                if (!fresh.file.exists() || fresh.file.length() == 0L) {
                    throw IllegalStateException("字幕文件无效")
                }
            },
            {
                state.downloadingId = null
                selectedResult = result
                selectedResultItemId = state.item.id()
                Toast.makeText(
                    activity,
                    "已选择字幕：${result.name()}",
                    Toast.LENGTH_SHORT,
                ).show()
                rerender()
            },
            { err ->
                state.downloadingId = null
                state.rowErrors[rowKey] = err
                rerender()
            },
        )
    }

    /**
     * Renders the [state] to the side sheet without restarting the search.
     * Invoked after in-row download state transitions. Mirrors the render
     * closure defined inside [showSearchSheet]; keep the two in sync!
     */
    private fun renderSheet(state: SheetState, onClose: () -> Unit) {
        if (sheetState !== state) return
        val sorted = sortResults(state.results)
        val selId = selectedResult
            ?.takeIf { selectedResultItemId == state.item.id() }
            ?.id()
        setAuxiliaryBackAction(onClose)
        renderCompose("搜索在线字幕") { palette ->
            ComposeSubtitleSearchScreen(
                palette = palette,
                itemName = state.item.name(),
                itemYear = if (state.item.productionYear() > 0) state.item.productionYear().toString() else "",
                itemEpisodeTag = state.episodeTag,
                searchKeyword = state.keyword,
                results = sorted,
                isLoading = false,
                error = state.searchError,
                downloadingId = state.downloadingId,
                rowErrorById = state.rowErrors,
                selectedId = selId,
                onPick = { result -> downloadAndSelect(state, result, onClose) },
                onRetryRow = { result ->
                    state.rowErrors.remove(state.rowKey(result))
                    renderSheet(state, onClose)
                    downloadAndSelect(state, result, onClose)
                },
                onRetry = {
                    // Full retry → re-open fresh sheet.
                    showSearchSheet(
                        item = state.item,
                        playbackInfo = null,
                        episodeContext = null,
                        onClose = onClose,
                    )
                },
                onClose = {
                    if (sheetState === state) sheetState = null
                    onClose()
                },
            )
        }
    }

    // --- keyword helpers -------------------------------------------------

    private fun buildKeyword(item: MediaItemSummary): Pair<String, String> {
        val sb = StringBuilder(item.name())
        var tag = ""
        if (item.productionYear() > 0) sb.append(' ').append(item.productionYear())
        if (item.parentIndexNumber() > 0) {
            val s = String.format("S%02d", item.parentIndexNumber())
            sb.append(' ').append(s)
            tag += s
        }
        if (item.indexNumber() > 0) {
            val e = String.format("E%02d", item.indexNumber())
            sb.append(e)
            tag += e
        }
        return sb.toString().trim() to tag
    }

    // --- external subtitles for the player --------------------------------

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
            // Mark the label so the player settings page can distinguish
            // online subtitles from server-provided ones even if the rest of
            // CinePilot does not expose provider metadata there.
            label = "[在线] ${result.name()}",
        )
        return listOf(extSub)
    }

    private fun subtitleMimeTypeFor(format: SubtitleSearchResult.Format): String {
        return when (format) {
            SubtitleSearchResult.Format.SRT ->
                androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
            SubtitleSearchResult.Format.ASS,
            SubtitleSearchResult.Format.SSA ->
                androidx.media3.common.MimeTypes.TEXT_SSA
            SubtitleSearchResult.Format.VTT ->
                androidx.media3.common.MimeTypes.TEXT_VTT
            SubtitleSearchResult.Format.PGS -> "application/x-pgs"
            SubtitleSearchResult.Format.UNKNOWN ->
                androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
        }
    }

    // --- result ordering --------------------------------------------------

    /**
     * Sort search results before rendering: Chinese-bilingual first, then
     * by language group, then by download count (descending), then by name.
     */
    private fun sortResults(results: List<SubtitleSearchResult>): List<SubtitleSearchResult> {
        return results.sortedWith(
            compareByDescending<SubtitleSearchResult> { languageRank(it.language()) }
                .thenByDescending { it.downloadCount() }
                .thenBy { it.name() }
        )
    }

    private fun languageRank(language: String): Int {
        if (language.isEmpty()) return 0
        if (language.contains("简英") || language.contains("简繁英")) return 50
        if (language.contains("繁英")) return 45
        if (language.contains("简繁") && !language.contains("英")) return 40
        if (language.contains("简体") || language.contains("简中")) return 35
        if (language.contains("繁体") || language.contains("繁中")) return 30
        if (language.contains("中文") || language.contains("zh") || language.contains("Chn")) return 25
        if (language.contains("双语")) return 20
        if (language.contains("英语") || language.contains("英文") || language.contains("eng")) return 10
        return 5
    }
}
