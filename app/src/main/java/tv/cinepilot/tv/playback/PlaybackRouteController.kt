package tv.cinepilot.tv.playback

import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.details.detailsRouteScreen
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.ui.playerScreen

class PlaybackRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val playerHost: Media3PlayerHost,
    private val subtitleStyleStore: SubtitleStyleStore,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
) {
    private var selectedPlaybackInfo: PlaybackInfo? = null
    private var lastPlaybackBackPressAt = 0L
    private var auxiliaryBackAction: (() -> Unit)? = null

    fun showDetails(item: MediaItemSummary, playbackInfo: PlaybackInfo? = null) {
        auxiliaryBackAction = null
        if (playbackInfo != null && playbackInfo.itemId() == item.id()) {
            selectedPlaybackInfo = playbackInfo
        }
        val effectivePlaybackInfo = playbackInfo ?: selectedPlaybackInfo?.takeIf { it.itemId() == item.id() }
        activity.setContentView(activity.detailsRouteScreen(
            item = item,
            playbackInfo = effectivePlaybackInfo,
            loadPosterImage = loadPosterImage,
            onPreparePlayback = ::preparePlaybackWith,
            onPlaybackOptions = { loadPlaybackOptions(item) },
            onSubtitleStyle = { showSubtitleStyleOptions(item) },
            onPlaybackSpeed = { showPlaybackSpeedOptions(item) },
            onSeriesNextUp = ::openSeriesNextUp,
            onOpenFolder = {
                runTask("正在打开目录...", {
                    workflowController.openFolder(item.id(), item.name())
                }) {
                    showHome(workflowController.state())
                }
            },
        ))
    }

    fun handleAuxiliaryBackPressed(): Boolean {
        val backAction = auxiliaryBackAction ?: return false
        backAction()
        return true
    }

    fun openMediaItem(row: HomeRow, item: MediaItemSummary) {
        workflowController.focusItem(row.id(), item.id())
        val loadingMessage = if (item.playable()) "正在打开详情..." else "正在打开目录..."
        var playbackInfo: PlaybackInfo? = null
        runTask(loadingMessage, {
            if (item.playable()) {
                workflowController.openItem(item.id())
                playbackInfo = runCatching {
                    workflowController.loadPlaybackChoices(null)
                }.getOrNull()
            } else {
                workflowController.openFolder(item.id(), item.name())
            }
        }) {
            val state = workflowController.state()
            if (item.playable()) {
                state.selectedItem()?.let { selectedItem -> showDetails(selectedItem, playbackInfo) }
            } else {
                showHome(state)
            }
        }
    }

    fun handlePlaybackBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackBackPressAt > PLAYBACK_BACK_EXIT_WINDOW_MS) {
            lastPlaybackBackPressAt = now
            Toast.makeText(activity, "再次按返回退出播放", Toast.LENGTH_SHORT).show()
            return
        }
        exitPlaybackToDetails()
    }

    private fun exitPlaybackToDetails() {
        lastPlaybackBackPressAt = 0L
        playerHost.release()
        workflowController.back()
        workflowController.state().selectedItem()?.let(::showDetails)
            ?: showHome(workflowController.state())
    }

    private fun loadPlaybackOptions(item: MediaItemSummary) {
        var choices: PlaybackInfo? = null
        runTask("正在读取音轨和字幕...", {
            choices = workflowController.loadPlaybackChoices(null)
        }) {
            choices?.let { showPlaybackOptions(item, it) }
        }
    }

    private fun showPlaybackOptions(item: MediaItemSummary, playbackInfo: PlaybackInfo) {
        auxiliaryBackAction = { showDetails(item) }
        if (playbackInfo.itemId() == item.id()) {
            selectedPlaybackInfo = playbackInfo
        }
        activity.setContentView(activity.playbackOptionsScreen(
            item = item,
            playbackInfo = playbackInfo,
            onDefault = { preparePlaybackWith(null) },
            onSource = { sourceId -> preparePlaybackWith(sourcePreferences(item, sourceId)) },
            onAudio = { sourceId, audioStreamIndex ->
                showSubtitleOptionsForAudio(item, playbackInfo, sourceId, audioStreamIndex)
            },
            onDisableSubtitles = { sourceId ->
                preparePlaybackWith(trackPreferences(item, sourceId, null, -1))
            },
            onSubtitle = { sourceId, subtitleStreamIndex ->
                preparePlaybackWith(trackPreferences(item, sourceId, null, subtitleStreamIndex))
            },
        ))
    }

    private fun showSubtitleOptionsForAudio(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo,
        sourceId: String,
        audioStreamIndex: Int,
    ) {
        val source = playbackInfo.mediaSources().firstOrNull { it.id() == sourceId }
        if (source == null) {
            preparePlaybackWith(trackPreferences(item, sourceId, audioStreamIndex, null))
            return
        }
        auxiliaryBackAction = { showPlaybackOptions(item, playbackInfo) }
        activity.setContentView(activity.subtitleOptionsForAudioScreen(
            source = source,
            audioStreamIndex = audioStreamIndex,
            onDefaultSubtitles = {
                preparePlaybackWith(trackPreferences(item, sourceId, audioStreamIndex, null))
            },
            onDisableSubtitles = {
                preparePlaybackWith(trackPreferences(item, sourceId, audioStreamIndex, -1))
            },
            onSubtitle = { subtitleStreamIndex ->
                preparePlaybackWith(trackPreferences(item, sourceId, audioStreamIndex, subtitleStreamIndex))
            },
        ))
    }

    private fun showPlaybackSpeedOptions(item: MediaItemSummary) {
        auxiliaryBackAction = { showDetails(item) }
        activity.setContentView(activity.playbackSpeedScreen(
            onSpeed = { rate -> preparePlaybackWith(speedPreferences(item, rate)) },
        ))
    }

    private fun showSubtitleStyleOptions(item: MediaItemSummary) {
        auxiliaryBackAction = { showDetails(item) }
        activity.showSubtitleStyleScreen(subtitleStyleStore)
    }

    fun showDiagnosticsFromError(state: TvAppState, onBackError: () -> Unit) {
        showDiagnostics(
            state = state,
            backLabel = "返回错误页",
            onBackDiagnosticsTarget = onBackError,
        )
    }

    fun retryLowBitrateFromError(state: TvAppState) {
        state.selectedItem()?.let { item ->
            preparePlaybackWith(lowBitratePreferences(item))
        }
    }

    fun showPlaybackOptionsFromError(state: TvAppState) {
        state.selectedItem()?.let(::loadPlaybackOptions)
    }

    private fun showDiagnostics(
        state: TvAppState,
        returnToPlayer: Boolean = false,
        backLabel: String? = null,
        onBackDiagnosticsTarget: () -> Unit,
    ) {
        val diagnostics = TvDiagnostics.describe(state)
        activity.setContentView(activity.diagnosticsScreen(
            diagnostics = diagnostics,
            returnToPlayer = returnToPlayer,
            backLabel = backLabel,
            onExport = {
                val file = activity.filesDir.resolve("cinepilot-diagnostics.txt")
                file.writeText(diagnostics)
                showDiagnosticsExported(state, file.absolutePath, returnToPlayer, backLabel, onBackDiagnosticsTarget)
            },
            onShare = { shareDiagnostics(diagnostics) },
            onBackDiagnosticsTarget = onBackDiagnosticsTarget,
        ))
    }

    private fun showDiagnosticsExported(
        state: TvAppState,
        path: String,
        returnToPlayer: Boolean = false,
        backLabel: String? = null,
        onBackDiagnosticsTarget: () -> Unit,
    ) {
        val diagnostics = TvDiagnostics.describe(state)
        activity.setContentView(activity.diagnosticsExportedScreen(
            path = path,
            returnToPlayer = returnToPlayer,
            backLabel = backLabel,
            onShare = { shareDiagnostics(diagnostics) },
            onBackDiagnostics = {
                showDiagnostics(state, returnToPlayer, backLabel, onBackDiagnosticsTarget)
            },
            onBackDiagnosticsTarget = onBackDiagnosticsTarget,
        ))
    }

    private fun shareDiagnostics(diagnostics: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CinePilot TV 诊断信息")
            putExtra(Intent.EXTRA_TEXT, diagnostics)
        }
        runCatching {
            activity.startActivity(Intent.createChooser(shareIntent, "分享诊断"))
        }.onFailure {
            Toast.makeText(activity, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlayer(state: TvAppState) {
        auxiliaryBackAction = null
        lastPlaybackBackPressAt = 0L
        val playerView = playerHost.createPlayerView(
            state = state,
            onPlaybackError = { error ->
                activity.runOnUiThread {
                    playerHost.release()
                    showError(error)
                }
            },
            onPlaybackEnded = {
                activity.runOnUiThread {
                    if (workflowController.state().route() != TvRoute.PLAYER) {
                        return@runOnUiThread
                    }
                    playerHost.release()
                    workflowController.back()
                    workflowController.state().selectedItem()?.let(::showDetails)
                        ?: showHome(workflowController.state())
                }
            },
        )
        activity.setContentView(activity.playerScreen(playerView = playerView))
        playerView.post { playerView.requestFocus() }
    }

    private fun openSeriesNextUp() {
        var playbackInfo: PlaybackInfo? = null
        runTask("正在打开本剧下一集...", {
            val nextItem = workflowController.nextUpForSelectedSeries()
            workflowController.openItem(nextItem.id())
            playbackInfo = runCatching {
                workflowController.loadPlaybackChoices(null)
            }.getOrNull()
        }) {
            workflowController.state().selectedItem()?.let { selectedItem ->
                showDetails(selectedItem, playbackInfo)
            } ?: showHome(workflowController.state())
        }
    }

    private fun preparePlaybackWith(preferences: PlaybackSelectionPreferences?) {
        runTask("正在准备播放...", {
            workflowController.preparePlayback(preferences)
        }) {
            showPlayer(workflowController.state())
        }
    }

    companion object {
        private const val PLAYBACK_BACK_EXIT_WINDOW_MS = 2_000L
    }
}
