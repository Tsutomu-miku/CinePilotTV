package tv.cinepilot.tv

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import tv.cinepilot.core.protocol.MediaBrowserException
import tv.cinepilot.core.protocol.MediaItemSummary
import tv.cinepilot.core.protocol.PlaybackSelectionPreferences
import tv.cinepilot.core.tv.HomeRow
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.core.tv.TvDiagnostics
import tv.cinepilot.core.tv.TvRoute
import tv.cinepilot.core.tv.TvWorkflowController
import tv.cinepilot.tv.player.Media3PlayerHost
import tv.cinepilot.tv.runtime.CinePilotRuntime

class MainActivity : Activity() {
    private lateinit var runtime: CinePilotRuntime
    private lateinit var playerHost: Media3PlayerHost
    private val executor = Executors.newSingleThreadExecutor()
    private val lastAccountStore by lazy { getSharedPreferences("cinepilot_last_account", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = CinePilotRuntime.create(this)
        playerHost = Media3PlayerHost(this, runtime.mediaBrowserClient)
        showServerEntry()
    }

    override fun onDestroy() {
        playerHost.release()
        executor.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android framework; retained for API 26 TV devices.")
    override fun onBackPressed() {
        when (runtime.workflowController.state().route()) {
            TvRoute.SERVER_ENTRY -> super.onBackPressed()
            TvRoute.HOME -> {
                val state = runtime.workflowController.back()
                if (state.route() == TvRoute.HOME) {
                    showHome(state)
                } else {
                    showServerEntry()
                }
            }
            TvRoute.LOGIN, TvRoute.ERROR -> {
                val state = runtime.workflowController.back()
                if (state.route() == TvRoute.HOME) {
                    showHome(state)
                } else {
                    showServerEntry()
                }
            }
            TvRoute.DETAILS -> {
                runtime.workflowController.back()
                showHome(runtime.workflowController.state())
            }
            TvRoute.PLAYER -> {
                playerHost.release()
                runtime.workflowController.back()
                runtime.workflowController.state().selectedItem()?.let(::showDetails)
                    ?: showHome(runtime.workflowController.state())
            }
        }
    }

    private fun showServerEntry() {
        val serverInput = input("https://your-server.example.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val recentAccounts = savedAccounts()
        setContentView(screen("CinePilot TV") {
            recentAccounts.forEach { account ->
                addView(action("继续 ${account.displayName()}") {
                    runTask("正在恢复上次登录...", {
                        runtime.workflowController.submitServer(account.serverAddress)
                        runtime.workflowController.restoreSession(account.userId)
                    }) {
                        showHome(runtime.workflowController.state())
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
                    runtime.workflowController.submitServer(serverInput.text.toString())
                    loadPublicUsersIfAvailable()
                }) {
                    showLogin()
                }
            })
        })
    }

    private fun showLogin() {
        val usernameInput = input("用户名", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
        val passwordInput = input("密码", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        setContentView(screen("登录 ${runtime.workflowController.state().server()?.serverName() ?: ""}") {
            if (runtime.workflowController.state().publicUsers().isNotEmpty()) {
                addView(section("选择用户"))
                runtime.workflowController.state().publicUsers().forEach { user ->
                    addView(action(user.name().ifBlank { user.id() }) {
                        usernameInput.setText(user.name().ifBlank { user.id() })
                        passwordInput.requestFocus()
                    })
                }
            }
            addView(label("用户名"))
            addView(usernameInput)
            addView(label("密码"))
            addView(passwordInput)
            addView(action("登录") {
                runTask("正在登录并加载首页...", {
                    runtime.workflowController.login(
                        usernameInput.text.toString(),
                        passwordInput.text.toString(),
                    )
                }) {
                    rememberAccount()
                    showHome(runtime.workflowController.state())
                }
            })
            addView(action("返回服务器输入") { showServerEntry() })
        })
    }

    private fun showHome(state: TvAppState) {
        var focusedButton: View? = null
        setContentView(screen("首页") {
            if (state.homeRows().isEmpty()) {
                addView(label("没有可显示的媒体"))
            }
            state.homeRows().forEach { row ->
                addView(section(row.title()))
                row.items().forEach { item ->
                    val button = itemButton(row, item)
                    if (state.focus()?.rowId() == row.id() && state.focus()?.itemId() == item.id()) {
                        focusedButton = button
                    }
                    addView(button)
                }
            }
            if (runtime.workflowController.canGoBackInBrowse()) {
                addView(action("返回上级") {
                    showHome(runtime.workflowController.back())
                })
            }
            addView(action("重新加载首页") {
                runTask("正在重新加载首页...", {
                    runtime.workflowController.loadHome()
                }) {
                    showHome(runtime.workflowController.state())
                }
            })
            addView(action("退出登录") {
                runTask("正在退出登录...", {
                    forgetAuthenticatedAccount()
                    runtime.workflowController.logout()
                }) {
                    showServerEntry()
                }
            })
        })
        focusedButton?.post { focusedButton?.requestFocus() }
    }

    private fun showDetails(item: MediaItemSummary) {
        setContentView(screen(item.name()) {
            addView(label("${item.type()}${if (item.productionYear() != null) " · ${item.productionYear()}" else ""}"))
            if (item.hasResumePosition()) {
                addView(label("可从 ${item.userData().playbackPositionTicks()} ticks 继续播放"))
            }
            if (item.playable()) {
                if (item.hasResumePosition()) {
                    addView(playbackAction("继续播放", null))
                    addView(playbackAction("从头播放", PlaybackSelectionPreferences.defaults()))
                } else {
                    addView(playbackAction("播放", null))
                }
            } else {
                addView(openFolderAction(item))
            }
            addView(action("返回首页") {
                runtime.workflowController.back()
                showHome(runtime.workflowController.state())
            })
        })
    }

    private fun showPlayerReady(state: TvAppState) {
        val playable = state.playableMedia()
        setContentView(screen("准备播放") {
            addView(label("播放方式：${playable?.playMethod() ?: ""}"))
            addView(label("媒体源：${playable?.mediaSourceId() ?: ""}"))
            addView(label(playable?.url() ?: playable?.request()?.path() ?: ""))
            addView(action("打开播放器") { showPlayer(state) })
            addView(action("诊断信息") { showDiagnostics(state) })
            addView(action("返回详情") {
                runtime.workflowController.back()
                runtime.workflowController.state().selectedItem()?.let(::showDetails)
            })
        })
    }

    private fun showDiagnostics(state: TvAppState) {
        setContentView(screen("诊断信息") {
            addView(label(TvDiagnostics.describe(state)))
            addView(action("返回播放准备") { showPlayerReady(state) })
        })
    }

    private fun showPlayer(state: TvAppState) {
        setContentView(screen("播放器") {
            addView(playerHost.createPlayerView(state), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                720,
            ))
            addView(action("停止并返回详情") {
                playerHost.release()
                runtime.workflowController.back()
                runtime.workflowController.state().selectedItem()?.let(::showDetails)
            })
        })
    }

    private fun itemButton(row: HomeRow, item: MediaItemSummary): View {
        return action(item.name().ifBlank { item.id() }) {
            runtime.workflowController.focusItem(row.id(), item.id())
            val loadingMessage = if (item.playable()) "正在打开详情..." else "正在打开目录..."
            runTask(loadingMessage, {
                if (item.playable()) {
                    runtime.workflowController.openItem(item.id())
                } else {
                    runtime.workflowController.openFolder(item.id(), item.name())
                }
            }) {
                val state = runtime.workflowController.state()
                if (item.playable()) {
                    state.selectedItem()?.let(::showDetails)
                } else {
                    showHome(state)
                }
            }
        }.also {
            it.contentDescription = "${row.title()} ${item.name()}"
        }
    }

    private fun openFolderAction(item: MediaItemSummary): View {
        return action("打开子项目") {
            runTask("正在打开目录...", {
                runtime.workflowController.openFolder(item.id(), item.name())
            }) {
                showHome(runtime.workflowController.state())
            }
        }
    }

    private fun playbackAction(text: String, preferences: PlaybackSelectionPreferences?): View {
        return action(text) {
            runTask("正在准备播放...", {
                runtime.workflowController.preparePlayback(preferences)
            }) {
                showPlayerReady(runtime.workflowController.state())
            }
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
            runtime.workflowController.forgetAuthenticatedSession()
        } else {
            runtime.workflowController.fail(error.message ?: error::class.java.simpleName)
        }
        val state = runtime.workflowController.state()
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
        return when (error.message) {
            TvWorkflowController.NO_CHILD_ITEM_MESSAGE -> "目录中没有可打开的媒体"
            TvWorkflowController.NO_PLAYABLE_SOURCE_MESSAGE -> "没有可用播放源"
            else -> error.message ?: error::class.java.simpleName
        }
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
        runCatching { runtime.workflowController.loadPublicUsers() }
    }

    private fun screen(title: String, content: LinearLayout.() -> Unit): ScrollView {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(Color.rgb(11, 16, 32))
        }
        container.addView(TextView(this).apply {
            text = title
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.START
            setPadding(0, 0, 0, 24)
        })
        container.content()
        return ScrollView(this).apply { addView(container) }
    }

    private fun input(hintText: String, inputTypeValue: Int): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = inputTypeValue
            textSize = 20f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(20, 12, 20, 12)
        }
    }

    private fun action(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 20f
            isAllCaps = false
            setOnClickListener { onClick() }
            setPadding(20, 14, 20, 14)
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }
    }

    private fun section(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.rgb(45, 212, 191))
            setPadding(0, 28, 0, 8)
        }
    }

    private fun rememberAccount() {
        val authenticated = runtime.workflowController.state().authenticated() ?: return
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
        val authenticated = runtime.workflowController.state().authenticated() ?: return
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
