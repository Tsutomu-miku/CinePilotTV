package tv.cinepilot.tv.playback

import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.util.ArrayDeque
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaItemType
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.details.detailsRouteScreen
import tv.cinepilot.tv.details.seasonDetailScreen
import tv.cinepilot.tv.details.seriesDetailScreen
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.runtime.DeviceCodecDiagnostics
import tv.cinepilot.tv.ui.playerScreen

class PlaybackRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val playerHost: Media3PlayerHost,
    private val subtitleStyleStore: SubtitleStyleStore,
    deviceCodecDiagnostics: DeviceCodecDiagnostics,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    private val loadArtworkImage: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    private val loadBackdropImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    private val loadPersonImage: (ImageView, MediaPerson, Int, Int) -> Unit,
) {
    private val diagnosticsController = PlaybackDiagnosticsController(activity, deviceCodecDiagnostics)
    private var selectedPlaybackInfo: PlaybackInfo? = null
    private var selectedTrackItemId: String? = null
    private var selectedTrackSelection = DetailTrackSelection()
    private var lastPlaybackBackPressAt = 0L
    private var auxiliaryBackAction: (() -> Unit)? = null
    private val mediaBackStack = ArrayDeque<() -> Unit>()

    fun showDetails(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo? = null,
        episodeContext: ShowStructure? = null,
    ) {
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
            loadBackdropImage = loadBackdropImage,
            loadArtworkImage = loadArtworkImage,
            siblingEpisodes = episodeContext?.episodes() ?: emptyList(),
            trackSelection = effectiveTrackSelection,
            onPreparePlayback = { preferences ->
                preparePlaybackWith(applyTrackSelection(item, preferences))
            },
            onTrackSelection = { selection ->
                selectedTrackItemId = item.id()
                selectedTrackSelection = selection
                showDetails(item, effectivePlaybackInfo, episodeContext)
            },
            onSubtitleStyle = { showSubtitleStyleOptions(item) },
            onPlaybackSpeed = { showPlaybackSpeedOptions(item) },
            onSeriesNextUp = ::openSeriesNextUp,
            onOpenEpisodePicker = { openEpisodeSeason(item) },
            onOpenSeries = { openEpisodeSeries(item) },
            onOpenEpisode = { episode ->
                openEpisodeDetail(episode) { showDetails(item, effectivePlaybackInfo, episodeContext) }
            },
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
        val backAction = auxiliaryBackAction
        if (backAction != null) {
            backAction()
            return true
        }
        val mediaBack = mediaBackStack.pollLast() ?: return false
        mediaBack()
        return true
    }

    fun openMediaItem(row: HomeRow, item: MediaItemSummary) {
        workflowController.focusItem(row.id(), item.id())
        val loadingMessage = if (item.playable() || item.shouldOpenAsDetails()) {
            "正在打开详情..."
        } else {
            "正在打开目录..."
        }
        var playbackInfo: PlaybackInfo? = null
        var episodeContext: ShowStructure? = null
        runTask(loadingMessage, {
            if (item.playable()) {
                workflowController.openItem(item.id())
                playbackInfo = runCatching {
                    workflowController.loadPlaybackChoices(null)
                }.getOrNull()
                if (item.type() == MediaItemType.EPISODE) {
                    episodeContext = runCatching {
                        workflowController.loadEpisodeContext(item)
                    }.getOrNull()
                }
            } else if (item.shouldOpenAsDetails()) {
                workflowController.openItem(item.id())
            } else {
                workflowController.openFolder(item.id(), item.name())
            }
        }) {
            val state = workflowController.state()
            if (item.type() == MediaItemType.SERIES) {
                openSeriesDetail(item.id(), pushBack = false)
            } else if (item.type() == MediaItemType.SEASON) {
                openSeasonDetail(item.id(), pushBack = false)
            } else if (item.playable()) {
                state.selectedItem()?.let { selectedItem -> showDetails(selectedItem, playbackInfo, episodeContext) }
            } else {
                showHome(state)
            }
        }
    }

    private fun openSeriesDetail(seriesId: String, pushBack: Boolean) {
        var structure: ShowStructure? = null
        runTask("正在打开剧集详情...", {
            structure = workflowController.loadSeriesStructure(seriesId)
        }) {
            structure?.let { showSeriesDetail(it, pushBack) }
        }
    }

    private fun openSeasonDetail(seasonId: String, pushBack: Boolean) {
        var structure: ShowStructure? = null
        runTask("正在打开季详情...", {
            structure = workflowController.loadSeasonStructure(seasonId)
        }) {
            structure?.let { showSeasonDetail(it, pushBack) }
        }
    }

    private fun openEpisodeSeason(item: MediaItemSummary) {
        if (item.parentId().isBlank()) {
            return
        }
        mediaBackStack.addLast { showDetails(item) }
        openSeasonDetail(item.parentId(), pushBack = false)
    }

    private fun openEpisodeSeries(item: MediaItemSummary) {
        if (item.seriesId().isBlank()) {
            return
        }
        mediaBackStack.addLast { showDetails(item) }
        openSeriesDetail(item.seriesId(), pushBack = false)
    }

    private fun showSeriesDetail(structure: ShowStructure, pushBack: Boolean) {
        if (pushBack) {
            mediaBackStack.addLast { showHome(workflowController.state()) }
        }
        activity.setContentView(activity.seriesDetailScreen(
            structure = structure,
            onOpenSeason = { season ->
                mediaBackStack.addLast { showSeriesDetail(structure, pushBack = false) }
                openSeasonDetail(season.id(), pushBack = false)
            },
            onOpenEpisode = { episode ->
                openEpisodeDetail(episode) { showSeriesDetail(structure, pushBack = false) }
            },
            loadPoster = loadPosterImage,
            loadBackdrop = { backdrop, item -> loadBackdropImage(backdrop, item, 1280, 720) },
            loadArtwork = loadArtworkImage,
            loadPerson = loadPersonImage,
        ))
    }

    private fun showSeasonDetail(structure: ShowStructure, pushBack: Boolean) {
        if (pushBack) {
            mediaBackStack.addLast { showHome(workflowController.state()) }
        }
        activity.setContentView(activity.seasonDetailScreen(
            structure = structure,
            onOpenEpisode = { episode ->
                openEpisodeDetail(episode) { showSeasonDetail(structure, pushBack = false) }
            },
            loadPoster = loadPosterImage,
            loadBackdrop = { backdrop, item -> loadBackdropImage(backdrop, item, 1280, 720) },
            loadArtwork = loadArtworkImage,
            loadPerson = loadPersonImage,
        ))
    }

    private fun openEpisodeDetail(item: MediaItemSummary, backRenderer: () -> Unit) {
        var playbackInfo: PlaybackInfo? = null
        var episodeContext: ShowStructure? = null
        runTask("正在打开详情...", {
            workflowController.openItem(item.id())
            playbackInfo = runCatching {
                workflowController.loadPlaybackChoices(null)
            }.getOrNull()
            episodeContext = runCatching {
                workflowController.loadEpisodeContext(item)
            }.getOrNull()
        }) {
            mediaBackStack.addLast(backRenderer)
            workflowController.state().selectedItem()?.let { selectedItem ->
                showDetails(selectedItem, playbackInfo, episodeContext)
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
        workflowController.state().selectedItem()?.let { showDetails(it) }
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
            preparePlaybackWith(applyTrackSelection(item, lowBitratePreferences(item)))
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
                    workflowController.state().selectedItem()?.let { showDetails(it) }
                        ?: showHome(workflowController.state())
                }
            },
        )
        activity.setContentView(activity.playerScreen(
            playerView = playerView,
            debugInfo = playbackDebugInfo(state),
        ))
        playerView.post { playerView.requestFocus() }
    }

    private fun openSeriesNextUp() {
        var playbackInfo: PlaybackInfo? = null
        var episodeContext: ShowStructure? = null
        runTask("正在打开本剧下一集...", {
            val nextItem = workflowController.nextUpForSelectedSeries()
            workflowController.openItem(nextItem.id())
            playbackInfo = runCatching {
                workflowController.loadPlaybackChoices(null)
            }.getOrNull()
            episodeContext = runCatching {
                workflowController.loadEpisodeContext(nextItem)
            }.getOrNull()
        }) {
            workflowController.state().selectedItem()?.let { selectedItem ->
                showDetails(selectedItem, playbackInfo, episodeContext)
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
            if (selection.subtitleSelected) {
                selection.burnSubtitleWhenTranscoding
            } else {
                base?.alwaysBurnInSubtitleWhenTranscoding()
            },
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

private fun MediaItemSummary.shouldOpenAsDetails(): Boolean {
    return type() == MediaItemType.SERIES || type() == MediaItemType.SEASON
}
