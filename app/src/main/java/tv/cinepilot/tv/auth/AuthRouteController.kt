package tv.cinepilot.tv.auth

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.core.protocol.ServerFlavor
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.compose.screens.ComposeLoginScreen
import tv.cinepilot.tv.compose.screens.ComposeQuickConnectScreen
import tv.cinepilot.tv.compose.screens.ComposeServerEntryScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.runtime.QuickConnectPoller
import tv.cinepilot.tv.runtime.RecentAccountStore

class AuthRouteController(
    private val activity: ComponentActivity,
    private val workflowController: TvWorkflowController,
    mainHandler: Handler,
    private val executor: Executor,
    private val runTask: (String, () -> Unit, () -> Unit) -> Unit,
    private val showLoading: (String) -> Unit,
    private val showHome: (TvAppState) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
    /**
     * Cache-aware entry flow: execute (preload) on the background, restore any
     * cached home rows to the UI as soon as possible, then run a full network
     * refresh and persist the result. Caller provides a loading-message hint
     * (null means "prefer cached paint over loading overlay"), an optional
     * (remember) callback invoked on the UI thread after a successful network
     * refresh, and a (fallback) for preload failure.
     */
    private val runHomeEntry: (String?, () -> Unit, () -> Unit, (Throwable) -> Unit) -> Unit,
    /** Persist currently-loaded home rows; safe to call from any thread. */
    private val persistHomeCache: () -> Unit,
    /** Clear any cached rows for the currently-authenticated scope. */
    private val clearHomeCache: () -> Unit,
) {
    private val recentAccountStore by lazy { RecentAccountStore(activity) }
    private var quickConnectStatus by mutableStateOf("")
    private val quickConnectPoller = QuickConnectPoller(
        mainHandler,
        executor,
        workflowController,
        onWaiting = ::updateQuickConnectWaiting,
        onApproved = {
            rememberAccount()
            persistHomeCache()
            showHome(workflowController.state())
        },
        onError = showError,
    )

    fun showServerEntry() {
        stopQuickConnectPolling()
        val recentAccounts = recentAccountStore.accounts()
        val recentServers = recentAccountStore.servers()
            .filterNot { server -> recentAccounts.any { account -> account.serverAddress == server.serverAddress } }
        renderCompose("CinePilot TV") { palette ->
            ComposeServerEntryScreen(
                palette = palette,
                recentAccounts = recentAccounts,
                recentServers = recentServers,
                onContinueAccount = { account ->
                    runHomeEntry(
                        null,
                        {
                            workflowController.submitServer(account.serverAddress)
                            workflowController.restoreSessionWithoutHome(account.userId)
                        },
                        {},
                        ::fallbackToServerEntry,
                    )
                },
                onOpenServer = ::connectToServer,
                onClearAccounts = {
                    clearSavedAccounts()
                    showServerEntry()
                },
            )
        }
    }

    fun restoreRecentAccountOnLaunch() {
        val account = recentAccountStore.accounts().firstOrNull()
        if (account == null) {
            showServerEntry()
            return
        }
        runHomeEntry(
            null,
            {
                workflowController.submitServer(account.serverAddress)
                workflowController.restoreSessionWithoutHome(account.userId)
            },
            {},
            { activity.runOnUiThread { showServerEntry() } },
        )
    }

    fun showLogin() {
        stopQuickConnectPolling()
        val state = workflowController.state()
        renderCompose("登录") { palette ->
            ComposeLoginScreen(
                palette = palette,
                serverName = state.server()?.serverName().orEmpty(),
                publicUsers = state.publicUsers(),
                quickConnectAvailable = state.server()?.flavor() == ServerFlavor.JELLYFIN,
                onLogin = ::loginWithCredentials,
                onQuickConnect = ::startQuickConnectLogin,
                onBackToServer = ::showServerEntry,
            )
        }
    }

    fun handleQaLoginIntent(intent: Intent?): Boolean {
        if (!isDebuggable()) {
            return false
        }
        val server = intent?.getStringExtra("qa_server")?.takeIf { it.isNotBlank() } ?: return false
        val username = intent.getStringExtra("qa_username").orEmpty()
        val password = intent.getStringExtra("qa_password").orEmpty()
        runHomeEntry(
            "正在执行 QA 登录...",
            {
                workflowController.submitServer(server)
                loadPublicUsersIfAvailable()
                workflowController.loginWithoutHome(username, password)
            },
            ::rememberAccount,
            { err -> showError(err) },
        )
        return true
    }

    fun logoutFromHome() {
        runTask("正在退出登录...", {
            forgetAuthenticatedAccount()
            workflowController.logout()
            clearHomeCache()
        }) {
            showServerEntry()
        }
    }

    fun stopQuickConnectPolling() {
        quickConnectPoller.stop()
    }

    fun forgetAuthenticatedAccount() {
        val authenticated = workflowController.state().authenticated() ?: return
        recentAccountStore.forget(authenticated)
    }

    private fun connectToServer(serverAddress: String) {
        runTask("正在连接服务器...", {
            workflowController.submitServer(serverAddress)
            workflowController.state().server()?.let { server ->
                recentAccountStore.rememberServer(server.address().value(), server.serverName())
            }
            loadPublicUsersIfAvailable()
        }) {
            showLogin()
        }
    }

    private fun loginWithCredentials(username: String, password: String) {
        runHomeEntry(
            "正在登录并加载首页...",
            { workflowController.loginWithoutHome(username, password) },
            ::rememberAccount,
            { err -> showError(err) },
        )
    }

    private fun startQuickConnectLogin() {
        showLoading("正在创建 Quick Connect...")
        executor.execute {
            try {
                val quickConnect = workflowController.startQuickConnect()
                activity.runOnUiThread {
                    showQuickConnect(quickConnect)
                    quickConnectPoller.start()
                }
            } catch (error: Throwable) {
                activity.runOnUiThread { showError(error) }
            }
        }
    }

    private fun showQuickConnect(quickConnect: QuickConnectSession) {
        quickConnectStatus = "正在等待授权，电视会每 2 秒自动检查一次"
        renderCompose("Quick Connect") { palette ->
            ComposeQuickConnectScreen(
                palette = palette,
                quickConnect = quickConnect,
                status = quickConnectStatus,
                onCheckNow = ::checkQuickConnectNow,
                onBackToLogin = ::showLogin,
            )
        }
    }

    private fun updateQuickConnectWaiting(attempts: Int) {
        quickConnectStatus = "还没有授权，已自动检查 $attempts 次"
    }

    private fun checkQuickConnectNow() {
        quickConnectStatus = "正在检查授权..."
        quickConnectPoller.checkNow()
    }

    private fun isDebuggable(): Boolean {
        return (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun loadPublicUsersIfAvailable() {
        runCatching { workflowController.loadPublicUsers() }
    }

    /**
     * Cache-aware login flows swallow failures with a plain server-entry
     * fallback so that the launch path is never left stuck on a loading or
     * error surface after a restoreSession.
     */
    private fun fallbackToServerEntry(@Suppress("unused") ignored: Throwable) {
        activity.runOnUiThread { showServerEntry() }
    }

    private fun rememberAccount() {
        val authenticated = workflowController.state().authenticated() ?: return
        recentAccountStore.remember(authenticated)
    }

    private fun clearSavedAccounts() {
        recentAccountStore.clear()
    }
}
