package tv.cinepilot.tv.playback

import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.details.DetailTrackSelection
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
    private val diagnosticsController = PlaybackDiagnosticsController(activity)
    private var selectedPlaybackInfo: PlaybackInfo? = null
    private var selectedTrackItemId: String? = null
    private var selectedTrackSelection = DetailTrackSelection()
    private var lastPlaybackBackPressAt = 0L
    private var auxiliaryBackAction: (() -> Unit)? = null

    fun showDetails(item: MediaItemSummary, playbackInfo: PlaybackInfo? = null) {
        auxiliaryBackAction = null
        if (playbackInfo != null && playbackInfo.itemId() == item.id()) {
            selectedPlaybackInfo = playbackInfo
        }
        val effectivePlaybackInfo = playbackInfo ?: selectedPlaybackInfo?.takeIf { it.itemId() == item.id() }
        val effectiveTrackSelection = normalizedTrackSelectionFor(item, effectivePlaybackInfo)
        activity.setContentView(activity.detailsRouteScreen(
            item = item,
            playbackInfo = effectivePlaybackInfo,
            loadPosterImage = loadPosterImage,
            trackSelection = effectiveTrackSelection,
            onPreparePlayback = { preferences ->
                preparePlaybackWith(applyTrackSelection(item, preferences))
            },
            onTrackSelection = { selection ->
                selectedTrackItemId = item.id()
                selectedTrackSelection = selection
                showDetails(item, effectivePlaybackInfo)
            },
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

    private fun showPlaybackSpeedOptions(item: MediaItemSummary) {
        auxiliaryBackAction = { showDetails(item) }
        activity.setContentView(activity.playbackSpeedScreen(
            onSpeed = { rate -> preparePlaybackWith(applyTrackSelection(item, speedPreferences(item, rate))) },
        ))
    }

    private fun showSubtitleStyleOptions(item: MediaItemSummary) {
        auxiliaryBackAction = { showDetails(item) }
        activity.showSubtitleStyleScreen(subtitleStyleStore)
    }

    fun showDiagnosticsFromError(state: TvAppState, onBackError: () -> Unit) {
        diagnosticsController.showFromError(state, onBackError)
    }

    fun retryLowBitrateFromError(state: TvAppState) {
        state.selectedItem()?.let { item ->
            preparePlaybackWith(lowBitratePreferences(item))
        }
    }

    fun showPlaybackOptionsFromError(state: TvAppState) {
        state.selectedItem()?.let { item ->
            var choices: PlaybackInfo? = null
            runTask("正在读取音轨和字幕...", {
                choices = workflowController.loadPlaybackChoices(null)
            }) {
                showDetails(item, choices)
            }
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

    private fun trackSelectionFor(item: MediaItemSummary): DetailTrackSelection {
        if (selectedTrackItemId != item.id()) {
            selectedTrackItemId = item.id()
            selectedTrackSelection = DetailTrackSelection()
        }
        return selectedTrackSelection
    }

    private fun applyTrackSelection(
        item: MediaItemSummary,
        base: PlaybackSelectionPreferences?,
    ): PlaybackSelectionPreferences? {
        val selection = trackSelectionFor(item)
            .normalizedFor(selectedPlaybackInfo?.takeIf { it.itemId() == item.id() })
        if (!selection.hasExplicitChoice()) {
            return base
        }
        return PlaybackSelectionPreferences(
            base?.startTimeTicks() ?: resumeStartTicks(item),
            if (selection.audioSelected) selection.audioStreamIndex else base?.audioStreamIndex(),
            if (selection.subtitleSelected) selection.subtitleStreamIndex else base?.subtitleStreamIndex(),
            base?.maxAudioChannels(),
            base?.maxWidth() ?: 0,
            base?.maxHeight() ?: 0,
            base?.maxBitRate() ?: 0,
            selection.mediaSourceId ?: base?.mediaSourceId(),
            base?.playbackRate(),
        )
    }

    private fun resumeStartTicks(item: MediaItemSummary): Long {
        return if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
    }

    private fun normalizedTrackSelectionFor(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
    ): DetailTrackSelection {
        val selection = trackSelectionFor(item).normalizedFor(playbackInfo)
        selectedTrackSelection = selection
        return selection
    }

    companion object {
        private const val PLAYBACK_BACK_EXIT_WINDOW_MS = 2_000L
    }
}
