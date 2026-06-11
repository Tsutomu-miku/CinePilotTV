package tv.cinepilot.tv.playback

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.util.ArrayDeque
import java.util.EnumSet
import tv.cinepilot.core.protocol.ChapterInfo
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.protocol.MediaSegmentInfo
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.core.protocol.OfflineRepository
import tv.cinepilot.core.protocol.PlayableMedia
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.protocol.TrickplayInfo
import tv.cinepilot.core.tv.ShowStructure
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.details.DetailTrackSelection
import tv.cinepilot.tv.details.seasonDetailScreen
import tv.cinepilot.tv.details.seriesDetailScreen
import tv.cinepilot.tv.details.detailsRouteScreen
import tv.cinepilot.tv.player.DisplayModeApplier
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.player.DisplayCapabilities
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.plugin.toSnapshot
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.runtime.DeviceCodecDiagnostics
import tv.cinepilot.tv.offline.DownloadCoordinator
import tv.cinepilot.core.protocol.OfflineRepository as CoreOfflineRepo
import tv.cinepilot.tv.subtitle.SubtitleCache
import tv.cinepilot.tv.ui.DisplayModeSwitchPrompt
import tv.cinepilot.tv.ui.ProviderIdEditorEntry
import tv.cinepilot.tv.ui.afmConfirmationSheet
import tv.cinepilot.tv.ui.buildProviderIdEditorEntries
import tv.cinepilot.tv.ui.providerIdsEditorSheet
import tv.cinepilot.tv.ui.PlayerNextUpInfo
import tv.cinepilot.tv.ui.PlayerOverrides
import tv.cinepilot.tv.playback.PlaybackSettingsFocusGroup
import tv.cinepilot.tv.ui.TrickplayGridSpec
import tv.cinepilot.tv.ui.isEpisode
import tv.cinepilot.tv.ui.isSeason
import tv.cinepilot.tv.ui.isSeries
import tv.cinepilot.tv.ui.isSeriesStructureRoot
import tv.cinepilot.tv.ui.isPlaylist
import tv.cinepilot.tv.ui.offlineManagerSheet
import tv.cinepilot.tv.ui.playerScreen
import tv.cinepilot.tv.ui.qualityPickerSheet

class PlaybackRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    private val playerHost: Media3PlayerHost,
    private val subtitleStyleStore: SubtitleStyleStore,
    private val deviceCodecDiagnostics: DeviceCodecDiagnostics,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val loadPosterImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    private val loadArtworkImage: (ImageView, MediaItemSummary, ArtworkTarget, Int, Int) -> Unit,
    private val loadBackdropImage: (ImageView, MediaItemSummary, Int, Int) -> Unit,
    private val loadPersonImage: (ImageView, MediaPerson, Int, Int) -> Unit,
    private val pluginHost: tv.cinepilot.tv.plugin.PluginHost,
    private val downloadCoordinator: DownloadCoordinator,
    private val playbackSettingsStore: PlaybackSettingsStore = PlaybackSettingsStore(activity),
) {
    private val diagnosticsController = PlaybackDiagnosticsController(activity, deviceCodecDiagnostics)
    private val displayCapabilities = DisplayCapabilities(activity)
    private val subtitleSearch = SubtitleSearchController(
        activity = activity,
        workflowController = workflowController,
        pluginHost = pluginHost,
        subtitleCache = SubtitleCache(activity),
        runTask = runTask,
        setAuxiliaryBackAction = { action -> auxiliaryBackAction = action },
    )
    private val playlists = PlaylistController(
        activity = activity,
        workflowController = workflowController,
        runTask = runTask,
        showHome = showHome,
        loadBackdropImage = loadBackdropImage,
        loadArtworkImage = loadArtworkImage,
        setAuxiliaryBackAction = { action -> auxiliaryBackAction = action },
        onOpenItem = { item, onBack ->
            mediaBackStack.addLast(onBack)
            openMediaItem(item)
        },
    )
    private var selectedPlaybackInfo: PlaybackInfo? = null
    private var selectedTrackItemId: String? = null
    private var selectedTrackSelection = DetailTrackSelection()
    private var lastPlaybackBackPressAt = 0L
    private var auxiliaryBackAction: (() -> Unit)? = null
    private val mediaBackStack = ArrayDeque<() -> Unit>()

    // ---- Player overlay state (P1 batch 6) -----------------------------------
    private val mainHandler = Handler(Looper.getMainLooper())
    private val displayModeApplier = DisplayModeApplier(activity)
    private var playerChapters: List<ChapterInfo> = emptyList()
    private var playerSegments: List<MediaSegmentInfo> = emptyList()
    private var playerTrickplay: TrickplayInfo = TrickplayInfo.empty()
    private var playerNextUp: MediaItemSummary? = null
    private var playerNextUpCountdownSeconds: Int = 0
    private var playerNextUpCancelled = false
    private val playerAutoSkipTypes = EnumSet.noneOf(MediaSegmentInfo.Type::class.java)
    private var lastOverlayTickKey = ""
    private var overlayRebuildScheduled = false
    private val overlayRebuildRunnable = Runnable { rebuildPlayerOverlayIfNeeded() }
    private var currentPlaybackSnapshot: MediaItemSummary? = null
    private var currentPlaybackDurationMs: Long = 0L
    private var playbackStartedFired = false

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
        // --- compute offline action label/state BEFORE going into runTask
        val offlineInfo = computeOfflineInfo(item)
        var sameCollection: List<MediaItemSummary> = emptyList()
        runTask("正在加载同系列作品...", {
            runCatching {
                if (workflowController.state().selectedItem()?.id() != item.id()) {
                    workflowController.openItem(item.id())
                }
                sameCollection = workflowController.loadSameCollectionItemsForSelectedItem()
            }
        }) {
            activity.setContentView(activity.detailsRouteScreen(
                item = item,
                playbackInfo = effectivePlaybackInfo,
                loadPosterImage = loadPosterImage,
                loadBackdropImage = loadBackdropImage,
                loadArtworkImage = loadArtworkImage,
                siblingEpisodes = episodeContext?.episodes() ?: emptyList(),
                sameCollectionItems = sameCollection,
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
                onOpenCollectionItem = ::openCollectionItem,
                onOpenFolder = {
                    runTask("正在打开目录...", {
                        workflowController.openFolder(item.id(), item.name())
                    }) {
                        showHome(workflowController.state())
                    }
                },
                onToggleFavorite = {
                    rerenderAfterUserAction({ workflowController.toggleFavorite() }) { newItem ->
                        showDetails(newItem, effectivePlaybackInfo, episodeContext)
                    }
                },
                onToggleWatched = {
                    rerenderAfterUserAction({ workflowController.toggleWatched() }) { newItem ->
                        showDetails(newItem, effectivePlaybackInfo, episodeContext)
                    }
                },
                onSetUserRating = { rating ->
                    rerenderAfterUserAction({ workflowController.setUserRating(rating) }) { newItem ->
                        showDetails(newItem, effectivePlaybackInfo, episodeContext)
                    }
                },
                onOpenProviderIdsEditor = {
                    openProviderIdsEditor(item) {
                        val fresh = workflowController.state().selectedItem() ?: item
                        showDetails(fresh, effectivePlaybackInfo, episodeContext)
                    }
                },
                onProviderBadgeClick = ::openExternalUrl,
                onChooseDownloadQuality = { quality ->
                    showDownloadQualityDialog(item, effectivePlaybackInfo, episodeContext, quality)
                },
                onManageOffline = {
                    showOfflineManager(item, effectivePlaybackInfo, episodeContext)
                },
                offlineActionLabel = offlineInfo?.label,
                offlineActionIsReady = offlineInfo?.isReady == true,
                onAddToPlaylist = {
                    playlists.showPicker(item, effectivePlaybackInfo, episodeContext) {
                        showDetails(item, effectivePlaybackInfo, episodeContext)
                    }
                },
                onSearchSubtitles = {
                    subtitleSearch.showSearchSheet(item, effectivePlaybackInfo, episodeContext) {
                        showDetails(item, effectivePlaybackInfo, episodeContext)
                    }
                },
                hasSubtitleSearch = subtitleSearch.hasSubtitleSearch(),
                supportedHdrTypes = supportedHdrLabels(),
                supportedPassthroughCodecs = supportedPassthroughLabels(),
            ))
        }
    }

    // --- Offline download UI --------------------------------------------------

    private data class OfflineInfo(val label: String, val isReady: Boolean)
    private val qualityLabels = arrayOf("自动", "480p", "720p", "1080p", "4K")
    private val qualityValues = intArrayOf(0, 1, 2, 3, 4)

    private fun computeOfflineInfo(item: MediaItemSummary): OfflineInfo? {
        val server = workflowController.state().authenticated()?.server() ?: return null
        val repo = workflowController.offlineRepository()
        val serverId = server.serverId()
        val ready = repo.readyFor(serverId, item.id())
        if (ready != null) {
            return OfflineInfo("已离线 (${qualityLabelFor(ready.quality())})", true)
        }
        for (entry in repo.listForServer(serverId)) {
            if (entry.itemId() != item.id()) continue
            return when (entry.state()) {
                CoreOfflineRepo.State.QUEUED -> OfflineInfo("下载队列中…", false)
                CoreOfflineRepo.State.DOWNLOADING ->
                    OfflineInfo("正在下载 ${String.format("%.0f%%", entry.progressPercent())}", false)
                CoreOfflineRepo.State.PAUSED -> OfflineInfo("下载已暂停", false)
                CoreOfflineRepo.State.FAILED -> OfflineInfo("下载失败", false)
                CoreOfflineRepo.State.READY -> OfflineInfo("已离线", true)
            }
        }
        return OfflineInfo("离线下载", false)
    }

    private fun qualityLabelFor(q: Int): String = qualityLabels.getOrElse(q.coerceIn(0, qualityValues.lastIndex)) { "自动" }

    /**
     * Displays a quality picker (自动 / 480p / 720p / 1080p / 4K). Once the
     * user confirms a quality, [TvWorkflowController.preparePlayback] resolves
     * the final media source + URL, then the result is handed to the
     * [DownloadCoordinator] for queueing. Rejections (quota / duplicate /
     * server errors) surface as toast messages.
     */
    private fun showDownloadQualityDialog(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
        @Suppress("UNUSED_PARAMETER") requestedQualityHint: Int,
    ) {
        val auth = workflowController.state().authenticated()
        if (auth == null) {
            toast("请先登录服务器")
            return
        }
        if (playbackInfo == null && selectedPlaybackInfo?.takeIf { it.itemId() == item.id() } == null) {
            runTask("正在加载片源信息...", {
                selectedPlaybackInfo = workflowController.loadPlaybackChoices(null)
            }) {
                showDownloadQualityDialog(item, selectedPlaybackInfo, episodeContext, 0)
            }
            return
        }
        showQualityPicker { quality ->
            val preferences = buildQualityPreferences(quality, item)
            var resolved: PlayableMedia? = null
            runTask("正在解析离线地址...", {
                workflowController.preparePlayback(preferences)
                resolved = workflowController.state().playableMedia()
                    ?: throw IllegalStateException("无法解析媒体播放地址")
            }) {
                val r = resolved
                if (r == null) {
                    toast("解析片源地址失败")
                } else {
                    downloadCoordinator.enqueue(auth, item, r, quality) { err ->
                        if (err != null) toast(err) else toast("已加入下载队列")
                        showDetails(item, selectedPlaybackInfo?.takeIf { it.itemId() == item.id() }, episodeContext)
                    }
                }
            }
        }
    }

    /** Map our quality enum (0=auto..4=4K) to a PlaybackSelectionPreferences hint. */
    private fun buildQualityPreferences(quality: Int, item: MediaItemSummary): PlaybackSelectionPreferences? {
        val base = applyTrackSelection(item, null)
        val (maxHeight, maxBitRate) = when (quality) {
            1 -> 480 to 2_000_000
            2 -> 720 to 4_000_000
            3 -> 1080 to 10_000_000
            4 -> 2160 to 40_000_000
            else -> return base // 自动: no caps
        }
        return PlaybackSelectionPreferences(
            base?.startTimeTicks() ?: 0L,
            base?.audioStreamIndex(),
            base?.subtitleStreamIndex(),
            base?.maxAudioChannels(),
            0,
            maxHeight,
            maxBitRate,
            base?.mediaSourceId(),
            base?.playbackRate(),
            base?.alwaysBurnInSubtitleWhenTranscoding() ?: false,
        )
    }

    /**
     * Side sheet that lists every offline entry for this item (one per quality
     * tier) with progress + pause/resume/delete actions. After user interaction
     * we re-render details so the "离线下载" action label reflects the new state.
     */
    private fun showOfflineManager(
        item: MediaItemSummary,
        playbackInfo: PlaybackInfo?,
        episodeContext: ShowStructure?,
    ) {
        val auth = workflowController.state().authenticated() ?: return
        val repo = workflowController.offlineRepository()
        val entries: List<OfflineRepository.Entry> = repo.listForServer(auth.server().serverId())
            .filter { e: OfflineRepository.Entry -> e.itemId() == item.id() }
        if (entries.isEmpty()) {
            toast("暂无离线记录")
            return
        }
        val sheet = activity.offlineManagerSheet(
            entries = entries,
            onClose = { showDetails(item, playbackInfo, episodeContext) },
            onPause = { e: OfflineRepository.Entry ->
                downloadCoordinator.pause(auth.server().serverId(), e.itemId(), e.quality())
            },
            onResume = { e: OfflineRepository.Entry ->
                downloadCoordinator.resume(auth.server().serverId(), e.itemId(), e.quality())
            },
            onDelete = { e: OfflineRepository.Entry ->
                downloadCoordinator.remove(auth.server().serverId(), e.itemId(), e.quality())
            },
        )
        auxiliaryBackAction = { showDetails(item, playbackInfo, episodeContext) }
        activity.setContentView(sheet)
    }

    // --- Playlist UI (delegated to PlaylistController) ---------------------------
    // P3-2: see PlaylistController.kt for picker, create, and detail flows.

    // --- Subtitle search UI (delegated to SubtitleSearchController) ---------
    // P3-1: see SubtitleSearchController.kt for search, download, and caching.

    private inline fun showQualityPicker(crossinline onChosen: (Int) -> Unit) {
        val sheet = activity.qualityPickerSheet(
            labels = qualityLabels.toList(),
            values = qualityValues.toList(),
            default = 0,
            onChosen = { q: Int -> onChosen(q) },
        )
        activity.setContentView(sheet)
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
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
        openMediaItem(item)
    }

    private fun openMediaItem(item: MediaItemSummary) {
        if (item.isPlaylist()) {
            playlists.openDetail(item)
            return
        }
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
                if (item.isEpisode()) {
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
            if (item.isSeries()) {
                openSeriesDetail(item.id(), pushBack = false)
            } else if (item.isSeason()) {
                openSeasonDetail(item.id(), pushBack = false)
            } else if (item.playable()) {
                state.selectedItem()?.let { selectedItem -> showDetails(selectedItem, playbackInfo, episodeContext) }
            } else {
                showHome(state)
            }
        }
    }

    private fun openCollectionItem(item: MediaItemSummary) = openMediaItem(item)

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

    private fun showSeriesDetail(structure: ShowStructure, pushBack: Boolean, foldedSeasonsExpanded: Boolean = false) {
        if (pushBack) {
            mediaBackStack.addLast { showHome(workflowController.state()) }
        }
        val series = structure.series()
        var seasonStartedCache: Map<String, Boolean> = emptyMap()
        var sameCollection: List<MediaItemSummary> = emptyList()
        var collectionLoaded = false
        fun rerender(updated: ShowStructure, pushBackCurrent: Boolean, expanded: Boolean = foldedSeasonsExpanded) {
            showSeriesDetail(updated, pushBackCurrent, expanded)
        }
        fun withExtras(render: (Map<String, Boolean>, List<MediaItemSummary>) -> Unit) {
            val needStatus = seasonStartedCache.isEmpty() && !foldedSeasonsExpanded
            val needCollection = !collectionLoaded
            if (!needStatus && !needCollection) {
                render(if (foldedSeasonsExpanded) emptyMap() else seasonStartedCache, sameCollection)
                return
            }
            runTask("正在加载补充信息...", {
                if (needStatus) {
                    runCatching {
                        seasonStartedCache = workflowController.loadSeasonsStartedStatus(structure)
                    }
                }
                if (needCollection) {
                    runCatching {
                        if (workflowController.state().selectedItem()?.id() != series.id()) {
                            workflowController.openItem(series.id())
                        }
                        sameCollection = workflowController.loadSameCollectionItemsForSelectedItem()
                    }
                    collectionLoaded = true
                }
            }) {
                render(if (foldedSeasonsExpanded) emptyMap() else seasonStartedCache, sameCollection)
            }
        }
        withExtras { status, collection ->
            activity.setContentView(activity.seriesDetailScreen(
                structure = structure,
                onOpenSeason = { season ->
                    mediaBackStack.addLast { showSeriesDetail(structure, pushBack = false, foldedSeasonsExpanded) }
                    openSeasonDetail(season.id(), pushBack = false)
                },
                onSelectSeason = { season ->
                    var next: ShowStructure? = null
                    runTask("正在加载${season.name()}...", {
                        next = workflowController.selectSeasonInStructure(structure, season.id())
                    }) {
                        next?.let { rerender(it, pushBack) }
                    }
                },
                onExpandFoldedSeasons = { rerender(structure, pushBack, expanded = true) },
                seasonStartedStatus = status,
                onOpenEpisode = { episode ->
                    openEpisodeDetail(episode) { showSeriesDetail(structure, pushBack = false, foldedSeasonsExpanded) }
                },
                onOpenCollectionItem = ::openCollectionItem,
                collection = collection,
                loadPoster = loadPosterImage,
                loadBackdrop = { backdrop, item -> loadBackdropImage(backdrop, item, 1280, 720) },
                loadArtwork = loadArtworkImage,
                loadPerson = loadPersonImage,
                onToggleFavorite = {
                    rerenderStructureItem(series.id(), { workflowController.toggleFavorite() }) {
                        showSeriesDetail(structure, pushBack, foldedSeasonsExpanded)
                    }
                },
                onToggleWatched = {
                    rerenderStructureItem(series.id(), { workflowController.toggleWatched() }) {
                        showSeriesDetail(structure, pushBack, foldedSeasonsExpanded)
                    }
                },
                onSetUserRating = { rating ->
                    rerenderStructureItem(series.id(), { workflowController.setUserRating(rating) }) {
                        showSeriesDetail(structure, pushBack, foldedSeasonsExpanded)
                    }
                },
                onOpenProviderIdsEditor = {
                    openProviderIdsEditor(series) {
                        showSeriesDetail(structure, pushBack, foldedSeasonsExpanded)
                    }
                },
                onProviderBadgeClick = ::openExternalUrl,
                onPersonClick = ::openPerson,
            ))
        }
    }

    private fun showSeasonDetail(structure: ShowStructure, pushBack: Boolean) {
        if (pushBack) {
            mediaBackStack.addLast { showHome(workflowController.state()) }
        }
        val season = structure.selectedSeason() ?: structure.series()
        var sameCollection: List<MediaItemSummary> = emptyList()
        var loaded = false
        fun render(collection: List<MediaItemSummary>) {
            activity.setContentView(activity.seasonDetailScreen(
                structure = structure,
                onOpenEpisode = { episode ->
                    openEpisodeDetail(episode) { showSeasonDetail(structure, pushBack = false) }
                },
                collection = collection,
                onOpenCollectionItem = ::openCollectionItem,
                loadPoster = loadPosterImage,
                loadBackdrop = { backdrop, item -> loadBackdropImage(backdrop, item, 1280, 720) },
                loadArtwork = loadArtworkImage,
                loadPerson = loadPersonImage,
                onToggleFavorite = {
                    rerenderStructureItem(season.id(), { workflowController.toggleFavorite() }) {
                        showSeasonDetail(structure, pushBack)
                    }
                },
                onToggleWatched = {
                    rerenderStructureItem(season.id(), { workflowController.toggleWatched() }) {
                        showSeasonDetail(structure, pushBack)
                    }
                },
                onSetUserRating = { rating ->
                    rerenderStructureItem(season.id(), { workflowController.setUserRating(rating) }) {
                        showSeasonDetail(structure, pushBack)
                    }
                },
                onOpenProviderIdsEditor = {
                    openProviderIdsEditor(season) {
                        showSeasonDetail(structure, pushBack)
                    }
                },
                onProviderBadgeClick = ::openExternalUrl,
                onPersonClick = ::openPerson,
            ))
        }
        if (loaded) {
            render(sameCollection)
        } else {
            runTask("正在加载同系列作品...", {
                runCatching {
                    if (workflowController.state().selectedItem()?.id() != season.id()) {
                        workflowController.openItem(season.id())
                    }
                    sameCollection = workflowController.loadSameCollectionItemsForSelectedItem()
                }
                loaded = true
            }) {
                render(sameCollection)
            }
        }
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
        releasePlayerAndDispatchStop()
        displayModeApplier.restorePrevious()
        workflowController.back()
        workflowController.state().selectedItem()?.let { showDetails(it) }
            ?: showHome(workflowController.state())
    }

    private fun releasePlayerAndDispatchStop() {
        val snapshot = currentPlaybackSnapshot
        val durationMs = currentPlaybackDurationMs
        val positionMs = ticksToMs(playerHost.currentPositionTicks())
        playerHost.release()
        val auth = workflowController.state().authenticated()
        if (snapshot != null && auth != null && playbackStartedFired) {
            pluginHost.dispatchPlaybackStopped(snapshot.toSnapshot(auth), positionMs, durationMs)
        }
        playbackStartedFired = false
        currentPlaybackSnapshot = null
        currentPlaybackDurationMs = 0L
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
        val selectedItem = state.selectedItem()
        currentPlaybackSnapshot = selectedItem
        currentPlaybackDurationMs = if (selectedItem == null) 0L else ticksToMs(selectedItem.runTimeTicks() ?: 0L)
        playbackStartedFired = false
        val settings = playbackSettingsStore.current()
        // Load chapters / segments / trickplay / next-up metadata on the worker executor.
        // Data is cached on workflowController.selectedItem and on controller fields; when
        // unavailable the overlays simply stay hidden, player continues unblocked.
        var chapters: List<ChapterInfo> = emptyList()
        var segments: List<MediaSegmentInfo> = emptyList()
        var trickplay: TrickplayInfo = TrickplayInfo.empty()
        var nextUp: MediaItemSummary? = null
        runTask("正在准备播放器辅助数据...", {
            val selected = state.selectedItem()
            if (selected != null) {
                runCatching { workflowController.loadChaptersForSelectedItem() }
                    .onSuccess { chapters = it }
                runCatching { workflowController.loadMediaSegmentsForSelectedItem() }
                    .onSuccess { segments = it }
                if (settings.showTrickplayPreview) {
                    runCatching { workflowController.loadTrickplayForSelectedItem(320) }
                        .onSuccess { trickplay = it }
                }
                if (settings.autoPlayNext && selected.isEpisode()) {
                    runCatching { workflowController.nextUpEpisodeForSelected() }
                        .onSuccess { nextUp = it }
                }
            }
        }) {
            playerChapters = chapters
            playerSegments = segments
            playerTrickplay = trickplay
            playerNextUp = nextUp
            playerNextUpCountdownSeconds = 0
            playerNextUpCancelled = false
            playerAutoSkipTypes.clear()
            lastOverlayTickKey = ""
            overlayRebuildScheduled = false
            val (offlineFactory, offlineCacheKey) = run {
                val serverId = state.authenticated()?.server()?.serverId()
                if (serverId != null && selectedItem != null) {
                    val ready = workflowController.offlineRepository().readyFor(serverId, selectedItem.id())
                    if (ready != null) {
                        val key = tv.cinepilot.tv.offline.DownloadCoordinator.mediaSourceContentId(
                            serverId, selectedItem.id(), ready.quality(),
                        )
                        downloadCoordinator.cacheDataSourceFactory to key
                    } else {
                        null to null
                    }
                } else {
                    null to null
                }
            }
            val externalSubs = selectedItem?.let { subtitleSearch.buildExternalSubtitles(it) }.orEmpty()
            val playerView = playerHost.createPlayerView(
                state = state,
                subtitleEncoding = settings.subtitleEncoding,
                dataSourceOverride = offlineFactory,
                externalSubtitles = externalSubs,
                offlineCacheKey = offlineCacheKey,
                onPlaybackError = { error ->
                    activity.runOnUiThread {
                        releasePlayerAndDispatchStop()
                        displayModeApplier.restorePrevious()
                        showError(error)
                    }
                },
                onPlaybackEnded = {
                    activity.runOnUiThread {
                        releasePlayerAndDispatchStop()
                        displayModeApplier.restorePrevious()
                        if (workflowController.state().route() != TvRoute.PLAYER) {
                            return@runOnUiThread
                        }
                        // Episode end: honor auto-play if the user never cancelled the Next Up
                        // banner.  Otherwise fall back to standard "return to details" flow.
                        val autoPlay = playbackSettingsStore.current().autoPlayNext &&
                                !playerNextUpCancelled
                        if (autoPlay) {
                            val next = playerNextUp
                            if (next != null && next.id().isNotBlank()) {
                                openEpisodeDetail(next, backRenderer = {
                                    workflowController.back()
                                    workflowController.state().selectedItem()?.let(::showDetails)
                                        ?: showHome(workflowController.state())
                                })
                                return@runOnUiThread
                            }
                        }
                        workflowController.back()
                        workflowController.state().selectedItem()?.let { showDetails(it) }
                            ?: showHome(workflowController.state())
                    }
                },
                onPositionTick = ::onPlayerPositionTick,
            )
            val pending = displayModeApplier.computePendingMode(
                referenceFrameRate = playerHost.referenceFrameRate(),
                matchColorSpace = settings.matchColorSpace,
                enabled = settings.autoFrameMatching,
            )
            fun finalize() {
                rebuildPlayerOverlay(state, rebuildRoot = true, playerView = playerView)
                playerView.post { playerView.requestFocus() }
            }
            if (pending != null &&
                settings.confirmBeforeFrameSwitch &&
                !settings.skipFrameSwitchConfirm
            ) {
                showAfmConfirmation(pending, ::finalize)
            } else {
                if (pending != null) displayModeApplier.commitPendingMode(pending)
                finalize()
            }
        }
    }

    private fun showAfmConfirmation(
        pending: android.view.Display.Mode,
        finalize: () -> Unit,
    ) {
        val prompt = DisplayModeSwitchPrompt.from(pending)
        val current = displayModeApplier.formatCurrentModeForDiagnostics()
        activity.setContentView(activity.afmConfirmationSheet(
            prompt = prompt,
            currentModeLabel = current,
            onSwitchNow = {
                displayModeApplier.commitPendingMode(pending)
                finalize()
            },
            onSkipOnce = {
                finalize()
            },
            onAlwaysSkip = {
                playbackSettingsStore.save(
                    playbackSettingsStore.current().copy(skipFrameSwitchConfirm = true)
                )
                displayModeApplier.commitPendingMode(pending)
                finalize()
            },
        ))
    }

    /**
     * Retries AFM once if the video format wasn't available at prepare() time.
     * Called from the position-tick callback so we pick up the real frame rate
     * as soon as the demuxer has parsed the stream.
     */
    private fun maybeDeferredFrameMatch() {
        if (displayModeApplier.hasAppliedMode()) return
        val frameRate = playerHost.referenceFrameRate() ?: return
        val settings = playbackSettingsStore.current()
        if (!settings.autoFrameMatching) return
        val pending = displayModeApplier.computePendingMode(
            referenceFrameRate = frameRate,
            matchColorSpace = settings.matchColorSpace,
            enabled = settings.autoFrameMatching,
        ) ?: return
        displayModeApplier.commitPendingMode(pending)
    }

    private fun onPlayerPositionTick(positionTicks: Long, durationTicks: Long) {
        val current = workflowController.state()
        if (current.route() != TvRoute.PLAYER) return
        // Deferred AFM: if the video format wasn't available at prepare() time, retry
        // once the actual frame rate is known (detected via position ticks).
        maybeDeferredFrameMatch()
        // Plugin playback hooks.
        val auth = current.authenticated()
        val item = currentPlaybackSnapshot
        val durationMs = ticksToMs(durationTicks)
        val positionMs = ticksToMs(positionTicks)
        if (item != null && auth != null) {
            val snapshot = item.toSnapshot(auth)
            if (!playbackStartedFired) {
                playbackStartedFired = true
                if (durationMs > 0) currentPlaybackDurationMs = durationMs
                pluginHost.dispatchPlaybackStarted(snapshot, currentPlaybackDurationMs.coerceAtLeast(durationMs))
            }
            pluginHost.dispatchPlaybackProgress(snapshot, positionMs, currentPlaybackDurationMs.coerceAtLeast(durationMs))
        }
        val settings = playbackSettingsStore.current()

        // Auto-skip (once per type per playback).
        for (segment in playerSegments) {
            if (playerAutoSkipTypes.contains(segment.type())) continue
            if (!segment.containsTicks(positionTicks)) continue
            val autoSkip = when (segment.type()) {
                MediaSegmentInfo.Type.INTRO -> settings.autoSkipIntro
                MediaSegmentInfo.Type.CREDITS -> settings.autoSkipCredits
                else -> false
            }
            if (autoSkip) {
                playerAutoSkipTypes.add(segment.type())
                playerHost.seekToTicks(segment.endPositionTicks())
                continue
            }
        }

        // Next Up countdown window: final 30 seconds of an episode (or of the credits segment
        // when it exists).
        if (playerNextUp != null && !playerNextUpCancelled) {
            val credits = playerSegments.firstOrNull { it.type() == MediaSegmentInfo.Type.CREDITS }
            val threshold = if (credits != null && credits.startPositionTicks() > 0) {
                credits.startPositionTicks()
            } else {
                (durationTicks - MediaTicks.fromSeconds(30)).coerceAtLeast(0)
            }
            if (positionTicks >= threshold && durationTicks > 0) {
                val remaining = (durationTicks - positionTicks).coerceAtLeast(0L)
                val countdown = (MediaTicks.toSeconds(remaining)).toInt().coerceAtLeast(1)
                if (playerNextUpCountdownSeconds != countdown) {
                    playerNextUpCountdownSeconds = countdown
                    scheduleOverlayRebuild()
                }
            } else if (playerNextUpCountdownSeconds != 0) {
                playerNextUpCountdownSeconds = 0
                scheduleOverlayRebuild()
            }
        }

        scheduleOverlayRebuild()
    }

    private fun scheduleOverlayRebuild() {
        if (overlayRebuildScheduled) return
        overlayRebuildScheduled = true
        mainHandler.post(overlayRebuildRunnable)
    }

    private fun rebuildPlayerOverlayIfNeeded() {
        overlayRebuildScheduled = false
        val state = workflowController.state()
        if (state.route() != TvRoute.PLAYER) return
        val surfaceView = playerHost.playerSurfaceView() ?: return
        val key = computeOverlayStateKey()
        if (key == lastOverlayTickKey) return
        lastOverlayTickKey = key
        rebuildPlayerOverlay(state, rebuildRoot = false, playerView = surfaceView)
    }

    private fun computeOverlayStateKey(): String {
        val position = playerHost.currentPositionTicks()
        val segments = playerSegments.joinToString("|") {
            "${it.type()}:${it.startPositionTicks()}-${it.endPositionTicks()}"
        }
        val chaptersCount = playerChapters.size
        val hasNextUp = if (playerNextUp == null) "0" else "1"
        return "$position|$segments|$chaptersCount|$hasNextUp|$playerNextUpCountdownSeconds|$playerNextUpCancelled"
    }

    private fun rebuildPlayerOverlay(
        state: TvAppState,
        rebuildRoot: Boolean,
        playerView: View,
    ) {
        // Detach the player surface from its current parent before reusing it in a new
        // layout, otherwise addView() throws "child already has a parent".
        (playerView.parent as? ViewGroup)?.removeView(playerView)
        val settings = playbackSettingsStore.current()
        val positionTicks = playerHost.currentPositionTicks()
        val introSegment = playerSegments.firstOrNull { it.type() == MediaSegmentInfo.Type.INTRO }
            ?.takeIf { it.containsTicks(positionTicks) && settings.showIntroSkipButton }
            ?.let { it.startPositionTicks()..it.endPositionTicks() }
        val creditsSegment = playerSegments.firstOrNull { it.type() == MediaSegmentInfo.Type.CREDITS }
            ?.takeIf { it.containsTicks(positionTicks) && settings.showCreditsSkipButton }
            ?.let { it.startPositionTicks()..it.endPositionTicks() }
        val nextUpInfo = playerNextUp?.takeIf { playerNextUpCountdownSeconds > 0 }
            ?.let { next ->
                PlayerNextUpInfo(
                    episodeLabel = next.parentIndexNumber()?.let { s ->
                        next.indexNumber()?.let { e -> "第 ${s}季 · 第 ${e}集" }
                    } ?: next.indexNumber()?.let { e -> "第 ${e}集" } ?: "下一集",
                    title = next.name(),
                    overview = next.overview(),
                    artworkUrl = "",
                    countdownSeconds = playerNextUpCountdownSeconds,
                    autoPlay = settings.autoPlayNext,
                )
            }
        val chapters = if (settings.showChapterStrip) {
            playerChapters.map { it.startPositionTicks() to (it.name().ifBlank {
                MediaTicks.formatShort(it.startPositionTicks())
            }) }
        } else {
            emptyList()
        }
        val trickplaySpec = if (settings.showTrickplayPreview && playerTrickplay.isValid()) {
            TrickplayGridSpec(
                tileWidth = playerTrickplay.tileWidth(),
                tileHeight = playerTrickplay.tileHeight(),
                tilesPerRow = playerTrickplay.tilesPerRow(),
                tileIntervalTicks = playerTrickplay.tileIntervalTicks(),
                tileCount = playerTrickplay.tileCount(),
            )
        } else {
            TrickplayGridSpec(0, 0, 0, 0L, 0)
        }
        val overlay = activity.playerScreen(
            playerView = playerView,
            debugInfo = playbackDebugInfo(state) +
                    "\n显示模式：${displayModeApplier.formatCurrentModeForDiagnostics()}" +
                    "\nHDR 格式：${playerHdrLabel()}" +
                    "\nHDR 支持：${displayCapabilities.describe()}" +
                    "\n音频直通：${audioPassthroughLabel()}",
            overlays = PlayerOverrides(
                chapterTitles = chapters,
                onChapterClick = { targetTicks ->
                    playerHost.seekToTicks(targetTicks)
                    scheduleOverlayRebuild()
                },
                introSegmentTicks = introSegment,
                creditsSegmentTicks = creditsSegment,
                onSkipIntro = {
                    playerSegments.firstOrNull { it.type() == MediaSegmentInfo.Type.INTRO }
                        ?.endPositionTicks()
                        ?.let { playerHost.seekToTicks(it) }
                    scheduleOverlayRebuild()
                },
                onSkipCredits = {
                    playerSegments.firstOrNull { it.type() == MediaSegmentInfo.Type.CREDITS }
                        ?.endPositionTicks()
                        ?.let { playerHost.seekToTicks(it) }
                    scheduleOverlayRebuild()
                },
                nextUp = nextUpInfo,
                onPlayNext = lambda@{
                    val next = playerNextUp ?: return@lambda
                    releasePlayerAndDispatchStop()
                    displayModeApplier.restorePrevious()
                    openEpisodeDetail(next, backRenderer = {
                        workflowController.back()
                        workflowController.state().selectedItem()?.let(::showDetails)
                            ?: showHome(workflowController.state())
                    })
                },
                onCancelNextUp = {
                    playerNextUpCancelled = true
                    scheduleOverlayRebuild()
                },
                trickplayTileUrl = playerTrickplay.imageUrl(),
                trickplayGrid = trickplaySpec,
                onOpenPlaybackSettings = { openPlaybackSettings(state) },
            ),
        )
        if (rebuildRoot) {
            activity.setContentView(overlay)
            overlay.post { overlay.requestFocus() }
        } else {
            val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                ?: return
            content.removeAllViews()
            content.addView(overlay)
            overlay.post { overlay.requestFocus() }
        }
    }

    private fun openPlaybackSettings(state: TvAppState) {
        val target = state
        val surfaceView = playerHost.playerSurfaceView() ?: return
        auxiliaryBackAction = {
            // Detach and reuse the player surface when returning from settings.
            (surfaceView.parent as? ViewGroup)?.removeView(surfaceView)
            activity.setContentView(activity.playerScreen(
                playerView = surfaceView,
                debugInfo = playbackDebugInfo(target),
            ))
        }
        showPlaybackSettingsScreen(focus = PlaybackSettingsFocusGroup.AFM, state = target)
    }

    fun showPlaybackSettingsScreen(
        focus: PlaybackSettingsFocusGroup = PlaybackSettingsFocusGroup.AFM,
        state: TvAppState,
    ) {
        val current = playbackSettingsStore.current()
        activity.setContentView(activity.playbackSettingsScreen(
            current = current,
            focusGroup = focus,
            onChanged = { updated ->
                playbackSettingsStore.save(updated)
                showPlaybackSettingsScreen(focus, state)
            },
            onBack = {
                val back = auxiliaryBackAction
                auxiliaryBackAction = null
                if (back != null) back()
            },
        ))
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
        val burnGraphic = playbackSettingsStore.current().burnGraphicSubtitleWhenTranscoding
        if (!selection.hasExplicitChoice() && base != null && burnGraphic == base.alwaysBurnInSubtitleWhenTranscoding()) {
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
            } else if (burnGraphic) {
                true
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

    // ---- P1 user-action helpers ---------------------------------------------------------------

    /**
     * Runs [action] (e.g. `toggleFavorite`) on the currently selected detail item,
     * then re-renders using [rerender] which receives the freshly-updated selected item.
     * Intended for movie/episode detail screens where the workflow already has a selectedItem.
     */
    private fun rerenderAfterUserAction(
        action: () -> Unit,
        rerender: (MediaItemSummary) -> Unit,
    ) {
        val before = workflowController.state().selectedItem()
        val auth = workflowController.state().authenticated()
        var updated: MediaItemSummary? = null
        runTask("正在更新...", {
            action()
            updated = workflowController.state().selectedItem()
        }) {
            val after = updated
            if (before != null && after != null && auth != null) {
                dispatchUserDataDeltas(before, after, auth)
            }
            after?.let(rerender)
        }
    }

    /**
     * Ensures the workflow has [itemId] loaded as its selectedItem (so `toggleFavorite` /
     * `toggleWatched` operate on the right media), runs [action], then invokes [rerender].
     * Used for series/season detail pages where `ShowStructure` loads via a separate endpoint
     * and `selectedItem` may be stale or missing.
     */
    private fun rerenderStructureItem(
        itemId: String,
        action: () -> Unit,
        rerender: () -> Unit,
    ) {
        val auth = workflowController.state().authenticated()
        var before: MediaItemSummary? = null
        var after: MediaItemSummary? = null
        runTask("正在更新...", {
            val current = workflowController.state().selectedItem()
            before = if (current?.id() == itemId) current else null
            if (current?.id() != itemId) {
                runCatching { workflowController.openItem(itemId) }
            }
            action()
            after = workflowController.state().selectedItem()
        }) {
            val ref = before ?: after
            if (ref != null && after != null && auth != null) {
                dispatchUserDataDeltas(ref, after!!, auth)
            }
            rerender()
        }
    }

    /** Compare before/after user-data and fire matching plugin hooks. */
    private fun dispatchUserDataDeltas(
        before: MediaItemSummary,
        after: MediaItemSummary,
        auth: tv.cinepilot.core.protocol.AuthenticatedServer,
    ) {
        val snapshot = after.toSnapshot(auth)
        val beforeUd = before.userData()
        val afterUd = after.userData()
        if (beforeUd.favorite() != afterUd.favorite()) {
            pluginHost.dispatchFavorite(snapshot, afterUd.favorite())
        }
        if (beforeUd.played() != afterUd.played()) {
            pluginHost.dispatchWatched(snapshot, afterUd.played())
        }
        val beforeRating = beforeUd.userRating()
        val afterRating = afterUd.userRating()
        if ((beforeRating ?: -1.0) != (afterRating ?: -1.0) && afterRating != null) {
            pluginHost.dispatchRating(snapshot, afterRating)
        }
    }

    private fun ticksToMs(ticks: Long): Long = if (ticks <= 0L) 0L else ticks / 10_000L

    private fun openExternalUrl(url: String) {
        if (url.isBlank()) return
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    // --- HDR / audio passthrough helpers (P3-4) ---------------------------------------

    private fun playerHdrLabel(): String {
        val runtimeFormat = playerHost.currentHdrFormat()
        if (runtimeFormat != null) {
            return hdrFormatLabel(runtimeFormat) + "（运行时检测）"
        }
        // Fallback to metadata-based detection from playable media
        val playable = workflowController.state().playableMedia() ?: return "SDR"
        val video = playable.mediaStreams().firstOrNull {
            it.type() == tv.cinepilot.core.protocol.MediaStreamType.VIDEO
        } ?: return "SDR"
        val format = DisplayCapabilities.Companion.detectHdrFormat(
            video.videoRangeType(),
            video.videoRange(),
            video.profile(),
            video.displayTitle(),
        )
        return if (format == DisplayCapabilities.HdrFormat.UNKNOWN) "SDR" else hdrFormatLabel(format) + "（元数据）"
    }

    private fun hdrFormatLabel(format: DisplayCapabilities.HdrFormat): String = when (format) {
        DisplayCapabilities.HdrFormat.DOLBY_VISION -> "杜比视界 Dolby Vision"
        DisplayCapabilities.HdrFormat.HDR10 -> "HDR10"
        DisplayCapabilities.HdrFormat.HDR10_PLUS -> "HDR10+"
        DisplayCapabilities.HdrFormat.HLG -> "HLG"
        DisplayCapabilities.HdrFormat.UNKNOWN -> "未知 HDR"
    }

    private fun audioPassthroughLabel(): String {
        val currentCodec = playerHost.currentAudioCodec()
        val supported = deviceCodecDiagnostics.supportedPassthroughCodecs()
        val supportedLabels = if (supported.isEmpty()) "无" else supported.joinToString(" / ") { it.label }
        val codecLabel = currentCodec?.let { codec ->
            val passthroughName = passthroughCodecName(codec)
            "当前音频：${passthroughName ?: codec}"
        } ?: "当前音频：未知"
        return "$codecLabel\n设备支持直通：$supportedLabels"
    }

    private fun passthroughCodecName(mimeType: String): String? {
        return when (mimeType.lowercase()) {
            "audio/ac3" -> "Dolby Digital"
            "audio/eac3" -> "Dolby Digital+"
            "audio/true-hd" -> "Dolby TrueHD"
            "audio/vnd.dts" -> "DTS"
            "audio/vnd.dts.hd" -> "DTS-HD MA"
            else -> null
        }
    }

    private fun supportedHdrLabels(): Set<String> {
        val support = displayCapabilities.hdrSupport()
        val labels = mutableSetOf<String>()
        if (support.dolbyVision) labels.add("Dolby Vision")
        if (support.hdr10) labels.add("HDR10")
        if (support.hdr10Plus) labels.add("HDR10+")
        if (support.hlg) labels.add("HLG")
        return labels
    }

    private fun supportedPassthroughLabels(): Set<String> {
        return deviceCodecDiagnostics.supportedPassthroughCodecs()
            .map { it.label }
            .toSet()
    }

    private fun openPerson(person: MediaPerson) {
        if (person.id().isBlank()) return
        val displayName = person.name().ifBlank { person.id() }
        runTask("正在加载 $displayName 的作品...", {
            workflowController.openPerson(person.id(), displayName)
        }) {
            showHome(workflowController.state())
        }
    }

    /**
     * Opens the ProviderIds editor side-sheet. Writes back each edited key on save; optionally
     * triggers a full server-side metadata refresh (user controls via checkbox). After the
     * network round-trip re-renders via [rerender] so badges / rail data refresh.
     */
    private fun openProviderIdsEditor(
        currentItem: MediaItemSummary,
        rerender: () -> Unit,
    ) {
        auxiliaryBackAction = rerender
        val values: Map<String, String> = currentItem.providerIds()?.toMap()
            ?: emptyMap()
        val entries: List<ProviderIdEditorEntry> = buildProviderIdEditorEntries(values)
        activity.setContentView(activity.providerIdsEditorSheet(
            entries = entries,
            onCancel = rerender,
            onSave = { next, refresh ->
                runTask("正在保存编号...", {
                    if (workflowController.state().selectedItem()?.id() != currentItem.id()) {
                        runCatching { workflowController.openItem(currentItem.id()) }
                    }
                    // Merge the user-supplied edits into the existing provider-id map; keys
                    // the user has cleared out of the editor are removed from the full map.
                    val merged = mutableMapOf<String, String>()
                    values.forEach { (k, v) -> merged[k] = v }
                    entries.forEach { entry ->
                        val newVal = next[entry.key]
                        if (newVal == null) {
                            merged.remove(entry.key)
                        } else {
                            merged[entry.key] = newVal
                        }
                    }
                    workflowController.setProviderIdsForSelectedItem(merged)
                    if (refresh) {
                        runCatching {
                            workflowController.refreshMetadataForSelectedItem(true)
                        }
                    }
                }) {
                    rerender()
                }
            },
        ))
    }
}

private fun MediaItemSummary.shouldOpenAsDetails(): Boolean {
    return isSeriesStructureRoot()
}
