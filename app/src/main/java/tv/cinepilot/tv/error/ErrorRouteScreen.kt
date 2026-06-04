package tv.cinepilot.tv.error

import android.view.View
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import tv.cinepilot.core.tv.TvAppState
import tv.cinepilot.tv.ui.TvIcon
import tv.cinepilot.tv.ui.action
import tv.cinepilot.tv.ui.iconAction
import tv.cinepilot.tv.ui.label
import tv.cinepilot.tv.ui.requestInitialFocus
import tv.cinepilot.tv.ui.screen

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
): ScrollView {
    return screen("出错了") {
        addView(label(message))
        val canRecoverPlayback = state.selectedItem() != null && !authenticationExpired
        val actions = mutableListOf<View>()
        if (canRecoverPlayback) {
            actions.add(iconAction("返回详情", TvIcon.BACK, onReturnDetails))
            actions.add(iconAction("低码率重试", TvIcon.SPEED, onRetryLowBitrate))
            actions.add(iconAction("切换音轨 / 字幕", TvIcon.SUBTITLES, onPlaybackOptions))
            actions.add(action("诊断信息", onDiagnostics))
        }
        if (state.homeRows().isNotEmpty() && !authenticationExpired) {
            actions.add(iconAction("返回首页", TvIcon.BACK, onHome))
        }
        if (state.server() != null) {
            actions.add(action("重新登录", onLogin))
        }
        actions.add(iconAction("返回服务器输入", TvIcon.BACK, onServerEntry))
        actions.forEachIndexed { index, view ->
            addView(if (index == 0) view.requestInitialFocus() else view)
        }
    }
}
