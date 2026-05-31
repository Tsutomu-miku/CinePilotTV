package tv.cinepilot.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.tv.auth.AuthRouteController
import tv.cinepilot.tv.error.errorRouteScreen
import tv.cinepilot.tv.home.homeRouteScreen
import tv.cinepilot.tv.home.searchScreen
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.playback.PlaybackRouteController
import tv.cinepilot.tv.runtime.PrimaryImageLoader
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.screen
import tv.cinepilot.tv.ui.tvErrorMessage

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CinePilotViewModel
    private lateinit var playerHost: Media3PlayerHost
    private lateinit var authRoutes: AuthRouteController
    private lateinit var playbackRoutes: PlaybackRouteController
    private lateinit var primaryImageLoader: PrimaryImageLoader
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var searchVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, CinePilotViewModel.factory(applicationContext))[CinePilotViewModel::class.java]
        playerHost = Media3PlayerHost(this, viewModel.mediaBrowserClient)
        primaryImageLoader = PrimaryImageLoader(viewModel.mediaBrowserClient)
        authRoutes = AuthRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            mainHandler = mainHandler,
            executor = executor,
            runTask = ::runTask,
            showLoading = ::showLoading,
            showHome = ::showHome,
            showError = ::showError,
        )
        playbackRoutes = PlaybackRouteController(
            activity = this,
            workflowController = viewModel.workflowController,
            playerHost = playerHost,
            runTask = ::runTask,
            showHome = ::showHome,
            showError = ::showError,
            loadPosterImage = ::loadPrimaryImage,
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
                    authRoutes.showServerEntry()
                }
            }
            TvRoute.LOGIN, TvRoute.ERROR -> {
                val state = viewModel.workflowController.back()
                if (state.route() == TvRoute.HOME) {
                    showHome(state)
                } else {
                    authRoutes.showServerEntry()
                }
            }
            TvRoute.DETAILS -> {
                viewModel.workflowController.back()
                showHome(viewModel.workflowController.state())
            }
            TvRoute.PLAYER -> {
                playbackRoutes.handlePlaybackBackPressed()
            }
        }
    }

    private fun showHome(state: TvAppState) {
        authRoutes.stopQuickConnectPolling()
        searchVisible = false
        var focusedCard: View? = null
        setContentView(homeRouteScreen(
            state = state,
            canGoBack = viewModel.workflowController.canGoBackInBrowse(),
            canPageBackward = viewModel.workflowController.canPageBackwardInBrowse(),
            canPageForward = viewModel.workflowController.canPageForwardInBrowse(),
            onSearch = ::showSearch,
            onRefresh = ::refreshHome,
            onLogout = authRoutes::logoutFromHome,
            onBackInBrowse = { showHome(viewModel.workflowController.back()) },
            onPreviousPage = ::previousBrowsePage,
            onNextPage = ::nextBrowsePage,
            onOpen = playbackRoutes::openMediaItem,
            loadImage = ::loadPrimaryImage,
            onFocusedCard = { focusedCard = it },
        ))
        focusedCard?.post { focusedCard?.requestFocus() }
    }

    private fun showSearch() {
        searchVisible = true
        val searchViews = searchScreen(
            onSubmit = ::submitSearch,
            onBackHome = { showHome(viewModel.workflowController.state()) },
        )
        setContentView(searchViews.root)
        searchViews.input.post { searchViews.input.requestFocus() }
    }

    private fun refreshHome() {
        runTask("正在重新加载首页...", {
            viewModel.workflowController.loadHome()
        }) {
            showHome(viewModel.workflowController.state())
        }
    }

    private fun previousBrowsePage() {
        runTask("正在加载上一页...", {
            viewModel.workflowController.previousBrowsePage()
        }) {
            showHome(viewModel.workflowController.state())
        }
    }

    private fun nextBrowsePage() {
        runTask("正在加载下一页...", {
            viewModel.workflowController.nextBrowsePage()
        }) {
            showHome(viewModel.workflowController.state())
        }
    }

    private fun submitSearch(term: String, searchInput: EditText) {
        if (term.isBlank()) {
            searchInput.requestFocus()
            return
        }
        runTask("正在搜索...", {
            viewModel.workflowController.search(term)
        }) {
            showHome(viewModel.workflowController.state())
        }
    }

    private fun loadPrimaryImage(target: ImageView, item: MediaItemSummary, width: Int, height: Int) {
        primaryImageLoader.load(this, viewModel.workflowController.state().authenticated(), target, item, width, height)
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
        val message = if (authenticationExpired) {
            "会话已过期，请重新登录"
        } else {
            tvErrorMessage(error)
        }
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
