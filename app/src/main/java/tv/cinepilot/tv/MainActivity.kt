package tv.cinepilot.tv

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.PlaybackException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executors
import javax.net.ssl.SSLException
import tv.cinepilot.core.protocol.MediaTicks
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaSourceInfo
import tv.cinepilot.core.protocol.MediaStreamInfo
import tv.cinepilot.core.protocol.MediaStreamType
import tv.cinepilot.core.protocol.PlaybackInfo
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.core.protocol.ServerFlavor
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.player.Media3PlayerHost
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
import tv.cinepilot.tv.ui.rounded
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.section
import tv.cinepilot.tv.ui.HomeNavigation
import tv.cinepilot.tv.ui.TvIcon

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CinePilotViewModel
    private lateinit var playerHost: Media3PlayerHost
    private val executor = Executors.newSingleThreadExecutor()
    private val imageExecutor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastAccountStore by lazy { getSharedPreferences("cinepilot_last_account", MODE_PRIVATE) }
    @Volatile private var quickConnectPolling = false
    private var searchVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, CinePilotViewModel.factory(applicationContext))[CinePilotViewModel::class.java]
        playerHost = Media3PlayerHost(this, viewModel.mediaBrowserClient)
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
        stopQuickConnectPolling()
        playerHost.shutdown()
        imageExecutor.shutdownNow()
        executor.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android framework; retained for API 26 TV devices.")
    override fun onBackPressed() {
        if (searchVisible) {
            searchVisible = false
            showHome(viewModel.workflowController.state())
            return
        }
        when (viewModel.workflowController.state().route()) {
            TvRoute.SERVER_ENTRY -> super.onBackPressed()
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
                playerHost.release()
                viewModel.workflowController.back()
                viewModel.workflowController.state().selectedItem()?.let(::showDetails)
                    ?: showHome(viewModel.workflowController.state())
            }
        }
    }

    private fun showServerEntry() {
        stopQuickConnectPolling()
        val serverInput = input("https://your-server.example.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val recentAccounts = savedAccounts()
        setContentView(screen("CinePilot TV") {
            recentAccounts.forEach { account ->
                addView(action("继续 ${account.displayName()}") {
                    runTask("正在恢复上次登录...", {
                        viewModel.workflowController.submitServer(account.serverAddress)
                        viewModel.workflowController.restoreSession(account.userId)
                    }) {
                        showHome(viewModel.workflowController.state())
                    }
                })
            }
            if (recentAccounts.isNotEmpty()) {
                addView(action("清除已保存登录") {
                    clearSavedAccounts()
                    showServerEntry()
                })
            }
            addView(label("服务器地址"))
            addView(serverInput)
            addView(action("连接服务器") {
                runTask("正在连接服务器...", {
                    viewModel.workflowController.submitServer(serverInput.text.toString())
                    loadPublicUsersIfAvailable()
                }) {
                    showLogin()
                }
            })
        })
    }

    private fun restoreRecentAccountOnLaunch() {
        val account = savedAccounts().firstOrNull()
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
        stopQuickConnectPolling()
        val usernameInput = input("用户名", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
        val passwordInput = input("密码", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        setContentView(screen("登录 ${viewModel.workflowController.state().server()?.serverName() ?: ""}") {
            if (viewModel.workflowController.state().publicUsers().isNotEmpty()) {
                addView(section("选择用户"))
                viewModel.workflowController.state().publicUsers().forEach { user ->
                    addView(publicUserAction(user, usernameInput, passwordInput))
                }
            }
            addView(label("用户名"))
            addView(usernameInput)
            addView(label("密码"))
            addView(passwordInput)
            addView(action("登录") {
                loginWithCredentials(usernameInput.text.toString(), passwordInput.text.toString())
            })
            if (viewModel.workflowController.state().server()?.flavor() == ServerFlavor.JELLYFIN) {
                addView(action("Quick Connect") { startQuickConnectLogin() })
            }
            addView(action("返回服务器输入") { showServerEntry() })
        })
    }

    private fun publicUserAction(user: PublicUserSummary, usernameInput: EditText, passwordInput: EditText): View {
        val userName = user.name().ifBlank { user.id() }
        if (!user.passwordRequired()) {
            return action("免密码登录 $userName") {
                usernameInput.setText(userName)
                loginWithCredentials(userName, "")
            }
        }
        return action(userName) {
            usernameInput.setText(userName)
            passwordInput.requestFocus()
        }
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
                    scheduleQuickConnectPoll()
                }
            } catch (error: Throwable) {
                runOnUiThread { showError(error) }
            }
        }
    }

    private fun showQuickConnect(quickConnect: QuickConnectSession) {
        quickConnectPolling = true
        setContentView(screen("Quick Connect") {
            addView(label("授权码：${quickConnect.code()}"))
            addView(label("请在 Jellyfin 中输入授权码，授权后会自动登录"))
            addView(action("完成登录") {
                runTask("正在完成 Quick Connect 登录...", {
                    viewModel.workflowController.completeQuickConnect()
                }) {
                    rememberAccount()
                    showHome(viewModel.workflowController.state())
                }
            })
            addView(action("返回登录") { showLogin() })
        })
    }

    private fun scheduleQuickConnectPoll() {
        if (!quickConnectPolling) {
            return
        }
        mainHandler.postDelayed({
            if (quickConnectPolling) {
                pollQuickConnectApproval()
            }
        }, 2_000L)
    }

    private fun pollQuickConnectApproval() {
        executor.execute {
            try {
                viewModel.workflowController.completeQuickConnect()
                runOnUiThread {
                    stopQuickConnectPolling()
                    rememberAccount()
                    showHome(viewModel.workflowController.state())
                }
            } catch (error: Throwable) {
                if (error.message == TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE) {
                    runOnUiThread { scheduleQuickConnectPoll() }
                } else {
                    runOnUiThread {
                        stopQuickConnectPolling()
                        showError(error)
                    }
                }
            }
        }
    }

    private fun stopQuickConnectPolling() {
        quickConnectPolling = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun showHome(state: TvAppState) {
        stopQuickConnectPolling()
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
        setContentView(screen("搜索媒体") {
            addView(searchInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56),
            ).apply {
                bottomMargin = dp(16)
            })
            addView(actionStrip(listOf(
                compactIconAction("搜索", TvIcon.SEARCH) {
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
                },
                iconAction("返回", TvIcon.BACK) { showHome(viewModel.workflowController.state()) },
            )))
        })
        searchInput.post { searchInput.requestFocus() }
    }

    private fun showDetails(item: MediaItemSummary) {
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
        return actions
    }

    private fun addPosterIfAvailable(container: LinearLayout, item: MediaItemSummary) {
        val poster = ImageView(this).apply {
            contentDescription = "${item.name()} 海报"
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackground(rounded(Color.rgb(30, 41, 59), dp(8)))
            adjustViewBounds = false
        }
        container.addView(poster, LinearLayout.LayoutParams(dp(220), dp(330)).apply {
            rightMargin = dp(28)
            bottomMargin = dp(20)
        })
        loadPrimaryImage(poster, item, 320, 480)
    }

    private fun loadPrimaryImage(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        val authenticated = viewModel.workflowController.state().authenticated() ?: return
        if (item.imageTags()["Primary"].isNullOrBlank()) {
            return
        }
        imageExecutor.execute {
            runCatching {
                val imageUrl = viewModel.mediaBrowserClient.primaryImageUrl(authenticated, item, width, height)
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()?.let { bitmap ->
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        target.setImageBitmap(bitmap)
                    }
                }
            }
        }
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
        val streams = playbackInfo.mediaSources()
            .flatMap { source -> source.mediaStreams() }
            .distinctBy { stream -> "${stream.type()}:${stream.index()}" }
        val audioStreams = streams.filter { stream -> stream.type() == MediaStreamType.AUDIO }
        val subtitleStreams = streams.filter { stream -> stream.type() == MediaStreamType.SUBTITLE }
        setContentView(screen("音轨 / 字幕") {
            addView(action("按服务器默认播放") {
                preparePlaybackWith(null)
            })
            if (playbackInfo.mediaSources().size > 1) {
                addView(section("媒体源"))
                playbackInfo.mediaSources().forEach { source ->
                    addView(action(sourceLabel(source)) {
                        preparePlaybackWith(sourcePreferences(item, source.id()))
                    })
                }
            }
            addView(section("音轨"))
            if (audioStreams.isEmpty()) {
                addView(label("服务器未返回可选音轨"))
            } else {
                audioStreams.forEach { stream ->
                    addView(action("音轨 ${stream.index()}：${streamLabel(stream)}") {
                        preparePlaybackWith(trackPreferences(item, stream.index(), null))
                    })
                }
            }
            addView(section("字幕"))
            addView(action("关闭字幕播放") {
                preparePlaybackWith(trackPreferences(item, null, -1))
            })
            if (subtitleStreams.isEmpty()) {
                addView(label("服务器未返回可选字幕"))
            } else {
                subtitleStreams.forEach { stream ->
                    addView(action("字幕 ${stream.index()}：${streamLabel(stream)}") {
                        preparePlaybackWith(trackPreferences(item, null, stream.index()))
                    })
                }
            }
            addView(action("返回详情") { showDetails(item) })
        })
    }

    private fun showPlayerReady(state: TvAppState) {
        val playable = state.playableMedia()
        setContentView(screen("准备播放") {
            addView(label("播放方式：${playable?.playMethod() ?: ""}"))
            addView(label("媒体源：${playable?.mediaSourceId() ?: ""}"))
            addView(label("播放地址已准备"))
            addView(iconAction("打开播放器", TvIcon.PLAY) { showPlayer(state) })
            addView(action("诊断信息") { showDiagnostics(state) })
            addView(iconAction("返回详情", TvIcon.BACK) {
                viewModel.workflowController.back()
                viewModel.workflowController.state().selectedItem()?.let(::showDetails)
            })
        })
    }

    private fun showDiagnostics(state: TvAppState) {
        val diagnostics = TvDiagnostics.describe(state)
        setContentView(screen("诊断信息") {
            addView(label(diagnostics))
            addView(action("导出诊断") {
                val file = filesDir.resolve("cinepilot-diagnostics.txt")
                file.writeText(diagnostics)
                showDiagnosticsExported(state, file.absolutePath)
            })
            addView(iconAction("返回播放准备", TvIcon.BACK) { showPlayerReady(state) })
        })
    }

    private fun showDiagnosticsExported(state: TvAppState, path: String) {
        setContentView(screen("诊断信息") {
            addView(label("诊断已导出：$path"))
            addView(iconAction("返回诊断信息", TvIcon.BACK) { showDiagnostics(state) })
            addView(iconAction("返回播放准备", TvIcon.BACK) { showPlayerReady(state) })
        })
    }

    private fun showPlayer(state: TvAppState) {
        var playerView: View? = null
        setContentView(screen("播放器") {
            playerView = playerHost.createPlayerView(state) { error ->
                runOnUiThread {
                    playerHost.release()
                    showError(error)
                }
            }
            addView(playerView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                720,
            ))
            addView(iconAction("停止并返回详情", TvIcon.BACK) {
                playerHost.release()
                viewModel.workflowController.back()
                viewModel.workflowController.state().selectedItem()?.let(::showDetails)
            })
        })
        playerView?.post { playerView?.requestFocus() }
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
        runTask(loadingMessage, {
            if (item.playable()) {
                viewModel.workflowController.openItem(item.id())
            } else {
                viewModel.workflowController.openFolder(item.id(), item.name())
            }
        }) {
            val state = viewModel.workflowController.state()
            if (item.playable()) {
                state.selectedItem()?.let(::showDetails)
            } else {
                showHome(state)
            }
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
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
    ): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences(startTimeTicks, audioStreamIndex, subtitleStreamIndex, null, 0, 0, 0)
    }

    private fun sourcePreferences(item: MediaItemSummary, mediaSourceId: String): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences(startTimeTicks, null, null, null, 0, 0, 0).withMediaSourceId(mediaSourceId)
    }

    private fun sourceLabel(source: MediaSourceInfo): String {
        val parts = mutableListOf<String>()
        if (source.name().isNotBlank()) {
            parts.add(source.name())
        } else if (source.path().isNotBlank()) {
            parts.add(source.path().substringAfterLast('/').substringAfterLast('\\'))
        } else if (source.container().isNotBlank()) {
            parts.add(source.container().uppercase())
        } else {
            parts.add(source.id())
        }
        if (source.container().isNotBlank()) {
            parts.add(source.container().uppercase())
        }
        if (source.bitRate() > 0) {
            parts.add("${source.bitRate() / 1_000_000} Mbps")
        }
        return "媒体源：${parts.distinct().joinToString(" · ")}"
    }

    private fun streamLabel(stream: MediaStreamInfo): String {
        val parts = mutableListOf<String>()
        if (stream.displayTitle().isNotBlank()) {
            parts.add(stream.displayTitle())
        } else {
            if (stream.language().isNotBlank()) {
                parts.add(stream.language())
            }
            if (stream.codec().isNotBlank()) {
                parts.add(stream.codec())
            }
        }
        if (stream.defaultStream()) {
            parts.add("默认")
        }
        if (stream.forced()) {
            parts.add("强制")
        }
        if (stream.external()) {
            parts.add("外挂")
        }
        return parts.ifEmpty { listOf("未命名") }.joinToString(" · ")
    }

    private fun episodeLabel(item: MediaItemSummary): String {
        val seriesName = item.seriesName().ifBlank { "" }
        val season = item.parentIndexNumber()
        val episode = item.indexNumber()
        val parts = mutableListOf<String>()
        if (seriesName.isNotBlank()) {
            parts.add(seriesName)
        }
        if (season != null) {
            parts.add("第 ${season} 季")
        }
        if (episode != null) {
            parts.add("第 ${episode} 集")
        }
        return parts.joinToString(" · ")
    }

    private fun lowBitratePreferences(item: MediaItemSummary): PlaybackSelectionPreferences {
        val startTimeTicks = if (item.hasResumePosition()) item.userData().playbackPositionTicks() else 0L
        return PlaybackSelectionPreferences.lowBitrate(startTimeTicks)
    }

    private fun formatPlaybackPosition(ticks: Long): String {
        val totalSeconds = MediaTicks.toMilliseconds(ticks) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
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
                displayErrorMessage(error)
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

    private fun displayErrorMessage(error: Throwable): String {
        if (error is MediaBrowserException && error.statusCode() > 0) {
            return when (error.statusCode()) {
                401 -> "会话已过期，请重新登录"
                403 -> "服务器拒绝访问，请确认账号权限"
                404 -> "服务器没有找到请求的资源，请确认服务器地址和媒体库是否正确"
                in 500..599 -> "服务器暂时无法处理请求，请稍后重试或检查 Jellyfin / Emby 服务状态"
                else -> "服务器请求失败，HTTP ${error.statusCode()}"
            }
        }
        val cause = rootCause(error)
        if (error is PlaybackException || cause is PlaybackException) {
            val playbackError = (error as? PlaybackException) ?: (cause as PlaybackException)
            return "播放器无法打开媒体，请尝试低码率播放、切换音轨 / 字幕，或检查服务器转码设置（${playbackError.errorCodeName}）"
        }
        when (cause) {
            is UnknownHostException -> return "无法解析服务器地址，请检查主机名、端口或网络 DNS"
            is ConnectException -> return "无法连接到服务器，请确认地址、端口和 Jellyfin / Emby 服务已启动"
            is SocketTimeoutException -> return "连接服务器超时，请检查网络或稍后重试"
            is SSLException -> return "HTTPS 连接失败，请检查服务器证书；家庭服务器也可以先使用 http:// 地址测试"
        }
        return when (error.message) {
            "Server address is required" -> "请输入服务器地址"
            "Only HTTP and HTTPS server addresses are supported" -> "服务器地址只支持 http:// 或 https://"
            "Server address must include a host" -> "服务器地址需要包含主机名或 IP"
            TvWorkflowController.NO_CHILD_ITEM_MESSAGE -> "目录中没有可打开的媒体"
            TvWorkflowController.NO_PLAYABLE_SOURCE_MESSAGE -> "没有可用播放源"
            TvWorkflowController.QUICK_CONNECT_DISABLED_MESSAGE -> "服务器未启用 Quick Connect"
            TvWorkflowController.QUICK_CONNECT_NOT_APPROVED_MESSAGE -> "Quick Connect 还没有完成授权"
            else -> error.message ?: error::class.java.simpleName
        }
    }

    private fun rootCause(error: Throwable): Throwable {
        var current = error
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
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
        val account = LastAccount(
            authenticated.server().address().value(),
            authenticated.server().serverName(),
            authenticated.session().userId(),
        )
        val accounts = (listOf(account) + savedAccounts().filterNot {
            it.serverAddress == account.serverAddress && it.userId == account.userId
        }).take(5)
        lastAccountStore.edit()
            .putString("recent_accounts", accounts.joinToString("\n") { it.serialize() })
            .putString("server_address", account.serverAddress)
            .putString("server_name", account.serverName)
            .putString("user_id", account.userId)
            .apply()
    }

    private fun savedAccounts(): List<LastAccount> {
        val recentAccounts = lastAccountStore.getString("recent_accounts", null)
            ?.lineSequence()
            ?.mapNotNull(LastAccount::deserialize)
            ?.toList()
            .orEmpty()
        if (recentAccounts.isNotEmpty()) {
            return recentAccounts
        }
        return savedLegacyAccount()?.let(::listOf).orEmpty()
    }

    private fun savedLegacyAccount(): LastAccount? {
        val serverAddress = lastAccountStore.getString("server_address", null)?.takeIf { it.isNotBlank() }
        val userId = lastAccountStore.getString("user_id", null)?.takeIf { it.isNotBlank() }
        if (serverAddress == null || userId == null) {
            return null
        }
        return LastAccount(
            serverAddress,
            lastAccountStore.getString("server_name", null).orEmpty(),
            userId,
        )
    }

    private fun clearSavedAccounts() {
        lastAccountStore.edit().clear().apply()
    }

    private fun forgetAuthenticatedAccount() {
        val authenticated = viewModel.workflowController.state().authenticated() ?: return
        val account = LastAccount(
            authenticated.server().address().value(),
            authenticated.server().serverName(),
            authenticated.session().userId(),
        )
        val accounts = savedAccounts().filterNot {
            it.serverAddress == account.serverAddress && it.userId == account.userId
        }
        lastAccountStore.edit()
            .putString("recent_accounts", accounts.joinToString("\n") { it.serialize() })
            .remove("server_address")
            .remove("server_name")
            .remove("user_id")
            .apply()
    }

    private data class LastAccount(
        val serverAddress: String,
        val serverName: String,
        val userId: String,
    ) {
        fun displayName(): String {
            val serverLabel = serverName.ifBlank { serverAddress }
            return "$serverLabel / $userId"
        }

        fun serialize(): String {
            return listOf(serverAddress, serverName, userId).joinToString("\t", transform = ::safeField)
        }

        companion object {
            fun deserialize(value: String): LastAccount? {
                val fields = value.split('\t')
                if (fields.size != 3 || fields[0].isBlank() || fields[2].isBlank()) {
                    return null
                }
                return LastAccount(fields[0], fields[1], fields[2])
            }

            private fun safeField(value: String): String {
                return value.replace('\t', ' ').replace('\n', ' ').trim()
            }
        }
    }
}
