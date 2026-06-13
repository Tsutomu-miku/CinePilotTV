package tv.cinepilot.tv.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.compose.components.InfoPanel
import tv.cinepilot.tv.compose.components.SettingsGrid
import tv.cinepilot.tv.compose.components.TvActionButton
import tv.cinepilot.tv.compose.theme.CinePilotPalette
import tv.cinepilot.tv.compose.theme.TvDp

@Composable
fun ComposeErrorScreen(
    palette: CinePilotPalette,
    state: TvAppState,
    message: String,
    authenticationExpired: Boolean,
    onReturnDetails: () -> Unit,
    onRetryLowBitrate: () -> Unit,
    onPlaybackOptions: () -> Unit,
    onDiagnostics: () -> Unit,
    onHome: () -> Unit,
    onLogin: () -> Unit,
    onServerEntry: () -> Unit,
) {
    val actions = recoveryActions(
        state = state,
        authenticationExpired = authenticationExpired,
        onReturnDetails = onReturnDetails,
        onRetryLowBitrate = onRetryLowBitrate,
        onPlaybackOptions = onPlaybackOptions,
        onDiagnostics = onDiagnostics,
        onHome = onHome,
        onLogin = onLogin,
        onServerEntry = onServerEntry,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDp.RowGap),
        modifier = Modifier.fillMaxWidth(),
    ) {
        InfoPanel(
            palette = palette,
            title = if (authenticationExpired) "会话已过期" else "播放遇到问题",
            body = message,
        )
        SettingsGrid(
            palette = palette,
            title = "恢复操作",
            rows = actions.mapIndexed { index, action ->
                {
                    TvActionButton(
                        palette = palette,
                        label = action.label,
                        selected = index == 0,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = action.onClick,
                    )
                }
            },
        )
    }
}

private data class RecoveryAction(
    val label: String,
    val onClick: () -> Unit,
)

private fun recoveryActions(
    state: TvAppState,
    authenticationExpired: Boolean,
    onReturnDetails: () -> Unit,
    onRetryLowBitrate: () -> Unit,
    onPlaybackOptions: () -> Unit,
    onDiagnostics: () -> Unit,
    onHome: () -> Unit,
    onLogin: () -> Unit,
    onServerEntry: () -> Unit,
): List<RecoveryAction> {
    return buildList {
        val canRecoverPlayback = state.selectedItem() != null && !authenticationExpired
        if (canRecoverPlayback) {
            add(RecoveryAction("低码率重试", onRetryLowBitrate))
            add(RecoveryAction("返回详情", onReturnDetails))
            add(RecoveryAction("切换音轨 / 字幕", onPlaybackOptions))
            add(RecoveryAction("诊断信息", onDiagnostics))
        }
        if (state.homeRows().isNotEmpty() && !authenticationExpired) {
            add(RecoveryAction("返回首页", onHome))
        }
        if (state.server() != null) {
            add(RecoveryAction("重新登录", onLogin))
        }
        add(RecoveryAction("返回服务器输入", onServerEntry))
    }
}
