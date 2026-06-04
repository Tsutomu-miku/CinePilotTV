package tv.cinepilot.tv.auth

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.concurrent.Executor
import tv.cinepilot.core.protocol.PublicUserSummary
import tv.cinepilot.core.protocol.QuickConnectSession
import tv.cinepilot.core.protocol.ServerFlavor
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvWorkflowController
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
    private val loadPublicUserImage: (ImageView, PublicUserSummary, Int, Int) -> Unit,
) {
    private val recentAccountStore by lazy { RecentAccountStore(activity) }
    private var quickConnectStatus: TextView? = null
    private val quickConnectPoller = QuickConnectPoller(
        mainHandler,
        executor,
        workflowController,
        onWaiting = ::updateQuickConnectWaiting,
        onApproved = {
            rememberAccount()
            showHome(workflowController.state())
        },
        onError = showError,
    )

    fun showServerEntry() {
        stopQuickConnectPolling()
        val recentAccounts = recentAccountStore.accounts()
        val recentServers = recentAccountStore.servers()
            .filterNot { server -> recentAccounts.any { account -> account.serverAddress == server.serverAddress } }
        activity.setContentView(activity.serverEntryScreen(
            recentAccounts = recentAccounts,
            recentServers = recentServers,
            onContinueAccount = { account ->
                runTask("正在恢复上次登录...", {
                    workflowController.submitServer(account.serverAddress)
                    workflowController.restoreSession(account.userId)
                }) {
                    showHome(workflowController.state())
                }
            },
            onOpenServer = ::connectToServer,
            onClearAccounts = {
                clearSavedAccounts()
                showServerEntry()
            },
        ))
    }

    fun restoreRecentAccountOnLaunch() {
        val account = recentAccountStore.accounts().firstOrNull()
        if (account == null) {
            showServerEntry()
            return
        }
        showLoading("正在恢复上次登录...")
        executor.execute {
            try {
                workflowController.submitServer(account.serverAddress)
                workflowController.restoreSession(account.userId)
                activity.runOnUiThread { showHome(workflowController.state()) }
            } catch (_: Throwable) {
                activity.runOnUiThread { showServerEntry() }
            }
        }
    }

    fun showLogin() {
        stopQuickConnectPolling()
        val state = workflowController.state()
        activity.setContentView(activity.loginScreen(
            serverName = state.server()?.serverName().orEmpty(),
            publicUsers = state.publicUsers(),
            quickConnectAvailable = state.server()?.flavor() == ServerFlavor.JELLYFIN,
            loadPublicUserImage = loadPublicUserImage,
            onLogin = ::loginWithCredentials,
            onQuickConnect = ::startQuickConnectLogin,
            onBackToServer = ::showServerEntry,
        ))
    }

    fun handleQaLoginIntent(intent: Intent?): Boolean {
        if (!isDebuggable()) {
            return false
        }
        val server = intent?.getStringExtra("qa_server")?.takeIf { it.isNotBlank() } ?: return false
        val username = intent.getStringExtra("qa_username").orEmpty()
        val password = intent.getStringExtra("qa_password").orEmpty()
        showLoading("正在执行 QA 登录...")
        executor.execute {
            try {
                workflowController.submitServer(server)
                loadPublicUsersIfAvailable()
                workflowController.login(username, password)
                activity.runOnUiThread {
                    rememberAccount()
                    showHome(workflowController.state())
                }
            } catch (error: Throwable) {
                activity.runOnUiThread { showError(error) }
            }
        }
        return true
    }

    fun logoutFromHome() {
        runTask("正在退出登录...", {
            forgetAuthenticatedAccount()
            workflowController.logout()
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
        runTask("正在登录并加载首页...", {
            workflowController.login(username, password)
        }) {
            rememberAccount()
            showHome(workflowController.state())
        }
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
        val quickConnectViews = activity.quickConnectScreen(
            quickConnect = quickConnect,
            onCheckNow = ::checkQuickConnectNow,
            onBackToLogin = ::showLogin,
        )
        quickConnectStatus = quickConnectViews.status
        activity.setContentView(quickConnectViews.root)
    }

    private fun updateQuickConnectWaiting(attempts: Int) {
        quickConnectStatus?.text = "还没有授权，已自动检查 $attempts 次"
    }

    private fun checkQuickConnectNow() {
        quickConnectStatus?.text = "正在检查授权..."
        quickConnectPoller.checkNow()
    }

    private fun isDebuggable(): Boolean {
        return (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun loadPublicUsersIfAvailable() {
        runCatching { workflowController.loadPublicUsers() }
    }

    private fun rememberAccount() {
        val authenticated = workflowController.state().authenticated() ?: return
        recentAccountStore.remember(authenticated)
    }

    private fun clearSavedAccounts() {
        recentAccountStore.clear()
    }
}
