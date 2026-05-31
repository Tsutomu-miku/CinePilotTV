package tv.cinepilot.tv

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.core.protocol.ServerFlavor
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.auth.loginScreen
import tv.cinepilot.tv.auth.quickConnectScreen
import tv.cinepilot.tv.auth.serverEntryScreen
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.playback.diagnosticsExportedScreen
import tv.cinepilot.tv.playback.diagnosticsScreen
import tv.cinepilot.tv.playback.playbackOptionsScreen
import tv.cinepilot.tv.playback.playbackSpeedScreen
import tv.cinepilot.tv.playback.playerReadyScreen
import tv.cinepilot.tv.runtime.PrimaryImageLoader
import tv.cinepilot.tv.runtime.QuickConnectPoller
import tv.cinepilot.tv.runtime.RecentAccountStore
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.actionStrip
import tv.cinepilot.tv.ui.compactAction
import tv.cinepilot.tv.ui.compactIconAction
import tv.cinepilot.tv.ui.detailsScreen
import tv.cinepilot.tv.ui.dp
import tv.cinepilot.tv.ui.homeScreen
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.input
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.mediaTechnicalPills
import tv.cinepilot.tv.ui.playerScreen
import tv.cinepilot.tv.ui.rounded
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.HomeNavigation
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.TvSize
import tv.cinepilot.tv.ui.episodeLabel
import tv.cinepilot.tv.ui.formatPlaybackPosition
import tv.cinepilot.tv.ui.tvErrorMessage

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CinePilotViewModel
    private lateinit var playerHost: Media3PlayerHost
    private lateinit var primaryImageLoader: PrimaryImageLoader
    private lateinit var quickConnectPoller: QuickConnectPoller
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recentAccountStore by lazy { RecentAccountStore(this) }
    private var searchVisible = false
    private var selectedPlaybackInfo: PlaybackInfo? = null
    private var lastPlaybackBackPressAt = 0L
    private var quickConnectStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, CinePilotViewModel.factory(applicationContext))[CinePilotViewModel::class.java]
        playerHost = Media3PlayerHost(this, viewModel.mediaBrowserClient)
        primaryImageLoader = PrimaryImageLoader(viewModel.mediaBrowserClient)
        quickConnectPoller = QuickConnectPoller(
            mainHandler,
            executor,
            viewModel.workflowController,
            onWaiting = ::updateQuickConnectWaiting,
            onApproved = {
                rememberAccount()
                showHome(viewModel.workflowController.state())
            },
            onError = ::showError,
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
        if (handleQaLoginIntent(intent)) {
            return
        }
        restoreRecentAccountOnLaunch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleQaLoginIntent(intent)
    }

    override fun onDestroy() {
        quickConnectPoller.stop()
        playerHost.shutdown()
        primaryImageLoader.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun handleBackPressed() {
        if (searchVisible) {
            searchVisible = false
            showHome(viewModel.workflowController.state())
            return
        }
        when (viewModel.workflowController.state().route()) {
            TvRoute.SERVER_ENTRY -> finish()
            TvRoute.HOME -> {
                val state = viewModel.workflowController.back()
                if (state.route() == TvRoute.HOME) {
                    showHome(state)
                } else {
                    showServerEntry()
                }
            }
            TvRoute.LOGIN, TvRoute.ERROR -> {
                val state = viewModel.workflowController.back()
                if (state.route() == TvRoute.HOME) {
                    showHome(state)
                } else {
                    showServerEntry()
                }
            }
            TvRoute.DETAILS -> {
                viewModel.workflowController.back()
                showHome(viewModel.workflowController.state())
            }
            TvRoute.PLAYER -> {
                handlePlaybackBackPressed()
            }
        }
    }

    private fun handlePlaybackBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackBackPressAt > PLAYBACK_BACK_EXIT_WINDOW_MS) {
            lastPlaybackBackPressAt = now
            Toast.makeText(this, "再次按返回退出播放", Toast.LENGTH_SHORT).show()
            return
        }
        exitPlaybackToDetails()
    }

    private fun exitPlaybackToDetails() {
        lastPlaybackBackPressAt = 0L
        playerHost.release()
        viewModel.workflowController.back()
        viewModel.workflowController.state().selectedItem()?.let(::showDetails)
            ?: showHome(viewModel.workflowController.state())
    }

    private fun showServerEntry() {
        quickConnectPoller.stop()
        val recentAccounts = recentAccountStore.accounts()
        val recentServers = recentAccountStore.servers()
            .filterNot { server -> recentAccounts.any { account -> account.serverAddress == server.serverAddress } }
        setContentView(serverEntryScreen(
            recentAccounts = recentAccounts,
            recentServers = recentServers,
            onContinueAccount = { account ->
                runTask("正在恢复上次登录...", {
                    viewModel.workflowController.submitServer(account.serverAddress)
                    viewModel.workflowController.restoreSession(account.userId)
                }) {
                    showHome(viewModel.workflowController.state())
                }
            },
            onOpenServer = ::connectToServer,
            onClearAccounts = {
                clearSavedAccounts()
                showServerEntry()
            },
        ))
    }

    private fun connectToServer(serverAddress: String) {
        runTask("正在连接服务器...", {
            viewModel.workflowController.submitServer(serverAddress)
            viewModel.workflowController.state().server()?.let { server ->
                recentAccountStore.rememberServer(server.address().value(), server.serverName())
            }
            loadPublicUsersIfAvailable()
        }) {
            showLogin()
        }
    }

    private fun restoreRecentAccountOnLaunch() {
        val account = recentAccountStore.accounts().firstOrNull()
        if (account == null) {
            showServerEntry()
            return
        }
        showLoading("正在恢复上次登录...")
        executor.execute {
            try {
                viewModel.workflowController.submitServer(account.serverAddress)
                viewModel.workflowController.restoreSession(account.userId)
                runOnUiThread { showHome(viewModel.workflowController.state()) }
            } catch (error: Throwable) {
                runOnUiThread { showServerEntry() }
            }
        }
    }

    private fun showLogin() {
        quickConnectPoller.stop()
        val state = viewModel.workflowController.state()
        setContentView(loginScreen(
            serverName = state.server()?.serverName().orEmpty(),
            publicUsers = state.publicUsers(),
            quickConnectAvailable = state.server()?.flavor() == ServerFlavor.JELLYFIN,
            onLogin = ::loginWithCredentials,
            onQuickConnect = ::startQuickConnectLogin,
            onBackToServer = ::showServerEntry,
        ))
    }

    private fun loginWithCredentials(username: String, password: String) {
        runTask("正在登录并加载首页...", {
            viewModel.workflowController.login(username, password)
        }) {
            rememberAccount()
            showHome(viewModel.workflowController.state())
        }
    }

    private fun handleQaLoginIntent(intent: Intent?): Boolean {
        if (!isDebuggable()) {
            return false
        }
        val server = intent?.getStringExtra("qa_server")?.takeIf { it.isNotBlank() } ?: return false
        val username = intent.getStringExtra("qa_username").orEmpty()
        val password = intent.getStringExtra("qa_password").orEmpty()
        showLoading("正在执行 QA 登录...")
        executor.execute {
            try {
                viewModel.workflowController.submitServer(server)
                loadPublicUsersIfAvailable()
                viewModel.workflowController.login(username, password)
                runOnUiThread {
                    rememberAccount()
                    showHome(viewModel.workflowController.state())
                }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
        return true
    }

    private fun isDebuggable(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun startQuickConnectLogin() {
        showLoading("正在创建 Quick Connect...")
        executor.execute {
            try {
                val quickConnect = viewModel.workflowController.startQuickConnect()
                runOnUiThread {
                    showQuickConnect(quickConnect)
                    quickConnectPoller.start()
                }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
    }

    private fun showQuickConnect(quickConnect: QuickConnectSession) {
        val quickConnectViews = quickConnectScreen(
            quickConnect = quickConnect,
            onCheckNow = ::checkQuickConnectNow,
            onBackToLogin = ::showLogin,
        )
        quickConnectStatus = quickConnectViews.status
        setContentView(quickConnectViews.root)
    }

    private fun updateQuickConnectWaiting(attempts: Int) {
        quickConnectStatus?.text = "还没有授权，已自动检查 $attempts 次"
    }

    private fun checkQuickConnectNow() {
        quickConnectStatus?.text = "正在检查授权..."
        executor.execute {
            try {
                viewModel.workflowController.completeQuickConnect()
                runOnUiThread {
                    quickConnectPoller.stop()
                    rememberAccount()
                    showHome(viewModel.workflowController.state())
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    if (error.message == TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE) {
                        quickConnectStatus?.text = "还没有授权，请在 Jellyfin 中输入授权码后稍等"
                    } else {
                        showError(error)
                    }
                }
            }
        }
    }

    private fun showHome(state: TvAppState) {
        quickConnectPoller.stop()
        searchVisible = false
        var focusedCard: View? = null
        setContentView(homeScreen(
            state = state,
            navigation = HomeNavigation(
                canGoBack = viewModel.workflowController.canGoBackInBrowse(),
                canPageBackward = viewModel.workflowController.canPageBackwardInBrowse(),
                canPageForward = viewModel.workflowController.canPageForwardInBrowse(),
                onSearch = ::showSearch,
                onRefresh = {
                    runTask("正在重新加载首页...", {
                        viewModel.workflowController.loadHome()
                    }) {
                        showHome(viewModel.workflowController.state())
                    }
                },
                onLogout = {
                    runTask("正在退出登录...", {
                        forgetAuthenticatedAccount()
                        viewModel.workflowController.logout()
                    }) {
                        showServerEntry()
                    }
                },
                onBackInBrowse = {
                    showHome(viewModel.workflowController.back())
                },
                onPreviousPage = {
                    runTask("正在加载上一页...", {
                        viewModel.workflowController.previousBrowsePage()
                    }) {
                        showHome(viewModel.workflowController.state())
                    }
                },
                onNextPage = {
                    runTask("正在加载下一页...", {
                        viewModel.workflowController.nextBrowsePage()
                    }) {
                        showHome(viewModel.workflowController.state())
                    }
                },
            ),
            onOpen = ::openMediaItem,
            loadImage = ::loadPrimaryImage,
            onFocusedCard = { focusedCard = it },
        ))
        focusedCard?.post { focusedCard?.requestFocus() }
    }

    private fun showSearch() {
        searchVisible = true
        val searchInput = input("搜索媒体", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
        fun submitSearch() {
            val term = searchInput.text.toString().trim()
            if (term.isBlank()) {
                searchInput.requestFocus()
            } else {
                runTask("正在搜索...", {
                    viewModel.workflowController.search(term)
                }) {
                    showHome(viewModel.workflowController.state())
                }
            }
        }
        searchInput.imeOptions = EditorInfo.IME_ACTION_SEARCH
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                submitSearch()
                true
            } else {
                false
            }
        }
        setContentView(screen("搜索媒体") {
            addView(searchInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56),
            ).apply {
                bottomMargin = dp(16)
            })
            addView(actionStrip(listOf(
                compactIconAction("搜索", TvIcon.SEARCH, ::submitSearch),
                iconAction("返回", TvIcon.BACK) { showHome(viewModel.workflowController.state()) },
            )))
        })
        searchInput.post { searchInput.requestFocus() }
    }

    private fun showDetails(item: MediaItemSummary) {
        showDetails(item, null)
    }

    private fun showDetails(item: MediaItemSummary, playbackInfo: PlaybackInfo?) {
        if (playbackInfo != null && playbackInfo.itemId() == item.id()) {
            selectedPlaybackInfo = playbackInfo
        }
        val effectivePlaybackInfo = playbackInfo ?: selectedPlaybackInfo?.takeIf { it.itemId() == item.id() }
        val playbackActions = if (item.playable()) {
            buildPlaybackActions(item)
        } else {
            emptyList()
        }
        setContentView(detailsScreen(
            item = item,
            episodeLabel = episodeLabel(item),
            formatTicks = ::formatPlaybackPosition,
            playbackActions = playbackActions,
            technicalInfo = mediaTechnicalPills(effectivePlaybackInfo),
            folderAction = openFolderAction(item),
            loadPoster = ::addPosterIfAvailable,
            onBackHome = {
                viewModel.workflowController.back()
                showHome(viewModel.workflowController.state())
            },
        ))
    }

    private fun buildPlaybackActions(item: MediaItemSummary): List<View> {
        val actions = mutableListOf<View>()
        if (item.hasResumePosition()) {
            actions.add(playbackAction("继续播放", TvIcon.PLAY, null))
            actions.add(playbackAction("从头播放", TvIcon.PLAY, PlaybackSelectionPreferences.defaults()))
        } else {
            actions.add(playbackAction("播放", TvIcon.PLAY, null))
        }
        actions.add(playbackAction("低码率播放", TvIcon.SPEED, lowBitratePreferences(item)))
        actions.add(iconAction("音轨 / 字幕", TvIcon.SUBTITLES) { loadPlaybackOptions(item) })
        actions.add(iconAction("播放速度", TvIcon.SPEED) { showPlaybackSpeedOptions(item) })
        if (item.seriesId().isNotBlank()) {
            actions.add(iconAction("本剧下一集", TvIcon.PLAY) { openSeriesNextUp() })
        }
        return actions
    }

    private fun addPosterIfAvailable(container: LinearLayout, item: MediaItemSummary) {
        val poster = ImageView(this).apply {
            contentDescription = "${item.name()} 海报"
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackground(rounded(Color.rgb(30, 41, 59), dp(8)))
            adjustViewBounds = false
        }
        container.addView(poster, LinearLayout.LayoutParams(dp(TvSize.DetailPosterWidth), dp(TvSize.DetailPosterHeight)).apply {
            rightMargin = dp(28)
            bottomMargin = dp(20)
        })
        loadPrimaryImage(poster, item, 320, 480)
    }

    private fun loadPrimaryImage(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        primaryImageLoader.load(this, viewModel.workflowController.state().authenticated(), target, item, width, height)
    }

    private fun loadPlaybackOptions(item: MediaItemSummary) {
        showLoading("正在读取音轨和字幕...")
        executor.execute {
            try {
                val choices = viewModel.workflowController.loadPlaybackChoices(null)
                runOnUiThread { showPlaybackOptions(item, choices) }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
    }

    private fun showPlaybackOptions(item: MediaItemSummary, playbackInfo: PlaybackInfo) {
        if (playbackInfo.itemId() == item.id()) {
            selectedPlaybackInfo = playbackInfo
        }
        setContentView(playbackOptionsScreen(
            item = item,
            playbackInfo = playbackInfo,
            onDefault = { preparePlaybackWith(null) },
            onSource = { sourceId -> preparePlaybackWith(sourcePreferences(item, sourceId)) },
            onAudio = { sourceId, audioStreamIndex ->
                preparePlaybackWith(trackPreferences(item, sourceId, audioStreamIndex, null))
            },
            onDisableSubtitles = { sourceId ->
                preparePlaybackWith(trackPreferences(item, sourceId, null, -1))
            },
            onSubtitle = { sourceId, subtitleStreamIndex ->
                preparePlaybackWith(trackPreferences(item, sourceId, null, subtitleStreamIndex))
            },
            onBackDetails = { showDetails(item) },
        ))
    }

    private fun showPlaybackSpeedOptions(item: MediaItemSummary) {
        setContentView(playbackSpeedScreen(
            onSpeed = { rate -> preparePlaybackWith(speedPreferences(item, rate)) },
            onBackDetails = { showDetails(item) },
        ))
    }

    private fun showPlayerReady(state: TvAppState) {
        setContentView(playerReadyScreen(
            state = state,
            onOpenPlayer = { showPlayer(state) },
            onDiagnostics = { showDiagnostics(state) },
            onBackDetails = {
                viewModel.workflowController.back()
                viewModel.workflowController.state().selectedItem()?.let(::showDetails)
            },
        ))
    }

    private fun showDiagnostics(state: TvAppState, returnToPlayer: Boolean = false) {
        val diagnostics = TvDiagnostics.describe(state)
        setContentView(diagnosticsScreen(
            diagnostics = diagnostics,
            returnToPlayer = returnToPlayer,
            onExport = {
                val file = filesDir.resolve("cinepilot-diagnostics.txt")
                file.writeText(diagnostics)
                showDiagnosticsExported(state, file.absolutePath, returnToPlayer)
            },
            onShare = { shareDiagnostics(diagnostics) },
            onBackDiagnosticsTarget = { showDiagnosticsTarget(state, returnToPlayer) },
        ))
    }

    private fun showDiagnosticsExported(state: TvAppState, path: String, returnToPlayer: Boolean = false) {
        val diagnostics = TvDiagnostics.describe(state)
        setContentView(diagnosticsExportedScreen(
            path = path,
            returnToPlayer = returnToPlayer,
            onShare = { shareDiagnostics(diagnostics) },
            onBackDiagnostics = { showDiagnostics(state, returnToPlayer) },
            onBackDiagnosticsTarget = { showDiagnosticsTarget(state, returnToPlayer) },
        ))
    }

    private fun showDiagnosticsTarget(state: TvAppState, returnToPlayer: Boolean) {
        if (returnToPlayer) {
            showPlayer(state)
        } else {
            showPlayerReady(state)
        }
    }

    private fun shareDiagnostics(diagnostics: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CinePilot TV 诊断信息")
            putExtra(Intent.EXTRA_TEXT, diagnostics)
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, "分享诊断"))
        }.onFailure {
            Toast.makeText(this, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlayer(state: TvAppState) {
        lastPlaybackBackPressAt = 0L
        val playerView = playerHost.createPlayerView(
            state = state,
            onPlaybackError = { error ->
                runOnUiThread {
                    playerHost.release()
                    showError(error)
                }
            },
            onPlaybackEnded = {
                runOnUiThread {
                    if (viewModel.workflowController.state().route() != TvRoute.PLAYER) {
                        return@runOnUiThread
                    }
                    playerHost.release()
                    viewModel.workflowController.back()
                    viewModel.workflowController.state().selectedItem()?.let(::showDetails)
                        ?: showHome(viewModel.workflowController.state())
                }
            },
        )
        setContentView(playerScreen(
            playerView = playerView,
        ))
        playerView.post { playerView.requestFocus() }
    }

    private fun itemButton(row: HomeRow, item: MediaItemSummary): View {
        return action(item.name().ifBlank { item.id() }) {
            openMediaItem(row, item)
        }.also {
            it.contentDescription = "${row.title()} ${item.name()}"
        }
    }

    private fun openMediaItem(row: HomeRow, item: MediaItemSummary) {
        viewModel.workflowController.focusItem(row.id(), item.id())
        val loadingMessage = if (item.playable()) "正在打开详情..." else "正在打开目录..."
        var playbackInfo: PlaybackInfo? = null
        runTask(loadingMessage, {
            if (item.playable()) {
                viewModel.workflowController.openItem(item.id())
                playbackInfo = runCatching {
                    viewModel.workflowController.loadPlaybackChoices(null)
                }.getOrNull()
            } else {
                viewModel.workflowController.openFolder(item.id(), item.name())
            }
        }) {
            val state = viewModel.workflowController.state()
            if (item.playable()) {
                state.selectedItem()?.let { selectedItem -> showDetails(selectedItem, playbackInfo) }
            } else {
                showHome(state)
            }
        }
    }

    private fun openSeriesNextUp() {
        var playbackInfo: PlaybackInfo? = null
        runTask("正在打开本剧下一集...", {
            val nextItem = viewModel.workflowController.nextUpForSelectedSeries()
            viewModel.workflowController.openItem(nextItem.id())
            playbackInfo = runCatching {
                viewModel.workflowController.loadPlaybackChoices(null)
            }.getOrNull()
        }) {
            viewModel.workflowController.state().selectedItem()?.let { selectedItem ->
                showDetails(selectedItem, playbackInfo)
            } ?: showHome(viewModel.workflowController.state())
        }
    }

    private fun openFolderAction(item: MediaItemSummary): View {
        return action("打开子项目") {
            runTask("正在打开目录...", {
                viewModel.workflowController.openFolder(item.id(), item.name())
            }) {
                showHome(viewModel.workflowController.state())
            }
        }
    }

    private fun playbackAction(text: String, icon: TvIcon, preferences: PlaybackSelectionPreferences?): View {
        return iconAction(text, icon) {
            preparePlaybackWith(preferences)
        }
    }

    private fun preparePlaybackWith(preferences: PlaybackSelectionPreferences?) {
        runTask("正在准备播放...", {
            viewModel.workflowController.preparePlayback(preferences)
        }) {
            showPlayer(viewModel.workflowController.state())
        }
    }

    private fun trackPreferences(
        item: MediaItemSummary,
        mediaSourceId: String,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
    ): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences(startTimeTicks, audioStreamIndex, subtitleStreamIndex, null, 0, 0, 0)
            .withMediaSourceId(mediaSourceId)
    }

    private fun sourcePreferences(item: MediaItemSummary, mediaSourceId: String): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences(startTimeTicks, null, null, null, 0, 0, 0).withMediaSourceId(mediaSourceId)
    }

    private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
    }

    private fun speedPreferences(item: MediaItemSummary, rate: Float): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences(startTimeTicks, null, null, null, 0, 0, 0).withPlaybackRate(rate)
    }

    private fun showLoading(message: String) {
        setContentView(screen("CinePilot TV") {
            addView(label(message))
        })
    }

    private fun showError(error: Throwable) {
        val authenticationExpired = error is MediaBrowserException && error.statusCode() == 401
        if (authenticationExpired) {
            forgetAuthenticatedAccount()
            viewModel.workflowController.forgetAuthenticatedSession()
        } else {
            viewModel.workflowController.fail(error.message ?: error::class.java.simpleName)
        }
        val state = viewModel.workflowController.state()
        setContentView(screen("出错了") {
            val message = if (authenticationExpired) {
                "会话已过期，请重新登录"
            } else {
                tvErrorMessage(error)
            }
            addView(label(message))
            if (state.selectedItem() != null && !authenticationExpired) {
                addView(action("返回详情") { state.selectedItem()?.let(::showDetails) })
            }
            if (state.homeRows().isNotEmpty() && !authenticationExpired) {
                addView(action("返回首页") { showHome(state) })
            }
            if (state.server() != null) {
                addView(action("重新登录") { showLogin() })
            }
            addView(action("返回服务器输入") { showServerEntry() })
        })
    }

    private fun runTask(message: String, task: () -> Unit, onSuccess: () -> Unit) {
        showLoading(message)
        executor.execute {
            try {
                task()
                runOnUiThread { onSuccess() }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
    }

    private fun loadPublicUsersIfAvailable() {
        runCatching { viewModel.workflowController.loadPublicUsers() }
    }

    private fun rememberAccount() {
        val authenticated = viewModel.workflowController.state().authenticated() ?: return
        recentAccountStore.remember(authenticated)
    }

    private fun clearSavedAccounts() {
        recentAccountStore.clear()
    }

    private fun forgetAuthenticatedAccount() {
        val authenticated = viewModel.workflowController.state().authenticated() ?: return
        recentAccountStore.forget(authenticated)
    }

    companion object {
        private const val PLAYBACK_BACK_EXIT_WINDOW_MS = 2_000L
    }

}
