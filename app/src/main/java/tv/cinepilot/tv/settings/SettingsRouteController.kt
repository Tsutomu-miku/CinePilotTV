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
    private var pluginToken = ""
    private var pluginVerifying = false
    private var pluginVerifyMessage = ""
    private var pluginVerifyIsError = false

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
            pluginToken = ""
            pluginVerifying = false
            pluginVerifyMessage = ""
            pluginVerifyIsError = false
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
        pluginVerifying = false
        pluginVerifyMessage = when (info.status) {
            PluginStatus.READY -> "当前令牌已通过验证。"
            PluginStatus.TEMPORARILY_UNAVAILABLE -> info.store.getString("lastError", "").let {
                it.ifBlank { "上次调用失败，请重试验证。" }
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
        pluginVerifying = true
        pluginVerifyMessage = ""
        pluginVerifyIsError = false
        render()
        runSilentTask(
            {
                val result: AuthVerificationResult = pluginHost.verifyPluginAuth(
                    plugin.descriptor.id(), token)
                if (result.isOk) {
                    pluginHost.savePluginAuth(plugin.descriptor.id(), token)
                }
                // stash the result across the thread boundary
                verificationResult = result
            },
            {
                pluginVerifying = false
                val result = verificationResult
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
                activePlugin = freshPluginInfo(plugin.descriptor.id())
                render()
            },
            { error ->
                pluginVerifying = false
                pluginVerifyIsError = true
                pluginVerifyMessage = error.message ?: "验证失败"
                render()
            },
        )
    }

    private var verificationResult: AuthVerificationResult =
        AuthVerificationResult.failure("")

    private fun disconnectCurrentPlugin() {
        val plugin = activePlugin ?: return
        pluginHost.clearPluginAuth(plugin.descriptor.id())
        pluginToken = ""
        pluginVerifyMessage = "已断开连接，令牌已删除。"
        pluginVerifyIsError = false
        activePlugin = freshPluginInfo(plugin.descriptor.id())
        render()
    }

    private fun freshPluginInfo(pluginId: String): PluginHost.PluginInfo? {
        return pluginHost.listPlugins().firstOrNull { it.descriptor.id() == pluginId }
    }
}
