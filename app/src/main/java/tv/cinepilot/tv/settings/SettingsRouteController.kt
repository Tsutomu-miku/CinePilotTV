package tv.cinepilot.tv.settings

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.plugin.spi.AuthVerificationResult
import tv.cinepilot.tv.compose.screens.ComposeSettingsScreen
import tv.cinepilot.tv.compose.screens.settings.BangumiPluginSettingsScreen
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.home.HomeSettings
import tv.cinepilot.tv.home.HomeSettingsStore
import tv.cinepilot.tv.plugin.PluginHost
import tv.cinepilot.tv.ui.TvColors
import tv.cinepilot.plugin.spi.PluginStatus

class SettingsRouteController(
    private val activity: ComponentActivity,
    private val settingsStore: SettingsStore,
    private val homeSettingsStore: HomeSettingsStore,
    private val pluginHost: PluginHost,
    private val showHome: (TvAppState) -> Unit,
    private val renderView: (View) -> Unit,
    private val renderCompose: (String, @Composable (CinePilotPalette) -> Unit) -> Unit,
    private val renderComposeFull: (@Composable (CinePilotPalette) -> Unit) -> Unit,
    private val runSilentTask: (
        task: () -> Unit,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) -> Unit,
) {
    private var returnState: TvAppState? = null
    private var visible = false

    // Plugin sub-screen state
    private var activePlugin: PluginHost.PluginInfo? = null
    private var activePluginLastError = ""
    private var pluginToken = ""
    private var pluginVerifying = false
    private var pluginVerifyMessage = ""
    private var pluginVerifyIsError = false
    /**
     * Bumped whenever the plugin auth context changes (disconnect, closeIfVisible,
     * clear of the plugin sub-screen, plugin change). Any in-flight verification uses this
     * snapshots the value at enqueue time and bails out if the epoch has shifted
     * by the time verification completes, so a "先点断开连接 races with pending /me.
     */
    private var pluginAuthEpoch: Long = 0L

    fun show(state: TvAppState) {
        returnState = state
        visible = true
        render()
    }

    fun hide() {
        visible = false
    }

    fun closeIfVisible(): Boolean {
        if (!visible) {
            return false
        }
        if (activePlugin != null) {
            // Back from plugin sub-screen -> return to main settings
            activePlugin = null
            activePluginLastError = ""
            pluginToken = ""
            pluginVerifying = false
            pluginVerifyMessage = ""
            pluginVerifyIsError = false
            pluginAuthEpoch++ // cancel any in-flight verification for this plugin
            render()
            return true
        }
        visible = false
        returnState?.let(showHome)
        return true
    }

    private fun render(
        themeSnapshot: AppTheme? = null,
        homeSettingsSnapshot: HomeSettings? = null,
    ) {
        val theme = themeSnapshot ?: settingsStore.stateFlow.value
        val homeSettings = homeSettingsSnapshot ?: homeSettingsStore.stateFlow.value
        TvColors.applyTheme(theme.id)

        val plugin = activePlugin
        if (plugin != null) {
            renderCompose(plugin.descriptor.name()) { palette ->
                BangumiPluginSettingsScreen(
                    palette = palette,
                    pluginHost = pluginHost,
                    pluginInfo = plugin,
                    tokenFieldValue = pluginToken,
                    onTokenChange = { pluginToken = it },
                    verifying = pluginVerifying,
                    verifyMessage = pluginVerifyMessage,
                    verifyMessageIsError = pluginVerifyIsError,
                    lastError = activePluginLastError,
                    onBack = { closeIfVisible() },
                    onVerify = { verifyCurrentPlugin() },
                    onDisconnect = { disconnectCurrentPlugin() },
                )
            }
            return
        }

        renderCompose("设置") { palette ->
            ComposeSettingsScreen(
                palette = palette,
                theme = theme,
                homeSettings = homeSettings,
                pluginHost = pluginHost,
                onTheme = { selected ->
                    activity.lifecycleScope.launch {
                        settingsStore.saveThemeAsync(selected)
                    }
                    TvColors.applyTheme(selected.id)
                    render(themeSnapshot = selected, homeSettingsSnapshot = homeSettings)
                },
                onHomeSettings = { next ->
                    activity.lifecycleScope.launch {
                        homeSettingsStore.saveAsync(next)
                    }
                    render(themeSnapshot = theme, homeSettingsSnapshot = next)
                },
                onOpenPlugin = { info -> openPlugin(info) },
            )
        }
    }

    // --- Plugin sub-screen ---------------------------------------------

    private fun openPlugin(info: PluginHost.PluginInfo) {
        activePlugin = info
        pluginToken = info.store.getString("token", "")
        activePluginLastError = info.store.getString("lastError", "")
        pluginVerifying = false
        pluginVerifyMessage = when (info.status) {
            PluginStatus.READY -> "当前令牌已通过验证。"
            PluginStatus.TEMPORARILY_UNAVAILABLE -> activePluginLastError.ifBlank {
                "上次调用失败，请重试验证。"
            }
            else -> ""
        }
        pluginVerifyIsError = info.status == PluginStatus.TEMPORARILY_UNAVAILABLE
        render()
    }

    private fun verifyCurrentPlugin() {
        val plugin = activePlugin ?: return
        val token = pluginToken.trim()
        if (token.isBlank()) {
            pluginVerifyMessage = "请输入令牌后再验证"
            pluginVerifyIsError = true
            render()
            return
        }
        // Snapshot the auth epoch and the current plugin id. If the user
        // disconnects, navigates away, or switches plugins while the /me
        // round-trip is in flight, we skip both savePluginAuth and the
        // success banner when the result lands.
        val epochAtStart = pluginAuthEpoch
        val pluginIdAtStart = plugin.descriptor.id()
        pluginVerifying = true
        pluginVerifyMessage = ""
        pluginVerifyIsError = false
        render()
        val resultHolder = arrayOfNulls<AuthVerificationResult>(1)
        runSilentTask(
            {
                val result: AuthVerificationResult = pluginHost.verifyPluginAuth(
                    pluginIdAtStart, token)
                if (result.isOk && pluginAuthEpoch == epochAtStart &&
                    pluginIdAtStart == activePlugin?.descriptor?.id() &&
                    token == pluginToken.trim()) {
                    pluginHost.savePluginAuth(pluginIdAtStart, token)
                }
                resultHolder[0] = result
            },
            {
                pluginVerifying = false
                val result = resultHolder[0] ?: AuthVerificationResult.failure("")
                // If the auth epoch or active plugin changed while the
                // request was in flight, silently drop the result.
                if (pluginAuthEpoch != epochAtStart ||
                    activePlugin?.descriptor?.id() != pluginIdAtStart) {
                    return@runSilentTask
                }
                pluginVerifyIsError = !result.isOk
                pluginVerifyMessage = when {
                    result.isOk -> buildString {
                        append("验证成功 ")
                        if (result.displayName().isNotBlank()) {
                            append("(")
                            append(result.displayName())
                            append(")")
                        }
                        append("，令牌已保存。")
                    }
                    result.message().isNotBlank() -> result.message()
                    else -> "验证失败"
                }
                activePlugin = freshPluginInfo(pluginIdAtStart)
                activePluginLastError = activePlugin?.store
                    ?.getString("lastError", "") ?: ""
                render()
            },
            { error ->
                // Same epoch check as onSuccess: only surface error if the
                // verify attempt still matches the current plugin context.
                if (pluginAuthEpoch == epochAtStart &&
                    activePlugin?.descriptor?.id() == pluginIdAtStart) {
                    pluginVerifying = false
                    pluginVerifyIsError = true
                    pluginVerifyMessage = error.message ?: "验证失败"
                    render()
                }
            },
        )
    }

    private fun disconnectCurrentPlugin() {
        val plugin = activePlugin ?: return
        pluginHost.clearPluginAuth(plugin.descriptor.id())
        pluginToken = ""
        pluginVerifyMessage = "已断开连接，令牌已删除。"
        pluginVerifyIsError = false
        activePluginLastError = ""
        pluginAuthEpoch++ // cancel any pending verify for this plugin context
        activePlugin = freshPluginInfo(plugin.descriptor.id())
        render()
    }

    private fun freshPluginInfo(pluginId: String): PluginHost.PluginInfo? {
        return pluginHost.listPlugins().firstOrNull { it.descriptor.id() == pluginId }
    }
}
