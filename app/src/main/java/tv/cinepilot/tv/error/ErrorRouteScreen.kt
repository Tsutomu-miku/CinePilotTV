package tv.cinepilot.tv.error

import android.view.View
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.InfuseAction
import tv.cinepilot.tv.ui.InfuseActionEmphasis
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.compactPanelSpacing
import tv.cinepilot.tv.ui.infuseActions
import tv.cinepilot.tv.ui.infusePanelNote
import tv.cinepilot.tv.ui.infusePanelScreen
import tv.cinepilot.tv.ui.infusePanelTitle

fun ComponentActivity.errorRouteScreen(
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
): View {
    return infusePanelScreen("出错了") {
        addView(infusePanelTitle("播放遇到问题"))
        addView(infusePanelNote(message).compactPanelSpacing(16))
        val canRecoverPlayback = state.selectedItem() != null && !authenticationExpired
        val actions = mutableListOf<InfuseAction>()
        if (canRecoverPlayback) {
            actions.add(InfuseAction("低码率重试", TvIcon.SPEED, InfuseActionEmphasis.PRIMARY, onRetryLowBitrate))
            actions.add(InfuseAction("返回详情", TvIcon.BACK, InfuseActionEmphasis.QUIET, onReturnDetails))
            actions.add(InfuseAction("切换音轨 / 字幕", TvIcon.SUBTITLES, InfuseActionEmphasis.QUIET, onPlaybackOptions))
            actions.add(InfuseAction("诊断信息", TvIcon.INFO, InfuseActionEmphasis.QUIET, onDiagnostics))
        }
        if (state.homeRows().isNotEmpty() && !authenticationExpired) {
            actions.add(primaryOrSecondaryAction(actions, "返回首页", TvIcon.BACK, onHome))
        }
        if (state.server() != null) {
            actions.add(primaryOrSecondaryAction(actions, "重新登录", TvIcon.ACCOUNT, onLogin))
        }
        actions.add(primaryOrSecondaryAction(actions, "返回服务器输入", TvIcon.BACK, onServerEntry))
        addView(infuseActions(actions, requestFirstFocus = true))
    }
}

private fun primaryOrSecondaryAction(
    existingActions: List<InfuseAction>,
    text: String,
    icon: TvIcon,
    onClick: () -> Unit,
): InfuseAction {
    val emphasis = if (existingActions.isEmpty()) {
        InfuseActionEmphasis.PRIMARY
    } else {
        InfuseActionEmphasis.QUIET
    }
    return InfuseAction(text, icon, emphasis, onClick)
}
