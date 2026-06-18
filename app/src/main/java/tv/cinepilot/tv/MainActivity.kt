package tv.cinepilot.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.tv.auth.AuthRouteController
import tv.cinepilot.tv.compose.CinePilotScreenHost
import tv.cinepilot.tv.compose.screens.ComposeErrorScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.deeplink.DeepLinkRouter
import tv.cinepilot.tv.home.HomeRouteController
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.home.SearchRouteController
import tv.cinepilot.tv.offline.DownloadCoordinator
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.playback.PlaybackRouteController
import tv.cinepilot.tv.playback.SubtitleStyleStore
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.profile.ProfileSwitcherRouteController
import tv.cinepilot.tv.runtime.ArtworkLoader
import tv.cinepilot.tv.runtime.ArtworkRequestFactory
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.runtime.CinePilotTaskRunner
import tv.cinepilot.tv.runtime.HomeEntryFlow
import tv.cinepilot.tv.settings.SettingsRouteController
import tv.cinepilot.tv.settings.SettingsStore
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.tvErrorMessage

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CinePilotViewModel
    private lateinit var playerHost: Media3PlayerHost
    private lateinit var authRoutes: AuthRouteController
    private lateinit var playbackRoutes: PlaybackRouteController
    private lateinit var searchRoutes: SearchRouteController
    private lateinit var settingsRoutes: SettingsRouteController
    private lateinit var profileSwitcherRoutes: ProfileSwitcherRouteController
    private lateinit var homeRoutes: HomeRouteController
    private lateinit var artworkLoader: ArtworkLoader
    private lateinit var artworkFactory: ArtworkRequestFactory
    private lateinit var subtitleStyleStore: SubtitleStyleStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var homeSettingsStore: HomeSettingsStore
    private lateinit var homeEntryFlow: HomeEntryFlow
    private lateinit var pluginHost: PluginHost
    private lateinit var downloadCoordinator: DownloadCoordinator
    private lateinit var deepLinkRouter: DeepLinkRouter
    private lateinit var screenHost: CinePilotScreenHost
    private lateinit var taskRunner: CinePilotTaskRunner
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var accountSwitcherReturnState: TvAppState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)
        homeSettingsStore = HomeSettingsStore(this)
        TvColors.applyTheme(settingsStore.theme().id)
        screenHost = CinePilotScreenHost(this, settingsStore.stateFlow)
        screenHost.install()
        taskRunner = CinePilotTaskRunner(
            executor = executor,
            postToMain = { action -> runOnUiThread(action) },
            showBlocking = ::showLoading,
            beginBusy = screenHost::beginBusy,
            showBlockingError = ::showError,
            showInPlaceError = ::showInPlaceError,
        )
        viewModel = ViewModelProvider(this, CinePilotViewModel.factory(applicationContext))[CinePilotViewModel::class.java]
        pluginHost = PluginHost.create(this)
        downloadCoordinator = DownloadCoordinator.getInstance(
            applicationContext,
            viewModel.runtime.offlineRepository,
            viewModel.runtime.streamingOkHttpClient,
        )
        subtitleStyleStore = SubtitleStyleStore(this)
        playerHost = Media3PlayerHost(
            this,
            viewModel.mediaBrowserClient,
            subtitleStyleStore,
            viewModel.runtime.streamingOkHttpClient,
        )
        artworkLoader = ArtworkLoader(
            viewModel.mediaBrowserClient,
            viewModel.runtime.bitmapCache,
        )
        artworkFactory = ArtworkRequestFactory(viewModel.mediaBrowserClient)
        homeEntryFlow = HomeEntryFlow(
            activity = this,
            executor = executor,
            workflowController = viewModel.workflowController,
            homeRowsCache = viewModel.runtime.homeRowsCache,
            homeSettingsStore = homeSettingsStore,
            mediaBrowserClient = viewModel.mediaBrowserClient,
            offlineRepository = viewModel.runtime.offlineRepository,
            showLoading = ::showLoading,
            showHome = ::showHome,
            showError = ::showError,
        )
        // 桌面媒体通道 / HomeChannel 的首次调度涉及 JobScheduler /
        // AlarmManager 的 IPC，抛到后台单线程执行避免阻塞首帧
        executor.execute {
            tv.cinepilot.tv.home.channel.HomeChannelInitializeReceiver.ensureScheduled(this@MainActivity)
        }
        authRoutes = AuthRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            mainHandler = mainHandler,
            executor = executor,
            runTask = ::runInPlaceTask,
            showHome = ::showHome,
            showError = ::showError,
            renderCompose = ::renderCompose,
            runHomeEntry = { msg, preload, remember, fallback -> homeEntryFlow.run(msg, preload, remember, fallback) },
            persistHomeCache = homeEntryFlow::persistCurrentHome,
            clearHomeCache = homeEntryFlow::clearCurrentHomeCache,
        )
        playbackRoutes = PlaybackRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            playerHost = playerHost,
            subtitleStyleStore = subtitleStyleStore,
            deviceCodecDiagnostics = viewModel.deviceCodecDiagnostics,
            executor = executor,
            runTask = ::runInPlaceTask,
            runSilentTask = ::runSilentTask,
            showHome = ::showHome,
            renderView = ::renderView,
            renderCompose = ::renderCompose,
            renderComposeFull = ::renderComposeFull,
            showError = ::showError,
            loadPosterImage = ::loadPosterImage,
            loadArtworkImage = ::loadArtworkImage,
            loadBackdropImage = ::loadBackdropImage,
            loadPersonImage = ::loadPersonImage,
            artworkFactory = artworkFactory,
            pluginHost = pluginHost,
            downloadCoordinator = downloadCoordinator,
        )
        deepLinkRouter = DeepLinkRouter(
            workflowController = viewModel.workflowController,
            runTask = ::runInPlaceTask,
            showDetails = playbackRoutes::showDetails,
            showHome = ::showHome,
        )
        searchRoutes = SearchRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            runTask = ::runInPlaceTask,
            showHome = ::showHome,
            renderView = ::renderView,
            renderCompose = ::renderCompose,
        )
        settingsRoutes = SettingsRouteController(
            activity = this,
            settingsStore = settingsStore,
            homeSettingsStore = homeSettingsStore,
            pluginHost = pluginHost,
            showHome = ::showHome,
            renderView = ::renderView,
            renderCompose = ::renderCompose,
            renderComposeFull = ::renderComposeFull,
            runSilentTask = ::runSilentTask,
        )
        profileSwitcherRoutes = ProfileSwitcherRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            homeSettingsStore = homeSettingsStore,
            artworkFactory = artworkFactory,
            runTask = ::runInPlaceTask,
            runSilentTask = ::runSilentTask,
            showHome = ::showHome,
            showServerEntry = authRoutes::showServerEntry,
            renderCompose = ::renderCompose,
        )
        homeRoutes = HomeRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            playbackRoutes = playbackRoutes,
            homeSettingsStore = homeSettingsStore,
            mediaBrowserClient = viewModel.mediaBrowserClient,
            executor = executor,
            runTask = ::runInPlaceTask,
            showHome = ::showHome,
            renderComposeFull = ::renderComposeFull,
            showAccountSwitcher = ::showAccountSwitcher,
            showSettings = settingsRoutes::show,
            showSearch = { term ->
                accountSwitcherReturnState = null
                searchRoutes.showSearch(term)
            },
            logoutFromHome = authRoutes::logoutFromHome,
            artworkFactory = artworkFactory,
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
        if (authRoutes.handleQaLoginIntent(intent)) return
        authRoutes.restoreRecentAccountOnLaunch()
        deepLinkRouter.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (authRoutes.handleQaLoginIntent(intent)) return
        deepLinkRouter.handleIntent(intent)
    }

    override fun onDestroy() {
        authRoutes.stopQuickConnectPolling()
        playerHost.shutdown()
        artworkLoader.shutdown()
        pluginHost.shutdown()
        downloadCoordinator.saveNow()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun handleBackPressed() {
        if (playbackRoutes.handleAuxiliaryBackPressed()) return
        if (searchRoutes.closeIfVisible()) return
        if (settingsRoutes.closeIfVisible()) return
        if (profileSwitcherRoutes.closeIfVisible()) return
        accountSwitcherReturnState?.let { returnState ->
            if (viewModel.workflowController.state().route() == TvRoute.HOME) {
                accountSwitcherReturnState = null
                showHome(returnState)
                return
            }
        }
        when (viewModel.workflowController.state().route()) {
            TvRoute.SERVER_ENTRY -> accountSwitcherReturnState?.let { returnState ->
                accountSwitcherReturnState = null
                showHome(returnState)
            } ?: finish()
            TvRoute.HOME -> {
                val state = viewModel.workflowController.back()
                if (state.route() == TvRoute.HOME) showHome(state) else authRoutes.showServerEntry()
            }
            TvRoute.LOGIN, TvRoute.ERROR -> {
                val state = viewModel.workflowController.back()
                if (state.route() == TvRoute.HOME) showHome(state) else authRoutes.showServerEntry()
            }
            TvRoute.DETAILS -> {
                viewModel.workflowController.back()
                showHome(viewModel.workflowController.state())
            }
            TvRoute.PLAYER -> playbackRoutes.handlePlaybackBackPressed()
            TvRoute.PROFILE_SWITCHER -> profileSwitcherRoutes.closeIfVisible()
        }
    }

    private fun showHome(state: TvAppState) {
        authRoutes.stopQuickConnectPolling()
        searchRoutes.hide()
        settingsRoutes.hide()
        accountSwitcherReturnState = null
        homeRoutes.render(state)
        // Replay any queued launcher deep link (e.g. cold start from a
        // preview program) now that an authenticated session is available.
        deepLinkRouter.replayPending()
    }

    private fun showAccountSwitcher() {
        searchRoutes.hide()
        settingsRoutes.hide()
        accountSwitcherReturnState = viewModel.workflowController.state()
        profileSwitcherRoutes.show(viewModel.workflowController.state())
    }

    private fun loadPosterImage(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        artworkLoader.loadPoster(this, viewModel.workflowController.state().authenticated(), target, item, width, height)
    }

    private fun loadArtworkImage(target: ImageView, item: MediaItemSummary, targetType: ArtworkTarget, width: Int, height: Int) {
        artworkLoader.loadArtwork(this, viewModel.workflowController.state().authenticated(), target, item, targetType, width, height)
    }

    private fun loadBackdropImage(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        artworkLoader.loadBackdrop(this, viewModel.workflowController.state().authenticated(), target, item, width, height)
    }

    private fun loadPersonImage(target: ImageView, person: MediaPerson, width: Int, height: Int) {
        artworkLoader.loadPerson(this, viewModel.workflowController.state().authenticated(), target, person, width, height)
    }

    private fun renderView(view: View) = screenHost.showLegacy(view)

    private fun renderCompose(title: String, content: @Composable (CinePilotPalette) -> Unit) =
        screenHost.showCompose(title, content)

    private fun renderComposeFull(content: @Composable (CinePilotPalette) -> Unit) = screenHost.showFullScreen(content)

    private fun showLoading(message: String) = screenHost.showLoading(message)

    private fun showError(error: Throwable) {
        val authenticationExpired = error is MediaBrowserException && error.statusCode() == 401
        if (authenticationExpired) {
            authRoutes.forgetAuthenticatedAccount()
            viewModel.workflowController.forgetAuthenticatedSession()
        } else {
            viewModel.workflowController.fail(error.message ?: error::class.java.simpleName)
        }
        val state = viewModel.workflowController.state()
        val message = if (authenticationExpired) "会话已过期，请重新登录" else tvErrorMessage(error)
        renderCompose(if (authenticationExpired) "重新登录" else "错误恢复") { palette ->
            ComposeErrorScreen(
                palette = palette,
                state = state,
                message = message,
                authenticationExpired = authenticationExpired,
                onReturnDetails = { state.selectedItem()?.let(playbackRoutes::showDetails) },
                onRetryLowBitrate = { playbackRoutes.retryLowBitrateFromError(state) },
                onPlaybackOptions = { playbackRoutes.showPlaybackOptionsFromError(state) },
                onDiagnostics = { playbackRoutes.showDiagnosticsFromError(state) { showError(error) } },
                onHome = { showHome(state) },
                onLogin = authRoutes::showLogin,
                onServerEntry = authRoutes::showServerEntry,
            )
        }
    }

    private fun showInPlaceError(error: Throwable) {
        val authenticationExpired = error is MediaBrowserException && error.statusCode() == 401
        if (authenticationExpired) {
            showError(error)
            return
        }
        Toast.makeText(this, tvErrorMessage(error), Toast.LENGTH_LONG).show()
    }

    private fun runBlockingTask(message: String, task: () -> Unit, onSuccess: () -> Unit) =
        taskRunner.runBlockingTask(message, task, onSuccess)

    private fun runInPlaceTask(message: String, task: () -> Unit, onSuccess: () -> Unit) =
        taskRunner.runInPlaceTask(message, task, onSuccess)

    private fun runSilentTask(
        task: () -> Unit,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = ::showError,
    ) = taskRunner.runSilentTask(task, onSuccess, onError)
}
