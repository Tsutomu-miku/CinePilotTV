package tv.cinepilot.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.MediaPerson
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.tv.auth.AuthRouteController
import tv.cinepilot.tv.error.errorRouteScreen
import tv.cinepilot.tv.home.HomeRouteController
import tv.cinepilot.tv.home.SearchRouteController
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.playback.PlaybackRouteController
import tv.cinepilot.tv.playback.SubtitleStyleStore
import tv.cinepilot.tv.runtime.ArtworkLoader
import tv.cinepilot.tv.runtime.ArtworkTarget
import tv.cinepilot.tv.runtime.HomeEntryFlow
import tv.cinepilot.tv.runtime.PrimaryImageLoader
import tv.cinepilot.tv.settings.SettingsRouteController
import tv.cinepilot.tv.settings.SettingsStore
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.tvErrorMessage

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CinePilotViewModel
    private lateinit var playerHost: Media3PlayerHost
    private lateinit var authRoutes: AuthRouteController
    private lateinit var playbackRoutes: PlaybackRouteController
    private lateinit var searchRoutes: SearchRouteController
    private lateinit var settingsRoutes: SettingsRouteController
    private lateinit var homeRoutes: HomeRouteController
    private lateinit var primaryImageLoader: PrimaryImageLoader
    private lateinit var artworkLoader: ArtworkLoader
    private lateinit var subtitleStyleStore: SubtitleStyleStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var homeEntryFlow: HomeEntryFlow
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var accountSwitcherReturnState: TvAppState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)
        TvColors.applyTheme(settingsStore.theme().id)
        viewModel = ViewModelProvider(this, CinePilotViewModel.factory(applicationContext))[CinePilotViewModel::class.java]
        subtitleStyleStore = SubtitleStyleStore(this)
        playerHost = Media3PlayerHost(this, viewModel.mediaBrowserClient, subtitleStyleStore)
        primaryImageLoader = PrimaryImageLoader(
            viewModel.mediaBrowserClient,
            viewModel.runtime.bitmapCache,
        )
        artworkLoader = ArtworkLoader(
            viewModel.mediaBrowserClient,
            viewModel.runtime.bitmapCache,
        )
        homeEntryFlow = HomeEntryFlow(
            activity = this,
            executor = executor,
            workflowController = viewModel.workflowController,
            homeRowsCache = viewModel.runtime.homeRowsCache,
            showLoading = ::showLoading,
            showHome = ::showHome,
            showError = ::showError,
        )
        authRoutes = AuthRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            mainHandler = mainHandler,
            executor = executor,
            runTask = ::runTask,
            showLoading = ::showLoading,
            showHome = ::showHome,
            showError = ::showError,
            loadPublicUserImage = ::loadPublicUserImage,
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
            runTask = ::runTask,
            showHome = ::showHome,
            showError = ::showError,
            loadPosterImage = ::loadPosterImage,
            loadArtworkImage = ::loadArtworkImage,
            loadBackdropImage = ::loadBackdropImage,
            loadPersonImage = ::loadPersonImage,
        )
        searchRoutes = SearchRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            runTask = ::runTask,
            showHome = ::showHome,
        )
        settingsRoutes = SettingsRouteController(
            activity = this,
            settingsStore = settingsStore,
            showHome = ::showHome,
        )
        homeRoutes = HomeRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            playbackRoutes = playbackRoutes,
            runTask = ::runTask,
            showHome = ::showHome,
            showAccountSwitcher = ::showAccountSwitcher,
            showSettings = settingsRoutes::show,
            showSearch = { term ->
                accountSwitcherReturnState = null
                searchRoutes.showSearch(term)
            },
            logoutFromHome = authRoutes::logoutFromHome,
            loadArtwork = ::loadArtworkImage,
            loadBackdrop = ::loadBackdropImage,
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
        if (authRoutes.handleQaLoginIntent(intent)) {
            return
        }
        authRoutes.restoreRecentAccountOnLaunch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        authRoutes.handleQaLoginIntent(intent)
    }

    override fun onDestroy() {
        authRoutes.stopQuickConnectPolling()
        playerHost.shutdown()
        artworkLoader.shutdown()
        primaryImageLoader.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun handleBackPressed() {
        if (playbackRoutes.handleAuxiliaryBackPressed()) return
        if (searchRoutes.closeIfVisible()) return
        if (settingsRoutes.closeIfVisible()) return
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
        }
    }

    private fun showHome(state: TvAppState) {
        authRoutes.stopQuickConnectPolling()
        searchRoutes.hide()
        settingsRoutes.hide()
        accountSwitcherReturnState = null
        homeRoutes.render(state)
    }

    private fun showAccountSwitcher() {
        searchRoutes.hide()
        accountSwitcherReturnState = viewModel.workflowController.state()
        authRoutes.showServerEntry()
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

    private fun loadPublicUserImage(target: ImageView, user: PublicUserSummary, width: Int, height: Int) {
        primaryImageLoader.loadPublicUser(this, viewModel.workflowController.state().server(), target, user, width, height)
    }

    private fun showLoading(message: String) {
        setContentView(screen("CinePilot TV") {
            addView(label(message))
        })
    }

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
        setContentView(errorRouteScreen(
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
        ))
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
}
